package be.imgn.mtg.engine.oracle.domain;

/// A spend restriction on a [Mana] payload (rule 106.6). Today only
/// the "Spend this mana only to/on …" shape is modelled; the
/// restriction body is captured as free text initially, then narrowed
/// over time into typed variants (e.g. `ToCastSpells(SpellCriterion)`,
/// `ToActivateAbilities(SourceCriterion)`, `ToPayCost(CostKind)`).
public sealed interface Restriction {

    /// "Spend this mana only \[to|on\] <text>." — the body is held
    /// verbatim until the restriction-body grammar is typed.
    record SpendOnly(String text) implements Restriction {}
}
