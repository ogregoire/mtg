package be.imgn.mtg.engine.ability.internal.parser;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.ability.Ability;

/// Main entry point for parsing oracle text into abilities.
///
/// Splits oracle text by newlines and tries each parser in order:
/// 1. [ActivatedAbilityParser] — "Cost: Effect." format
/// 2. [SpellAbilityParser] — standalone spell effects
///
/// Lines that cannot be parsed (keywords, unsupported text) are silently skipped.
public final class AbilityParser {

    private AbilityParser() {}

    /// Parses oracle text into a list of abilities, replacing the card name with `~`.
    ///
    /// Self-references like "Lightning Bolt deals 3 damage" become "~ deals 3 damage"
    /// before parsing, so parsers can match the `~` placeholder instead of the card name.
    ///
    /// @param cardName   the name of the card (used for self-reference replacement), or null
    /// @param oracleText the full oracle text (may contain newlines)
    /// @return the parsed abilities, never null (may be empty)
    public static List<Ability> parse(@Nullable String cardName, @Nullable String oracleText) {
        if (oracleText == null || oracleText.isBlank()) {
            return List.of();
        }
        var text = cardName != null ? oracleText.replace(cardName, "~") : oracleText;
        return parseText(text);
    }

    /// Parses oracle text into a list of abilities.
    ///
    /// @param oracleText the full oracle text (may contain newlines)
    /// @return the parsed abilities, never null (may be empty)
    public static List<Ability> parse(@Nullable String oracleText) {
        if (oracleText == null || oracleText.isBlank()) {
            return List.of();
        }
        return parseText(oracleText);
    }

    private static List<Ability> parseText(String oracleText) {
        var abilities = new ArrayList<Ability>();
        for (var line : oracleText.split("\n")) {
            var trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            var ability = tryParseLine(trimmed);
            if (ability != null) {
                abilities.add(ability);
            }
        }
        return List.copyOf(abilities);
    }

    @SuppressWarnings("OverlyBroadCatchBlock")
    private static @Nullable Ability tryParseLine(String line) {
        try {
            return ActivatedAbilityParser.parse(line);
        } catch (Exception _) {
            // Not an activated ability
        }

        try {
            return SpellAbilityParser.parse(line);
        } catch (Exception _) {
            // Not a parseable effect
        }

        return null;
    }
}
