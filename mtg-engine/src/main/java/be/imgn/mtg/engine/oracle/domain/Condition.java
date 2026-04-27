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

    /// "if you don't" / "if they don't" — back-reference to whether
    /// a preceding [Effect.Optional] action was *not* taken (Blood
    /// Crypt: "you may pay 2 life. If you don't, it enters
    /// tapped.").
    enum YouDidNotDoIt implements Condition {
        YOU_DID_NOT_DO_IT;

        @Override
        public Kind kind() {
            return Kind.IF;
        }
    }

    /// "Otherwise, …" — back-reference to the *negation* of the
    /// condition gating the preceding clause (Phyrexian Boon:
    /// "Enchanted creature gets +2/+1 as long as it's black.
    /// Otherwise, it gets -1/-2."). The referent is the predicate of
    /// the most recently parsed conditional/duration clause; the
    /// engine resolves it at evaluation time by negating that
    /// predicate.
    enum Otherwise implements Condition {
        OTHERWISE;

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
    /// control" / "don't control". `past=true` for past-tense
    /// "controlled" (Boomerang Basics: "If you controlled that
    /// permanent, draw a card.").
    record PlayerControls(Kind kind, Subject who, boolean negated, boolean past, Subject what) implements Condition {
        public PlayerControls(Kind kind, Subject who, Subject what) {
            this(kind, who, false, false, what);
        }

        public PlayerControls(Kind kind, Subject who, boolean negated, Subject what) {
            this(kind, who, negated, false, what);
        }
    }

    /// "\[player\] ha\[s\|ve\] \<count\> card\[s\] in hand" — hand-size
    /// check (Idle Thoughts: "Draw a card if you have no cards in
    /// hand."). `count` compares the runtime hand size against an
    /// oracle-text bound; "no cards" is `Exactly(exact(0))`.
    record CardsInHand(Kind kind, Subject who, AmountMatcher count) implements Condition {}

    /// "\[player\] [has|have] \[N\] cards in [their|your] library" —
    /// library-size gate (Battle of Wits: "if you have 200 or more
    /// cards in your library").
    record CardsInLibrary(Kind kind, Subject who, AmountMatcher count) implements Condition {}

    /// "a \[zone\] has \[N\] cards in it" — count over a single zone
    /// instance (Visions of Beyond: "If a graveyard has twenty or
    /// more cards in it, draw three cards instead."). Existential
    /// over the named zone — true if *any* such zone has the
    /// matching count.
    record AnyZoneHasCards(Kind kind, ZoneName zone, AmountMatcher count) implements Condition {}

    /// "\<self\> was kicked" — kicker-status check on the targeted
    /// spell or self-reference (Ertai's Trickery: "Counter target
    /// spell if it was kicked.").
    record WasKicked(Kind kind, Subject what) implements Condition {}

    /// "\<self\> is equipped" — equipped-state check (Training Drone:
    /// "This creature can't attack or block unless it's equipped.").
    record IsEquipped(Kind kind, Subject what) implements Condition {}

    /// "\<self\> is enchanted" — enchanted-state check (Krond the
    /// Dawn-Clad: "Whenever Krond attacks, if it's enchanted, exile
    /// target permanent.").
    record IsEnchanted(Kind kind, Subject what) implements Condition {}

    /// "\<self\> is paired with \<selector\>" — soulbond pairing
    /// check (Flowering Lumberknot: "This creature can't attack or
    /// block unless it's paired with a creature with soulbond.").
    /// Rule 702.93.
    record IsPairedWith(Kind kind, Subject what, Selector pair) implements Condition {}

    /// "\[player\] is poisoned" — poison-status check (Corrupted
    /// Resolve: "Counter target spell if its controller is poisoned.").
    /// Per rule 704.5c, a player is "poisoned" when they have ≥ 1
    /// poison counter.
    record IsPoisoned(Kind kind, Subject who) implements Condition {}

    /// "\[player\] is the monarch" — monarchy-status check (Throne
    /// Warden: "At the beginning of your end step, if you're the
    /// monarch, put a +1/+1 counter on this creature."). Rule 716.
    record IsTheMonarch(Kind kind, Subject who) implements Condition {}

    /// "no mana was spent to cast \<self\>" — pay-cost check (Nix:
    /// "Counter target spell if no mana was spent to cast it.").
    record NoManaSpentToCast(Kind kind, Subject spell) implements Condition {}

    /// "\[if|unless\] that mana is spent on \<subject\>" — mana-rider gate
    /// (Carnelian Orb of Dragonkind: "If that mana is spent on a Dragon
    /// creature spell, it gains haste until end of turn."; Boseiju, Who
    /// Shelters All: "If that mana is spent on an instant or sorcery spell,
    /// that spell can't be countered.").
    record ThatManaSpentOn(Kind kind, Subject target) implements Condition {}

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

    /// "\<attacker\> attacked \<target\>" — checks that the attacker directed
    /// their attacks at the specified target (Ever-Watching Threshold:
    /// "if they attacked you and/or a planeswalker you control"). The
    /// `target` may be a [Subject.OneOf] for "and/or" targets.
    record AttackedTarget(Kind kind, Subject attacker, Subject target) implements Condition {}

    /// "\<subject\> [has\|hasn't] dealt damage yet" — game-history
    /// check on whether the subject has dealt any damage so far
    /// (Palladia-Mors, the Ruiner: "Palladia-Mors has hexproof if it
    /// hasn't dealt damage yet."). `negated=true` for the "hasn't"
    /// wording.
    record HasDealtDamageYet(Kind kind, Subject who, boolean negated) implements Condition {}

    /// "\<player\> \[has\|have\] \<source\> deal \<amount\> damage to
    /// \<target\>" — pay-with-damage gate (Dwarven Driller / Lava
    /// Blister: "unless its controller has this creature deal 2
    /// damage to them."). The condition is satisfied when the
    /// controller chooses to "pay" by routing damage through the
    /// named source.
    record PlayerCausesDamage(Kind kind, Subject who, Subject source, Amount amount, Subject target)
            implements Condition {}

    /// "\<player\> gained \<amount\> life this turn" — life-gain
    /// history check (The Gaffer: "if you gained 3 or more life this
    /// turn, draw a card."). The amount is an [AmountMatcher] so
    /// "3 or more" / "exactly 5" / etc. round-trip structurally.
    record GainedLifeThisTurn(Kind kind, Subject who, AmountMatcher amount) implements Condition {}

    /// "\<subject\> is \<color\>" — color check (Hydroblast: "Counter
    /// target spell if it's red.").
    record IsColor(Kind kind, Subject what, Color color) implements Condition {}

    /// "\<subject\> was \<color\>" — past-tense color check on a
    /// recently-destroyed permanent or resolved spell (Filigree
    /// Fracture: "If that permanent was blue or black, draw a
    /// card."). Disjunctions like "blue or black" compose via
    /// [AnyOf].
    record WasColor(Kind kind, Subject what, Color color) implements Condition {}

    /// "\<subject\> regenerates this way" — back-reference to a
    /// preceding regenerate effect in the same resolution (Debt of
    /// Loyalty: "You gain control of that creature if it regenerates
    /// this way.").
    record RegeneratesThisWay(Kind kind, Subject who) implements Condition {}

    /// "\<subject\> blocked this turn" — combat-history check over a
    /// creature subject (used as one disjunct in Lurker's "attacked
    /// or blocked this turn"). Distinct from [WasBlockedThisTurn]
    /// (passive: was blocked by another creature) and from
    /// [WasBlocking] (currently blocking a specific creature).
    record BlockedThisTurn(Kind kind, Subject who) implements Condition {}

    /// Generic disjunction — `outer-kind` gates whether the
    /// disjunction is an `if` or `unless` (matching the rest of the
    /// condition system); inner alternatives are full
    /// [Condition]s whose own `kind` field is irrelevant under the
    /// wrapper (the parser sets them to `IF` as a neutral
    /// placeholder). Used to compose existing condition variants
    /// rather than inventing combo records like
    /// `AttackedOrBlockedThisTurn`.
    record AnyOf(Kind kind, List<Condition> alternatives) implements Condition {}

    /// "\[player\] played a land this turn" — land-play history
    /// check (River of Tears: "If you played a land this turn, add
    /// {B} instead.").
    record PlayedLandThisTurn(Kind kind, Subject who, boolean negated) implements Condition {
        public PlayedLandThisTurn(Kind kind, Subject who) {
            this(kind, who, false);
        }
    }

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
    /// …"; Eye Gouge: "If it's a Cyclops, …"). `matcher` is a
    /// [TypeMatcher] so the predicate may span the card-type,
    /// subtype, supertype, and game-object axes; "isn't" wording
    /// folds into a [TypeMatcher.Not] leaf.
    record IsType(Kind kind, Subject what, TypeMatcher matcher) implements Condition {}

    /// "\<self\> was \[a\|an\] \<supertype\>? \<subtype\>?
    /// \<card-type\>? spell?" — past-state type check on a destroyed
    /// permanent or a resolved spell (Thermokarst: "If that land
    /// was a snow land, …"; Jace's Defeat: "If it was a Jace
    /// planeswalker spell, …"; Glorious Gale: "If it was a legendary
    /// spell, …"; Helldozer: "If that land was nonbasic, …").
    /// `matcher` is a [TypeMatcher] so the predicate can span
    /// supertype, subtype, card-type, and game-object axes; "non-X"
    /// wording folds into a [TypeMatcher.Not] leaf. `asSpell=true`
    /// for the "\[…\] spell" wording (the match was a stack object
    /// rather than a permanent).
    record WasCardType(Kind kind, Subject what, TypeMatcher matcher, boolean asSpell) implements Condition {
        public WasCardType(Kind kind, Subject what, TypeMatcher matcher) {
            this(kind, what, matcher, false);
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

    /// "\<player\> exile\[s\] \<subject\>" — exile-cost condition
    /// (Grip of Amnesia: "Counter target spell unless its
    /// controller exiles all cards from their graveyard").
    record PlayerExiles(Kind kind, Subject who, Subject what) implements Condition {}

    /// "\<player\> return\[s\] \<subject\> to \[its\|their\|his\|her\] owner's hand" —
    /// player-bounce condition (Tragic Lesson: "discard a card unless
    /// you return a land you control to its owner's hand").
    record PlayerReturns(Kind kind, Subject who, Subject what) implements Condition {}

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

    /// "\[player\] \[has|'ve\] discarded \<subject\> this turn" —
    /// discard-history check (Gilt-Blade Prowler: "Activate only if
    /// you've discarded a card this turn."). `what` is a [Subject]
    /// (typically a Subject.Select over a card type) so it can carry
    /// a full selector grammar.
    record DiscardedThisTurn(Kind kind, Subject who, Subject what) implements Condition {}

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

    /// "\<subject\> share\[s\] a color [with \<subject\>]?" — color-
    /// equality check. Either between two named subjects (Jaded
    /// Response: "if it shares a color with a creature you control.")
    /// or reflexive across the surrounding clause's two subjects
    /// (Well-Laid Plans: "Prevent all damage that would be dealt to a
    /// creature by another creature if they share a color." —
    /// `other=null`).
    record SharesColorWith(Kind kind, Subject who, @Nullable Subject other) implements Condition {
        public SharesColorWith(Kind kind, Subject who) {
            this(kind, who, null);
        }
    }

    /// "\<subject\> is \[tapped\|untapped\]" — tap-state check
    /// (Centaur Omenreader: "As long as this creature is tapped,
    /// …"; Nim Abomination: "if this creature is untapped, …").
    /// `tapped=true` for "is tapped"; `false` for "is untapped".
    record IsTapped(Kind kind, Subject who, boolean tapped) implements Condition {}

    /// "\[player\] [has|have] \<matcher\> opponents" — opponent-count
    /// check (Bountiful Promenade and the other Battlebond double-
    /// control lands: "unless you have two or more opponents.").
    record HasOpponents(Kind kind, Subject who, AmountMatcher count) implements Condition {}

    /// "it's [not] \[player\]'s turn" — turn-owner check. `negated=true`
    /// for "it's not their turn" (Glademuse).
    record IsTurnOwner(Kind kind, Subject who, boolean negated) implements Condition {}

    /// "\[mana-symbol\] was spent to cast \<self\>" — mana-color
    /// payment check (Tin Street Hooligan: "if {G} was spent to cast
    /// it"). The color identity is captured via the symbol; the
    /// engine resolves which color was paid.
    record ManaSpentToCast(Kind kind, List<ManaSymbol> symbols, Subject what) implements Condition {
        public ManaSpentToCast(Kind kind, ManaSymbol symbol, Subject what) {
            this(kind, List.of(symbol), what);
        }
    }

    /// "\<matcher\> \<color\> mana was spent to cast \<self\>" —
    /// Adamant-style color-and-amount condition (Unexplained Vision:
    /// "If at least three blue mana was spent to cast this spell,
    /// scry 3."). Distinct from [#ManaSpentToCast] which is keyed
    /// off a [ManaSymbol] for single-pip checks.
    record ColorManaSpentToCast(Kind kind, AmountMatcher amount, Color color, Subject what) implements Condition {}

    /// "\<matcher\> colored mana was spent to cast \<spell\>" —
    /// color-agnostic colored-mana check (Void Mirror: "if no
    /// colored mana was spent to cast it"). Distinct from
    /// [#ColorManaSpentToCast] which names a single color.
    record ColoredManaSpentToCast(Kind kind, AmountMatcher amount, Subject what) implements Condition {}

    /// "there are \<matcher\> \<subject\>" — existence / count check
    /// on a referenced selector (Deep-Sea Terror: "unless there are
    /// seven or more cards in your graveyard.").
    record CountOf(Kind kind, AmountMatcher count, Subject what) implements Condition {}

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

    /// "\[player\] [has|have] [more|fewer] cards in hand than
    /// \[other-player\]" — hand-size comparison (Balance of Power:
    /// "If target opponent has more cards in hand than you, draw
    /// cards equal to the difference."). Reuses the
    /// [PlayerControlsCompared.ComparatorMore] enum for the
    /// more/fewer axis.
    record CardsInHandCompared(Kind kind, Subject who, PlayerControlsCompared.ComparatorMore cmp, Subject other)
            implements Condition {}

    /// "it targets \<selector\>" — target-of-spell check (Dragon's
    /// Prey: "if it targets a Dragon."). `what` describes the
    /// targeted creature/object class.
    record SpellTargets(Kind kind, Subject spell, Subject what) implements Condition {}

    /// "\[spell/it\] would destroy \<subject\>" — destruction-check
    /// condition (Equinox: "Counter target spell if it would destroy
    /// a land you control."). `spell` is the pronoun or selector
    /// for the spell being evaluated; `what` is the object class
    /// that would be destroyed.
    record SpellWouldDestroy(Kind kind, Subject spell, Subject what) implements Condition {}

    /// "\[player\] ha\[s\|ve\] \<matcher\> life" — life-total
    /// comparison (Convalescence: "if you have 10 or less life";
    /// Near-Death Experience: "if you have exactly 1 life"; Spell
    /// Snuff: "if you have 5 or less life"; Test of Endurance: "if
    /// you have 50 or more life").
    record HasLife(Kind kind, Subject who, AmountMatcher amount) implements Condition {}

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

    /// "\<subject\> is blocked" — present-tense block-state predicate
    /// for currently-blocked attackers (Cinder Crawler: "Activate
    /// only if this creature is blocked."). Distinct from
    /// [#WasBlockedThisTurn] (any time in the turn) and
    /// [#WasBlocking] (defender-side history).
    record IsBlocked(Kind kind, Subject who) implements Condition {}

    /// "\<subject\> was milled this way" — back-reference to the
    /// preceding mill effect (Saprazzan Breaker: "{U}: Mill a
    /// card. If a land card was milled this way, this creature
    /// can't be blocked this turn.").
    record WasMilledThisWay(Kind kind, Subject what) implements Condition {}

    /// "X is \<matcher\>" — comparison on the spell's bound X value
    /// (Martial Coup: "If X is 5 or more, destroy all other
    /// creatures."). The matcher carries the comparator.
    record XValue(Kind kind, AmountMatcher amount) implements Condition {}

    /// "\<player\> lost \<matcher\>? life this turn" — life-loss
    /// history check (Mounted Dreadknight: "if an opponent lost
    /// life this turn"). Bare "lost life" maps to
    /// [AmountMatcher.AtLeast]\(1\). Distinct from
    /// [#GainedLifeThisTurn] (gain history).
    record LostLifeThisTurn(Kind kind, Subject who, AmountMatcher amount) implements Condition {}

    /// "\<amount\> damage was dealt to \<subject\> this turn" —
    /// damage-dealt-with-amount check (Rushing-Tide Zubera: "if 4
    /// or more damage was dealt to it this turn, draw three
    /// cards.").
    record DamageDealtThisTurn(Kind kind, AmountMatcher amount, Subject who) implements Condition {}

    /// "\<subject\>['s] mana value [is|was] \<matcher\>" — mana-value
    /// check on a referenced object (Extinguish the Light: "If its
    /// mana value was 3 or less, you gain 3 life."). Tense-agnostic;
    /// the matcher carries the comparator.
    record HasManaValue(Kind kind, Subject what, AmountMatcher amount) implements Condition {}

    /// "\<subject\> has the same mana value as \<other\>" — mana-value
    /// equality between two objects (Hisoka, Minamo Sensei: "if it has
    /// the same mana value as the discarded card").
    record SameManaValueAs(Kind kind, Subject who, Subject other) implements Condition {}

    /// "\<subject\> is \<P\>/\<T\>" — P/T equality check (Sigil
    /// Captain: "if that creature is 1/1, put two +1/+1 counters on
    /// it."). Distinct from [#IsType] which checks card-type axes.
    record HasPT(Kind kind, Subject what, PtValue pt) implements Condition {}

    /// "\<player\> cast \<spell\> during \<phase\>" — Addendum-style
    /// timing predicate (Sphinx's Insight: "If you cast this spell
    /// during your main phase, you gain 2 life."). The `phase`
    /// reuses [TriggerEvent.AtPhase] so the owner/qualifier slots
    /// are typed (e.g., "your main phase" → owner=YOU, phase=MAIN).
    record CastDuringPhase(Kind kind, Subject who, Subject what, TriggerEvent.AtPhase phase) implements Condition {}

    /// "it's \[day|night\]" — day/night state check (Moonrager's Slash:
    /// "if it's night"; daybound/nightbound abilities). Reuses the
    /// [Effect.BecomeDayNight.DayNight] enum for the state value.
    record IsDayNight(Kind kind, Effect.BecomeDayNight.DayNight state) implements Condition {}

    /// "\[subject\] has \[ability\]" — keyword-presence check (Compleat
    /// Devotion: "If that creature has toxic, draw a card."; Hexgold
    /// Slash: "If that creature has toxic, Hexgold Slash deals 4 damage
    /// to that creature instead."). When `ability` is [Ability.Toxic]
    /// with a null `n`, any toxic level satisfies the check.
    record HasAbility(Kind kind, Subject what, Ability ability) implements Condition {}

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
