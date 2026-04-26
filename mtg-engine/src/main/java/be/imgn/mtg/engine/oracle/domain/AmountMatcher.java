package be.imgn.mtg.engine.oracle.domain;

/// A structural predicate over a quantity — "matches if the runtime
/// value satisfies the relation against the wrapped \[#amount()\]".
/// Used exclusively by [Condition] variants whose semantics is
/// "compare a counted thing to an oracle-text bound" (Convalescence
/// "if you have 10 or less life", Deep-Sea Terror "unless there are
/// seven or more cards in your graveyard", Bountiful Promenade
/// "unless you have two or more opponents", Idle Thoughts "if you
/// have no cards in hand").
///
/// Distinct from [Amount] — `Amount` is the quantity itself
/// ("draw 3 cards", "deal 4 damage"), `AmountMatcher` is the
/// relational predicate over a quantity. Effects that produce or
/// consume a count keep using `Amount`; conditions that *compare* a
/// count use this type.
public sealed interface AmountMatcher {

    /// The bound value the runtime quantity is compared against. For
    /// [InRange] this is the lower bound; consumers that need both
    /// ends should switch on the variant rather than calling this
    /// accessor.
    Amount amount();

    /// "≥ amount" — "N or more", "at least N" (Test of Endurance:
    /// "if you have 50 or more life"; Bountiful Promenade: "two or
    /// more opponents"; Deep-Sea Terror: "seven or more cards in
    /// your graveyard").
    record AtLeast(Amount amount) implements AmountMatcher {}

    /// "≤ amount" — "N or less", "at most N" (Convalescence: "10 or
    /// less life"; Spell Snuff: "5 or less life").
    record AtMost(Amount amount) implements AmountMatcher {}

    /// "== amount" — bare "N", "exactly N", and "no" (folded to
    /// `Exactly(exact(0))`, Idle Thoughts: "no cards in hand";
    /// Near-Death Experience: "exactly 1 life").
    record Exactly(Amount amount) implements AmountMatcher {}

    /// "min ≤ value ≤ max" — closed range. Reserved for future
    /// shapes like "between N and M".
    record InRange(Amount min, Amount max) implements AmountMatcher {
        @Override
        public Amount amount() {
            return min;
        }
    }
}
