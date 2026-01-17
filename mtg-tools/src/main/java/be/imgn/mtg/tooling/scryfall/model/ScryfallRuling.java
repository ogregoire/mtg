package be.imgn.mtg.tooling.scryfall.model;

/// Represents a ruling from the Scryfall API.
public record ScryfallRuling(String oracleId, String source, String publishedAt, String comment) {}
