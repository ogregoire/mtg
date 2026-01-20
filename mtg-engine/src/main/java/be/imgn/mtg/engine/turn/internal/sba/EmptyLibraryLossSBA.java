package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.StateBasedAction;

/// Player who attempted to draw from an empty library loses ({@mtg.rule 704.5b}).
// TODO Implement EmptyLibraryLossSBA
final class EmptyLibraryLossSBA implements StateBasedAction {

    @Override
    public boolean appliesTo(GameState gameState) {
        return false;
    }

    @Override
    public void apply(GameState gameState) {}
}
