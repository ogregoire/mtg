package be.imgn.mtg.engine.oracle.domain2.selector;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle.domain2.Supertype;

/// Selects an object by its supertype ({@mtg.rule 205.4}) —
/// "legendary", "basic", "snow", "world". Negation goes through
/// `ObjectPropertySelector.Not(...)` ("nonbasic", "nonlegendary",
/// "nonsnow").
public sealed interface SupertypeSelector extends CharacteristicSelector permits SupertypeSelector.Is {

    /// "[supertype]" — single positive supertype match. Example:
    /// "legendary creature" →
    /// `AllOf(CardTypeSelector.CREATURE, new Is(Supertype.LEGENDARY))`.
    record Is(Supertype supertype) implements SupertypeSelector {
        public Is {
            requireNonNull(supertype);
        }
    }

    /// "legendary".
    SupertypeSelector LEGENDARY = new Is(Supertype.LEGENDARY);
    /// "basic" (basic land).
    SupertypeSelector BASIC = new Is(Supertype.BASIC);
    /// "snow".
    SupertypeSelector SNOW = new Is(Supertype.SNOW);
    /// "world" (legacy World enchantments).
    SupertypeSelector WORLD = new Is(Supertype.WORLD);
}
