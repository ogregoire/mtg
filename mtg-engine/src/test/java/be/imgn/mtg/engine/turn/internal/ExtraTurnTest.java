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

class ExtraTurnTest {

    private GameState gameState;
    private DefaultTurnState turnState;

    private Player player1;
    private Player player2;
    private Player player3;

    @BeforeEach
    void setUp() {
        gameState = mock(GameState.class);
        player1 = mock(Player.class);
        player2 = mock(Player.class);
        player3 = mock(Player.class);

        when(gameState.players()).thenReturn(List.of(player1, player2, player3));
        when(gameState.nextPlayerInTurnOrder(player1)).thenReturn(player2);
        when(gameState.nextPlayerInTurnOrder(player2)).thenReturn(player3);
        when(gameState.nextPlayerInTurnOrder(player3)).thenReturn(player1);

        turnState = new DefaultTurnState(gameState);
        turnState.initialize(player1);
    }

    @Nested
    class ExtraTurnQueue {

        @Test
        void extraTurnTakesEffectBeforeNormalTurn() {
            turnState.addExtraTurn(player2);

            // First nextTurn should be the extra turn
            Player next = turnState.nextTurn();

            assertThat(next).isEqualTo(player2);
        }

        @Test
        void extraTurnsAreLIFO() {
            turnState.addExtraTurn(player2);
            turnState.addExtraTurn(player3);

            // Player3's extra turn should be first (LIFO)
            assertThat(turnState.nextTurn()).isEqualTo(player3);
            // Then player2's extra turn
            assertThat(turnState.nextTurn()).isEqualTo(player2);
        }

        @Test
        void normalTurnResumeAfterExtraTurns() {
            turnState.addExtraTurn(player3);

            // Take the extra turn
            turnState.nextTurn();

            // Next turn follows from the player who took the extra turn (player3 -> player1)
            // Note: This differs from strict MTG rules where turn order would resume from player2
            assertThat(turnState.nextTurn()).isEqualTo(player1);
        }
    }

    @Nested
    class PeekExtraTurn {

        @Test
        void returnsEmptyWhenNoExtraTurns() {
            assertThat(turnState.peekExtraTurn()).isEmpty();
        }

        @Test
        void returnsNextExtraTurnWithoutConsuming() {
            turnState.addExtraTurn(player2);

            assertThat(turnState.peekExtraTurn()).contains(player2);
            assertThat(turnState.peekExtraTurn()).contains(player2); // Still there
        }
    }

    @Nested
    class RemovePlayerExtraTurns {

        @Test
        void removesAllExtraTurnsForPlayer() {
            turnState.addExtraTurn(player2);
            turnState.addExtraTurn(player3);
            turnState.addExtraTurn(player2);

            turnState.removePlayer(player2);

            // Only player3's turn should remain
            assertThat(turnState.peekExtraTurn()).contains(player3);
        }
    }
}
