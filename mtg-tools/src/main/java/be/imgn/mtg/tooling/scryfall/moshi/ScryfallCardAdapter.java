package be.imgn.mtg.tooling.scryfall.moshi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.tooling.scryfall.model.ScryfallCard;
import be.imgn.mtg.tooling.scryfall.model.ScryfallCard.ScryfallCardFace;

/// Manual JsonAdapter for [ScryfallCard] to avoid reflection.
public final class ScryfallCardAdapter extends JsonAdapter<ScryfallCard> {

    private static final JsonReader.Options OPTIONS = JsonReader.Options.of(
            "id",
            "oracle_id",
            "name",
            "layout",
            "mana_cost",
            "cmc",
            "type_line",
            "oracle_text",
            "colors",
            "color_identity",
            "color_indicator",
            "power",
            "toughness",
            "loyalty",
            "defense",
            "hand_modifier",
            "life_modifier",
            "keywords",
            "legalities",
            "card_faces",
            "set",
            "collector_number",
            "rarity");

    private static final JsonReader.Options FACE_OPTIONS = JsonReader.Options.of(
            "name",
            "mana_cost",
            "type_line",
            "oracle_text",
            "colors",
            "color_indicator",
            "power",
            "toughness",
            "loyalty",
            "defense",
            "cmc");

    @Override
    public ScryfallCard fromJson(JsonReader reader) throws IOException {
        String id = null;
        @Nullable String oracleId = null;
        String name = null;
        String layout = null;
        @Nullable String manaCost = null;
        double cmc = 0;
        @Nullable String typeLine = null;
        @Nullable String oracleText = null;
        @Nullable List<String> colors = null;
        @Nullable List<String> colorIdentity = null;
        @Nullable List<String> colorIndicator = null;
        @Nullable String power = null;
        @Nullable String toughness = null;
        @Nullable String loyalty = null;
        @Nullable String defense = null;
        @Nullable String handModifier = null;
        @Nullable String lifeModifier = null;
        @Nullable List<String> keywords = null;
        Map<String, String> legalities = new LinkedHashMap<>();
        @Nullable List<ScryfallCardFace> cardFaces = null;
        @Nullable String setCode = null;
        @Nullable String collectorNumber = null;
        @Nullable String rarity = null;

        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.selectName(OPTIONS)) {
                case 0 -> id = reader.nextString();
                case 1 -> oracleId = readNullableString(reader);
                case 2 -> name = reader.nextString();
                case 3 -> layout = reader.nextString();
                case 4 -> manaCost = readNullableString(reader);
                case 5 -> cmc = reader.nextDouble();
                case 6 -> typeLine = readNullableString(reader);
                case 7 -> oracleText = readNullableString(reader);
                case 8 -> colors = readNullableStringList(reader);
                case 9 -> colorIdentity = readNullableStringList(reader);
                case 10 -> colorIndicator = readNullableStringList(reader);
                case 11 -> power = readNullableString(reader);
                case 12 -> toughness = readNullableString(reader);
                case 13 -> loyalty = readNullableString(reader);
                case 14 -> defense = readNullableString(reader);
                case 15 -> handModifier = readNullableString(reader);
                case 16 -> lifeModifier = readNullableString(reader);
                case 17 -> keywords = readNullableStringList(reader);
                case 18 -> legalities = readStringMap(reader);
                case 19 -> cardFaces = readCardFaces(reader);
                case 20 -> setCode = readNullableString(reader);
                case 21 -> collectorNumber = readNullableString(reader);
                case 22 -> rarity = readNullableString(reader);
                default -> {
                    reader.skipName();
                    reader.skipValue();
                }
            }
        }
        reader.endObject();

        if (id == null || name == null || layout == null) {
            throw new JsonDataException("Required field missing");
        }

        return new ScryfallCard(
                id,
                oracleId,
                name,
                layout,
                manaCost,
                cmc,
                typeLine,
                oracleText,
                colors,
                colorIdentity,
                colorIndicator,
                power,
                toughness,
                loyalty,
                defense,
                handModifier,
                lifeModifier,
                keywords,
                legalities,
                cardFaces,
                setCode,
                collectorNumber,
                rarity);
    }

    private @Nullable String readNullableString(JsonReader reader) throws IOException {
        if (reader.peek() == JsonReader.Token.NULL) {
            return reader.nextNull();
        }
        return reader.nextString();
    }

    private @Nullable List<String> readNullableStringList(JsonReader reader) throws IOException {
        if (reader.peek() == JsonReader.Token.NULL) {
            return reader.nextNull();
        }
        var list = new ArrayList<String>();
        reader.beginArray();
        while (reader.hasNext()) {
            list.add(reader.nextString());
        }
        reader.endArray();
        return list;
    }

    private Map<String, String> readStringMap(JsonReader reader) throws IOException {
        var map = new LinkedHashMap<String, String>();
        reader.beginObject();
        while (reader.hasNext()) {
            var key = reader.nextName();
            var value = reader.nextString();
            map.put(key, value);
        }
        reader.endObject();
        return map;
    }

    private @Nullable List<ScryfallCardFace> readCardFaces(JsonReader reader) throws IOException {
        if (reader.peek() == JsonReader.Token.NULL) {
            return reader.nextNull();
        }
        var faces = new ArrayList<ScryfallCardFace>();
        reader.beginArray();
        while (reader.hasNext()) {
            faces.add(readCardFace(reader));
        }
        reader.endArray();
        return faces;
    }

    private ScryfallCardFace readCardFace(JsonReader reader) throws IOException {
        String name = null;
        @Nullable String manaCost = null;
        @Nullable String typeLine = null;
        @Nullable String oracleText = null;
        @Nullable List<String> colors = null;
        @Nullable List<String> colorIndicator = null;
        @Nullable String power = null;
        @Nullable String toughness = null;
        @Nullable String loyalty = null;
        @Nullable String defense = null;
        double cmc = 0;

        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.selectName(FACE_OPTIONS)) {
                case 0 -> name = reader.nextString();
                case 1 -> manaCost = readNullableString(reader);
                case 2 -> typeLine = readNullableString(reader);
                case 3 -> oracleText = readNullableString(reader);
                case 4 -> colors = readNullableStringList(reader);
                case 5 -> colorIndicator = readNullableStringList(reader);
                case 6 -> power = readNullableString(reader);
                case 7 -> toughness = readNullableString(reader);
                case 8 -> loyalty = readNullableString(reader);
                case 9 -> defense = readNullableString(reader);
                case 10 -> cmc = reader.nextDouble();
                default -> {
                    reader.skipName();
                    reader.skipValue();
                }
            }
        }
        reader.endObject();

        if (name == null) {
            throw new JsonDataException("Required field 'name' missing in card face");
        }

        return new ScryfallCardFace(
                name, manaCost, typeLine, oracleText, colors, colorIndicator, power, toughness, loyalty, defense, cmc);
    }

    @Override
    public void toJson(JsonWriter writer, @Nullable ScryfallCard value) throws IOException {
        if (value == null) {
            writer.nullValue();
            return;
        }
        writer.beginObject();
        writer.name("id").value(value.id());
        writer.name("oracle_id").value(value.oracleId());
        writer.name("name").value(value.name());
        writer.name("layout").value(value.layout());
        writer.name("mana_cost").value(value.manaCost());
        writer.name("cmc").value(value.cmc());
        writer.name("type_line").value(value.typeLine());
        writer.name("oracle_text").value(value.oracleText());
        writeStringList(writer, "colors", value.colors());
        writeStringList(writer, "color_identity", value.colorIdentity());
        writeStringList(writer, "color_indicator", value.colorIndicator());
        writer.name("power").value(value.power());
        writer.name("toughness").value(value.toughness());
        writer.name("loyalty").value(value.loyalty());
        writer.name("defense").value(value.defense());
        writer.name("hand_modifier").value(value.handModifier());
        writer.name("life_modifier").value(value.lifeModifier());
        writeStringList(writer, "keywords", value.keywords());
        writeStringMap(writer, "legalities", value.legalities());
        writeCardFaces(writer, value.cardFaces());
        writer.name("set").value(value.setCode());
        writer.name("collector_number").value(value.collectorNumber());
        writer.name("rarity").value(value.rarity());
        writer.endObject();
    }

    private void writeStringList(JsonWriter writer, String name, @Nullable List<String> list) throws IOException {
        writer.name(name);
        if (list == null) {
            writer.nullValue();
            return;
        }
        writer.beginArray();
        for (var item : list) {
            writer.value(item);
        }
        writer.endArray();
    }

    private void writeStringMap(JsonWriter writer, String name, Map<String, String> map) throws IOException {
        writer.name(name);
        writer.beginObject();
        for (var entry : map.entrySet()) {
            writer.name(entry.getKey()).value(entry.getValue());
        }
        writer.endObject();
    }

    private void writeCardFaces(JsonWriter writer, @Nullable List<ScryfallCardFace> faces) throws IOException {
        writer.name("card_faces");
        if (faces == null) {
            writer.nullValue();
            return;
        }
        writer.beginArray();
        for (var face : faces) {
            writeCardFace(writer, face);
        }
        writer.endArray();
    }

    private void writeCardFace(JsonWriter writer, ScryfallCardFace face) throws IOException {
        writer.beginObject();
        writer.name("name").value(face.name());
        writer.name("mana_cost").value(face.manaCost());
        writer.name("type_line").value(face.typeLine());
        writer.name("oracle_text").value(face.oracleText());
        writeStringList(writer, "colors", face.colors());
        writeStringList(writer, "color_indicator", face.colorIndicator());
        writer.name("power").value(face.power());
        writer.name("toughness").value(face.toughness());
        writer.name("loyalty").value(face.loyalty());
        writer.name("defense").value(face.defense());
        writer.name("cmc").value(face.cmc());
        writer.endObject();
    }
}
