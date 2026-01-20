package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.StateBasedAction;

/// Saga with lore counters equal to or greater than its final chapter number
/// is sacrificed ({@mtg.rule 704.5s}).
///
/// This only happens if the chapter ability has already triggered.
// TODO Implement SagaFinalChapterSBA
final class SagaFinalChapterSBA implements StateBasedAction {

    @Override
    public boolean appliesTo(GameState gameState) {
        return false;
    }

    @Override
    public void apply(GameState gameState) {}
}
