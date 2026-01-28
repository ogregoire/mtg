package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.internal.StateBasedAction;

/// Permanent with a counter limit has excess counters removed ({@mtg.rule 704.5r}).
///
/// Some abilities specify a maximum number of counters a permanent can have.
// TODO Implement CounterLimitSBA
final class CounterLimitSBA implements StateBasedAction {

    private final GameState gameState;

    CounterLimitSBA(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public boolean checkAndApply() {
        return false;
    }
}
