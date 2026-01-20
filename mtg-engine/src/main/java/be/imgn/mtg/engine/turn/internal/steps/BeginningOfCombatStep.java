package be.imgn.mtg.engine.turn.internal.steps;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.StepType;

/// Implementation of the beginning of combat step ({@mtg.rule 507}).
///
/// During this step, "at the beginning of combat" triggered abilities trigger.
/// The active player also announces which player or planeswalker they will
/// attack in formats where this matters ({@mtg.rule 507.1}).
///
/// Players receive priority during this step.
public final class BeginningOfCombatStep extends AbstractStep {

    public BeginningOfCombatStep(int occurrence) {
        super(StepType.BEGINNING_OF_COMBAT, occurrence);
    }

    @Override
    public void performTurnBasedActions(GameState gameState) {
        // No turn-based actions in this step
        // "At the beginning of combat" triggers are handled by the trigger system
        // The active player's choice of which player to attack is handled during
        // the declare attackers step
    }
}
