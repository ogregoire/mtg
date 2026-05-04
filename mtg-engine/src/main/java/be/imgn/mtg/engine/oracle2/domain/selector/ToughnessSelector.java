package be.imgn.mtg.engine.oracle2.domain.selector;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.AmountMatcher;

/// Selects a creature by its toughness ({@mtg.rule 208}). Two arms:
///
/// - [HasToughness] — "with toughness [matcher]" ("creature with
///   toughness 2 or less"). Bound is an [AmountMatcher].
/// - [SharesToughnessWith] — "with the same toughness as X"
///   (relational equality).
public sealed interface ToughnessSelector extends CharacteristicSelector
        permits ToughnessSelector.HasToughness, ToughnessSelector.SharesToughnessWith {

    /// "with toughness [matcher]" — numeric comparison.
    record HasToughness(AmountMatcher matcher) implements ToughnessSelector {
        public HasToughness {
            requireNonNull(matcher);
        }
    }

    /// "with the same toughness as X" — equal toughness to the
    /// referenced object.
    record SharesToughnessWith(ObjectSelector with) implements ToughnessSelector {
        public SharesToughnessWith {
            requireNonNull(with);
        }
    }
}
