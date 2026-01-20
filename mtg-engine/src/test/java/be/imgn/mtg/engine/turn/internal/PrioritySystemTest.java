package be.imgn.mtg.engine.turn.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.APNAPOrder;
import be.imgn.mtg.engine.turn.PrioritySystem;

class PrioritySystemTest {

    private PrioritySystem prioritySystem;
    private GameState gameState;
    private APNAPOrder apnapOrder;
    private Player player1;
    private Player player2;

    @BeforeEach
    void setUp() {
        gameState = mock(GameState.class);
        apnapOrder = new DefaultAPNAPOrder();
        prioritySystem = new DefaultPrioritySystem(gameState, apnapOrder);

        player1 = mock(Player.class);
        player2 = mock(Player.class);

        when(gameState.players()).thenReturn(List.of(player1, player2));
        when(gameState.activePlayer()).thenReturn(player1);
    }

    @Nested
    class InitialState {

        @Test
        void noOneHasPriorityInitially() {
            assertThat(prioritySystem.currentPriorityHolder()).isNull();
        }

        @Test
        void notAllPassedInitially() {
            assertThat(prioritySystem.allPassed()).isFalse();
        }
    }

    @Nested
    class GivePriority {

        @Test
        void givePrioritySetsHolder() {
            prioritySystem.givePriority(player1);
            assertThat(prioritySystem.currentPriorityHolder()).isEqualTo(player1);
        }

        @Test
        void canGivePriorityToAnyPlayer() {
            prioritySystem.givePriority(player2);
            assertThat(prioritySystem.currentPriorityHolder()).isEqualTo(player2);
        }
    }

    @Nested
    class PassPriority {

        @Test
        void passMovesPriorityToNextPlayer() {
            prioritySystem.givePriority(player1);
            prioritySystem.pass(player1);
            assertThat(prioritySystem.currentPriorityHolder()).isEqualTo(player2);
        }

        @Test
        void passWrapsAround() {
            prioritySystem.givePriority(player2);
            prioritySystem.pass(player2);
            assertThat(prioritySystem.currentPriorityHolder()).isEqualTo(player1);
        }

        @Test
        void cannotPassWithoutPriority() {
            prioritySystem.givePriority(player1);
            assertThatThrownBy(() -> prioritySystem.pass(player2)).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class AllPassed {

        @Test
        void allPassedAfterBothPlayersPassed() {
            prioritySystem.givePriority(player1);
            prioritySystem.pass(player1);
            prioritySystem.pass(player2);
            assertThat(prioritySystem.allPassed()).isTrue();
        }

        @Test
        void notAllPassedWithOnlyOnePassing() {
            prioritySystem.givePriority(player1);
            prioritySystem.pass(player1);
            assertThat(prioritySystem.allPassed()).isFalse();
        }
    }

    @Nested
    class Reset {

        @Test
        void resetClearsPassState() {
            prioritySystem.givePriority(player1);
            prioritySystem.pass(player1);
            prioritySystem.pass(player2);
            assertThat(prioritySystem.allPassed()).isTrue();

            prioritySystem.reset();
            assertThat(prioritySystem.allPassed()).isFalse();
        }

        @Test
        void resetDoesNotClearCurrentHolder() {
            prioritySystem.givePriority(player1);
            prioritySystem.pass(player1);
            prioritySystem.reset();
            assertThat(prioritySystem.currentPriorityHolder()).isEqualTo(player2);
        }
    }

    @Nested
    class ClearPriority {

        @Test
        void clearPriorityRemovesHolder() {
            prioritySystem.givePriority(player1);
            prioritySystem.clearPriority();
            assertThat(prioritySystem.currentPriorityHolder()).isNull();
        }
    }
}
