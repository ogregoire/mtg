package be.imgn.mtg.engine.oracle2.domain;

import static java.util.Objects.requireNonNull;

/// A structural predicate over a quantity — "matches if the runtime
/// value satisfies the relation against the wrapped [#amount()]".
/// Used wherever oracle text *compares* a count to a bound: selector
/// predicates ("creature with mana value 3 or greater"), conditions
/// ("if you have 10 or less life"), and similar.
///
/// Distinct from [Amount] — `Amount` is the quantity itself ("draw 3
/// cards", "deal 4 damage"), `AmountMatcher` is the relational
/// predicate over a quantity.
public sealed interface AmountMatcher {

    /// The bound value the runtime quantity is compared against. For
    /// [InRange] this returns the lower bound; switch on the variant
    /// when both bounds are needed.
    Amount amount();

    /// "≥ amount" — "N or more", "N or greater", "at least N".
    record AtLeast(Amount amount) implements AmountMatcher {
        public AtLeast {
            requireNonNull(amount);
        }
    }

    /// "≤ amount" — "N or less", "at most N".
    record AtMost(Amount amount) implements AmountMatcher {
        public AtMost {
            requireNonNull(amount);
        }
    }

    /// "== amount" — bare "N", "exactly N", and "no" (folded to
    /// `Exactly(Exact(0))`).
    record Exactly(Amount amount) implements AmountMatcher {
        public Exactly {
            requireNonNull(amount);
        }
    }

    /// "min ≤ value ≤ max" — closed range. Reserved for future shapes
    /// like "between N and M".
    record InRange(Amount min, Amount max) implements AmountMatcher {
        public InRange {
            requireNonNull(min);
            requireNonNull(max);
        }

        @Override
        public Amount amount() {
            return min;
        }
    }
}
