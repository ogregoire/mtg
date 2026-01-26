package be.imgn.mtg.engine.cost.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.cost.Cost;

@DisplayName("MillCost")
class MillCostTest {

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        void createsMillCostWithExactAmount() {
            var amount = new Amount.Exact(3);
            var cost = new MillCost(amount);

            assertThat(cost.amount()).isEqualTo(amount);
        }

        @Test
        void createsMillCostWithVariableAmount() {
            var amount = Amount.Variable.X;
            var cost = new MillCost(amount);

            assertThat(cost.amount()).isEqualTo(amount);
        }

        @Test
        void createsMillCostWithReferenceAmount() {
            var amount = new Amount.Reference("that much");
            var cost = new MillCost(amount);

            assertThat(cost.amount()).isEqualTo(amount);
        }
    }

    @Nested
    @DisplayName("description")
    class Description {

        @Test
        void returnsDescriptionForExactAmount() {
            var cost = new MillCost(new Amount.Exact(5));

            assertThat(cost.description()).isEqualTo("Mill 5");
        }

        @Test
        void returnsDescriptionForSingleCard() {
            var cost = new MillCost(new Amount.Exact(1));

            assertThat(cost.description()).isEqualTo("Mill 1");
        }

        @Test
        void returnsDescriptionForVariableAmount() {
            var cost = new MillCost(Amount.Variable.X);

            assertThat(cost.description()).isEqualTo("Mill X");
        }

        @Test
        void returnsDescriptionForReferenceAmount() {
            var cost = new MillCost(new Amount.Reference("that many"));

            assertThat(cost.description()).isEqualTo("Mill that many");
        }

        @Test
        void returnsDescriptionForZeroAmount() {
            var cost = new MillCost(new Amount.Exact(0));

            assertThat(cost.description()).isEqualTo("Mill 0");
        }

        @Test
        void returnsDescriptionForLargeAmount() {
            var cost = new MillCost(new Amount.Exact(100));

            assertThat(cost.description()).isEqualTo("Mill 100");
        }
    }

    @Nested
    @DisplayName("implements Cost")
    class ImplementsCost {

        @Test
        void isInstanceOfCost() {
            Cost cost = new MillCost(new Amount.Exact(3));

            assertThat(cost).isInstanceOf(Cost.class);
        }

        @Test
        void canBeUsedAsCost() {
            Cost cost = new MillCost(new Amount.Exact(7));

            assertThat(cost.description()).isEqualTo("Mill 7");
        }

        @Test
        void isNotLoyaltyCost() {
            Cost cost = new MillCost(new Amount.Exact(2));

            assertThat(cost.isLoyaltyCost()).isFalse();
        }
    }

    @Nested
    @DisplayName("equality and hashCode")
    class EqualityAndHashCode {

        @Test
        void sameAmountCreatesEqualCosts() {
            var cost1 = new MillCost(new Amount.Exact(3));
            var cost2 = new MillCost(new Amount.Exact(3));

            assertThat(cost1).isEqualTo(cost2);
            assertThat(cost1.hashCode()).isEqualTo(cost2.hashCode());
        }

        @Test
        void differentAmountCreatesUnequalCosts() {
            var cost1 = new MillCost(new Amount.Exact(3));
            var cost2 = new MillCost(new Amount.Exact(5));

            assertThat(cost1).isNotEqualTo(cost2);
        }

        @Test
        void variableAmountsAreEqual() {
            var cost1 = new MillCost(Amount.Variable.X);
            var cost2 = new MillCost(Amount.Variable.X);

            assertThat(cost1).isEqualTo(cost2);
            assertThat(cost1.hashCode()).isEqualTo(cost2.hashCode());
        }

        @Test
        void referenceAmountsWithSameNameAreEqual() {
            var cost1 = new MillCost(new Amount.Reference("that much"));
            var cost2 = new MillCost(new Amount.Reference("that much"));

            assertThat(cost1).isEqualTo(cost2);
            assertThat(cost1.hashCode()).isEqualTo(cost2.hashCode());
        }

        @Test
        void referenceAmountsWithDifferentNamesAreNotEqual() {
            var cost1 = new MillCost(new Amount.Reference("that much"));
            var cost2 = new MillCost(new Amount.Reference("that many"));

            assertThat(cost1).isNotEqualTo(cost2);
        }

        @Test
        void exactAndVariableAmountsAreNotEqual() {
            var cost1 = new MillCost(new Amount.Exact(5));
            var cost2 = new MillCost(Amount.Variable.X);

            assertThat(cost1).isNotEqualTo(cost2);
        }

        @Test
        void exactAndReferenceAmountsAreNotEqual() {
            var cost1 = new MillCost(new Amount.Exact(3));
            var cost2 = new MillCost(new Amount.Reference("three"));

            assertThat(cost1).isNotEqualTo(cost2);
        }

        @Test
        void isNotEqualToNull() {
            var cost = new MillCost(new Amount.Exact(4));

            assertThat(cost).isNotEqualTo(null);
        }

        @Test
        void isNotEqualToDifferentCostType() {
            var millCost = new MillCost(new Amount.Exact(2));
            var tapCost = new TapCost();

            assertThat(millCost).isNotEqualTo(tapCost);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        void containsAmountInformation() {
            var cost = new MillCost(new Amount.Exact(4));

            assertThat(cost.toString()).contains("4");
        }

        @Test
        void containsVariableInformation() {
            var cost = new MillCost(Amount.Variable.X);

            assertThat(cost.toString()).contains("X");
        }

        @Test
        void containsReferenceInformation() {
            var cost = new MillCost(new Amount.Reference("that much"));

            assertThat(cost.toString()).contains("that much");
        }
    }

    @Nested
    @DisplayName("amount accessor")
    class AmountAccessor {

        @Test
        void returnsExactAmount() {
            var amount = new Amount.Exact(6);
            var cost = new MillCost(amount);

            assertThat(cost.amount()).isEqualTo(amount);
        }

        @Test
        void returnsVariableAmount() {
            var amount = Amount.Variable.X;
            var cost = new MillCost(amount);

            assertThat(cost.amount()).isEqualTo(amount);
        }

        @Test
        void returnsReferenceAmount() {
            var amount = new Amount.Reference("twice that");
            var cost = new MillCost(amount);

            assertThat(cost.amount()).isEqualTo(amount);
        }
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCases {

        @Test
        void handlesNegativeExactAmount() {
            var cost = new MillCost(new Amount.Exact(-1));

            assertThat(cost.description()).isEqualTo("Mill -1");
            assertThat(cost.amount()).isEqualTo(new Amount.Exact(-1));
        }

        @Test
        void handlesMaxIntegerAmount() {
            var cost = new MillCost(new Amount.Exact(Integer.MAX_VALUE));

            assertThat(cost.description()).isEqualTo("Mill " + Integer.MAX_VALUE);
        }

        @Test
        void handlesMinIntegerAmount() {
            var cost = new MillCost(new Amount.Exact(Integer.MIN_VALUE));

            assertThat(cost.description()).isEqualTo("Mill " + Integer.MIN_VALUE);
        }

        @Test
        void handlesEmptyReferenceString() {
            var cost = new MillCost(new Amount.Reference(""));

            assertThat(cost.description()).isEqualTo("Mill ");
        }

        @Test
        void handlesLongReferenceString() {
            var longReference = "a".repeat(1000);
            var cost = new MillCost(new Amount.Reference(longReference));

            assertThat(cost.description()).isEqualTo("Mill " + longReference);
        }

        @Test
        void handlesSpecialCharactersInReference() {
            var cost = new MillCost(new Amount.Reference("that many (rounded down)"));

            assertThat(cost.description()).isEqualTo("Mill that many (rounded down)");
        }
    }
}
