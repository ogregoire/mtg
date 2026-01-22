package be.imgn.mtg.engine.mana;

/// An effect that adds exact mana symbols to a player's mana pool.
///
/// Examples: "Add {G}.", "Add {G}{G}.", "Add {W}{U}{B}{R}{G}."
///
/// @param mana the mana symbols to add (e.g., "{G}{G}", "{R}", "{1}")
public record AddExactManaEffect(String mana) implements AddManaEffect {}
