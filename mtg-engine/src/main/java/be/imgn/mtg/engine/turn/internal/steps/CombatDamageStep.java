package be.imgn.mtg.engine.turn.internal.steps;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.StepType;

/// Implementation of the combat damage step ({@mtg.rule 510}).
///
/// This step handles combat damage assignment and dealing.
/// This step may occur multiple times per combat phase if creatures
/// with first strike or double strike are involved.
///
/// Note: The full combat system is out of scope for this implementation.
/// This is a stub that provides priority to players.
public final class CombatDamageStep extends AbstractStep {

    public CombatDamageStep(int occurrence) {
        super(StepType.COMBAT_DAMAGE, occurrence);
    }

    @Override
    public void performTurnBasedActions(GameState gameState) {
        // TODO: Implement combat damage (Rule 510)
        // 1. Attacking player assigns combat damage from attacking creatures
        // 2. Defending player assigns combat damage from blocking creatures
        // 3. All combat damage is dealt simultaneously (as a single event)
        // 4. Damage triggers go on the stack
    }
}
