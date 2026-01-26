package be.imgn.mtg.engine.ability.internal.parser.selector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PowerToughness")
class PowerToughnessTest {

    @Nested
    @DisplayName("of")
    class OfTests {

        @Test
        @DisplayName("creates PowerToughness with exact values")
        void createsWithExactValues() {
            var pt = PowerToughness.of(2, 3);

            assertThat(pt.power()).isEqualTo(new Amount.Exact(2));
            assertThat(pt.toughness()).isEqualTo(new Amount.Exact(3));
        }
    }

    @Nested
    @DisplayName("isExact")
    class IsExactTests {

        @Test
        @DisplayName("returns true when both power and toughness are exact")
        void returnsTrueForExact() {
            var pt = PowerToughness.of(2, 3);

            assertThat(pt.isExact()).isTrue();
        }

        @Test
        @DisplayName("returns false when power is variable")
        void returnsFalseForVariablePower() {
            var pt = new PowerToughness(Amount.X, new Amount.Exact(3));

            assertThat(pt.isExact()).isFalse();
        }

        @Test
        @DisplayName("returns false when toughness is variable")
        void returnsFalseForVariableToughness() {
            var pt = new PowerToughness(new Amount.Exact(2), Amount.X);

            assertThat(pt.isExact()).isFalse();
        }

        @Test
        @DisplayName("returns false when both are variable")
        void returnsFalseForBothVariable() {
            var pt = new PowerToughness(Amount.X, Amount.X);

            assertThat(pt.isExact()).isFalse();
        }

        @Test
        @DisplayName("returns false when power is reference")
        void returnsFalseForReferencePower() {
            var pt = new PowerToughness(new Amount.Reference("chosen"), new Amount.Exact(3));

            assertThat(pt.isExact()).isFalse();
        }

        @Test
        @DisplayName("returns false when toughness is reference")
        void returnsFalseForReferenceToughness() {
            var pt = new PowerToughness(new Amount.Exact(2), new Amount.Reference("chosen"));

            assertThat(pt.isExact()).isFalse();
        }
    }

    @Nested
    @DisplayName("powerValue")
    class PowerValueTests {

        @Test
        @DisplayName("returns power value when exact")
        void returnsPowerValue() {
            var pt = PowerToughness.of(5, 3);

            assertThat(pt.powerValue()).isEqualTo(5);
        }

        @Test
        @DisplayName("throws when power is variable X")
        void throwsForVariableX() {
            var pt = new PowerToughness(Amount.X, new Amount.Exact(3));

            assertThatThrownBy(pt::powerValue)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Power is not an exact value");
        }

        @Test
        @DisplayName("throws when power is reference")
        void throwsForReference() {
            var pt = new PowerToughness(new Amount.Reference("chosen"), new Amount.Exact(3));

            assertThatThrownBy(pt::powerValue)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Power is not an exact value");
        }
    }

    @Nested
    @DisplayName("toughnessValue")
    class ToughnessValueTests {

        @Test
        @DisplayName("returns toughness value when exact")
        void returnsToughnessValue() {
            var pt = PowerToughness.of(5, 3);

            assertThat(pt.toughnessValue()).isEqualTo(3);
        }

        @Test
        @DisplayName("throws when toughness is variable X")
        void throwsForVariableX() {
            var pt = new PowerToughness(new Amount.Exact(2), Amount.X);

            assertThatThrownBy(pt::toughnessValue)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Toughness is not an exact value");
        }

        @Test
        @DisplayName("throws when toughness is reference")
        void throwsForReference() {
            var pt = new PowerToughness(new Amount.Exact(2), new Amount.Reference("chosen"));

            assertThatThrownBy(pt::toughnessValue)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Toughness is not an exact value");
        }
    }
}
