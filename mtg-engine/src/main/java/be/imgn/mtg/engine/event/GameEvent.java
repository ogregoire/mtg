package be.imgn.mtg.engine.event;

import be.imgn.mtg.engine.characteristics.CounterEvent;
import be.imgn.mtg.engine.combat.DamageEvent;
import be.imgn.mtg.engine.game.LifeChangeEvent;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.TapEvent;
import be.imgn.mtg.engine.zone.ZoneChangeEvent;

/// Sealed base interface for all game events that can be processed by the game action processor.
///
/// Game events represent state changes in the game that may be subject to replacement effects,
/// trigger abilities, and are ultimately resolved to mutate game state. All game events
/// affect a specific player who makes choices when multiple replacement effects apply.
///
/// Events are organized by domain:
/// - [ZoneChangeEvent] - Objects moving between zones
/// - [DamageEvent] - Damage being dealt
/// - [LifeChangeEvent] - Life total changes
/// - [CounterEvent] - Counters being added/removed
/// - [TapEvent] - Permanent tap status changes
///
/// @see ZoneChangeEvent
/// @see DamageEvent
public sealed interface GameEvent extends Event
        permits ZoneChangeEvent, DamageEvent, LifeChangeEvent, CounterEvent, TapEvent {

    /// Returns the player affected by this event.
    ///
    /// When multiple replacement effects could apply to an event, this player
    /// chooses the order in which they are applied ({@mtg.rule 616.1}).
    ///
    /// @return the affected player
    Player affectedPlayer();
}
