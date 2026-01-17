package be.imgn.mtg.tooling.rules.model;

import org.jspecify.annotations.Nullable;

/// Represents a search result from the full-text search.
///
/// @param ruleNumber the rule number
/// @param text the rule text (may be truncated)
/// @param section the section name
/// @param score the search relevance score
public record SearchResult(
        String ruleNumber, String text, @Nullable String section, double score) {}
