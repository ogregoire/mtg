package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.internal.StateBasedAction;

/// State-based action: A creature with 0 or less toughness is put into
/// its owner's graveyard ({@mtg.rule 704.5f}).
///
/// Unlike lethal damage, this puts the creature into the graveyard directly.
/// It's not destruction, so indestructible doesn't prevent it and regeneration
/// can't replace it.
///
/// Note: This is a stub implementation. Full implementation requires:
/// - Creature/permanent tracking on the battlefield
/// - Toughness calculation (may be modified by continuous effects)
public final class ZeroToughnessSBA implements StateBasedAction {

    @SuppressWarnings("UnusedVariable")
    private final GameState gameState;

    public ZeroToughnessSBA(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public boolean checkAndApply() {
        // TODO: Check if any creature has 0 or less toughness and move to graveyard
        return false;
    }
}
