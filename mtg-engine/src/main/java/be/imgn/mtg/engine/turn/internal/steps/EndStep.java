package be.imgn.mtg.engine.turn.internal.steps;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.StepType;

/// Implementation of the end step ({@mtg.rule 513}).
///
/// During this step, "at the beginning of your end step" and "at the beginning
/// of the next end step" triggered abilities trigger ({@mtg.rule 513.1}).
///
/// This step tracks whether we are currently "in the end step" for triggers
/// that care about this timing ({@mtg.rule 513.2}).
///
/// Players receive priority during this step.
public final class EndStep extends AbstractStep {

    public EndStep(int occurrence) {
        super(StepType.END, occurrence);
    }

    @Override
    public void performTurnBasedActions(GameState gameState) {
        // No turn-based actions in this step
        // "At the beginning of your end step" triggers are handled by the trigger system
    }
}
