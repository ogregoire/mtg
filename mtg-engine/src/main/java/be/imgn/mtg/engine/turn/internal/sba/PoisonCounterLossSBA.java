package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.internal.StateBasedAction;

/// Player with 10 or more poison counters loses the game ({@mtg.rule 704.5c}).
// TODO Implement PoisonCounterLossSBA
final class PoisonCounterLossSBA implements StateBasedAction {

    @SuppressWarnings("UnusedVariable")
    private final GameState gameState;

    PoisonCounterLossSBA(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public boolean checkAndApply() {
        return false;
    }
}
