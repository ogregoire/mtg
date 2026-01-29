package be.imgn.mtg.engine.state;

import java.util.Optional;

import be.imgn.mtg.engine.object.GameObject;

/// Tracks the last known information of objects that have changed zones ({@mtg.rule 400.7}).
///
/// When an object changes zones, some effects and triggered abilities need to use
/// information about that object as it last existed in its previous zone. This is
/// called "last known information" (LKI).
///
/// Common uses:
/// - A creature dying triggers abilities that reference its power/toughness
/// - Spell resolution effects that reference the spell's controller
/// - Replacement effects that need to know what was trying to enter a zone
///
/// @see ObjectSnapshot
public interface LastKnownInformation {

    /// Records a snapshot of an object before it changes zones.
    ///
    /// @param object the game object to record
    /// @param snapshot the object's state before the zone change
    void record(GameObject object, ObjectSnapshot snapshot);

    /// Retrieves the last known information for an object.
    ///
    /// @param object the game object
    /// @return the snapshot, or empty if no LKI recorded
    Optional<ObjectSnapshot> get(GameObject object);

    /// Clears all last known information.
    ///
    /// This is typically called at the end of each step/phase when LKI is no longer needed.
    void clear();

    /// Clears the last known information for a specific object.
    ///
    /// @param object the game object
    void clear(GameObject object);
}
