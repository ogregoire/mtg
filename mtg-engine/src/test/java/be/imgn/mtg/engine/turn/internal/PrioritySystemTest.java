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
import be.imgn.mtg.engine.turn.PrioritySystem;

class PrioritySystemTest {

    private PrioritySystem prioritySystem;
    private GameState gameState;
    private Player player1;
    private Player player2;
    private Player player3;

    @BeforeEach
    void setUp() {
        gameState = mock(GameState.class);
        prioritySystem = new DefaultPrioritySystem(gameState);

        player1 = mock(Player.class);
        player2 = mock(Player.class);
        player3 = mock(Player.class);

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

    @Nested
    class APNAPOrder {

        @Nested
        class TwoPlayerGame {

            @Test
            void activePlayerFirst() {
                var order = prioritySystem.getAPNAPOrder();
                assertThat(order).containsExactly(player1, player2);
            }

            @Test
            void nonActivePlayerSecond() {
                when(gameState.activePlayer()).thenReturn(player2);
                var order = prioritySystem.getAPNAPOrder();
                assertThat(order).containsExactly(player2, player1);
            }
        }

        @Nested
        class MultiplayerGame {

            @BeforeEach
            void setUp() {
                when(gameState.players()).thenReturn(List.of(player1, player2, player3));
            }

            @Test
            void activePlayerFirstThenTurnOrder() {
                when(gameState.activePlayer()).thenReturn(player1);
                var order = prioritySystem.getAPNAPOrder();
                assertThat(order).containsExactly(player1, player2, player3);
            }

            @Test
            void wrapsAroundTurnOrder() {
                when(gameState.activePlayer()).thenReturn(player3);
                var order = prioritySystem.getAPNAPOrder();
                assertThat(order).containsExactly(player3, player1, player2);
            }

            @Test
            void middlePlayerActive() {
                when(gameState.activePlayer()).thenReturn(player2);
                var order = prioritySystem.getAPNAPOrder();
                assertThat(order).containsExactly(player2, player3, player1);
            }
        }

        @Nested
        class SpecificActivePlayer {

            @BeforeEach
            void setUp() {
                when(gameState.players()).thenReturn(List.of(player1, player2, player3));
                when(gameState.activePlayer()).thenReturn(player1);
            }

            @Test
            void canSpecifyDifferentActivePlayer() {
                var order = prioritySystem.getAPNAPOrder(player2);
                assertThat(order).containsExactly(player2, player3, player1);
            }

            @Test
            void throwsForUnknownPlayer() {
                var unknownPlayer = mock(Player.class);
                assertThatThrownBy(() -> prioritySystem.getAPNAPOrder(unknownPlayer))
                        .isInstanceOf(IllegalArgumentException.class);
            }
        }
    }
}
