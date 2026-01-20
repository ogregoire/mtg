package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.StateBasedAction;

/// Permanent with a counter limit has excess counters removed ({@mtg.rule 704.5r}).
///
/// Some abilities specify a maximum number of counters a permanent can have.
// TODO Implement CounterLimitSBA
final class CounterLimitSBA implements StateBasedAction {

    @Override
    public boolean appliesTo(GameState gameState) {
        return false;
    }

    @Override
    public void apply(GameState gameState) {}
}
