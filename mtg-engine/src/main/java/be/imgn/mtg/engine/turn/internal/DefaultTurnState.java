package be.imgn.mtg.engine.turn.internal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.TurnState;

/// Default implementation of [TurnState].
///
/// Tracks turn number, active player, turn order, and extra turns queue.
final class DefaultTurnState implements TurnState {

    private final GameState gameState;
    private final Deque<Player> extraTurns;

    private int turnNumber;
    private @Nullable Player activePlayer;

    DefaultTurnState(GameState gameState) {
        this.gameState = gameState;
        this.extraTurns = new ArrayDeque<>();
        this.turnNumber = 0;
    }

    /// Initializes the turn state with the starting player.
    ///
    /// @param startingPlayer the player who takes the first turn
    void initialize(Player startingPlayer) {
        this.activePlayer = startingPlayer;
        gameState.setActivePlayer(startingPlayer);
    }

    @Override
    public int turnNumber() {
        return turnNumber;
    }

    @Override
    public Player activePlayer() {
        if (activePlayer == null) {
            throw new IllegalStateException("Active player not yet initialized");
        }
        return activePlayer;
    }

    @Override
    public Player nextTurn() {
        turnNumber++;

        // Check for extra turns first (LIFO)
        if (!extraTurns.isEmpty()) {
            activePlayer = extraTurns.pop();
        } else {
            if (activePlayer == null) {
                throw new IllegalStateException("Cannot advance turn: active player not initialized");
            }
            activePlayer = gameState.nextPlayerInTurnOrder(activePlayer);
        }

        gameState.setActivePlayer(activePlayer);
        return activePlayer;
    }

    @Override
    public void removePlayer(Player player) {
        // Remove any pending extra turns for this player
        extraTurns.removeIf(p -> p.equals(player));

        // If the removed player was active, move to next player
        // This is handled by the caller, but we track it here
    }

    @Override
    public void addExtraTurn(Player player) {
        // Extra turns are stored in LIFO order (stack)
        extraTurns.push(player);
    }

    @Override
    public Optional<Player> peekExtraTurn() {
        return Optional.ofNullable(extraTurns.peek());
    }

    /// Returns the remaining players in turn order.
    List<Player> remainingPlayers() {
        return gameState.players();
    }
}
