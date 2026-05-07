package be.imgn.mtg.engine.oracle2.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.CardType;
import be.imgn.mtg.engine.oracle2.domain.Cost;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Colored;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Generic;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Variable;
import be.imgn.mtg.engine.oracle2.domain.selector.CardTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.QuantifierSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.domain.selector.SelfSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ZoneSelector;

class CostParserTest {

    private static Cost parse(String input) {
        return CostParser.COST.parseSkipping(CharPredicate.is(' '), input);
    }

    private static Cost.ManaCost mana(ManaSymbol... symbols) {
        return new Cost.ManaCost(List.of(symbols));
    }

    private static final ZoneSelector.Battlefield CREATURE =
            new ZoneSelector.Battlefield(new ObjectTypeSelector.Permanent(new CardTypeSelector.Is(CardType.CREATURE)));

    private static Selector one(Selector inner) {
        return new QuantifierSelector(new Amount.Exact(1), inner);
    }

    @Nested
    class Primitive {
        @Test
        void tapSelf() {
            assertThat(parse("{T}")).isEqualTo(new Cost.Tap(SelfSelector.SELF));
        }

        @Test
        void manaCostGenericAndColor() {
            // ManaParser does the symbol-by-symbol heavy lifting (see
            // ManaParserTest); CostParser just wraps the result in
            // Cost.ManaCost.
            assertThat(parse("{1}{G}")).isEqualTo(mana(new Generic(1), Colored.GREEN));
        }

        @Test
        void manaCostWithVariable() {
            assertThat(parse("{X}{R}")).isEqualTo(mana(Variable.X, Colored.RED));
        }

        @Test
        void sacrificeACreature() {
            assertThat(parse("Sacrifice a creature")).isEqualTo(new Cost.Sacrifice(one(CREATURE)));
        }

        @Test
        void payTwoLife() {
            assertThat(parse("Pay 2 life")).isEqualTo(new Cost.PayLife(new Amount.Exact(2)));
        }

        @Test
        void payXLife() {
            assertThat(parse("Pay X life")).isEqualTo(new Cost.PayLife(Amount.Standard.X));
        }
    }

    @Nested
    class Compound {
        @Test
        void manaThenTap() {
            assertThat(parse("{1}, {T}"))
                    .isEqualTo(new Cost.CompoundCost(List.of(mana(new Generic(1)), new Cost.Tap(SelfSelector.SELF))));
        }

        @Test
        void manaTapAndSacrifice() {
            assertThat(parse("{1}{G}, {T}, Sacrifice a creature"))
                    .isEqualTo(new Cost.CompoundCost(List.of(
                            mana(new Generic(1), Colored.GREEN),
                            new Cost.Tap(SelfSelector.SELF),
                            new Cost.Sacrifice(one(CREATURE)))));
        }

        @Test
        void tapAndPayLife() {
            assertThat(parse("{T}, Pay 2 life"))
                    .isEqualTo(new Cost.CompoundCost(
                            List.of(new Cost.Tap(SelfSelector.SELF), new Cost.PayLife(new Amount.Exact(2)))));
        }

        @Test
        void singletonStaysBare() {
            // Make sure a singleton list doesn't accidentally wrap in CompoundCost.
            assertThat(parse("{2}{R}")).isInstanceOf(Cost.ManaCost.class);
        }
    }
}
