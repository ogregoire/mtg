package be.imgn.mtg.engine.oracle2.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.AmountMatcher;

class AmountMatcherParserTest {
    private static final CharPredicate SPACE = CharPredicate.is(' ');

    private AmountMatcher parse(String input) {
        return AmountMatcherParser.AMOUNT_MATCHER.parseSkipping(SPACE, input);
    }

    @Test
    void postfixOrLess() {
        assertThat(parse("3 or less")).isEqualTo(new AmountMatcher.AtMost(new Amount.Exact(3)));
    }

    @Test
    void postfixOrMore() {
        assertThat(parse("4 or more")).isEqualTo(new AmountMatcher.AtLeast(new Amount.Exact(4)));
    }

    @Test
    void postfixOrGreater() {
        assertThat(parse("five or greater")).isEqualTo(new AmountMatcher.AtLeast(new Amount.Exact(5)));
    }

    @Test
    void variableXOrLess() {
        assertThat(parse("X or less")).isEqualTo(new AmountMatcher.AtMost(Amount.Standard.X));
    }

    @Test
    void prefixAtLeast() {
        assertThat(parse("at least 2")).isEqualTo(new AmountMatcher.AtLeast(new Amount.Exact(2)));
    }

    @Test
    void prefixAtMost() {
        assertThat(parse("at most 7")).isEqualTo(new AmountMatcher.AtMost(new Amount.Exact(7)));
    }

    @Test
    void prefixExactly() {
        assertThat(parse("exactly 1")).isEqualTo(new AmountMatcher.Exactly(new Amount.Exact(1)));
    }

    @Test
    void noFoldsToExactlyZero() {
        assertThat(parse("no")).isEqualTo(new AmountMatcher.Exactly(new Amount.Exact(0)));
    }

    @Test
    void bareNumberFoldsToExactly() {
        assertThat(parse("1")).isEqualTo(new AmountMatcher.Exactly(new Amount.Exact(1)));
        assertThat(parse("six")).isEqualTo(new AmountMatcher.Exactly(new Amount.Exact(6)));
    }
}
