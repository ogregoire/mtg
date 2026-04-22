package be.imgn.mtg.engine.oracle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WordsTest {

    private static final CharPredicate SPACE = CharPredicate.is(' ');

    @Nested
    class Phrase {

        @Test
        void firstTokenCapitalizedIsCaseInsensitive() {
            var p = Words.phrase("Destroy creature");
            assertThat(p.parseSkipping(SPACE, "Destroy creature")).isEqualTo("Destroy creature");
            // The matched input's casing is preserved in the result.
            assertThat(p.parseSkipping(SPACE, "destroy creature")).isEqualTo("destroy creature");
        }

        @Test
        void firstTokenLowercaseIsCaseSensitive() {
            var p = Words.phrase("deal damage");
            assertThat(p.parseSkipping(SPACE, "deal damage")).isEqualTo("deal damage");
            assertThatThrownBy(() -> p.parseSkipping(SPACE, "Deal damage")).isInstanceOf(Exception.class);
        }

        @Test
        void nonFirstTokensAreCaseSensitive() {
            var p = Words.phrase("Destroy creature");
            // "Creature" mid-sentence never appears in oracle text.
            assertThatThrownBy(() -> p.parseSkipping(SPACE, "Destroy Creature")).isInstanceOf(Exception.class);
        }

        @Test
        void pluralSuffix() {
            var p = Words.phrase("Destroy creature(s)");
            assertThat(p.parseSkipping(SPACE, "Destroy creature")).isEqualTo("Destroy creature");
            assertThat(p.parseSkipping(SPACE, "Destroy creatures")).isEqualTo("Destroy creatures");
        }

        @Test
        void arbitrarySuffix() {
            var p = Words.phrase("witness(es)");
            assertThat(p.parseSkipping(SPACE, "witness")).isEqualTo("witness");
            assertThat(p.parseSkipping(SPACE, "witnesses")).isEqualTo("witnesses");

            var gerund = Words.phrase("play(ing)");
            assertThat(gerund.parseSkipping(SPACE, "play")).isEqualTo("play");
            assertThat(gerund.parseSkipping(SPACE, "playing")).isEqualTo("playing");
        }

        @Test
        void alternatives() {
            var p = Words.phrase("Target creature [is|are] blocked");
            assertThat(p.parseSkipping(SPACE, "Target creature is blocked")).isEqualTo("Target creature is blocked");
            assertThat(p.parseSkipping(SPACE, "Target creature are blocked")).isEqualTo("Target creature are blocked");
        }

        @Test
        void alternativesOfMoreThanTwo() {
            var p = Words.phrase("for each [white|blue|black|red|green]");
            for (var color : new String[] {"white", "blue", "black", "red", "green"}) {
                assertThat(p.parseSkipping(SPACE, "for each " + color)).isEqualTo("for each " + color);
            }
            assertThatThrownBy(() -> p.parseSkipping(SPACE, "for each yellow")).isInstanceOf(Exception.class);
        }

        @Test
        void multiWordBracket() {
            var p = Words.phrase("the top card [of your library]");
            assertThat(p.parseSkipping(SPACE, "the top card of your library"))
                    .isEqualTo("the top card of your library");
        }

        @Test
        void optionalWord() {
            var p = Words.phrase("deal(s) combat? damage");
            assertThat(p.parseSkipping(SPACE, "deal combat damage")).isEqualTo("deal combat damage");
            assertThat(p.parseSkipping(SPACE, "deal damage")).isEqualTo("deal damage");
        }

        @Test
        void optionalBracket() {
            var p = Words.phrase("the top card [of your library]?");
            assertThat(p.parseSkipping(SPACE, "the top card of your library"))
                    .isEqualTo("the top card of your library");
            assertThat(p.parseSkipping(SPACE, "the top card")).isEqualTo("the top card");
        }

        @Test
        void optionalAlternatives() {
            var p = Words.phrase("attack(s) [alone|first]?");
            assertThat(p.parseSkipping(SPACE, "attacks alone")).isEqualTo("attacks alone");
            assertThat(p.parseSkipping(SPACE, "attacks")).isEqualTo("attacks");
        }

        @Test
        void firstTokenCannotBeOptional() {
            assertThatThrownBy(() -> Words.phrase("also? attack"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot start with an optional token");
        }

        @Test
        void firstBracketTokenCannotBeOptional() {
            assertThatThrownBy(() -> Words.phrase("[at random]? destroy"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot start with an optional token");
        }

        @Test
        void unclosedBracketIsRejected() {
            assertThatThrownBy(() -> Words.phrase("Destroy [creature")).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void firstTokenAllCapsIsOnlyCaseInsensitiveByTitleConvention() {
            // "X" title == lower — single-arm match.
            var p = Words.phrase("X");
            assertThat(p.parseSkipping(SPACE, "X")).isEqualTo("X");
            // "x" is the lowercase form — accepted at sentence start, preserved in the result.
            assertThat(p.parseSkipping(SPACE, "x")).isEqualTo("x");
        }

        @Test
        void apostropheInToken() {
            var p = Words.phrase("can't attack");
            assertThat(p.parseSkipping(SPACE, "can't attack")).isEqualTo("can't attack");
            assertThatThrownBy(() -> p.parseSkipping(SPACE, "cant attack")).isInstanceOf(Exception.class);
        }

        @Test
        void hyphenInToken() {
            var p = Words.phrase("a non-creature card");
            assertThat(p.parseSkipping(SPACE, "a non-creature card")).isEqualTo("a non-creature card");
            var titled = Words.phrase("Non-creature card");
            assertThat(titled.parseSkipping(SPACE, "Non-creature card")).isEqualTo("Non-creature card");
            assertThat(titled.parseSkipping(SPACE, "non-creature card")).isEqualTo("non-creature card");
        }

        @Test
        void digitInToken() {
            var p = Words.phrase("deal 2 damage");
            assertThat(p.parseSkipping(SPACE, "deal 2 damage")).isEqualTo("deal 2 damage");
        }

        @Test
        void failsWhenInputMissesRequiredToken() {
            var p = Words.phrase("Destroy target creature");
            assertThatThrownBy(() -> p.parseSkipping(SPACE, "Destroy creature")).isInstanceOf(Exception.class);
        }

        @Test
        void bracketMultiWordAlternatives() {
            var p = Words.phrase("[until end of turn|this turn]");
            assertThat(p.parseSkipping(SPACE, "until end of turn")).isEqualTo("until end of turn");
            assertThat(p.parseSkipping(SPACE, "this turn")).isEqualTo("this turn");
            assertThatThrownBy(() -> p.parseSkipping(SPACE, "next turn")).isInstanceOf(Exception.class);
        }

        @Test
        void singleBracketAlternativeBehavesAsGroup() {
            var p = Words.phrase("[on the battlefield]");
            assertThat(p.parseSkipping(SPACE, "on the battlefield")).isEqualTo("on the battlefield");
        }

        @Test
        void comma() {
            var p = Words.phrase("destroy it, exile it");
            assertThat(p.parseSkipping(SPACE, "destroy it, exile it")).isEqualTo("destroy it, exile it");
            assertThatThrownBy(() -> p.parseSkipping(SPACE, "destroy it exile it"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        void period() {
            var p = Words.phrase("Destroy target creature.");
            assertThat(p.parseSkipping(SPACE, "Destroy target creature.")).isEqualTo("Destroy target creature.");
            assertThatThrownBy(() -> p.parseSkipping(SPACE, "Destroy target creature"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        void colon() {
            var p = Words.phrase("activate:");
            assertThat(p.parseSkipping(SPACE, "activate:")).isEqualTo("activate:");
        }

        @Test
        void mixedPunctuationWithAlternativesAndPlural() {
            var p = Words.phrase("can't attack, block, or crew");
            assertThat(p.parseSkipping(SPACE, "can't attack, block, or crew"))
                    .isEqualTo("can't attack, block, or crew");
        }

        @Test
        void consecutivePunctuation() {
            var p = Words.phrase("end.,:");
            assertThat(p.parseSkipping(SPACE, "end.,:")).isEqualTo("end.,:");
        }

        @Test
        void punctuationHasNoOptionalMark() {
            assertThatThrownBy(() -> Words.phrase(",?")).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void inflectionVariants() {
            var pastTense = Words.phrase("add(ed)");
            assertThat(pastTense.parseSkipping(SPACE, "add")).isEqualTo("add");
            assertThat(pastTense.parseSkipping(SPACE, "added")).isEqualTo("added");
            var plain = Words.phrase("card(s)");
            assertThat(plain.parseSkipping(SPACE, "card")).isEqualTo("card");
            assertThat(plain.parseSkipping(SPACE, "cards")).isEqualTo("cards");
        }

        @Test
        void inflectionFirstTokenRespectsTitleCase() {
            var p = Words.phrase("Target(s) creature");
            assertThat(p.parseSkipping(SPACE, "Target creature")).isEqualTo("Target creature");
            assertThat(p.parseSkipping(SPACE, "Targets creature")).isEqualTo("Targets creature");
            assertThat(p.parseSkipping(SPACE, "target creature")).isEqualTo("target creature");
            assertThat(p.parseSkipping(SPACE, "targets creature")).isEqualTo("targets creature");
        }

        @Test
        void bracketFirstTokenRespectsTitleCaseForAlternatives() {
            var p = Words.phrase("[Up|Down] for the count");
            assertThat(p.parseSkipping(SPACE, "Up for the count")).isEqualTo("Up for the count");
            assertThat(p.parseSkipping(SPACE, "up for the count")).isEqualTo("up for the count");
            assertThat(p.parseSkipping(SPACE, "Down for the count")).isEqualTo("Down for the count");
            assertThat(p.parseSkipping(SPACE, "down for the count")).isEqualTo("down for the count");
        }

        @Test
        void bracketLowercaseFirstTokenIsCaseSensitiveForAlternatives() {
            var p = Words.phrase("Roll [up|down] for the count");
            assertThat(p.parseSkipping(SPACE, "Roll up for the count")).isEqualTo("Roll up for the count");
            assertThatThrownBy(() -> p.parseSkipping(SPACE, "Roll Up for the count"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        void returnsMatchedText() {
            // Same template, two inputs → two different outputs.
            var p = Words.phrase("attack(s)");
            assertThat(p.parseSkipping(SPACE, "attack")).isEqualTo("attack");
            assertThat(p.parseSkipping(SPACE, "attacks")).isEqualTo("attacks");
        }

        @Test
        void whitespaceBetweenTokensNormalizesToSingleSpace() {
            var p = Words.phrase("Destroy target creature");
            assertThat(p.parseSkipping(SPACE, "Destroy   target   creature")).isEqualTo("Destroy target creature");
        }

        @Test
        void threeOrMoreOptionalTrailingTokens() {
            var p = Words.phrase("attack(s) also? this? turn?");
            assertThat(p.parseSkipping(SPACE, "attacks")).isEqualTo("attacks");
            assertThat(p.parseSkipping(SPACE, "attacks also")).isEqualTo("attacks also");
            assertThat(p.parseSkipping(SPACE, "attacks also this")).isEqualTo("attacks also this");
            assertThat(p.parseSkipping(SPACE, "attacks also this turn")).isEqualTo("attacks also this turn");
        }

        @Test
        void emptyTemplateRejected() {
            assertThatThrownBy(() -> Words.phrase("")).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void whitespaceOnlyTemplateRejected() {
            assertThatThrownBy(() -> Words.phrase("   ")).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void invalidTemplateCharactersRejected() {
            assertThatThrownBy(() -> Words.phrase("foo_bar")).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void unmatchedBracketCloseRejected() {
            assertThatThrownBy(() -> Words.phrase("destroy creature]")).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void errorMessageIncludesTemplate() {
            assertThatThrownBy(() -> Words.phrase("Destroy [creature"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Destroy [creature");
        }

        // ── Recursive brackets ─────────────────────────────────────────────

        @Test
        void nestedBrackets() {
            var p = Words.phrase("Destroy [each [artifact|creature]]");
            assertThat(p.parseSkipping(SPACE, "Destroy each artifact")).isEqualTo("Destroy each artifact");
            assertThat(p.parseSkipping(SPACE, "destroy each creature")).isEqualTo("destroy each creature");
            assertThatThrownBy(() -> p.parseSkipping(SPACE, "Destroy each enchantment"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        void inflectionInsideBracketAlt() {
            var p = Words.phrase("[deal(s) damage|gain(s) life]");
            assertThat(p.parseSkipping(SPACE, "deal damage")).isEqualTo("deal damage");
            assertThat(p.parseSkipping(SPACE, "deals damage")).isEqualTo("deals damage");
            assertThat(p.parseSkipping(SPACE, "gain life")).isEqualTo("gain life");
            assertThat(p.parseSkipping(SPACE, "gains life")).isEqualTo("gains life");
            assertThatThrownBy(() -> p.parseSkipping(SPACE, "deal life")).isInstanceOf(Exception.class);
        }

        @Test
        void punctuationInsideBracketAlt() {
            var p = Words.phrase("[destroy it, exile it|counter it]");
            assertThat(p.parseSkipping(SPACE, "destroy it, exile it")).isEqualTo("destroy it, exile it");
            assertThat(p.parseSkipping(SPACE, "counter it")).isEqualTo("counter it");
        }

        @Test
        void twoWordAltsAsFirstPositionBracket() {
            var p = Words.phrase("[Destroy|Exile] target creature");
            // First-position bracket: each alt's head gets title-or-lower.
            assertThat(p.parseSkipping(SPACE, "Destroy target creature")).isEqualTo("Destroy target creature");
            assertThat(p.parseSkipping(SPACE, "destroy target creature")).isEqualTo("destroy target creature");
            assertThat(p.parseSkipping(SPACE, "Exile target creature")).isEqualTo("Exile target creature");
            assertThat(p.parseSkipping(SPACE, "exile target creature")).isEqualTo("exile target creature");
        }

        @Test
        void optionalTokenInsideBracketAlt() {
            // "able" is optional inside the second alt — both forms should match.
            var p = Words.phrase("[cast this turn|play this turn if able?]");
            assertThat(p.parseSkipping(SPACE, "cast this turn")).isEqualTo("cast this turn");
            assertThat(p.parseSkipping(SPACE, "play this turn if able")).isEqualTo("play this turn if able");
            assertThat(p.parseSkipping(SPACE, "play this turn if")).isEqualTo("play this turn if");
        }

        @Test
        void optionalAsAltHeadThrows() {
            // The first alt's head is optional — same error as outer first-token-optional.
            assertThatThrownBy(() -> Words.phrase("[also? other|x]"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot start with an optional token");
        }

        @Test
        void multiWordBracketTightenedCaseRule() {
            // Under the recursive model, a first-position bracket whose alt head is
            // capitalized gives its alt head title-or-lower casing; every subsequent
            // word in the alt is strict case-sensitive. The old ciSentence path
            // accepted all casings; the new path tightens to oracle-text convention.
            var p = Words.phrase("[Until end of turn|This turn]");
            assertThat(p.parseSkipping(SPACE, "Until end of turn")).isEqualTo("Until end of turn");
            assertThat(p.parseSkipping(SPACE, "until end of turn")).isEqualTo("until end of turn");
            assertThat(p.parseSkipping(SPACE, "This turn")).isEqualTo("This turn");
            // Mid-alt-word capitalization is rejected (would have been accepted under
            // the old fully-case-insensitive ciSentence path).
            assertThatThrownBy(() -> p.parseSkipping(SPACE, "Until End of turn"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        void spacesAroundPipeAreTolerated() {
            // `between("[", "]")` lets the outer space-skip predicate stay active,
            // so cosmetic whitespace around `|` is fine.
            var p = Words.phrase("[ a | b ]");
            assertThat(p.parseSkipping(SPACE, "a")).isEqualTo("a");
            assertThat(p.parseSkipping(SPACE, "b")).isEqualTo("b");
        }

        @Test
        void emptyBracketRejected() {
            assertThatThrownBy(() -> Words.phrase("[]")).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
