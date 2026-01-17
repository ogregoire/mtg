package be.imgn.mtg.tooling.card.model;

import java.time.LocalDate;

/// Represents a ruling for a card.
///
/// @param publishedAt the date the ruling was published
/// @param source the source of the ruling (e.g., "wotc", "scryfall")
/// @param comment the ruling text
public record CardRulingResult(LocalDate publishedAt, String source, String comment) {}
