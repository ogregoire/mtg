package be.imgn.mtg.engine.oracle.parser;

import static com.google.common.labs.parse.Parser.word;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.oracle.domain.Color;

class MtgParsersTest {

    private static final CharPredicate SPACE = CharPredicate.is(' ');

    @Nested
    class OxfordAnd {

        private static final Parser<List<String>> LIST = MtgParsers.andList(word());

        @Test
        void singleElement() {
            assertThat(LIST.parseSkipping(SPACE, "creature")).containsExactly("creature");
        }

        @Test
        void twoElements() {
            assertThat(LIST.parseSkipping(SPACE, "flying and haste")).containsExactly("flying", "haste");
        }

        @Test
        void threeElementsOxfordComma() {
            assertThat(LIST.parseSkipping(SPACE, "flying, trample, and haste"))
                    .containsExactly("flying", "trample", "haste");
        }

        @Test
        void fourElements() {
            assertThat(LIST.parseSkipping(SPACE, "flying, first, trample, and haste"))
                    .containsExactly("flying", "first", "trample", "haste");
        }

        @Test
        void fiveElements() {
            assertThat(LIST.parseSkipping(SPACE, "a, b, c, d, and e")).containsExactly("a", "b", "c", "d", "e");
        }
    }

    @Nested
    class OxfordOr {

        private static final Parser<List<String>> LIST = MtgParsers.orList(word());

        @Test
        void singleElement() {
            assertThat(LIST.parseSkipping(SPACE, "creature")).containsExactly("creature");
        }

        @Test
        void twoElements() {
            assertThat(LIST.parseSkipping(SPACE, "artifact or enchantment")).containsExactly("artifact", "enchantment");
        }

        @Test
        void threeElementsOxfordComma() {
            assertThat(LIST.parseSkipping(SPACE, "artifact, creature, or enchantment"))
                    .containsExactly("artifact", "creature", "enchantment");
        }

        @Test
        void fourElements() {
            assertThat(LIST.parseSkipping(SPACE, "artifact, creature, enchantment, or land"))
                    .containsExactly("artifact", "creature", "enchantment", "land");
        }
    }

    @Nested
    class WithTypedParsers {

        private static final Parser<List<Color>> COLORS = MtgParsers.andList(SelectorParsers.COLOR);

        @Test
        void singleColor() {
            assertThat(COLORS.parseSkipping(SPACE, "white")).containsExactly(Color.WHITE);
        }

        @Test
        void twoColors() {
            assertThat(COLORS.parseSkipping(SPACE, "white and blue")).containsExactly(Color.WHITE, Color.BLUE);
        }

        @Test
        void threeColors() {
            assertThat(COLORS.parseSkipping(SPACE, "white, blue, and black"))
                    .containsExactly(Color.WHITE, Color.BLUE, Color.BLACK);
        }

        @Test
        void fiveColors() {
            assertThat(COLORS.parseSkipping(SPACE, "white, blue, black, red, and green"))
                    .containsExactly(Color.WHITE, Color.BLUE, Color.BLACK, Color.RED, Color.GREEN);
        }
    }

    @Nested
    class CustomConnector {

        private static final Parser<List<String>> LIST = MtgParsers.andOrList(word());

        @Test
        void twoElements() {
            assertThat(LIST.parseSkipping(SPACE, "library and/or graveyard")).containsExactly("library", "graveyard");
        }

        @Test
        void singleElement() {
            assertThat(LIST.parseSkipping(SPACE, "library")).containsExactly("library");
        }
    }
}
