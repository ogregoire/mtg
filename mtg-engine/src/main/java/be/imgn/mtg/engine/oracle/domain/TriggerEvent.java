package be.imgn.mtg.engine.oracle.domain;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.turn.Phase;
import be.imgn.mtg.engine.turn.Step;

/// Structured trigger event for [Ability.TriggeredAbility]. Replaces
/// the prior free-text capture so trigger conditions are recognized by the
/// grammar rather than absorbed verbatim. Each variant corresponds to a
/// common oracle-text shape; add new variants when oracle text introduces
/// new event forms.
public sealed interface TriggerEvent {

    /// "\[subject\] enter\[s\] \[tapped\]? \[during X's turn\]?" (rule
    /// 603.6a). `tapped=true` for shapes like "a permanent you control
    /// enters tapped" (Amulet of Vigor); `duringYourTurn=true` for
    /// temporal-scoped variants like Foe-liage ("Whenever a land
    /// enters during your turn, …").
    record Enters(Subject subject, boolean tapped, boolean duringYourTurn) implements TriggerEvent {
        public Enters(Subject subject) {
            this(subject, false, false);
        }

        public Enters(Subject subject, boolean tapped) {
            this(subject, tapped, false);
        }

        public Enters withTapped() {
            return new Enters(subject, true, duringYourTurn);
        }

        public Enters asDuringYourTurn() {
            return new Enters(subject, tapped, true);
        }
    }

    /// "\[subject\] die\[s\] \[during combat\]?" (rule 603.6c-d — put into
    /// graveyard from battlefield). `duringCombat=true` narrows the
    /// trigger to deaths inside the combat phase (Mongrel Pack: "When
    /// this creature dies during combat, …").
    record Dies(Subject subject, boolean duringCombat) implements TriggerEvent {
        public Dies(Subject subject) {
            this(subject, false);
        }

        public Dies asDuringCombat() {
            return new Dies(subject, true);
        }
    }

    /// "\[subject\] attack\[s\] \[target\]? \[alone\]?" (rule 603.6e). `target`
    /// is the attacked player or planeswalker when oracle names one (e.g.,
    /// "a creature attacks you"); null for the common agent-only form.
    record Attacks(Subject subject, @Nullable Subject target, boolean alone) implements TriggerEvent {
        public Attacks(Subject subject) {
            this(subject, null, false);
        }

        public Attacks withTarget(Subject target) {
            return new Attacks(subject, target, alone);
        }

        public Attacks attackingAlone() {
            return new Attacks(subject, target, true);
        }
    }

    /// "\[subject\] attack\[s\] and isn't blocked" — compound combat
    /// trigger requiring both attack declaration and the unblocked
    /// state after blockers are declared (Abyssal Nightstalker:
    /// "Whenever this creature attacks and isn't blocked, …"). Rule
    /// 509.1h establishes the unblocked state; the trigger fires at
    /// that point, not at attack declaration.
    record AttacksUnblocked(Subject subject) implements TriggerEvent {}

    /// "\[subject\] block\[s\] \[target\]?" (rule 603.6e). `target` is
    /// the attacker when named (e.g., "this creature blocks a creature");
    /// null for the agent-only form.
    record Blocks(Subject subject, @Nullable Subject target) implements TriggerEvent {
        public Blocks(Subject subject) {
            this(subject, null);
        }

        public Blocks withTarget(Subject target) {
            return new Blocks(subject, target);
        }
    }

    /// "\[subject\] become\[s\] blocked \[by X\]?" (rule 509).
    record BecomesBlocked(Subject subject, @Nullable Subject by) implements TriggerEvent {
        public BecomesBlocked(Subject subject) {
            this(subject, null);
        }

        public BecomesBlocked withBy(Subject by) {
            return new BecomesBlocked(subject, by);
        }
    }

    /// "\[subject\] become\[s\] \[tapped|untapped\]" — status-change trigger.
    record BecomesStatus(Subject subject, Status status) implements TriggerEvent {
        public enum Status {
            TAPPED,
            UNTAPPED
        }
    }

    static BecomesStatus becomesTapped(Subject subject) {
        return new BecomesStatus(subject, BecomesStatus.Status.TAPPED);
    }

    static BecomesStatus becomesUntapped(Subject subject) {
        return new BecomesStatus(subject, BecomesStatus.Status.UNTAPPED);
    }

    /// "\[subject\] becomes the target of \[subject\]" (rule 603.6m).
    /// `what` is a [Subject] (not a [Selector]) so disjunctive
    /// targeters like "a spell or ability" can flow through as
    /// [Subject.OneOf].
    record BecomesTargetOf(Subject subject, Subject what) implements TriggerEvent {}

    /// "\[source\] deals \[amount\]? \[combat\]? damage [to \[target\]]?" (rule
    /// 603.6h). `target` is null for the agent-only form ("this creature
    /// deals damage" — Chalice of Life, Sliver damage triggers). `amount`
    /// is null when oracle text omits a quantifier; a non-null amount
    /// ([Amount.AtLeast], [Amount.Exact]) gates the trigger on a minimum
    /// damage threshold (Dragonborn Champion: "deals 5 or more damage").
    record DealsDamage(
            Subject source,
            @Nullable Amount amount,
            boolean combat,
            @Nullable Subject target) implements TriggerEvent {
        public DealsDamage(Subject source, boolean combat) {
            this(source, null, combat, null);
        }

        public DealsDamage withAmount(Amount amount) {
            return new DealsDamage(source, amount, combat, target);
        }

        public DealsDamage withTarget(Subject target) {
            return new DealsDamage(source, amount, combat, target);
        }
    }

    /// "\[subject\] is cast" (rule 603.6i — cast trigger on the stack).
    record IsCast(Subject subject) implements TriggerEvent {}

    /// "\[subject\] is countered".
    record IsCountered(Subject subject) implements TriggerEvent {}

    /// "\[subject\] is dealt \[combat|noncombat\]? \[\<amount\>\]? damage
    /// \[by a single source\]?" — received-damage trigger. `kind` narrows
    /// the damage source: [DamageKind#COMBAT], [DamageKind#NONCOMBAT], or
    /// [DamageKind#ANY] for unspecified. `amount` narrows the trigger to a
    /// damage threshold (null = any amount); `bySingleSource` requires the
    /// damage to come from one source.
    record IsDealtDamage(
            Subject subject, DamageKind kind, @Nullable AmountMatcher amount, boolean bySingleSource)
            implements TriggerEvent {

        /// Which category of damage triggers this event (rule 120.3a–c).
        public enum DamageKind {
            COMBAT,
            NONCOMBAT,
            ANY
        }

        public IsDealtDamage(Subject subject) {
            this(subject, DamageKind.ANY, null, false);
        }

        public IsDealtDamage(Subject subject, DamageKind kind) {
            this(subject, kind, null, false);
        }

        public IsDealtDamage withAmount(AmountMatcher amount) {
            return new IsDealtDamage(subject, kind, amount, bySingleSource);
        }

        public IsDealtDamage asBySingleSource() {
            return new IsDealtDamage(subject, kind, amount, true);
        }
    }

    /// "\[subject\] is put into \[destination\] \[from \[source\]\]?" —
    /// zone-change trigger for cards/permanents (rule 603.6c, 603.10).
    /// `destination` captures the optional explicit target zone
    /// ("put into a graveyard from anywhere" — Planar Void); when the
    /// destination is implicit (legacy "is put into from X" form) it
    /// is `null`. `from` is the optional source-zone restriction.
    record PutInto(Subject subject, @Nullable Zone destination, Zone.@Nullable Source from) implements TriggerEvent {
        public PutInto(Subject subject, Zone.Source from) {
            this(subject, null, from);
        }

        public PutInto(Subject subject, Zone destination) {
            this(subject, destination, null);
        }
    }

    /// "\[subject\] leave\[s\] \[zone\]" — zone-leaving trigger.
    record Leaves(Subject subject, Zone zone) implements TriggerEvent {}

    /// "\[subject\] is returned to \[zone\]" — bounce-style zone change
    /// (Warped Devotion: "Whenever a permanent is returned to a
    /// player's hand, …"). Rule 701.10 (Return) is a special-case zone
    /// change; kept as its own trigger variant to preserve the oracle
    /// distinction from [PutInto].
    record IsReturnedTo(Subject subject, Zone destination) implements TriggerEvent {}

    /// "\[player\] roll\[s\] \<quantity\> dice|die" — dice-rolling trigger
    /// (rule 706.2). The [Quantity] sealed type distinguishes a count
    /// threshold ("one or more dice", "two dice") from a positional
    /// per-turn reference ("your third die each turn").
    /// - Brazen Dwarf: "Whenever you roll one or more dice, …" →
    ///   [Quantity.Count].
    /// - Resolute Veggiesaur: "Whenever you roll your third die each
    ///   turn, …" → [Quantity.Nth].
    /// "\<player\> clash[es] and win[s]" — rule 701.23 clash trigger
    /// (Sylvan Echoes: "Whenever you clash and win, you may draw a
    /// card."). The win-side bookkeeping is implicit in the variant.
    record PlayerClashAndWins(Subject player) implements TriggerEvent {}

    record PlayerRollsDice(Subject player, Quantity quantity) implements TriggerEvent {
        public sealed interface Quantity {
            /// "[amount] dice" — a count threshold (one-or-more, an
            /// exact integer, etc.).
            record Count(Amount amount) implements Quantity {}

            /// "\[your\] \[ordinal\] die each turn" — positional per-turn
            /// reference; the trigger fires only on the Nth die rolled
            /// that turn.
            record Nth(int ordinal) implements Quantity {}
        }
    }

    /// "\[player\] cast\[s\] \[spell\] \[from zone\]? \[this turn\]? \[ordinal\]?."
    /// - `from`: zone-of-casting restriction — a spell can be cast
    ///   from hand, graveyard (flashback), exile (suspend, foretell), or
    ///   library (cascade); the trigger only fires when the cast origin
    ///   matches (Secrets of the Dead: "from your graveyard").
    /// - `thisTurn`: temporal scope — the trigger is only live
    ///   during the current turn (Glimpse of Nature: "this turn").
    /// - `nthEachTurn`: the "your first/second/… spell each turn"
    ///   qualifier (Rodeo Pyromancers).
    record PlayerCasts(
            Subject player,
            Selector spell,
            Zone.@Nullable Source from,
            @Nullable TurnScope turnScope,
            @Nullable Integer nthEachTurn)
            implements TriggerEvent {
        public PlayerCasts(Subject player, Selector spell) {
            this(player, spell, null, null, null);
        }

        public PlayerCasts withFrom(Zone.Source from) {
            return new PlayerCasts(player, spell, from, turnScope, nthEachTurn);
        }

        public PlayerCasts scopedToThisTurn() {
            return new PlayerCasts(player, spell, from, TurnScope.THIS_TURN, nthEachTurn);
        }

        public PlayerCasts withTurnScope(TurnScope scope) {
            return new PlayerCasts(player, spell, from, scope, nthEachTurn);
        }

        public PlayerCasts nth(int n) {
            return new PlayerCasts(player, spell, from, turnScope, n);
        }

        /// Closed-set turn-scope qualifiers on a cast trigger.
        public enum TurnScope {
            /// "this turn" — Glimpse of Nature.
            THIS_TURN,
            /// "during an opponent's turn" — Faerie Tauntings.
            DURING_OPPONENT_TURN,
            /// "during your turn" — Wavebreak Hippocamp.
            DURING_YOUR_TURN,
            /// "during each opponent's turn" — narrower scope used by
            /// the n-th-spell variant.
            DURING_EACH_OPPONENT_TURN
        }
    }

    /// "When \[player\] cast\[s\] this spell/~" — the self-cast trigger
    /// common on spells that do extra work on resolution via a cast
    /// trigger (Desolation Twin: "When you cast this spell, create a
    /// 10/10 colorless Eldrazi creature token."). Distinct from
    /// [PlayerCasts] because the spell target is a
    /// self-reference rather than a selector.
    record PlayerCastsSelf(Subject player) implements TriggerEvent {}

    /// "Whenever \[player\] copies \[spell\]" — copy-detection trigger
    /// (Archmage Emeritus: "Whenever you cast or copy an instant or
    /// sorcery spell, draw a card." emits a [PlayerCasts] + this
    /// pair). Distinct from [PlayerCasts] because copy events are
    /// not cast events (rule 707).
    record PlayerCopies(Subject player, Selector spell) implements TriggerEvent {}

    /// "Whenever \[player\] proliferate\[s\]" — proliferate trigger (rule
    /// 701.25, Scheming Aspirant: "Whenever you proliferate, each
    /// opponent loses 2 life and you gain 2 life.").
    record PlayerProliferates(Subject player) implements TriggerEvent {}

    /// "Whenever \[player\] activate\[s\] a\[n\] \[kind\]? ability \[of
    /// \[source\]\]?" — ability-activation trigger (Frenzied Raider:
    /// "Whenever you activate a boast ability …"; Ceaseless Searblades:
    /// "Whenever you activate an ability of an Elemental, …"). `kind`
    /// names the ability tag ("boast", "cycling", …); captured as free
    /// text since the tag universe is open-ended. `kind` is null when
    /// no tag is named. `source` is the object whose ability is being
    /// activated, null when unconstrained.
    record PlayerActivatesAbility(
            Subject player, @Nullable String kind, @Nullable Subject source) implements TriggerEvent {
        public PlayerActivatesAbility(Subject player, String kind) {
            this(player, kind, null);
        }

        public PlayerActivatesAbility withSource(Subject source) {
            return new PlayerActivatesAbility(player, kind, source);
        }
    }

    /// "Whenever \[player\] scr\[y\|ies\]" — scry trigger (rule 701.18).
    record PlayerScries(Subject player) implements TriggerEvent {}

    /// "Whenever \[player\] surveil\[s\]" — surveil trigger (rule 701.41).
    record PlayerSurveils(Subject player) implements TriggerEvent {}

    /// "Whenever \[player\] manifest\[s\] dread" — manifest dread trigger
    /// (Paranormal Analyst; rule 701.65). Distinct from
    /// [IsTurnedFaceUp] (the post-manifest flip).
    record PlayerManifestsDread(Subject player) implements TriggerEvent {}

    /// "Whenever \[player\] investigate\[s\] \[for the first time each
    /// turn\]?" — investigate trigger (Erdwal Illuminator; rule 701.27
    /// Investigate). The optional first-time-each-turn frequency
    /// limiter narrows the trigger window.
    record PlayerInvestigates(Subject player, boolean firstTimeEachTurn) implements TriggerEvent {
        public PlayerInvestigates(Subject player) {
            this(player, false);
        }

        public PlayerInvestigates asFirstTimeEachTurn() {
            return new PlayerInvestigates(player, true);
        }
    }

    /// "Whenever \[player\] shuffle\[s\] \[their|its\] library" — library-
    /// shuffle trigger (Cosi's Trickster; rule 701.20). Fires on the
    /// shuffling action itself, not on the cause.
    record PlayerShufflesLibrary(Subject player) implements TriggerEvent {}

    /// "\[subject\] is turned face up" — morph/manifest flip trigger.
    record IsTurnedFaceUp(Subject subject) implements TriggerEvent {}

    /// "\[subject\] mutates" — mutate stack event (Ikoria).
    record Mutates(Subject subject) implements TriggerEvent {}

    /// "\[player\] give\[s\] a gift" — Aetherdrift Gifts mechanic.
    record PlayerGivesGift(Subject player) implements TriggerEvent {}

    /// "\[player\] attack\[s\] with \[amount\] \[creatures\]?." — attack
    /// formation trigger (e.g., Raiding Horde: "Whenever you attack with
    /// two or more creatures, …").
    /// "\[player\] attack\[s\] with \[amount\] creature(s) \[with \<keyword\>\]?"
    /// — multi-attacker count trigger (Raiding Horde; Tide Skimmer:
    /// "Whenever you attack with two or more creatures with flying,
    /// draw a card."). The optional keyword restricts which
    /// attackers count; a [Selector.WithClause.HasAbility] that's
    /// attached to the implicit attackers.
    record AttacksWith(Subject player, Amount amount, Selector.@Nullable WithClause with) implements TriggerEvent {
        public AttacksWith(Subject player, Amount amount) {
            this(player, amount, null);
        }

        public AttacksWith withWith(Selector.WithClause with) {
            return new AttacksWith(player, amount, with);
        }
    }

    /// "\[player\] control\[s\] no \[selector\]" — existential state check used
    /// as a trigger condition (Barbarian Outcast: "When you control no
    /// Swamps, sacrifice this creature."). Not strictly an event; fires
    /// whenever the state first becomes true (rule 603.6d / 603.10).
    record ControlsNone(Subject player, Selector what) implements TriggerEvent {}

    /// "\[player\] control\[s\] \[selector\]" — state-condition trigger
    /// on the positive side: fires while the player controls at
    /// least one object matching the selector (Endangered Armodon:
    /// "When you control a creature with toughness 2 or less,
    /// sacrifice this creature."). Distinct from [ControlsNone].
    record Controls(Subject player, Selector what) implements TriggerEvent {}

    /// "\[player\] play\[s\] \[selector\]" — the generic land-play trigger with
    /// an explicit selector, distinct from the common "plays a land" form
    /// (e.g., "When you play another land").
    record PlayerPlays(Subject player, Selector what) implements TriggerEvent {}

    /// "\[player\] cycle\[s\] \[card\]".
    record PlayerCycles(Subject player, Selector card) implements TriggerEvent {}

    /// "\[player\] discard\[s\] \[card\]".
    record PlayerDiscards(Subject player, Selector card) implements TriggerEvent {}

    /// "\[player\] kick\[s\] \[spell\]." — Saproling Infestation. Fires when
    /// a player pays a kicker cost while casting a spell (rule 702.32).
    record PlayerKicks(Subject player, Selector spell) implements TriggerEvent {}

    /// "\[player\] searches \[whose\] library" — library-search trigger
    /// (Archivist of Oghma: "Whenever an opponent searches their
    /// library, …"). Rule 701.19. `libraryOwner` names whose library
    /// is being searched (typically the searcher themself, but
    /// search effects can name a different player).
    record PlayerSearchesLibrary(Subject player, Subject.PlayerRef libraryOwner) implements TriggerEvent {}

    /// "\[player\] draw\[s\] \[amount\]".
    /// "\[player\] draw\[s\] \[amount\] \[card(s)\] \[each turn\]?" — if
    /// `nthEachTurn` is non-null, this fires only on that specific
    /// draw within each turn (Erudite Wizard: "Whenever you draw your
    /// second card each turn, …").
    record PlayerDraws(
            Subject player, Amount amount, @Nullable Integer nthEachTurn) implements TriggerEvent {
        public PlayerDraws(Subject player, Amount amount) {
            this(player, amount, null);
        }

        public PlayerDraws nth(int n) {
            return new PlayerDraws(player, amount, n);
        }
    }

    /// "\[player\] gain\[s\] life".
    record PlayerGainsLife(Subject player) implements TriggerEvent {}

    /// "\[player\] lose\[s\] life".
    record PlayerLosesLife(Subject player) implements TriggerEvent {}

    /// "\[player\] play\[s\] a land".
    record PlayerPlaysLand(Subject player) implements TriggerEvent {}

    /// "\[player\] sacrifice\[s\] \[selector\]".
    record PlayerSacrifices(Subject player, Selector what) implements TriggerEvent {}

    /// "\[player\] create\[s\] \[selector\]" — token-creation trigger
    /// (Mirkwood Bats: "Whenever you create or sacrifice a token, …").
    record PlayerCreates(Subject player, Selector what) implements TriggerEvent {}

    /// "\[subject\] crew\[s\] \[selector\]" — Vehicle-crew trigger
    /// (Speedway Fanatic: "Whenever this creature crews a Vehicle, …";
    /// rule 702.122). Fires when the subject taps as part of paying a
    /// Crew cost.
    record Crews(Subject subject, Selector what) implements TriggerEvent {}

    /// "\[caster\] spend\[s\] this mana to cast \[what\]" — mana-spending
    /// trigger tied to the mana produced by the preceding Add-Mana
    /// effect (Scaled Nurturer: "Add {G}. When you spend this mana to
    /// cast a Dragon creature spell, you gain 2 life."). Fires on the
    /// next cast that consumes the produced mana.
    record SpendManaToCast(Subject caster, Subject what) implements TriggerEvent {}

    /// "\[subject\] regenerate\[s\] \[this way\]?" — regeneration trigger
    /// (Matopi Golem: "When it regenerates this way, put a -1/-1 counter
    /// on it."). `thisWay` restricts the trigger to regenerations caused
    /// by the same ability's preceding Regenerate effect, versus any
    /// regeneration of the subject.
    record Regenerates(Subject subject, boolean thisWay) implements TriggerEvent {
        public Regenerates(Subject subject) {
            this(subject, false);
        }

        public Regenerates asThisWay() {
            return new Regenerates(subject, true);
        }
    }

    /// "\[subject\] tap\[s\] \[land\] for mana".
    record TapsForMana(Subject subject, Subject what) implements TriggerEvent {}

    /// "\[subject\] is tapped for mana" — passive-voice form of the
    /// mana-tap trigger (Vernal Bloom: "Whenever a Forest is tapped
    /// for mana …"). Distinct from [TapsForMana] because the
    /// tapping player isn't named in the oracle text.
    record IsTappedForMana(Subject subject) implements TriggerEvent {}

    /// "\[player\] activate\[s\] \[ability\]".
    record PlayerActivates(Subject player, String description) implements TriggerEvent {}

    /// "at the beginning of \[owner\]'s/each \[step|phase\] …" — the
    /// phase/step-scoped triggers that share an owner + each-player
    /// prefix. Enables polymorphic dispatch on [#withOwner].
    sealed interface OwnerScoped extends TriggerEvent permits AtStep, AtPhase {
        public OwnerScoped withOwner(@Nullable Subject owner, boolean each);
    }

    /// "at the beginning of \[owner\]'s/each \[step\] step" — rule 603.6g
    /// beginning-of-step trigger.
    record AtStep(@Nullable Subject owner, boolean each, Step step) implements OwnerScoped {
        public AtStep(Step step) {
            this(null, false, step);
        }

        @Override
        public AtStep withOwner(@Nullable Subject owner, boolean each) {
            return new AtStep(owner, each, step);
        }
    }

    /// Ordinal qualifier on a twin-main-phase trigger — Hulking Raptor
    /// ("first main phase"), Vernal Equinox ("precombat main phase").
    /// The rules engine consults this when a turn exposes two main
    /// phases so the trigger fires on the right one.
    enum PhaseQualifier {
        FIRST,
        SECOND,
        PRECOMBAT,
        POSTCOMBAT
    }

    /// "at the beginning of \[owner\]'s/each \[qualifier\]? \[phase\] phase".
    /// `qualifier` is `null` for unqualified phases.
    record AtPhase(
            @Nullable Subject owner,
            boolean each,
            Phase phase,
            @Nullable PhaseQualifier qualifier) implements OwnerScoped {
        public AtPhase(Phase phase) {
            this(null, false, phase, null);
        }

        public AtPhase(Phase phase, @Nullable PhaseQualifier qualifier) {
            this(null, false, phase, qualifier);
        }

        @Override
        public AtPhase withOwner(@Nullable Subject owner, boolean each) {
            return new AtPhase(owner, each, phase, qualifier);
        }
    }

    /// "at end of combat".
    enum EndOfCombat implements TriggerEvent {
        END_OF_COMBAT
    }

    /// "at end of turn".
    enum EndOfTurn implements TriggerEvent {
        END_OF_TURN
    }
}
