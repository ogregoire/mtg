package be.imgn.mtg.engine.oracle2.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.oracle2.domain.Amount;

class AmountParserTest {

    private Amount parse(String input) {
        return AmountParser.AMOUNT.parseSkipping(CharPredicate.is(' '), input);
    }

    @Test
    void parsesIntegerAsExact() {
        assertThat(parse("3")).isEqualTo(new Amount.Exact(3));
        assertThat(parse("0")).isEqualTo(new Amount.Exact(0));
        assertThat(parse("42")).isEqualTo(new Amount.Exact(42));
    }

    @Test
    void parsesWordNumberAsExact() {
        assertThat(parse("two")).isEqualTo(new Amount.Exact(2));
        assertThat(parse("Five")).isEqualTo(new Amount.Exact(5));
        assertThat(parse("twenty")).isEqualTo(new Amount.Exact(20));
    }

    @Test
    void parsesArticleAsExactOne() {
        assertThat(parse("a")).isEqualTo(new Amount.Exact(1));
        assertThat(parse("an")).isEqualTo(new Amount.Exact(1));
    }

    @Test
    void parsesXAsStandardX() {
        assertThat(parse("X")).isEqualTo(Amount.Standard.X);
    }

    @Test
    void parsesThatManyAsReference() {
        assertThat(parse("that many")).isEqualTo(Amount.Standard.REFERENCE);
        assertThat(parse("that much")).isEqualTo(Amount.Standard.REFERENCE);
        assertThat(parse("that number")).isEqualTo(Amount.Standard.REFERENCE);
    }

    @Test
    void parsesUpToInteger() {
        assertThat(parse("up to 5")).isEqualTo(new Amount.UpTo(new Amount.Exact(5)));
        assertThat(parse("up to three")).isEqualTo(new Amount.UpTo(new Amount.Exact(3)));
    }

    @Test
    void parsesUpToX() {
        assertThat(parse("up to X")).isEqualTo(new Amount.UpTo(Amount.Standard.X));
    }

    @Test
    void parsesRange() {
        assertThat(parse("one or two")).isEqualTo(new Amount.Range(1, 2));
        assertThat(parse("3 or 5")).isEqualTo(new Amount.Range(3, 5));
    }

    @Test
    void rejectsGarbage() {
        assertThatThrownBy(() -> parse("creature")).isInstanceOf(Exception.class);
    }
}
