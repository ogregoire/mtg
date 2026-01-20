package be.imgn.mtg.engine.turn;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.ObjectId;

/// Actions a player can take when they have priority ({@mtg.rule 117.1}).
///
/// A player with priority can:
/// - Cast a spell
/// - Activate an ability
/// - Take a special action
/// - Play a land (during main phase with empty stack)
/// - Pass priority
///
/// @see PlayerInputHandler
/// @see PrioritySystem
public sealed interface PlayerAction {

    /// Returns the player taking the action.
    ///
    /// @return the acting player
    Player player();

    /// The player passes priority without taking any action.
    ///
    /// @param player the player passing priority
    record Pass(Player player) implements PlayerAction {}

    /// The player casts a spell.
    ///
    /// @param player the player casting
    /// @param spellId the ID of the card being cast as a spell
    record CastSpell(Player player, ObjectId spellId) implements PlayerAction {}

    /// The player activates an ability.
    ///
    /// @param player the player activating
    /// @param sourceId the ID of the object with the ability
    /// @param abilityIndex the index of the ability being activated (0-indexed)
    record ActivateAbility(Player player, ObjectId sourceId, int abilityIndex) implements PlayerAction {}

    /// The player plays a land ({@mtg.rule 305}).
    ///
    /// @param player the player playing the land
    /// @param landId the ID of the land card being played
    record PlayLand(Player player, ObjectId landId) implements PlayerAction {}

    /// The player takes a special action ({@mtg.rule 116}).
    ///
    /// Special actions don't use the stack. Examples include:
    /// - Turning a face-down creature face up (morph)
    /// - Suspending a card
    /// - Foretelling a card
    ///
    /// @param player the player taking the action
    /// @param actionType the type of special action
    /// @param targetId the ID of the object involved, if any
    record SpecialAction(Player player, String actionType, ObjectId targetId) implements PlayerAction {}
}
