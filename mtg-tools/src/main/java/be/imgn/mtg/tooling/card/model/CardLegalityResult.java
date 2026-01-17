package be.imgn.mtg.tooling.card.model;

/// Represents a card's legality in a specific format.
///
/// @param formatName the format name (e.g., "standard", "modern", "legacy")
/// @param legality the legality status (e.g., "legal", "banned", "restricted", "not_legal")
public record CardLegalityResult(String formatName, String legality) {}
