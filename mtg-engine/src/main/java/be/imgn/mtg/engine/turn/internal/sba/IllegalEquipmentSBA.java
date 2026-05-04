package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.internal.StateBasedAction;

/// Equipment or Fortification attached to an illegal permanent becomes unattached ({@mtg.rule 704.5n}).
// TODO Implement IllegalEquipmentSBA
final class IllegalEquipmentSBA implements StateBasedAction {

    @SuppressWarnings("UnusedVariable")
    private final GameState gameState;

    IllegalEquipmentSBA(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public boolean checkAndApply() {
        return false;
    }
}
