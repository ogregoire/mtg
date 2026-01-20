package be.imgn.mtg.engine.turn.internal.steps;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.StepType;

/// Implementation of the upkeep step ({@mtg.rule 503}).
///
/// The upkeep step has no turn-based actions. Its main purpose is to
/// provide a time for triggered abilities with "at the beginning of
/// your upkeep" to trigger, and for players to cast instants and
/// activate abilities.
///
/// Players receive priority during this step.
public final class UpkeepStep extends AbstractStep {

    public UpkeepStep(int occurrence) {
        super(StepType.UPKEEP, occurrence);
    }

    @Override
    public void performTurnBasedActions(GameState gameState) {
        // No turn-based actions in the upkeep step
        // "At the beginning of your upkeep" triggers are handled by the trigger system
    }
}
