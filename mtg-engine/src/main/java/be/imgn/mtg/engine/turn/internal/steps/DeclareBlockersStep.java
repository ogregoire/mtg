package be.imgn.mtg.engine.turn.internal.steps;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.StepType;

/// Implementation of the declare blockers step ({@mtg.rule 509}).
///
/// This step handles the combat system's blocker declaration.
/// The defending player declares blockers, and the attacking player
/// orders blockers for damage assignment.
///
/// Note: The full combat system is out of scope for this implementation.
/// This is a stub that provides priority to players.
public final class DeclareBlockersStep extends AbstractStep {

    public DeclareBlockersStep(int occurrence) {
        super(StepType.DECLARE_BLOCKERS, occurrence);
    }

    @Override
    public void performTurnBasedActions(GameState gameState) {
        // TODO: Implement blocker declaration (Rule 509)
        // 1. Defending player declares blockers
        // 2. Creatures become blocking
        // 3. Attacking player orders blockers (for multiple blockers per attacker)
        // 4. "Whenever blocks" triggers go on the stack
    }
}
