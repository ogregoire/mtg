package be.imgn.mtg.engine.turn.internal.steps;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.StepType;

/// Implementation of the cleanup step ({@mtg.rule 514}).
///
/// During the cleanup step:
/// 1. The active player discards down to their maximum hand size ({@mtg.rule 514.1})
/// 2. All damage is removed from permanents ({@mtg.rule 514.2})
/// 3. Effects that last "until end of turn" or "this turn" expire ({@mtg.rule 514.2})
///
/// Normally no player receives priority during this step. However, if any
/// state-based actions are performed or triggered abilities are put on the
/// stack during cleanup, players do receive priority, and after the stack
/// empties, another cleanup step begins ({@mtg.rule 514.3a}).
public final class CleanupStep extends AbstractStep {

    private boolean triggeredCleanupLoop;

    public CleanupStep(int occurrence) {
        super(StepType.CLEANUP, occurrence);
    }

    @Override
    public void performTurnBasedActions(GameState gameState) {
        // Step 1: Active player discards to maximum hand size (Rule 514.1)
        // TODO: Get active player's hand size and max hand size
        // TODO: If hand size > max, make active player discard
        // This is a simultaneous discard choice

        // Step 2: Remove all damage from permanents (Rule 514.2)
        // TODO: Iterate all permanents on battlefield and clear damage markers

        // Step 3: "Until end of turn" and "this turn" effects expire (Rule 514.2)
        // This is handled by the DurationTracker
    }

    @Override
    public boolean hasPriority() {
        // Normally no priority, but if SBAs happen or triggers go on stack,
        // we need to grant priority. The TurnTracker handles this special case.
        return false;
    }

    /// Marks that this cleanup step triggered the cleanup loop.
    ///
    /// When SBAs occur or triggers are generated during cleanup, another
    /// cleanup step must occur after the stack empties.
    public void markTriggeredLoop() {
        this.triggeredCleanupLoop = true;
    }

    /// Returns whether this cleanup step should be followed by another.
    ///
    /// @return true if SBAs or triggers occurred and another cleanup is needed
    public boolean shouldRepeat() {
        return triggeredCleanupLoop;
    }
}
