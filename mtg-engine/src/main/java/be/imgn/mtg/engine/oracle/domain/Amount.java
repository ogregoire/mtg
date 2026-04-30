package be.imgn.mtg.engine.oracle.domain;

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

    /// An arithmetic difference of amounts, as in "that many cards minus one"
    /// (Dark Deal).
    record Minus(Amount left, Amount right) implements Amount {}

    /// "one / <amount> for each [subject] [in zone]" — a count-expression that
    /// equals the number of objects matching `subject`, optionally
    /// scoped to a specific zone (e.g., "for each card in your hand").
    record CountOf(
            Subject subject,
            Zone.@Nullable Owned zone,
            @Nullable Integer maximum) implements Amount {
        public CountOf(Subject subject) {
            this(subject, null, null);
        }

        public CountOf(Subject subject, Zone.@Nullable Owned zone) {
            this(subject, zone, null);
        }

        public CountOf withMaximum(int max) {
            return new CountOf(subject, zone, max);
        }
    }

    /// "equal to [subject]'s [property]" — the amount is the named
    /// characteristic of the referenced object (e.g., Soul's Grace: "You
    /// gain life equal to target creature's power.").
    record PropertyOf(Subject subject, Property property) implements Amount {}

    /// Size (card count) of a named zone — used where oracle text refers
    /// to "\[possessive\] library" or similar as a numeric quantity
    /// (Traumatize: "half their library"). Distinct from
    /// [PropertyOf] since a zone is not a characteristic.
    record ZoneSize(Zone.Owned zone) implements Amount {}

    /// "half of [base] [rounded up/down]" — an arithmetic half. `rounding`
    /// is `null` when the parser hasn't yet resolved the direction: either
    /// because the inline "\[, rounded up|down]" suffix didn't fire or
    /// because the card uses the whole-clause "Round up/down each time."
    /// directive that's applied by a later post-pass. A `null` reaching
    /// the resolver is a hard error — the parser must fully specialize
    /// every [Half] before the AST leaves its hands.
    /// Rounding direction for fractional amounts ([Half], [Third]).
    enum Rounding {
        UP,
        DOWN
    }

    record Half(Amount base, @Nullable Rounding rounding) implements Amount {
        public Half(Amount base) {
            this(base, null);
        }

        public Half withRounding(Rounding rounding) {
            return new Half(base, rounding);
        }
    }

    /// "a third of \[possessive\] life\[, rounded up/down\]" — one-third
    /// of the player's life total (Dire Fleet Ravager).
    record Third(Amount base, @Nullable Rounding rounding) implements Amount {
        public Third(Amount base) {
            this(base, null);
        }

        public Third withRounding(Rounding rounding) {
            return new Third(base, rounding);
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

    /// "any number" — unbounded count, chooser picks zero or more
    /// (Boulderfall: "deals 5 damage divided as you choose among any
    /// number of targets."). Distinct from [AtLeast] (1+) in that
    /// the lower bound is zero.
    enum AnyNumber implements Amount {
        ANY_NUMBER
    }

    /// "all" — sweep all matching objects/counters (Aether Snap: "Remove
    /// all counters from each permanent."; Leeches: "Target player loses
    /// all poison counters."). Singleton; the enclosing context names
    /// what "all" ranges over.
    enum All implements Amount {
        ALL
    }

    /// "twice [base]" — double an underlying amount (Boon Reflection:
    /// "you gain twice that much life instead").
    record Times(int factor, Amount base) implements Amount {}

    /// "the \[greatest|lowest\] \[property\] among \[subject\]" — an extremum
    /// of a property computed across the objects matching `subject`
    /// (One with the Machine: "the greatest mana value among artifacts
    /// you control"; Repay in Kind: "the lowest life total among all
    /// players").
    record Extremum(Kind kind, Property property, Subject subject) implements Amount {
        public enum Kind {
            GREATEST,
            LOWEST
        }
    }

    /// "the damage \[already\|so far\]? dealt to \[subject\] \[so far\]?
    /// this turn \[by \[source\]\]?" — turn-history reference to total
    /// damage dealt to a subject in the current turn, optionally
    /// narrowed to a specific source (Final Punishment: "life equal to
    /// the damage already dealt to that player this turn."; Reverse
    /// Polarity: "twice the damage dealt to you so far this turn by
    /// artifacts.").
    record DamageDealtThisTurn(Subject target, @Nullable Subject by) implements Amount {
        public DamageDealtThisTurn(Subject target) {
            this(target, null);
        }

        public DamageDealtThisTurn withBy(Subject by) {
            return new DamageDealtThisTurn(target, by);
        }
    }

    /// "as many \[cards\] as \[who\] discarded this way" — back-reference to
    /// the count of cards discarded by `who` in a preceding Discard clause
    /// of the same resolution (Forget: "Target player discards two cards,
    /// then draws as many cards as they discarded this way.").
    record CardsDiscardedThisWay(Subject who) implements Amount {}

    /// "N life \[who\] gained" — back-reference to the life gained by `who`
    /// in the trigger event that caused this effect (False Cure: "loses 2
    /// life for each 1 life they gained"). The `factor` is the amount
    /// lost per unit of life gained.
    record LifeGainedThisWay(Subject who) implements Amount {}

    /// "the amount of life \[who\] gained this turn" — turn-history
    /// reference to the total life gained by `who` in the current turn
    /// (Voracious Wurm: "where X is the amount of life you've gained
    /// this turn."). Analogous to [DamageDealtThisTurn] for life gain.
    record LifeGainedThisTurn(Subject who) implements Amount {}

    /// "the difference" — back-reference to the numeric delta introduced by
    /// a preceding comparison condition (Balance of Power: "If target
    /// opponent has more cards in hand than you, draw cards equal to the
    /// difference."). Singleton — the comparison is carried by the
    /// enclosing condition.
    enum Difference implements Amount {
        DIFFERENCE
    }

    /// "all but \[N\]" — the total minus an exception amount (Temple
    /// Altisaur: "prevent all but 1 of that damage"; Forcefield: "prevent
    /// all but 1 of that damage"). Used inside prevention effects where
    /// [exception] specifies how much damage passes through.
    record AllBut(Amount exception) implements Amount {}

    /// "the total \[property\] of \[subject\]" — the sum of a property
    /// across all objects matching `subject` (Ancient Ooze: "the total
    /// mana value of other creatures you control"). Analogous to
    /// [Extremum] (which takes greatest/lowest of a set) but computes
    /// the aggregate sum instead.
    record TotalPropertyOf(Subject subject, Property property) implements Amount {}

    /// "each time \[who\] cast \[what\] from \[zone\] this game" — game-scoped
    /// count of past cast events matching `what`, optionally narrowed by
    /// source zone (Commander's Insignia: "for each time you've cast your
    /// commander from the command zone this game").
    record CastCount(Selector what, Zone.@Nullable Owned from) implements Amount {
        public CastCount(Selector what) {
            this(what, null);
        }

        public CastCount withFrom(Zone.Owned from) {
            return new CastCount(what, from);
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
