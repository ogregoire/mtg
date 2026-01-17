package be.imgn.mtg.tooling.scryfall.moshi;

import java.io.IOException;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.tooling.scryfall.model.ScryfallSet;

/// Manual JsonAdapter for [ScryfallSet] to avoid reflection.
public final class ScryfallSetAdapter extends JsonAdapter<ScryfallSet> {

    private static final JsonReader.Options OPTIONS = JsonReader.Options.of(
            "id",
            "code",
            "name",
            "set_type",
            "released_at",
            "block_code",
            "block",
            "parent_set_code",
            "card_count",
            "icon_svg_uri");

    @Override
    public ScryfallSet fromJson(JsonReader reader) throws IOException {
        String id = null;
        String code = null;
        String name = null;
        String setType = null;
        @Nullable String releasedAt = null;
        @Nullable String blockCode = null;
        @Nullable String block = null;
        @Nullable String parentSetCode = null;
        int cardCount = 0;
        String iconSvgUri = null;

        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.selectName(OPTIONS)) {
                case 0 -> id = reader.nextString();
                case 1 -> code = reader.nextString();
                case 2 -> name = reader.nextString();
                case 3 -> setType = reader.nextString();
                case 4 -> releasedAt = reader.peek() == JsonReader.Token.NULL ? reader.nextNull() : reader.nextString();
                case 5 -> blockCode = reader.peek() == JsonReader.Token.NULL ? reader.nextNull() : reader.nextString();
                case 6 -> block = reader.peek() == JsonReader.Token.NULL ? reader.nextNull() : reader.nextString();
                case 7 ->
                    parentSetCode = reader.peek() == JsonReader.Token.NULL ? reader.nextNull() : reader.nextString();
                case 8 -> cardCount = reader.nextInt();
                case 9 -> iconSvgUri = reader.nextString();
                default -> {
                    reader.skipName();
                    reader.skipValue();
                }
            }
        }
        reader.endObject();

        if (id == null || code == null || name == null || setType == null || iconSvgUri == null) {
            throw new JsonDataException("Required field missing");
        }

        return new ScryfallSet(
                id, code, name, setType, releasedAt, blockCode, block, parentSetCode, cardCount, iconSvgUri);
    }

    @Override
    public void toJson(JsonWriter writer, @Nullable ScryfallSet value) throws IOException {
        if (value == null) {
            writer.nullValue();
            return;
        }
        writer.beginObject();
        writer.name("id").value(value.id());
        writer.name("code").value(value.code());
        writer.name("name").value(value.name());
        writer.name("set_type").value(value.setType());
        writer.name("released_at").value(value.releasedAt());
        writer.name("block_code").value(value.blockCode());
        writer.name("block").value(value.block());
        writer.name("parent_set_code").value(value.parentSetCode());
        writer.name("card_count").value(value.cardCount());
        writer.name("icon_svg_uri").value(value.iconSvgUri());
        writer.endObject();
    }
}
