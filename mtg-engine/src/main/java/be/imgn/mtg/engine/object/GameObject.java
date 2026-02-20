package be.imgn.mtg.engine.object;

import be.imgn.mtg.engine.game.Controlled;
import be.imgn.mtg.engine.game.Owned;
import be.imgn.mtg.engine.selector.Selectable;

/// An object in Magic: The Gathering ({@mtg.rule 109}).
///
/// Objects are entities that exist in the game. Each object has an
/// [owner][Owned#owner()] and a [controller][Controlled#controller()].
///
/// Objects with the full set of characteristics (name, mana cost, color, card type,
/// subtype, supertype, abilities, power, toughness, loyalty) implement [TypedObject].
/// Objects without characteristics (such as abilities on the stack and emblems)
/// extend this interface directly.
///
/// @see TypedObject
/// @see AbilityOnStack
/// @see Emblem
public sealed interface GameObject extends Owned, Controlled, Selectable permits TypedObject, AbilityOnStack, Emblem {}
