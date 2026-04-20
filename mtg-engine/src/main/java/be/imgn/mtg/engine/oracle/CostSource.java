package be.imgn.mtg.engine.oracle;

/// Source of a cost that a [Effect.ModifyCost] effect modifies: either
/// spells matching a [Subject], or a keyword ability's variable cost
/// ("buyback costs", "kicker costs", …) per rule 702.1a.
public sealed interface CostSource {

    /// Spells / abilities referenced by a subject ("Spells you cast…",
    /// "Creature spells…").
    record Spell(Subject subject) implements CostSource {}

    /// A keyword ability's cost ("Buyback costs", "Kicker costs", …).
    record Ability(CostKeyword keyword) implements CostSource {}
}
