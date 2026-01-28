package be.imgn.mtg.engine.mana.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.Supertype;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.cost.CostContext;
import be.imgn.mtg.engine.game.Choice;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.mana.Mana;
import be.imgn.mtg.engine.mana.ManaPaymentOption;
import be.imgn.mtg.engine.mana.ManaPoolPaymentResult;
import be.imgn.mtg.engine.mana.ManaRestriction;
import be.imgn.mtg.engine.mana.ManaSymbol;
import be.imgn.mtg.engine.mana.ManaType;
import be.imgn.mtg.engine.mana.PaymentResult;
import be.imgn.mtg.engine.object.Card;

/// Additional edge case tests for DefaultManaPool to improve branch coverage.
class DefaultManaPoolEdgeCasesTest {

    private DefaultManaPool pool;
    private Card source;
    private Card snowSource;
    private CostContext context;
    private Player player;

    @BeforeEach
    void setUp() {
        pool = new DefaultManaPool();
        player = mock(Player.class);
        when(player.lifeTotal()).thenReturn(20);
        source = createSource();
        snowSource = createSnowSource();
        context = new CostContext(player, source);
    }

    private Card createSource() {
        return Card.builder().owner(player).controller(player).name("Test Card").build();
    }

    private Card createSnowSource() {
        return Card.builder()
                .owner(player)
                .controller(player)
                .name("Snow Card")
                .supertype(Supertype.SNOW)
                .build();
    }

    @Nested
    class PaymentPriority {

        @Test
        void prioritizesColoredOverGeneric() {
            pool.add(Mana.of(ManaType.GREEN, source));
            pool.add(Mana.of(ManaType.RED, source));
            var cost = DefaultManaCost.parse("{G}{1}");

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
            var payable = (ManaPoolPaymentResult.FullyPayable) result;
            // Should pay {G} with green mana, {1} with red mana
            assertThat(payable.assignments()).hasSize(2);
        }

        @Test
        void prioritizesColorlessOverGeneric() {
            pool.add(Mana.of(ManaType.COLORLESS, source));
            pool.add(Mana.of(ManaType.GREEN, source));
            var cost = DefaultManaCost.parse("{C}{1}");

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
        }

        @Test
        void prioritizesSnowOverGeneric() {
            pool.add(Mana.of(ManaType.GREEN, snowSource));
            pool.add(Mana.of(ManaType.RED, source));
            var cost = new DefaultManaCost(List.of(ManaSymbol.Snow.SNOW, new ManaSymbol.Generic(1)));

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
        }
    }

    @Nested
    class RestrictedManaEdgeCases {

        @Test
        void restrictedManaNotUsedForNonMatching() {
            var restriction = new ManaRestriction.TypeRestriction(Type.CREATURE);
            pool.add(Mana.restricted(ManaType.GREEN, source, restriction));
            pool.add(Mana.of(ManaType.GREEN, source));
            var cost = DefaultManaCost.parse("{G}");

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
            var payable = (ManaPoolPaymentResult.FullyPayable) result;
            // Should use unrestricted mana since source doesn't match restriction
            assertThat(payable.assignments()).hasSize(1);
            assertThat(payable.assignments().getFirst().mana()).isNotInstanceOf(Mana.Restricted.class);
        }

        @Test
        void onlyRestrictedManaNotMatching() {
            var restriction = new ManaRestriction.TypeRestriction(Type.CREATURE);
            pool.add(Mana.restricted(ManaType.GREEN, source, restriction));
            var cost = DefaultManaCost.parse("{G}");

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.NotPayable.class);
        }

        @Test
        void restrictedSnowManaNotMatching() {
            var restriction = new ManaRestriction.TypeRestriction(Type.CREATURE);
            pool.add(Mana.restricted(ManaType.GREEN, snowSource, restriction));
            var cost = new DefaultManaCost(List.of(ManaSymbol.Snow.SNOW));

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.NotPayable.class);
        }
    }

    @Nested
    class HybridPayment {

        @Test
        @SuppressWarnings("unchecked")
        void payHybridWithSecondOption() {
            pool.add(Mana.of(ManaType.BLUE, source));

            when(player.choose(any(Choice.class))).thenAnswer(inv -> {
                Choice<ManaPaymentOption> choice = inv.getArgument(0);
                // Choose second option if available
                if (choice.options().size() > 1) {
                    return List.of(choice.options().get(1).value());
                }
                return List.of(choice.options().getFirst().value());
            });

            var cost = new DefaultManaCost(List.of(ManaSymbol.Hybrid.WHITE_BLUE));
            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.Success.class);
            var success = (PaymentResult.Success) result;
            assertThat(success.manaSpent()).hasSize(1);
            assertThat(success.manaSpent().getFirst().type()).isEqualTo(ManaType.BLUE);
        }

        @Test
        void canPayHybridWithNeitherOption() {
            pool.add(Mana.of(ManaType.GREEN, source));
            var cost = new DefaultManaCost(List.of(ManaSymbol.Hybrid.WHITE_BLUE));

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.NotPayable.class);
        }
    }

    @Nested
    class PhyrexianPayment {

        @Test
        void canPayPhyrexianWithNoManaNoLife() {
            when(player.lifeTotal()).thenReturn(1);
            var cost = new DefaultManaCost(List.of(ManaSymbol.Phyrexian.GREEN_PHYREXIAN));

            assertThat(pool.canPayFully(cost, context)).isFalse();
        }

        @Test
        void canPayMultiplePhyrexianWithLife() {
            when(player.lifeTotal()).thenReturn(6);
            var cost = new DefaultManaCost(List.of(
                    ManaSymbol.Phyrexian.GREEN_PHYREXIAN,
                    ManaSymbol.Phyrexian.RED_PHYREXIAN,
                    ManaSymbol.Phyrexian.BLUE_PHYREXIAN));

            assertThat(pool.canPayFully(cost, context)).isTrue();
        }

        @Test
        void cannotPayMultiplePhyrexianWithInsufficientLife() {
            when(player.lifeTotal()).thenReturn(5);
            var cost = new DefaultManaCost(List.of(
                    ManaSymbol.Phyrexian.GREEN_PHYREXIAN,
                    ManaSymbol.Phyrexian.RED_PHYREXIAN,
                    ManaSymbol.Phyrexian.BLUE_PHYREXIAN));

            assertThat(pool.canPayFully(cost, context)).isFalse();
        }
    }

    @Nested
    class MonoColorHybridPayment {

        @Test
        @SuppressWarnings("unchecked")
        void payMonoColorHybridWithTwoGeneric() {
            pool.add(Mana.of(ManaType.RED, source));
            pool.add(Mana.of(ManaType.BLUE, source));

            when(player.choose(any(Choice.class))).thenAnswer(inv -> {
                Choice<ManaPaymentOption> choice = inv.getArgument(0);
                // Choose generic option
                for (var opt : choice.options()) {
                    if (opt.value() instanceof ManaPaymentOption.PayGeneric) {
                        return List.of(opt.value());
                    }
                }
                return List.of(choice.options().getFirst().value());
            });

            var cost = new DefaultManaCost(List.of(ManaSymbol.MonoColorHybrid.TWO_WHITE));
            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.Success.class);
        }

        @Test
        void cannotPayMonoColorHybridWithOnlyOneGeneric() {
            pool.add(Mana.of(ManaType.RED, source));
            var cost = new DefaultManaCost(List.of(ManaSymbol.MonoColorHybrid.TWO_WHITE));

            var result = pool.canPay(cost, context);

            // Cannot pay because color doesn't match and only 1 generic available
            assertThat(result).isInstanceOf(ManaPoolPaymentResult.NotPayable.class);
        }
    }

    @Nested
    class ColorlessHybridPayment {

        @Test
        @SuppressWarnings("unchecked")
        void payColorlessHybridWithColor() {
            pool.add(Mana.of(ManaType.GREEN, source));

            when(player.choose(any(Choice.class))).thenAnswer(inv -> {
                Choice<ManaPaymentOption> choice = inv.getArgument(0);
                return List.of(choice.options().getFirst().value());
            });

            var cost = new DefaultManaCost(List.of(ManaSymbol.ColorlessHybrid.COLORLESS_GREEN));
            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.Success.class);
        }

        @Test
        void canPayColorlessHybridWithEither() {
            pool.add(Mana.of(ManaType.COLORLESS, source));
            var cost = new DefaultManaCost(List.of(ManaSymbol.ColorlessHybrid.COLORLESS_GREEN));

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
        }
    }

    @Nested
    class HybridPhyrexianPayment {

        @Test
        @SuppressWarnings("unchecked")
        void payHybridPhyrexianWithSecondColor() {
            pool.add(Mana.of(ManaType.BLUE, source));
            when(player.lifeTotal()).thenReturn(10);

            when(player.choose(any(Choice.class))).thenAnswer(inv -> {
                Choice<ManaPaymentOption> choice = inv.getArgument(0);
                // Choose second option if it's a mana payment
                if (choice.options().size() > 1) {
                    var secondOpt = choice.options().get(1).value();
                    if (secondOpt instanceof ManaPaymentOption.PayMana) {
                        return List.of(secondOpt);
                    }
                }
                return List.of(choice.options().getFirst().value());
            });

            var cost = new DefaultManaCost(List.of(ManaSymbol.HybridPhyrexian.WHITE_BLUE_PHYREXIAN));
            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.Success.class);
            var success = (PaymentResult.Success) result;
            assertThat(success.manaSpent()).hasSize(1);
        }

        @Test
        void canPayHybridPhyrexianWithNoManaButLife() {
            when(player.lifeTotal()).thenReturn(5);
            var cost = new DefaultManaCost(List.of(ManaSymbol.HybridPhyrexian.WHITE_BLUE_PHYREXIAN));

            assertThat(pool.canPayFully(cost, context)).isTrue();
        }

        @Test
        void cannotPayHybridPhyrexianWithNoOptions() {
            when(player.lifeTotal()).thenReturn(1);
            var cost = new DefaultManaCost(List.of(ManaSymbol.HybridPhyrexian.WHITE_BLUE_PHYREXIAN));

            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.InsufficientMana.class);
        }
    }

    @Nested
    class VariableCosts {

        @Test
        void variableInMixedCost() {
            pool.add(Mana.of(ManaType.GREEN, source));
            pool.add(Mana.of(ManaType.GREEN, source));
            var cost = DefaultManaCost.parse("{X}{G}{G}");

            var result = pool.canPay(cost, context);

            // X is not paid from pool, so only {G}{G} matters
            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
        }

        @Test
        void multipleVariables() {
            pool.add(Mana.of(ManaType.RED, source));
            var cost = DefaultManaCost.parse("{X}{X}{R}");

            var result = pool.canPay(cost, context);

            // X symbols are not paid from pool, so only {R} matters
            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
        }
    }

    @Nested
    class SnowCosts {

        @Test
        void canPaySnowWithSnowColorless() {
            pool.add(Mana.of(ManaType.COLORLESS, snowSource));
            var cost = new DefaultManaCost(List.of(ManaSymbol.Snow.SNOW));

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
        }

        @Test
        void preferSnowManaForSnowCost() {
            pool.add(Mana.of(ManaType.GREEN, source)); // non-snow
            pool.add(Mana.of(ManaType.RED, snowSource)); // snow
            var cost = new DefaultManaCost(List.of(ManaSymbol.Snow.SNOW));

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
            var payable = (ManaPoolPaymentResult.FullyPayable) result;
            assertThat(payable.assignments().getFirst().mana().isSnow()).isTrue();
        }

        @Test
        void multipleSnowCosts() {
            pool.add(Mana.of(ManaType.GREEN, snowSource));
            pool.add(Mana.of(ManaType.RED, snowSource));
            pool.add(Mana.of(ManaType.BLUE, snowSource));
            var cost = new DefaultManaCost(List.of(ManaSymbol.Snow.SNOW, ManaSymbol.Snow.SNOW, ManaSymbol.Snow.SNOW));

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
        }
    }

    @Nested
    class ComplexScenarios {

        @Test
        void mixedRestrictedAndUnrestrictedMana() {
            var restriction = new ManaRestriction.TypeRestriction(Type.INSTANT);
            pool.add(Mana.restricted(ManaType.BLUE, source, restriction));
            pool.add(Mana.of(ManaType.BLUE, source));
            pool.add(Mana.of(ManaType.RED, source));
            var cost = DefaultManaCost.parse("{2}{U}");

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
        }

        @Test
        void complexCostWithAllSymbolTypes() {
            pool.add(Mana.of(ManaType.WHITE, source));
            pool.add(Mana.of(ManaType.BLUE, source));
            pool.add(Mana.of(ManaType.COLORLESS, source));
            pool.add(Mana.of(ManaType.GREEN, snowSource));
            pool.add(Mana.of(ManaType.RED, source));

            var cost = new DefaultManaCost(List.of(
                    new ManaSymbol.Generic(1),
                    ManaSymbol.Colored.WHITE,
                    ManaSymbol.Hybrid.WHITE_BLUE,
                    ManaSymbol.Colorless.COLORLESS,
                    ManaSymbol.Snow.SNOW));

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
        }

        @Test
        void largeGenericCost() {
            for (int i = 0; i < 10; i++) {
                pool.add(Mana.of(ManaType.GREEN, source));
            }
            var cost = DefaultManaCost.parse("{10}");

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
        }

        @Test
        void partiallyPayableComplex() {
            pool.add(Mana.of(ManaType.GREEN, source));
            pool.add(Mana.of(ManaType.RED, source));
            var cost = DefaultManaCost.parse("{G}{R}{U}{B}");

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.PartiallyPayable.class);
            var partial = (ManaPoolPaymentResult.PartiallyPayable) result;
            assertThat(partial.partialAssignments()).hasSize(2); // G and R paid
            assertThat(partial.unpayable()).hasSize(2); // U and B unpayable
        }
    }

    @Nested
    class MinusGenericEdgeCases {

        @Test
        void minusGenericFromMultipleGenericSymbolsExact() {
            var cost = DefaultManaCost.parse("{3}{2}");

            var result = cost.minusGeneric(5);

            assertThat(result.genericComponent()).isZero();
            assertThat(result.isEmpty()).isTrue();
        }

        @Test
        void minusGenericLeavingOneSymbol() {
            var cost = DefaultManaCost.parse("{5}{2}{G}");

            var result = cost.minusGeneric(6);

            assertThat(result.genericComponent()).isEqualTo(1);
            assertThat(result.symbols()).hasSize(2); // {1} and {G}
        }

        @Test
        void minusGenericFromFirstSymbolPartially() {
            var cost = DefaultManaCost.parse("{5}{3}{G}");

            var result = cost.minusGeneric(4);

            assertThat(result.genericComponent()).isEqualTo(4);
            assertThat(result.symbols()).hasSize(3); // {1}{3}{G}
        }
    }

    @Nested
    class ToStringVariations {

        @Test
        void toStringWithSnow() {
            pool.add(Mana.of(ManaType.GREEN, snowSource));
            pool.add(Mana.of(ManaType.RED, source));

            var str = pool.toString();

            assertThat(str).contains("{G}");
            assertThat(str).contains("{R}");
        }

        @Test
        void toStringWithColorless() {
            pool.add(Mana.of(ManaType.COLORLESS, source));
            pool.add(Mana.of(ManaType.COLORLESS, source));

            var str = pool.toString();

            assertThat(str).contains("{C}");
            assertThat(str).contains("x 2");
        }

        @Test
        void toStringWithAllTypes() {
            pool.add(Mana.of(ManaType.WHITE, source));
            pool.add(Mana.of(ManaType.BLUE, source));
            pool.add(Mana.of(ManaType.BLACK, source));
            pool.add(Mana.of(ManaType.RED, source));
            pool.add(Mana.of(ManaType.GREEN, source));
            pool.add(Mana.of(ManaType.COLORLESS, source));

            var str = pool.toString();

            assertThat(str).contains("{W}");
            assertThat(str).contains("{U}");
            assertThat(str).contains("{B}");
            assertThat(str).contains("{R}");
            assertThat(str).contains("{G}");
            assertThat(str).contains("{C}");
        }
    }
}
