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

    /// "\[player\] pay\[s\] \<cost\> \[for each \<scope\>\]?" —
    /// typically the right-hand side of "unless …" on a counterspell
    /// or restriction (Clash of Wills, Tyrannize, Qal Sisma Behemoth).
    /// `scaleBy` is the optional "for each …" multiplier on the cost
    /// (Oppressive Will: "unless its controller pays {1} for each
    /// card in your hand."; Override: "for each artifact you
    /// control."). The same shape can also appear as an "if" gate;
    /// [#kind] discriminates.
    record PlayerPays(
            Kind kind, Subject who, Cost cost, @Nullable Amount scaleBy) implements Condition {
        public PlayerPays(Kind kind, Subject who, Cost cost) {
            this(kind, who, cost, null);
        }
    }

    /// "\[player\] [doesn't|don't]? control\[s\] \<subject\>" —
    /// possession check on a referenced player (Mindless Null:
    /// "This creature can't block unless you control a Vampire.";
    /// War Falcon: "unless you control a Knight or a Soldier.";
    /// Scourge of Numai: "if you don't control an Ogre."). The
    /// subject can be a single [Subject.Select] selector or a
    /// [Subject.OneOf] disjunction. `negated=true` for "doesn't
    /// control" / "don't control".
    record PlayerControls(Kind kind, Subject who, boolean negated, Subject what) implements Condition {
        public PlayerControls(Kind kind, Subject who, Subject what) {
            this(kind, who, false, what);
        }
    }

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

    /// "\<self\> [is\|isn't\] a \<type\>" — type check on a
    /// demonstrative reference (Topple the Statue: "If it's an
    /// artifact, …"; Fa'adiyah Seer / Sindbad: "If it isn't a land
    /// card, …"; Holy Justiciar: "If that creature is a Zombie,
    /// …"; Eye Gouge: "If it's a Cyclops, …"). `type` is a
    /// [Selector.SingleType] so card-type, subtype, or game-object
    /// shapes all share one record. `negated=true` for "isn't"
    /// wording.
    record IsType(Kind kind, Subject what, boolean negated, Selector.SingleType type) implements Condition {
        public IsType(Kind kind, Subject what, Selector.SingleType type) {
            this(kind, what, false, type);
        }
    }

    /// "\<self\> was \[a\|an\] \<supertype\>? \<subtype\>?
    /// \<card-type\>? spell?" — past-state type check on a destroyed
    /// permanent or a resolved spell (Thermokarst: "If that land
    /// was a snow land, …"; Jace's Defeat: "If it was a Jace
    /// planeswalker spell, …"; Glorious Gale: "If it was a legendary
    /// spell, …"). All of `supertype`, `subtype`, and `type` are
    /// optional; at least one must be set. `asSpell=true` for the
    /// "\[…\] spell" wording (the match was a stack object rather
    /// than a permanent).
    record WasCardType(
            Kind kind,
            Subject what,
            @Nullable Supertype supertype,
            @Nullable Subtype subtype,
            @Nullable CardType type,
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

    /// "\[player\] discard\[s\] \<subject\> \[at random\]?" — typed
    /// discard-gate condition (Wrench Mind: "discards two cards
    /// unless they discard an artifact card."; Fallow Wurm,
    /// Thundering Wurm: "unless you discard a land card."; Balduvian
    /// Horde, Minotaur Explorer, Pillaging Horde: "unless you
    /// discard a card at random.").
    record PlayerDiscards(Kind kind, Subject who, Subject what, boolean atRandom) implements Condition {
        public PlayerDiscards(Kind kind, Subject who, Subject what) {
            this(kind, who, what, false);
        }
    }

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

    /// "\<subject\> also attack\[s\]" — co-attacker check (Scarred
    /// Puma: "unless a black or green creature also attacks."). The
    /// "also" implies the enclosing creature is itself attacking;
    /// this gates on a co-attacker matching `who`.
    record AlsoAttacks(Kind kind, Subject who) implements Condition {}

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

    /// "\<subject\> is \[tapped\|untapped\]" — tap-state check
    /// (Centaur Omenreader: "As long as this creature is tapped,
    /// …"; Nim Abomination: "if this creature is untapped, …").
    /// `tapped=true` for "is tapped"; `false` for "is untapped".
    record IsTapped(Kind kind, Subject who, boolean tapped) implements Condition {}

    /// "\[player\] [has|have] \<comparator\> \<amount\> opponents" —
    /// opponent-count check (Bountiful Promenade and the other Battlebond
    /// double-control lands: "unless you have two or more opponents.").
    record HasOpponents(Kind kind, Subject who, HasLife.LifeComparator cmp, Amount amount) implements Condition {}

    /// "it's [not] \[player\]'s turn" — turn-owner check. `negated=true`
    /// for "it's not their turn" (Glademuse).
    record IsTurnOwner(Kind kind, Subject who, boolean negated) implements Condition {}

    /// "\[mana-symbol\] was spent to cast \<self\>" — mana-color
    /// payment check (Tin Street Hooligan: "if {G} was spent to cast
    /// it"). The color identity is captured via the symbol; the
    /// engine resolves which color was paid.
    record ManaSpentToCast(Kind kind, ManaSymbol symbol, Subject what) implements Condition {}

    /// "there are \<comparator\> \<amount\> \<subject\>" — existence
    /// / count check on a referenced selector (Deep-Sea Terror:
    /// "unless there are seven or more cards in your graveyard.").
    record CountOf(Kind kind, HasLife.LifeComparator cmp, Amount amount, Subject what) implements Condition {}

    /// "\[player\] control\[s\] \<comparator\> \<amount\>?
    /// \<selector\> than \<subject\>" — comparison count
    /// (Unified Will: "if you control more creatures than that
    /// spell's controller."). The `comparator` selects whether "you"
    /// must have more / fewer than the other player.
    record PlayerControlsCompared(Kind kind, Subject who, ComparatorMore cmp, Subject what, Subject other)
            implements Condition {
        public enum ComparatorMore {
            MORE,
            FEWER
        }
    }

    /// "it targets \<selector\>" — target-of-spell check (Dragon's
    /// Prey: "if it targets a Dragon."). `what` describes the
    /// targeted creature/object class.
    record SpellTargets(Kind kind, Subject spell, Subject what) implements Condition {}

    /// "\[player\] ha\[s\|ve\] \<comparator\> \<amount\> life" —
    /// life-total comparison (Convalescence: "if you have 10 or less
    /// life"; Near-Death Experience: "if you have exactly 1 life";
    /// Spell Snuff: "if you have 5 or less life").
    record HasLife(Kind kind, Subject who, LifeComparator cmp, Amount amount) implements Condition {
        public enum LifeComparator {
            LESS_THAN_OR_EQUAL,
            GREATER_THAN_OR_EQUAL,
            EQUAL
        }
    }

    /// "an enchantment is on the battlefield" / "\<selector\> is on
    /// the battlefield" — existence check on a referenced selector
    /// (Wirecat: "if an enchantment is on the battlefield"). The
    /// `negated` flag handles the "isn't on the battlefield" form.
    record SelectorOnBattlefield(Kind kind, Subject what, boolean negated) implements Condition {}

    /// "they're \[mana\] abilities" — type check on the implicit
    /// "they" (the abilities being modified — Suppression Field:
    /// "Activated abilities cost {2} more to activate unless
    /// they're mana abilities."). The check is whether the
    /// abilities are mana abilities (rule 605).
    record AreManaAbilities(Kind kind) implements Condition {}

    /// "\[player\] [has|have] been dealt damage this turn" — past
    /// damage history (Bloodcrazed Goblin: "unless an opponent has
    /// been dealt damage this turn.").
    record HasBeenDealtDamageThisTurn(Kind kind, Subject who) implements Condition {}

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
