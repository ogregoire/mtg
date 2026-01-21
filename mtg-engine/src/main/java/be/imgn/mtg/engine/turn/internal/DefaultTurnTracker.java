package be.imgn.mtg.engine.turn.internal;

import java.util.ArrayList;
import java.util.List;

import be.imgn.mtg.engine.action.TurnBasedActionRegistry;
import be.imgn.mtg.engine.action.TurnBasedTiming;
import be.imgn.mtg.engine.event.EventBus;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.DurationTracker;
import be.imgn.mtg.engine.turn.OccurrenceTracker;
import be.imgn.mtg.engine.turn.Phase;
import be.imgn.mtg.engine.turn.PhaseEndedEvent;
import be.imgn.mtg.engine.turn.PhaseStartedEvent;
import be.imgn.mtg.engine.turn.PhaseType;
import be.imgn.mtg.engine.turn.PrioritySystem;
import be.imgn.mtg.engine.turn.SBAEngine;
import be.imgn.mtg.engine.turn.SkipTracker;
import be.imgn.mtg.engine.turn.Step;
import be.imgn.mtg.engine.turn.StepEndedEvent;
import be.imgn.mtg.engine.turn.StepStartedEvent;
import be.imgn.mtg.engine.turn.StepType;
import be.imgn.mtg.engine.turn.TurnEndedEvent;
import be.imgn.mtg.engine.turn.TurnStartedEvent;
import be.imgn.mtg.engine.turn.TurnState;
import be.imgn.mtg.engine.turn.TurnTracker;
import be.imgn.mtg.engine.turn.internal.steps.BeginningOfCombatStep;
import be.imgn.mtg.engine.turn.internal.steps.CleanupStep;
import be.imgn.mtg.engine.turn.internal.steps.CombatDamageStep;
import be.imgn.mtg.engine.turn.internal.steps.DeclareAttackersStep;
import be.imgn.mtg.engine.turn.internal.steps.DeclareBlockersStep;
import be.imgn.mtg.engine.turn.internal.steps.DrawStep;
import be.imgn.mtg.engine.turn.internal.steps.EndOfCombatStep;
import be.imgn.mtg.engine.turn.internal.steps.EndStep;
import be.imgn.mtg.engine.turn.internal.steps.MainPhaseStep;
import be.imgn.mtg.engine.turn.internal.steps.UntapStep;
import be.imgn.mtg.engine.turn.internal.steps.UpkeepStep;

/// Default implementation of [TurnTracker].
///
/// Orchestrates the game flow through turns, phases, and steps.
/// Handles priority rounds, state-based actions, and duration expiration.
public final class DefaultTurnTracker implements TurnTracker {

    private final GameState gameState;
    private final EventBus eventBus;
    private final DefaultTurnState turnState;
    private final OccurrenceTracker occurrenceTracker;
    private final PrioritySystem prioritySystem;
    private final SBAEngine sbaEngine;
    private final DurationTracker durationTracker;
    private final SkipTracker skipTracker;
    private final TurnBasedActionRegistry turnBasedActionRegistry;
    private final GameEventProcessor gameEventProcessor;

    private boolean endTurnRequested;

    DefaultTurnTracker(
            GameState gameState,
            EventBus eventBus,
            OccurrenceTracker occurrenceTracker,
            PrioritySystem prioritySystem,
            SBAEngine sbaEngine,
            DurationTracker durationTracker,
            SkipTracker skipTracker,
            TurnBasedActionRegistry turnBasedActionRegistry,
            GameEventProcessor gameEventProcessor) {
        this.gameState = gameState;
        this.eventBus = eventBus;
        this.turnState = new DefaultTurnState(gameState);
        this.occurrenceTracker = occurrenceTracker;
        this.prioritySystem = prioritySystem;
        this.sbaEngine = sbaEngine;
        this.durationTracker = durationTracker;
        this.skipTracker = skipTracker;
        this.turnBasedActionRegistry = turnBasedActionRegistry;
        this.gameEventProcessor = gameEventProcessor;
    }

    @Override
    public void run() {
        // Initialize with first player
        List<Player> players = gameState.players();
        if (players.isEmpty()) {
            throw new IllegalStateException("Cannot run game with no players");
        }
        turnState.initialize(players.getFirst());

        // Main game loop
        while (!gameState.isGameOver()) {
            runTurn();
        }
    }

    @Override
    public void endTurnEarly() {
        endTurnRequested = true;
    }

    @Override
    public TurnState turnState() {
        return turnState;
    }

    private void runTurn() {
        Player activePlayer = turnState.nextTurn();
        occurrenceTracker.reset();
        skipTracker.resetForNewTurn();
        endTurnRequested = false;

        // Check if this turn should be skipped (Rule 500.11)
        if (skipTracker.shouldSkipNextTurn(activePlayer)) {
            skipTracker.clearTurnSkip(activePlayer);
            // Skipped turns still expire "until end of turn" effects
            durationTracker.expireUntilEndOfTurn();
            return;
        }

        // Expire "until your next turn" effects
        durationTracker.expireUntilNextTurn(turnState);

        // Fire turn started event
        eventBus.post(new TurnStartedEvent(turnState.turnNumber(), activePlayer));

        // Run phases in order
        List<Phase> phases = buildPhases();
        for (Phase phase : phases) {
            if (gameState.isGameOver() || endTurnRequested) {
                break;
            }
            runPhase(phase);
        }

        // Handle end turn early - skip to cleanup
        if (endTurnRequested) {
            // Exile everything on the stack
            // TODO: gameState.stack().exileAll();

            // Run cleanup (may loop if SBAs/triggers occur)
            runCleanupLoop();
        }

        // Fire turn ended event
        eventBus.post(new TurnEndedEvent(turnState.turnNumber(), activePlayer));
    }

    private List<Phase> buildPhases() {
        List<Phase> phases = new ArrayList<>();

        // Beginning phase
        int beginningOccurrence = occurrenceTracker.increment(PhaseType.BEGINNING);
        phases.add(new DefaultPhase(PhaseType.BEGINNING, beginningOccurrence, buildSteps(PhaseType.BEGINNING)));

        // First main phase
        int mainOccurrence1 = occurrenceTracker.increment(PhaseType.MAIN);
        phases.add(new DefaultPhase(PhaseType.MAIN, mainOccurrence1, List.of(new MainPhaseStep(mainOccurrence1))));

        // Combat phase
        int combatOccurrence = occurrenceTracker.increment(PhaseType.COMBAT);
        phases.add(new DefaultPhase(PhaseType.COMBAT, combatOccurrence, buildSteps(PhaseType.COMBAT)));

        // Second main phase
        int mainOccurrence2 = occurrenceTracker.increment(PhaseType.MAIN);
        phases.add(new DefaultPhase(PhaseType.MAIN, mainOccurrence2, List.of(new MainPhaseStep(mainOccurrence2))));

        // Ending phase
        int endingOccurrence = occurrenceTracker.increment(PhaseType.ENDING);
        phases.add(new DefaultPhase(PhaseType.ENDING, endingOccurrence, buildSteps(PhaseType.ENDING)));

        return phases;
    }

    private List<Step> buildSteps(PhaseType phaseType) {
        List<Step> steps = new ArrayList<>();

        for (StepType stepType : phaseType.steps()) {
            int occurrence = occurrenceTracker.increment(stepType);
            steps.add(createStep(stepType, occurrence));
        }

        return steps;
    }

    private Step createStep(StepType stepType, int occurrence) {
        return switch (stepType) {
            case UNTAP -> new UntapStep(occurrence);
            case UPKEEP -> new UpkeepStep(occurrence);
            case DRAW -> new DrawStep(occurrence);
            case BEGINNING_OF_COMBAT -> new BeginningOfCombatStep(occurrence);
            case DECLARE_ATTACKERS -> new DeclareAttackersStep(occurrence);
            case DECLARE_BLOCKERS -> new DeclareBlockersStep(occurrence);
            case COMBAT_DAMAGE -> new CombatDamageStep(occurrence);
            case END_OF_COMBAT -> new EndOfCombatStep(occurrence);
            case END -> new EndStep(occurrence);
            case CLEANUP -> new CleanupStep(occurrence);
        };
    }

    private void runPhase(Phase phase) {
        // Check if this phase should be skipped (Rule 500.11)
        if (skipTracker.isSkipped(phase.type())) {
            // Skipped phases still expire "until end of phase" effects (Rule 614.10)
            if (phase.type() == PhaseType.COMBAT) {
                durationTracker.expireUntilEndOfCombat();
            }
            return;
        }

        // Fire phase started event
        eventBus.post(new PhaseStartedEvent(phase.type(), phase.occurrence()));

        // Run steps in this phase
        for (Step step : phase.steps()) {
            if (gameState.isGameOver() || endTurnRequested) {
                break;
            }
            runStep(step);
        }

        // Handle "until end of combat" at end of combat phase
        if (phase.type() == PhaseType.COMBAT) {
            durationTracker.expireUntilEndOfCombat();
        }

        // Empty mana pools at end of phase
        gameState.emptyManaPools();

        // Fire phase ended event
        eventBus.post(new PhaseEndedEvent(phase.type(), phase.occurrence()));
    }

    private void runStep(Step step) {
        // Check if this step should be skipped (Rule 500.11)
        if (step.type() != null && skipTracker.isSkipped(step.type())) {
            // Skipped steps still expire "until end of step" effects (Rule 614.10)
            durationTracker.expireUntilEndOfStep(step);
            return;
        }

        // Fire step started event
        if (step.type() != null) {
            eventBus.post(new StepStartedEvent(step.type(), step.occurrence()));
        }

        // Expire effects that end at start of this step
        durationTracker.expireUntilStep(step);

        // Perform turn-based actions for this step via registry
        executeTurnBasedActionsForStep(step);

        // Run priority round if this step has priority
        if (step.hasPriority()) {
            runPriorityRound();
        }

        // Special handling for cleanup step
        if (step instanceof CleanupStep cleanupStep) {
            handleCleanupStep(cleanupStep);
        }

        // Perform end-of-step actions
        step.performEndActions(gameState);

        // Expire effects that end at end of this step
        durationTracker.expireUntilEndOfStep(step);

        // Empty mana pools at end of step
        gameState.emptyManaPools();

        // Fire step ended event
        if (step.type() != null) {
            eventBus.post(new StepEndedEvent(step.type(), step.occurrence()));
        }
    }

    private void executeTurnBasedActionsForStep(Step step) {
        if (step.type() == null) {
            return;
        }

        switch (step.type()) {
            case UNTAP -> {
                // Untap step: phasing, day/night, then untap (Rule 502)
                turnBasedActionRegistry.executeAll(TurnBasedTiming.UNTAP_STEP_PHASING, gameState, gameEventProcessor);
                turnBasedActionRegistry.executeAll(TurnBasedTiming.UNTAP_STEP_DAY_NIGHT, gameState, gameEventProcessor);
                turnBasedActionRegistry.executeAll(TurnBasedTiming.UNTAP_STEP_UNTAP, gameState, gameEventProcessor);
            }
            case DRAW -> {
                // Draw step: draw a card (Rule 504)
                turnBasedActionRegistry.executeAll(TurnBasedTiming.DRAW_STEP_DRAW, gameState, gameEventProcessor);
            }
            case CLEANUP -> {
                // Cleanup step: discard and remove damage (Rule 514)
                turnBasedActionRegistry.executeAll(TurnBasedTiming.CLEANUP_DISCARD, gameState, gameEventProcessor);
                turnBasedActionRegistry.executeAll(
                        TurnBasedTiming.CLEANUP_REMOVE_DAMAGE, gameState, gameEventProcessor);
            }
            default -> {
                // Other steps don't have turn-based actions via the registry
                // (they may still have step-specific actions handled by the step itself)
                step.performTurnBasedActions(gameState);
            }
        }
    }

    private void runPriorityRound() {
        prioritySystem.reset();
        prioritySystem.givePriority(turnState.activePlayer());

        while (!gameState.isGameOver()) {
            // Stabilize game state: check SBAs and process triggers
            stabilizeGameState();

            if (gameState.isGameOver()) {
                return;
            }

            // Check if we're done with priority round
            if (gameState.stack().isEmpty() && prioritySystem.allPassed()) {
                break;
            }

            // If all passed and stack not empty, resolve top of stack
            if (prioritySystem.allPassed()) {
                // TODO: Resolve top of stack
                // gameState.stack().resolveTop();
                prioritySystem.reset();
                prioritySystem.givePriority(turnState.activePlayer());
                continue;
            }

            // Wait for action from current priority holder
            // For now, we just pass (AI/player input not implemented)
            Player holder = prioritySystem.currentPriorityHolder();
            if (holder != null) {
                prioritySystem.pass(holder);
            } else {
                break;
            }
        }
    }

    private void stabilizeGameState() {
        // Check and apply SBAs
        sbaEngine.checkAndApply(gameState);

        // TODO: Process triggers that were generated
        // triggerSystem.processTriggeredAbilities();
    }

    private void handleCleanupStep(CleanupStep cleanupStep) {
        // Check if SBAs would apply or triggers are pending
        boolean needsAnotherCleanup = sbaEngine.wouldPerformActions(gameState);
        // TODO: || triggerSystem.hasPendingTriggers();

        if (needsAnotherCleanup) {
            cleanupStep.markTriggeredLoop();

            // Grant priority for this cleanup
            runPriorityRound();

            // After priority round completes, we'll need another cleanup step
            // This is handled by the cleanup loop
        }

        // Expire "until end of turn" effects
        durationTracker.expireUntilEndOfTurn();
    }

    private void runCleanupLoop() {
        boolean needsAnotherCleanup = true;

        while (needsAnotherCleanup && !gameState.isGameOver()) {
            int occurrence = occurrenceTracker.increment(StepType.CLEANUP);
            CleanupStep cleanupStep = new CleanupStep(occurrence);

            // Fire step started event
            eventBus.post(new StepStartedEvent(StepType.CLEANUP, occurrence));

            // Perform turn-based actions via registry
            turnBasedActionRegistry.executeAll(TurnBasedTiming.CLEANUP_DISCARD, gameState, gameEventProcessor);
            turnBasedActionRegistry.executeAll(TurnBasedTiming.CLEANUP_REMOVE_DAMAGE, gameState, gameEventProcessor);

            // Check if SBAs or triggers require another cleanup
            boolean sbasWouldApply = sbaEngine.wouldPerformActions(gameState);
            // TODO: boolean triggersPresent = triggerSystem.hasPendingTriggers();

            if (sbasWouldApply /* || triggersPresent */) {
                cleanupStep.markTriggeredLoop();
                runPriorityRound();
                needsAnotherCleanup = true;
            } else {
                needsAnotherCleanup = false;
            }

            // Expire "until end of turn" effects
            durationTracker.expireUntilEndOfTurn();

            // Fire step ended event
            eventBus.post(new StepEndedEvent(StepType.CLEANUP, occurrence));
        }
    }
}
