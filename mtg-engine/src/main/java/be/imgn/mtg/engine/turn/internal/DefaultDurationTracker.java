package be.imgn.mtg.engine.turn.internal;

import be.imgn.mtg.engine.turn.DurationTracker;
import be.imgn.mtg.engine.turn.TurnState;

/// Default implementation of [DurationTracker].
///
/// Tracks effects with durations and handles their expiration.
/// Currently a stub implementation - will be expanded when the continuous effects
/// system is implemented.
final class DefaultDurationTracker implements DurationTracker {

    DefaultDurationTracker() {}

    @Override
    public void expireUntilStep(Step step) {
        // TODO: Implement when continuous effects are added
        // Expire effects that end "until [step]"
    }

    @Override
    public void expireUntilEndOfStep(Step step) {
        // TODO: Implement when continuous effects are added
        // Expire effects that end "until end of [step]"
    }

    @Override
    public void expireUntilEndOfTurn() {
        // TODO: Implement when continuous effects are added
        // Expire "until end of turn" effects during cleanup
    }

    @Override
    public void expireUntilEndOfCombat() {
        // TODO: Implement when continuous effects are added
        // Expire "until end of combat" effects at end of combat phase
    }

    @Override
    public void expireUntilNextTurn(TurnState turnState) {
        // TODO: Implement when continuous effects are added
        // Expire "until your next turn" effects at the start of a player's turn
    }
}
