package be.imgn.mtg.engine.ability.internal.parser.selector;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Amount")
class AmountTest {

    @Nested
    @DisplayName("Exact")
    class ExactTests {

        @Test
        @DisplayName("stores the exact value")
        void storesValue() {
            var amount = new Amount.Exact(5);
            assertThat(amount.value()).isEqualTo(5);
        }

        @Test
        @DisplayName("can be zero")
        void canBeZero() {
            var amount = new Amount.Exact(0);
            assertThat(amount.value()).isEqualTo(0);
        }

        @Test
        @DisplayName("can be negative")
        void canBeNegative() {
            var amount = new Amount.Exact(-3);
            assertThat(amount.value()).isEqualTo(-3);
        }
    }

    @Nested
    @DisplayName("Reference")
    class ReferenceTests {

        @Test
        @DisplayName("stores the reference name")
        void storesName() {
            var amount = new Amount.Reference("that much");
            assertThat(amount.name()).isEqualTo("that much");
        }
    }

    @Nested
    @DisplayName("Variable")
    class VariableTests {

        @Test
        @DisplayName("X is available")
        void xAvailable() {
            var amount = Amount.Variable.X;
            assertThat(amount).isNotNull();
        }

        @Test
        @DisplayName("X constant is same as Variable.X")
        void xConstant() {
            assertThat(Amount.X).isSameAs(Amount.Variable.X);
        }
    }
}
