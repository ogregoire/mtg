package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.internal.StateBasedAction;

/// +1/+1 counters and -1/-1 counters on a permanent cancel each other out ({@mtg.rule 704.5q}).
///
/// Equal numbers of each are removed simultaneously.
// TODO Implement CounterCancellationSBA
final class CounterCancellationSBA implements StateBasedAction {

    @SuppressWarnings("UnusedVariable")
    private final GameState gameState;

    CounterCancellationSBA(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public boolean checkAndApply() {
        return false;
    }
}
