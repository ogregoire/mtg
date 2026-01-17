package be.imgn.mtg.tooling.scryfall;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.tooling.scryfall.model.ScryfallBulkData;
import be.imgn.mtg.tooling.scryfall.model.ScryfallCard;
import be.imgn.mtg.tooling.scryfall.model.ScryfallRuling;
import be.imgn.mtg.tooling.scryfall.model.ScryfallSet;
import be.imgn.mtg.tooling.scryfall.moshi.ScryfallBulkDataAdapter;
import be.imgn.mtg.tooling.scryfall.moshi.ScryfallCardAdapter;
import be.imgn.mtg.tooling.scryfall.moshi.ScryfallRulingAdapter;
import be.imgn.mtg.tooling.scryfall.moshi.ScryfallSetAdapter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okio.Okio;

/// Client for accessing the Scryfall API.
///
/// This client implements rate limiting (100ms between requests) and
/// disk caching to comply with Scryfall's API guidelines.
public final class ScryfallClient implements AutoCloseable {

    private static final String DEFAULT_API_BASE = "https://api.scryfall.com";
    private static final String BULK_DATA_ENDPOINT = "/bulk-data";
    private static final String SETS_ENDPOINT = "/sets";

    private final OkHttpClient client;
    private final Moshi moshi;
    private final JsonAdapter<List<ScryfallBulkData>> bulkDataListAdapter;
    private final JsonAdapter<List<ScryfallSet>> setListAdapter;
    private final String apiBase;

    /// Creates a new ScryfallClient with the specified cache directory.
    ///
    /// @param cacheDir directory for HTTP cache
    public ScryfallClient(Path cacheDir) {
        this(
                new OkHttpClient.Builder()
                        .addInterceptor(new RateLimitInterceptor())
                        .addInterceptor(new CacheInterceptor(cacheDir))
                        .build(),
                DEFAULT_API_BASE);
    }

    /// Package-private constructor for testing with custom client and base URL.
    ScryfallClient(OkHttpClient client, String apiBase) {
        this.client = client;
        this.apiBase = apiBase;
        this.moshi = new Moshi.Builder()
                .add(ScryfallBulkData.class, new ScryfallBulkDataAdapter())
                .add(ScryfallCard.class, new ScryfallCardAdapter())
                .add(ScryfallSet.class, new ScryfallSetAdapter())
                .add(ScryfallRuling.class, new ScryfallRulingAdapter())
                .build();
        this.bulkDataListAdapter = moshi.adapter(Types.newParameterizedType(List.class, ScryfallBulkData.class));
        this.setListAdapter = moshi.adapter(Types.newParameterizedType(List.class, ScryfallSet.class));
    }

    /// Result of downloading bulk data list, containing both the data and download size.
    public record BulkDataDownload(List<ScryfallBulkData> bulkData, long bytes) {}

    /// Gets the list of available bulk data downloads.
    ///
    /// @return bulk data list and download size
    /// @throws IOException if the request fails
    public BulkDataDownload getBulkDataList() throws IOException {
        var request = new Request.Builder().url(apiBase + BULK_DATA_ENDPOINT).build();

        try (var response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch bulk data list: " + response.code());
            }
            var body = response.body();
            if (body == null) {
                throw new IOException("Empty response body");
            }

            // Read body to get size, then parse
            var bodyBytes = body.bytes();
            var source = new okio.Buffer();
            source.write(bodyBytes);

            try (var reader = JsonReader.of(source)) {
                reader.beginObject();
                while (reader.hasNext()) {
                    if ("data".equals(reader.nextName())) {
                        var result = bulkDataListAdapter.fromJson(reader);
                        return new BulkDataDownload(result != null ? result : List.of(), bodyBytes.length);
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
            }
            return new BulkDataDownload(List.of(), bodyBytes.length);
        }
    }

    /// Result of downloading sets, containing both the data and download size.
    public record SetsDownload(List<ScryfallSet> sets, long bytes) {}

    /// Gets all sets from Scryfall.
    ///
    /// @return sets and download size
    /// @throws IOException if the request fails
    public SetsDownload getSets() throws IOException {
        var request = new Request.Builder().url(apiBase + SETS_ENDPOINT).build();

        try (var response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch sets: " + response.code());
            }
            var body = response.body();
            if (body == null) {
                throw new IOException("Empty response body");
            }

            // Read body to get size, then parse
            var bodyBytes = body.bytes();
            var source = new okio.Buffer();
            source.write(bodyBytes);

            try (var reader = JsonReader.of(source)) {
                reader.beginObject();
                while (reader.hasNext()) {
                    if ("data".equals(reader.nextName())) {
                        var result = setListAdapter.fromJson(reader);
                        return new SetsDownload(result != null ? result : List.of(), bodyBytes.length);
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
            }
            return new SetsDownload(List.of(), bodyBytes.length);
        }
    }

    /// Downloads a bulk data file and streams cards to the consumer.
    ///
    /// @param downloadUri the URI to download
    /// @param consumer consumer that receives each card
    /// @throws IOException if the download or parsing fails
    public void streamCards(String downloadUri, Consumer<ScryfallCard> consumer) throws IOException {
        streamCards(downloadUri, consumer, null);
    }

    /// Downloads a bulk data file and streams cards to the consumer with progress tracking.
    ///
    /// @param downloadUri the URI to download
    /// @param consumer consumer that receives each card
    /// @param bytesReadCallback optional callback for download progress (bytes read so far)
    /// @throws IOException if the download or parsing fails
    public void streamCards(
            String downloadUri, Consumer<ScryfallCard> consumer, @Nullable LongConsumer bytesReadCallback)
            throws IOException {
        var adapter = moshi.adapter(ScryfallCard.class);
        streamJsonArray(downloadUri, adapter, consumer, bytesReadCallback);
    }

    /// Downloads a bulk data file and streams sets to the consumer.
    ///
    /// @param downloadUri the URI to download
    /// @param consumer consumer that receives each set
    /// @throws IOException if the download or parsing fails
    public void streamSets(String downloadUri, Consumer<ScryfallSet> consumer) throws IOException {
        var adapter = moshi.adapter(ScryfallSet.class);
        streamJsonArray(downloadUri, adapter, consumer, null);
    }

    /// Downloads a bulk data file and streams rulings to the consumer.
    ///
    /// @param downloadUri the URI to download
    /// @param consumer consumer that receives each ruling
    /// @throws IOException if the download or parsing fails
    public void streamRulings(String downloadUri, Consumer<ScryfallRuling> consumer) throws IOException {
        streamRulings(downloadUri, consumer, null);
    }

    /// Downloads a bulk data file and streams rulings to the consumer with progress tracking.
    ///
    /// @param downloadUri the URI to download
    /// @param consumer consumer that receives each ruling
    /// @param bytesReadCallback optional callback for download progress (bytes read so far)
    /// @throws IOException if the download or parsing fails
    public void streamRulings(
            String downloadUri, Consumer<ScryfallRuling> consumer, @Nullable LongConsumer bytesReadCallback)
            throws IOException {
        var adapter = moshi.adapter(ScryfallRuling.class);
        streamJsonArray(downloadUri, adapter, consumer, bytesReadCallback);
    }

    private <T> void streamJsonArray(
            String uri, JsonAdapter<T> adapter, Consumer<T> consumer, @Nullable LongConsumer bytesReadCallback)
            throws IOException {
        var request = new Request.Builder().url(uri).build();

        try (var response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to download: " + response.code());
            }
            var body = response.body();
            if (body == null) {
                throw new IOException("Empty response body");
            }

            var source = body.source();
            if (bytesReadCallback != null) {
                source = Okio.buffer(new ProgressSource(source, bytesReadCallback));
            }

            try (var reader = JsonReader.of(source)) {
                reader.beginArray();
                while (reader.hasNext()) {
                    var item = adapter.fromJson(reader);
                    if (item != null) {
                        consumer.accept(item);
                    }
                }
                reader.endArray();
            }
        }
    }

    @Override
    public void close() {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }
}
