package be.imgn.mtg.engine.turn.internal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.event.EventBus;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.game.PlayerLeftEvent;
import be.imgn.mtg.engine.turn.Phase;
import be.imgn.mtg.engine.turn.PhaseEndedEvent;
import be.imgn.mtg.engine.turn.PhaseStartedEvent;
import be.imgn.mtg.engine.turn.Step;
import be.imgn.mtg.engine.turn.StepEndedEvent;
import be.imgn.mtg.engine.turn.StepStartedEvent;
import be.imgn.mtg.engine.turn.Turn;
import be.imgn.mtg.engine.turn.TurnEndedEvent;
import be.imgn.mtg.engine.turn.TurnStartedEvent;
import be.imgn.mtg.engine.turn.TurnTracker;
import be.imgn.mtg.engine.util.Multiset;
import be.imgn.mtg.engine.util.SetMultimap;
import be.imgn.mtg.engine.zone.Stack;

/// Default implementation of [TurnTracker].
///
/// This implementation drives the game loop through priority passing.
/// When all players pass with an empty stack, it automatically advances.
public final class DefaultTurnTracker implements TurnTracker {

    private final List<Player> players;
    private final Stack stack;
    private final EventBus eventBus;
    private final List<StateBasedActionChecker> sbaCheckers;

    // Turn state
    private int turnNumber;
    private @Nullable Player activePlayer;
    private boolean gameStarted;

    // Phase/step state
    private @Nullable Phase currentPhase;
    private @Nullable Step currentStep;
    private int currentPhaseOccurrence;
    private int currentStepOccurrenceInPhase;

    // Turn structure - remaining phases/steps for this turn
    private final Deque<PhaseEntry> remainingPhases = new ArrayDeque<>();
    private final Deque<Step> remainingStepsInPhase = new ArrayDeque<>();

    // Phase/step occurrence tracking (per turn)
    private final Multiset<Phase> phaseOccurrences = Multiset.newEnumMultiset(Phase.class);
    private final Multiset<Step> stepOccurrencesInTurn = Multiset.newEnumMultiset(Step.class);
    private final Multiset<Step> stepOccurrencesInPhase = Multiset.newEnumMultiset(Step.class);

    // Extra turns (LIFO)
    private final Deque<Player> extraTurns = new ArrayDeque<>();
    // Track the last player who took a normal (non-extra) turn, for resuming after extra turns
    private @Nullable Player lastNormalTurnPlayer;

    // Skip tracking
    private final Multiset<Player> skipNextTurn = Multiset.newHashMultiset();
    private final SetMultimap<Player, Step> skipAllSteps = SetMultimap.newHashEnumSetMultimap(Step.class);
    private final Map<Player, Multiset<Step>> skipNextSteps = new HashMap<>();
    private final SetMultimap<Player, Phase> skipPhaseNextTurn = SetMultimap.newHashEnumSetMultimap(Phase.class);
    private final Set<Phase> skipPhaseThisTurn = EnumSet.noneOf(Phase.class);

    // Priority state
    private @Nullable Player priorityHolder;
    private final Set<Player> passedPriority = new HashSet<>();
    private int apnapIndex;

    // First turn draw skip
    private boolean skipFirstDraw;
    private @Nullable Player firstPlayer;

    // Players still in the game (updated via PlayerLeftEvent)
    private final Set<Player> playersInGame;

    /// Functional interface for state-based action checking.
    @FunctionalInterface
    public interface StateBasedActionChecker {
        /// Returns true if any SBAs were applied.
        boolean checkAndApply();
    }

    /// Very high priority for PlayerLeftEvent subscription.
    /// Lower than HIGH_PRIORITY (0) but higher than DEFAULT_PRIORITY (100).
    private static final int PLAYER_LEFT_PRIORITY = 10;

    public DefaultTurnTracker(
            List<Player> players, Stack stack, EventBus eventBus, List<StateBasedActionChecker> sbaCheckers) {
        this.players = List.copyOf(players);
        this.stack = stack;
        this.eventBus = eventBus;
        this.sbaCheckers = List.copyOf(sbaCheckers);
        this.playersInGame = new HashSet<>(players);

        // Subscribe to PlayerLeftEvent at very high priority to update internal state
        eventBus.subscribe(PlayerLeftEvent.class, PLAYER_LEFT_PRIORITY, this::onPlayerLeft);
    }

    private void onPlayerLeft(PlayerLeftEvent event) {
        playersInGame.remove(event.player());
    }

    @Override
    public void startGame(Player startingPlayer) {
        if (gameStarted) {
            throw new IllegalStateException("Game has already been started");
        }
        if (!players.contains(startingPlayer)) {
            throw new IllegalArgumentException("Starting player is not in the game");
        }

        gameStarted = true;
        turnNumber = 0;
        // Don't set activePlayer yet - let startNextTurn() set it via determineNextActivePlayer()
        skipFirstDraw = true;
        firstPlayer = startingPlayer;

        // Start the first turn
        startNextTurn();
    }

    @Override
    public Player activePlayer() {
        if (activePlayer == null) {
            throw new IllegalStateException("Game has not been started");
        }
        return activePlayer;
    }

    @Override
    public Turn currentTurn() {
        return new Turn(turnNumber, activePlayer());
    }

    @Override
    public @Nullable Phase currentPhase() {
        return currentPhase;
    }

    @Override
    public @Nullable Step currentStep() {
        return currentStep;
    }

    @Override
    public int currentPhaseOccurrence() {
        return currentPhaseOccurrence;
    }

    @Override
    public int currentStepOccurrenceInPhase() {
        return currentStepOccurrenceInPhase;
    }

    @Override
    public void addExtraTurn(Player player) {
        extraTurns.push(player);
    }

    @Override
    public void skipNextTurn(Player player) {
        skipNextTurn.add(player, 1);
    }

    @Override
    public void skipAllSteps(Player player, Step step) {
        skipAllSteps.put(player, step);
    }

    @Override
    public void skipNextOccurrence(Player player, Step step) {
        skipNextSteps
                .computeIfAbsent(player, _ -> Multiset.newEnumMultiset(Step.class))
                .add(step, 1);
    }

    @Override
    public void skipPhaseNextTurn(Player player, Phase phase) {
        skipPhaseNextTurn.put(player, phase);
    }

    @Override
    public void skipPhaseThisTurn(Phase phase) {
        skipPhaseThisTurn.add(phase);
    }

    @Override
    public void insertPhaseAfterCurrent(Phase phase) {
        // Insert at front of remaining phases
        remainingPhases.addFirst(new PhaseEntry(phase, buildStepsForPhase(phase)));
    }

    @Override
    public void insertStepAfterCurrent(Step step) {
        // Insert at front of remaining steps in current phase
        remainingStepsInPhase.addFirst(step);
    }

    @Override
    public void endTurnEarly() {
        // Fire ended events for current step/phase
        fireStepEndedIfPresent();
        firePhaseEndedIfPresent();

        // Clear the stack (exile all)
        while (!stack.isEmpty()) {
            stack.pop();
            // TODO: Actually exile, not just remove
        }

        // Clear remaining phases and steps
        remainingPhases.clear();
        remainingStepsInPhase.clear();

        // Go directly to cleanup
        currentPhase = Phase.ENDING;
        currentPhaseOccurrence = phaseOccurrences.add(Phase.ENDING, 1) + 1;
        stepOccurrencesInPhase.clear();

        // Fire phase started for ending phase
        eventBus.post(new PhaseStartedEvent(Phase.ENDING, currentPhaseOccurrence));

        runCleanupLoop();

        // Fire ended events for ending phase and turn
        firePhaseEndedIfPresent();
        eventBus.post(new TurnEndedEvent(turnNumber, activePlayer()));

        // Start next turn
        startNextTurn();
    }

    @Override
    public boolean hasPriority(Player player) {
        return player.equals(priorityHolder);
    }

    @Override
    public void passPriority(Player player) {
        if (!player.equals(priorityHolder)) {
            throw new IllegalStateException("Player does not have priority");
        }

        passedPriority.add(player);

        // Check if all players still in the game have passed
        if (passedPriority.containsAll(playersInGame())) {
            handleAllPlayersPassed();
        } else {
            // Move to next player in APNAP order
            advancePriorityToNextPlayer();
        }
    }

    private void grantPriority(Player player) {
        // Reset pass state when priority is explicitly granted
        passedPriority.clear();

        // Check SBAs before actually granting priority
        checkStateBasedActions();

        priorityHolder = player;
        apnapIndex = players.indexOf(player);
    }

    // ===== Private implementation =====

    private void fireStepEndedIfPresent() {
        if (currentStep != null) {
            eventBus.post(new StepEndedEvent(currentStep, currentStepOccurrenceInPhase));
        }
    }

    private void firePhaseEndedIfPresent() {
        if (currentPhase != null) {
            eventBus.post(new PhaseEndedEvent(currentPhase, currentPhaseOccurrence));
        }
    }

    private void startNextTurn() {
        // Check if this will be an extra turn or a normal turn
        var isExtraTurn = !extraTurns.isEmpty();

        // Determine next active player
        var nextPlayer = determineNextActivePlayer();

        // Check if turn should be skipped
        if (skipNextTurn.count(nextPlayer) > 0) {
            skipNextTurn.remove(nextPlayer, 1);
            // For skip tracking, we need to update lastNormalTurnPlayer if this was a normal turn
            // (even though it's being skipped)
            if (!isExtraTurn) {
                lastNormalTurnPlayer = nextPlayer;
            }
            // Turn is skipped, move to the next one
            startNextTurn();
            return;
        }

        turnNumber++;
        activePlayer = nextPlayer;

        // Track last normal turn player (for resuming after extra turns)
        if (!isExtraTurn) {
            lastNormalTurnPlayer = nextPlayer;
        }

        // Reset per-turn state
        phaseOccurrences.clear();
        stepOccurrencesInTurn.clear();
        skipPhaseThisTurn.clear();

        // Consume "skip phase next turn" for this player
        var phasesToSkip = skipPhaseNextTurn.removeAll(nextPlayer);

        // Build the turn structure
        buildTurnStructure(phasesToSkip);

        // Fire turn started event
        eventBus.post(new TurnStartedEvent(turnNumber, nextPlayer));

        // Start the first phase
        advanceToNextPhase();
    }

    private Player determineNextActivePlayer() {
        // Check for extra turns first (LIFO)
        // Skip extra turns for players who have left the game
        while (!extraTurns.isEmpty()) {
            var extraTurnPlayer = extraTurns.pop();
            if (playersInGame.contains(extraTurnPlayer)) {
                return extraTurnPlayer;
            }
            // Player left, discard this extra turn and check next
        }

        // First turn - use the starting player
        if (lastNormalTurnPlayer == null) {
            return Objects.requireNonNull(firstPlayer, "firstPlayer must be set before first turn");
        }

        // Normal turn order - next player after the last normal turn player
        // (not activePlayer, which may have been an extra turn player)
        // Skip players who have left the game
        var currentIndex = players.indexOf(lastNormalTurnPlayer);
        for (int i = 1; i <= players.size(); i++) {
            var nextIndex = (currentIndex + i) % players.size();
            var nextPlayer = players.get(nextIndex);
            if (playersInGame.contains(nextPlayer)) {
                return nextPlayer;
            }
        }

        // All players have left - this shouldn't happen in a valid game
        throw new IllegalStateException("No players remaining in the game");
    }

    private void buildTurnStructure(Set<Phase> phasesToSkip) {
        remainingPhases.clear();

        // Standard turn structure
        addPhaseIfNotSkipped(Phase.BEGINNING, phasesToSkip);
        addPhaseIfNotSkipped(Phase.MAIN, phasesToSkip); // First main
        addPhaseIfNotSkipped(Phase.COMBAT, phasesToSkip);
        addPhaseIfNotSkipped(Phase.MAIN, phasesToSkip); // Second main
        addPhaseIfNotSkipped(Phase.ENDING, phasesToSkip);
    }

    private void addPhaseIfNotSkipped(Phase phase, Set<Phase> phasesToSkip) {
        if (phasesToSkip.contains(phase)) {
            return;
        }
        remainingPhases.addLast(new PhaseEntry(phase, buildStepsForPhase(phase)));
    }

    private List<Step> buildStepsForPhase(Phase phase) {
        // Don't filter here - we check shouldSkipStep dynamically in advanceToNextStep
        // This allows skip methods to be called at any time during the turn
        return new ArrayList<>(phase.steps());
    }

    private boolean shouldSkipStep(Step step) {
        var player = activePlayer();

        // Check permanent skip
        if (skipAllSteps.get(player).contains(step)) {
            return true;
        }

        // Check one-time skip
        var oneTimeSkips = skipNextSteps.get(player);
        if (oneTimeSkips != null && oneTimeSkips.count(step) > 0) {
            oneTimeSkips.remove(step, 1);
            return true;
        }

        // Check first draw step skip (only in two-player games per rule 103.8)
        if (skipFirstDraw
                && step == Step.DRAW
                && players.size() == 2
                && activePlayer().equals(firstPlayer)
                && turnNumber == 1) {
            skipFirstDraw = false;
            return true;
        }

        return false;
    }

    private void advanceToNextPhase() {
        if (remainingPhases.isEmpty()) {
            // Turn is over - fire ended event before next turn
            eventBus.post(new TurnEndedEvent(turnNumber, activePlayer()));
            startNextTurn();
            return;
        }

        // Safe: we just checked isEmpty() above
        var entry = remainingPhases.pollFirst();

        // Check if phase should be skipped this turn
        if (skipPhaseThisTurn.contains(entry.phase())) {
            skipPhaseThisTurn.remove(entry.phase());
            advanceToNextPhase();
            return;
        }

        currentPhase = entry.phase();
        currentPhaseOccurrence = phaseOccurrences.add(entry.phase(), 1) + 1;
        stepOccurrencesInPhase.clear();

        // Set up steps for this phase
        remainingStepsInPhase.clear();
        remainingStepsInPhase.addAll(entry.steps());

        // Fire phase started event
        eventBus.post(new PhaseStartedEvent(currentPhase, currentPhaseOccurrence));

        if (currentPhase == Phase.MAIN) {
            // Main phase has no steps, it IS the step
            currentStep = null;
            currentStepOccurrenceInPhase = 0;
            grantPriority(activePlayer());
        } else {
            advanceToNextStep();
        }
    }

    private void advanceToNextStep() {
        if (remainingStepsInPhase.isEmpty()) {
            // Phase is over - fire ended event before next phase
            firePhaseEndedIfPresent();
            advanceToNextPhase();
            return;
        }

        // Safe: we just checked isEmpty() above
        var step = remainingStepsInPhase.pollFirst();

        // Check if this step should be skipped (dynamically, so skips can be added anytime)
        if (shouldSkipStep(step)) {
            advanceToNextStep();
            return;
        }

        currentStep = step;
        currentStepOccurrenceInPhase = stepOccurrencesInPhase.add(step, 1) + 1;
        stepOccurrencesInTurn.add(step, 1);

        // Fire step started event
        eventBus.post(new StepStartedEvent(step, currentStepOccurrenceInPhase));

        // Perform turn-based actions for this step
        performTurnBasedActions(step);

        // Grant priority if this step has priority
        if (step.hasPriority()) {
            grantPriority(activePlayer());
        } else {
            // Steps without priority (untap, cleanup) advance immediately
            // But cleanup is special - it might loop
            if (step == Step.CLEANUP) {
                handleCleanupStep();
            } else {
                // Fire ended event before advancing (no priority = immediate)
                fireStepEndedIfPresent();
                advanceToNextStep();
            }
        }
    }

    private void performTurnBasedActions(Step step) {
        // TODO: Integrate with TurnBasedActionRegistry
        // For now, this is a placeholder
        switch (step) {
            case UNTAP -> {
                // Phasing, then untap
            }
            case DRAW -> {
                // Active player draws a card
            }
            case CLEANUP -> {
                // Discard to hand size, remove damage
            }
            default -> {
                // Other steps have no automatic turn-based actions
            }
        }
    }

    private void handleCleanupStep() {
        // Check if SBAs apply or triggers exist
        var needsPriority = checkStateBasedActions();
        // TODO: || hasPendingTriggers();

        if (needsPriority) {
            // Grant priority, which will eventually lead back here
            grantPriority(activePlayer());
            insertStepAfterCurrent(Step.CLEANUP);
        } else {
            // Cleanup step ends - fire ended event before advancing
            fireStepEndedIfPresent();
            advanceToNextStep();
        }
    }

    private void runCleanupLoop() {
        var needsAnotherCleanup = true;

        while (needsAnotherCleanup) {
            currentStep = Step.CLEANUP;
            currentStepOccurrenceInPhase = stepOccurrencesInPhase.add(Step.CLEANUP, 1) + 1;

            // Fire step started event
            eventBus.post(new StepStartedEvent(Step.CLEANUP, currentStepOccurrenceInPhase));

            performTurnBasedActions(Step.CLEANUP);

            var sbasApplied = checkStateBasedActions();
            // TODO: var triggersExist = hasPendingTriggers();

            if (sbasApplied /* || triggersExist */) {
                // Run priority round
                grantPriority(activePlayer());
                runPriorityRoundBlocking();
                // Cleanup ends, will loop for another
                fireStepEndedIfPresent();
                // needsAnotherCleanup remains true, loop continues
            } else {
                // Final cleanup ends
                fireStepEndedIfPresent();
                needsAnotherCleanup = false;
            }
        }
    }

    private void runPriorityRoundBlocking() {
        // This would block until all players pass with empty stack
        // In practice, this is driven by external calls to passPriority()
        // For cleanup loops during endTurnEarly(), we need synchronous behavior
        // TODO: Implement proper blocking/async handling
    }

    private void handleAllPlayersPassed() {
        passedPriority.clear();

        if (!stack.isEmpty()) {
            // Resolve top of stack
            stack.pop();
            // TODO: Actually resolve, not just pop

            // After resolution, active player gets priority
            grantPriority(activePlayer());
        } else {
            // Stack is empty, advance the game
            priorityHolder = null;

            if (currentPhase == Phase.MAIN) {
                // Main phase ends - fire ended event before advancing
                firePhaseEndedIfPresent();
                advanceToNextPhase();
            } else {
                // Non-main phases always have a current step when priority is granted
                // Step ends - fire ended event before advancing
                fireStepEndedIfPresent();
                advanceToNextStep();
            }
        }
    }

    private void advancePriorityToNextPlayer() {
        // Find next player who hasn't left the game
        do {
            apnapIndex = (apnapIndex + 1) % players.size();
            priorityHolder = players.get(apnapIndex);
        } while (!playersInGame.contains(priorityHolder));
    }

    private boolean checkStateBasedActions() {
        var anyApplied = false;
        boolean appliedThisPass;

        do {
            appliedThisPass = false;
            for (var checker : sbaCheckers) {
                if (checker.checkAndApply()) {
                    appliedThisPass = true;
                    anyApplied = true;
                }
            }
        } while (appliedThisPass);

        return anyApplied;
    }

    /// Returns players who have not left the game.
    private Set<Player> playersInGame() {
        return playersInGame;
    }

    /// Internal record for tracking phases with their steps.
    private record PhaseEntry(Phase phase, List<Step> steps) {}
}
