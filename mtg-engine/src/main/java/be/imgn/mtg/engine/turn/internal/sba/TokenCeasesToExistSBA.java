package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.internal.StateBasedAction;

/// Token in a zone other than the battlefield ceases to exist ({@mtg.rule 704.5d}).
// TODO Implement TokenCeasesToExistSBA
final class TokenCeasesToExistSBA implements StateBasedAction {

    @SuppressWarnings("UnusedVariable")
    private final GameState gameState;

    TokenCeasesToExistSBA(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public boolean checkAndApply() {
        return false;
    }
}
