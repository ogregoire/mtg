package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.StateBasedAction;

/// Creature dealt damage by a source with deathtouch is destroyed ({@mtg.rule 704.5h}).
///
/// This can be regenerated.
// TODO Implement DeathtouchDamageSBA
final class DeathtouchDamageSBA implements StateBasedAction {

    @Override
    public boolean appliesTo(GameState gameState) {
        return false;
    }

    @Override
    public void apply(GameState gameState) {}
}
