package be.imgn.mtg.tooling.scryfall.moshi;

import java.io.IOException;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.tooling.scryfall.model.ScryfallBulkData;

/// Manual JsonAdapter for [ScryfallBulkData] to avoid reflection.
public final class ScryfallBulkDataAdapter extends JsonAdapter<ScryfallBulkData> {

    private static final JsonReader.Options OPTIONS = JsonReader.Options.of(
            "id", "type", "updated_at", "download_uri", "content_type", "content_encoding", "size");

    @Override
    public ScryfallBulkData fromJson(JsonReader reader) throws IOException {
        String id = null;
        String type = null;
        String updatedAt = null;
        String downloadUri = null;
        String contentType = null;
        @Nullable String contentEncoding = null;
        long size = 0;

        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.selectName(OPTIONS)) {
                case 0 -> id = reader.nextString();
                case 1 -> type = reader.nextString();
                case 2 -> updatedAt = reader.nextString();
                case 3 -> downloadUri = reader.nextString();
                case 4 -> contentType = reader.nextString();
                case 5 ->
                    contentEncoding = reader.peek() == JsonReader.Token.NULL ? reader.nextNull() : reader.nextString();
                case 6 -> size = reader.nextLong();
                default -> {
                    reader.skipName();
                    reader.skipValue();
                }
            }
        }
        reader.endObject();

        if (id == null || type == null || updatedAt == null || downloadUri == null || contentType == null) {
            throw new JsonDataException("Required field missing");
        }

        return new ScryfallBulkData(id, type, updatedAt, downloadUri, contentType, contentEncoding, size);
    }

    @Override
    public void toJson(JsonWriter writer, @Nullable ScryfallBulkData value) throws IOException {
        if (value == null) {
            writer.nullValue();
            return;
        }
        writer.beginObject();
        writer.name("id").value(value.id());
        writer.name("type").value(value.type());
        writer.name("updated_at").value(value.updatedAt());
        writer.name("download_uri").value(value.downloadUri());
        writer.name("content_type").value(value.contentType());
        writer.name("content_encoding").value(value.contentEncoding());
        writer.name("size").value(value.size());
        writer.endObject();
    }
}
