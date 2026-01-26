package be.imgn.mtg.engine.game.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.cost.CostContext;
import be.imgn.mtg.engine.game.Choice;
import be.imgn.mtg.engine.game.ChoiceHandler;
import be.imgn.mtg.engine.game.Option;
import be.imgn.mtg.engine.game.PlayerData;
import be.imgn.mtg.engine.game.PlayerId;
import be.imgn.mtg.engine.game.Team;
import be.imgn.mtg.engine.mana.ManaCost;
import be.imgn.mtg.engine.mana.ManaPool;
import be.imgn.mtg.engine.mana.PaymentResult;
import be.imgn.mtg.engine.zone.Graveyard;
import be.imgn.mtg.engine.zone.Hand;
import be.imgn.mtg.engine.zone.Library;

class PlayerImplTest {

    record TestPlayerId(String id) implements PlayerId {}

    record TestTeam(String name) implements Team {}

    private PlayerData playerData;
    private Library library;
    private Hand hand;
    private Graveyard graveyard;
    private ManaPool manaPool;
    private ChoiceHandler choiceHandler;
    private PlayerImpl player;

    @BeforeEach
    void setUp() {
        playerData = new PlayerData(new TestPlayerId("p1"), new TestTeam("team1"));
        library = mock(Library.class);
        hand = mock(Hand.class);
        graveyard = mock(Graveyard.class);
        manaPool = mock(ManaPool.class);
        choiceHandler = mock(ChoiceHandler.class);

        player = new PlayerImpl(playerData, library, hand, graveyard, manaPool, choiceHandler);
    }

    @Nested
    class PlayerDataAccess {

        @Test
        void returnsPlayerData() {
            assertThat(player.data()).isEqualTo(playerData);
        }
    }

    @Nested
    class ZoneAccess {

        @Test
        void returnsLibrary() {
            assertThat(player.library()).isEqualTo(library);
        }

        @Test
        void returnsHand() {
            assertThat(player.hand()).isEqualTo(hand);
        }

        @Test
        void returnsGraveyard() {
            assertThat(player.graveyard()).isEqualTo(graveyard);
        }

        @Test
        void returnsManaPool() {
            assertThat(player.manaPool()).isEqualTo(manaPool);
        }
    }

    @Nested
    class LifeTotal {

        @Test
        void startsAt20() {
            assertThat(player.lifeTotal()).isEqualTo(20);
        }

        @Test
        void gainLifeIncreasesTotal() {
            player.gainLife(5);

            assertThat(player.lifeTotal()).isEqualTo(25);
        }

        @Test
        void gainLifeMultipleTimesAccumulates() {
            player.gainLife(3);
            player.gainLife(7);

            assertThat(player.lifeTotal()).isEqualTo(30);
        }

        @Test
        void gainLifeWithZeroThrowsException() {
            assertThatThrownBy(() -> player.gainLife(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be positive");
        }

        @Test
        void gainLifeWithNegativeThrowsException() {
            assertThatThrownBy(() -> player.gainLife(-5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be positive");
        }

        @Test
        void loseLifeDecreasesTotal() {
            player.loseLife(5);

            assertThat(player.lifeTotal()).isEqualTo(15);
        }

        @Test
        void loseLifeMultipleTimesAccumulates() {
            player.loseLife(3);
            player.loseLife(7);

            assertThat(player.lifeTotal()).isEqualTo(10);
        }

        @Test
        void loseLifeCanGoBelowZero() {
            player.loseLife(25);

            assertThat(player.lifeTotal()).isEqualTo(-5);
        }

        @Test
        void loseLifeWithZeroThrowsException() {
            assertThatThrownBy(() -> player.loseLife(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be positive");
        }

        @Test
        void loseLifeWithNegativeThrowsException() {
            assertThatThrownBy(() -> player.loseLife(-5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be positive");
        }

        @Test
        void setLifeTotalChangesValue() {
            player.setLifeTotal(10);

            assertThat(player.lifeTotal()).isEqualTo(10);
        }

        @Test
        void setLifeTotalCanBeZero() {
            player.setLifeTotal(0);

            assertThat(player.lifeTotal()).isEqualTo(0);
        }

        @Test
        void setLifeTotalCanBeNegative() {
            player.setLifeTotal(-10);

            assertThat(player.lifeTotal()).isEqualTo(-10);
        }

        @Test
        void setLifeTotalCanBeVeryHigh() {
            player.setLifeTotal(1000);

            assertThat(player.lifeTotal()).isEqualTo(1000);
        }
    }

    @Nested
    class ManaCostPayment {

        private CostContext context;

        @BeforeEach
        void setUp() {
            context = mock(CostContext.class);
        }

        @Test
        void canPayEmptyCostReturnsTrue() {
            var cost = mock(ManaCost.class);
            when(cost.isEmpty()).thenReturn(true);

            assertThat(player.canPay(cost, context)).isTrue();
        }

        @Test
        void canPayDelegatesToManaPool() {
            var cost = mock(ManaCost.class);
            when(cost.isEmpty()).thenReturn(false);
            when(manaPool.canPayFully(cost, context)).thenReturn(true);

            assertThat(player.canPay(cost, context)).isTrue();
        }

        @Test
        void canPayReturnsFalseWhenManaPoolCannotPay() {
            var cost = mock(ManaCost.class);
            when(cost.isEmpty()).thenReturn(false);
            when(manaPool.canPayFully(cost, context)).thenReturn(false);

            assertThat(player.canPay(cost, context)).isFalse();
        }

        @Test
        void payEmptyCostDoesNothing() {
            var cost = mock(ManaCost.class);
            when(cost.isEmpty()).thenReturn(true);

            player.pay(cost, context);

            assertThat(player.lifeTotal()).isEqualTo(20);
        }

        @Test
        void paySuccessfullyWithNoLifeCost() {
            var cost = mock(ManaCost.class);
            when(cost.isEmpty()).thenReturn(false);
            when(manaPool.payFully(cost, context)).thenReturn(new PaymentResult.Success(List.of(), 0));

            player.pay(cost, context);

            assertThat(player.lifeTotal()).isEqualTo(20);
        }

        @Test
        void paySuccessfullyWithLifeCost() {
            var cost = mock(ManaCost.class);
            when(cost.isEmpty()).thenReturn(false);
            when(manaPool.payFully(cost, context)).thenReturn(new PaymentResult.Success(List.of(), 4));

            player.pay(cost, context);

            assertThat(player.lifeTotal()).isEqualTo(16);
        }

        @Test
        void payThrowsWhenInsufficientMana() {
            var cost = mock(ManaCost.class);
            when(cost.isEmpty()).thenReturn(false);
            when(manaPool.payFully(cost, context)).thenReturn(new PaymentResult.InsufficientMana(List.of()));

            assertThatThrownBy(() -> player.pay(cost, context))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot pay mana cost");
        }

        @Test
        void payThrowsWhenInsufficientLife() {
            var cost = mock(ManaCost.class);
            when(cost.isEmpty()).thenReturn(false);
            when(manaPool.payFully(cost, context)).thenReturn(new PaymentResult.InsufficientLife(5, 2));

            assertThatThrownBy(() -> player.pay(cost, context))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not enough life");
        }
    }

    @Nested
    class Choices {

        @Test
        void chooseDelegatesToChoiceHandler() {
            var choice = Choice.oneOf(List.of("option1", "option2", "option3"), "Pick one");
            var option = new Option<>("option2", "option2");
            doReturn(CompletableFuture.completedFuture(List.of(option)))
                    .when(choiceHandler)
                    .choose(any());

            var result = player.choose(choice);

            assertThat(result).containsExactly("option2");
        }

        @Test
        void chooseUnwrapsOptions() {
            var choice = Choice.oneOf(List.of("A", "B", "C"), "Choose");
            var optionB = new Option<>("B", "B");
            doReturn(CompletableFuture.completedFuture(List.of(optionB)))
                    .when(choiceHandler)
                    .choose(any());

            var result = player.choose(choice);

            assertThat(result).containsExactly("B");
        }

        @Test
        void chooseValidatesSelectionIsFromAvailableOptions() {
            var choice = Choice.oneOf(List.of("A", "B"), "Choose");
            var invalidOption = new Option<>("C", "C");
            doReturn(CompletableFuture.completedFuture(List.of(invalidOption)))
                    .when(choiceHandler)
                    .choose(any());

            assertThatThrownBy(() -> player.choose(choice))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid selection")
                    .hasMessageContaining("not in the available options");
        }

        @Test
        void chooseValidatesSelectionCount() {
            var choice = Choice.nOf(List.of("A", "B", "C"), 2, "Pick two");
            var option = new Option<>("A", "A");
            doReturn(CompletableFuture.completedFuture(List.of(option)))
                    .when(choiceHandler)
                    .choose(any());

            assertThatThrownBy(() -> player.choose(choice))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid selection count");
        }

        @Test
        void chooseHandlesMultipleSelections() {
            var choice = Choice.nOf(List.of("X", "Y", "Z"), 2, "Pick two");
            var opt1 = new Option<>("X", "X");
            var opt2 = new Option<>("Z", "Z");
            doReturn(CompletableFuture.completedFuture(List.of(opt1, opt2)))
                    .when(choiceHandler)
                    .choose(any());

            var result = player.choose(choice);

            assertThat(result).containsExactly("X", "Z");
        }
    }

    @Nested
    class LeftTheGame {

        @Test
        void defaultsToFalse() {
            assertThat(player.hasLeftTheGame()).isFalse();
        }

        // Note: PlayerImpl doesn't expose a method to set leftTheGame to true,
        // so we can only test the default value here. The actual mutation would
        // happen through other game systems.
    }
}
