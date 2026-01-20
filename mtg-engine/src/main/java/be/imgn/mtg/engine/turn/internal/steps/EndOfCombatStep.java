package be.imgn.mtg.engine.turn.internal.steps;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.StepType;

/// Implementation of the end of combat step ({@mtg.rule 511}).
///
/// During this step, "at end of combat" triggered abilities trigger.
/// After this step ends, creatures stop being attacking and blocking,
/// and effects that last "until end of combat" expire.
///
/// Players receive priority during this step.
public final class EndOfCombatStep extends AbstractStep {

    public EndOfCombatStep(int occurrence) {
        super(StepType.END_OF_COMBAT, occurrence);
    }

    @Override
    public void performTurnBasedActions(GameState gameState) {
        // No turn-based actions in this step
        // "At end of combat" triggers are handled by the trigger system
    }

    @Override
    public void performEndActions(GameState gameState) {
        // After this step ends:
        // - Creatures stop being attacking/blocking
        // - Effects lasting "until end of combat" expire
        // This is handled by the duration tracker
    }
}
