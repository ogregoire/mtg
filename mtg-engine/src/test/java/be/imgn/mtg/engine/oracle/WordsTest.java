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
            assertThat(p.parseSkipping(SPACE, "destroy creature")).isEqualTo("Destroy creature");
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
            assertThat(p.parseSkipping(SPACE, "Destroy creature")).isEqualTo("Destroy creature(s)");
            assertThat(p.parseSkipping(SPACE, "Destroy creatures")).isEqualTo("Destroy creature(s)");
        }

        @Test
        void arbitrarySuffix() {
            var p = Words.phrase("witness(es)");
            assertThat(p.parseSkipping(SPACE, "witness")).isEqualTo("witness(es)");
            assertThat(p.parseSkipping(SPACE, "witnesses")).isEqualTo("witness(es)");

            var gerund = Words.phrase("play(ing)");
            assertThat(gerund.parseSkipping(SPACE, "play")).isEqualTo("play(ing)");
            assertThat(gerund.parseSkipping(SPACE, "playing")).isEqualTo("play(ing)");
        }

        @Test
        void alternatives() {
            var p = Words.phrase("Target creature [is|are] blocked");
            assertThat(p.parseSkipping(SPACE, "Target creature is blocked"))
                    .isEqualTo("Target creature [is|are] blocked");
            assertThat(p.parseSkipping(SPACE, "Target creature are blocked"))
                    .isEqualTo("Target creature [is|are] blocked");
        }

        @Test
        void alternativesOfMoreThanTwo() {
            var p = Words.phrase("for each [white|blue|black|red|green]");
            for (var color : new String[] {"white", "blue", "black", "red", "green"}) {
                assertThat(p.parseSkipping(SPACE, "for each " + color))
                        .isEqualTo("for each [white|blue|black|red|green]");
            }
            assertThatThrownBy(() -> p.parseSkipping(SPACE, "for each yellow")).isInstanceOf(Exception.class);
        }

        @Test
        void multiWordBracket() {
            var p = Words.phrase("the top card [of your library]");
            assertThat(p.parseSkipping(SPACE, "the top card of your library"))
                    .isEqualTo("the top card [of your library]");
        }

        @Test
        void optionalWord() {
            var p = Words.phrase("deal(s) combat? damage");
            assertThat(p.parseSkipping(SPACE, "deal combat damage")).isEqualTo("deal(s) combat? damage");
            assertThat(p.parseSkipping(SPACE, "deal damage")).isEqualTo("deal(s) combat? damage");
        }

        @Test
        void optionalBracket() {
            var p = Words.phrase("the top card [of your library]?");
            assertThat(p.parseSkipping(SPACE, "the top card of your library"))
                    .isEqualTo("the top card [of your library]?");
            assertThat(p.parseSkipping(SPACE, "the top card")).isEqualTo("the top card [of your library]?");
        }

        @Test
        void optionalAlternatives() {
            var p = Words.phrase("attack(s) [alone|first]?");
            assertThat(p.parseSkipping(SPACE, "attacks alone")).isEqualTo("attack(s) [alone|first]?");
            assertThat(p.parseSkipping(SPACE, "attacks")).isEqualTo("attack(s) [alone|first]?");
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
    }
}
