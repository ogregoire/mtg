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
        permits ZoneSelector, SelfSelector, ObjectSelector.Bound, ObjectSelector.Target {

    /// "target X" on an object axis ({@mtg.rule 115.1}). The wrapped
    /// `inner` is itself an [ObjectSelector] so the targeting marker
    /// composes uniformly with every other object arm.
    record Target(ObjectSelector inner) implements ObjectSelector {
        public Target {
            requireNonNull(inner);
        }
    }

    /// Back-reference to the object bound by the nearest enclosing
    /// binding scope. Two producer paths funnel into the same marker
    /// so a single engine-side walk resolves both:
    ///
    /// - **Leaf-emitted** at anaphoric pronoun sites — "it", "itself",
    ///   "them", "that creature", "that planeswalker" — by
    ///   [be.imgn.mtg.engine.oracle2.parser.selector.ObjectSelectorParser].
    ///   The binding scope is whatever wrapper introduces the
    ///   antecedent (a trigger event's `subject`, a sibling-shared
    ///   subject, a previously-chosen target).
    /// - **Synthesized** by [be.imgn.mtg.engine.oracle2.parser.effect.EffectParser]
    ///   when fanning a subject across the sibling clauses of
    ///   [be.imgn.mtg.engine.oracle2.domain.effect.SharedSubjectEffect]
    ///   or the verb-choice alternatives of
    ///   [be.imgn.mtg.engine.oracle2.domain.effect.ChoiceEffect]. The
    ///   binding scope here is the wrapper itself, with its `subject`
    ///   field holding the bound value.
    ///
    /// The engine resolves [#OBJECT] at evaluation time by walking the
    /// surrounding AST to the nearest binding scope and reading its
    /// bound subject. The axis (ObjectSelector) is preserved so
    /// object-narrowed slots can hold the marker without a cast.
    enum Bound implements ObjectSelector {
        OBJECT
    }
}
