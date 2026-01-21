package be.imgn.mtg.engine.ability;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.state.GameState;

/// Context in which an ability is being activated or resolved.
///
/// The context captures:
/// - The source object (the permanent, card, or stack object with the ability)
/// - The controller at the time of activation/resolution
/// - The game state at the time of activation/resolution
///
/// This context is used by effects to determine targets, check conditions,
/// and apply effects appropriately.
///
/// @param source the game object that has this ability
/// @param controller the player who controls the ability
/// @param state the game state at the time of activation/resolution
public record AbilityContext(GameObject source, Player controller, GameState state) {}
