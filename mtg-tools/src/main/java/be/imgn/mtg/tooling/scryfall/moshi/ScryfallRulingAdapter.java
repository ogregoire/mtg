package be.imgn.mtg.tooling.scryfall.moshi;

import java.io.IOException;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.tooling.scryfall.model.ScryfallRuling;

/// Manual JsonAdapter for [ScryfallRuling] to avoid reflection.
public final class ScryfallRulingAdapter extends JsonAdapter<ScryfallRuling> {

    private static final JsonReader.Options OPTIONS =
            JsonReader.Options.of("oracle_id", "source", "published_at", "comment");

    @Override
    public ScryfallRuling fromJson(JsonReader reader) throws IOException {
        String oracleId = null;
        String source = null;
        String publishedAt = null;
        String comment = null;

        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.selectName(OPTIONS)) {
                case 0 -> oracleId = reader.nextString();
                case 1 -> source = reader.nextString();
                case 2 -> publishedAt = reader.nextString();
                case 3 -> comment = reader.nextString();
                default -> {
                    reader.skipName();
                    reader.skipValue();
                }
            }
        }
        reader.endObject();

        if (oracleId == null || source == null || publishedAt == null || comment == null) {
            throw new JsonDataException("Required field missing");
        }

        return new ScryfallRuling(oracleId, source, publishedAt, comment);
    }

    @Override
    public void toJson(JsonWriter writer, @Nullable ScryfallRuling value) throws IOException {
        if (value == null) {
            writer.nullValue();
            return;
        }
        writer.beginObject();
        writer.name("oracle_id").value(value.oracleId());
        writer.name("source").value(value.source());
        writer.name("published_at").value(value.publishedAt());
        writer.name("comment").value(value.comment());
        writer.endObject();
    }
}
