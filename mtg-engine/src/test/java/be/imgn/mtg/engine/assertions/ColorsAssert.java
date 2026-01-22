package be.imgn.mtg.engine.assertions;

import org.assertj.core.api.AbstractObjectAssert;

import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.Colors;

/// Assertion class for Colors.
public class ColorsAssert extends AbstractObjectAssert<ColorsAssert, Colors> {

    protected ColorsAssert(Colors actual) {
        super(actual, ColorsAssert.class);
    }

    public static ColorsAssert assertThat(Colors actual) {
        return new ColorsAssert(actual);
    }

    public ColorsAssert isEmpty() {
        isNotNull();
        if (!actual.isEmpty()) {
            failWithMessage("Expected colors to be empty but was <%s>", actual);
        }
        return this;
    }

    public ColorsAssert isNotEmpty() {
        isNotNull();
        if (actual.isEmpty()) {
            failWithMessage("Expected colors not to be empty");
        }
        return this;
    }

    public ColorsAssert hasCount(int expected) {
        isNotNull();
        if (actual.count() != expected) {
            failWithMessage("Expected color count to be <%d> but was <%d>", expected, actual.count());
        }
        return this;
    }

    public ColorsAssert contains(Color color) {
        isNotNull();
        if (!actual.contains(color)) {
            failWithMessage("Expected colors to contain <%s> but was <%s>", color, actual);
        }
        return this;
    }

    public ColorsAssert doesNotContain(Color color) {
        isNotNull();
        if (actual.contains(color)) {
            failWithMessage("Expected colors not to contain <%s> but was <%s>", color, actual);
        }
        return this;
    }

    public ColorsAssert containsExactly(Color... colors) {
        isNotNull();
        if (actual.count() != colors.length) {
            failWithMessage("Expected exactly <%d> colors but had <%d>: <%s>", colors.length, actual.count(), actual);
        }
        for (var color : colors) {
            if (!actual.contains(color)) {
                failWithMessage("Expected colors to contain <%s> but was <%s>", color, actual);
            }
        }
        return this;
    }

    public ColorsAssert isColorless() {
        isNotNull();
        if (!actual.isColorless()) {
            failWithMessage("Expected to be colorless but was <%s>", actual);
        }
        return this;
    }

    public ColorsAssert isColored() {
        isNotNull();
        if (!actual.isColored()) {
            failWithMessage("Expected to be colored but was colorless");
        }
        return this;
    }

    public ColorsAssert isMonoColored() {
        isNotNull();
        if (!actual.isMonoColored()) {
            failWithMessage("Expected to be mono-colored but was <%s>", actual);
        }
        return this;
    }

    public ColorsAssert isMultiColored() {
        isNotNull();
        if (!actual.isMultiColored()) {
            failWithMessage("Expected to be multi-colored but was <%s>", actual);
        }
        return this;
    }

    public ColorsAssert isWhite() {
        isNotNull();
        if (!actual.isWhite()) {
            failWithMessage("Expected to contain white but was <%s>", actual);
        }
        return this;
    }

    public ColorsAssert isBlue() {
        isNotNull();
        if (!actual.isBlue()) {
            failWithMessage("Expected to contain blue but was <%s>", actual);
        }
        return this;
    }

    public ColorsAssert isBlack() {
        isNotNull();
        if (!actual.isBlack()) {
            failWithMessage("Expected to contain black but was <%s>", actual);
        }
        return this;
    }

    public ColorsAssert isRed() {
        isNotNull();
        if (!actual.isRed()) {
            failWithMessage("Expected to contain red but was <%s>", actual);
        }
        return this;
    }

    public ColorsAssert isGreen() {
        isNotNull();
        if (!actual.isGreen()) {
            failWithMessage("Expected to contain green but was <%s>", actual);
        }
        return this;
    }
}
