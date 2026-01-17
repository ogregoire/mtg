package be.imgn.mtg.tooling.rules;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.tooling.rules.model.ParsedRules;
import be.imgn.mtg.tooling.rules.model.ParsedRules.ParsedGlossaryEntry;
import be.imgn.mtg.tooling.rules.model.ParsedRules.ParsedRule;

/// Parser for the MTG Comprehensive Rules TXT file format.
///
/// The CR file has the following structure:
/// ```
/// Magic: The Gathering Comprehensive Rules
/// These rules are effective as of January 10, 2026.
///
/// Contents
/// 1. Game Concepts
/// ...
///
/// 1. Game Concepts
/// 100. General
/// 100.1. These Magic rules apply...
/// 100.1a A two-player game is...
///
/// Glossary
/// Mana Value
/// A number that represents...
///
/// Credits
/// ```
public final class RulesParser {

    private static final Pattern EFFECTIVE_DATE_PATTERN =
            Pattern.compile("These rules are effective as of (\\w+ \\d+, \\d{4})\\.");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US);

    // Matches section headers like "100. General" or "702. Keyword Abilities"
    private static final Pattern SECTION_HEADER_PATTERN = Pattern.compile("^(\\d{3})\\. (.+)$");

    // Matches rules like "100.1. Text" (with dot) or "100.1a Text" (without dot for lettered sub-rules)
    private static final Pattern RULE_PATTERN = Pattern.compile("^(\\d{3}\\.\\d+[a-z]*)(?:\\.)?\\s+(.+)$");

    // Matches keyword ability headers in section 702 like "702.9. Flying"
    private static final Pattern KEYWORD_HEADER_PATTERN = Pattern.compile("^(702\\.\\d+)\\. (\\w+(?:\\s+\\w+)?)$");

    private RulesParser() {}

    /// Parses the Comprehensive Rules from text content.
    ///
    /// @param content the full text content of the CR file
    /// @return the parsed rules
    /// @throws IllegalArgumentException if the content cannot be parsed
    public static ParsedRules parse(String content) {
        var lines = content.lines().toList();
        var effectiveDate = parseEffectiveDate(lines);

        // Find where the actual rules start (after the Contents section)
        var rulesStartIndex = findRulesStart(lines);
        // Find where the glossary starts
        var glossaryStartIndex = findGlossaryStart(lines);
        // Find where credits start (end of glossary)
        var creditsStartIndex = findCreditsStart(lines);

        var rules = parseRules(lines, rulesStartIndex, glossaryStartIndex);
        var glossary = parseGlossary(lines, glossaryStartIndex, creditsStartIndex);
        var keywords = extractKeywords(rules);

        return new ParsedRules(effectiveDate, rules, glossary, keywords);
    }

    private static LocalDate parseEffectiveDate(List<String> lines) {
        for (var line : lines) {
            var matcher = EFFECTIVE_DATE_PATTERN.matcher(line);
            if (matcher.find()) {
                return LocalDate.parse(matcher.group(1), DATE_FORMATTER);
            }
        }
        throw new IllegalArgumentException("Could not find effective date in rules document");
    }

    private static int findRulesStart(List<String> lines) {
        // The Contents section lists only section headers like "100. General"
        // The actual rules section has sub-rules like "100.1. ..." or "100.1a ..."
        // We find the first sub-rule and back up to find its section header

        // Pattern for a sub-rule (e.g., "100.1. " or "702.9a ")
        var subRulePattern = Pattern.compile("^\\d{3}\\.\\d+[a-z]*(?:\\.)?\\s+");

        // Find the first sub-rule
        int firstSubRuleIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i).trim();
            if (subRulePattern.matcher(line).find()) {
                firstSubRuleIndex = i;
                break;
            }
        }

        if (firstSubRuleIndex == -1) {
            throw new IllegalArgumentException("Could not find any sub-rules in document");
        }

        // Back up to find the section header (e.g., "100. General")
        for (int i = firstSubRuleIndex - 1; i >= 0; i--) {
            var line = lines.get(i).trim();
            if (line.matches("^\\d{3}\\. .+$")) {
                return i;
            }
        }

        // If no section header found, start from the first sub-rule
        return firstSubRuleIndex;
    }

    private static int findGlossaryStart(List<String> lines) {
        // Find the LAST "Glossary" line - the actual glossary section, not the one in Contents
        int lastGlossaryIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            if ("Glossary".equals(lines.get(i).trim())) {
                lastGlossaryIndex = i;
            }
        }
        if (lastGlossaryIndex == -1) {
            throw new IllegalArgumentException("Could not find Glossary section in document");
        }
        return lastGlossaryIndex;
    }

    private static int findCreditsStart(List<String> lines) {
        // Find the LAST "Credits" line - the actual credits section, not the one in Contents
        int lastCreditsIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            if ("Credits".equals(lines.get(i).trim())) {
                lastCreditsIndex = i;
            }
        }
        return lastCreditsIndex != -1 ? lastCreditsIndex : lines.size();
    }

    private static List<ParsedRule> parseRules(List<String> lines, int startIndex, int endIndex) {
        var rules = new ArrayList<ParsedRule>();
        String currentSection = null;
        String currentSectionNumber = null;
        var sectionNames = new HashMap<String, String>();

        for (int i = startIndex; i < endIndex; i++) {
            var line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }

            // Check for section header
            var sectionMatcher = SECTION_HEADER_PATTERN.matcher(line);
            if (sectionMatcher.matches()) {
                currentSectionNumber = sectionMatcher.group(1);
                currentSection = sectionMatcher.group(2);
                sectionNames.put(currentSectionNumber, currentSection);

                // Add the section header as a rule entry
                rules.add(new ParsedRule(
                        currentSectionNumber, currentSection, null, currentSection, currentSectionNumber));
                continue;
            }

            // Check for rule
            var ruleMatcher = RULE_PATTERN.matcher(line);
            if (ruleMatcher.matches()) {
                var ruleNumber = ruleMatcher.group(1);
                var text = ruleMatcher.group(2);
                var parentRule = computeParentRule(ruleNumber);
                var sectionNum = ruleNumber.split("\\.")[0];

                // Get section name from the stored map, or use current
                var section = sectionNames.getOrDefault(sectionNum, currentSection);

                rules.add(new ParsedRule(ruleNumber, text, parentRule, section, sectionNum));
            }
        }

        return rules;
    }

    private static @Nullable String computeParentRule(String ruleNumber) {
        // Rule "702.9a" -> parent "702.9"
        // Rule "702.9" -> parent "702"
        // Rule "702" -> parent null (but we return empty string for non-null field)

        if (ruleNumber.matches("\\d{3}\\.\\d+[a-z]+")) {
            // Remove the letter suffix
            return ruleNumber.replaceAll("[a-z]+$", "");
        } else if (ruleNumber.matches("\\d{3}\\.\\d+")) {
            // Return section number
            return ruleNumber.split("\\.")[0];
        }
        return null;
    }

    private static List<ParsedGlossaryEntry> parseGlossary(List<String> lines, int startIndex, int endIndex) {
        var entries = new ArrayList<ParsedGlossaryEntry>();

        // Skip "Glossary" header and empty lines
        int i = startIndex + 1;
        while (i < endIndex && lines.get(i).trim().isEmpty()) {
            i++;
        }

        String currentTerm = null;
        var definitionBuilder = new StringBuilder();

        while (i < endIndex) {
            var line = lines.get(i).trim();

            if (line.isEmpty()) {
                // Empty line might signal a new entry
                if (currentTerm != null && !definitionBuilder.isEmpty()) {
                    entries.add(new ParsedGlossaryEntry(
                            currentTerm, definitionBuilder.toString().trim()));
                    currentTerm = null;
                    definitionBuilder.setLength(0);
                }
            } else if (currentTerm == null) {
                // This line is a term
                currentTerm = line;
            } else {
                // This line is part of the definition
                if (!definitionBuilder.isEmpty()) {
                    definitionBuilder.append(" ");
                }
                definitionBuilder.append(line);
            }
            i++;
        }

        // Add final entry if any
        if (currentTerm != null && !definitionBuilder.isEmpty()) {
            entries.add(new ParsedGlossaryEntry(
                    currentTerm, definitionBuilder.toString().trim()));
        }

        return entries;
    }

    private static Map<String, String> extractKeywords(List<ParsedRule> rules) {
        var keywords = new LinkedHashMap<String, String>();

        for (var rule : rules) {
            var ruleNumber = rule.ruleNumber();
            // Look for keyword headers in section 702 (e.g., "702.9. Flying")
            if (ruleNumber.startsWith("702.") && !ruleNumber.contains("a") && !ruleNumber.contains("b")) {
                var matcher = KEYWORD_HEADER_PATTERN.matcher(ruleNumber + ". " + rule.text());
                if (matcher.matches()) {
                    var keyword = matcher.group(2).toLowerCase();
                    keywords.put(keyword, ruleNumber);
                }
            }
        }

        return keywords;
    }
}
