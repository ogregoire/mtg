package be.imgn.mtg.engine.oracle2.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.Test;

class NumberParserTest {
    private static final CharPredicate SPACE = CharPredicate.is(' ');

    @Test
    void integerParsesDigits() {
        assertThat(NumberParser.INTEGER.parseSkipping(SPACE, "0")).isEqualTo(0);
        assertThat(NumberParser.INTEGER.parseSkipping(SPACE, "42")).isEqualTo(42);
    }

    @Test
    void wordNumberParsesEnglishOneToTwenty() {
        assertThat(NumberParser.WORD_NUMBER.parseSkipping(SPACE, "one")).isEqualTo(1);
        assertThat(NumberParser.WORD_NUMBER.parseSkipping(SPACE, "Twenty")).isEqualTo(20);
    }

    @Test
    void numberAcceptsBothForms() {
        assertThat(NumberParser.NUMBER.parseSkipping(SPACE, "three")).isEqualTo(3);
        assertThat(NumberParser.NUMBER.parseSkipping(SPACE, "3")).isEqualTo(3);
    }

    /// Regression: longer `-teen` forms must win over their short
    /// counterparts, e.g. "sixteen" → 16 (not 6 with leftover "teen").
    @Test
    void teenFormsWinOverShorterPrefixes() {
        assertThat(NumberParser.WORD_NUMBER.parseSkipping(SPACE, "sixteen")).isEqualTo(16);
        assertThat(NumberParser.WORD_NUMBER.parseSkipping(SPACE, "Seventeen")).isEqualTo(17);
        assertThat(NumberParser.WORD_NUMBER.parseSkipping(SPACE, "eighteen")).isEqualTo(18);
        assertThat(NumberParser.WORD_NUMBER.parseSkipping(SPACE, "nineteen")).isEqualTo(19);
        assertThat(NumberParser.WORD_NUMBER.parseSkipping(SPACE, "fourteen")).isEqualTo(14);
        assertThat(NumberParser.WORD_NUMBER.parseSkipping(SPACE, "thirteen")).isEqualTo(13);
        assertThat(NumberParser.WORD_NUMBER.parseSkipping(SPACE, "fifteen")).isEqualTo(15);
    }

    @Test
    void signedIntAcceptsPlusAndAsciiMinus() {
        assertThat(NumberParser.SIGNED_INT.parseSkipping(SPACE, "+1")).isEqualTo(1);
        assertThat(NumberParser.SIGNED_INT.parseSkipping(SPACE, "-3")).isEqualTo(-3);
    }

    @Test
    void signedIntAcceptsTypographicMinus() {
        assertThat(NumberParser.SIGNED_INT.parseSkipping(SPACE, "−2")).isEqualTo(-2);
    }

    @Test
    void signedIntRejectsBareInt() {
        assertThatThrownBy(() -> NumberParser.SIGNED_INT.parseSkipping(SPACE, "5"))
                .isInstanceOf(Exception.class);
    }
}
