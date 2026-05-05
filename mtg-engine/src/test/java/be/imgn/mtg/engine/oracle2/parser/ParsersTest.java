package be.imgn.mtg.engine.oracle2.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ParsersTest {

    private static final CharPredicate SKIP = CharPredicate.is(' ');

    @Nested
    class WordBoundaries {
        /// `phrase("a(n)")` matches the article "a" / "an" — but the
        /// underlying `word(...)` requires a word boundary, so "another"
        /// is not partially consumed as "an" + "other".
        @Test
        void anDoesNotEatAnother() {
            // dot-parse `word("an")` matches "an" but then enforces a
            // word boundary; "another" continues with `o`, so the
            // overall parse fails rather than partially consuming "an".
            assertThatThrownBy(() -> Parsers.phrase("a(n)").parseSkipping(SKIP, "another"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        void anotherIsAWholeWord() {
            assertThat(Parsers.phrase("Another").parseSkipping(SKIP, "another")).isEqualTo("another");
            assertThatThrownBy(() -> Parsers.phrase("Another").parseSkipping(SKIP, "an"))
                    .isInstanceOf(Exception.class);
        }

        /// With an optional `an?` prefix in front of a required noun,
        /// "another opponent" must NOT decompose as `an` + `other` +
        /// `opponent` — `word("an")` won't bite into "another".
        @Test
        void optionalArticleDoesNotEatAnother() {
            var p = Parsers.phrase("a(n)? opponent");
            assertThat(p.parseSkipping(SKIP, "an opponent")).isEqualTo("an opponent");
            assertThat(p.parseSkipping(SKIP, "opponent")).isEqualTo("opponent");
            assertThatThrownBy(() -> p.parseSkipping(SKIP, "another opponent")).isInstanceOf(Exception.class);
        }
    }

    @Nested
    class OptionalPrefix {
        @Test
        void singleOptionalPrefix() {
            var p = Parsers.phrase("Your? opponent(s)");
            assertThat(p.parseSkipping(SKIP, "Your opponent")).isEqualTo("Your opponent");
            assertThat(p.parseSkipping(SKIP, "your opponent")).isEqualTo("your opponent");
            assertThat(p.parseSkipping(SKIP, "Opponent")).isEqualTo("Opponent");
            assertThat(p.parseSkipping(SKIP, "opponent")).isEqualTo("opponent");
            assertThat(p.parseSkipping(SKIP, "opponents")).isEqualTo("opponents");
        }

        /// Skipped optionals do NOT introduce a leading or extra space
        /// in the returned matched text.
        @Test
        void skippedOptionalLeavesNoSpace() {
            var p = Parsers.phrase("Your? opponent");
            assertThat(p.parseSkipping(SKIP, "opponent")).isEqualTo("opponent").doesNotContain(" ");
        }

        @Test
        void multipleOptionalPrefixes() {
            var p = Parsers.phrase("[The|That]? other? opponent");
            assertThat(p.parseSkipping(SKIP, "opponent")).isEqualTo("opponent");
            assertThat(p.parseSkipping(SKIP, "the opponent")).isEqualTo("the opponent");
            assertThat(p.parseSkipping(SKIP, "other opponent")).isEqualTo("other opponent");
            assertThat(p.parseSkipping(SKIP, "the other opponent")).isEqualTo("the other opponent");
            assertThat(p.parseSkipping(SKIP, "That other opponent")).isEqualTo("That other opponent");
        }

        @Test
        void purelyOptionalTemplateRejected() {
            assertThatThrownBy(() -> Parsers.phrase("Your?"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("non-optional");
        }

        @Test
        void trailingOptionalStillSupported() {
            var p = Parsers.phrase("Test optional?");
            assertThat(p.parseSkipping(SKIP, "Test")).isEqualTo("Test");
            assertThat(p.parseSkipping(SKIP, "Test optional")).isEqualTo("Test optional");
        }
    }
}
