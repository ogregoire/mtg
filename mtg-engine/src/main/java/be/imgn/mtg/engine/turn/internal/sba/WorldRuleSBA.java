package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.internal.StateBasedAction;

/// World rule: if multiple permanents with the supertype world are on the battlefield,
/// all except the newest are put into their owners' graveyards ({@mtg.rule 704.5k}).
// TODO Implement WorldRuleSBA
final class WorldRuleSBA implements StateBasedAction {

    @Override
    public boolean appliesTo(GameState gameState) {
        return false;
    }

    @Override
    public void apply(GameState gameState) {}
}
