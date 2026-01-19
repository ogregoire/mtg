package be.imgn.mtg.engine.trigger;

import java.util.List;

import be.imgn.mtg.engine.event.GameEvent;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.ObjectId;
import be.imgn.mtg.engine.state.GameState;

/// Detects triggered abilities that should trigger from game events ({@mtg.rule 603.2}).
///
/// The trigger detector maintains a registry of all active triggered abilities
/// and checks incoming events against their trigger conditions.
public interface TriggerDetector {

    /// Registers a triggered ability from a source.
    ///
    /// @param ability the triggered ability
    /// @param source the ID of the object with this ability
    /// @param controller the player who controls the source
    void register(TriggeredAbility ability, ObjectId source, Player controller);

    /// Unregisters all triggered abilities from a source.
    ///
    /// This is called when a permanent leaves the battlefield or changes zones
    /// to a zone where its abilities don't function.
    ///
    /// @param source the ID of the source to unregister
    void unregister(ObjectId source);

    /// Detects all triggered abilities that trigger from an event.
    ///
    /// This checks all registered abilities against the event and returns
    /// instances for each that triggers. Intervening-if conditions are
    /// checked at this point.
    ///
    /// @param event the event to check
    /// @param state the current game state
    /// @return list of triggered ability instances
    List<TriggeredAbilityInstance> detect(GameEvent event, GameState state);
}
