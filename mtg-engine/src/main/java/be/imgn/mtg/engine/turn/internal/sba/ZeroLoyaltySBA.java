package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.internal.StateBasedAction;

/// Planeswalker with 0 loyalty is put into its owner's graveyard ({@mtg.rule 704.5i}).
// TODO Implement ZeroLoyaltySBA
final class ZeroLoyaltySBA implements StateBasedAction {

    private final GameState gameState;

    ZeroLoyaltySBA(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public boolean checkAndApply() {
        return false;
    }
}
