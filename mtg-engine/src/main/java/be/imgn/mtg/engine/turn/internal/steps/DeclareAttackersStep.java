package be.imgn.mtg.engine.turn.internal.steps;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.StepType;

/// Implementation of the declare attackers step ({@mtg.rule 508}).
///
/// This step handles the combat system's attacker declaration.
/// The active player declares attackers, pays costs, and "whenever attacks"
/// triggered abilities trigger.
///
/// Note: The full combat system is out of scope for this implementation.
/// This is a stub that provides priority to players.
public final class DeclareAttackersStep extends AbstractStep {

    public DeclareAttackersStep(int occurrence) {
        super(StepType.DECLARE_ATTACKERS, occurrence);
    }

    @Override
    public void performTurnBasedActions(GameState gameState) {
        // TODO: Implement attacker declaration (Rule 508)
        // 1. Active player declares attackers
        // 2. Creatures become attacking
        // 3. "Whenever attacks" triggers go on the stack
        // 4. Attacking costs are determined and paid
    }
}
