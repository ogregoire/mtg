package be.imgn.mtg.engine.zone;

import be.imgn.mtg.engine.event.GameEvent;

/// Sealed base interface for all zone change events.
///
/// Zone changes are fundamental to MTG game flow. Objects move between zones
/// as a result of game actions, spell effects, and state-based actions.
///
/// Zone change events can be subscribed to either specifically (e.g., [DrawEvent])
/// or generically (this interface) to handle all zone changes.
///
/// @see DrawEvent
/// @see DiscardEvent
/// @see MillEvent
/// @see DiesEvent
/// @see ExileEvent
/// @see EntersBattlefieldEvent
/// @see LeavesBattlefieldEvent
/// @see ReturnToHandEvent
/// @see CastEvent
/// @see CounterEvent
/// @see PutIntoGraveyardEvent
/// @see ShuffleIntoLibraryEvent
public sealed interface ZoneChangeEvent extends GameEvent
        permits DrawEvent,
                DiscardEvent,
                MillEvent,
                DiesEvent,
                ExileEvent,
                EntersBattlefieldEvent,
                LeavesBattlefieldEvent,
                ReturnToHandEvent,
                CastEvent,
                CounterEvent,
                PutIntoGraveyardEvent,
                ShuffleIntoLibraryEvent {

    /// Returns the zone the object is moving from.
    ///
    /// @return the source zone type
    ZoneType from();

    /// Returns the zone the object is moving to.
    ///
    /// @return the destination zone type
    ZoneType to();
}
