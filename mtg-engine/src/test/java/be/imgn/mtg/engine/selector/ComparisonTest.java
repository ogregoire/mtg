package be.imgn.mtg.engine.selector;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Comparison")
class ComparisonTest {

    @Nested
    @DisplayName("EQUAL")
    class EqualTests {

        @Test
        @DisplayName("returns true when values are equal")
        void returnsTrue() {
            assertThat(Comparison.EQUAL.test(3, 3)).isTrue();
        }

        @Test
        @DisplayName("returns false when values are not equal")
        void returnsFalse() {
            assertThat(Comparison.EQUAL.test(3, 4)).isFalse();
            assertThat(Comparison.EQUAL.test(5, 3)).isFalse();
        }
    }

    @Nested
    @DisplayName("LESS")
    class LessTests {

        @Test
        @DisplayName("returns true when actual is less than expected")
        void returnsTrue() {
            assertThat(Comparison.LESS.test(2, 5)).isTrue();
        }

        @Test
        @DisplayName("returns false when actual equals expected")
        void returnsFalseWhenEqual() {
            assertThat(Comparison.LESS.test(3, 3)).isFalse();
        }

        @Test
        @DisplayName("returns false when actual is greater than expected")
        void returnsFalseWhenGreater() {
            assertThat(Comparison.LESS.test(5, 3)).isFalse();
        }
    }

    @Nested
    @DisplayName("LESS_OR_EQUAL")
    class LessOrEqualTests {

        @Test
        @DisplayName("returns true when actual is less than expected")
        void returnsTrueWhenLess() {
            assertThat(Comparison.LESS_OR_EQUAL.test(2, 5)).isTrue();
        }

        @Test
        @DisplayName("returns true when actual equals expected")
        void returnsTrueWhenEqual() {
            assertThat(Comparison.LESS_OR_EQUAL.test(3, 3)).isTrue();
        }

        @Test
        @DisplayName("returns false when actual is greater than expected")
        void returnsFalse() {
            assertThat(Comparison.LESS_OR_EQUAL.test(5, 3)).isFalse();
        }
    }

    @Nested
    @DisplayName("GREATER")
    class GreaterTests {

        @Test
        @DisplayName("returns true when actual is greater than expected")
        void returnsTrue() {
            assertThat(Comparison.GREATER.test(5, 2)).isTrue();
        }

        @Test
        @DisplayName("returns false when actual equals expected")
        void returnsFalseWhenEqual() {
            assertThat(Comparison.GREATER.test(3, 3)).isFalse();
        }

        @Test
        @DisplayName("returns false when actual is less than expected")
        void returnsFalseWhenLess() {
            assertThat(Comparison.GREATER.test(2, 5)).isFalse();
        }
    }

    @Nested
    @DisplayName("GREATER_OR_EQUAL")
    class GreaterOrEqualTests {

        @Test
        @DisplayName("returns true when actual is greater than expected")
        void returnsTrueWhenGreater() {
            assertThat(Comparison.GREATER_OR_EQUAL.test(5, 2)).isTrue();
        }

        @Test
        @DisplayName("returns true when actual equals expected")
        void returnsTrueWhenEqual() {
            assertThat(Comparison.GREATER_OR_EQUAL.test(3, 3)).isTrue();
        }

        @Test
        @DisplayName("returns false when actual is less than expected")
        void returnsFalse() {
            assertThat(Comparison.GREATER_OR_EQUAL.test(2, 5)).isFalse();
        }
    }
}
