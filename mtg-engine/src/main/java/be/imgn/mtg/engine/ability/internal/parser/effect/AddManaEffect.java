package be.imgn.mtg.engine.ability.internal.parser.effect;

/// An effect that adds mana to a player's mana pool.
///
/// @param mana the mana to add (e.g., "{G}{G}", "{R}", "{1}")
public record AddManaEffect(String mana) implements Effect {}
