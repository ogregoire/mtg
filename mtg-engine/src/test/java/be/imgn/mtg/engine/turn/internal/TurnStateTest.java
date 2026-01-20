package be.imgn.mtg.engine.turn.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.state.GameState;

class TurnStateTest {

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
    }

    @Nested
    class Initialization {

        @Test
        void turnNumberStartsAtZero() {
            assertThat(turnState.turnNumber()).isZero();
        }

        @Test
        void activePlayerThrowsBeforeInitialized() {
            assertThatThrownBy(() -> turnState.activePlayer()).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void initializeSetsActivePlayer() {
            turnState.initialize(player1);
            assertThat(turnState.activePlayer()).isEqualTo(player1);
        }

        @Test
        void initializeUpdatesGameState() {
            turnState.initialize(player1);
            verify(gameState).setActivePlayer(player1);
        }
    }

    @Nested
    class NextTurn {

        @BeforeEach
        void initialize() {
            turnState.initialize(player1);
        }

        @Test
        void incrementsTurnNumber() {
            turnState.nextTurn();
            assertThat(turnState.turnNumber()).isEqualTo(1);

            turnState.nextTurn();
            assertThat(turnState.turnNumber()).isEqualTo(2);
        }

        @Test
        void advancesToNextPlayerInTurnOrder() {
            turnState.nextTurn(); // Turn 1 - player1 initialized, advances to player2
            assertThat(turnState.activePlayer()).isEqualTo(player2);

            turnState.nextTurn(); // Turn 2
            assertThat(turnState.activePlayer()).isEqualTo(player3);

            turnState.nextTurn(); // Turn 3 - wraps around
            assertThat(turnState.activePlayer()).isEqualTo(player1);
        }

        @Test
        void returnsNewActivePlayer() {
            Player newActive = turnState.nextTurn();
            assertThat(newActive).isEqualTo(player2);
        }

        @Test
        void updatesGameStateActivePlayer() {
            turnState.nextTurn();
            verify(gameState).setActivePlayer(player2);
        }
    }

    @Nested
    class ExtraTurns {

        @BeforeEach
        void initialize() {
            turnState.initialize(player1);
        }

        @Test
        void noExtraTurnsByDefault() {
            assertThat(turnState.peekExtraTurn()).isEmpty();
        }

        @Test
        void addExtraTurnMakesItAvailable() {
            turnState.addExtraTurn(player2);
            assertThat(turnState.peekExtraTurn()).contains(player2);
        }

        @Test
        void extraTurnsAreLIFO() {
            turnState.addExtraTurn(player2);
            turnState.addExtraTurn(player3);

            // Player3's extra turn should be taken first (LIFO)
            assertThat(turnState.peekExtraTurn()).contains(player3);
        }

        @Test
        void nextTurnUsesExtraTurnFirst() {
            turnState.addExtraTurn(player3);

            Player activePlayer = turnState.nextTurn();

            assertThat(activePlayer).isEqualTo(player3);
            assertThat(turnState.peekExtraTurn()).isEmpty();
        }

        @Test
        void extraTurnOrderingWithMultipleExtraTurns() {
            turnState.addExtraTurn(player2);
            turnState.addExtraTurn(player3);

            // First extra turn (LIFO - player3)
            assertThat(turnState.nextTurn()).isEqualTo(player3);

            // Second extra turn (player2)
            assertThat(turnState.nextTurn()).isEqualTo(player2);

            // Normal turn order resumes (after player1, next is player2)
            assertThat(turnState.nextTurn()).isEqualTo(player3);
        }
    }

    @Nested
    class RemovePlayer {

        @BeforeEach
        void initialize() {
            turnState.initialize(player1);
        }

        @Test
        void removesPlayerExtraTurns() {
            turnState.addExtraTurn(player2);
            turnState.addExtraTurn(player3);
            turnState.addExtraTurn(player2);

            turnState.removePlayer(player2);

            // Only player3's extra turn should remain
            assertThat(turnState.peekExtraTurn()).contains(player3);
        }
    }
}
