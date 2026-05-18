package be.imgn.mtg.engine.oracle2.domain.mana;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.Nullable;

/// A single mana instance — one [ManaSymbol] optionally carrying a
/// spend [Restriction] ({@mtg.rule 106.6}). Restrictions live on the
/// individual mana instance, not on the bundle, so multi-symbol
/// productions like "Add `{U}{U}`. Spend this mana only to cast
/// creature spells." materialize as two [Mana] instances, each with
/// the same restriction.
///
/// The group concept (what an [be.imgn.mtg.engine.oracle2.domain.effect.AddManaEffect]
/// actually produces in one resolution) is [ProducedMana].
public record Mana(ManaSymbol symbol, @Nullable Restriction restriction) {

    public Mana {
        requireNonNull(symbol);
    }

    public Mana(ManaSymbol symbol) {
        this(symbol, null);
    }

    public Mana withRestriction(Restriction restriction) {
        return new Mana(symbol, restriction);
    }
}
