package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.internal.StateBasedAction;

/// If multiple Role tokens controlled by the same player are attached to the same
/// permanent, all except the newest are put into their owners' graveyards ({@mtg.rule 704.5y}).
// TODO Implement MultipleRolesSBA
final class MultipleRolesSBA implements StateBasedAction {

    @Override
    public boolean appliesTo(GameState gameState) {
        return false;
    }

    @Override
    public void apply(GameState gameState) {}
}
