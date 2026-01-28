package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.internal.StateBasedAction;

/// Saga with lore counters equal to or greater than its final chapter number
/// is sacrificed ({@mtg.rule 704.5s}).
///
/// This only happens if the chapter ability has already triggered.
// TODO Implement SagaFinalChapterSBA
final class SagaFinalChapterSBA implements StateBasedAction {

    private final GameState gameState;

    SagaFinalChapterSBA(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public boolean checkAndApply() {
        return false;
    }
}
