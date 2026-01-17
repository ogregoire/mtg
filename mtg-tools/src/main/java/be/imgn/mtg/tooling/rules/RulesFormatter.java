package be.imgn.mtg.tooling.rules;

import java.util.List;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.tooling.rules.model.GlossaryResult;
import be.imgn.mtg.tooling.rules.model.RuleResult;
import be.imgn.mtg.tooling.rules.model.RuleVersionResult;
import be.imgn.mtg.tooling.rules.model.SearchResult;

/// Formats rules information for console output.
public final class RulesFormatter {

    private static final int MAX_TEXT_LENGTH = 100;

    private RulesFormatter() {}

    /// Formats rules version information.
    ///
    /// @param version the version info
    /// @return formatted version string
    public static String formatVersion(RuleVersionResult version) {
        var sb = new StringBuilder();
        sb.append("MTG Comprehensive Rules\n\n");
        sb.append("Effective: ")
                .append(formatDate(version.effectiveDate().toString()))
                .append("\n");
        sb.append("Downloaded: ").append(version.downloadedAt().toLocalDate()).append("\n");
        sb.append("Source: ").append(version.sourceUrl()).append("\n");
        return sb.toString();
    }

    /// Formats a list of rules (e.g., a rule and its sub-rules).
    ///
    /// @param rules the rules to format
    /// @param showCardHint whether to show the card search hint
    /// @param keyword the keyword being looked up (for the hint), or null
    /// @return formatted rules string
    public static String formatRules(List<RuleResult> rules, boolean showCardHint, @Nullable String keyword) {
        var sb = new StringBuilder();

        for (var rule : rules) {
            sb.append(rule.ruleNumber()).append(". ").append(rule.text()).append("\n");
        }

        if (showCardHint && keyword != null && !keyword.isEmpty()) {
            sb.append("\nTo see cards with ")
                    .append(keyword.toLowerCase())
                    .append(", run: ./mtg card --oracle \"*")
                    .append(keyword.toLowerCase())
                    .append("*\"\n");
        }

        return sb.toString();
    }

    /// Formats a list of rules with extended information (cross-references).
    ///
    /// @param rules the main rules
    /// @param crossReferences rules that reference these rules
    /// @param keyword the keyword being looked up, or null
    /// @return formatted rules string with cross-references
    public static String formatRulesExtended(
            List<RuleResult> rules, List<RuleResult> crossReferences, @Nullable String keyword) {
        var sb = new StringBuilder();

        // Main rules
        for (var rule : rules) {
            sb.append(rule.ruleNumber()).append(". ").append(rule.text()).append("\n");
        }

        // Cross-references
        if (!crossReferences.isEmpty()) {
            sb.append("\nCross-references:\n");
            for (var ref : crossReferences) {
                var section = ref.section() != null ? ref.section() : "Unknown Section";
                sb.append("  ").append(ref.ruleNumber());
                sb.append(" (").append(section).append(")");
                sb.append(" - ./mtg rules ").append(ref.ruleNumber()).append("\n");
            }
        }

        // Card hint
        if (keyword != null && !keyword.isEmpty()) {
            sb.append("\nTo see cards with ")
                    .append(keyword.toLowerCase())
                    .append(", run: ./mtg card --oracle \"*")
                    .append(keyword.toLowerCase())
                    .append("*\"\n");
        }

        return sb.toString();
    }

    /// Formats a single rule.
    ///
    /// @param rule the rule to format
    /// @return formatted rule string
    public static String formatRule(RuleResult rule) {
        return rule.ruleNumber() + ". " + rule.text() + "\n";
    }

    /// Formats search results.
    ///
    /// @param query the search query
    /// @param results the search results
    /// @return formatted search results string
    public static String formatSearchResults(String query, List<SearchResult> results) {
        var sb = new StringBuilder();

        sb.append("Search results for \"").append(query).append("\":\n\n");

        if (results.isEmpty()) {
            sb.append("No results found.\n");
            return sb.toString();
        }

        for (var result : results) {
            var section = result.section() != null ? result.section() : "Unknown";
            sb.append(result.ruleNumber()).append(" [").append(section).append("]\n");

            // Truncate text if too long and highlight query
            var text = result.text();
            if (text.length() > MAX_TEXT_LENGTH) {
                // Try to show context around the query
                var queryIndex = text.toLowerCase().indexOf(query.toLowerCase());
                if (queryIndex >= 0) {
                    var start = Math.max(0, queryIndex - 30);
                    var end = Math.min(text.length(), queryIndex + query.length() + 60);
                    text = (start > 0 ? "..." : "")
                            + text.substring(start, end)
                            + (end < result.text().length() ? "..." : "");
                } else {
                    text = text.substring(0, MAX_TEXT_LENGTH) + "...";
                }
            }
            sb.append("  ").append(text).append("\n");
            sb.append("  -> ./mtg rules ").append(result.ruleNumber()).append("\n\n");
        }

        return sb.toString();
    }

    /// Formats a glossary entry.
    ///
    /// @param entry the glossary entry
    /// @return formatted glossary string
    public static String formatGlossary(GlossaryResult entry) {
        var sb = new StringBuilder();
        sb.append(entry.term()).append("\n");
        sb.append(entry.definition()).append("\n");
        return sb.toString();
    }

    /// Formats glossary search results.
    ///
    /// @param query the search query
    /// @param results the glossary results
    /// @return formatted glossary results string
    public static String formatGlossaryResults(String query, List<GlossaryResult> results) {
        var sb = new StringBuilder();

        sb.append("Glossary results for \"").append(query).append("\":\n\n");

        if (results.isEmpty()) {
            sb.append("No glossary entries found.\n");
            return sb.toString();
        }

        for (var result : results) {
            sb.append(result.term()).append("\n");
            // Truncate definition if too long
            var definition = result.definition();
            if (definition.length() > MAX_TEXT_LENGTH * 2) {
                definition = definition.substring(0, MAX_TEXT_LENGTH * 2) + "...";
            }
            sb.append("  ").append(definition).append("\n\n");
        }

        return sb.toString();
    }

    /// Formats a full section with all rules.
    ///
    /// @param rules the rules in the section
    /// @return formatted section with all rules
    public static String formatSection(List<RuleResult> rules) {
        if (rules.isEmpty()) {
            return "No rules found.\n";
        }

        var sb = new StringBuilder();

        // Find the section header (rule without a dot)
        var sectionHeader = rules.stream()
                .filter(r -> !r.ruleNumber().contains("."))
                .findFirst()
                .orElse(null);

        if (sectionHeader != null) {
            sb.append(sectionHeader.ruleNumber())
                    .append(". ")
                    .append(sectionHeader.text())
                    .append("\n");

            // Count sub-rules
            var subRuleCount = rules.size() - 1;
            if (subRuleCount > 0) {
                sb.append("(").append(subRuleCount).append(" rules in this section)\n");
            }
            sb.append("\n");
        }

        // Show all rules (skip the section header we already showed)
        for (var rule : rules) {
            if (sectionHeader != null && rule.ruleNumber().equals(sectionHeader.ruleNumber())) {
                continue;
            }
            sb.append(rule.ruleNumber()).append(". ").append(rule.text()).append("\n");
        }

        return sb.toString();
    }

    /// Formats a section overview (just the section header and first-level rules).
    ///
    /// @param rules the rules in the section
    /// @return formatted section overview
    public static String formatSectionOverview(List<RuleResult> rules) {
        var sb = new StringBuilder();

        for (var rule : rules) {
            // Only show section headers and first-level rules (e.g., 702.1, not 702.1a)
            var ruleNumber = rule.ruleNumber();
            if (!ruleNumber.contains(".") || !ruleNumber.matches(".*\\d[a-z]+$")) {
                sb.append(ruleNumber).append(". ").append(rule.text()).append("\n");
            }
        }

        return sb.toString();
    }

    private static String formatDate(String date) {
        // Convert "2026-01-10" to "January 10, 2026"
        var parts = date.split("-");
        if (parts.length != 3) {
            return date;
        }
        var months = new String[] {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        };
        var month = Integer.parseInt(parts[1]);
        var day = Integer.parseInt(parts[2]);
        var year = parts[0];
        return months[month - 1] + " " + day + ", " + year;
    }
}
