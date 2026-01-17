package be.imgn.mtg.tooling.rules.model;

/// Represents a glossary entry from the Comprehensive Rules.
///
/// @param term the glossary term
/// @param definition the definition text
public record GlossaryResult(String term, String definition) {}
