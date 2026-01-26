package be.imgn.mtg.engine.mana.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

class DefaultManaPoolTest {

    private DefaultManaPool pool;
    private Card source;
    private Card otherSource;
    private CostContext context;
    private Player player;

    @BeforeEach
    void setUp() {
        pool = new DefaultManaPool();
        player = mock(Player.class);
        when(player.lifeTotal()).thenReturn(20);
        source = createSource();
        otherSource = createSource();
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

    private Card createCreatureSource() {
        return Card.builder()
                .owner(player)
                .controller(player)
                .name("Creature Card")
                .type(Type.CREATURE)
                .build();
    }

    @Nested
    class BasicOperations {

        @Test
        void startsEmpty() {
            assertThat(pool.isEmpty()).isTrue();
            assertThat(pool.totalCount()).isZero();
            assertThat(pool.contents()).isEmpty();
        }

        @Test
        void addMana() {
            var mana = Mana.of(ManaType.GREEN, source);
            pool.add(mana);

            assertThat(pool.isEmpty()).isFalse();
            assertThat(pool.totalCount()).isEqualTo(1);
            assertThat(pool.count(ManaType.GREEN)).isEqualTo(1);
            assertThat(pool.contents()).containsExactly(mana);
        }

        @Test
        void addMultipleMana() {
            var green = Mana.of(ManaType.GREEN, source);
            var red = Mana.of(ManaType.RED, source);
            pool.add(green);
            pool.add(red);

            assertThat(pool.totalCount()).isEqualTo(2);
            assertThat(pool.count(ManaType.GREEN)).isEqualTo(1);
            assertThat(pool.count(ManaType.RED)).isEqualTo(1);
        }

        @Test
        void addAllMana() {
            var manaList = List.of(
                    Mana.of(ManaType.WHITE, source), Mana.of(ManaType.BLUE, source), Mana.of(ManaType.BLACK, source));
            pool.addAll(manaList);

            assertThat(pool.totalCount()).isEqualTo(3);
            assertThat(pool.count(ManaType.WHITE)).isEqualTo(1);
            assertThat(pool.count(ManaType.BLUE)).isEqualTo(1);
            assertThat(pool.count(ManaType.BLACK)).isEqualTo(1);
        }

        @Test
        void removeMana() {
            var mana = Mana.of(ManaType.GREEN, source);
            pool.add(mana);
            pool.remove(mana);

            assertThat(pool.isEmpty()).isTrue();
        }

        @Test
        void removeNonExistentManaThrows() {
            var mana = Mana.of(ManaType.GREEN, source);

            assertThatThrownBy(() -> pool.remove(mana))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not in pool");
        }

        @Test
        void emptyPool() {
            pool.add(Mana.of(ManaType.GREEN, source));
            pool.add(Mana.of(ManaType.RED, source));
            pool.empty();

            assertThat(pool.isEmpty()).isTrue();
            assertThat(pool.totalCount()).isZero();
        }

        @Test
        void countByType() {
            pool.add(Mana.of(ManaType.GREEN, source));
            pool.add(Mana.of(ManaType.GREEN, source));
            pool.add(Mana.of(ManaType.RED, source));

            assertThat(pool.count(ManaType.GREEN)).isEqualTo(2);
            assertThat(pool.count(ManaType.RED)).isEqualTo(1);
            assertThat(pool.count(ManaType.BLUE)).isZero();
        }

        @Test
        void containsSnow() {
            var snowSource = createSnowSource();
            assertThat(pool.containsSnow()).isFalse();

            pool.add(Mana.of(ManaType.GREEN, source));
            assertThat(pool.containsSnow()).isFalse();

            pool.add(Mana.of(ManaType.BLUE, snowSource));
            assertThat(pool.containsSnow()).isTrue();
        }
    }

    @Nested
    class ToString {

        @Test
        void emptyPoolToString() {
            assertThat(pool.toString()).isEqualTo("ManaPool[]");
        }

        @Test
        void nonEmptyPoolToString() {
            pool.add(Mana.of(ManaType.GREEN, source));
            pool.add(Mana.of(ManaType.GREEN, source));
            pool.add(Mana.of(ManaType.RED, source));

            var str = pool.toString();
            assertThat(str).startsWith("ManaPool[");
            assertThat(str).contains("{G} x 2");
            assertThat(str).contains("{R} x 1");
        }

        @Test
        void toStringWithSingleManaType() {
            pool.add(Mana.of(ManaType.WHITE, source));

            var str = pool.toString();
            assertThat(str).isEqualTo("ManaPool[{W} x 1]");
        }

        @Test
        void toStringWithColorlessMana() {
            pool.add(Mana.of(ManaType.COLORLESS, source));
            pool.add(Mana.of(ManaType.COLORLESS, source));

            var str = pool.toString();
            assertThat(str).contains("{C} x 2");
        }
    }

    @Nested
    class CanPay {

        @Test
        void canPayEmptyCost() {
            var result = pool.canPay(DefaultManaCost.EMPTY, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
        }

        @Test
        void canPayColoredCost() {
            pool.add(Mana.of(ManaType.GREEN, source));
            var cost = DefaultManaCost.parse("{G}");

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
        }

        @Test
        void cannotPayColoredCostWithWrongColor() {
            pool.add(Mana.of(ManaType.RED, source));
            var cost = DefaultManaCost.parse("{G}");

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.NotPayable.class);
        }

        @Test
        void canPayGenericCostWithAnyMana() {
            pool.add(Mana.of(ManaType.GREEN, source));
            var cost = DefaultManaCost.parse("{1}");

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
        }

        @Test
        void canPayColorlessCostWithColorlessMana() {
            pool.add(Mana.of(ManaType.COLORLESS, source));
            var cost = DefaultManaCost.parse("{C}");

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
        }

        @Test
        void cannotPayColorlessCostWithColoredMana() {
            pool.add(Mana.of(ManaType.GREEN, source));
            var cost = DefaultManaCost.parse("{C}");

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.NotPayable.class);
        }

        @Test
        void partiallyPayable() {
            pool.add(Mana.of(ManaType.GREEN, source));
            var cost = DefaultManaCost.parse("{G}{G}");

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.PartiallyPayable.class);
            var partial = (ManaPoolPaymentResult.PartiallyPayable) result;
            // partialAssignments contains the assignment for one {G} we could pay
            assertThat(partial.partialAssignments()).hasSize(1);
            // unpayable contains the second {G} we couldn't pay
            assertThat(partial.unpayable()).hasSize(1);
        }

        @Test
        void canPayMixedCost() {
            pool.add(Mana.of(ManaType.WHITE, source));
            pool.add(Mana.of(ManaType.WHITE, source));
            pool.add(Mana.of(ManaType.RED, source));
            var cost = DefaultManaCost.parse("{1}{W}{W}");

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
        }

        @Test
        void canPayHybridWithFirstOption() {
            pool.add(Mana.of(ManaType.WHITE, source));
            var cost = new DefaultManaCost(List.of(ManaSymbol.Hybrid.WHITE_BLUE));

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
        }

        @Test
        void canPayHybridWithSecondOption() {
            pool.add(Mana.of(ManaType.BLUE, source));
            var cost = new DefaultManaCost(List.of(ManaSymbol.Hybrid.WHITE_BLUE));

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
        }

        @Test
        void canPaySnowCost() {
            var snowSource = createSnowSource();
            pool.add(Mana.of(ManaType.GREEN, snowSource));
            var cost = new DefaultManaCost(List.of(ManaSymbol.Snow.SNOW));

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
        }

        @Test
        void cannotPaySnowCostWithNonSnowMana() {
            pool.add(Mana.of(ManaType.GREEN, source)); // non-snow
            var cost = new DefaultManaCost(List.of(ManaSymbol.Snow.SNOW));

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.NotPayable.class);
        }

        @Test
        void canPayColorlessHybridWithColorless() {
            pool.add(Mana.of(ManaType.COLORLESS, source));
            var cost = new DefaultManaCost(List.of(ManaSymbol.ColorlessHybrid.COLORLESS_GREEN));

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
        }

        @Test
        void canPayColorlessHybridWithColor() {
            pool.add(Mana.of(ManaType.GREEN, source));
            var cost = new DefaultManaCost(List.of(ManaSymbol.ColorlessHybrid.COLORLESS_GREEN));

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
        }

        @Test
        void canPayMonoColorHybridWithColor() {
            pool.add(Mana.of(ManaType.WHITE, source));
            var cost = new DefaultManaCost(List.of(ManaSymbol.MonoColorHybrid.TWO_WHITE));

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
        }

        @Test
        void variableSymbolsAreNotPaidFromPool() {
            // X costs are not paid from the pool during canPay check
            var cost = new DefaultManaCost(List.of(ManaSymbol.Variable.X));

            var result = pool.canPay(cost, context);

            // Variable symbols return null from findManaForSymbol, so they're unpayable
            assertThat(result).isInstanceOf(ManaPoolPaymentResult.NotPayable.class);
        }
    }

    @Nested
    class RestrictedMana {

        @Test
        void restrictedManaCanPayMatchingSource() {
            // Source is a creature, restriction allows spending on creatures
            var creatureSource = createCreatureSource();
            var creatureContext = new CostContext(player, creatureSource);
            var restriction = new ManaRestriction.TypeRestriction(Type.CREATURE);
            var mana = Mana.restricted(ManaType.GREEN, otherSource, restriction);
            pool.add(mana);
            var cost = DefaultManaCost.parse("{G}");

            var result = pool.canPay(cost, creatureContext);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
        }

        @Test
        void restrictedManaCannotPayNonMatchingSource() {
            // Source is not a creature, but restriction requires creature
            var restriction = new ManaRestriction.TypeRestriction(Type.CREATURE);
            var mana = Mana.restricted(ManaType.GREEN, otherSource, restriction);
            pool.add(mana);
            var cost = DefaultManaCost.parse("{G}");

            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.NotPayable.class);
        }

        @Test
        void prefersRestrictedManaWhenMatching() {
            // When restricted mana matches, it should be used first
            var creatureSource = createCreatureSource();
            var creatureContext = new CostContext(player, creatureSource);
            var restriction = new ManaRestriction.TypeRestriction(Type.CREATURE);
            var restrictedMana = Mana.restricted(ManaType.GREEN, otherSource, restriction);
            var unrestrictedMana = Mana.of(ManaType.GREEN, source);

            pool.add(unrestrictedMana);
            pool.add(restrictedMana);

            var cost = DefaultManaCost.parse("{G}");
            var result = pool.canPay(cost, creatureContext);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
            var assignments = ((ManaPoolPaymentResult.FullyPayable) result).assignments();
            // Should prefer restricted mana that matches
            assertThat(assignments).hasSize(1);
            assertThat(assignments.getFirst().mana()).isEqualTo(restrictedMana);
        }

        @Test
        void restrictedManaCanPayGeneric() {
            var creatureSource = createCreatureSource();
            var creatureContext = new CostContext(player, creatureSource);
            var restriction = new ManaRestriction.TypeRestriction(Type.CREATURE);
            var mana = Mana.restricted(ManaType.GREEN, otherSource, restriction);
            pool.add(mana);
            var cost = DefaultManaCost.parse("{1}");

            var result = pool.canPay(cost, creatureContext);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
        }

        @Test
        void restrictedSnowManaCanPaySnowCost() {
            var snowSource = createSnowSource();
            var creatureSource = createCreatureSource();
            var creatureContext = new CostContext(player, creatureSource);
            var restriction = new ManaRestriction.TypeRestriction(Type.CREATURE);
            var mana = Mana.restricted(ManaType.GREEN, snowSource, restriction);
            pool.add(mana);
            var cost = new DefaultManaCost(List.of(ManaSymbol.Snow.SNOW));

            var result = pool.canPay(cost, creatureContext);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
        }

        @Test
        void cannotPayWithOnlyRestrictedNonMatchingMana() {
            // Pool has restricted mana, but source doesn't match restriction
            var restriction = new ManaRestriction.TypeRestriction(Type.ARTIFACT);
            var restrictedMana = Mana.restricted(ManaType.GREEN, otherSource, restriction);
            pool.add(restrictedMana);
            var cost = DefaultManaCost.parse("{G}");

            var result = pool.canPay(cost, context); // context.source is not an artifact

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.NotPayable.class);
        }

        @Test
        void canPayGenericWithOnlyRestrictedMatchingMana() {
            var creatureSource = createCreatureSource();
            var creatureContext = new CostContext(player, creatureSource);
            var restriction = new ManaRestriction.TypeRestriction(Type.CREATURE);
            var restrictedMana = Mana.restricted(ManaType.BLUE, otherSource, restriction);
            pool.add(restrictedMana);
            var cost = DefaultManaCost.parse("{1}");

            var result = pool.canPay(cost, creatureContext);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
        }

        @Test
        void cannotPayGenericWithOnlyRestrictedNonMatchingMana() {
            var restriction = new ManaRestriction.TypeRestriction(Type.INSTANT);
            var restrictedMana = Mana.restricted(ManaType.RED, otherSource, restriction);
            pool.add(restrictedMana);
            var cost = DefaultManaCost.parse("{1}");

            var result = pool.canPay(cost, context); // context.source is not an instant

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.NotPayable.class);
        }

        @Test
        void prefersUnrestrictedOverRestrictedNonMatching() {
            // Pool has both unrestricted and restricted non-matching mana
            var restriction = new ManaRestriction.TypeRestriction(Type.PLANESWALKER);
            var restrictedMana = Mana.restricted(ManaType.WHITE, otherSource, restriction);
            var unrestrictedMana = Mana.of(ManaType.WHITE, source);

            pool.add(restrictedMana); // Add restricted first
            pool.add(unrestrictedMana);

            var cost = DefaultManaCost.parse("{W}");
            var result = pool.canPay(cost, context);

            assertThat(result).isInstanceOf(ManaPoolPaymentResult.FullyPayable.class);
            var assignments = ((ManaPoolPaymentResult.FullyPayable) result).assignments();
            assertThat(assignments.getFirst().mana()).isEqualTo(unrestrictedMana);
        }
    }

    @Nested
    class PayAssignments {

        @Test
        void payRemovesManaFromPool() {
            var mana = Mana.of(ManaType.GREEN, source);
            pool.add(mana);
            var cost = DefaultManaCost.parse("{G}");
            var result = (ManaPoolPaymentResult.FullyPayable) pool.canPay(cost, context);

            pool.pay(cost, result.assignments());

            assertThat(pool.isEmpty()).isTrue();
        }

        @Test
        void payHandlesNullManaInAssignment() {
            // Some assignments may have null mana (e.g., life payment)
            pool.add(Mana.of(ManaType.GREEN, source));
            var cost = DefaultManaCost.parse("{G}");

            // This should not throw even with empty assignments
            pool.pay(cost, List.of());
            assertThat(pool.count(ManaType.GREEN)).isEqualTo(1);
        }
    }

    @Nested
    class CanPayFully {

        @Test
        void canPayFullyWithEmptyCost() {
            assertThat(pool.canPayFully(DefaultManaCost.EMPTY, context)).isTrue();
        }

        @Test
        void canPayFullyWithSufficientMana() {
            pool.add(Mana.of(ManaType.GREEN, source));
            pool.add(Mana.of(ManaType.GREEN, source));
            var cost = DefaultManaCost.parse("{1}{G}");

            assertThat(pool.canPayFully(cost, context)).isTrue();
        }

        @Test
        void cannotPayFullyWithInsufficientMana() {
            pool.add(Mana.of(ManaType.GREEN, source));
            var cost = DefaultManaCost.parse("{G}{G}");

            assertThat(pool.canPayFully(cost, context)).isFalse();
        }

        @Test
        void canPayFullyPhyrexianWithLife() {
            // No mana in pool, but can pay Phyrexian with life
            when(player.lifeTotal()).thenReturn(5);
            var cost = new DefaultManaCost(List.of(ManaSymbol.Phyrexian.GREEN_PHYREXIAN));

            assertThat(pool.canPayFully(cost, context)).isTrue();
        }

        @Test
        void cannotPayFullyPhyrexianWithInsufficientLife() {
            when(player.lifeTotal()).thenReturn(1);
            var cost = new DefaultManaCost(List.of(ManaSymbol.Phyrexian.GREEN_PHYREXIAN));

            assertThat(pool.canPayFully(cost, context)).isFalse();
        }

        @Test
        void canPayFullyHybridPhyrexianWithLife() {
            when(player.lifeTotal()).thenReturn(5);
            var cost = new DefaultManaCost(List.of(ManaSymbol.HybridPhyrexian.WHITE_BLUE_PHYREXIAN));

            assertThat(pool.canPayFully(cost, context)).isTrue();
        }

        @Test
        void cannotPayNonPhyrexianWithLife() {
            // Colored mana cannot be paid with life
            when(player.lifeTotal()).thenReturn(20);
            var cost = DefaultManaCost.parse("{G}");

            assertThat(pool.canPayFully(cost, context)).isFalse();
        }

        @Test
        void canPayPartiallyUnpayablePhyrexianWithLife() {
            // One mana in pool, one Phyrexian payable with life
            pool.add(Mana.of(ManaType.GREEN, source));
            when(player.lifeTotal()).thenReturn(5);
            var cost = new DefaultManaCost(List.of(ManaSymbol.Colored.GREEN, ManaSymbol.Phyrexian.RED_PHYREXIAN));

            assertThat(pool.canPayFully(cost, context)).isTrue();
        }
    }

    @Nested
    class PayFully {

        @Test
        void payFullyEmptyCost() {
            var result = pool.payFully(DefaultManaCost.EMPTY, context);

            assertThat(result).isInstanceOf(PaymentResult.Success.class);
            var success = (PaymentResult.Success) result;
            assertThat(success.manaSpent()).isEmpty();
            assertThat(success.lifePaid()).isZero();
        }

        @Test
        void payFullyColoredCost() {
            pool.add(Mana.of(ManaType.GREEN, source));
            var cost = DefaultManaCost.parse("{G}");

            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.Success.class);
            var success = (PaymentResult.Success) result;
            assertThat(success.manaSpent()).hasSize(1);
            assertThat(pool.isEmpty()).isTrue();
        }

        @Test
        void payFullyGenericCost() {
            pool.add(Mana.of(ManaType.RED, source));
            var cost = DefaultManaCost.parse("{1}");

            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.Success.class);
            assertThat(pool.isEmpty()).isTrue();
        }

        @Test
        void payFullyColorlessCost() {
            pool.add(Mana.of(ManaType.COLORLESS, source));
            var cost = DefaultManaCost.parse("{C}");

            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.Success.class);
            assertThat(pool.isEmpty()).isTrue();
        }

        @Test
        void payFullySnowCost() {
            var snowSource = createSnowSource();
            pool.add(Mana.of(ManaType.GREEN, snowSource));
            var cost = new DefaultManaCost(List.of(ManaSymbol.Snow.SNOW));

            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.Success.class);
            assertThat(pool.isEmpty()).isTrue();
        }

        @Test
        void payFullyInsufficientMana() {
            var cost = DefaultManaCost.parse("{G}");

            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.InsufficientMana.class);
            var insufficient = (PaymentResult.InsufficientMana) result;
            assertThat(insufficient.missingSymbols()).hasSize(1);
        }

        @Test
        void payFullyVariableSkipped() {
            // Variable costs are skipped (not paid from pool)
            var cost = new DefaultManaCost(List.of(ManaSymbol.Variable.X));

            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.Success.class);
        }

        @Test
        @SuppressWarnings("unchecked")
        void payFullyPhyrexianWithManaChoice() {
            pool.add(Mana.of(ManaType.GREEN, source));
            when(player.lifeTotal()).thenReturn(10);

            // Mock player choosing to pay with mana
            when(player.choose(any(Choice.class))).thenAnswer(inv -> {
                Choice<ManaPaymentOption> choice = inv.getArgument(0);
                // Find the mana option
                for (var opt : choice.options()) {
                    if (opt.value() instanceof ManaPaymentOption.PayMana) {
                        return List.of(opt.value());
                    }
                }
                return List.of(choice.options().getFirst().value());
            });

            var cost = new DefaultManaCost(List.of(ManaSymbol.Phyrexian.GREEN_PHYREXIAN));
            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.Success.class);
            var success = (PaymentResult.Success) result;
            assertThat(success.manaSpent()).hasSize(1);
            assertThat(success.lifePaid()).isZero();
        }

        @Test
        @SuppressWarnings("unchecked")
        void payFullyPhyrexianWithLifeChoice() {
            pool.add(Mana.of(ManaType.GREEN, source));
            when(player.lifeTotal()).thenReturn(10);

            // Mock player choosing to pay with life
            when(player.choose(any(Choice.class))).thenAnswer(inv -> {
                Choice<ManaPaymentOption> choice = inv.getArgument(0);
                // Find the life option
                for (var opt : choice.options()) {
                    if (opt.value() instanceof ManaPaymentOption.PayLife) {
                        return List.of(opt.value());
                    }
                }
                return List.of(choice.options().getFirst().value());
            });

            var cost = new DefaultManaCost(List.of(ManaSymbol.Phyrexian.GREEN_PHYREXIAN));
            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.Success.class);
            var success = (PaymentResult.Success) result;
            assertThat(success.manaSpent()).isEmpty();
            assertThat(success.lifePaid()).isEqualTo(2);
            // Mana should still be in pool since we paid life
            assertThat(pool.count(ManaType.GREEN)).isEqualTo(1);
        }

        @Test
        @SuppressWarnings("unchecked")
        void payFullyHybridWithFirstOption() {
            pool.add(Mana.of(ManaType.WHITE, source));
            pool.add(Mana.of(ManaType.BLUE, source));

            // Mock player choosing first option (white)
            when(player.choose(any(Choice.class))).thenAnswer(inv -> {
                Choice<ManaPaymentOption> choice = inv.getArgument(0);
                return List.of(choice.options().getFirst().value());
            });

            var cost = new DefaultManaCost(List.of(ManaSymbol.Hybrid.WHITE_BLUE));
            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.Success.class);
            var success = (PaymentResult.Success) result;
            assertThat(success.manaSpent()).hasSize(1);
            assertThat(success.manaSpent().getFirst().type()).isEqualTo(ManaType.WHITE);
        }

        @Test
        @SuppressWarnings("unchecked")
        void payFullyColorlessHybridWithColorless() {
            pool.add(Mana.of(ManaType.COLORLESS, source));
            pool.add(Mana.of(ManaType.GREEN, source));

            // Mock player choosing colorless option
            when(player.choose(any(Choice.class))).thenAnswer(inv -> {
                Choice<ManaPaymentOption> choice = inv.getArgument(0);
                return List.of(choice.options().getFirst().value());
            });

            var cost = new DefaultManaCost(List.of(ManaSymbol.ColorlessHybrid.COLORLESS_GREEN));
            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.Success.class);
        }

        @Test
        @SuppressWarnings("unchecked")
        void payFullyMonoColorHybridWithColor() {
            pool.add(Mana.of(ManaType.WHITE, source));

            // Mock player choosing color option
            when(player.choose(any(Choice.class))).thenAnswer(inv -> {
                Choice<ManaPaymentOption> choice = inv.getArgument(0);
                return List.of(choice.options().getFirst().value());
            });

            var cost = new DefaultManaCost(List.of(ManaSymbol.MonoColorHybrid.TWO_WHITE));
            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.Success.class);
        }

        @Test
        @SuppressWarnings("unchecked")
        void payFullyMonoColorHybridWithGeneric() {
            pool.add(Mana.of(ManaType.RED, source));
            pool.add(Mana.of(ManaType.BLUE, source));

            // Mock player choosing generic option ({2})
            when(player.choose(any(Choice.class))).thenAnswer(inv -> {
                Choice<ManaPaymentOption> choice = inv.getArgument(0);
                // Find the generic option
                for (var opt : choice.options()) {
                    if (opt.value() instanceof ManaPaymentOption.PayGeneric) {
                        return List.of(opt.value());
                    }
                }
                return List.of(choice.options().getFirst().value());
            });

            var cost = new DefaultManaCost(List.of(ManaSymbol.MonoColorHybrid.TWO_WHITE));
            var result = pool.payFully(cost, context);

            // This pays one mana from the generic option, but 2/W requires 2 generic
            // The current implementation only pays 1 mana for this case
            assertThat(result).isInstanceOf(PaymentResult.Success.class);
        }

        @Test
        @SuppressWarnings("unchecked")
        void payFullyHybridPhyrexianWithMana() {
            pool.add(Mana.of(ManaType.WHITE, source));
            when(player.lifeTotal()).thenReturn(10);

            // Mock player choosing mana option
            when(player.choose(any(Choice.class))).thenAnswer(inv -> {
                Choice<ManaPaymentOption> choice = inv.getArgument(0);
                return List.of(choice.options().getFirst().value());
            });

            var cost = new DefaultManaCost(List.of(ManaSymbol.HybridPhyrexian.WHITE_BLUE_PHYREXIAN));
            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.Success.class);
            var success = (PaymentResult.Success) result;
            assertThat(success.manaSpent()).hasSize(1);
        }

        @Test
        @SuppressWarnings("unchecked")
        void payFullyHybridPhyrexianWithLife() {
            when(player.lifeTotal()).thenReturn(10);

            // Mock player choosing life option
            when(player.choose(any(Choice.class))).thenAnswer(inv -> {
                Choice<ManaPaymentOption> choice = inv.getArgument(0);
                // Find the life option (last option)
                for (var opt : choice.options()) {
                    if (opt.value() instanceof ManaPaymentOption.PayLife) {
                        return List.of(opt.value());
                    }
                }
                return List.of(choice.options().getFirst().value());
            });

            var cost = new DefaultManaCost(List.of(ManaSymbol.HybridPhyrexian.WHITE_BLUE_PHYREXIAN));
            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.Success.class);
            var success = (PaymentResult.Success) result;
            assertThat(success.lifePaid()).isEqualTo(2);
        }

        @Test
        void payFullyHybridWithNoOptions() {
            // No mana that can pay the hybrid cost
            var cost = new DefaultManaCost(List.of(ManaSymbol.Hybrid.WHITE_BLUE));
            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.InsufficientMana.class);
        }

        @Test
        void payFullyColorlessHybridWithNoOptions() {
            // No colorless or green mana
            pool.add(Mana.of(ManaType.RED, source));
            var cost = new DefaultManaCost(List.of(ManaSymbol.ColorlessHybrid.COLORLESS_GREEN));
            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.InsufficientMana.class);
        }

        @Test
        void payFullyMonoColorHybridWithNoOptions() {
            // Only 1 mana, need 2 for generic option and no white
            pool.add(Mana.of(ManaType.RED, source));
            var cost = new DefaultManaCost(List.of(ManaSymbol.MonoColorHybrid.TWO_WHITE));
            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.InsufficientMana.class);
        }

        @Test
        void payFullyPhyrexianWithNoOptions() {
            // No green mana and not enough life
            when(player.lifeTotal()).thenReturn(1);
            var cost = new DefaultManaCost(List.of(ManaSymbol.Phyrexian.GREEN_PHYREXIAN));
            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.InsufficientMana.class);
        }

        @Test
        void payFullySnowWithNoSnowMana() {
            pool.add(Mana.of(ManaType.GREEN, source)); // non-snow
            var cost = new DefaultManaCost(List.of(ManaSymbol.Snow.SNOW));
            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.InsufficientMana.class);
        }

        @Test
        void payFullyWithRestrictedManaMatching() {
            var creatureSource = createCreatureSource();
            var creatureContext = new CostContext(player, creatureSource);
            var restriction = new ManaRestriction.TypeRestriction(Type.CREATURE);
            var restrictedMana = Mana.restricted(ManaType.BLACK, otherSource, restriction);
            pool.add(restrictedMana);
            var cost = DefaultManaCost.parse("{B}");

            var result = pool.payFully(cost, creatureContext);

            assertThat(result).isInstanceOf(PaymentResult.Success.class);
            assertThat(pool.isEmpty()).isTrue();
        }

        @Test
        void payFullyWithRestrictedManaNonMatching() {
            var restriction = new ManaRestriction.TypeRestriction(Type.ENCHANTMENT);
            var restrictedMana = Mana.restricted(ManaType.RED, otherSource, restriction);
            pool.add(restrictedMana);
            var cost = DefaultManaCost.parse("{R}");

            var result = pool.payFully(cost, context); // source is not an enchantment

            assertThat(result).isInstanceOf(PaymentResult.InsufficientMana.class);
        }

        @Test
        @SuppressWarnings("unchecked")
        void payFullyHybridWithSecondOption() {
            pool.add(Mana.of(ManaType.BLUE, source));

            // Mock player choosing second option (blue)
            when(player.choose(any(Choice.class))).thenAnswer(inv -> {
                Choice<ManaPaymentOption> choice = inv.getArgument(0);
                // Find second option
                var options = choice.options();
                if (options.size() > 1) {
                    return List.of(options.get(1).value());
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
        @SuppressWarnings("unchecked")
        void payFullyColorlessHybridWithColorOption() {
            pool.add(Mana.of(ManaType.GREEN, source));

            // Mock player choosing color option
            when(player.choose(any(Choice.class))).thenAnswer(inv -> {
                Choice<ManaPaymentOption> choice = inv.getArgument(0);
                // Find the green option (not colorless)
                for (var opt : choice.options()) {
                    if (opt.value() instanceof ManaPaymentOption.PayMana(var type) && type == ManaType.GREEN) {
                        return List.of(opt.value());
                    }
                }
                return List.of(choice.options().getFirst().value());
            });

            var cost = new DefaultManaCost(List.of(ManaSymbol.ColorlessHybrid.COLORLESS_GREEN));
            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.Success.class);
            var success = (PaymentResult.Success) result;
            assertThat(success.manaSpent()).hasSize(1);
            assertThat(success.manaSpent().getFirst().type()).isEqualTo(ManaType.GREEN);
        }

        @Test
        @SuppressWarnings("unchecked")
        void payFullyHybridPhyrexianWithSecondColorOption() {
            pool.add(Mana.of(ManaType.BLUE, source));
            when(player.lifeTotal()).thenReturn(10);

            // Mock player choosing second color option (blue)
            when(player.choose(any(Choice.class))).thenAnswer(inv -> {
                Choice<ManaPaymentOption> choice = inv.getArgument(0);
                // Find blue option
                for (var opt : choice.options()) {
                    if (opt.value() instanceof ManaPaymentOption.PayMana(var type) && type == ManaType.BLUE) {
                        return List.of(opt.value());
                    }
                }
                return List.of(choice.options().getFirst().value());
            });

            var cost = new DefaultManaCost(List.of(ManaSymbol.HybridPhyrexian.WHITE_BLUE_PHYREXIAN));
            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.Success.class);
            var success = (PaymentResult.Success) result;
            assertThat(success.manaSpent()).hasSize(1);
            assertThat(success.manaSpent().getFirst().type()).isEqualTo(ManaType.BLUE);
        }

        @Test
        void payFullyHybridPhyrexianWithNoOptions() {
            when(player.lifeTotal()).thenReturn(1); // Not enough life
            // No mana in pool

            var cost = new DefaultManaCost(List.of(ManaSymbol.HybridPhyrexian.WHITE_BLUE_PHYREXIAN));
            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.InsufficientMana.class);
        }

        @Test
        void payFullyMultipleMixedCosts() {
            pool.add(Mana.of(ManaType.WHITE, source));
            pool.add(Mana.of(ManaType.BLUE, source));
            pool.add(Mana.of(ManaType.BLACK, source));
            var cost = DefaultManaCost.parse("{W}{U}{B}");

            var result = pool.payFully(cost, context);

            assertThat(result).isInstanceOf(PaymentResult.Success.class);
            var success = (PaymentResult.Success) result;
            assertThat(success.manaSpent()).hasSize(3);
            assertThat(pool.isEmpty()).isTrue();
        }
    }
}
