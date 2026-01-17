package be.imgn.mtg.tooling.rules.model;

import org.jspecify.annotations.Nullable;

/// Represents a rule from the Comprehensive Rules.
///
/// @param ruleNumber the rule number (e.g., "702.9", "702.9a")
/// @param text the rule text
/// @param parentRule the parent rule number (e.g., "702.9" for "702.9a")
/// @param section the section name (e.g., "Keyword Abilities")
/// @param sectionNumber the section number (e.g., "702")
public record RuleResult(
        String ruleNumber,
        String text,
        @Nullable String parentRule,
        @Nullable String section,
        @Nullable String sectionNumber) {}
