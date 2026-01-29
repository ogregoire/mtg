package be.imgn.mtg.engine.trigger;

import be.imgn.mtg.engine.event.GameEvent;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.TypedObject;

/// A specific instance of a triggered ability waiting to go on the stack ({@mtg.rule 603.3}).
///
/// When a triggered ability triggers, an instance is created that captures:
/// - The ability that triggered
/// - The source permanent
/// - The controller at the time of triggering
/// - The event that caused the trigger (for "whenever X" abilities)
///
/// Triggered ability instances are held in the trigger queue until they're put
/// onto the stack during the next time a player would receive priority.
///
/// @param ability the triggered ability
/// @param source the object with this ability
/// @param controller the player who controls the source
/// @param triggeringEvent the event that caused this ability to trigger
public record TriggeredAbilityInstance(
        TriggeredAbility ability, TypedObject source, Player controller, GameEvent triggeringEvent) {}
