package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.internal.StateBasedAction;

/// Dungeon card in the command zone with a venture marker on the bottommost room
/// is removed from the game ({@mtg.rule 704.5t}).
///
/// This only happens if the room ability has already triggered.
// TODO Implement DungeonCompletedSBA
final class DungeonCompletedSBA implements StateBasedAction {

    private final GameState gameState;

    DungeonCompletedSBA(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public boolean checkAndApply() {
        return false;
    }
}
