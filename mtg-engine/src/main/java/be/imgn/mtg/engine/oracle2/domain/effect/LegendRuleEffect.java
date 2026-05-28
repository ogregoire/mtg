package be.imgn.mtg.engine.oracle2.domain.effect;

/// Effect that disables the legend rule ({@mtg.rule 704.5j}) — the
/// state-based action that culls duplicate legendary permanents.
/// Mirror Gallery: "The \"legend rule\" doesn't apply." is the only
/// printed card with this effect today. Modeled as a single-constant
/// enum because the effect carries no payload; if other rule-
/// disabling effects appear, refactor into a parameterised
/// `RuleDisabledEffect(Rule)`.
public enum LegendRuleEffect implements Effect {
    DOES_NOT_APPLY
}
