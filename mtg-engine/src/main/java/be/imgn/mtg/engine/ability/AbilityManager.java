package be.imgn.mtg.engine.ability;

import java.util.List;

import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.state.GameState;

/// Central manager for ability activation and registration ({@mtg.rule 112}, {@mtg.rule 113}).
///
/// The AbilityManager coordinates:
/// - Activation of activated abilities (Rule 602)
/// - Handling of mana abilities (Rule 605) - immediate resolution
/// - Handling of loyalty abilities (Rule 606) - sorcery speed, once per planeswalker
/// - Registration of static and triggered abilities when permanents enter the battlefield
/// - Validation of ability activation legality
///
/// @see ActivatedAbility
/// @see StaticAbility
/// @see be.imgn.mtg.engine.trigger.TriggeredAbility
public interface AbilityManager {

    /// Checks if an activated ability can be legally activated.
    ///
    /// This checks:
    /// - The source is in a zone where the ability functions
    /// - Timing restrictions are satisfied (sorcery speed, mana payment, etc.)
    /// - Activation limits haven't been exceeded
    /// - The player can pay the costs
    /// - No restriction effects prevent activation
    ///
    /// @param ability the ability to check
    /// @param source the object with the ability
    /// @param state the current game state
    /// @return true if the ability can be activated
    boolean canActivate(ActivatedAbility ability, GameObject source, GameState state);

    /// Activates an activated ability.
    ///
    /// For mana abilities (ability.isManaAbility() returns true):
    /// - The ability resolves immediately without using the stack
    /// - Returns ManaAbilitySuccess on success
    ///
    /// For non-mana abilities:
    /// - Costs are paid
    /// - The ability is put on the stack
    /// - Returns Success on success
    ///
    /// For illegal activations:
    /// - No costs are paid, no state changes
    /// - Returns Illegal with the reason
    ///
    /// @param ability the ability to activate
    /// @param source the object with the ability
    /// @param context the activation context
    /// @return the result of the activation attempt
    ActivationResult activate(ActivatedAbility ability, GameObject source, AbilityContext context);

    /// Returns all activatable abilities for a source that can currently be activated.
    ///
    /// Filters the source's abilities to those that:
    /// - Are activated abilities
    /// - Can currently be activated (timing, limits, costs)
    ///
    /// @param source the game object to check
    /// @param state the current game state
    /// @return list of currently activatable abilities
    List<ActivatedAbility> getActivatableAbilities(GameObject source, GameState state);

    /// Registers all abilities from a permanent that just entered the battlefield.
    ///
    /// This:
    /// - Registers static abilities with the continuous effect system
    /// - Registers triggered abilities with the trigger detector
    ///
    /// @param permanent the permanent that entered
    /// @param state the current game state
    void registerAbilities(Permanent permanent, GameState state);

    /// Unregisters all abilities from a permanent leaving the battlefield.
    ///
    /// This:
    /// - Removes static ability effects
    /// - Removes triggered ability registrations
    ///
    /// @param permanent the permanent leaving
    /// @param state the current game state
    void unregisterAbilities(Permanent permanent, GameState state);
}
