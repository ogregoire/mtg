package be.imgn.mtg.tooling.card.model;

/// Represents a card's print in a specific set.
///
/// @param setName the set name (e.g., "Double Masters 2022")
/// @param setCode the set code (e.g., "2X2")
/// @param collectorNumber the collector number within the set
public record CardPrintResult(String setName, String setCode, String collectorNumber) {}
