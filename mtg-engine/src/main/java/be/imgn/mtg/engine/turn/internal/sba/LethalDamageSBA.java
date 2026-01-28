package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.internal.StateBasedAction;

/// State-based action: A creature with lethal damage is destroyed ({@mtg.rule 704.5g}).
///
/// A creature has lethal damage if it has damage marked on it equal to or greater
/// than its toughness. Such a creature is destroyed.
///
/// Note: This SBA doesn't apply to creatures with indestructible.
///
/// Note: This is a stub implementation. Full implementation requires:
/// - Creature/permanent tracking on the battlefield
/// - Damage marked on permanents
/// - Toughness calculation (may be modified by effects)
/// - Indestructible ability checking
public final class LethalDamageSBA implements StateBasedAction {

    private final GameState gameState;

    public LethalDamageSBA(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public boolean checkAndApply() {
        // TODO: Check if any creature has lethal damage and destroy it
        return false;
    }
}
