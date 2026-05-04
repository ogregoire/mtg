package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.internal.StateBasedAction;

/// Battle with 0 defense is put into its owner's graveyard ({@mtg.rule 704.5v}).
///
/// This only happens if any "when the last defense counter is removed" triggered
/// abilities have already been put on the stack.
// TODO Implement BattleZeroDefenseSBA
final class BattleZeroDefenseSBA implements StateBasedAction {

    @SuppressWarnings("UnusedVariable")
    private final GameState gameState;

    BattleZeroDefenseSBA(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public boolean checkAndApply() {
        return false;
    }
}
