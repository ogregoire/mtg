package be.imgn.mtg.engine.oracle;

import org.jspecify.annotations.Nullable;

/// Numeric amount in oracle text.
public sealed interface Amount {
    record Exact(int value) implements Amount {}

    /// The variable amount "X" — a singleton.
    enum Variable implements Amount {
        VARIABLE
    }

    record Reference(String type) implements Amount {}

    record Formula(String expression) implements Amount {}

    /// An arithmetic sum of amounts, as in "X plus 3" or "2 plus that amount".
    record Plus(Amount left, Amount right) implements Amount {}

    /// "one / <amount> for each [subject] [in zone]" — a count-expression that
    /// equals the number of objects matching `subject`, optionally
    /// scoped to a specific zone (e.g., "for each card in your hand").
    record CountOf(Subject subject, Zone.@Nullable Named zone) implements Amount {
        CountOf(Subject subject) {
            this(subject, null);
        }
    }

    /// "equal to [subject]'s [property]" — the amount is the named
    /// characteristic of the referenced object (e.g., Soul's Grace: "You
    /// gain life equal to target creature's power.").
    record PropertyOf(Subject subject, String property) implements Amount {}

    /// "half of [base] [rounded up/down]" — an arithmetic half with explicit
    /// rounding (e.g., Cruel Bargain: "lose half your life, rounded up").
    record Half(Amount base, Rounding rounding) implements Amount {
        public enum Rounding {
            UP,
            DOWN
        }
    }

    /// "N or more" — an inclusive lower bound (Military Intelligence:
    /// "you attack with two or more creatures").
    record AtLeast(int min) implements Amount {}

    /// "N or M" — inclusive range bounded on both sides (Storm of Steel:
    /// "each of one or two targets").
    record Range(int min, int max) implements Amount {}

    /// "up to N" — an inclusive upper bound (Render Inert: "Remove up
    /// to five counters from target permanent.").
    record UpTo(int max) implements Amount {}

    /// "twice [base]" — double an underlying amount (Boon Reflection:
    /// "you gain twice that much life instead").
    record Times(int factor, Amount base) implements Amount {}

    /// "the \[greatest|lowest\] \[property\] among \[subject\]" — an extremum
    /// of a property computed across the objects matching `subject`
    /// (One with the Machine: "the greatest mana value among artifacts
    /// you control"; Repay in Kind: "the lowest life total among all
    /// players").
    record Extremum(Kind kind, String property, Subject subject) implements Amount {
        public enum Kind {
            GREATEST,
            LOWEST
        }
    }

    /// Creates an [Exact] amount.
    static Amount exact(int value) {
        return new Exact(value);
    }

    /// Returns the variable amount "X".
    static Amount variable() {
        return Variable.VARIABLE;
    }

    /// Creates a [Reference] amount.
    static Amount reference(String type) {
        return new Reference(type);
    }

    /// Creates a [Formula] amount.
    static Amount formula(String expression) {
        return new Formula(expression);
    }
}
