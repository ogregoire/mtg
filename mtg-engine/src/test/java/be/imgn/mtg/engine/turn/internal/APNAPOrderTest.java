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

class APNAPOrderTest {

    private APNAPOrder apnapOrder;
    private GameState gameState;
    private Player player1;
    private Player player2;
    private Player player3;

    @BeforeEach
    void setUp() {
        apnapOrder = new DefaultAPNAPOrder();
        gameState = mock(GameState.class);
        player1 = mock(Player.class);
        player2 = mock(Player.class);
        player3 = mock(Player.class);
    }

    @Nested
    class TwoPlayerGame {

        @BeforeEach
        void setUp() {
            when(gameState.players()).thenReturn(List.of(player1, player2));
            when(gameState.activePlayer()).thenReturn(player1);
        }

        @Test
        void activePlayerFirst() {
            var order = apnapOrder.getOrder(gameState);
            assertThat(order).containsExactly(player1, player2);
        }

        @Test
        void nonActivePlayerSecond() {
            when(gameState.activePlayer()).thenReturn(player2);
            var order = apnapOrder.getOrder(gameState);
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
            var order = apnapOrder.getOrder(gameState);
            assertThat(order).containsExactly(player1, player2, player3);
        }

        @Test
        void wrapsAroundTurnOrder() {
            when(gameState.activePlayer()).thenReturn(player3);
            var order = apnapOrder.getOrder(gameState);
            assertThat(order).containsExactly(player3, player1, player2);
        }

        @Test
        void middlePlayerActive() {
            when(gameState.activePlayer()).thenReturn(player2);
            var order = apnapOrder.getOrder(gameState);
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
            var order = apnapOrder.getOrder(gameState, player2);
            assertThat(order).containsExactly(player2, player3, player1);
        }

        @Test
        void throwsForUnknownPlayer() {
            var unknownPlayer = mock(Player.class);
            assertThatThrownBy(() -> apnapOrder.getOrder(gameState, unknownPlayer))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
