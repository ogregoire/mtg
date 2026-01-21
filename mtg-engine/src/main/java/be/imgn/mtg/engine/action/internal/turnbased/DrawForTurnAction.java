package be.imgn.mtg.engine.action.internal.turnbased;

import be.imgn.mtg.engine.action.TurnBasedAction;
import be.imgn.mtg.engine.action.TurnBasedTiming;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.state.GameState;

/// Active player draws a card for turn ({@mtg.rule 504.1}).
///
/// As a turn-based action at the beginning of the draw step, the active player
/// draws a card. This draw can be replaced by replacement effects.
///
/// The starting player skips this draw on the first turn of the game.
///
/// Note: This is a partial implementation. Full implementation requires:
/// - DrawEvent for the GameEventProcessor
/// - First turn tracking to skip the starting player's first draw
/// - Library empty state handling (empty library draws are recorded for SBA)
public final class DrawForTurnAction implements TurnBasedAction {

    @Override
    public TurnBasedTiming timing() {
        return TurnBasedTiming.DRAW_STEP_DRAW;
    }

    @Override
    public void execute(GameState state, GameEventProcessor processor) {
        // TODO: Implement draw for turn
        // 1. Check if this is the first turn of the game (skip draw for starting player)
        // 2. Check if library is empty (record for SBA, don't crash)
        // 3. Create and process DrawEvent
        //
        // var activePlayer = state.activePlayer();
        //
        // // First turn skip
        // if (state.isFirstTurnOfGame() && state.isStartingPlayer(activePlayer)) {
        //     return;
        // }
        //
        // // Handle empty library
        // var library = state.library(activePlayer);
        // if (library.isEmpty()) {
        //     state.recordDrawFromEmptyLibrary(activePlayer);
        //     return;
        // }
        //
        // // Draw
        // var event = new DrawEvent(activePlayer);
        // processor.process(event);
    }
}
