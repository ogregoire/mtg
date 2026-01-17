package be.imgn.mtg.tooling.scryfall.model;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/// Represents a card from the Scryfall API.
///
/// This maps the JSON structure returned by Scryfall's bulk data endpoint.
/// Only fields relevant for the game engine are included.
public record ScryfallCard(
        String id,
        @Nullable String oracleId,
        String name,
        String layout,
        @Nullable String manaCost,
        double cmc,
        @Nullable String typeLine,
        @Nullable String oracleText,
        @Nullable List<String> colors,
        @Nullable List<String> colorIdentity,
        @Nullable List<String> colorIndicator,
        @Nullable String power,
        @Nullable String toughness,
        @Nullable String loyalty,
        @Nullable String defense,
        @Nullable String handModifier,
        @Nullable String lifeModifier,
        @Nullable List<String> keywords,
        Map<String, String> legalities,
        @Nullable List<ScryfallCardFace> cardFaces,
        // Print-specific fields (for default_cards bulk data)
        @Nullable String setCode,
        @Nullable String collectorNumber,
        @Nullable String rarity) {

    /// Represents a face of a multi-faced card.
    public record ScryfallCardFace(
            String name,
            @Nullable String manaCost,
            @Nullable String typeLine,
            @Nullable String oracleText,
            @Nullable List<String> colors,
            @Nullable List<String> colorIndicator,
            @Nullable String power,
            @Nullable String toughness,
            @Nullable String loyalty,
            @Nullable String defense,
            double cmc) {}
}
