package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.internal.StateBasedAction;

/// Legend rule: player with multiple legendary permanents with the same name chooses one,
/// the rest are put into their owners' graveyards ({@mtg.rule 704.5j}).
// TODO Implement LegendRuleSBA
final class LegendRuleSBA implements StateBasedAction {

    private final GameState gameState;

    LegendRuleSBA(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public boolean checkAndApply() {
        return false;
    }
}
