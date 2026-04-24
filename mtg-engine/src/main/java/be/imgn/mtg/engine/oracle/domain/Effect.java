package be.imgn.mtg.engine.oracle.domain;

import java.util.List;

import org.jspecify.annotations.Nullable;

/// An effect produced by a spell or ability.
public sealed interface Effect {

    // Removal

    /// "Destroy \[target\] \[at end of combat | at the beginning of …\]?" —
    /// `at` defers resolution to a later timing (e.g., Silent
    /// Assassin: "Destroy target blocking creature at end of combat.").
    /// Null `at` is the common immediate form.
    record Destroy(Subject target, @Nullable Duration at, boolean atRandom) implements Effect {
        public Destroy(Subject target) {
            this(target, null, false);
        }

        public Destroy withAt(Duration at) {
            return new Destroy(target, at, atRandom);
        }

        public Destroy withAtRandom() {
            return new Destroy(target, at, true);
        }
    }

    /// Exile objects (via a [Subject]) or whole zones (via a
    /// [variant). The optional `from][Exiled.Zones`] restricts
    /// which source zone is searched ("from any graveyard", "from exile").
    /// The optional `actor` is the player performing the exile — set
    /// when oracle text says "\[player\] exiles …" (e.g., Mudhole: "Target
    /// player exiles all land cards from their graveyard."); null for the
    /// common imperative form where the spell itself does the exiling.
    record Exile(
            Exiled exiled,
            Zone.@Nullable Source from,
            @Nullable Subject actor) implements Effect {
        public Exile(Exiled exiled) {
            this(exiled, null, null);
        }

        public Exile(Exiled exiled, Zone.@Nullable Source from) {
            this(exiled, from, null);
        }

        public Exile withActor(Subject actor) {
            return new Exile(exiled, from, actor);
        }
    }

    /// "\[who\] sacrifice\[s\] \[what\]." — `what` is a [Subject] so
    /// it can be either a selector ("a creature you control") or a
    /// self-reference ("this creature", e.g., Barbarian Outcast).
    /// "\[who\] sacrifice\[s\] \[what\] \[at <timing>\]?." — `at` defers
    /// resolution to a later timing (Mardu Blazebringer: "sacrifice it at
    /// end of combat."), mirroring [Destroy#at]. `scaleBy` carries a
    /// "for each …" count that multiplies the sacrifice count (Thoughts
    /// of Ruin: "Each player sacrifices a land of their choice for each
    /// card in your hand.").
    record Sacrifice(
            Subject who,
            Subject what,
            @Nullable Duration at,
            @Nullable Amount scaleBy) implements Effect {
        public Sacrifice(Subject who, Subject what) {
            this(who, what, null, null);
        }

        public Sacrifice(Subject who, Subject what, @Nullable Duration at) {
            this(who, what, at, null);
        }

        public Sacrifice withAt(Duration at) {
            return new Sacrifice(who, what, at, scaleBy);
        }

        public Sacrifice withScaleBy(Amount scaleBy) {
            return new Sacrifice(who, what, at, scaleBy);
        }
    }

    /// "Return \[subject\] \[from X\]? to Y." — move an object to a destination.
    /// The optional `from` specifies the source zone when it isn't
    /// the battlefield default (e.g., Auroral Procession: "Return target
    /// card from your graveyard to your hand.").
    record Bounce(Subject target, Zone.@Nullable Source from, Zone.Destination to) implements Effect {
        public Bounce(Subject target, Zone.Destination to) {
            this(target, null, to);
        }
    }

    // Damage & Life

    /// "\[source\] deal\[s\] \[amount\] damage to \[target\] \[chosen at random\]?."
    /// — direct damage (rule 119). `atRandom` is set when the oracle
    /// picks the target at random rather than letting the controller
    /// choose (Goblin Test Pilot: "This creature deals 2 damage to any
    /// target chosen at random.").
    record DealDamage(Subject source, Amount amount, Subject target, boolean atRandom) implements Effect {
        public DealDamage(Subject source, Amount amount, Subject target) {
            this(source, amount, target, false);
        }

        public DealDamage asRandom() {
            return new DealDamage(source, amount, target, true);
        }

        public DealDamage withAmount(Amount amount) {
            return new DealDamage(source, amount, target, atRandom);
        }
    }

    /// "\[source\] deal\[s\] \[amount\] damage divided as \[chooser\]? chooses
    /// among \[targets\]." — split damage (rule 609.4). `targets` names
    /// the group receiving the split (e.g., "one or two targets",
    /// "X targets"). The chooser defaults to the spell's controller
    /// when absent.
    record DealDividedDamage(Subject source, Amount totalAmount, Subject targets) implements Effect {}

    record GainLife(Subject player, Amount amount, @Nullable Amount xDefinition) implements Effect {
        public GainLife(Subject player, Amount amount) {
            this(player, amount, null);
        }

        public GainLife withXDefinition(Amount xDefinition) {
            return new GainLife(player, amount, xDefinition);
        }
    }

    record LoseLife(Subject player, Amount amount, @Nullable Amount xDefinition) implements Effect {
        public LoseLife(Subject player, Amount amount) {
            this(player, amount, null);
        }

        public LoseLife withXDefinition(Amount xDefinition) {
            return new LoseLife(player, amount, xDefinition);
        }
    }

    // Card Manipulation

    record Draw(Subject player, Amount amount, @Nullable Amount xDefinition) implements Effect {
        public Draw(Subject player, Amount amount) {
            this(player, amount, null);
        }

        public Draw withXDefinition(Amount xDefinition) {
            return new Draw(player, amount, xDefinition);
        }
    }

    /// Discard cards from the player's hand. The [Discarded] variant
    /// distinguishes between "discard N cards" and "discard your hand".
    record Discard(Subject player, Discarded discarded) implements Effect {}

    /// "\[player\] mill\[s\] \[amount\] cards \[, where X is \<def\>\]?." —
    /// put the top N cards of the player's library into their
    /// graveyard (rule 701.13). The optional `xDefinition` binds the X
    /// used in `amount` when the count is variable (Dreadwaters:
    /// "Target player mills X cards, where X is the number of lands
    /// you control.").
    record Mill(Subject player, Amount amount, @Nullable Amount xDefinition) implements Effect {
        public Mill(Subject player, Amount amount) {
            this(player, amount, null);
        }

        public Mill withXDefinition(Amount xDefinition) {
            return new Mill(player, amount, xDefinition);
        }
    }

    record Scry(Amount amount) implements Effect {}

    /// "Surveil N" (rule 701.41) — look at the top N cards, then put
    /// each into the graveyard or on top of the library in any order.
    /// Distinct from [Scry] because surveil changes zones of revealed
    /// cards rather than only reordering the library.
    record Surveil(Amount amount) implements Effect {}

    /// "\<action\> at \<timing\>." — delayed triggered ability created
    /// by the enclosing effect (rule 603.7). Schedules `action` to
    /// happen at the named timing rather than immediately (Blessed
    /// Wine: "Draw a card at the beginning of the next turn's
    /// upkeep.").
    record Delayed(Effect action, DelayedTiming when) implements Effect {}

    /// "When \<event\>, \<action\>." — delayed triggered ability bound
    /// to a [TriggerEvent] rather than a fixed timing (rule 603.7b).
    /// Unlike [Delayed], the delayed trigger fires on the next matching
    /// event (Matopi Golem: "Regenerate this creature. When it
    /// regenerates this way, put a -1/-1 counter on it.").
    record DelayedTrigger(TriggerEvent event, Effect action) implements Effect {}

    /// "Search \[whose\] library for \[what\]." — `who` names the library
    /// owner when oracle text specifies one (Extract: "Search target
    /// player's library …"). Null `who` means the controller's own
    /// library — the common "Search your library for …" form.
    record Search(@Nullable Subject who, Selector what) implements Effect {
        public Search(Selector what) {
            this(null, what);
        }
    }

    /// "\[player\] shuffles \[source\]? [into \[destination\]]?." — unified
    /// shuffle effect. The common short form "\[player\] shuffles \[their\]
    /// library" leaves both zones null (e.g., Soldier of Fortune). When
    /// oracle text names both a source and a destination, the cards in
    /// `source` are placed into `destination` which is then
    /// shuffled (e.g., Mnemonic Nexus: "Each player shuffles their
    /// graveyard into their library.").
    record Shuffle(
            Subject player,
            @Nullable Subject subject,
            @Nullable Zone source,
            @Nullable Zone destination) implements Effect {
        public Shuffle(Subject player, @Nullable Zone source, @Nullable Zone destination) {
            this(player, null, source, destination);
        }

        public Shuffle(Subject player) {
            this(player, null, null, null);
        }

        public Shuffle withSubject(Subject subject) {
            return new Shuffle(player, subject, source, destination);
        }
    }

    /// "\[actor\]? reveal\[s\] \[target\]." — reveal an object (or zone via a
    /// possessive "hand"). The optional `actor` is the player performing
    /// the reveal when oracle text names one (e.g., Trapfinder's Trick:
    /// "Target player reveals their hand…"); null for the common imperative
    /// form where the spell itself reveals.
    /// "\[player\] reveal\[s\] \[target\]." — `player` is the one doing
    /// the revealing (defaults to the controller when oracle text
    /// omits a subject), `target` is what gets revealed (a subject or,
    /// equivalently, the contents of a zone such as `CardManipulationEffectParsers.HAND`).
    record Reveal(Subject player, Subject target) implements Effect {}

    // Tap/Untap

    /// "Tap \[target\]." — e.g., Twiddle.
    record Tap(Subject target) implements Effect {}

    /// "Untap \[target\]." — e.g., Early Harvest, Twiddle. Also covers
    /// the player-initiated "\[player\] untaps \[target\]" shape.
    record Untap(Subject target) implements Effect {}

    // Counters

    record AddCounters(
            Amount count,
            CounterType type,
            Subject target,
            @Nullable Amount xDefinition) implements Effect {
        public AddCounters(Amount count, CounterType type, Subject target) {
            this(count, type, target, null);
        }

        public AddCounters withXDefinition(Amount xDefinition) {
            return new AddCounters(count, type, target, xDefinition);
        }
    }

    record RemoveCounters(Amount count, CounterType type, Subject target) implements Effect {}

    /// "Put \[N₁\] \[t₁\] counter or \[N₂\] \[t₂\] counter on \[target\]." —
    /// chooser picks one of two counter placements on a shared
    /// target (Dwarven Armorer: "Put a +0/+1 counter or a +1/+0
    /// counter on target creature."). Emits structured options so
    /// the engine presents a choice at resolution.
    record AddCounterChoice(List<AddCounters> options) implements Effect {}

    // Ability Modification

    record GainAbility(
            Subject target,
            List<Ability> abilities,
            @Nullable Duration duration) implements Effect {
        public GainAbility(Subject target, List<Ability> abilities) {
            this(target, abilities, null);
        }

        public GainAbility withDuration(Duration duration) {
            return new GainAbility(target, abilities, duration);
        }
    }

    /// "\[subject\] lose\[s\] \[keyword…|all abilities\] \[duration\]." The
    /// [LoseAbility.Lost] variant distinguishes between named keyword
    /// abilities (e.g., "lose flying") and the sweeping "lose all abilities"
    /// form (e.g., Yixlid Jailer). A trailing duration ("until end of turn")
    /// makes the loss temporary (e.g., Radjan Spirit).
    record LoseAbility(Subject target, Lost lost, @Nullable Duration duration) implements Effect {
        public LoseAbility(Subject target, Lost lost) {
            this(target, lost, null);
        }

        public LoseAbility withDuration(Duration duration) {
            return new LoseAbility(target, lost, duration);
        }

        public sealed interface Lost {
            record Specific(List<Ability> abilities) implements Lost {}

            enum All implements Lost {
                ALL
            }

            /// "all \"<quoted text>\" abilities" — Shelkin Brownie:
            /// "Target creature loses all \"bands with other\"
            /// abilities until end of turn." The quoted text names
            /// a named-keyword family (e.g., "bands with other",
            /// "forecast") that's parameterized and thus can't map
            /// to a single [Ability] constant.
            record Named(String quotedName) implements Lost {}

            /// "all \[family\] abilities" — sweeping removal of a whole
            /// parameterized keyword family (Hammerheim: "loses all
            /// landwalk abilities"). The [Family] enum names which
            /// family is swept.
            record AllInFamily(Family family) implements Lost {}

            /// "\[ability\] or \[ability\]" — chooser picks one of the
            /// listed abilities to lose (Urborg: "Target creature
            /// loses first strike or swampwalk until end of turn.").
            record ChooseOne(List<Ability> options) implements Lost {}

            /// Closed set of parameterized keyword families that a
            /// "loses all \[family\] abilities" clause can sweep.
            enum Family {
                /// Rule 702.14 landwalk — islandwalk, forestwalk,
                /// mountainwalk, swampwalk, plainswalk, and the
                /// qualified forms ("legendary landwalk", "nonbasic
                /// landwalk", etc.).
                LANDWALK
            }
        }
    }

    // P/T Modification

    /// "\[subject\] get\[s\] \[±X/±Y\] \[duration\]?" — P/T modifier. Two optional
    /// tails refine the base modifier:
    /// - `scaleBy`: "for each …" multiplies the base by the count
    ///   (Grim Strider: "-1/-1 for each card in your hand").
    /// - `xDefinition`: ", where X is …" binds the X in a variable
    ///   modifier (Death's Shadow: "-X/-X, where X is your life total").
    record ModifyPT(
            Subject target,
            PtModifier modifier,
            @Nullable Duration duration,
            @Nullable Amount scaleBy,
            @Nullable Amount xDefinition)
            implements Effect {
        public ModifyPT(Subject target, PtModifier modifier) {
            this(target, modifier, null, null, null);
        }

        public ModifyPT(Subject target, PtModifier modifier, @Nullable Duration duration) {
            this(target, modifier, duration, null, null);
        }

        public ModifyPT withDuration(Duration duration) {
            return new ModifyPT(target, modifier, duration, scaleBy, xDefinition);
        }

        public ModifyPT withScaleBy(Amount scaleBy) {
            return new ModifyPT(target, modifier, duration, scaleBy, xDefinition);
        }

        public ModifyPT withXDefinition(Amount xDefinition) {
            return new ModifyPT(target, modifier, duration, scaleBy, xDefinition);
        }
    }

    // Control

    record GainControl(
            Subject player, Subject target, @Nullable Duration duration) implements Effect {
        public GainControl(Subject player, Subject target) {
            this(player, target, null);
        }

        public GainControl withDuration(Duration duration) {
            return new GainControl(player, target, duration);
        }
    }

    /// "Exchange control of \[targets\]." — swap controllers between the
    /// selected permanents (e.g., Switcheroo: two target creatures).
    /// "Exchange control of \[targets\]." — swap controllers between the
    /// specified permanents. `targets` is a [Subject] so self-
    /// references ("this artifact") and multi-subject conjunctions ("this
    /// artifact and target nonland permanent" — Avarice Totem) both round
    /// trip alongside plain selectors.
    record ExchangeControl(Subject targets) implements Effect {}

    // Tokens

    /// "\[creator\]? [Cc]reate\[s\] \[N\] \[tapped\]? \[token\]." — `creator`
    /// names the player putting the tokens onto the battlefield when
    /// oracle text explicitly says so (Seed the Land: "Whenever a
    /// land enters, its controller creates a 1/1 green Snake
    /// creature token."). `null` for the common imperative form
    /// where the spell's controller creates (Shadow Summoning:
    /// "Create two tapped 1/1 white Spirit creature tokens with
    /// flying."). `tapped` is true when oracle text says the tokens
    /// enter the battlefield tapped.
    record CreateToken(
            @Nullable Subject creator,
            Amount count,
            TokenDescription token,
            boolean tapped,
            @Nullable Amount xDefinition)
            implements Effect {
        public CreateToken(@Nullable Subject creator, Amount count, TokenDescription token, boolean tapped) {
            this(creator, count, token, tapped, null);
        }

        public CreateToken(Amount count, TokenDescription token) {
            this(null, count, token, false, null);
        }

        public CreateToken(Amount count, TokenDescription token, boolean tapped) {
            this(null, count, token, tapped, null);
        }

        public CreateToken withCreator(Subject creator) {
            return new CreateToken(creator, count, token, tapped, xDefinition);
        }

        public CreateToken withXDefinition(Amount xDefinition) {
            return new CreateToken(creator, count, token, tapped, xDefinition);
        }
    }

    // Counterspell

    /// "Counter \[target\] \[if <condition>\]." — counterspell effect, optionally
    /// gated on a condition about the target spell (e.g., Ertai's Trickery:
    /// "Counter target spell if it was kicked.").
    record CounterSpell(
            Subject target,
            @Nullable Condition condition,
            @Nullable Amount xDefinition) implements Effect {
        public CounterSpell(Subject target) {
            this(target, null, null);
        }

        public CounterSpell withCondition(Condition condition) {
            return new CounterSpell(target, condition, xDefinition);
        }

        public CounterSpell withXDefinition(Amount xDefinition) {
            return new CounterSpell(target, condition, xDefinition);
        }
    }

    // Combat

    record Fight(Subject a, Subject b) implements Effect {}

    // Mana

    /// Add one or more mana options to the player's mana pool. Each option is
    /// a [ManaOption] — either a fixed set of symbols or a repeated
    /// (variable-count) pattern. When more than one option is present (e.g.,
    /// `Add {B} or {R}`, or `Add X mana of any one color`), the player
    /// chooses one.
    /// Add one or more mana options to a player's mana pool. The optional
    /// `player` is the actor when oracle text names one (Tangleroot:
    /// "that player adds {G}"); null for the common imperative "Add …"
    /// form where the controller is implicit.
    record AddMana(@Nullable Subject player, List<ManaOption> options) implements Effect {
        public AddMana(List<ManaOption> options) {
            this(null, options);
        }

        public AddMana withPlayer(Subject player) {
            return new AddMana(player, options);
        }
    }

    // Zone Movement

    /// "Put \[what\] \[from X\]? \[to Y\]." — move a subject to a destination,
    /// optionally naming the source zone (e.g., False Mourning: "Put
    /// target card from your graveyard on top of your library.").
    /// "Put \[what\] \[from X\]? \[to Y\] \[in any order\]?." — move a subject
    /// to a destination, optionally naming the source zone (False Mourning:
    /// "Put target card from your graveyard on top of your library.").
    /// `anyOrder=true` signals that when the moved group is placed onto an
    /// ordered zone (typically library), the player orders the cards
    /// (Brainsurge: "put two cards from your hand on top of your library
    /// in any order.").
    record ZoneMove(Subject what, Zone.@Nullable Source from, Zone.Destination to, boolean anyOrder) implements Effect {
        public ZoneMove(Subject what, Zone.@Nullable Source from, Zone.Destination to) {
            this(what, from, to, false);
        }

        public ZoneMove(Subject what, Zone.Destination to) {
            this(what, null, to, false);
        }

        public ZoneMove withFrom(Zone.Source from) {
            return new ZoneMove(what, from, to, anyOrder);
        }

        public ZoneMove inAnyOrder() {
            return new ZoneMove(what, from, to, true);
        }
    }

    // Transform/Copy

    record Transform(Subject target) implements Effect {}

    record Copy(Subject target) implements Effect {}

    // Replacement & Prevention

    record Replace(Subject what, String event, Effect replacement, boolean onlyNextTime) implements Effect {
        public Replace(Subject what, String event, Effect replacement) {
            this(what, event, replacement, false);
        }

        public Replace asOnlyNextTime() {
            return new Replace(what, event, replacement, true);
        }
    }

    /// "If <condition>, <override> instead." — conditional override of the
    /// previously-stated effect. Covers the short "Add {U}. If you played a
    /// land this turn, add {B} instead." idiom (River of Tears) where the
    /// grammar doesn't name an explicit `would` event.
    record ConditionalOverride(Condition condition, Effect override) implements Effect {}

    /// "For each \[scope\], \[body\]." — iterate the body over each object
    /// matching `scope`. Within the body, demonstrative references
    /// like "that land" refer to the current iteration (Cleansing: "For
    /// each land, destroy that land unless any player pays 1 life.").
    record ForEach(Selector scope, Effect body) implements Effect {}

    /// "For each \[kind\] among \[scope\], \[body\]." — per-distinct-property
    /// loop over a set of objects (Bloom Tender: "For each color
    /// among permanents you control, add one mana of that color.";
    /// would-be Domain-style cards). Distinct from [ForEach] because
    /// the iteration is over values of a property, not over objects.
    record ForEachAmong(AmongKind kind, Subject scope, Effect body) implements Effect {
        public enum AmongKind {
            COLOR,
            BASIC_LAND_TYPE,
            CREATURE_TYPE
        }
    }

    /// "For each \[player-ref\], \[body\]." — iterate the body over each
    /// referenced player (Blatant Thievery: "For each opponent, gain
    /// control of target permanent that player controls."). Distinct
    /// from [ForEach] since [Selector] models objects, not players.
    record ForEachPlayer(Subject.PlayerRef player, Effect body) implements Effect {}

    /// "Switch \[subject\]'s power and toughness \[duration\]?" — swap the
    /// creature's power and toughness values (About Face). Duration is
    /// null for the permanent form and set for the common temporary
    /// "until end of turn" variant.
    record SwitchPT(Subject target, @Nullable Duration duration) implements Effect {
        public SwitchPT(Subject target) {
            this(target, null);
        }

        public SwitchPT withDuration(Duration duration) {
            return new SwitchPT(target, duration);
        }
    }

    /// "Attach \[what\] to \[to\]." — move an Equipment/Aura to a new host
    /// (Aura Finesse: "Attach target Aura you control to target creature.").
    record Attach(Subject what, Subject to) implements Effect {}

    /// "Distribute \[N\] \[type\] counters among \[subject\]." — distribute a
    /// pool of counters across multiple targets chosen by the controller
    /// (Elven Rite: "Distribute two +1/+1 counters among one or two target
    /// creatures."). The distribution choice itself is deferred to
    /// resolution.
    record DistributeCounters(Amount count, CounterType type, Subject among) implements Effect {}

    /// "\[subject\] crews \[selector\] as though its power were \[N\] greater."
    /// — crew-boost (Hotshot Mechanic). The delta modifies the effective
    /// power used toward a Crew cost requirement.
    record CrewsWithBoostedPower(Subject subject, Selector target, int powerDelta) implements Effect {}

    /// "Flip \[N\] coin\[s\] \[and ignore M\]?" — coin-flip effect (Krark's
    /// Thumb replacement, Crush of Wurms, etc.). `ignore` is the
    /// number of flips discarded from the pool (0 for plain flips).
    record FlipCoins(Amount count, int ignore) implements Effect {
        public FlipCoins(Amount count) {
            this(count, 0);
        }
    }

    /// "Roll the planar die." — Planechase planar-die roll effect
    /// (Fractured Powerstone).
    enum RollPlanarDie implements Effect {
        ROLL_PLANAR_DIE
    }

    /// "Move \[count\] \[type\]? counters from \[source\] onto \[dest\]." — Fate
    /// Transfer, Power Conduit. Relocates counters of the given type
    /// between two permanents.
    record MoveCounters(Amount count, @Nullable CounterType type, Subject from, Subject onto) implements Effect {}

    /// "\[player\] may activate \[kind\] abilities any time \[player\] could
    /// cast \[an instant|a sorcery\]." — lift the timing restriction on a
    /// family of activated abilities (Leonin Shikari, Teferi, Temporal
    /// Archmage Emblem). `kind` names the ability class;
    /// `speed` preserves the distinction between instant-speed (always OK)
    /// and sorcery-speed (your main phase on an empty stack) timing
    /// grants — the two produce very different game permissions.
    record MayActivateAnyTime(Subject player, Kind kind, Speed speed) implements Effect {
        public enum Speed {
            INSTANT,
            SORCERY
        }

        /// Families of activated abilities oracle text scopes this timing
        /// grant to. The set is closed by the printed cards: "equip"
        /// (Leonin Shikari) and "loyalty" (Teferi, Temporal Archmage Emblem).
        public enum Kind {
            EQUIP,
            LOYALTY
        }
    }

    /// "Double the amount of each type of unspent mana [you|target
    /// player] ha\[s|ve\]." — Doubling Cube / Mana Reflection. Doubles every
    /// type of mana currently in the player's pool.
    record DoubleMana(Subject player) implements Effect {}

    /// "\[player\] pays \[cost\]." — optional payment inside a `you may pay …. If you do, …` idiom (Inheritance).
    // Stored as a general
    /// payment action; the "if you do" continuation attaches to the
    /// enclosing [Optional].
    record Pay(Subject player, Cost cost) implements Effect {}

    /// "\[player\] loses all unspent mana." — empties the player's
    /// mana pool (Mana Short: "… and that player loses all unspent
    /// mana.").
    record LoseUnspentMana(Subject player) implements Effect {}

    /// Damage prevention (rule 615). Two shapes share this interface:
    ///  - [AllDamage] — "Prevent \[all\]? \[combat|noncombat\]? damage \[dealt
    ///    to/by …\]? \[duration\]?" (Fog, Ethereal Haze, Cho-Manno's Blessing,
    ///    Bubble Matrix, Mark of Asylum, Harmless Assault, Statecraft, Personal
    ///    Sanctuary, Indentured Oaf);
    ///  - [ThatDamage] — "Prevent \[N of\]? that damage" (Callous Giant, Urza's
    ///    Armor), a back-reference to damage mentioned in the preceding clause.
    ///
    /// Distinct from [PreventNextDamage], which is a *one-shot shield* of a
    /// specific numeric size (Shield of the Ages, Decorated Griffin).
    sealed interface Prevent extends Effect {

        /// Combat / noncombat / any — the damage-kind qualifier that oracle
        /// text attaches to "prevent all … damage".
        enum Kind {
            ANY,
            COMBAT,
            NONCOMBAT
        }

        /// "Prevent all \[combat|noncombat\]? damage \[dealt to/by …\]? \[duration\]?"
        /// — the universal form. Every qualifier oracle text attaches to a
        /// prevention has its own structured slot:
        ///  - [kind] — combat-only, noncombat-only, or any damage;
        ///  - [by] / [to] — damage source and target subjects;
        ///  - [bothDirections] — Statecraft's "to and dealt by \[subject\]"
        ///    form where a single subject is both source and target;
        ///  - [duration] — "this turn", "during your turn" (encoded as
        ///    [Duration.Fixed#DURING_YOUR_TURN]), or null for an always-on
        ///    prevention.
        record AllDamage(
                Kind kind,
                @Nullable Subject by,
                @Nullable Subject to,
                boolean bothDirections,
                @Nullable Duration duration)
                implements Prevent {

            public AllDamage() {
                this(Kind.ANY, null, null, false, null);
            }

            public AllDamage(Kind kind) {
                this(kind, null, null, false, null);
            }

            public AllDamage withKind(Kind kind) {
                return new AllDamage(kind, by, to, bothDirections, duration);
            }

            public AllDamage withBy(Subject by) {
                return new AllDamage(kind, by, to, bothDirections, duration);
            }

            public AllDamage withTo(Subject to) {
                return new AllDamage(kind, by, to, bothDirections, duration);
            }

            /// Statecraft's "to and dealt by \[subject\]" — a single subject that is
            /// both source and target of the prevented damage.
            public AllDamage withBothDirections(Subject subject) {
                return new AllDamage(kind, subject, subject, true, duration);
            }

            public AllDamage withDuration(Duration duration) {
                return new AllDamage(kind, by, to, bothDirections, duration);
            }
        }

        /// "Prevent \[N of\]? that damage." — back-reference to damage named
        /// in the preceding clause (Callous Giant: "If a source would deal 3
        /// or less damage to this creature, prevent that damage."; Urza's
        /// Armor: "If a source would deal damage to you, prevent 1 of that
        /// damage."). `amount` is `null` for the full-subset form.
        record ThatDamage(@Nullable Amount amount) implements Prevent {
            public ThatDamage() {
                this(null);
            }
        }
    }

    /// "\[that permanent\] produces twice as much of that mana." — mana-doubling
    /// replacement body used inside [Replace] (Mana Reflection). The surrounding
    /// [Replace] event identifies the mana source; this variant carries no
    /// fields, only the semantic marker.
    record DoubleManaProduced() implements Effect {}

    /// "You may \[alternative\] rather than pay this spell's mana cost." —
    /// inline alternative casting cost (rule 117.9; Delraich, Crash,
    /// Pulverize, Flare of Denial). The `alternative` carries the
    /// replacement action (sacrifice N of a type, pay colored mana,
    /// exile cards …); the engine treats this record as a cost
    /// substitution at cast time, not a resolution-time effect.
    record AlternativeCastingCost(Effect alternative) implements Effect {}

    /// "Prevent the next \[amount\] \[combat\]? damage that would be dealt
    /// to \[to\] \[duration\]?." — structured damage prevention (Shield
    /// of the Ages, Decorated Griffin). Distinct from [Prevent]'s
    /// free-text fallback: this variant carries the Amount/target
    /// structurally so downstream can size the shield correctly.
    /// Both `to` and `duration` are optional.
    record PreventNextDamage(
            Amount amount,
            boolean combat,
            @Nullable Subject to,
            @Nullable Duration duration) implements Effect {
        public PreventNextDamage(Amount amount, boolean combat) {
            this(amount, combat, null, null);
        }

        public PreventNextDamage withTarget(Subject to) {
            return new PreventNextDamage(amount, combat, to, duration);
        }

        public PreventNextDamage withDuration(Duration duration) {
            return new PreventNextDamage(amount, combat, to, duration);
        }
    }

    /// "Damage that would be dealt \[by|to\] \[subject\] can't be prevented." —
    /// inverse-prevention rule (Excruciator). The `dealtBy` flag
    /// distinguishes the "by" side (damage dealt by the subject is
    /// unprevenable) from the "to" side (damage dealt to the subject is
    /// unprevenable).
    record DamageCantBePrevented(Subject subject, boolean dealtBy) implements Effect {}

    /// "The damage can't be prevented." — sentence-scoped back-
    /// reference to the damage dealt by the immediately-preceding
    /// effect in the same resolution (Pinpoint Avalanche: "Pinpoint
    /// Avalanche deals 4 damage to target creature. The damage
    /// can't be prevented."). Distinct from [DamageCantBePrevented]
    /// which scopes by subject.
    record ThatDamageCantBePrevented() implements Effect {}

    /// "All \[kind\]? damage that would be dealt to \[from\] is dealt to
    /// \[to\] instead." — damage-redirection replacement (Pariah, Pariah's
    /// Shield, Palisade Giant, Empyrial Archangel, …). Distinct from
    /// [Prevent.AllDamage]: prevention zeroes the damage out, redirection
    /// retargets it. Rule 615 replacement effect.
    record RedirectDamage(Prevent.Kind kind, Subject from, Subject to) implements Effect {
        public RedirectDamage(Subject from, Subject to) {
            this(Prevent.Kind.ANY, from, to);
        }
    }

    /// "\[subject\] assign\[s\] \[its|their\] combat damage as though \[it|they\] weren't
    /// blocked." — lets a blocked attacker send all combat damage to the
    /// defending player/planeswalker (Deathcoil Wurm, Lone Wolf, Pride of
    /// Lions). Distinct from trample: there's no requirement to assign
    /// lethal to blockers first.
    record AssignDamageAsUnblocked(Subject subject) implements Effect {}

    /// "Remove \[subject\] from combat." — pulls an attacker or blocker out
    /// of combat without destroying it (rule 506.4; Labyrinth of Skophos:
    /// "Remove target attacking or blocking creature from combat.").
    record RemoveFromCombat(Subject subject) implements Effect {}

    /// "\[subject\] is every \[kind\] type." — sweeping type-union effect
    /// (Arachnoform: "Enchanted creature … is every creature type.";
    /// Prismatic Omen: "Lands you control are every basic land
    /// type."). The [TypeKind] enum names the family; semantically
    /// the subject gains every subtype in that family.
    record IsEveryType(
            Subject subject, TypeKind kind, @Nullable Duration duration) implements Effect {
        public IsEveryType(Subject subject, TypeKind kind) {
            this(subject, kind, null);
        }

        public IsEveryType withDuration(Duration duration) {
            return new IsEveryType(subject, kind, duration);
        }

        public enum TypeKind {
            CREATURE,
            LAND,
            BASIC_LAND,
            ENCHANTMENT,
            ARTIFACT,
            PLANESWALKER
        }
    }

    /// "\[sources\] can't cause \[player\] to sacrifice \[what\]." —
    /// sacrifice-immunity (Tajuru Preserver: "Spells and abilities
    /// your opponents control can't cause you to sacrifice
    /// permanents."). Distinct from a blanket "can't sacrifice"
    /// since it only blocks external forced-sacrifice sources.
    record CantBeForcedToSacrifice(Selector sources, Subject player, Selector what) implements Effect {}

    /// "It becomes \[day|night\]." — day/night designator flip (rule
    /// 726; Into the Night: "It becomes night."). Independent of
    /// the daybound/nightbound keyword triggers — this is a direct
    /// state change.
    record BecomeDayNight(DayNight state) implements Effect {
        public enum DayNight {
            DAY,
            NIGHT
        }
    }

    /// "\[subject\] assigns combat damage equal to \[use\] rather than \[insteadOf\]."
    /// — damage-assignment substitution (Doran, the Siege Tower: "Each
    /// creature assigns combat damage equal to its toughness rather than
    /// its power."). The \[Stat\] enum keeps the source/target clean
    /// instead of free-text.
    record AssignDamageUsing(Subject subject, Stat use, Stat insteadOf) implements Effect {
        public enum Stat {
            POWER,
            TOUGHNESS
        }
    }

    // Win/Loss

    record WinGame(Subject player) implements Effect {}

    record LoseGame(Subject player) implements Effect {}

    /// "\[player\] can't win the game." — Platinum Angel.
    record CantWinGame(Subject player) implements Effect {}

    /// "\[player\] can't lose the game." — Platinum Angel.
    record CantLoseGame(Subject player) implements Effect {}

    // Characteristics

    record SetCharacteristic(
            Subject target, String description, @Nullable Duration duration) implements Effect {
        public SetCharacteristic(Subject target, String description) {
            this(target, description, null);
        }

        public SetCharacteristic withDuration(Duration duration) {
            return new SetCharacteristic(target, description, duration);
        }
    }

    /// "\[effect\] if \[condition\]." — a base effect gated on a condition
    /// checked at resolution (e.g., Idle Thoughts: "Draw a card if you have
    /// no cards in hand."). The condition text is captured verbatim until
    /// the grammar refines structured variants.
    record Conditional(Effect effect, Condition condition) implements Effect {}

    /// "You may \[action\]. If you do, \[ifDone\]." — an optional action paired
    /// with a follow-up that resolves only if the player chose to do it
    /// (e.g., Abandon Attachments: "You may discard a card. If you do, draw
    /// two cards."). When the oracle text has no "if you do" continuation,
    /// `ifDone` is null.
    record Optional(Effect action, @Nullable Effect ifDone) implements Effect {
        public Optional(Effect action) {
            this(action, null);
        }

        public Optional withIfDone(Effect ifDone) {
            return new Optional(action, ifDone);
        }
    }

    // Combat restrictions

    /// "\[subject\] can't block \[what\] \[this turn\]." — static or temporary
    /// block restriction. `what` is a [Subject] so both
    /// selectors ("a creature you control") and self/demonstrative
    /// references ("this creature") parse. When oracle omits `what`,
    /// it defaults to the universal "all creatures" selector; `duration` is null unless the oracle specifies one.
    record CantBlock(
            Subject subject, Subject what, @Nullable Duration duration) implements Effect {
        public CantBlock(Subject subject, Subject what) {
            this(subject, what, null);
        }

        public CantBlock withWhat(Subject what) {
            return new CantBlock(subject, what, duration);
        }

        public CantBlock withDuration(Duration duration) {
            return new CantBlock(subject, what, duration);
        }
    }

    /// "\[subject\] \[can't|must|can only|can\] attack …" — unified
    /// attack-side restriction/capability. Variants of [Capability]
    /// capture the specific oracle shape; `duration` is optional and
    /// covers temporary forms ("can't attack this turn", "attacks each
    /// combat if able"). See also [Effect.CanBlock] for the
    /// block-side counterpart.
    record AttackRestriction(
            Subject subject,
            Capability capability,
            @Nullable Duration duration) implements Effect {
        public AttackRestriction(Subject subject, Capability capability) {
            this(subject, capability, null);
        }

        public AttackRestriction withDuration(Duration duration) {
            return new AttackRestriction(subject, capability, duration);
        }

        public sealed interface Capability {
            /// "can't attack" — plain prohibition (static on creatures
            /// like Walls in past rulings, or temporary via duration).
            enum Cant implements Capability {
                CANT
            }

            /// "can't attack alone" — may attack only alongside
            /// another creature.
            enum CantAlone implements Capability {
                CANT_ALONE
            }

            /// "can only attack alone" — may attack only when it's
            /// the sole attacker (Errantry).
            enum OnlyAlone implements Capability {
                ONLY_ALONE
            }

            /// "must attack \[whom\]? each combat if able" — forced
            /// attack (Crazed Goblin, Alluring Siren). `whom` is
            /// set when the oracle names a specific attack target.
            record Must(@Nullable Subject whom) implements Capability {
                public Must() {
                    this(null);
                }
            }

            /// "can't attack \[whom\]" — scoped prohibition (Creatures
            /// can't attack you).
            record CantWhom(Subject whom) implements Capability {}

            /// "can attack as though \[they\] didn't have \[ability\]" —
            /// Rolling Stones: "Wall creatures can attack as though
            /// they didn't have defender."
            record AsThoughWithout(Ability ability) implements Capability {}
        }
    }

    // Replacement-style statics

    /// "\[subject\] enter\[s\] tapped \[duration\]." — replacement on ETB. A
    /// trailing duration ("this turn") makes the replacement temporary
    /// (e.g., Due Respect: "Permanents enter tapped this turn.").
    record EnterTapped(Subject subject, @Nullable Duration duration) implements Effect {
        public EnterTapped(Subject subject) {
            this(subject, null);
        }

        public EnterTapped withDuration(Duration duration) {
            return new EnterTapped(subject, duration);
        }
    }

    /// "\[subject\] enter\[s\] with \[count\] \[type\] counters on it." — ETB
    /// replacement that places counters (e.g., Endless One, Hangarback
    /// Walker).
    record EnterWithCounters(Subject subject, Amount count, CounterType type) implements Effect {}

    /// "\[subject\] enter\[s\] with \[chooser\]'s choice of a \[A\] counter or
    /// a \[B\] counter on it." — ETB replacement where the player named by
    /// `chooser` picks one counter type from `options` at the time the
    /// permanent enters (Flycatcher Giraffid: "with your choice of a
    /// vigilance counter or a reach counter"). Emits exactly one counter
    /// of the chosen type.
    record EnterWithChosenCounter(Subject subject, Subject chooser, List<CounterType> options) implements Effect {}

    // Characteristic-setting statics

    /// "\[subject\] are/is \[colors\] \[duration\]?" — continuous color
    /// override. `colors` is a sealed [Colors] describing
    /// whether the new colors are fixed at parse time ([Colors.Fixed],
    /// covers "colorless" as an empty list and "all colors" as the
    /// five basic colors) or chosen at resolution
    /// ([Colors.OfChoice], Vodalian Mystic). Duration is optional;
    /// set for temporary overrides (e.g., Ancient Kavu: "becomes
    /// colorless until end of turn").
    record SetColors(
            Subject subject, Colors colors, @Nullable Duration duration) implements Effect {
        public SetColors(Subject subject, Colors colors) {
            this(subject, colors, null);
        }

        public SetColors withDuration(Duration duration) {
            return new SetColors(subject, colors, duration);
        }

        public sealed interface Colors {
            /// "\[colors\]" — an explicit color set. Empty list models
            /// "colorless"; the five basic colors model "all colors".
            record Fixed(List<Color> colors) implements Colors {}

            /// "the color of \[poss\] choice" — the actor picks a color
            /// at resolution (Vodalian Mystic). The possessive phrase
            /// ("your"/"their"/"his"/"her"/"its") resolves to the player
            /// who makes the choice; defaults to [Subject.PlayerRef#YOU].
            record OfChoice(Subject chooser) implements Colors {
                public OfChoice() {
                    this(Subject.player(Subject.PlayerRef.YOU));
                }
            }
        }
    }

    /// "\[subject\] are/is \[subtype\]+ \[duration\]?." — continuous effect
    /// setting one or more subtypes (e.g., "Nonbasic lands are Islands";
    /// Lush Growth: "Enchanted land is a Mountain, Forest, and Plains.").
    /// Optional duration for temporary forms (Slimy Kavu: "… until end
    /// of turn.").
    record SetSubtype(
            Subject subject,
            List<Subtype> subtypes,
            @Nullable Duration duration) implements Effect {
        public SetSubtype(Subject subject, Subtype subtype) {
            this(subject, List.of(subtype), null);
        }

        public SetSubtype(Subject subject, List<Subtype> subtypes) {
            this(subject, subtypes, null);
        }

        public SetSubtype withDuration(Duration duration) {
            return new SetSubtype(subject, subtypes, duration);
        }
    }

    /// "\[player\] chooses a card in their hand and discards the rest." —
    /// Monomania. Retains all chosen cards, discards everything else in
    /// the player's hand.
    record DiscardAllButOne(Subject player) implements Effect {}

    /// "Activate only \[when\]." — activation-time restriction on the
    /// enclosing activated ability. Variants cover the common oracle
    /// shapes: gated on a game-state condition (Temple of the False God,
    /// Fool's Tome), restricted to sorcery speed (Fractured Powerstone),
    /// or bounded to a specific timing window (Disrupting Scepter:
    /// "Activate only during your turn.").
    sealed interface ActivateOnly extends Effect {
        /// "Activate only if \[condition\]." — free-text predicate for now.
        record If(Condition condition) implements ActivateOnly {}

        /// "Activate only as a sorcery." — sorcery-speed restriction.
        enum AsSorcery implements ActivateOnly {
            AS_SORCERY
        }

        /// "Activate only during \[when\]." — timing-window restriction
        /// (Disrupting Scepter: "during your turn"; Winding Canyons:
        /// "during an opponent's turn"). `when` captures the rest of
        /// the clause verbatim since the set of legal timing phrases is
        /// open-ended.
        record During(String when) implements ActivateOnly {}
    }

    /// "\[subject\] are/is \[card type\] in addition to their other types." —
    /// additive card-type assignment (Enchanted Evening: "All permanents
    /// are enchantments in addition to their other types."). Distinct from
    /// [SetSubtype] in that it targets card types (enchantment,
    /// creature, artifact, …) rather than subtypes.
    record AddCardType(
            Subject subject, List<CardType> types, @Nullable Duration duration) implements Effect {
        public AddCardType(Subject subject, List<CardType> types) {
            this(subject, types, null);
        }
    }

    /// "\[subject\] are/is no longer \[supertype\]." — continuous effect removing
    /// a supertype (e.g., "All lands are no longer snow").
    record LoseSupertype(Subject subject, Supertype supertype) implements Effect {}

    // Regeneration

    /// "Regenerate \[subject\]." — rule 701.15.
    record Regenerate(Subject subject) implements Effect {}

    // Cost modification

    /// Continuous cost modifier. The [CostSource] distinguishes between
    /// "Spells … cost {N} more/less" (a subject) and "\[Keyword\] costs cost
    /// {N} more/less" (a keyword ability, rule 702.1a). `scaleBy`
    /// carries a "for each …" count that multiplies `amount`
    /// (Ghoultree: "This spell costs {1} less to cast for each
    /// creature card in your graveyard.").
    record ModifyCost(
            CostSource source,
            List<ManaSymbol> amount,
            CostDelta delta,
            @Nullable Condition condition,
            @Nullable Amount scaleBy)
            implements Effect {
        public ModifyCost(CostSource source, List<ManaSymbol> amount, CostDelta delta) {
            this(source, amount, delta, null, null);
        }

        public ModifyCost(CostSource source, List<ManaSymbol> amount, CostDelta delta, @Nullable Condition condition) {
            this(source, amount, delta, condition, null);
        }

        public ModifyCost withCondition(Condition condition) {
            return new ModifyCost(source, amount, delta, condition, scaleBy);
        }

        public ModifyCost withScaleBy(Amount scaleBy) {
            return new ModifyCost(source, amount, delta, condition, scaleBy);
        }
    }

    // Action restrictions

    /// "\[subject\] can't cycle cards." — restriction on activating cycling.
    record CantCycle(Subject subject) implements Effect {}

    /// "X is \[amount\]." — binds the ability-level X to an amount
    /// (Bargaining Table: "X is the number of cards in an opponent's
    /// hand."). Typically trails the primary effect on cards with an
    /// {X} cost component.
    record DefineX(Amount amount) implements Effect {}

    /// "\[player\] get\[s\] {E}{E}..." — gain energy counters
    /// (Live Fast, Attune with Aether). `count` is the number of
    /// energy symbols in the cost-like {E} sequence.
    record GainEnergy(Subject player, int count) implements Effect {}

    /// "\[subject\] crews \[selector\] using \[property\] rather than \[other\]."
    /// — Giant Ox. The creature substitutes a non-power stat when
    /// computing crew contribution.
    record CrewsUsing(Subject subject, Selector what, Property propertyUsed, Property propertyReplaced)
            implements Effect {}

    /// "\[subject\] can't phase out \[duration\]?" — Spatial Binding.
    record CantPhaseOut(Subject subject, @Nullable Duration duration) implements Effect {
        public CantPhaseOut(Subject subject) {
            this(subject, null);
        }

        public CantPhaseOut withDuration(Duration duration) {
            return new CantPhaseOut(subject, duration);
        }
    }

    /// "Activated abilities of \[selector\] can't be activated." — e.g.,
    /// Collector Ouphe ("of artifacts"), Cursed Totem ("of creatures").
    record CantActivate(Selector owners) implements Effect {}

    /// "\[subject\] can't be blocked \[By\] \[duration\]." — evasion restriction.
    /// `by == null` means unconditionally (no one can block). The
    /// [By] variants distinguish "by X" (X specifically cannot block)
    /// from "except by X" (only X can block, all others cannot). A trailing
    /// duration ("this turn", "until end of turn") makes the restriction
    /// temporary (e.g., Trailblazer: "Target creature can't be blocked this
    /// turn.").
    record CantBeBlocked(
            Subject subject, @Nullable By by, @Nullable Duration duration) implements Effect {

        public CantBeBlocked(Subject subject) {
            this(subject, null, null);
        }

        public CantBeBlocked(Subject subject, @Nullable By by) {
            this(subject, by, null);
        }

        public CantBeBlocked withBy(By by) {
            return new CantBeBlocked(subject, by, duration);
        }

        public CantBeBlocked withDuration(Duration duration) {
            return new CantBeBlocked(subject, by, duration);
        }

        /// Which blockers the restriction names.
        public sealed interface By {
            /// "by \[selector\]" — the named blockers are forbidden (others
            /// may still block).
            record Matching(Selector selector) implements By {}

            /// "except by \[selector\]" — only the named blockers are allowed
            /// (every other blocker is forbidden).
            record Except(Selector selector) implements By {}

            /// "by more than \[max\] \[selector\]" — attacker can be blocked,
            /// but no more than `max` blockers at once (e.g., Huang
            /// Zhong, Shu General: "can't be blocked by more than one
            /// creature.").
            record LimitOf(Amount max, Selector selector) implements By {}
        }
    }

    /// "\[subject\] can't search libraries." — search-restriction effect.
    record CantSearchLibraries(Subject subject) implements Effect {}

    /// "\[players\] can cast spells only during \[timing\]." — positive
    /// timing restriction: overrides rule 117.1 so the named players may
    /// only cast spells during the named window (e.g., Dosan the Falling
    /// Leaf: "Players can cast spells only during their own turns.").
    /// The timing phrase is captured verbatim until the grammar refines
    /// structured turn/phase references.
    record RestrictSpellTiming(Subject players, String timing) implements Effect {}

    /// "\[subject\] can't cast \[what\] spells \[duration\]." — casting restriction.
    /// A trailing duration ("this turn") makes it temporary (e.g., Silence).
    record CantCast(
            Subject subject, Selector what, @Nullable Duration duration) implements Effect {
        public CantCast(Subject subject, Selector what) {
            this(subject, what, null);
        }

        public CantCast withDuration(Duration duration) {
            return new CantCast(subject, what, duration);
        }
    }

    /// "\[subject\] can't block alone." — can block only alongside another.
    record CantBlockAlone(Subject subject) implements Effect {}

    /// "Count the number of \[subject\]." — binds a "that number"
    /// back-reference for the following sentence (Invincible Hymn:
    /// "Count the number of cards in your library. Your life total
    /// becomes that number."). The `amount` is the computed count,
    /// which a subsequent [Amount.Reference]("that number") picks up
    /// at resolution.
    record Count(Amount amount) implements Effect {}

    /// "create one of each." — replacement-effect shorthand where the
    /// chooser creates one of each token kind named in the preceding
    /// "If you would create A, B, or C token" event (Academy
    /// Manufactor). No fields — the kinds come from the replaced
    /// event by resolution convention.
    enum CreateOneOfEach implements Effect {
        CREATE_ONE_OF_EACH
    }

    /// "Populate." — create a token that's a copy of a creature token
    /// you control (rule 701.28, Wake the Reflections). Parameter-less;
    /// the choice of source token is resolved at effect resolution.
    enum Populate implements Effect {
        POPULATE
    }

    /// "\[player\] take\[s\] \[N\] extra turn(s) after this one." — rule 500.7.
    /// Count defaults to one ("an extra turn") but can be higher (Time
    /// Stretch: "Target player takes two extra turns after this one.").
    record TakeExtraTurn(Subject player, Amount count) implements Effect {
        public TakeExtraTurn(Subject player) {
            this(player, Amount.exact(1));
        }
    }

    /// "\[player\] may play lands from \[zone\]." — permission to play lands from
    /// a non-hand zone (typically graveyard).
    record PlayLandsFrom(Subject player, Zone.Named zone) implements Effect {}

    /// "\[player\] may play \[up to\] N additional lands \[this turn\]." —
    /// raises the per-turn land-play limit (rule 305.2) by the stated amount
    /// (e.g., Summer Bloom: "You may play up to three additional lands this
    /// turn."). Duration is optional — a null duration means the permission
    /// is static, while "this turn" scopes it to the current turn.
    record PlayAdditionalLands(
            Subject player, Amount count, @Nullable Duration duration) implements Effect {
        public PlayAdditionalLands(Subject player, Amount count) {
            this(player, count, null);
        }

        public PlayAdditionalLands withDuration(Duration duration) {
            return new PlayAdditionalLands(player, count, duration);
        }
    }

    /// "\[player\] skip\[s\] \[what\] \[duration\]?" — replacement effect
    /// per rule 614.10. Replaces an upcoming turn, phase, or step with
    /// nothing. Trailing duration ("this turn") scopes it (Moment of
    /// Silence: "Target player skips their next combat phase this
    /// turn.").
    record Skip(Subject player, Skippable what, @Nullable Duration duration) implements Effect {
        public Skip(Subject player, Skippable what) {
            this(player, what, null);
        }

        public Skip withDuration(Duration duration) {
            return new Skip(player, what, duration);
        }
    }

    /// "The Ring tempts \[player\]." — rule 716.
    record RingTempts(Subject player) implements Effect {}

    /// "Look at \[target\]." — reveal-to-looker-only effect. The target can be
    /// any subject: a player's hand ("target player's hand" as a
    /// [Subject.PossessiveSubject]), a face-down creature (Smoke
    /// Teller), a card in exile, etc.
    record LookAt(Subject target) implements Effect {}

    /// "Put \[subject\] back in any order." — put cards back in a
    /// chosen order (Index: "Look at the top five cards of your
    /// library, then put them back in any order."). Typically the
    /// subject is a pronoun ("them") referring to a prior [LookAt].
    record PutBack(Subject target) implements Effect {}

    /// "\[player\] may cast \[what\] from \[zone\]." — permission to cast a
    /// specific card from a non-standard zone (e.g., Misthollow Griffin).
    /// "\[player\] may cast \[what\] from \[zone\]+." — permission to cast from
    /// one or more non-stack zones (e.g., Squee, the Immortal: "… from
    /// your graveyard or from exile."). The source list has at least one
    /// entry.
    record CastFromZone(Subject player, Subject what, List<Zone.Named> from) implements Effect {
        public CastFromZone(Subject player, Subject what, Zone.Named from) {
            this(player, what, List.of(from));
        }
    }

    /// "\[player\] may choose new targets for \[spell\]." — redirect a spell's
    /// targets (e.g., Redirect).
    record ChooseNewTargets(Subject player, Subject spell) implements Effect {}

    /// "\[chooser\]? choose\[s\] \[selector\] \[at random\]?." — player
    /// selects from the named set, marking them for a later effect
    /// (Duneblast: "Choose up to one creature. Destroy the rest.";
    /// Imperial Edict: "Target opponent chooses a creature they
    /// control. Destroy that creature."). `chooser` is `null` for the
    /// imperative "Choose X" form where "you" is implicit;
    /// non-null names the player making the choice. `atRandom=true`
    /// when the oracle specifies "at random" (Last One Standing).
    /// "Choose a color \[of \[scope\]\]?." — color-choice effect. The
    /// chosen color is usually bound by a following "that color"
    /// reference (Meteor Crater: "Choose a color of a permanent you
    /// control. Add one mana of that color."). `scope` restricts the
    /// pool of choosable colors to those of a referenced group; null
    /// for the unrestricted form (Brave the Elements, etc.).
    record ChooseColor(@Nullable Subject chooser, @Nullable Subject scope) implements Effect {
        public ChooseColor() {
            this(null, null);
        }

        public ChooseColor withScope(Subject scope) {
            return new ChooseColor(chooser, scope);
        }
    }

    /// "Choose a \[creature|land|artifact|enchantment|planeswalker\] type."
    /// — a type-choice effect that sets up a subsequent "the chosen
    /// type" back-reference (Kindred Dominance: "Choose a creature
    /// type. Destroy all creatures that aren't of the chosen type.").
    /// `kind` is the canonical kind name ("creature", "land", …) so
    /// consumers match against a closed set rather than free text.
    record ChooseType(CardType kind) implements Effect {}

    record Choose(@Nullable Subject chooser, Subject what, boolean atRandom) implements Effect {
        public Choose(Subject what) {
            this(null, what, false);
        }

        public Choose(Subject what, boolean atRandom) {
            this(null, what, atRandom);
        }

        public Choose withChooser(Subject chooser) {
            return new Choose(chooser, what, atRandom);
        }

        public Choose asRandom() {
            return new Choose(chooser, what, true);
        }
    }

    /// "\[player\] become\[s\] the monarch." — rule 716 (e.g., Palace Sentinels).
    record BecomeMonarch(Subject player) implements Effect {}

    /// "\[players\] exchange life totals." — swap life between the two target
    /// players (e.g., Soul Conduit).
    record ExchangeLifeTotals(Subject players) implements Effect {}

    /// "\[player\] may change any targets of \[spell\]." — retarget any number
    /// of targets on a spell, analogous to [ChooseNewTargets] but
    /// targets can remain unchanged (e.g., Sideswipe).
    record ChangeAnyTargets(Subject player, Subject spell) implements Effect {}

    /// "\[player\] get\[s\] \[N\] \[marker\]." — receive N marker counters not
    /// attached to any permanent. Used for energy ({E}), tickets ({TK}),
    /// and similar non-permanent counters that reside on a player.
    record GetMarker(Subject player, Amount count, Marker marker) implements Effect {}

    /// "\[subject\] can't be countered." — spell counter-immunity.
    record CantBeCountered(Subject subject) implements Effect {}

    /// "\[subject\] must be blocked \[by \[by\]\]? \[if able\] \[duration\]?." —
    /// combat must-block restriction. Optional `by` narrows the blocker set
    /// (Slayer's Cleaver: "Equipped creature … must be blocked by an
    /// Eldrazi if able."); null `by` means any creature that can.
    /// Optional duration (Satyr Piper: "Target creature must be blocked
    /// this turn if able.").
    record MustBeBlocked(
            Subject subject,
            @Nullable Selector by,
            @Nullable Duration duration) implements Effect {
        public MustBeBlocked(Subject subject) {
            this(subject, null, null);
        }

        public MustBeBlocked withBy(Selector by) {
            return new MustBeBlocked(subject, by, duration);
        }

        public MustBeBlocked withDuration(Duration duration) {
            return new MustBeBlocked(subject, by, duration);
        }
    }

    /// "\[subject\] can't have counters put on it." — prevents counter
    /// placement (e.g., Melira's Keepers).
    record CantHaveCounters(Subject subject) implements Effect {}

    /// "\[subject\] can't be regenerated \[duration\]." — suppresses the
    /// regeneration replacement effect (rule 701.15), often attached to a
    /// Destroy. A trailing duration ("this turn") makes it temporary
    /// (e.g., Furnace Brood: "{R}: Target creature can't be regenerated
    /// this turn.").
    record CantBeRegenerated(Subject subject, @Nullable Duration duration) implements Effect {
        public CantBeRegenerated(Subject subject) {
            this(subject, null);
        }

        public CantBeRegenerated withDuration(Duration duration) {
            return new CantBeRegenerated(subject, duration);
        }
    }

    /// "\[subject\] can't be equipped." — prevents Equipment from attaching
    /// (e.g., Goblin Brawler).
    record CantBeEquipped(Subject subject) implements Effect {}

    /// "\[subject\] can't be enchanted by \[by\]." — Aura-binding
    /// restriction. `by` names the blocked Aura selector. Typically
    /// "other Auras" (Consecrate Land: "Enchanted land … can't be
    /// enchanted by other Auras.") — the OTHER qualifier is load-
    /// bearing: the current enchantment may stay attached.
    record CantBeEnchanted(Subject subject, Selector by) implements Effect {}

    /// "Unattach \[selector\] from \[target\]." — forcibly removes all matching
    /// attached objects (Auras/Equipment/Fortifications) from the target.
    record Unattach(Selector what, Subject from) implements Effect {}

    /// "\[player\] may cast \[what\] as though \[clause\]." — lifts a timing or
    /// zone restriction (e.g., Vedalken Orrery: "You may cast spells as
    /// though they had flash."). The `asThough` predicate is captured
    /// verbatim until the grammar refines structured variants.
    /// "\[player\] may cast \[what\] as though \[clause\]" — cast
    /// permission with a modifier (Vedalken Orrery: "You may cast
    /// spells as though they had flash."; Silver Scrutiny: "You may
    /// cast this spell as though it had flash if X is 3 or less.").
    /// `what` is the target — typically a selector ("spells",
    /// "instant spells"), but can also be the self-cast `~` when a
    /// card references its own casting. The `asThough` string is a
    /// free-text fallback for the modifier clause.
    record CastAsThough(Subject player, Subject what, String asThough) implements Effect {}

    /// "\[player\] may cast \[what\] without paying \[its|their\] mana cost(s)."
    /// — alternative-cost permission (e.g., Dracogenesis: "You may cast
    /// Dragon spells without paying their mana costs.").
    record CastWithoutPaying(Subject player, Selector what) implements Effect {}

    /// "\[player\] may pay \[alternative\] rather than pay the mana cost
    /// for \[spells\]." — alternative-cost permission (Fist of Suns).
    /// `alternative` is the structured replacement cost (a
    /// [Cost.Mana] pay); `forSpells` selects which spells may use it
    /// (typically "spells you cast").
    record AlternativeCostForSpells(Subject player, Cost alternative, Selector forSpells) implements Effect {}

    /// "\[player\] may spend \[X\] mana as though it were \[Y\] mana." — color
    /// substitution on mana spend (e.g., Sunglasses of Urza: "You may spend
    /// white mana as though it were red mana."). Today only color-for-color
    /// substitution is modeled; other shapes ("any color", "mana of any
    /// type") can slot in as additional fields or a sealed variant later.
    record SpendManaAsThough(Subject player, Color fromColor, Color asColor) implements Effect {}

    /// "\[subject\] have base \[power|toughness|power and toughness\]
    /// \[value\] \[duration\]?." — sets a base P/T (rule 613.4, layer 7b).
    /// Either side of `basePT` may be null for the asymmetric oracle
    /// forms: "base power N" (Singing Tree) leaves toughness null,
    /// "base toughness N" (Maha) leaves power null, and "base power
    /// and toughness P/T" (Godhead of Awe) sets both. The word *base*
    /// distinguishing this effect from layer 7c P/T arithmetic is
    /// carried by the effect type itself.
    record SetBasePT(
            Subject target, PtValue basePT, @Nullable Duration duration) implements Effect {
        public SetBasePT(Subject target, PtValue basePT) {
            this(target, basePT, null);
        }

        public SetBasePT withDuration(Duration duration) {
            return new SetBasePT(target, basePT, duration);
        }
    }

    /// "Exchange \[zone a\] and \[zone b\]." — swap the contents of two zones
    /// for the named player (e.g., Harness Infinity: "Exchange your hand
    /// and graveyard.").
    record ExchangeZones(Subject player, Zone a, Zone b) implements Effect {}

    /// "Exchange \[player\]'s life total with \[subject\]'s \[property\]." —
    /// swap a life total with a numeric permanent characteristic (e.g.,
    /// Evra, Halcyon Witness: "Exchange your life total with ~'s power.").
    record ExchangeLifeWithProperty(Subject player, Subject source, Property property) implements Effect {}

    /// "Players don't lose unspent mana as steps and phases end." —
    /// Upwelling. A unique static effect that alters rule 106.4's mana
    /// pool emptying.
    record ManaPoolPersists(Subject subject) implements Effect {}

    /// "As an additional cost to cast this spell, \[cost\]." — appends an
    /// extra cost to the spell's casting cost (e.g., Mardu Outrider).
    /// Captured as a spell-ability effect; the cost is retained as a
    /// [Cost].
    record AdditionalCost(Cost cost) implements Effect {}

    /// "Spend only mana \[produced by <selector>\] to cast this spell." —
    /// restricts which mana can pay for this spell (e.g., Myr Superion:
    /// "Spend only mana produced by creatures to cast this spell.").
    record ManaSpendRestriction(Selector source) implements Effect {}

    /// "\[chooser\] choose\[s\] how \[voter\] vote\[s\] \[duration\]?." — redirects
    /// the voting choice for a Voting-Box-style mechanic (e.g., Illusion of
    /// Choice: "You choose how each player votes this turn.").
    record ChoosePlayerVote(
            Subject chooser, Subject voter, @Nullable Duration duration) implements Effect {
        public ChoosePlayerVote(Subject chooser, Subject voter) {
            this(chooser, voter, null);
        }

        public ChoosePlayerVote withDuration(Duration duration) {
            return new ChoosePlayerVote(chooser, voter, duration);
        }
    }

    /// "\[player\] take\[s\] the initiative." — initiative mechanic (rule
    /// 718). Captures the player who becomes the Initiative holder.
    record TakeInitiative(Subject player) implements Effect {}

    /// "\[subject\] don't untap \[scope\]?." — static restriction blocking
    /// untap of matching permanents (Choke: "Islands don't untap during
    /// their controllers' untap steps.").
    record DontUntap(Subject subject, @Nullable String scope) implements Effect {
        public DontUntap(Subject subject) {
            this(subject, null);
        }

        public DontUntap withScope(String scope) {
            return new DontUntap(subject, scope);
        }
    }

    /// "\[subject\] can't untap more than \[amount\] \[selector\] during
    /// \[scope\]?." — a per-period cap on untaps (Mungha Wurm).
    record UntapLimit(
            Subject subject,
            Amount max,
            Selector what,
            @Nullable String scope) implements Effect {
        public UntapLimit(Subject subject, Amount max, Selector what) {
            this(subject, max, what, null);
        }

        public UntapLimit withScope(String scope) {
            return new UntapLimit(subject, max, what, scope);
        }
    }

    /// "You can cast only \[amount\] more spell\[s\] this turn." — a
    /// hard cap on remaining casts for the controller (Irencrag Feat).
    record CastCountLimit(
            Subject player, Amount max, @Nullable Duration duration) implements Effect {
        public CastCountLimit(Subject player, Amount max) {
            this(player, max, null);
        }
    }

    /// "\[subject\] can't play lands \[duration\]?." — prevents land plays
    /// (e.g., Turf Wound: "Target player can't play lands this turn.").
    record CantPlayLands(Subject subject, @Nullable Duration duration) implements Effect {
        public CantPlayLands(Subject subject) {
            this(subject, null);
        }

        public CantPlayLands withDuration(Duration duration) {
            return new CantPlayLands(subject, duration);
        }
    }

    /// "No more than N creatures can attack \[whom\] each combat." — cap on
    /// attackers per combat (e.g., Crawlspace). The cap applies across all
    /// attackers, not per subject.
    record AttackLimit(Amount max, Subject whom) implements Effect {}

    /// "Double the power \[and/or toughness\] of \[subject\] \[N times\]?
    /// \[duration\]?." — P/T-doubling effect (e.g., Unleash Fury, Exponential
    /// Growth). `doublePower` and `doubleToughness`
    /// independently flag which stat doubles; `times` is the repeat
    /// count (null means one doubling).
    record DoublePT(
            Subject target,
            boolean doublePower,
            boolean doubleToughness,
            @Nullable Amount times,
            @Nullable Duration duration)
            implements Effect {
        public DoublePT(Subject target, boolean doublePower, boolean doubleToughness) {
            this(target, doublePower, doubleToughness, null, null);
        }

        public DoublePT withTimes(Amount times) {
            return new DoublePT(target, doublePower, doubleToughness, times, duration);
        }

        public DoublePT withDuration(Duration duration) {
            return new DoublePT(target, doublePower, doubleToughness, times, duration);
        }
    }

    /// "Change the target of \[spell\]." — redirects a single-target spell
    /// or ability (e.g., Deflection). Distinct from [ChooseNewTargets]
    /// which retargets multiple or all targets.
    record ChangeTheTarget(Subject spell) implements Effect {}

    /// "\[subject\] enter\[s\] as a copy of \[target\]." — replacement effect
    /// that substitutes entry with a copy of another permanent (e.g.,
    /// Essence of the Wild: "Creatures you control enter as a copy of this
    /// creature.").
    /// "\[subject\] becomes a copy of \[source\]" — live copy effect,
    /// distinct from [EnterAsCopy] in that the subject is already on
    /// the battlefield (Mirrorform: "Each nonland permanent you
    /// control becomes a copy of target non-Aura permanent.").
    record BecomeCopy(
            Subject subject, Subject copyOf, @Nullable Duration duration) implements Effect {
        public BecomeCopy(Subject subject, Subject copyOf) {
            this(subject, copyOf, null);
        }

        public BecomeCopy withDuration(Duration duration) {
            return new BecomeCopy(subject, copyOf, duration);
        }
    }

    record EnterAsCopy(Subject subject, Subject copyOf, boolean tapped) implements Effect {
        public EnterAsCopy(Subject subject, Subject copyOf) {
            this(subject, copyOf, false);
        }

        public EnterAsCopy withTapped() {
            return new EnterAsCopy(subject, copyOf, true);
        }
    }

    /// "\[subject\]'s \[property\] is equal to \[amount\]." — static
    /// characteristic-setting effect (e.g., Sima Yi: "Sima Yi's power is
    /// equal to the number of Swamps you control.").
    record SetPropertyValue(Subject subject, Property property, Amount value) implements Effect {}

    /// "Spend this mana only to \[restriction\]." — restricts how the
    /// produced mana may be used (e.g., Omen Hawker: "Spend this mana
    /// only to activate abilities.").
    record SpendThisManaOnly(String restriction) implements Effect {}

    /// "Activate only \[N\] time\[s\] each turn." — caps activations of the
    /// preceding ability (Salvaged Manaworker: "Activate only once each
    /// turn."). Applies to the most recently declared activated ability.
    record ActivationLimit(Amount max) implements Effect {}

    /// "You may play a card you own from outside the game this turn." —
    /// Wish-style effect. Captures the scope as free text for now.
    record PlayFromOutside(Subject player, @Nullable Duration duration) implements Effect {
        public PlayFromOutside(Subject player) {
            this(player, null);
        }

        public PlayFromOutside withDuration(Duration duration) {
            return new PlayFromOutside(player, duration);
        }
    }

    /// "\[subject\] are \[supertype\]." — continuous effect adding a
    /// supertype (e.g., Rootpath Purifier: "Lands you control and land
    /// cards in your library are basic."). Distinct from
    /// [LoseSupertype] which removes.
    record SetSupertype(Subject subject, Supertype supertype) implements Effect {}

    /// "Turn \[subject\] face up." — flips a face-down permanent
    /// (Break Open). The actor is the spell/ability controller.
    record TurnFaceUp(Subject target) implements Effect {}

    /// "Turn \[subject\] face down." — Cyber Conversion.
    record TurnFaceDown(Subject target) implements Effect {}

    /// "\[subject\] \[entering|dying|entering or dying\] don't cause abilities
    /// [of \[scope\]]? to trigger." — suppresses ETB- or death-triggered
    /// abilities (e.g., Tocatli Honor Guard, Torpor Orb, Hushbringer —
    /// "entering or dying"; Elesh Norn — scoped to "abilities of permanents
    /// your opponents control").
    record SuppressEtbTriggers(
            Subject subject, Event event, @Nullable Selector scope) implements Effect {
        public SuppressEtbTriggers(Subject subject, Event event) {
            this(subject, event, null);
        }

        public SuppressEtbTriggers withScope(Selector scope) {
            return new SuppressEtbTriggers(subject, event, scope);
        }

        public enum Event {
            ENTERING,
            DYING,
            ENTERING_OR_DYING
        }
    }

    /// "If <trigger-clause>, that ability triggers \[amount\] additional
    /// time\[s\]." — Panharmonicon-style trigger duplication (e.g., Elesh
    /// Norn, Mother of Machines). The trigger clause is captured verbatim
    /// until the grammar refines the sub-structure into selectors.
    record AdditionalEtbTriggers(String triggerClause, Amount additional) implements Effect {}

    /// "\[kind\] abilities of \[scope\] trigger \[amount\] additional time\[s\]." —
    /// Panharmonicon-style duplication scoped to a named ability kind on a
    /// referenced object (e.g., Hama Pashar, Ruin Seeker: "Room abilities
    /// of dungeons you own trigger an additional time.").
    record AbilityKindTriggersAdditional(String kind, Selector scope, Amount additional) implements Effect {}

    /// "\[player\] play\[s\] with the top card of \[their\] library revealed." —
    /// rule 701.18 (e.g., Goblin Spy, Future Sight).
    record PlayWithTopRevealed(Subject player) implements Effect {}

    /// "\[subject\] can block …" — positive block-capability expansion. The
    /// [Capability] variant names the specific extension (today only
    /// [Capability.AnyNumberOf] for "can block any number of X"; more
    /// shapes like "can block an additional creature" fit the same mold).
    record CanBlock(
            Subject subject,
            Capability capability,
            @Nullable Duration duration) implements Effect {
        public CanBlock(Subject subject, Capability capability) {
            this(subject, capability, null);
        }

        public CanBlock withDuration(Duration duration) {
            return new CanBlock(subject, capability, duration);
        }

        public sealed interface Capability {
            /// "can block any number of \[what\]." — lifts the single-blocker
            /// restriction (e.g., Palace Guard, Wall of Tears).
            record AnyNumberOf(Selector what) implements Capability {}

            /// "can block an additional \[what\] each combat" — raises the
            /// block cap by a fixed amount per combat (e.g., Foriysian
            /// Brigade: "can block an additional creature each combat").
            record Additional(Amount count, Selector what) implements Capability {}

            /// "can block \[selector\] as though it had \[keyword\]" —
            /// grants an ability-match capability to bypass an
            /// evasion keyword on the blocked creature (Heartwood
            /// Dryad: "can block creatures with shadow as though it
            /// had shadow.").
            record AsThoughHad(Selector what, Ability keyword) implements Capability {}

            /// "can block as though \[state\]" — grants blocking ability
            /// as if the subject were in a different state (Masako
            /// the Humorless: "Tapped creatures you control can block
            /// as though they were untapped."). `state` captures the
            /// pretended state as a free-text predicate.
            record AsThoughState(String state) implements Capability {}

            /// "can block only \[restriction\]" — narrows which creatures
            /// may be blocked (Gloomwidow: "This creature can block
            /// only creatures with flying.").
            record Only(Selector restriction) implements Capability {}
        }
    }

    /// "\[subject\] can't crew \[vehicles\]." — suppresses the crew
    /// activated ability on the target vehicles (Revoke Privileges).
    record CantCrew(Subject subject, Selector crewTarget) implements Effect {}

    /// "\[subject\] attacks or blocks each combat if able." — disjunctive
    /// must-attack-or-block restriction (e.g., Iron Golem, Relentless
    /// Raptor). Satisfied by either attacking or blocking in each combat.
    record MustAttackOrBlock(Subject subject) implements Effect {}

    /// "\[player\]'s life total becomes N." — set a player's life to a fixed value.
    record LifeTotalBecomes(Subject player, Amount value) implements Effect {}

    /// "\[subject\] can't be the target of spells or abilities / of \[what\]."
    /// When `by` is null the restriction covers all spells/abilities.
    record CantBeTargeted(Subject subject, @Nullable Selector by) implements Effect {
        public CantBeTargeted(Subject subject) {
            this(subject, null);
        }
    }

    /// "\[subject\] blocks \[target\]? \[if able\] \[duration\]?." — must-block
    /// requirement on a blocker. The `target` is optional; when
    /// present, it names the specific attacker that must be blocked
    /// (Hunt Down: "Target creature blocks target creature this turn if
    /// able.").
    record MustBlock(
            Subject subject,
            @Nullable Subject target,
            @Nullable Duration duration) implements Effect {
        public MustBlock(Subject subject) {
            this(subject, null, null);
        }

        public MustBlock(Subject subject, @Nullable Duration duration) {
            this(subject, null, duration);
        }

        public MustBlock withTarget(Subject target) {
            return new MustBlock(subject, target, duration);
        }

        public MustBlock withDuration(Duration duration) {
            return new MustBlock(subject, target, duration);
        }
    }

    /// "Players play with their hands revealed." — rule 701.17 variant.
    record PlayWithHandsRevealed(Subject players) implements Effect {}

    /// `The "<rule>" doesn't apply.` — a static effect that suppresses a named
    /// comprehensive rule (e.g., Mirror Gallery suppresses the legend rule).
    record RuleDoesntApply(GameRule rule) implements Effect {}

    /// "\[subject\] can't \[draw|cast\] more than N \[cards|spells\] each turn." —
    /// a per-turn upper limit on draws or spell casts (e.g., Spirit of the
    /// Labyrinth, Arcane Laboratory). Distinct from [CantCast] /
    /// [Draw] because it's a limit, not a prohibition.
    record PerTurnLimit(Subject subject, PerTurnLimit.Action action, Amount max) implements Effect {
        public enum Action {
            DRAW_CARDS,
            CAST_SPELLS
        }
    }

    /// Modifies the maximum hand size rule for `player`:
    /// fixed ("maximum hand size is 8"), delta ("+1 maximum hand size"), or
    /// removed ("no maximum hand size"). Maximum hand size is a player-only
    /// concept — the subject is always a player.
    record MaximumHandSize(Subject player, HandSize size) implements Effect {
        public sealed interface HandSize {
            /// "Maximum hand size is N."
            record Fixed(int value) implements HandSize {}

            /// "Your maximum hand size is increased/reduced by N."
            record Delta(int delta) implements HandSize {}

            /// "You have no maximum hand size."
            enum None implements HandSize {
                NONE
            }
        }
    }
}
