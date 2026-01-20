package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.StateBasedAction;

/// Copy of a spell or card in a zone other than the stack ceases to exist ({@mtg.rule 704.5e}).
// TODO Implement CopyCeasesToExistSBA
final class CopyCeasesToExistSBA implements StateBasedAction {

    @Override
    public boolean appliesTo(GameState gameState) {
        return false;
    }

    @Override
    public void apply(GameState gameState) {}
}
