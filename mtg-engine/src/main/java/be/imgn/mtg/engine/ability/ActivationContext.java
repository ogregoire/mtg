package be.imgn.mtg.engine.ability;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.event.EventTracker;

/// Context for checking whether an activated ability can be activated ({@mtg.rule 602.2}).
///
/// Provides the game state information needed to evaluate activation restrictions such as
/// once-per-turn limits on loyalty abilities.
///
/// @param tracker the event tracker for querying past events this turn
public record ActivationContext(EventTracker tracker) {

    /// Creates a new activation context.
    ///
    /// @throws NullPointerException if tracker is null
    public ActivationContext {
        requireNonNull(tracker, "tracker");
    }
}
