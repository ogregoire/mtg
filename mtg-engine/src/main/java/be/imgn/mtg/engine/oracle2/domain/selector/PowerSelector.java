package be.imgn.mtg.engine.oracle2.domain.selector;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.AmountMatcher;

/// Selects a creature by its power ({@mtg.rule 208}). Two arms:
///
/// - [HasPower] — "with power [matcher]" (Eternal Isolation: "target
///   creature with power 4 or greater"). The dominant comparison
///   form. Bound is an [AmountMatcher].
/// - [SharesPowerWith] — "with the same power as X" (relational
///   equality).
public sealed interface PowerSelector extends CharacteristicSelector
        permits PowerSelector.HasPower, PowerSelector.SharesPowerWith {

    /// "with power [matcher]" — numeric comparison. Examples:
    /// - Eternal Isolation: "target creature with power 4 or greater"
    ///   → `HasPower(AtLeast(Exact(4)))`.
    /// - "creature with power 2 or less" → `HasPower(AtMost(Exact(2)))`.
    record HasPower(AmountMatcher matcher) implements PowerSelector {
        public HasPower {
            requireNonNull(matcher);
        }
    }

    /// "with the same power as X" — equal power to the referenced
    /// object.
    record SharesPowerWith(ObjectSelector with) implements PowerSelector {
        public SharesPowerWith {
            requireNonNull(with);
        }
    }
}
