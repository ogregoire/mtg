package be.imgn.mtg.engine.oracle.domain;

import org.jspecify.annotations.Nullable;

/// Add a [Mana] payload to a player's mana pool. The optional
/// `player` is the actor when oracle text names one (Tangleroot:
/// "that player adds {G}"); null for the common imperative "Add …"
/// form where the controller is implicit. The `mana` value carries
/// the full structure of what's being added — literal symbols,
/// color choices, alternation, restrictions, etc. — see [Mana].
public record AddManaEffect(@Nullable Subject player, Mana mana) implements Effect {

    public AddManaEffect(Mana mana) {
        this(null, mana);
    }

    public AddManaEffect withPlayer(Subject player) {
        return new AddManaEffect(player, mana);
    }

    /// Wraps the mana in [Mana.Restricted] with the given restriction
    /// (rule 106.6). Used by the parser to absorb a trailing "Spend
    /// this mana only …" sentence into the preceding AddMana.
    public AddManaEffect withRestriction(Restriction restriction) {
        return new AddManaEffect(player, new Mana.Restricted(mana, restriction));
    }
}
