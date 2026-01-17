package be.imgn.mtg.tooling.scryfall.model;

import org.jspecify.annotations.Nullable;

/// Represents a set from the Scryfall API.
public record ScryfallSet(
        String id,
        String code,
        String name,
        String setType,
        @Nullable String releasedAt,
        @Nullable String blockCode,
        @Nullable String block,
        @Nullable String parentSetCode,
        int cardCount,
        String iconSvgUri) {}
