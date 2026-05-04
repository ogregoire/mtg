package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.internal.StateBasedAction;

/// Player who attempted to draw from an empty library loses ({@mtg.rule 704.5b}).
// TODO Implement EmptyLibraryLossSBA
final class EmptyLibraryLossSBA implements StateBasedAction {

    @SuppressWarnings("UnusedVariable")
    private final GameState gameState;

    EmptyLibraryLossSBA(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public boolean checkAndApply() {
        return false;
    }
}
