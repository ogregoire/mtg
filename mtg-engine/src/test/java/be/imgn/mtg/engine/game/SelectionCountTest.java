package be.imgn.mtg.engine.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SelectionCountTest {

    @Nested
    class Exactly {

        @Test
        void validatesExactMatch() {
            var count = SelectionCount.exactly(3);

            assertThat(count.isValid(3)).isTrue();
        }

        @Test
        void rejectsLessThanExact() {
            var count = SelectionCount.exactly(5);

            assertThat(count.isValid(4)).isFalse();
            assertThat(count.isValid(0)).isFalse();
        }

        @Test
        void rejectsMoreThanExact() {
            var count = SelectionCount.exactly(2);

            assertThat(count.isValid(3)).isFalse();
            assertThat(count.isValid(10)).isFalse();
        }

        @Test
        void acceptsZeroWhenExactlyZero() {
            var count = SelectionCount.exactly(0);

            assertThat(count.isValid(0)).isTrue();
        }

        @Test
        void throwsWhenNegative() {
            assertThatThrownBy(() -> SelectionCount.exactly(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("non-negative");
        }

        @Test
        void hasDescriptiveToString() {
            var count = SelectionCount.exactly(4);

            assertThat(count.toString()).isEqualTo("exactly 4");
        }
    }

    @Nested
    class Between {

        @Test
        void validatesWithinRange() {
            var count = SelectionCount.between(2, 5);

            assertThat(count.isValid(2)).isTrue();
            assertThat(count.isValid(3)).isTrue();
            assertThat(count.isValid(5)).isTrue();
        }

        @Test
        void rejectsBelowMin() {
            var count = SelectionCount.between(3, 7);

            assertThat(count.isValid(0)).isFalse();
            assertThat(count.isValid(2)).isFalse();
        }

        @Test
        void rejectsAboveMax() {
            var count = SelectionCount.between(1, 4);

            assertThat(count.isValid(5)).isFalse();
            assertThat(count.isValid(10)).isFalse();
        }

        @Test
        void acceptsMinEqualsMax() {
            var count = SelectionCount.between(3, 3);

            assertThat(count.isValid(3)).isTrue();
            assertThat(count.isValid(2)).isFalse();
            assertThat(count.isValid(4)).isFalse();
        }

        @Test
        void throwsWhenMinNegative() {
            assertThatThrownBy(() -> SelectionCount.between(-1, 5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("non-negative");
        }

        @Test
        void throwsWhenMaxLessThanMin() {
            assertThatThrownBy(() -> SelectionCount.between(5, 3))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("max must be >= min");
        }

        @Test
        void hasDescriptiveToString() {
            var count = SelectionCount.between(1, 3);

            assertThat(count.toString()).isEqualTo("between 1 and 3");
        }
    }

    @Nested
    class UpTo {

        @Test
        void validatesZeroToMax() {
            var count = SelectionCount.upTo(4);

            assertThat(count.isValid(0)).isTrue();
            assertThat(count.isValid(1)).isTrue();
            assertThat(count.isValid(4)).isTrue();
        }

        @Test
        void rejectsAboveMax() {
            var count = SelectionCount.upTo(3);

            assertThat(count.isValid(4)).isFalse();
            assertThat(count.isValid(100)).isFalse();
        }

        @Test
        void rejectsNegative() {
            var count = SelectionCount.upTo(5);

            assertThat(count.isValid(-1)).isFalse();
        }

        @Test
        void throwsWhenMaxNegative() {
            assertThatThrownBy(() -> SelectionCount.upTo(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("non-negative");
        }

        @Test
        void hasDescriptiveToString() {
            var count = SelectionCount.upTo(7);

            assertThat(count.toString()).isEqualTo("up to 7");
        }
    }

    @Nested
    class AtLeast {

        @Test
        void validatesMinAndAbove() {
            var count = SelectionCount.atLeast(3);

            assertThat(count.isValid(3)).isTrue();
            assertThat(count.isValid(4)).isTrue();
            assertThat(count.isValid(100)).isTrue();
        }

        @Test
        void rejectsBelowMin() {
            var count = SelectionCount.atLeast(5);

            assertThat(count.isValid(0)).isFalse();
            assertThat(count.isValid(4)).isFalse();
        }

        @Test
        void acceptsZeroWhenMinIsZero() {
            var count = SelectionCount.atLeast(0);

            assertThat(count.isValid(0)).isTrue();
            assertThat(count.isValid(1)).isTrue();
        }

        @Test
        void throwsWhenMinNegative() {
            assertThatThrownBy(() -> SelectionCount.atLeast(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("non-negative");
        }

        @Test
        void hasDescriptiveToString() {
            var count = SelectionCount.atLeast(2);

            assertThat(count.toString()).isEqualTo("at least 2");
        }
    }

    @Nested
    class FactoryMethods {

        @Test
        void exactlyCreatesExactlyInstance() {
            var count = SelectionCount.exactly(5);

            assertThat(count).isInstanceOf(SelectionCount.Exactly.class);
            assertThat(((SelectionCount.Exactly) count).n()).isEqualTo(5);
        }

        @Test
        void upToCreatesUpToInstance() {
            var count = SelectionCount.upTo(3);

            assertThat(count).isInstanceOf(SelectionCount.UpTo.class);
            assertThat(((SelectionCount.UpTo) count).max()).isEqualTo(3);
        }

        @Test
        void atLeastCreatesAtLeastInstance() {
            var count = SelectionCount.atLeast(2);

            assertThat(count).isInstanceOf(SelectionCount.AtLeast.class);
            assertThat(((SelectionCount.AtLeast) count).min()).isEqualTo(2);
        }

        @Test
        void betweenCreatesBetweenInstance() {
            var count = SelectionCount.between(1, 4);

            assertThat(count).isInstanceOf(SelectionCount.Between.class);
            assertThat(((SelectionCount.Between) count).min()).isEqualTo(1);
            assertThat(((SelectionCount.Between) count).max()).isEqualTo(4);
        }
    }
}
