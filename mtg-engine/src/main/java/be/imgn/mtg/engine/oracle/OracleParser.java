package be.imgn.mtg.engine.oracle;

import static be.imgn.mtg.engine.oracle.Words.w;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.ArrayList;
import java.util.List;

import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

/// Entry point for parsing MTG oracle text using dot-parse combinators.
public final class OracleParser {
    private OracleParser() {}

    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    // ── Triggered ability ──────────────────────────────────────────────

    /// Trigger event text: words (including "~" self-reference) up to the comma separator.
    private static final Parser<String> EVENT_WORD = anyOf(string("~"), word());

    private static final Parser<String> EVENT_TEXT = EVENT_WORD.atLeastOnce().map(words -> String.join(" ", words));

    static final Parser<Ability> TRIGGERED = sequence(
            anyOf(w("when"), w("whenever"), w("at")),
            EVENT_TEXT.followedBy(string(",")),
            EffectParsers.EFFECT.atLeastOnce(),
            (trigger, event, effects) -> new Ability.Triggered(trigger, event, null, effects));

    // ── Activated ability: cost : effects ───────────────────────────────

    static final Parser<Ability> ACTIVATED = sequence(
            CostParsers.COST_EXPRESSION.followedBy(string(":")),
            EffectParsers.EFFECT.atLeastOnce(),
            Ability.Activated::new);

    // ── Spell ability: just effects ────────────────────────────────────

    static final Parser<Ability> SPELL = EffectParsers.EFFECT.atLeastOnce().map(Ability.Spell::new);

    /// Parse oracle text into a list of abilities.
    /// Each line (paragraph) is parsed as a separate ability.
    public static List<Ability> parse(String cardName, String oracleText) {
        if (oracleText == null || oracleText.isBlank()) return List.of();

        // Replace card name with ~ for self-reference
        var normalized = oracleText.replace(cardName, "~");
        var paragraphs = normalized.split("\\n");
        var abilities = new ArrayList<Ability>();

        for (var paragraph : paragraphs) {
            var trimmed = paragraph.trim();
            if (trimmed.isEmpty()) continue;

            abilities.add(tryParse(trimmed));
        }
        return List.copyOf(abilities);
    }

    private static Ability tryParse(String text) {
        // Strip trailing sentence-ending punctuation before parsing
        var input = text.endsWith(".") || text.endsWith("?")
                ? text.substring(0, text.length() - 1).stripTrailing()
                : text;

        // Try activated ability (contains ":")
        if (input.contains(":")) {
            try {
                return ACTIVATED.parseSkipping(WHITESPACE, input);
            } catch (Exception e) {
                /* fall through */
            }
        }

        // Try triggered ability (starts with when/whenever/at)
        var lower = input.toLowerCase();
        if (lower.startsWith("when") || lower.startsWith("at ")) {
            try {
                return TRIGGERED.parseSkipping(WHITESPACE, input);
            } catch (Exception e) {
                /* fall through */
            }
        }

        // Try spell ability
        try {
            return SPELL.parseSkipping(WHITESPACE, input);
        } catch (Exception e) {
            /* fall through */
        }

        // Fallback: keyword ability
        return new Ability.Keyword(text, null);
    }
}
