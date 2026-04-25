package be.imgn.mtg.engine.oracle.domain;

import java.util.List;

/// A condition attached to an effect or ability — an "if" / "unless" /
/// "as long as" clause. Modelled as a sealed type with structural
/// variants only; there is no free-text fallback. New oracle shapes
/// require a new typed variant rather than a String predicate. The
/// [Kind] distinguishes the semantic flavour: [Kind#IF] resolves the
/// effect only when the predicate holds; [Kind#UNLESS] is the
/// negation; [Kind#AS_LONG_AS] is a continuous predicate gating the
/// enclosing continuous effect.
public sealed interface Condition {

    Kind kind();

    /// "if you do" / "if they do" — back-reference to whether a
    /// preceding [Effect.Optional] action was actually taken
    /// (Inheritance: "you may pay {3}. If you do, draw a card."). No
    /// parameters; the referent is the most recent optional in the
    /// same resolution.
    enum YouDidIt implements Condition {
        YOU_DID_IT;

        @Override
        public Kind kind() {
            return Kind.IF;
        }
    }

    /// "\[player\] both own\[s\] and control\[s\] \<subjects\>" — meld-gate
    /// condition (rule 701.39, Gisela, the Broken Blade: "if you both
    /// own and control Gisela and a creature named Bruna, the Fading
    /// Light"). `who` is the player; `targets` is the conjunction of
    /// objects whose ownership and control is being checked.
    record OwnsAndControls(Kind kind, Subject who, List<Subject> targets) implements Condition {}

    /// "\[player\] pay\[s\] \<cost\>" — typically the right-hand side of
    /// "unless …" on a counterspell or restriction (Clash of Wills:
    /// "Counter target spell unless its controller pays {X}.";
    /// Tyrannize: "Target player discards their hand unless they pay
    /// 7 life."; Qal Sisma Behemoth: "This creature can't attack or
    /// block unless you pay {2}."). The same shape can also appear
    /// as an "if" gate; [#kind] discriminates.
    record PlayerPays(Kind kind, Subject who, Cost cost) implements Condition {}

    /// "\[player\] control\[s\] \<selector\>" — possession check on a
    /// referenced player (Mindless Null: "This creature can't block
    /// unless you control a Vampire."; Desperate Castaways: "This
    /// creature can't attack unless you control an artifact.").
    /// `kind` is usually [Kind#UNLESS] but the same shape supports
    /// [Kind#IF] for symmetric "if you control a Vampire" forms.
    record PlayerControls(Kind kind, Subject who, Selector what) implements Condition {}

    /// "\[player\] ha\[s\|ve\] \<count\> card\[s\] in hand" — hand-size
    /// check (Idle Thoughts: "Draw a card if you have no cards in
    /// hand.").
    record CardsInHand(Kind kind, Subject who, Amount count) implements Condition {}

    /// "\<self\> was kicked" — kicker-status check on the targeted
    /// spell or self-reference (Ertai's Trickery: "Counter target
    /// spell if it was kicked.").
    record WasKicked(Kind kind, Subject what) implements Condition {}

    /// "\<self\> is equipped" — equipped-state check (Training Drone:
    /// "This creature can't attack or block unless it's equipped.").
    record IsEquipped(Kind kind, Subject what) implements Condition {}

    /// "\[player\] is poisoned" — poison-status check (Corrupted
    /// Resolve: "Counter target spell if its controller is poisoned.").
    /// Per rule 704.5c, a player is "poisoned" when they have ≥ 1
    /// poison counter.
    record IsPoisoned(Kind kind, Subject who) implements Condition {}

    /// "no mana was spent to cast \<self\>" — pay-cost check (Nix:
    /// "Counter target spell if no mana was spent to cast it.").
    record NoManaSpentToCast(Kind kind, Subject spell) implements Condition {}

    enum Kind {
        /// "if \[predicate\]" — the enclosing effect resolves only when
        /// the predicate is true.
        IF,
        /// "unless \[predicate\]" — the enclosing effect is countered /
        /// does nothing if the predicate holds; it happens when the
        /// predicate is false (or the opponent declines to meet it).
        UNLESS,
        /// "as long as \[predicate\]" — continuous predicate that
        /// gates the enclosing continuous effect; distinct from
        /// [#IF] (one-shot check at resolution) in that the predicate
        /// is re-checked while the effect is active.
        AS_LONG_AS
    }
}
