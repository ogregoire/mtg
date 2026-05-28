package be.imgn.mtg.engine.oracle2.domain;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// A numeric expression — used wherever oracle text states a count
/// (selection count, damage, life loss/gain, draw count, etc.).
/// Extends [Quantifier] so a numeric amount can also serve as the
/// count axis on a selection.
///
/// Variants:
/// - [Standard] for bare-word numerics (X, that-many).
/// - [Exact] / [UpTo] / [Range] for explicit numeric forms.
/// - [CountOf] for "for each X" / "the number of X" forms.
/// - [PowerOf] / [ToughnessOf] / [ManaValueOf] for
///   "equal to X's \<property\>" forms.
///
/// Future variants (e.g., "twice X", "X plus Y") get added as new
/// permitted records when oracle text needs them — not pre-emptively.
public sealed interface Amount extends Quantifier
        permits Amount.Standard,
                Amount.Exact,
                Amount.UpTo,
                Amount.Range,
                Amount.CountOf,
                Amount.PowerOf,
                Amount.ToughnessOf,
                Amount.ManaValueOf,
                Amount.Plus {

    /// Bare-word numeric atoms — values whose count is determined
    /// elsewhere (cost or context) rather than written as a literal.
    enum Standard implements Amount {
        /// "X" — variable bound by the spell or ability's cost
        /// ({@mtg.rule 107.3}).
        X,
        /// "that many" — back-reference to a count established
        /// earlier in the resolution (e.g., damage just dealt, cards
        /// just drawn).
        REFERENCE
    }

    /// Explicit count — "two", "three", etc.
    record Exact(int n) implements Amount {}

    /// "up to N" — between 0 and N inclusive. The bound is itself an
    /// [Amount] so it can be a literal ([Exact]) or a variable
    /// ([Standard#X]); the parser restricts it to those two shapes,
    /// no recursive `UpTo(UpTo(...))` etc.
    record UpTo(Amount n) implements Amount {
        public UpTo {
            requireNonNull(n);
        }
    }

    /// "N or M" / "between N and M" — inclusive range.
    record Range(int min, int max) implements Amount {}

    /// "for each X" / "the number of X" — count over a referenced
    /// selector. `subject` is the broad [Selector] because real
    /// oracle text in this position covers both objects ("for each
    /// creature you control", "for each card revealed this way")
    /// and players ("for each opponent").
    record CountOf(Selector subject) implements Amount {
        public CountOf {
            requireNonNull(subject);
        }
    }

    /// "X's power" — power of the referenced object ({@mtg.rule
    /// 208.1}). Narrowed to [ObjectSelector] because only objects
    /// have power; a `PlayerSelector` here would be ill-typed.
    record PowerOf(ObjectSelector subject) implements Amount {
        public PowerOf {
            requireNonNull(subject);
        }
    }

    /// "X's toughness" — toughness of the referenced object
    /// ({@mtg.rule 208.1}). [ObjectSelector] only.
    record ToughnessOf(ObjectSelector subject) implements Amount {
        public ToughnessOf {
            requireNonNull(subject);
        }
    }

    /// "X's mana value" — mana value of the referenced object
    /// ({@mtg.rule 202.3}). [ObjectSelector] only.
    record ManaValueOf(ObjectSelector subject) implements Amount {
        public ManaValueOf {
            requireNonNull(subject);
        }
    }

    /// "X plus N" — composite amount: a base [Amount] plus an integer
    /// offset. Vitalizing Cascade ("You gain X plus 3 life.").
    /// `base` is the broader [Amount] so the offset can attach to any
    /// numeric form (`X`, `the number of …`, an explicit literal, …).
    record Plus(Amount base, int offset) implements Amount {
        public Plus {
            requireNonNull(base);
        }
    }
}
