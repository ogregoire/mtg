package be.imgn.mtg.engine.oracle2.domain.selector;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.Supertype;

/// Selects an object by its supertype ({@mtg.rule 205.4}) —
/// "legendary", "basic", "snow", "world". Single-axis negation
/// ("nonbasic", "nonlegendary", "nonsnow") has its own [IsNot] arm
/// rather than going through `ObjectPropertySelector.Not(...)`.
public sealed interface SupertypeSelector extends CharacteristicSelector
        permits SupertypeSelector.Is, SupertypeSelector.IsNot {

    /// "[supertype]" — single positive supertype match. Example:
    /// "legendary creature" →
    /// `AllOf(new CardTypeSelector.Is(CardType.CREATURE), new Is(Supertype.LEGENDARY))`.
    record Is(Supertype supertype) implements SupertypeSelector {
        public Is {
            requireNonNull(supertype);
        }
    }

    /// "non[supertype]" — single negative supertype match. Example:
    /// "nonbasic land" →
    /// `AllOf(new CardTypeSelector.Is(CardType.LAND), new IsNot(Supertype.BASIC))`.
    record IsNot(Supertype supertype) implements SupertypeSelector {
        public IsNot {
            requireNonNull(supertype);
        }
    }
}
