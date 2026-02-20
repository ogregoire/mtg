package be.imgn.mtg.engine.selector;

/// Represents "with" clauses that add additional criteria to selectors.
public sealed interface WithClause {

    /// Has a specific ability (e.g., "with flying").
    record Ability(String abilityName) implements WithClause {}

    /// Has a mana value comparison (e.g., "with mana value 3 or less").
    record ManaValue(Comparison comparison, int value) implements WithClause {}

    /// Has a power comparison (e.g., "with power 2 or less").
    record Power(Comparison comparison, int value) implements WithClause {}

    /// Has a toughness comparison (e.g., "with toughness 4 or greater").
    record Toughness(Comparison comparison, int value) implements WithClause {}

    /// Has a specific counter type (e.g., "with a +1/+1 counter").
    record Counter(String counterType) implements WithClause {}
}
