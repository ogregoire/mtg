package be.imgn.mtg.engine.turn.internal.sba;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.internal.StateBasedAction;

/// Dungeon card in the command zone with a venture marker on the bottommost room
/// is removed from the game ({@mtg.rule 704.5t}).
///
/// This only happens if the room ability has already triggered.
// TODO Implement DungeonCompletedSBA
final class DungeonCompletedSBA implements StateBasedAction {

    @Override
    public boolean appliesTo(GameState gameState) {
        return false;
    }

    @Override
    public void apply(GameState gameState) {}
}
