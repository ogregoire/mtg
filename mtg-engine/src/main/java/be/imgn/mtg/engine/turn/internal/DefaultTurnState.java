package be.imgn.mtg.engine.turn.internal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.PhaseType;
import be.imgn.mtg.engine.turn.StepType;
import be.imgn.mtg.engine.turn.TurnState;
import be.imgn.mtg.engine.util.Multiset;

/// Default implementation of [TurnState].
///
/// Tracks turn number, active player, turn order, extra turns queue, occurrences, and skips.
final class DefaultTurnState implements TurnState {

    private final GameState gameState;
    private final Deque<Player> extraTurns;
    private final Multiset<PhaseType> phaseCounts;
    private final Multiset<StepType> stepCounts;
    private final Set<PhaseType> skippedPhases;
    private final Set<StepType> skippedSteps;
    private final Set<Player> playersWithSkippedTurn;

    private int turnNumber;
    private @Nullable Player activePlayer;

    DefaultTurnState(GameState gameState) {
        this.gameState = gameState;
        this.extraTurns = new ArrayDeque<>();
        this.phaseCounts = Multiset.newEnumMultiset(PhaseType.class);
        this.stepCounts = Multiset.newEnumMultiset(StepType.class);
        this.skippedPhases = EnumSet.noneOf(PhaseType.class);
        this.skippedSteps = EnumSet.noneOf(StepType.class);
        this.playersWithSkippedTurn = new HashSet<>();
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

    // ===== Occurrence tracking =====

    @Override
    public int incrementOccurrence(PhaseType phase) {
        phaseCounts.add(phase);
        return phaseCounts.count(phase);
    }

    @Override
    public int incrementOccurrence(StepType step) {
        stepCounts.add(step);
        return stepCounts.count(step);
    }

    @Override
    public int occurrence(PhaseType phase) {
        return phaseCounts.count(phase);
    }

    @Override
    public int occurrence(StepType step) {
        return stepCounts.count(step);
    }

    @Override
    public void resetOccurrences() {
        phaseCounts.clear();
        stepCounts.clear();
    }

    // ===== Skip tracking =====

    @Override
    public boolean isSkipped(PhaseType phaseType) {
        return skippedPhases.contains(phaseType);
    }

    @Override
    public boolean isSkipped(StepType stepType) {
        return skippedSteps.contains(stepType);
    }

    @Override
    public boolean shouldSkipNextTurn(Player player) {
        return playersWithSkippedTurn.contains(player);
    }

    @Override
    public void skipPhase(PhaseType phaseType) {
        skippedPhases.add(phaseType);
    }

    @Override
    public void skipStep(StepType stepType) {
        skippedSteps.add(stepType);
    }

    @Override
    public void skipNextTurn(Player player) {
        playersWithSkippedTurn.add(player);
    }

    @Override
    public void clearTurnSkip(Player player) {
        playersWithSkippedTurn.remove(player);
    }

    @Override
    public void resetSkipsForNewTurn() {
        skippedPhases.clear();
        skippedSteps.clear();
    }
}
