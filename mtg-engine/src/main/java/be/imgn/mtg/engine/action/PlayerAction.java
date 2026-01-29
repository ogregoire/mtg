package be.imgn.mtg.engine.action;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.GameObject;

/// Actions a player can take when they have priority ({@mtg.rule 117.1}).
///
/// A player with priority can:
/// - Cast a spell
/// - Activate an ability
/// - Take a special action
/// - Play a land (during main phase with empty stack)
/// - Pass priority
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
    /// @param card the card being cast as a spell
    record CastSpell(Player player, Card card) implements PlayerAction {}

    /// The player activates an ability.
    ///
    /// @param player the player activating
    /// @param source the object with the ability
    /// @param abilityIndex the index of the ability being activated (0-indexed)
    record ActivateAbility(Player player, GameObject source, int abilityIndex) implements PlayerAction {}

    /// The player plays a land ({@mtg.rule 305}).
    ///
    /// @param player the player playing the land
    /// @param land the land card being played
    record PlayLand(Player player, Card land) implements PlayerAction {}

    /// The player takes a special action ({@mtg.rule 116}).
    ///
    /// Special actions don't use the stack. Examples include:
    /// - Turning a face-down creature face up (morph)
    /// - Suspending a card
    /// - Foretelling a card
    ///
    /// @param player the player taking the action
    /// @param actionType the type of special action
    /// @param target the object involved, if any
    record SpecialAction(Player player, SpecialActionType actionType, GameObject target) implements PlayerAction {}
}
