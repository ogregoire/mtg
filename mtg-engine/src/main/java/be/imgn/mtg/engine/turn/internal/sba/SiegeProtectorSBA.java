package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.internal.StateBasedAction;

/// Siege whose controller is also its protector gets a new protector
/// or is put into its owner's graveyard ({@mtg.rule 704.5x}).
// TODO Implement SiegeProtectorSBA
final class SiegeProtectorSBA implements StateBasedAction {

    @SuppressWarnings("UnusedVariable")
    private final GameState gameState;

    SiegeProtectorSBA(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public boolean checkAndApply() {
        return false;
    }
}
