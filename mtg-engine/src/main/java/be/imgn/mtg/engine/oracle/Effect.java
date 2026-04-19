package be.imgn.mtg.engine.oracle;

import java.util.List;

import org.jspecify.annotations.Nullable;

/// An effect produced by a spell or ability.
public sealed interface Effect {

    // Removal

    /// "Destroy [target] [at end of combat | at the beginning of …]?" —
    /// {@code at} defers resolution to a later timing (e.g., Silent
    /// Assassin: "Destroy target blocking creature at end of combat.").
    /// Null {@code at} is the common immediate form.
    record Destroy(Subject target, @Nullable Duration at) implements Effect {
        Destroy(Subject target) {
            this(target, null);
        }

        public Destroy withAt(Duration at) {
            return new Destroy(target, at);
        }
    }

    /// Exile objects (via a {@link Subject}) or whole zones (via a
    /// {@link Exiled.Zones} variant). The optional {@code from} restricts
    /// which source zone is searched ("from any graveyard", "from exile").
    /// The optional {@code actor} is the player performing the exile — set
    /// when oracle text says "[player] exiles …" (e.g., Mudhole: "Target
    /// player exiles all land cards from their graveyard."); null for the
    /// common imperative form where the spell itself does the exiling.
    record Exile(
            Exiled exiled,
            Zone.@Nullable Source from,
            @Nullable Subject actor) implements Effect {
        Exile(Exiled exiled) {
            this(exiled, null, null);
        }

        Exile(Exiled exiled, Zone.@Nullable Source from) {
            this(exiled, from, null);
        }

        public Exile withActor(Subject actor) {
            return new Exile(exiled, from, actor);
        }
    }

    /// "[who] sacrifice[s] [what]." — {@code what} is a {@link Subject} so
    /// it can be either a selector ("a creature you control") or a
    /// self-reference ("this creature", e.g., Barbarian Outcast).
    record Sacrifice(Subject who, Subject what) implements Effect {}

    /// "Return [subject] [from X]? to Y." — move an object to a destination.
    /// The optional {@code from} specifies the source zone when it isn't
    /// the battlefield default (e.g., Auroral Procession: "Return target
    /// card from your graveyard to your hand.").
    record Bounce(Subject target, Zone.@Nullable Source from, Zone.Destination to) implements Effect {
        Bounce(Subject target, Zone.Destination to) {
            this(target, null, to);
        }
    }

    // Damage & Life

    record DealDamage(Subject source, Amount amount, Subject target) implements Effect {}

    record GainLife(Subject player, Amount amount) implements Effect {}

    record LoseLife(Subject player, Amount amount) implements Effect {}

    // Card Manipulation

    record Draw(Subject player, Amount amount) implements Effect {}

    /// Discard cards from the player's hand. The {@link Discarded} variant
    /// distinguishes between "discard N cards" and "discard your hand".
    record Discard(Subject player, Discarded discarded) implements Effect {}

    record Mill(Subject player, Amount amount) implements Effect {}

    record Scry(Amount amount) implements Effect {}

    record Search(String possessive, Selector what) implements Effect {}

    /// "[player] shuffles [source]? [into [destination]]?." — unified
    /// shuffle effect. The common short form "[player] shuffles [their]
    /// library" leaves both zones null (e.g., Soldier of Fortune). When
    /// oracle text names both a source and a destination, the cards in
    /// {@code source} are placed into {@code destination} which is then
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

    /// "[actor]? reveal[s] [target]." — reveal an object (or zone via a
    /// possessive "hand"). The optional {@code actor} is the player performing
    /// the reveal when oracle text names one (e.g., Trapfinder's Trick:
    /// "Target player reveals their hand…"); null for the common imperative
    /// form where the spell itself reveals.
    record Reveal(@Nullable Subject actor, Subject target) implements Effect {
        Reveal(Subject target) {
            this(null, target);
        }

        public Reveal withActor(Subject actor) {
            return new Reveal(actor, target);
        }
    }

    // Tap/Untap

    record Tap(Subject target) implements Effect {}

    record Untap(Subject target) implements Effect {}

    // Counters

    record AddCounters(Amount count, CounterType type, Subject target) implements Effect {}

    record RemoveCounters(Amount count, CounterType type, Subject target) implements Effect {}

    // Ability Modification

    record GainAbility(
            Subject target,
            List<Ability> abilities,
            @Nullable Duration duration) implements Effect {
        GainAbility(Subject target, List<Ability> abilities) {
            this(target, abilities, null);
        }

        public GainAbility withDuration(Duration duration) {
            return new GainAbility(target, abilities, duration);
        }
    }

    /// "[subject] lose[s] [keyword…|all abilities] [duration]." The
    /// {@link LoseAbility.Lost} variant distinguishes between named keyword
    /// abilities (e.g., "lose flying") and the sweeping "lose all abilities"
    /// form (e.g., Yixlid Jailer). A trailing duration ("until end of turn")
    /// makes the loss temporary (e.g., Radjan Spirit).
    record LoseAbility(Subject target, Lost lost, @Nullable Duration duration) implements Effect {
        LoseAbility(Subject target, Lost lost) {
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
        }
    }

    // P/T Modification

    /// "[subject] get[s] [±X/±Y] [duration]?" — P/T modifier. Two optional
    /// tails refine the base modifier:
    /// - {@code scaleBy}: "for each …" multiplies the base by the count
    ///   (Grim Strider: "-1/-1 for each card in your hand").
    /// - {@code xDefinition}: ", where X is …" binds the X in a variable
    ///   modifier (Death's Shadow: "-X/-X, where X is your life total").
    record ModifyPT(
            Subject target,
            PtModifier modifier,
            @Nullable Duration duration,
            @Nullable Amount scaleBy,
            @Nullable Amount xDefinition)
            implements Effect {
        ModifyPT(Subject target, PtModifier modifier) {
            this(target, modifier, null, null, null);
        }

        ModifyPT(Subject target, PtModifier modifier, @Nullable Duration duration) {
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
        GainControl(Subject player, Subject target) {
            this(player, target, null);
        }

        public GainControl withDuration(Duration duration) {
            return new GainControl(player, target, duration);
        }
    }

    /// "Exchange control of [targets]." — swap controllers between the
    /// selected permanents (e.g., Switcheroo: two target creatures).
    record ExchangeControl(Selector targets) implements Effect {}

    // Tokens

    /// "Create [N] [tapped]? [token]." — `tapped` is true when oracle text
    /// says the tokens enter the battlefield tapped (e.g., Shadow Summoning:
    /// "Create two tapped 1/1 white Spirit creature tokens with flying.").
    record CreateToken(Amount count, TokenDescription token, boolean tapped) implements Effect {
        public CreateToken(Amount count, TokenDescription token) {
            this(count, token, false);
        }
    }

    // Counterspell

    /// "Counter [target] [if <condition>]." — counterspell effect, optionally
    /// gated on a condition about the target spell (e.g., Ertai's Trickery:
    /// "Counter target spell if it was kicked.").
    record CounterSpell(Subject target, @Nullable Condition condition) implements Effect {
        CounterSpell(Subject target) {
            this(target, null);
        }

        public CounterSpell withCondition(Condition condition) {
            return new CounterSpell(target, condition);
        }
    }

    // Combat

    record Fight(Subject a, Subject b) implements Effect {}

    // Mana

    /// Add one or more mana options to the player's mana pool. Each option is
    /// a {@link ManaOption} — either a fixed set of symbols or a repeated
    /// (variable-count) pattern. When more than one option is present (e.g.,
    /// `Add {B} or {R}`, or `Add X mana of any one color`), the player
    /// chooses one.
    /// Add one or more mana options to a player's mana pool. The optional
    /// {@code player} is the actor when oracle text names one (Tangleroot:
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

    /// "Put [what] [from X]? [to Y]." — move a subject to a destination,
    /// optionally naming the source zone (e.g., False Mourning: "Put
    /// target card from your graveyard on top of your library.").
    record ZoneMove(Subject what, Zone.@Nullable Source from, Zone.Destination to) implements Effect {
        ZoneMove(Subject what, Zone.Destination to) {
            this(what, null, to);
        }

        public ZoneMove withFrom(Zone.Source from) {
            return new ZoneMove(what, from, to);
        }
    }

    // Transform/Copy

    record Transform(Subject target) implements Effect {}

    record Copy(Subject target) implements Effect {}

    // Replacement & Prevention

    record Replace(Subject what, String event, Effect replacement) implements Effect {}

    /// "If <condition>, <override> instead." — conditional override of the
    /// previously-stated effect. Covers the short "Add {U}. If you played a
    /// land this turn, add {B} instead." idiom (River of Tears) where the
    /// grammar doesn't name an explicit `would` event.
    record ConditionalOverride(Condition condition, Effect override) implements Effect {}

    /// "For each [scope], [body]." — iterate the body over each object
    /// matching {@code scope}. Within the body, demonstrative references
    /// like "that land" refer to the current iteration (Cleansing: "For
    /// each land, destroy that land unless any player pays 1 life.").
    record ForEach(Selector scope, Effect body) implements Effect {}

    /// "Switch [subject]'s power and toughness [duration]?" — swap the
    /// creature's power and toughness values (About Face). Duration is
    /// null for the permanent form and set for the common temporary
    /// "until end of turn" variant.
    record SwitchPT(Subject target, @Nullable Duration duration) implements Effect {
        SwitchPT(Subject target) {
            this(target, null);
        }

        public SwitchPT withDuration(Duration duration) {
            return new SwitchPT(target, duration);
        }
    }

    /// "Attach [what] to [to]." — move an Equipment/Aura to a new host
    /// (Aura Finesse: "Attach target Aura you control to target creature.").
    record Attach(Subject what, Subject to) implements Effect {}

    /// "Distribute [N] [type] counters among [subject]." — distribute a
    /// pool of counters across multiple targets chosen by the controller
    /// (Elven Rite: "Distribute two +1/+1 counters among one or two target
    /// creatures."). The distribution choice itself is deferred to
    /// resolution.
    record DistributeCounters(Amount count, CounterType type, Subject among) implements Effect {}

    /// "[subject] crews [selector] as though its power were [N] greater."
    /// — crew-boost (Hotshot Mechanic). The delta modifies the effective
    /// power used toward a Crew cost requirement.
    record CrewsWithBoostedPower(Subject subject, Selector target, int powerDelta) implements Effect {}

    /// "Flip [N] coin[s] [and ignore M]?" — coin-flip effect (Krark's
    /// Thumb replacement, Crush of Wurms, etc.). {@code ignore} is the
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

    /// "Double the amount of each type of unspent mana [you|target
    /// player] ha[s|ve]." — Doubling Cube / Mana Reflection. Doubles every
    /// type of mana currently in the player's pool.
    record DoubleMana(Subject player) implements Effect {}

    /// "[player] pays [cost]." — optional payment inside a {@code you may
    /// pay …. If you do, …} idiom (Inheritance). Stored as a general
    /// payment action; the "if you do" continuation attaches to the
    /// enclosing {@link Optional}.
    record Pay(Subject player, Cost cost) implements Effect {}

    record Prevent(String description) implements Effect {}

    /// "Damage that would be dealt [by|to] [subject] can't be prevented." —
    /// inverse-prevention rule (Excruciator). The {@code dealtBy} flag
    /// distinguishes the "by" side (damage dealt by the subject is
    /// unprevenable) from the "to" side (damage dealt to the subject is
    /// unprevenable).
    record DamageCantBePrevented(Subject subject, boolean dealtBy) implements Effect {}

    // Win/Loss

    record WinGame(Subject player) implements Effect {}

    record LoseGame(Subject player) implements Effect {}

    // Characteristics

    record SetCharacteristic(
            Subject target, String description, @Nullable Duration duration) implements Effect {
        SetCharacteristic(Subject target, String description) {
            this(target, description, null);
        }

        public SetCharacteristic withDuration(Duration duration) {
            return new SetCharacteristic(target, description, duration);
        }
    }

    /// "[effect] if [condition]." — a base effect gated on a condition
    /// checked at resolution (e.g., Idle Thoughts: "Draw a card if you have
    /// no cards in hand."). The condition text is captured verbatim until
    /// the grammar refines structured variants.
    record Conditional(Effect effect, Condition condition) implements Effect {}

    /// "You may [action]. If you do, [ifDone]." — an optional action paired
    /// with a follow-up that resolves only if the player chose to do it
    /// (e.g., Abandon Attachments: "You may discard a card. If you do, draw
    /// two cards."). When the oracle text has no "if you do" continuation,
    /// {@code ifDone} is null.
    record Optional(Effect action, @Nullable Effect ifDone) implements Effect {
        Optional(Effect action) {
            this(action, null);
        }

        public Optional withIfDone(Effect ifDone) {
            return new Optional(action, ifDone);
        }
    }

    // Combat restrictions

    /// "[subject] can't block [what] [this turn]." — static or temporary
    /// block restriction. {@code what} is a {@link Subject} so both
    /// selectors ("a creature you control") and self/demonstrative
    /// references ("this creature") parse. When oracle omits {@code what},
    /// it defaults to the universal "all creatures" selector; {@code
    /// duration} is null unless the oracle specifies one.
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

    /// "[subject] can't attack." — static restriction granted by an effect.
    record CantAttack(Subject subject, @Nullable Duration duration) implements Effect {
        public CantAttack(Subject subject) {
            this(subject, null);
        }

        public CantAttack withDuration(Duration duration) {
            return new CantAttack(subject, duration);
        }
    }

    // Replacement-style statics

    /// "[subject] enter[s] tapped [duration]." — replacement on ETB. A
    /// trailing duration ("this turn") makes the replacement temporary
    /// (e.g., Due Respect: "Permanents enter tapped this turn.").
    record EnterTapped(Subject subject, @Nullable Duration duration) implements Effect {
        EnterTapped(Subject subject) {
            this(subject, null);
        }

        public EnterTapped withDuration(Duration duration) {
            return new EnterTapped(subject, duration);
        }
    }

    /// "[subject] enter[s] with [count] [type] counters on it." — ETB
    /// replacement that places counters (e.g., Endless One, Hangarback
    /// Walker).
    record EnterWithCounters(Subject subject, Amount count, CounterType type) implements Effect {}

    // Characteristic-setting statics

    /// "[subject] are/is [colors]." — continuous effect setting the color(s).
    /// An empty list means colorless ("are colorless").
    /// "[subject] are/is [colors|colorless|all colors] [duration]?" —
    /// continuous color override. Duration is optional; it's set when the
    /// effect is temporary (e.g., Ancient Kavu: "becomes colorless until
    /// end of turn").
    record SetColors(
            Subject subject, List<Color> colors, @Nullable Duration duration) implements Effect {
        SetColors(Subject subject, List<Color> colors) {
            this(subject, colors, null);
        }

        public SetColors withDuration(Duration duration) {
            return new SetColors(subject, colors, duration);
        }
    }

    /// "[subject] are/is [subtype]+ [duration]?." — continuous effect
    /// setting one or more subtypes (e.g., "Nonbasic lands are Islands";
    /// Lush Growth: "Enchanted land is a Mountain, Forest, and Plains.").
    /// Optional duration for temporary forms (Slimy Kavu: "… until end
    /// of turn.").
    record SetSubtype(
            Subject subject,
            List<String> subtypes,
            @Nullable Duration duration) implements Effect {
        public SetSubtype(Subject subject, String subtype) {
            this(subject, List.of(subtype), null);
        }

        public SetSubtype(Subject subject, List<String> subtypes) {
            this(subject, subtypes, null);
        }

        public SetSubtype withDuration(Duration duration) {
            return new SetSubtype(subject, subtypes, duration);
        }
    }

    /// "[player] chooses a card in their hand and discards the rest." —
    /// Monomania. Retains all chosen cards, discards everything else in
    /// the player's hand.
    record DiscardAllButOne(Subject player) implements Effect {}

    /// "Activate only [when]." — activation-time restriction on the
    /// enclosing activated ability. Variants cover the two common oracle
    /// shapes: gated on a game-state condition (Temple of the False God,
    /// Fool's Tome) or restricted to sorcery speed (Fractured Powerstone).
    sealed interface ActivateOnly extends Effect {
        /// "Activate only if [condition]." — free-text predicate for now.
        record If(Condition condition) implements ActivateOnly {}

        /// "Activate only as a sorcery." — sorcery-speed restriction.
        enum AsSorcery implements ActivateOnly {
            AS_SORCERY
        }
    }

    /// "[subject] are/is [card type] in addition to their other types." —
    /// additive card-type assignment (Enchanted Evening: "All permanents
    /// are enchantments in addition to their other types."). Distinct from
    /// {@link SetSubtype} in that it targets card types (enchantment,
    /// creature, artifact, …) rather than subtypes.
    record AddCardType(
            Subject subject, List<CardType> types, @Nullable Duration duration) implements Effect {
        public AddCardType(Subject subject, List<CardType> types) {
            this(subject, types, null);
        }
    }

    /// "[subject] are/is no longer [supertype]." — continuous effect removing
    /// a supertype (e.g., "All lands are no longer snow").
    record LoseSupertype(Subject subject, Supertype supertype) implements Effect {}

    // Regeneration

    /// "Regenerate [subject]." — rule 701.15.
    record Regenerate(Subject subject) implements Effect {}

    // Cost modification

    /// Continuous cost modifier. The {@link CostSource} distinguishes between
    /// "Spells … cost {N} more/less" (a subject) and "[Keyword] costs cost
    /// {N} more/less" (a keyword ability, rule 702.1a).
    record ModifyCost(CostSource source, List<ManaSymbol> amount, CostDelta delta) implements Effect {}

    // Action restrictions

    /// "[subject] can't cycle cards." — restriction on activating cycling.
    record CantCycle(Subject subject) implements Effect {}

    /// "Activated abilities of [selector] can't be activated." — e.g.,
    /// Collector Ouphe ("of artifacts"), Cursed Totem ("of creatures").
    record CantActivate(Selector owners) implements Effect {}

    /// "[subject] can't be blocked [By] [duration]." — evasion restriction.
    /// {@code by == null} means unconditionally (no one can block). The
    /// {@link By} variants distinguish "by X" (X specifically cannot block)
    /// from "except by X" (only X can block, all others cannot). A trailing
    /// duration ("this turn", "until end of turn") makes the restriction
    /// temporary (e.g., Trailblazer: "Target creature can't be blocked this
    /// turn.").
    record CantBeBlocked(
            Subject subject, @Nullable By by, @Nullable Duration duration) implements Effect {

        CantBeBlocked(Subject subject) {
            this(subject, null, null);
        }

        CantBeBlocked(Subject subject, @Nullable By by) {
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
            /// "by [selector]" — the named blockers are forbidden (others
            /// may still block).
            record Matching(Selector selector) implements By {}

            /// "except by [selector]" — only the named blockers are allowed
            /// (every other blocker is forbidden).
            record Except(Selector selector) implements By {}

            /// "by more than [max] [selector]" — attacker can be blocked,
            /// but no more than {@code max} blockers at once (e.g., Huang
            /// Zhong, Shu General: "can't be blocked by more than one
            /// creature.").
            record LimitOf(Amount max, Selector selector) implements By {}
        }
    }

    /// "[subject] can't search libraries." — search-restriction effect.
    record CantSearchLibraries(Subject subject) implements Effect {}

    /// "[players] can cast spells only during [timing]." — positive
    /// timing restriction: overrides rule 117.1 so the named players may
    /// only cast spells during the named window (e.g., Dosan the Falling
    /// Leaf: "Players can cast spells only during their own turns.").
    /// The timing phrase is captured verbatim until the grammar refines
    /// structured turn/phase references.
    record RestrictSpellTiming(Subject players, String timing) implements Effect {}

    /// "[subject] can't cast [what] spells [duration]." — casting restriction.
    /// A trailing duration ("this turn") makes it temporary (e.g., Silence).
    record CantCast(
            Subject subject, Selector what, @Nullable Duration duration) implements Effect {
        CantCast(Subject subject, Selector what) {
            this(subject, what, null);
        }

        public CantCast withDuration(Duration duration) {
            return new CantCast(subject, what, duration);
        }
    }

    /// "[subject] can't block alone." — can block only alongside another.
    record CantBlockAlone(Subject subject) implements Effect {}

    /// "[subject] can't attack alone." — can attack only alongside another.
    record CantAttackAlone(Subject subject) implements Effect {}

    /// "[subject] can't attack [whom]." — cannot attack a specific player
    /// (e.g., "Creatures can't attack you").
    record CantAttackWhom(Subject subject, Subject whom) implements Effect {}

    /// "[player] take[s] [N] extra turn(s) after this one." — rule 500.7.
    /// Count defaults to one ("an extra turn") but can be higher (Time
    /// Stretch: "Target player takes two extra turns after this one.").
    record TakeExtraTurn(Subject player, Amount count) implements Effect {
        TakeExtraTurn(Subject player) {
            this(player, Amount.exact(1));
        }
    }

    /// "[player] may play lands from [zone]." — permission to play lands from
    /// a non-hand zone (typically graveyard).
    record PlayLandsFrom(Subject player, Zone.Named zone) implements Effect {}

    /// "[player] may play [up to] N additional lands [this turn]." —
    /// raises the per-turn land-play limit (rule 305.2) by the stated amount
    /// (e.g., Summer Bloom: "You may play up to three additional lands this
    /// turn."). Duration is optional — a null duration means the permission
    /// is static, while "this turn" scopes it to the current turn.
    record PlayAdditionalLands(
            Subject player, Amount count, @Nullable Duration duration) implements Effect {
        PlayAdditionalLands(Subject player, Amount count) {
            this(player, count, null);
        }

        public PlayAdditionalLands withDuration(Duration duration) {
            return new PlayAdditionalLands(player, count, duration);
        }
    }

    /// "[player] skip[s] [what]." — replacement effect per rule 614.10.
    /// Replaces an upcoming turn, phase, or step with nothing.
    /// "[player] skip[s] [their] [next] [what] [duration]?" — rule 614.10.
    /// Trailing duration ("this turn") scopes it (Moment of Silence:
    /// "Target player skips their next combat phase this turn.").
    record Skip(Subject player, Skippable what, @Nullable Duration duration) implements Effect {
        Skip(Subject player, Skippable what) {
            this(player, what, null);
        }

        public Skip withDuration(Duration duration) {
            return new Skip(player, what, duration);
        }
    }

    /// "The Ring tempts [player]." — rule 716.
    record RingTempts(Subject player) implements Effect {}

    /// "Look at [target]." — reveal-to-looker-only effect. The target can be
    /// any subject: a player's hand ("target player's hand" as a
    /// {@link Subject.PossessiveSubject}), a face-down creature (Smoke
    /// Teller), a card in exile, etc.
    record LookAt(Subject target) implements Effect {}

    /// "[player] may cast [what] from [zone]." — permission to cast a
    /// specific card from a non-standard zone (e.g., Misthollow Griffin).
    /// "[player] may cast [what] from [zone]+." — permission to cast from
    /// one or more non-stack zones (e.g., Squee, the Immortal: "… from
    /// your graveyard or from exile."). The source list has at least one
    /// entry.
    record CastFromZone(Subject player, Subject what, List<Zone.Named> from) implements Effect {
        public CastFromZone(Subject player, Subject what, Zone.Named from) {
            this(player, what, List.of(from));
        }
    }

    /// "[player] may choose new targets for [spell]." — redirect a spell's
    /// targets (e.g., Redirect).
    record ChooseNewTargets(Subject player, Subject spell) implements Effect {}

    /// "Choose [selector] [at random]?." — player selects from the named
    /// set, marking them for a later effect (Duneblast: "Choose up to one
    /// creature. Destroy the rest."). {@code atRandom=true} when the
    /// oracle specifies "at random" (Last One Standing).
    record Choose(Subject what, boolean atRandom) implements Effect {
        Choose(Subject what) {
            this(what, false);
        }
    }

    /// "[player] become[s] the monarch." — rule 716 (e.g., Palace Sentinels).
    record BecomeMonarch(Subject player) implements Effect {}

    /// "[players] exchange life totals." — swap life between the two target
    /// players (e.g., Soul Conduit).
    record ExchangeLifeTotals(Subject players) implements Effect {}

    /// "[player] may change any targets of [spell]." — retarget any number
    /// of targets on a spell, analogous to {@link ChooseNewTargets} but
    /// targets can remain unchanged (e.g., Sideswipe).
    record ChangeAnyTargets(Subject player, Subject spell) implements Effect {}

    /// "[player] get[s] [N] [marker]." — receive N marker counters not
    /// attached to any permanent. Used for energy ({E}), tickets ({TK}),
    /// and similar non-permanent counters that reside on a player.
    record GetMarker(Subject player, Amount count, String marker) implements Effect {}

    /// "[subject] can't be countered." — spell counter-immunity.
    record CantBeCountered(Subject subject) implements Effect {}

    /// "[subject] must be blocked [if able]." — combat must-block restriction.
    /// "[subject] must be blocked [if able] [duration]?." — combat
    /// must-block restriction with optional duration (Satyr Piper: "Target
    /// creature must be blocked this turn if able.").
    record MustBeBlocked(Subject subject, @Nullable Duration duration) implements Effect {
        MustBeBlocked(Subject subject) {
            this(subject, null);
        }

        public MustBeBlocked withDuration(Duration duration) {
            return new MustBeBlocked(subject, duration);
        }
    }

    /// "[subject] can't attack or block [duration]." — combined combat
    /// restriction, optionally scoped to a duration (e.g., Off Balance:
    /// "Target creature can't attack or block this turn.").
    record CantAttackOrBlock(Subject subject, @Nullable Duration duration) implements Effect {
        CantAttackOrBlock(Subject subject) {
            this(subject, null);
        }

        public CantAttackOrBlock withDuration(Duration duration) {
            return new CantAttackOrBlock(subject, duration);
        }
    }

    /// "[subject] can't have counters put on it." — prevents counter
    /// placement (e.g., Melira's Keepers).
    record CantHaveCounters(Subject subject) implements Effect {}

    /// "[subject] can't be regenerated [duration]." — suppresses the
    /// regeneration replacement effect (rule 701.15), often attached to a
    /// Destroy. A trailing duration ("this turn") makes it temporary
    /// (e.g., Furnace Brood: "{R}: Target creature can't be regenerated
    /// this turn.").
    record CantBeRegenerated(Subject subject, @Nullable Duration duration) implements Effect {
        CantBeRegenerated(Subject subject) {
            this(subject, null);
        }

        public CantBeRegenerated withDuration(Duration duration) {
            return new CantBeRegenerated(subject, duration);
        }
    }

    /// "[subject] can't be equipped." — prevents Equipment from attaching
    /// (e.g., Goblin Brawler).
    record CantBeEquipped(Subject subject) implements Effect {}

    /// "Unattach [selector] from [target]." — forcibly removes all matching
    /// attached objects (Auras/Equipment/Fortifications) from the target.
    record Unattach(Selector what, Subject from) implements Effect {}

    /// "[player] may cast [what] as though [clause]." — lifts a timing or
    /// zone restriction (e.g., Vedalken Orrery: "You may cast spells as
    /// though they had flash."). The {@code asThough} predicate is captured
    /// verbatim until the grammar refines structured variants.
    record CastAsThough(Subject player, Selector what, String asThough) implements Effect {}

    /// "[player] may cast [what] without paying [its|their] mana cost(s)."
    /// — alternative-cost permission (e.g., Dracogenesis: "You may cast
    /// Dragon spells without paying their mana costs.").
    record CastWithoutPaying(Subject player, Selector what) implements Effect {}

    /// "[player] may spend [X] mana as though it were [Y] mana." — color
    /// substitution on mana spend (e.g., Sunglasses of Urza: "You may spend
    /// white mana as though it were red mana."). Today only color-for-color
    /// substitution is modeled; other shapes ("any color", "mana of any
    /// type") can slot in as additional fields or a sealed variant later.
    record SpendManaAsThough(Subject player, Color fromColor, Color asColor) implements Effect {}

    /// "[subject] have base power and toughness [P/T] [duration]?." —
    /// sets a base P/T (e.g., Godhead of Awe).
    record SetBasePT(
            Subject target, PtValue basePT, @Nullable Duration duration) implements Effect {
        SetBasePT(Subject target, PtValue basePT) {
            this(target, basePT, null);
        }

        public SetBasePT withDuration(Duration duration) {
            return new SetBasePT(target, basePT, duration);
        }
    }

    /// "[subject] can block only [restriction]." — narrows which creatures
    /// can be blocked (e.g., Gloomwidow: "This creature can block only
    /// creatures with flying.").
    record CanBlockOnly(Subject subject, Selector restriction) implements Effect {}

    /// "Exchange [zone a] and [zone b]." — swap the contents of two zones
    /// for the named player (e.g., Harness Infinity: "Exchange your hand
    /// and graveyard.").
    record ExchangeZones(Subject player, Zone a, Zone b) implements Effect {}

    /// "Exchange [player]'s life total with [subject]'s [property]." —
    /// swap a life total with a numeric permanent characteristic (e.g.,
    /// Evra, Halcyon Witness: "Exchange your life total with ~'s power.").
    record ExchangeLifeWithProperty(Subject player, Subject source, String property) implements Effect {}

    /// "Players don't lose unspent mana as steps and phases end." —
    /// Upwelling. A unique static effect that alters rule 106.4's mana
    /// pool emptying.
    record ManaPoolPersists(Subject subject) implements Effect {}

    /// "As an additional cost to cast this spell, [cost]." — appends an
    /// extra cost to the spell's casting cost (e.g., Mardu Outrider).
    /// Captured as a spell-ability effect; the cost is retained as a
    /// {@link Cost}.
    record AdditionalCost(Cost cost) implements Effect {}

    /// "Spend only mana [produced by <selector>] to cast this spell." —
    /// restricts which mana can pay for this spell (e.g., Myr Superion:
    /// "Spend only mana produced by creatures to cast this spell.").
    record ManaSpendRestriction(Selector source) implements Effect {}

    /// "[chooser] choose[s] how [voter] vote[s] [duration]?." — redirects
    /// the voting choice for a Voting-Box-style mechanic (e.g., Illusion of
    /// Choice: "You choose how each player votes this turn.").
    record ChoosePlayerVote(
            Subject chooser, Subject voter, @Nullable Duration duration) implements Effect {
        ChoosePlayerVote(Subject chooser, Subject voter) {
            this(chooser, voter, null);
        }

        public ChoosePlayerVote withDuration(Duration duration) {
            return new ChoosePlayerVote(chooser, voter, duration);
        }
    }

    /// "[player] take[s] the initiative." — initiative mechanic (rule
    /// 718). Captures the player who becomes the Initiative holder.
    record TakeInitiative(Subject player) implements Effect {}

    /// "[subject] don't untap [scope]?." — static restriction blocking
    /// untap of matching permanents (Choke: "Islands don't untap during
    /// their controllers' untap steps.").
    record DontUntap(Subject subject, @Nullable String scope) implements Effect {
        DontUntap(Subject subject) {
            this(subject, null);
        }

        public DontUntap withScope(String scope) {
            return new DontUntap(subject, scope);
        }
    }

    /// "[subject] can't untap more than [amount] [selector] during
    /// [scope]?." — a per-period cap on untaps (Mungha Wurm).
    record UntapLimit(
            Subject subject,
            Amount max,
            Selector what,
            @Nullable String scope) implements Effect {
        UntapLimit(Subject subject, Amount max, Selector what) {
            this(subject, max, what, null);
        }

        public UntapLimit withScope(String scope) {
            return new UntapLimit(subject, max, what, scope);
        }
    }

    /// "You can cast only [amount] more spell[s] this turn." — a
    /// hard cap on remaining casts for the controller (Irencrag Feat).
    record CastCountLimit(
            Subject player, Amount max, @Nullable Duration duration) implements Effect {
        CastCountLimit(Subject player, Amount max) {
            this(player, max, null);
        }
    }

    /// "[subject] can't play lands [duration]?." — prevents land plays
    /// (e.g., Turf Wound: "Target player can't play lands this turn.").
    record CantPlayLands(Subject subject, @Nullable Duration duration) implements Effect {
        CantPlayLands(Subject subject) {
            this(subject, null);
        }

        public CantPlayLands withDuration(Duration duration) {
            return new CantPlayLands(subject, duration);
        }
    }

    /// "No more than N creatures can attack [whom] each combat." — cap on
    /// attackers per combat (e.g., Crawlspace). The cap applies across all
    /// attackers, not per subject.
    record AttackLimit(Amount max, Subject whom) implements Effect {}

    /// "Double the power [and/or toughness] of [subject] [N times]?
    /// [duration]?." — P/T-doubling effect (e.g., Unleash Fury, Exponential
    /// Growth). {@code doublePower} and {@code doubleToughness}
    /// independently flag which stat doubles; {@code times} is the repeat
    /// count (null means one doubling).
    record DoublePT(
            Subject target,
            boolean doublePower,
            boolean doubleToughness,
            @Nullable Amount times,
            @Nullable Duration duration)
            implements Effect {
        DoublePT(Subject target, boolean doublePower, boolean doubleToughness) {
            this(target, doublePower, doubleToughness, null, null);
        }

        public DoublePT withTimes(Amount times) {
            return new DoublePT(target, doublePower, doubleToughness, times, duration);
        }

        public DoublePT withDuration(Duration duration) {
            return new DoublePT(target, doublePower, doubleToughness, times, duration);
        }
    }

    /// "Change the target of [spell]." — redirects a single-target spell
    /// or ability (e.g., Deflection). Distinct from {@link ChooseNewTargets}
    /// which retargets multiple or all targets.
    record ChangeTheTarget(Subject spell) implements Effect {}

    /// "[subject] enter[s] as a copy of [target]." — replacement effect
    /// that substitutes entry with a copy of another permanent (e.g.,
    /// Essence of the Wild: "Creatures you control enter as a copy of this
    /// creature.").
    record EnterAsCopy(Subject subject, Subject copyOf) implements Effect {}

    /// "[subject]'s [property] is equal to [amount]." — static
    /// characteristic-setting effect (e.g., Sima Yi: "Sima Yi's power is
    /// equal to the number of Swamps you control.").
    record SetPropertyValue(Subject subject, String property, Amount value) implements Effect {}

    /// "Spend this mana only to [restriction]." — restricts how the
    /// produced mana may be used (e.g., Omen Hawker: "Spend this mana
    /// only to activate abilities.").
    record SpendThisManaOnly(String restriction) implements Effect {}

    /// "Activate only [N] time[s] each turn." — caps activations of the
    /// preceding ability (Salvaged Manaworker: "Activate only once each
    /// turn."). Applies to the most recently declared activated ability.
    record ActivationLimit(Amount max) implements Effect {}

    /// "You may play a card you own from outside the game this turn." —
    /// Wish-style effect. Captures the scope as free text for now.
    record PlayFromOutside(Subject player, @Nullable Duration duration) implements Effect {
        PlayFromOutside(Subject player) {
            this(player, null);
        }

        public PlayFromOutside withDuration(Duration duration) {
            return new PlayFromOutside(player, duration);
        }
    }

    /// "[subject] are [supertype]." — continuous effect adding a
    /// supertype (e.g., Rootpath Purifier: "Lands you control and land
    /// cards in your library are basic."). Distinct from
    /// {@link LoseSupertype} which removes.
    record SetSupertype(Subject subject, Supertype supertype) implements Effect {}

    /// "Turn [subject] face up." — flips a face-down permanent
    /// (Break Open). The actor is the spell/ability controller.
    record TurnFaceUp(Subject target) implements Effect {}

    /// "[subject] [entering|dying|entering or dying] don't cause abilities
    /// [of [scope]]? to trigger." — suppresses ETB- or death-triggered
    /// abilities (e.g., Tocatli Honor Guard, Torpor Orb, Hushbringer —
    /// "entering or dying"; Elesh Norn — scoped to "abilities of permanents
    /// your opponents control").
    record SuppressEtbTriggers(
            Subject subject, Event event, @Nullable Selector scope) implements Effect {
        SuppressEtbTriggers(Subject subject, Event event) {
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

    /// "If <trigger-clause>, that ability triggers [amount] additional
    /// time[s]." — Panharmonicon-style trigger duplication (e.g., Elesh
    /// Norn, Mother of Machines). The trigger clause is captured verbatim
    /// until the grammar refines the sub-structure into selectors.
    record AdditionalEtbTriggers(String triggerClause, Amount additional) implements Effect {}

    /// "[kind] abilities of [scope] trigger [amount] additional time[s]." —
    /// Panharmonicon-style duplication scoped to a named ability kind on a
    /// referenced object (e.g., Hama Pashar, Ruin Seeker: "Room abilities
    /// of dungeons you own trigger an additional time.").
    record AbilityKindTriggersAdditional(String kind, Selector scope, Amount additional) implements Effect {}

    /// "[player] may tap or untap [target]." — player chooses tap or untap
    /// on the same target (e.g., Thassa's Ire, Puppeteer).
    record TapOrUntap(Subject target) implements Effect {}

    /// "[player] play[s] with the top card of [their] library revealed." —
    /// rule 701.18 (e.g., Goblin Spy, Future Sight).
    record PlayWithTopRevealed(Subject player) implements Effect {}

    /// "[subject] can block …" — positive block-capability expansion. The
    /// {@link Capability} variant names the specific extension (today only
    /// {@link Capability.AnyNumberOf} for "can block any number of X"; more
    /// shapes like "can block an additional creature" fit the same mold).
    record CanBlock(
            Subject subject,
            Capability capability,
            @Nullable Duration duration) implements Effect {
        CanBlock(Subject subject, Capability capability) {
            this(subject, capability, null);
        }

        public CanBlock withDuration(Duration duration) {
            return new CanBlock(subject, capability, duration);
        }

        public sealed interface Capability {
            /// "can block any number of [what]." — lifts the single-blocker
            /// restriction (e.g., Palace Guard, Wall of Tears).
            record AnyNumberOf(Selector what) implements Capability {}

            /// "can block an additional [what] each combat" — raises the
            /// block cap by a fixed amount per combat (e.g., Foriysian
            /// Brigade: "can block an additional creature each combat").
            record Additional(Amount count, Selector what) implements Capability {}
        }
    }

    /// "[subject] can't attack or block alone." — may only attack/block
    /// alongside another creature (e.g., Ember Beast).
    record CantAttackOrBlockAlone(Subject subject) implements Effect {}

    /// "[subject] attacks [each combat / each turn | this turn] if able." —
    /// must-attack restriction (e.g., Crazed Goblin, Goblin Diplomats).
    /// Rule 702.38. Duration is null for the static "each combat/turn" form
    /// and set for temporary "this turn" forms.
    record MustAttack(Subject subject, @Nullable Duration duration) implements Effect {
        MustAttack(Subject subject) {
            this(subject, null);
        }

        public MustAttack withDuration(Duration duration) {
            return new MustAttack(subject, duration);
        }
    }

    /// "[subject] attacks or blocks each combat if able." — disjunctive
    /// must-attack-or-block restriction (e.g., Iron Golem, Relentless
    /// Raptor). Satisfied by either attacking or blocking in each combat.
    record MustAttackOrBlock(Subject subject) implements Effect {}

    /// "[subject] can attack as though [they] didn't have [ability]." —
    /// conditional attack-ability override (Rolling Stones: "Wall creatures
    /// can attack as though they didn't have defender."). {@code without}
    /// names the ability whose restriction is ignored.
    record CanAttackAsThoughWithout(Subject subject, String without) implements Effect {}

    /// "[player]'s life total becomes N." — set a player's life to a fixed value.
    record LifeTotalBecomes(Subject player, Amount value) implements Effect {}

    /// "[subject] can't be the target of spells or abilities / of [what]."
    /// When {@code by} is null the restriction covers all spells/abilities.
    record CantBeTargeted(Subject subject, @Nullable Selector by) implements Effect {
        CantBeTargeted(Subject subject) {
            this(subject, null);
        }
    }

    /// "[subject] blocks [target]? [if able] [duration]?." — must-block
    /// requirement on a blocker. The {@code target} is optional; when
    /// present, it names the specific attacker that must be blocked
    /// (Hunt Down: "Target creature blocks target creature this turn if
    /// able.").
    record MustBlock(
            Subject subject,
            @Nullable Subject target,
            @Nullable Duration duration) implements Effect {
        MustBlock(Subject subject) {
            this(subject, null, null);
        }

        MustBlock(Subject subject, @Nullable Duration duration) {
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

    /// "[subject] can't [draw|cast] more than N [cards|spells] each turn." —
    /// a per-turn upper limit on draws or spell casts (e.g., Spirit of the
    /// Labyrinth, Arcane Laboratory). Distinct from {@link CantCast} /
    /// {@link Draw} because it's a limit, not a prohibition.
    record PerTurnLimit(Subject subject, PerTurnLimit.Action action, Amount max) implements Effect {
        public enum Action {
            DRAW_CARDS,
            CAST_SPELLS
        }
    }

    /// Modifies the maximum hand size rule for {@code player}:
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
