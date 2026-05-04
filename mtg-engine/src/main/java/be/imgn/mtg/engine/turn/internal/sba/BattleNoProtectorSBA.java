package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.internal.StateBasedAction;

/// Battle with no player designated as its protector gets a protector or is put
/// into its owner's graveyard ({@mtg.rule 704.5w}).
// TODO Implement BattleNoProtectorSBA
final class BattleNoProtectorSBA implements StateBasedAction {

    @SuppressWarnings("UnusedVariable")
    private final GameState gameState;

    BattleNoProtectorSBA(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public boolean checkAndApply() {
        return false;
    }
}
