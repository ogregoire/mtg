package be.imgn.mtg.engine.mana.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.Colors;
import be.imgn.mtg.engine.mana.ManaSymbol;

class DefaultManaCostTest {

    @Nested
    class EmptyCost {

        @Test
        void emptyConstant() {
            assertThat(DefaultManaCost.EMPTY.isEmpty()).isTrue();
            assertThat(DefaultManaCost.EMPTY.symbols()).isEmpty();
            assertThat(DefaultManaCost.EMPTY.manaValue()).isZero();
            assertThat(DefaultManaCost.EMPTY.genericComponent()).isZero();
            assertThat(DefaultManaCost.EMPTY.hasVariable()).isFalse();
            assertThat(DefaultManaCost.EMPTY.variableCount()).isZero();
        }

        @Test
        void emptyDescription() {
            assertThat(DefaultManaCost.EMPTY.description()).isEqualTo("{0}");
        }

        @Test
        void emptyToString() {
            assertThat(DefaultManaCost.EMPTY.toString()).isEqualTo("{0}");
        }

        @Test
        void emptyColors() {
            assertThat(DefaultManaCost.EMPTY.colors()).isEqualTo(Colors.empty());
        }
    }

    @Nested
    class Parse {

        @Test
        void parseEmptyString() {
            var cost = DefaultManaCost.parse("");
            assertThat(cost.isEmpty()).isTrue();
            assertThat(cost).isSameAs(DefaultManaCost.EMPTY);
        }

        @Test
        void parseColoredCost() {
            var cost = DefaultManaCost.parse("{G}");
            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.manaValue()).isEqualTo(1);
        }

        @Test
        void parseGenericCost() {
            var cost = DefaultManaCost.parse("{3}");
            assertThat(cost.symbols()).hasSize(1);
            assertThat(cost.manaValue()).isEqualTo(3);
            assertThat(cost.genericComponent()).isEqualTo(3);
        }

        @Test
        void parseMixedCost() {
            var cost = DefaultManaCost.parse("{2}{W}{W}");
            assertThat(cost.symbols()).hasSize(3);
            assertThat(cost.manaValue()).isEqualTo(4);
            assertThat(cost.genericComponent()).isEqualTo(2);
        }

        @Test
        void parseVariableCost() {
            var cost = DefaultManaCost.parse("{X}{R}");
            assertThat(cost.hasVariable()).isTrue();
            assertThat(cost.variableCount()).isEqualTo(1);
            assertThat(cost.manaValue()).isEqualTo(1); // X has 0 mana value
        }
    }

    @Nested
    class ManaValue {

        @Test
        void coloredSymbolsContributeOne() {
            var cost = new DefaultManaCost(List.of(ManaSymbol.Colored.WHITE, ManaSymbol.Colored.BLUE));
            assertThat(cost.manaValue()).isEqualTo(2);
        }

        @Test
        void genericSymbolsContributeAmount() {
            var cost = new DefaultManaCost(List.of(new ManaSymbol.Generic(5)));
            assertThat(cost.manaValue()).isEqualTo(5);
        }

        @Test
        void hybridSymbolsContributeOne() {
            var cost = new DefaultManaCost(List.of(ManaSymbol.Hybrid.WHITE_BLUE));
            assertThat(cost.manaValue()).isEqualTo(1);
        }

        @Test
        void monoColorHybridSymbolsContributeTwo() {
            var cost = new DefaultManaCost(List.of(ManaSymbol.MonoColorHybrid.TWO_WHITE));
            assertThat(cost.manaValue()).isEqualTo(2);
        }

        @Test
        void phyrexianSymbolsContributeOne() {
            var cost = new DefaultManaCost(List.of(ManaSymbol.Phyrexian.GREEN_PHYREXIAN));
            assertThat(cost.manaValue()).isEqualTo(1);
        }

        @Test
        void variableSymbolsContributeZero() {
            var cost = new DefaultManaCost(List.of(ManaSymbol.Variable.X));
            assertThat(cost.manaValue()).isZero();
        }

        @Test
        void snowSymbolsContributeOne() {
            var cost = new DefaultManaCost(List.of(ManaSymbol.Snow.SNOW));
            assertThat(cost.manaValue()).isEqualTo(1);
        }

        @Test
        void colorlessSymbolsContributeOne() {
            var cost = new DefaultManaCost(List.of(ManaSymbol.Colorless.COLORLESS));
            assertThat(cost.manaValue()).isEqualTo(1);
        }
    }

    @Nested
    class ColorTests {

        @Test
        void monoColorCost() {
            var cost = new DefaultManaCost(List.of(ManaSymbol.Colored.GREEN));
            assertThat(cost.colors()).isEqualTo(Colors.of(Color.GREEN));
        }

        @Test
        void multiColorCost() {
            var cost = new DefaultManaCost(List.of(ManaSymbol.Colored.WHITE, ManaSymbol.Colored.BLUE));
            var expected = Colors.builder().add(Color.WHITE).add(Color.BLUE).build();
            assertThat(cost.colors()).isEqualTo(expected);
        }

        @Test
        void hybridAddsColors() {
            var cost = new DefaultManaCost(List.of(ManaSymbol.Hybrid.RED_GREEN));
            var expected = Colors.builder().add(Color.RED).add(Color.GREEN).build();
            assertThat(cost.colors()).isEqualTo(expected);
        }

        @Test
        void colorlessDoesNotAddColors() {
            var cost = new DefaultManaCost(List.of(ManaSymbol.Colorless.COLORLESS));
            assertThat(cost.colors()).isEqualTo(Colors.empty());
        }

        @Test
        void genericDoesNotAddColors() {
            var cost = new DefaultManaCost(List.of(new ManaSymbol.Generic(3)));
            assertThat(cost.colors()).isEqualTo(Colors.empty());
        }
    }

    @Nested
    class GenericComponent {

        @Test
        void singleGenericSymbol() {
            var cost = new DefaultManaCost(List.of(new ManaSymbol.Generic(3)));
            assertThat(cost.genericComponent()).isEqualTo(3);
        }

        @Test
        void multipleGenericSymbols() {
            var cost = new DefaultManaCost(List.of(new ManaSymbol.Generic(2), new ManaSymbol.Generic(3)));
            assertThat(cost.genericComponent()).isEqualTo(5);
        }

        @Test
        void noGenericSymbols() {
            var cost = new DefaultManaCost(List.of(ManaSymbol.Colored.WHITE));
            assertThat(cost.genericComponent()).isZero();
        }

        @Test
        void zeroGenericSymbol() {
            var cost = new DefaultManaCost(List.of(new ManaSymbol.Generic(0)));
            assertThat(cost.genericComponent()).isZero();
        }
    }

    @Nested
    class Variable {

        @Test
        void noVariableSymbols() {
            var cost = new DefaultManaCost(List.of(ManaSymbol.Colored.RED));
            assertThat(cost.hasVariable()).isFalse();
            assertThat(cost.variableCount()).isZero();
        }

        @Test
        void singleVariableSymbol() {
            var cost = new DefaultManaCost(List.of(ManaSymbol.Variable.X));
            assertThat(cost.hasVariable()).isTrue();
            assertThat(cost.variableCount()).isEqualTo(1);
        }

        @Test
        void multipleVariableSymbols() {
            var cost =
                    new DefaultManaCost(List.of(ManaSymbol.Variable.X, ManaSymbol.Variable.X, ManaSymbol.Colored.RED));
            assertThat(cost.hasVariable()).isTrue();
            assertThat(cost.variableCount()).isEqualTo(2);
        }
    }

    @Nested
    class Plus {

        @Test
        void addTwoEmptyCosts() {
            var result = DefaultManaCost.EMPTY.plus(DefaultManaCost.EMPTY);
            assertThat(result.isEmpty()).isTrue();
        }

        @Test
        void addToEmptyCost() {
            var cost = new DefaultManaCost(List.of(ManaSymbol.Colored.GREEN));
            var result = DefaultManaCost.EMPTY.plus(cost);
            assertThat(result.symbols()).containsExactly(ManaSymbol.Colored.GREEN);
        }

        @Test
        void addTwoCosts() {
            var cost1 = new DefaultManaCost(List.of(ManaSymbol.Colored.WHITE));
            var cost2 = new DefaultManaCost(List.of(ManaSymbol.Colored.BLUE));
            var result = cost1.plus(cost2);
            assertThat(result.symbols()).containsExactly(ManaSymbol.Colored.WHITE, ManaSymbol.Colored.BLUE);
        }

        @Test
        void addPreservesManaValue() {
            var cost1 = new DefaultManaCost(List.of(new ManaSymbol.Generic(2)));
            var cost2 = new DefaultManaCost(List.of(ManaSymbol.Colored.RED));
            var result = cost1.plus(cost2);
            assertThat(result.manaValue()).isEqualTo(3);
        }
    }

    @Nested
    class MinusGeneric {

        @Test
        void minusZero() {
            var cost = new DefaultManaCost(List.of(new ManaSymbol.Generic(3)));
            var result = cost.minusGeneric(0);
            assertThat(result).isSameAs(cost);
        }

        @Test
        void minusNegative() {
            var cost = new DefaultManaCost(List.of(new ManaSymbol.Generic(3)));
            var result = cost.minusGeneric(-1);
            assertThat(result).isSameAs(cost);
        }

        @Test
        void minusLessThanGeneric() {
            var cost = new DefaultManaCost(List.of(new ManaSymbol.Generic(5)));
            var result = cost.minusGeneric(2);
            assertThat(result.symbols()).hasSize(1);
            assertThat(result.genericComponent()).isEqualTo(3);
        }

        @Test
        void minusExactGenericAmount() {
            var cost = new DefaultManaCost(List.of(new ManaSymbol.Generic(3)));
            var result = cost.minusGeneric(3);
            assertThat(result.symbols()).isEmpty();
        }

        @Test
        void minusMoreThanGeneric() {
            var cost = new DefaultManaCost(List.of(new ManaSymbol.Generic(2)));
            var result = cost.minusGeneric(5);
            assertThat(result.symbols()).isEmpty();
        }

        @Test
        void doesNotRemoveColoredSymbols() {
            var cost = new DefaultManaCost(
                    List.of(new ManaSymbol.Generic(2), ManaSymbol.Colored.WHITE, ManaSymbol.Colored.BLUE));
            var result = cost.minusGeneric(2);
            assertThat(result.symbols()).containsExactly(ManaSymbol.Colored.WHITE, ManaSymbol.Colored.BLUE);
        }

        @Test
        void removesFromMultipleGenericSymbols() {
            var cost = new DefaultManaCost(List.of(new ManaSymbol.Generic(2), new ManaSymbol.Generic(3)));
            var result = cost.minusGeneric(4);
            assertThat(result.genericComponent()).isEqualTo(1);
        }

        @Test
        void removesAllGenericKeepsOthers() {
            var cost = new DefaultManaCost(
                    List.of(new ManaSymbol.Generic(2), ManaSymbol.Colored.RED, new ManaSymbol.Generic(1)));
            var result = cost.minusGeneric(10);
            assertThat(result.symbols()).containsExactly(ManaSymbol.Colored.RED);
        }

        @Test
        void preservesOrderWhenPartiallyRemoving() {
            var cost = new DefaultManaCost(
                    List.of(ManaSymbol.Colored.WHITE, new ManaSymbol.Generic(5), ManaSymbol.Colored.BLUE));
            var result = cost.minusGeneric(2);
            assertThat(result.symbols())
                    .containsExactly(ManaSymbol.Colored.WHITE, new ManaSymbol.Generic(3), ManaSymbol.Colored.BLUE);
        }

        @Test
        void handlesMultipleGenericSymbolsCorrectly() {
            // {3}{2} minus 4 = {1}
            var cost = new DefaultManaCost(List.of(new ManaSymbol.Generic(3), new ManaSymbol.Generic(2)));
            var result = cost.minusGeneric(4);
            assertThat(result.genericComponent()).isEqualTo(1);
            assertThat(result.symbols()).hasSize(1);
        }
    }

    @Nested
    class Description {

        @Test
        void singleColoredSymbol() {
            var cost = new DefaultManaCost(List.of(ManaSymbol.Colored.GREEN));
            assertThat(cost.description()).isEqualTo("{G}");
        }

        @Test
        void multipleSymbols() {
            var cost = new DefaultManaCost(List.of(new ManaSymbol.Generic(2), ManaSymbol.Colored.WHITE));
            assertThat(cost.description()).isEqualTo("{2}{W}");
        }

        @Test
        void complexCost() {
            var cost = new DefaultManaCost(List.of(
                    ManaSymbol.Variable.X, new ManaSymbol.Generic(1), ManaSymbol.Colored.RED, ManaSymbol.Colored.RED));
            assertThat(cost.description()).isEqualTo("{X}{1}{R}{R}");
        }
    }

    @Nested
    class EqualsAndHashCode {

        @Test
        void equalCostsAreEqual() {
            var cost1 = new DefaultManaCost(List.of(ManaSymbol.Colored.WHITE));
            var cost2 = new DefaultManaCost(List.of(ManaSymbol.Colored.WHITE));
            assertThat(cost1).isEqualTo(cost2);
            assertThat(cost1.hashCode()).isEqualTo(cost2.hashCode());
        }

        @Test
        void differentCostsAreNotEqual() {
            var cost1 = new DefaultManaCost(List.of(ManaSymbol.Colored.WHITE));
            var cost2 = new DefaultManaCost(List.of(ManaSymbol.Colored.BLUE));
            assertThat(cost1).isNotEqualTo(cost2);
        }

        @Test
        void emptyAndNonEmptyAreNotEqual() {
            var cost = new DefaultManaCost(List.of(ManaSymbol.Colored.GREEN));
            assertThat(cost).isNotEqualTo(DefaultManaCost.EMPTY);
        }

        @Test
        void notEqualToNull() {
            var cost = new DefaultManaCost(List.of(ManaSymbol.Colored.RED));
            assertThat(cost).isNotEqualTo(null);
        }

        @Test
        void notEqualToOtherType() {
            var cost = new DefaultManaCost(List.of(ManaSymbol.Colored.BLACK));
            assertThat(cost).isNotEqualTo("not a cost");
        }
    }

    @Nested
    class ToString {

        @Test
        void toStringMatchesDescription() {
            var cost = new DefaultManaCost(List.of(new ManaSymbol.Generic(3), ManaSymbol.Colored.GREEN));
            assertThat(cost.toString()).isEqualTo(cost.description());
        }
    }
}
