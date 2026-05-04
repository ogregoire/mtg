package be.imgn.mtg.engine.oracle2.domain.selector;

/// Top-level marker for an object selection (CR 109.1). Permits only
/// two arms — the canonical zone-rooted form via [ZoneSelector] and
/// the source self-reference via [SelfSelector]. Per-axis predicates
/// (characteristics, controller, status, counters, etc.) live as
/// [ObjectPropertySelector] arms one layer down, inside an
/// [ObjectTypeSelector] record's `where` slot. The strict envelope is
/// `Quantifier → Target? → Zone → ObjectType → Property`.
public sealed interface ObjectSelector extends Selector permits ZoneSelector, SelfSelector {}
