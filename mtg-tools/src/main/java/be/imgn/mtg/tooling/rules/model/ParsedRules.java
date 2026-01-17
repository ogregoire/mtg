package be.imgn.mtg.tooling.rules.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/// Represents the parsed content from the Comprehensive Rules document.
///
/// @param effectiveDate the effective date of the rules
/// @param rules all rules indexed by rule number
/// @param glossary all glossary entries indexed by term
/// @param keywords keyword abilities mapped to their main rule number
public record ParsedRules(
        LocalDate effectiveDate,
        List<ParsedRule> rules,
        List<ParsedGlossaryEntry> glossary,
        Map<String, String> keywords) {

    /// A single parsed rule.
    ///
    /// @param ruleNumber the rule number (e.g., "702.9", "702.9a")
    /// @param text the rule text
    /// @param parentRule the parent rule number (nullable for section headers)
    /// @param section the section name
    /// @param sectionNumber the section number
    public record ParsedRule(
            String ruleNumber, String text, @Nullable String parentRule, String section, String sectionNumber) {}

    /// A single parsed glossary entry.
    ///
    /// @param term the glossary term
    /// @param definition the definition text
    public record ParsedGlossaryEntry(String term, String definition) {}
}
