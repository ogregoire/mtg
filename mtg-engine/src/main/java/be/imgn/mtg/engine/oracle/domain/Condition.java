package be.imgn.mtg.engine.oracle.domain;

import java.util.List;

import org.jspecify.annotations.Nullable;

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

    /// "it's your turn" — turn-owner check (Fated Retribution: "If
    /// it's your turn, scry 2."). The implicit owner is `you`; if
    /// future cards introduce other owners ("if it's an opponent's
    /// turn") add a `Subject who` slot.
    enum ItsYourTurn implements Condition {
        IT_IS_YOUR_TURN_IF;

        @Override
        public Kind kind() {
            return Kind.IF;
        }
    }

    /// "\[player\] attacked this turn" — combat-history check (Chart
    /// a Course: "Then discard a card unless you attacked this
    /// turn.").
    record AttackedThisTurn(Kind kind, Subject who) implements Condition {}

    /// "\[player\] played a land this turn" — land-play history
    /// check (River of Tears: "If you played a land this turn, add
    /// {B} instead.").
    record PlayedLandThisTurn(Kind kind, Subject who) implements Condition {}

    /// "\<self\> attacked during \[player\]'s last turn" — past-turn
    /// combat history (Giant Turtle: "This creature can't attack if
    /// it attacked during your last turn.").
    record AttackedDuringLastTurn(Kind kind, Subject who, Subject ownerOfLastTurn) implements Condition {}

    /// "\<self\> ha\[s\|ve\] a \<counter\> counter on \<self\>" —
    /// counter-presence check (Pipsqueak, Rebel Strongarm:
    /// "Pipsqueak can't attack alone unless he has a +1/+1 counter
    /// on him.").
    record HasCounter(Kind kind, Subject who, CounterType counter, Subject on) implements Condition {}

    /// "\<self\> [is\|isn't\] a \<card-type\>" — type check on a
    /// demonstrative reference (Topple the Statue: "If it's an
    /// artifact, destroy it."; Fa'adiyah Seer / Sindbad: "If it isn't
    /// a land card, discard it."). `negated=true` for "isn't" wording.
    record IsCardType(Kind kind, Subject what, boolean negated, CardType type) implements Condition {
        public IsCardType(Kind kind, Subject what, CardType type) {
            this(kind, what, false, type);
        }
    }

    /// "\<self\> was \[a\|an\] \<supertype\>? \<subtype\>?
    /// \<card-type\> spell?" — past-state type check on a destroyed
    /// permanent or a resolved spell (Thermokarst: "If that land
    /// was a snow land, …"; Jace's Defeat: "If it was a Jace
    /// planeswalker spell, …"). Both `supertype` and `subtype` are
    /// optional; `asSpell=true` for the "\[…\] spell" wording (the
    /// match was a stack object rather than a permanent).
    record WasCardType(
            Kind kind,
            Subject what,
            @Nullable Supertype supertype,
            @Nullable Subtype subtype,
            CardType type,
            boolean asSpell)
            implements Condition {
        public WasCardType(Kind kind, Subject what, @Nullable Supertype supertype, CardType type) {
            this(kind, what, supertype, null, type, false);
        }
    }

    /// "\[player\] cast \<self\> \[from \<zone\>\]?" — cast-source
    /// history check (Iridescent Tiger: "if you cast it"; Coal
    /// Stoker: "if you cast it from your hand"). Optional `from`
    /// zone narrows the cast origin.
    record WasCastBy(Kind kind, Subject who, Subject what, Zone.@Nullable Source from) implements Condition {
        public WasCastBy(Kind kind, Subject who, Subject what) {
            this(kind, who, what, null);
        }
    }

    /// "\[player\] win\[s\] the flip" — coin-flip outcome check
    /// (Tavern Swindler: "If you win the flip, you gain 6 life.").
    record WonFlip(Kind kind, Subject who) implements Condition {}

    /// "\[player\] sacrifice\[s\] \<subject\>" — typed sacrifice-gate
    /// condition (Plant Elemental, Rogue Elephant: "sacrifice it
    /// unless you sacrifice a Forest."; Mold Demon: "unless you
    /// sacrifice two Swamps.").
    record PlayerSacrifices(Kind kind, Subject who, Subject what) implements Condition {}

    /// "\[player\] discard\[s\] \<subject\>" — typed discard-gate
    /// condition (Wrench Mind: "discards two cards unless they
    /// discard an artifact card."; Fallow Wurm, Thundering Wurm:
    /// "unless you discard a land card.").
    record PlayerDiscards(Kind kind, Subject who, Subject what) implements Condition {}

    /// "\[player\] ha\[s\|ve\] cast \<spell-selector\> this turn" —
    /// cast-history check this turn (Gigastorm Titan: "if you've cast
    /// another spell this turn."; Goblin Cohort: "unless you've cast
    /// a creature spell this turn.").
    record CastThisTurn(Kind kind, Subject who, Selector what) implements Condition {}

    /// "\<subject\> attack\[s\]" — combat-action check (Viashino Bey:
    /// "If this creature attacks, …"; Ekundu Cyclops: "If a creature
    /// you control attacks, …"). One-shot check on whether the named
    /// subject is currently attacking (rule 506).
    record SubjectAttacks(Kind kind, Subject who) implements Condition {}

    /// "\<subject\> was blocked this turn" — past-blocked-state check
    /// (Fyndhorn Druid: "if it was blocked this turn, …").
    record WasBlockedThisTurn(Kind kind, Subject who) implements Condition {}

    /// "\<subject\> [was|wasn't] blocking" — past-blocking-state
    /// check (Guildsworn Prowler: "if it wasn't blocking, …").
    /// `negated=true` for "wasn't blocking".
    record WasBlocking(Kind kind, Subject who, boolean negated) implements Condition {}

    /// "\<subject\> died this turn" — past-death check (Life Goes
    /// On: "If a creature died this turn, …").
    record DiedThisTurn(Kind kind, Subject who) implements Condition {}

    /// "\<subject\> had a \<counter\> counter on \<subject\>" —
    /// past counter-presence check (Promising Duskmage: "if it had
    /// a +1/+1 counter on it, …"). Distinct from [HasCounter] in
    /// that the check is on the permanent's last existence state.
    record HadCounter(Kind kind, Subject who, CounterType counter, Subject on) implements Condition {}

    /// "\<subject\> share\[s\] a color with \<subject\>" — color-
    /// equality check (Jaded Response: "if it shares a color with a
    /// creature you control.").
    record SharesColorWith(Kind kind, Subject who, Subject other) implements Condition {}

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
