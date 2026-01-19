package be.imgn.mtg.engine.characteristics;

import be.imgn.mtg.engine.event.GameEvent;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.ObjectId;

/// Event representing counters being added to or removed from an object ({@mtg.rule 122}).
///
/// Counters modify characteristics of permanents (e.g., +1/+1 counters increase power/toughness)
/// or track game state (e.g., loyalty counters on planeswalkers).
///
/// Counter events can be replaced (e.g., "If one or more +1/+1 counters would be put on a creature,
/// twice that many are put on instead").
///
/// @param objectId the ID of the object receiving/losing counters
/// @param controller the player who controls the object
/// @param counterType the type of counter
/// @param amount the number of counters (positive for adding, negative for removing)
public record CounterEvent(ObjectId objectId, Player controller, CounterType counterType, int amount)
        implements GameEvent {

    @Override
    public Player affectedPlayer() {
        return controller;
    }

    /// Returns true if counters are being added.
    ///
    /// @return true if amount is positive
    public boolean isAdding() {
        return amount > 0;
    }

    /// Returns true if counters are being removed.
    ///
    /// @return true if amount is negative
    public boolean isRemoving() {
        return amount < 0;
    }
}
