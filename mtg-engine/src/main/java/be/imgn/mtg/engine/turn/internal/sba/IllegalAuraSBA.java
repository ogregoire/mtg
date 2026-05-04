package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.internal.StateBasedAction;

/// Aura attached to an illegal permanent/player or not attached to anything
/// is put into its owner's graveyard ({@mtg.rule 704.5m}).
// TODO Implement IllegalAuraSBA
final class IllegalAuraSBA implements StateBasedAction {

    @SuppressWarnings("UnusedVariable")
    private final GameState gameState;

    IllegalAuraSBA(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public boolean checkAndApply() {
        return false;
    }
}
