package be.imgn.mtg.engine.ability.internal.parser.effect;

import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;

/// An effect that causes a player to scry.
///
/// @param amount how many cards to scry
public record ScryEffect(Amount amount) implements Effect {}
