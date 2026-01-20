package be.imgn.mtg.engine.turn.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.APNAPOrder;

class MultiplayerTurnOrderTest {

    private GameState gameState;
    private Player player1;
    private Player player2;
    private Player player3;
    private Player player4;

    @BeforeEach
    void setUp() {
        gameState = mock(GameState.class);
        player1 = mock(Player.class);
        player2 = mock(Player.class);
        player3 = mock(Player.class);
        player4 = mock(Player.class);

        when(gameState.players()).thenReturn(List.of(player1, player2, player3, player4));
        when(gameState.activePlayer()).thenReturn(player1);
    }

    @Nested
    class APNAPOrdering {

        private APNAPOrder apnapOrder;

        @BeforeEach
        void setUp() {
            apnapOrder = new DefaultAPNAPOrder();
        }

        @Test
        void activePlayerIsFirst() {
            List<Player> order = apnapOrder.getOrder(gameState);

            assertThat(order.getFirst()).isEqualTo(player1);
        }

        @Test
        void playersAreInTurnOrder() {
            when(gameState.activePlayer()).thenReturn(player2);

            List<Player> order = apnapOrder.getOrder(gameState);

            assertThat(order).containsExactly(player2, player3, player4, player1);
        }

        @Test
        void canSpecifyDifferentActivePlayer() {
            List<Player> order = apnapOrder.getOrder(gameState, player3);

            assertThat(order).containsExactly(player3, player4, player1, player2);
        }
    }

    @Nested
    class TurnOrderProgression {

        private DefaultTurnState turnState;

        @BeforeEach
        void setUp() {
            when(gameState.nextPlayerInTurnOrder(player1)).thenReturn(player2);
            when(gameState.nextPlayerInTurnOrder(player2)).thenReturn(player3);
            when(gameState.nextPlayerInTurnOrder(player3)).thenReturn(player4);
            when(gameState.nextPlayerInTurnOrder(player4)).thenReturn(player1);

            turnState = new DefaultTurnState(gameState);
            turnState.initialize(player1);
        }

        @Test
        void turnsProgressInOrder() {
            assertThat(turnState.nextTurn()).isEqualTo(player2);
            assertThat(turnState.nextTurn()).isEqualTo(player3);
            assertThat(turnState.nextTurn()).isEqualTo(player4);
            assertThat(turnState.nextTurn()).isEqualTo(player1);
        }

        @Test
        void turnNumberIncrementsEachTurn() {
            assertThat(turnState.turnNumber()).isZero();

            turnState.nextTurn();
            assertThat(turnState.turnNumber()).isEqualTo(1);

            turnState.nextTurn();
            assertThat(turnState.turnNumber()).isEqualTo(2);
        }
    }
}
