package be.imgn.mtg.engine.oracle2.domain.selector;

import static java.util.Objects.requireNonNull;

/// Top-level marker for an object selection (CR 109.1). Permits the
/// canonical zone-rooted form via [ZoneSelector], the source
/// self-reference via [SelfSelector], and [ObjectSelector.Target]
/// which marks the selection as a target on the spell or ability
/// (CR 115.1). Per-axis predicates (characteristics, controller,
/// status, counters, etc.) live as [ObjectPropertySelector] arms one
/// layer down, inside an [ObjectTypeSelector] record's `where` slot.
/// The strict envelope is `Quantifier → Target? → Zone → ObjectType
/// → Property`.
public sealed interface ObjectSelector extends Selector
        permits ZoneSelector, SelfSelector, ObjectSelector.SharedSubject, ObjectSelector.Target {

    /// "target X" on an object axis ({@mtg.rule 115.1}). The wrapped
    /// `inner` is itself an [ObjectSelector] so the targeting marker
    /// composes uniformly with every other object arm.
    record Target(ObjectSelector inner) implements ObjectSelector {
        public Target {
            requireNonNull(inner);
        }
    }

    /// Placeholder for the shared subject of an enclosing
    /// [be.imgn.mtg.engine.oracle2.domain.effect.SharedSubjectEffect]
    /// on the object axis. Never produced by a parser directly; only
    /// synthesized by the effect parser when fanning an object-axis
    /// subject across multiple verb clauses.
    enum SharedSubject implements ObjectSelector {
        INSTANCE
    }
}
