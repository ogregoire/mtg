package be.imgn.mtg.engine.trigger;

import be.imgn.mtg.engine.event.GameEvent;
import be.imgn.mtg.engine.state.GameState;

/// A condition that determines when a triggered ability triggers ({@mtg.rule 603}).
///
/// Trigger conditions check for specific game events or state changes. Common
/// trigger words are "when", "whenever", and "at".
///
/// Examples:
/// - "Whenever a creature enters the battlefield..."
/// - "When this creature dies..."
/// - "At the beginning of your upkeep..."
@FunctionalInterface
public interface TriggerCondition {

    /// Checks if this trigger condition is satisfied by the given event.
    ///
    /// @param event the event to check
    /// @param state the current game state
    /// @return true if this condition matches the event
    boolean matches(GameEvent event, GameState state);
}
