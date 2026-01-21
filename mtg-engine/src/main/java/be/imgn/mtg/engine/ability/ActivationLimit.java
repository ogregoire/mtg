package be.imgn.mtg.engine.ability;

import be.imgn.mtg.engine.event.EventTracker;

/// Functional interface for checking if an ability's activation limit has been reached.
///
/// Some abilities have activation limits such as:
/// - "Activate only once each turn"
/// - "Activate only during your turn"
/// - Loyalty abilities (once per planeswalker per turn)
///
/// Implementations check the event tracker to determine if the ability can still be
/// activated according to its specific restrictions.
///
/// @see ActivatedAbility
/// @see OncePerTurn
@FunctionalInterface
public interface ActivationLimit {

    /// An activation limit that allows unlimited activations.
    ActivationLimit UNLIMITED = (abilityId, tracker) -> true;

    /// Checks if the ability can still be activated given the current event tracker.
    ///
    /// @param abilityId the ID of the ability being checked
    /// @param tracker the event tracker for querying past events
    /// @return true if the ability can be activated, false if the limit has been reached
    boolean canActivate(AbilityId abilityId, EventTracker tracker);
}
