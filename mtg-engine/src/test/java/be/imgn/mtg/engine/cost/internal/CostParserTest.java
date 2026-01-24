package be.imgn.mtg.engine.cost.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.cost.Cost;
import be.imgn.mtg.engine.mana.ManaCost;

@DisplayName("CostParser")
class CostParserTest {

    @Nested
    @DisplayName("Tap and untap symbols")
    class TapUntapSymbols {

        @Test
        @DisplayName("{T} → TapCost")
        void tapSymbol() {
            var cost = CostParser.parse("{T}");

            assertThat(cost).isInstanceOf(TapCost.class);
            assertThat(cost.description()).isEqualTo("{T}");
        }

        @Test
        @DisplayName("{Q} → UntapCost")
        void untapSymbol() {
            var cost = CostParser.parse("{Q}");

            assertThat(cost).isInstanceOf(UntapCost.class);
            assertThat(cost.description()).isEqualTo("{Q}");
        }
    }

    @Nested
    @DisplayName("Loyalty costs")
    class LoyaltyCosts {

        @Test
        @DisplayName("[+1] → LoyaltyCost(1)")
        void positiveLoyalty() {
            var cost = CostParser.parse("[+1]");

            assertThat(cost).isInstanceOf(LoyaltyCost.class);
            assertThat(((LoyaltyCost) cost).amount()).isEqualTo(1);
            assertThat(cost.isLoyaltyCost()).isTrue();
        }

        @Test
        @DisplayName("[-3] → LoyaltyCost(-3)")
        void negativeLoyalty() {
            var cost = CostParser.parse("[-3]");

            assertThat(cost).isInstanceOf(LoyaltyCost.class);
            assertThat(((LoyaltyCost) cost).amount()).isEqualTo(-3);
            assertThat(cost.isLoyaltyCost()).isTrue();
        }

        @Test
        @DisplayName("[0] → LoyaltyCost(0)")
        void zeroLoyalty() {
            var cost = CostParser.parse("[0]");

            assertThat(cost).isInstanceOf(LoyaltyCost.class);
            assertThat(((LoyaltyCost) cost).amount()).isEqualTo(0);
            assertThat(cost.isLoyaltyCost()).isTrue();
        }

        @Test
        @DisplayName("[+5] → LoyaltyCost(5)")
        void largePositiveLoyalty() {
            var cost = CostParser.parse("[+5]");

            assertThat(cost).isInstanceOf(LoyaltyCost.class);
            assertThat(((LoyaltyCost) cost).amount()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Mana costs")
    class ManaCosts {

        @Test
        @DisplayName("{2}{W} → ManaCost with mana value 3")
        void genericAndColored() {
            var cost = CostParser.parse("{2}{W}");

            assertThat(cost).isInstanceOf(ManaCost.class);
            assertThat(((ManaCost) cost).manaValue()).isEqualTo(3);
        }

        @Test
        @DisplayName("{G} → ManaCost with mana value 1")
        void singleColored() {
            var cost = CostParser.parse("{G}");

            assertThat(cost).isInstanceOf(ManaCost.class);
            assertThat(((ManaCost) cost).manaValue()).isEqualTo(1);
        }

        @Test
        @DisplayName("{1}{R} → ManaCost with mana value 2")
        void genericAndRed() {
            var cost = CostParser.parse("{1}{R}");

            assertThat(cost).isInstanceOf(ManaCost.class);
            assertThat(((ManaCost) cost).manaValue()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Life costs")
    class LifeCosts {

        @Test
        @DisplayName("Pay 2 life → LifeCost")
        void payTwoLife() {
            var cost = CostParser.parse("Pay 2 life");

            assertThat(cost).isInstanceOf(LifeCost.class);
            assertThat(cost.description()).isEqualTo("Pay 2 life");
        }
    }

    @Nested
    @DisplayName("Sacrifice costs")
    class SacrificeCosts {

        @Test
        @DisplayName("Sacrifice a creature → SacrificeCost")
        void sacrificeCreature() {
            var cost = CostParser.parse("Sacrifice a creature");

            assertThat(cost).isInstanceOf(SacrificeCost.class);
        }
    }

    @Nested
    @DisplayName("Discard costs")
    class DiscardCosts {

        @Test
        @DisplayName("Discard a card → DiscardCost")
        void discardCard() {
            var cost = CostParser.parse("Discard a card");

            assertThat(cost).isInstanceOf(DiscardCost.class);
        }
    }

    @Nested
    @DisplayName("Compound costs")
    class CompoundCosts {

        @Test
        @DisplayName("{T}, {G} → CompoundCost([TapCost, ManaCost])")
        void tapAndMana() {
            var cost = CostParser.parse("{T}, {G}");

            assertThat(cost).isInstanceOf(CompoundCost.class);
            var compound = (CompoundCost) cost;
            assertThat(compound.costs()).hasSize(2);
            assertThat(compound.costs().get(0)).isInstanceOf(TapCost.class);
            assertThat(compound.costs().get(1)).isInstanceOf(ManaCost.class);
        }

        @Test
        @DisplayName("{2}{B}, Sacrifice a creature → CompoundCost")
        void manaAndSacrifice() {
            var cost = CostParser.parse("{2}{B}, Sacrifice a creature");

            assertThat(cost).isInstanceOf(CompoundCost.class);
            var compound = (CompoundCost) cost;
            assertThat(compound.costs()).hasSize(2);
            assertThat(compound.costs().get(0)).isInstanceOf(ManaCost.class);
            assertThat(compound.costs().get(1)).isInstanceOf(SacrificeCost.class);
        }

        @Test
        @DisplayName("compound cost is not a loyalty cost")
        void compoundNotLoyalty() {
            var cost = CostParser.parse("{T}, {G}");

            assertThat(cost.isLoyaltyCost()).isFalse();
        }
    }

    @Nested
    @DisplayName("Description")
    class Description {

        @Test
        @DisplayName("TapCost description is {T}")
        void tapDescription() {
            assertThat(new TapCost().description()).isEqualTo("{T}");
        }

        @Test
        @DisplayName("UntapCost description is {Q}")
        void untapDescription() {
            assertThat(new UntapCost().description()).isEqualTo("{Q}");
        }

        @Test
        @DisplayName("LoyaltyCost(+2) description is [+2]")
        void positiveLoyaltyDescription() {
            assertThat(new LoyaltyCost(2).description()).isEqualTo("[+2]");
        }

        @Test
        @DisplayName("LoyaltyCost(-3) description is [-3]")
        void negativeLoyaltyDescription() {
            assertThat(new LoyaltyCost(-3).description()).isEqualTo("[-3]");
        }

        @Test
        @DisplayName("CompoundCost description joins with comma")
        void compoundDescription() {
            var cost = (Cost) new CompoundCost(List.of(new TapCost(), new LoyaltyCost(-1)));
            assertThat(cost.description()).isEqualTo("{T}, [-1]");
        }
    }
}
