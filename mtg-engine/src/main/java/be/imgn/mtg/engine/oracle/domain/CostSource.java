package be.imgn.mtg.engine.oracle.domain;

import org.jspecify.annotations.Nullable;

/// Source of a cost that a [Effect.ModifyCost] effect modifies: either
/// spells matching a [Subject], or a keyword ability's variable cost
/// ("buyback costs", "kicker costs", …) per rule 702.1a.
public sealed interface CostSource {

    /// Spells / abilities referenced by a subject ("Spells you cast…",
    /// "Creature spells…").
    record Spell(Subject subject) implements CostSource {}

    /// A keyword ability's cost ("Buyback costs", "Kicker costs", …).
    /// `payer` narrows the modifier to costs paid by a specific player
    /// (Catalyst Stone: "Flashback costs you pay cost {2} less.";
    /// "Flashback costs your opponents pay cost {2} more."). Null means
    /// the modifier applies regardless of payer (the common case,
    /// Rooftop Storm etc.).
    record Ability(CostKeyword keyword, @Nullable Subject payer) implements CostSource {
        public Ability(CostKeyword keyword) {
            this(keyword, null);
        }
    }
}
