package be.imgn.mtg.engine.ability;

import be.imgn.mtg.engine.event.EventTracker;

/// An [ActivationLimit] that restricts an ability to once per turn ({@mtg.rule 602.5b}).
///
/// Many abilities have text like "Activate only once each turn." This implementation
/// checks the event tracker to see if the ability has already been activated during
/// the current turn.
///
/// @see ActivationLimit
public final class OncePerTurn implements ActivationLimit {

    /// Singleton instance of the once-per-turn limit.
    public static final OncePerTurn INSTANCE = new OncePerTurn();

    private OncePerTurn() {}

    @Override
    public boolean canActivate(AbilityId abilityId, EventTracker tracker) {
        return tracker.eventsFromThisTurn(AbilityActivatedEvent.class)
                .noneMatch(event -> event.abilityId().equals(abilityId));
    }
}
