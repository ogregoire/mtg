package be.imgn.mtg.engine.oracle.domain2.selector;

import static java.util.Objects.requireNonNull;

import java.util.List;

/// Predicate over a single game object — the inner content of an
/// [ObjectTypeSelector] record's `where` slot. Permits the
/// characteristic axes (CR 109.3) via [CharacteristicSelector], the
/// non-characteristic axes (controller, owner, status, designation,
/// counters, stickers, combat status), the relational axes
/// ([AttachesToSelector], [OtherObjectSelector]), and the nested
/// boolean composers + attachment hosts.
///
/// Deliberately does NOT extend [ObjectSelector] — that's how the
/// strict envelope is enforced. [QuantifierSelector] only accepts
/// [Selector], so a property cannot sit at the top of a selection
/// tree; it can only appear inside [ObjectTypeSelector.Permanent],
/// `Token`, `Spell`, `Ability`, `Copy`, `Card`, or `Emblem`.
public sealed interface ObjectPropertySelector
        permits CharacteristicSelector,
                ControlledBySelector,
                OwnedBySelector,
                StatusSelector,
                ObjectDesignationSelector,
                ObjectCounterSelector,
                StickerSelector,
                CombatStatusSelector,
                AttachesToSelector,
                OtherObjectSelector,
                ObjectPropertySelector.Anything,
                ObjectPropertySelector.AllOf,
                ObjectPropertySelector.AnyOf,
                ObjectPropertySelector.Not,
                ObjectPropertySelector.Enchanted,
                ObjectPropertySelector.Equipped,
                ObjectPropertySelector.Fortified {

    /// Always-true predicate. Canonical filler for the `where` slot
    /// when oracle text imposes no further constraint
    /// ("target two permanents" → `Permanent(ANYTHING)`).
    enum Anything implements ObjectPropertySelector {
        ANYTHING
    }

    /// Boolean conjunction — every nested predicate must hold.
    /// Vacuously true on an empty list (semantically equivalent to
    /// [Anything#ANYTHING]).
    record AllOf(List<ObjectPropertySelector> selectors) implements ObjectPropertySelector {
        public AllOf {
            selectors = List.copyOf(selectors);
        }
    }

    /// Boolean disjunction — at least one nested predicate must hold.
    /// Vacuously false on an empty list.
    record AnyOf(List<ObjectPropertySelector> selectors) implements ObjectPropertySelector {
        public AnyOf {
            selectors = List.copyOf(selectors);
        }
    }

    /// Boolean negation.
    record Not(ObjectPropertySelector selector) implements ObjectPropertySelector {
        public Not {
            requireNonNull(selector);
        }
    }

    /// "enchanted creature/permanent/land" — Aura host
    /// ({@mtg.rule 702.5}). The host is selected; `by` references the
    /// Aura (typically [SelfSelector#SELF] when a card refers to "the
    /// creature this Aura enchants").
    record Enchanted(ObjectSelector by) implements ObjectPropertySelector {
        public Enchanted {
            requireNonNull(by);
        }
    }

    /// "equipped creature" — Equipment host ({@mtg.rule 702.6}).
    record Equipped(ObjectSelector by) implements ObjectPropertySelector {
        public Equipped {
            requireNonNull(by);
        }
    }

    /// "fortified land" — Fortification host ({@mtg.rule 702.67}).
    record Fortified(ObjectSelector by) implements ObjectPropertySelector {
        public Fortified {
            requireNonNull(by);
        }
    }
}
