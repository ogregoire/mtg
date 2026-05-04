package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.internal.StateBasedAction;

/// Creature dealt damage by a source with deathtouch is destroyed ({@mtg.rule 704.5h}).
///
/// This can be regenerated.
// TODO Implement DeathtouchDamageSBA
final class DeathtouchDamageSBA implements StateBasedAction {

    @SuppressWarnings("UnusedVariable")
    private final GameState gameState;

    DeathtouchDamageSBA(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public boolean checkAndApply() {
        return false;
    }
}
