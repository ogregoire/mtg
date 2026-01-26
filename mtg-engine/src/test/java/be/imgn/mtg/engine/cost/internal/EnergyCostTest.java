package be.imgn.mtg.engine.cost.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;

@DisplayName("EnergyCost")
class EnergyCostTest {

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("creates cost with exact amount")
        void createsWithExactAmount() {
            var amount = new Amount.Exact(3);
            var cost = new EnergyCost(amount);

            assertThat(cost.amount()).isEqualTo(amount);
        }

        @Test
        @DisplayName("creates cost with variable amount")
        void createsWithVariableAmount() {
            var amount = Amount.Variable.X;
            var cost = new EnergyCost(amount);

            assertThat(cost.amount()).isEqualTo(amount);
        }

        @Test
        @DisplayName("creates cost with reference amount")
        void createsWithReferenceAmount() {
            var amount = new Amount.Reference("that much");
            var cost = new EnergyCost(amount);

            assertThat(cost.amount()).isEqualTo(amount);
        }
    }

    @Nested
    @DisplayName("description()")
    class Description {

        @Test
        @DisplayName("returns 'Pay 1 {E}' for exact amount 1")
        void exactAmountOne() {
            var cost = new EnergyCost(new Amount.Exact(1));

            assertThat(cost.description()).isEqualTo("Pay 1 {E}");
        }

        @Test
        @DisplayName("returns 'Pay 3 {E}' for exact amount 3")
        void exactAmountThree() {
            var cost = new EnergyCost(new Amount.Exact(3));

            assertThat(cost.description()).isEqualTo("Pay 3 {E}");
        }

        @Test
        @DisplayName("returns 'Pay 0 {E}' for exact amount 0")
        void exactAmountZero() {
            var cost = new EnergyCost(new Amount.Exact(0));

            assertThat(cost.description()).isEqualTo("Pay 0 {E}");
        }

        @Test
        @DisplayName("returns 'Pay 10 {E}' for large exact amount")
        void largeExactAmount() {
            var cost = new EnergyCost(new Amount.Exact(10));

            assertThat(cost.description()).isEqualTo("Pay 10 {E}");
        }

        @Test
        @DisplayName("returns 'Pay X {E}' for variable amount")
        void variableAmount() {
            var cost = new EnergyCost(Amount.Variable.X);

            assertThat(cost.description()).isEqualTo("Pay X {E}");
        }

        @Test
        @DisplayName("returns 'Pay that much {E}' for reference amount")
        void referenceAmount() {
            var cost = new EnergyCost(new Amount.Reference("that much"));

            assertThat(cost.description()).isEqualTo("Pay that much {E}");
        }

        @Test
        @DisplayName("returns reference name in description")
        void referenceNameInDescription() {
            var cost = new EnergyCost(new Amount.Reference("twice that"));

            assertThat(cost.description()).isEqualTo("Pay twice that {E}");
        }
    }

    @Nested
    @DisplayName("isLoyaltyCost()")
    class IsLoyaltyCost {

        @Test
        @DisplayName("returns false")
        void returnsFalse() {
            var cost = new EnergyCost(new Amount.Exact(2));

            assertThat(cost.isLoyaltyCost()).isFalse();
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("costs with same exact amount are equal")
        void sameExactAmount() {
            var cost1 = new EnergyCost(new Amount.Exact(3));
            var cost2 = new EnergyCost(new Amount.Exact(3));

            assertThat(cost1).isEqualTo(cost2);
            assertThat(cost1.hashCode()).isEqualTo(cost2.hashCode());
        }

        @Test
        @DisplayName("costs with different exact amounts are not equal")
        void differentExactAmounts() {
            var cost1 = new EnergyCost(new Amount.Exact(2));
            var cost2 = new EnergyCost(new Amount.Exact(3));

            assertThat(cost1).isNotEqualTo(cost2);
        }

        @Test
        @DisplayName("costs with same variable amount are equal")
        void sameVariableAmount() {
            var cost1 = new EnergyCost(Amount.Variable.X);
            var cost2 = new EnergyCost(Amount.Variable.X);

            assertThat(cost1).isEqualTo(cost2);
            assertThat(cost1.hashCode()).isEqualTo(cost2.hashCode());
        }

        @Test
        @DisplayName("costs with same reference amount are equal")
        void sameReferenceAmount() {
            var cost1 = new EnergyCost(new Amount.Reference("that much"));
            var cost2 = new EnergyCost(new Amount.Reference("that much"));

            assertThat(cost1).isEqualTo(cost2);
            assertThat(cost1.hashCode()).isEqualTo(cost2.hashCode());
        }

        @Test
        @DisplayName("costs with different reference amounts are not equal")
        void differentReferenceAmounts() {
            var cost1 = new EnergyCost(new Amount.Reference("that much"));
            var cost2 = new EnergyCost(new Amount.Reference("twice that"));

            assertThat(cost1).isNotEqualTo(cost2);
        }

        @Test
        @DisplayName("exact and variable amounts are not equal")
        void exactAndVariableNotEqual() {
            var cost1 = new EnergyCost(new Amount.Exact(5));
            var cost2 = new EnergyCost(Amount.Variable.X);

            assertThat(cost1).isNotEqualTo(cost2);
        }

        @Test
        @DisplayName("exact and reference amounts are not equal")
        void exactAndReferenceNotEqual() {
            var cost1 = new EnergyCost(new Amount.Exact(2));
            var cost2 = new EnergyCost(new Amount.Reference("2"));

            assertThat(cost1).isNotEqualTo(cost2);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("negative exact amount formats correctly")
        void negativeExactAmount() {
            var cost = new EnergyCost(new Amount.Exact(-1));

            assertThat(cost.description()).isEqualTo("Pay -1 {E}");
        }

        @Test
        @DisplayName("empty reference name formats correctly")
        void emptyReferenceName() {
            var cost = new EnergyCost(new Amount.Reference(""));

            assertThat(cost.description()).isEqualTo("Pay  {E}");
        }

        @Test
        @DisplayName("reference with whitespace formats correctly")
        void referenceWithWhitespace() {
            var cost = new EnergyCost(new Amount.Reference("that many"));

            assertThat(cost.description()).isEqualTo("Pay that many {E}");
        }

        @Test
        @DisplayName("very large exact amount formats correctly")
        void veryLargeExactAmount() {
            var cost = new EnergyCost(new Amount.Exact(1_000_000));

            assertThat(cost.description()).isEqualTo("Pay 1000000 {E}");
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToString {

        @Test
        @DisplayName("contains amount information")
        void containsAmountInformation() {
            var cost = new EnergyCost(new Amount.Exact(3));

            assertThat(cost.toString()).contains("3");
        }

        @Test
        @DisplayName("contains EnergyCost class name")
        void containsClassName() {
            var cost = new EnergyCost(new Amount.Exact(2));

            assertThat(cost.toString()).contains("EnergyCost");
        }
    }
}
