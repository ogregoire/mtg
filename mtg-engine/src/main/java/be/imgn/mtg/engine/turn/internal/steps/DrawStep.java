package be.imgn.mtg.engine.turn.internal.steps;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.StepType;

/// Implementation of the draw step ({@mtg.rule 504}).
///
/// During the draw step, the active player draws a card as a turn-based action
/// ({@mtg.rule 504.1}). This draw can be replaced by replacement effects.
///
/// After the draw, "at the beginning of your draw step" abilities trigger,
/// and players receive priority.
public final class DrawStep extends AbstractStep {

    public DrawStep(int occurrence) {
        super(StepType.DRAW, occurrence);
    }

    @Override
    public void performTurnBasedActions(GameState gameState) {
        // TODO: Active player draws a card (Rule 504.1)
        // This is the normal once-per-turn draw
        // It can be replaced by replacement effects
        // The first turn of the game (for the starting player) skips this draw
    }
}
