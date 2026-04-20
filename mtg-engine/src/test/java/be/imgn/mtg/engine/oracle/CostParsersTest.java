package be.imgn.mtg.engine.oracle;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CostParsersTest {

    private static final CharPredicate SPACE = CharPredicate.is(' ');

    // ── TapSelf ───────────────────────────────────────────────────────────

    @Nested
    class TapSelfCost {

        @Test
        void parsesTapSymbol() {
            var result = CostParsers.COST_COMPONENT.parseSkipping(SPACE, "{T}");
            assertThat(result).isInstanceOf(Cost.TapSelf.class);
        }
    }

    // ── UntapSelf ─────────────────────────────────────────────────────────

    @Nested
    class UntapSelfCost {

        @Test
        void parsesUntapSymbol() {
            var result = CostParsers.COST_COMPONENT.parseSkipping(SPACE, "{Q}");
            assertThat(result).isInstanceOf(Cost.UntapSelf.class);
        }
    }

    // ── Mana ──────────────────────────────────────────────────────────────

    @Nested
    class ManaCost {

        @Test
        void parsesWhiteMana() {
            var result = CostParsers.MANA_COST.parseSkipping(SPACE, "{W}");
            assertThat(result).isInstanceOf(Cost.Mana.class);
            var mana = (Cost.Mana) result;
            assertThat(mana.symbols()).containsExactly(new ManaSymbol("{W}"));
        }

        @Test
        void parsesTwoBlackMana() {
            var result = CostParsers.MANA_COST.parseSkipping(SPACE, "{2}{B}");
            assertThat(result).isInstanceOf(Cost.Mana.class);
            var mana = (Cost.Mana) result;
            assertThat(mana.symbols()).containsExactly(new ManaSymbol("{2}"), new ManaSymbol("{B}"));
        }

        @Test
        void parsesXGreenGreenMana() {
            var result = CostParsers.MANA_COST.parseSkipping(SPACE, "{X}{G}{G}");
            assertThat(result).isInstanceOf(Cost.Mana.class);
            var mana = (Cost.Mana) result;
            assertThat(mana.symbols())
                    .containsExactly(new ManaSymbol("{X}"), new ManaSymbol("{G}"), new ManaSymbol("{G}"));
        }

        @Test
        void parsesWhiteManaCostComponent() {
            var result = CostParsers.COST_COMPONENT.parseSkipping(SPACE, "{W}");
            assertThat(result).isInstanceOf(Cost.Mana.class);
        }
    }

    // ── PayLife ───────────────────────────────────────────────────────────

    @Nested
    class PayLifeCost {

        @Test
        void paysThreeLife() {
            var result = CostParsers.PAY_LIFE.parseSkipping(SPACE, "Pay 3 life");
            assertThat(result).isInstanceOf(Cost.PayLife.class);
            var pl = (Cost.PayLife) result;
            assertThat(pl.amount()).isEqualTo(new Amount.Exact(3));
        }

        @Test
        void paysTwoLife() {
            var result = CostParsers.PAY_LIFE.parseSkipping(SPACE, "pay 2 life");
            assertThat(result).isInstanceOf(Cost.PayLife.class);
            var pl = (Cost.PayLife) result;
            assertThat(pl.amount()).isEqualTo(new Amount.Exact(2));
        }
    }

    // ── SacrificePermanent ────────────────────────────────────────────────

    @Nested
    class SacrificeCost {

        @Test
        void sacrificesACreature() {
            var result = CostParsers.SACRIFICE_COST.parseSkipping(SPACE, "Sacrifice a creature");
            assertThat(result).isInstanceOf(Cost.SacrificePermanent.class);
            var sac = (Cost.SacrificePermanent) result;
            assertThat(sac.what()).isInstanceOf(Subject.Select.class);
            var select = (Subject.Select) sac.what();
            assertThat(select.selector().type())
                    .isEqualTo(new Selector.TypeExpression.Single(new Selector.SingleType.OfCard(CardType.CREATURE)));
        }

        @Test
        void sacrificesALand() {
            var result = CostParsers.SACRIFICE_COST.parseSkipping(SPACE, "sacrifice a land");
            assertThat(result).isInstanceOf(Cost.SacrificePermanent.class);
        }

        @Test
        void sacrificesSelf() {
            var result = CostParsers.SACRIFICE_COST.parseSkipping(SPACE, "sacrifice this creature");
            assertThat(result).isInstanceOf(Cost.SacrificePermanent.class);
            var sac = (Cost.SacrificePermanent) result;
            assertThat(sac.what()).isInstanceOf(Subject.SelfRef.class);
        }
    }

    // ── DiscardCard ───────────────────────────────────────────────────────

    @Nested
    class DiscardCost {

        @Test
        void discardsACard() {
            var result = CostParsers.DISCARD_COST.parseSkipping(SPACE, "Discard a card");
            assertThat(result).isInstanceOf(Cost.DiscardCard.class);
            var discard = (Cost.DiscardCard) result;
            assertThat(discard.what()).isInstanceOf(Subject.Select.class);
            var select = (Subject.Select) discard.what();
            assertThat(select.selector().type())
                    .isEqualTo(new Selector.TypeExpression.Single(
                            new Selector.SingleType.OfGameObject(GameObjectType.CARD)));
        }
    }

    // ── Loyalty ───────────────────────────────────────────────────────────

    @Nested
    class LoyaltyCost {

        @Test
        void parsesPositiveLoyaltyCost() {
            var result = CostParsers.LOYALTY_COST.parseSkipping(SPACE, "+1");
            assertThat(result).isInstanceOf(Cost.Loyalty.class);
            var loyalty = (Cost.Loyalty) result;
            assertThat(loyalty.change()).isEqualTo(1);
        }

        @Test
        void parsesNegativeLoyaltyCost() {
            var result = CostParsers.LOYALTY_COST.parseSkipping(SPACE, "-3");
            assertThat(result).isInstanceOf(Cost.Loyalty.class);
            var loyalty = (Cost.Loyalty) result;
            assertThat(loyalty.change()).isEqualTo(-3);
        }
    }

    // ── Compound cost ─────────────────────────────────────────────────────

    @Nested
    class CompoundCostExpression {

        @Test
        void parsesTapSacrificeCreature() {
            var result = CostParsers.COST_EXPRESSION.parseSkipping(SPACE, "{T}, Sacrifice a creature");
            assertThat(result).isInstanceOf(Cost.Compound.class);
            var compound = (Cost.Compound) result;
            assertThat(compound.costs()).hasSize(2);
            assertThat(compound.costs().get(0)).isInstanceOf(Cost.TapSelf.class);
            assertThat(compound.costs().get(1)).isInstanceOf(Cost.SacrificePermanent.class);
        }

        @Test
        void parsesTwoWhiteTap() {
            var result = CostParsers.COST_EXPRESSION.parseSkipping(SPACE, "{2}{W}, {T}");
            assertThat(result).isInstanceOf(Cost.Compound.class);
            var compound = (Cost.Compound) result;
            assertThat(compound.costs()).hasSize(2);
            assertThat(compound.costs().get(0)).isInstanceOf(Cost.Mana.class);
            assertThat(compound.costs().get(1)).isInstanceOf(Cost.TapSelf.class);
        }

        @Test
        void parsesSingleCostNotWrapped() {
            var result = CostParsers.COST_EXPRESSION.parseSkipping(SPACE, "{T}");
            // Single cost should not be wrapped in Compound
            assertThat(result).isInstanceOf(Cost.TapSelf.class);
        }

        @Test
        void parsesTapDiscardCard() {
            var result = CostParsers.COST_EXPRESSION.parseSkipping(SPACE, "{T}, Discard a card");
            assertThat(result).isInstanceOf(Cost.Compound.class);
            var compound = (Cost.Compound) result;
            assertThat(compound.costs()).hasSize(2);
            assertThat(compound.costs().get(0)).isInstanceOf(Cost.TapSelf.class);
            assertThat(compound.costs().get(1)).isInstanceOf(Cost.DiscardCard.class);
        }
    }
}
