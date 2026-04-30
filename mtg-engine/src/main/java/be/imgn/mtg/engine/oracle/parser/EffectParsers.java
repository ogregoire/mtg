package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.AMOUNT;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.CARD_TYPE;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.COLOR;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.COUNTER_TYPE;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.INTEGER;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.NUMBER;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.PLURAL_ZONE_NAME;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.PT_VALUE;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.QUALIFIER;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.SELECTOR;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.SUBTYPE;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.SUPERTYPE;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.ZONE_NAME;
import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.caseInsensitive;
import static com.google.common.labs.parse.Parser.consecutive;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.labs.parse.CharacterSet;
import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.oracle.domain.*;
import be.imgn.mtg.engine.turn.Phase;
import be.imgn.mtg.engine.turn.Step;

/// Parsers for effect productions in oracle text.
final class EffectParsers {
    private EffectParsers() {}

    /// Forward-declared rule for a multi-effect clause. Populated via
    /// `.definedAs(...)` in the trailing `static {}` block. Declared
    /// as a `Parser.Rule` so [OracleParser#EFFECT_SEQUENCE] can
    /// reference `EffectParsers.CLAUSE` at class-init time without
    /// hitting the circular init between this class and
    /// [OracleParser] (one direction of which is already broken by
    /// [OracleParser#ABILITY] being a `Parser.Rule`).
    public static final Parser.Rule<List<Effect>> CLAUSE = new Parser.Rule<>();

    // ── Shared primitives ──────────────────────────────────────────────

    static final Parser<ManaSymbol> MANA_SYMBOL = string("{")
            .then(consecutive(CharPredicate.noneOf(" {}"), "mana content"))
            .followedBy(string("}"))
            .map(s -> new ManaSymbol("{" + s + "}"));

    /// Word-or-contraction token (e.g., "it's") used when capturing free
    /// predicate text — broader than [Parser#word()] so English
    /// contractions like "it's" and "isn't" round-trip.
    static final Parser<String> WORD_OR_CONTRACTION = consecutive(CharacterSet.charsIn("[A-Za-z0-9'-]"), "word");

    /// Predicate-token parser for `as long as` clauses — like
    /// [#WORD_OR_CONTRACTION] plus "+/-" and "/" so P/T markers
    /// ("+1/+1") and signed values round-trip inside the free-text
    /// predicate (Lightwalker: "as long as it has a +1/+1 counter on it.").
    /// Hyphen placed first inside the character class so it is unambiguously
    /// literal (some character-class implementations treat `-` between
    /// characters as a range delimiter). Accepts `-1/-1` and similar
    /// signed P/T markers inside the predicate (Tenacious Hunter:
    /// "as long as a creature has a -1/-1 counter on it").
    private static final Parser<String> AS_LONG_AS_TOKEN =
            consecutive(CharacterSet.charsIn("[-A-Za-z0-9'+/]"), "as-long-as word");

    /// "[for]? as long as [condition]" — [Duration.ForAsLongAs]
    /// captured as free text (allows English contractions such as
    /// "it's"). The optional "for" prefix appears when the clause is
    /// a trailing duration on a verb (Rootwater Matriarch: "Gain
    /// control of target creature for as long as that creature is
    /// enchanted.") rather than a sentence-starting condition.
    private static final Parser<Duration> AS_LONG_AS = anyOf(phrase("As long as"), phrase("for as long as"))
            // Tokens may carry a trailing comma (Angelic Voices: "as long
            // as you control no nonartifact, nonwhite creatures") — the
            // comma is preserved in the predicate text since the clause
            // is still free-text fallback, not structured.
            .then(AS_LONG_AS_TOKEN
                    .optionallyFollowedBy(string(","), (w, _) -> w + ",")
                    .atLeastOnce()
                    .map(words -> String.join(" ", words)))
            .map(Duration::forAsLongAs);

    /// Owner for "until \[owner\]'s next \[step\]" — a possessive subject
    /// ("its controller") or a possessive pronoun ("your", "their").
    private static final Parser<Subject> UNTIL_NEXT_STEP_OWNER = anyOf(
            SubjectParsers.POSSESSIVE.followedBy(string("'s")),
            word("your").thenReturn(Subject.player(PlayerRef.Pronoun.YOU)),
            word("their").thenReturn(Subject.player(PlayerRef.Pronoun.THEY)));

    /// "until \[owner\]'s next \[step-name\] step" — anchored duration that
    /// ends at the next occurrence of a specific step belonging to a
    /// specific player (Orcish Farmer: "until its controller's next
    /// untap step").
    private static final Parser<Duration> UNTIL_NEXT_STEP = sequence(
            phrase("until").then(UNTIL_NEXT_STEP_OWNER),
            word("next").then(TriggerEventParsers.STEP_NAME),
            (owner, step) -> (Duration) new Duration.UntilNextStep(owner, step));

    /// "during \[owner\]'s next turn" — a player-scoped duration
    /// (Sphinx's Decree: "Each opponent can't cast instant or
    /// sorcery spells during that player's next turn."). Distinct
    /// from [Duration.Fixed#UNTIL_YOUR_NEXT_TURN], which is a
    /// suffix "until the controller's next turn" form.
    /// Possessive owner for [#DURING_NEXT_TURN] / [#DURING_STEP] — the
    /// player whose turn or step the duration scopes to.
    private static final Parser<PlayerRef> DURING_OWNER = anyOf(
            phrase("target player's").thenReturn(PlayerRef.targetPlayer()),
            phrase("target opponent's").thenReturn(PlayerRef.targetOpponent()),
            phrase("that player's").thenReturn(PlayerRef.Pronoun.THAT_PLAYER),
            phrase("that opponent's").thenReturn(PlayerRef.Pronoun.THAT_OPPONENT),
            phrase("your").thenReturn(PlayerRef.Pronoun.YOU),
            phrase("their").thenReturn(PlayerRef.Pronoun.THEY),
            phrase("an opponent's").thenReturn(PlayerRef.Pronoun.AN_OPPONENT),
            phrase("each opponent's").thenReturn(PlayerRef.Pronoun.EACH_OPPONENT));

    private static final Parser<Duration.DuringNextTurn> DURING_NEXT_TURN =
            phrase("During").then(DURING_OWNER).followedBy(phrase("next turn")).map(Duration.DuringNextTurn::new);

    /// "During \[owner\]'s \[step\]" — recurring step-scoped duration
    /// (Final-Word Phantom: "During each opponent's end step, you may
    /// cast spells as though they had flash.").
    private static final Parser<Duration.DuringStep> DURING_STEP =
            sequence(phrase("During").then(DURING_OWNER), TriggerEventParsers.STEP_NAME, Duration.DuringStep::new);

    static final Parser<Duration> DURATION = anyOf(
            phrase("Until end of turn").thenReturn(Duration.Fixed.UNTIL_END_OF_TURN),
            phrase("Until the end of your next turn").thenReturn(Duration.Fixed.UNTIL_END_OF_YOUR_NEXT_TURN),
            phrase("Until your next turn").thenReturn(Duration.Fixed.UNTIL_YOUR_NEXT_TURN),
            phrase("Until end of combat").thenReturn(Duration.Fixed.UNTIL_END_OF_COMBAT),
            UNTIL_NEXT_STEP,
            phrase("This turn").thenReturn(Duration.Fixed.THIS_TURN),
            phrase("This combat").thenReturn(Duration.Fixed.THIS_COMBAT),
            phrase("On each of your turns").thenReturn(Duration.Fixed.EACH_YOUR_TURN),
            phrase("During combat").thenReturn(Duration.Fixed.DURING_COMBAT),
            phrase("during your turn").thenReturn(Duration.Fixed.DURING_YOUR_TURN),
            phrase("during turns other than yours").thenReturn(Duration.Fixed.DURING_OTHERS_TURN),
            DURING_NEXT_TURN, // must precede DURING_STEP ("next turn" longer match)
            DURING_STEP,
            AS_LONG_AS);

    private static final Parser<String> KEYWORD_NAME = anyOf(
            phrase("First strike"),
            phrase("Double strike"),
            phrase("Death touch").thenReturn("deathtouch"),
            phrase("Flying"),
            phrase("Trample"),
            phrase("Haste"),
            phrase("Vigilance"),
            phrase("Lifelink"),
            phrase("Deathtouch"),
            phrase("Hexproof"),
            phrase("Indestructible"),
            phrase("Menace"),
            phrase("Reach"),
            phrase("Defender"),
            phrase("Flash"),
            phrase("Fear"),
            phrase("Intimidate"),
            phrase("Shroud"),
            phrase("Wither"),
            phrase("Infect"),
            phrase("Prowess"),
            word().suchThat(k -> k.length() > 2 && Character.isLowerCase(k.charAt(0)), "keyword name"));

    private static final Parser<List<String>> KEYWORD_LIST = KEYWORD_NAME.atLeastOnceDelimitedBy(",");

    // ── Effects ────────────────────────────────────────────────────────

    /// The implicit "you" subject — used when an effect omits the player
    /// (e.g., "Draw a card." = "you draw a card.").
    private static final Subject YOU = Subject.player(PlayerRef.Pronoun.YOU);

    /// "Switch \[subject\]'s power and toughness \[duration\]?." / "Switch
    /// \[its|their\] power and toughness \[duration\]?." — swap P/T. About
    /// Face uses the possessive form; the pronoun form appears in
    /// triggered abilities where the subject is already bound by the
    /// trigger (Valakut Fireboar: "Whenever this creature attacks, switch
    /// its power and toughness until end of turn.").
    static final Parser<Effect.SwitchPT> SWITCH_PT = anyOf(
                    phrase("Switch")
                            .then(SubjectParsers.SUBJECT)
                            .followedBy(string("'s"))
                            .followedBy(phrase("power and toughness"))
                            .map(Effect.SwitchPT::new),
                    phrase("Switch [its|their] power and toughness")
                            .thenReturn(new Effect.SwitchPT(Subject.pronoun(PronounType.IT))))
            .optionallyFollowedBy(DURATION, Effect.SwitchPT::withDuration);

    /// "[subject] crews [selector] as though its power were N greater." —
    /// Hotshot Mechanic. The power delta is captured as a plain integer.
    static final Parser<Effect.CrewsWithBoostedPower> CREWS_WITH_BOOSTED_POWER = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("crew(s)")),
            SELECTOR.followedBy(phrase("as though its power were")),
            INTEGER.followedBy(word("greater")),
            Effect.CrewsWithBoostedPower::new);

    /// "Roll the planar die." — Planechase effect (Fractured Powerstone).
    static final Parser<Effect.RollPlanarDie> ROLL_PLANAR_DIE =
            phrase("Roll the planar die").thenReturn(Effect.RollPlanarDie.ROLL_PLANAR_DIE);

    // ROLL_DIE is declared lower in this file (after BASE_EFFECT) since
    // its outcome-table body references BASE_EFFECT.

    /// "Move [N|all] [type]? counter(s) from [source] onto [dest]." —
    /// Fate Transfer, Power Conduit. Both the count (integer word or
    /// "all") and the type are optional.
    /// "[player] may activate [kind] abilities any time [player] could
    /// cast [an instant|a sorcery]." — Leonin Shikari: "You may activate
    /// equip abilities any time you could cast an instant." The
    /// instant/sorcery speed is preserved since the two produce very
    /// different timing permissions.
    static final Parser<Effect.MayActivateAnyTime> MAY_ACTIVATE_ANY_TIME = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("may activate")),
            anyOf(
                            word("equip").thenReturn(Effect.MayActivateAnyTime.Kind.EQUIP),
                            word("loyalty").thenReturn(Effect.MayActivateAnyTime.Kind.LOYALTY))
                    .followedBy(word("abilities")),
            phrase("any time")
                    .then(SubjectParsers.PLAYER_SUBJECT)
                    .then(phrase("could cast"))
                    .then(anyOf(
                            phrase("an instant").thenReturn(Effect.MayActivateAnyTime.Speed.INSTANT),
                            phrase("a sorcery").thenReturn(Effect.MayActivateAnyTime.Speed.SORCERY))),
            Effect.MayActivateAnyTime::new);

    static final Parser<Effect.MoveCounters> MOVE_COUNTERS = sequence(
            phrase("Move").then(anyOf(word("all").thenReturn(Amount.All.ALL), AMOUNT)),
            anyOf(
                            COUNTER_TYPE.followedBy(phrase("counter(s)")),
                            phrase("counter(s)").thenReturn((CounterType) null))
                    .followedBy(word("from")),
            SubjectParsers.SUBJECT.followedBy(word("onto")),
            SubjectParsers.SUBJECT,
            Effect.MoveCounters::new);

    /// "Double the amount of each type of unspent mana [player] has." —
    /// Doubling Cube / Mana Reflection.
    static final Parser<Effect.DoubleMana> DOUBLE_MANA = phrase("Double the amount of each type of unspent mana")
            .then(SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("[has|have]")))
            .map(Effect.DoubleMana::new);

    /// "Flip [N] coin[s] [and ignore one]?" — Krark's Thumb replacement
    /// body. Captures the count and the ignore tail for later resolution.
    static final Parser<Effect.FlipCoins> FLIP_COINS = phrase("Flip")
            .then(AMOUNT)
            .followedBy(phrase("coin(s)"))
            .<Effect.FlipCoins>map(Effect.FlipCoins::new)
            .optionallyFollowedBy(phrase("and ignore").then(NUMBER), (fc, n) -> new Effect.FlipCoins(fc.count(), n));

    /// "Attach [what] to [target]." — move an Aura/Equipment (Aura
    /// Finesse: "Attach target Aura you control to target creature.").
    static final Parser<Effect.Attach> ATTACH = sequence(
            phrase("Attach").then(SubjectParsers.SUBJECT),
            phrase("To").then(SubjectParsers.SUBJECT),
            Effect.Attach::new);

    /// "[player] <verb-body>" — a subject-less player-actor verb body,
    /// rebound to the player captured by [#PLAYER_ACTOR_AND_CHAIN].
    /// Each arm is the same body the bare (YOU-defaulted) parsers use,
    /// just with the actor plumbed in.
    private static Parser<Effect> playerVerbBody(Subject actor) {
        return Parser.<Effect>anyOf(
                // "reveals a card at random from [poss] hand" — must precede the
                // plain REVEAL_NO_PLAYER so the longer at-random phrase wins.
                CardManipulationEffectParsers.REVEAL_AT_RANDOM_NO_PLAYER.map(
                        what -> new Effect.Reveal(actor, what).withAtRandom()),
                // "reveals their hand" — sub-hand reveal; a plain SUBJECT
                // wouldn't match "their hand" since it isn't a card-level
                // selector.
                CardManipulationEffectParsers.REVEAL_NO_PLAYER.map(what -> new Effect.Reveal(actor, what)),
                DamageEffectParsers.LOSE_LIFE_NO_PLAYER.map(amt -> new Effect.LoseLife(actor, amt)),
                DamageEffectParsers.GAIN_LIFE_NO_PLAYER.map(amt -> new Effect.GainLife(actor, amt)),
                CardManipulationEffectParsers.DRAW_NO_PLAYER.map(amt -> new Effect.Draw(actor, amt)),
                CardManipulationEffectParsers.DISCARD_NO_PLAYER.map(d -> new Effect.Discard(actor, d)));
    }

    /// "{E}..." — one or more energy symbols; returns the count. Used
    /// by [#GAIN_ENERGY] and inline inside [#PLAYER_VERB_BODY].
    private static final Parser<Integer> ENERGY_SYMBOLS =
            string("{E}").atLeastOnce().map(List::size);

    /// "[player] <action1> and <action2> [and <action3>]" — a shared
    /// player subject distributed over two or more subject-less verb
    /// bodies joined by "and" (Trapfinder's Trick: "Target player reveals
    /// their hand and discards all Trap cards."; Thoughtcutter Agent:
    /// "Target player loses 1 life and reveals their hand."). The
    /// single-action form is excluded — those go through the bare verb
    /// parsers — so `suchThat` requires at least two bodies.
    /// A subject-less verb body that doesn't know yet which player performs
    /// it — the actor is plumbed in later by [#PLAYER_ACTOR_AND_CHAIN].
    private static final Parser<Function<Subject, Effect>> PLAYER_VERB_BODY = Parser.<Function<Subject, Effect>>anyOf(
            // "reveals a card at random from [poss] hand" — must precede plain
            // REVEAL_NO_PLAYER so the longer phrase wins (Hired Torturer).
            CardManipulationEffectParsers.REVEAL_AT_RANDOM_NO_PLAYER.map(
                    what -> actor -> new Effect.Reveal(actor, what).withAtRandom()),
            CardManipulationEffectParsers.REVEAL_NO_PLAYER.map(what -> actor -> new Effect.Reveal(actor, what)),
            // Inline "loses N life" without LOSE_LIFE_NO_PLAYER's optional
            // CountOfParsers.FOR_EACH tail — which can swallow "and <verb>" via its
            // trailing subject parser.
            phrase("lose(s)")
                    .then(AMOUNT)
                    .followedBy(word("life"))
                    .map(amt -> actor -> new Effect.LoseLife(actor, amt)),
            phrase("gain(s)")
                    .then(AMOUNT)
                    .followedBy(word("life"))
                    .optionallyFollowedBy(
                            CountOfParsers.FOR_EACH,
                            (base, scale) -> base instanceof Amount.Exact e ? new Amount.Times(e.value(), scale) : base)
                    .map(amt -> actor -> new Effect.GainLife(actor, amt)),
            CardManipulationEffectParsers.DRAW_NO_PLAYER.map(amt -> actor -> new Effect.Draw(actor, amt)),
            CardManipulationEffectParsers.DISCARD_NO_PLAYER.map(d -> actor -> new Effect.Discard(actor, d)),
            CardManipulationEffectParsers.MILL_NO_PLAYER.map(amt -> actor -> new Effect.Mill(actor, amt)),
            // "exchange life totals" — binary life-swap (Axis of
            // Mortality: "you may have two target players exchange
            // life totals."). Actor (typically plural players) is
            // the subject of the ExchangeLifeTotals record.
            phrase("exchange life totals").<Function<Subject, Effect>>thenReturn(Effect.ExchangeLifeTotals::new),
            // "get {E}..." — energy counter gain (Live Fast).
            phrase("get(s)").then(ENERGY_SYMBOLS).map(n -> actor -> new Effect.GainEnergy(actor, n)),
            // "get(s) N <type> counter(s)" — non-energy player
            // counter gain (Caress of Phyrexia: "Target player draws
            // three cards, loses 3 life, and gets three poison
            // counters.").
            sequence(phrase("get(s)").then(AMOUNT), COUNTER_TYPE.followedBy(phrase("counter(s)")), (amt, type) ->
                    (Function<Subject, Effect>) actor -> new Effect.AddCounters(amt, type, actor)),
            // "create(s) [N] [tapped]? [token]" — player-scoped token
            // creation (Seed the Land: "its controller creates a
            // 1/1 green Snake creature token."). The actor becomes
            // the token's creator.
            anyOf(
                            sequence(
                                    phrase("create(s)").then(AMOUNT).followedBy(word("tapped")),
                                    TokenDescriptionParsers.TOKEN_DESCRIPTION,
                                    (amt, td) -> (Function<Subject, Effect>)
                                            actor -> new Effect.CreateToken(amt, td).withCreator(actor)),
                            sequence(
                                    phrase("create(s)").then(AMOUNT),
                                    TokenDescriptionParsers.TOKEN_DESCRIPTION,
                                    (amt, td) -> (Function<Subject, Effect>)
                                            actor -> new Effect.CreateToken(amt, td).withCreator(actor)))
                    .map(fn -> fn),
            // "put(s) N [type] counter(s) on [subject]" — player-actor
            // counter placement (Hunted Nightmare: "target opponent puts
            // a deathtouch counter on a creature they control."). Must
            // precede the zone-move "puts" arms so the longer phrase
            // "counter(s) on" wins over "puts <subject> <destination>".
            phrase("put(s)")
                    .then(CounterEffectParsers.PUT_COUNTER_BODY)
                    .<Function<Subject, Effect>>map(ac -> _actor -> ac),
            // "puts <subject> <from> <destination> [in any order]?" —
            // player-actor zone move with an explicit source zone (Prying
            // Questions: "Target opponent … puts a card from their
            // hand on top of their library."; Stunted Growth: "puts them
            // on top of their library in any order."). Actor is consumed
            // as flavor since Effect.ZoneMove doesn't carry one.
            sequence(
                            phrase("put(s)").then(SubjectParsers.SUBJECT),
                            ZoneExpressionParsers.IN_ZONE_FROM.<Zone.Source>map(Zone.Source::fromZone),
                            ZoneParsers.ZONE_DESTINATION,
                            Effect.ZoneMove::new)
                    .optionallyFollowedBy(phrase("in any order"), (zm, _) -> zm.inAnyOrder())
                    .<Function<Subject, Effect>>map(zm -> _actor -> zm),
            // "puts <subject> <destination> [in any order]?" — no explicit
            // source (Stunted Growth second verb body when subject carries
            // the zone; e.g., "puts them on top of their library in any
            // order.").
            sequence(phrase("put(s)").then(SubjectParsers.SUBJECT), ZoneParsers.ZONE_DESTINATION, Effect.ZoneMove::new)
                    .optionallyFollowedBy(phrase("in any order"), (zm, _) -> zm.inAnyOrder())
                    .<Function<Subject, Effect>>map(zm -> _actor -> zm),
            // "choose(s) <selector> from <zone>" — player-actor choice with
            // an explicit source zone (Stunted Growth: "Target player
            // chooses three cards from their hand and puts them on top of
            // their library in any order."). The zone is folded into the
            // selector's zone field. Must precede the bare "choose(s)
            // <subject>" arm so the longer "from <zone>" phrase wins.
            sequence(
                    phrase("choose(s)").then(SELECTOR),
                    ZoneExpressionParsers.IN_ZONE_FROM,
                    (sel, zone) -> (Function<Subject, Effect>)
                            actor -> new Effect.Choose(Subject.select(sel.withZone(zone))).withChooser(actor)),
            // "choose(s) <selector>" — player-actor choice (Watchers of
            // the Dead: "Each opponent chooses two cards in their
            // graveyard and exiles the rest.").
            phrase("choose(s)")
                    .then(SubjectParsers.SUBJECT)
                    .<Function<Subject, Effect>>map(what -> actor -> new Effect.Choose(what).withChooser(actor)),
            // "exile(s) <subject>" — player-actor exile (Watchers of
            // the Dead's "exiles the rest").
            phrase("exile(s)")
                    .then(SubjectParsers.SUBJECT)
                    .<Function<Subject, Effect>>map(
                            what -> actor -> new Effect.Exile(new Exiled.Objects(what)).withActor(actor)),
            // "sacrifice(s) <subject> [of their choice]?" — player-actor
            // sacrifice (Predatory Nightstalker: "you may have target
            // opponent sacrifice a creature of their choice."). The
            // "of their choice" tail is consumed as flavor since
            // [Effect.Sacrifice] doesn't carry a chooser slot.
            phrase("sacrifice(s)")
                    .then(SubjectParsers.SUBJECT)
                    .optionallyFollowedBy(phrase("of [their|its|his|her] choice"), (s, _) -> s)
                    .<Function<Subject, Effect>>map(what -> actor -> new Effect.Sacrifice(actor, what)));

    /// Binds an X definition onto every effect in a shared-actor chain
    /// that carries an xDefinition slot (Monumental Corruption,
    /// Lucid Dreams). Effects without one pass through unchanged.
    private static List<Effect> bindXDefinition(List<Effect> effects, Amount xDefinition) {
        return effects.stream()
                .map(e -> switch (e) {
                    case Effect.Draw d -> (Effect) d.withXDefinition(xDefinition);
                    case Effect.LoseLife ll -> ll.withXDefinition(xDefinition);
                    case Effect.GainLife gl -> gl.withXDefinition(xDefinition);
                    case Effect.Mill m -> m.withXDefinition(xDefinition);
                    case Effect.AddCounters ac -> ac.withXDefinition(xDefinition);
                    default -> e;
                })
                .toList();
    }

    /// "[player] <action1>, <action2>, and <actionN>" — a shared player
    /// actor distributed across an Oxford-comma-delimited list of
    /// subject-less verb bodies (Trapfinder's Trick: "[player] X and Y.";
    /// Live Fast: "You draw two cards, lose 2 life, and get {E}{E}.").
    /// Returns the pair as a `List<Effect>` so [OracleParser#EFFECT_SEQUENCE] flattens it into the surrounding
    /// list — the chain is a syntactic clause that produces multiple
    /// effects, not a single compound one.
    static final Parser<List<Effect>> PLAYER_ACTOR_AND_CHAIN = sequence(
                    SubjectParsers.PLAYER_SUBJECTS,
                    MtgParsers.andOrThenList(PLAYER_VERB_BODY)
                            .suchThat(list -> list.size() >= 2, "at least two verb bodies"),
                    (actor, bodies) ->
                            bodies.stream().map(fn -> fn.apply(actor)).toList())
            // Trailing ", where X is <def>" — binds X across every
            // effect in the chain that carries an xDefinition slot
            // (Monumental Corruption: "Target player draws X cards and
            // loses X life, where X is the number of artifacts you
            // control.").
            .optionallyFollowedBy(CountOfParsers.WHERE_X_IS, EffectParsers::bindXDefinition);

    /// Tail of a "[player]? play with the top card of [poss] library
    /// revealed" phrase — consumes the verb and its body, leaving only
    /// the optional leading subject for the outer parser.
    private static final Parser<String> PLAY_WITH_TOP_REVEALED_TAIL =
            phrase("Play(s)").followedBy(phrase("with the top card of [your|their|its] [libraries|library] revealed"));

    /// "[player]? play[s] with the top card of [their] library revealed."
    /// — e.g., Goblin Spy, Future Sight, Field of Dreams (plural
    /// "libraries"). The player defaults to "you" when omitted.
    static final Parser<Effect.PlayWithTopRevealed> PLAY_WITH_TOP_REVEALED = anyOf(
            sequence(
                    SubjectParsers.PLAYER_SUBJECT,
                    PLAY_WITH_TOP_REVEALED_TAIL,
                    (subj, _) -> new Effect.PlayWithTopRevealed(subj)),
            PLAY_WITH_TOP_REVEALED_TAIL.thenReturn(new Effect.PlayWithTopRevealed(YOU)));

    /// Capability tail for "[subject] can block …". Tried arm-by-arm;
    /// the five variants of [Effect.CanBlock.Capability] each carry
    /// their own oracle shape.
    ///
    /// Arm order: the "as though \[state\]" form comes first so its
    /// leading "as though" isn't mis-eaten by
    /// [#CAN_BLOCK_AS_THOUGH_HAD] (which expects a selector in
    /// between); then the longer structural arms; "only" last since
    /// its body is a plain selector that could otherwise shadow the
    /// "as though" forms.
    private static final Parser<Effect.CanBlock.Capability> CAN_BLOCK_CAPABILITY = anyOf(
            // "as though \[state\]" — Masako the Humorless.
            phrase("as though")
                    .then(word().atLeastOnce().map(ws -> String.join(" ", ws)))
                    .<Effect.CanBlock.Capability>map(Effect.CanBlock.Capability.AsThoughState::new),
            // "any number of \[selector\]" — Palace Guard.
            phrase("any number of")
                    .then(SELECTOR)
                    .<Effect.CanBlock.Capability>map(Effect.CanBlock.Capability.AnyNumberOf::new),
            // "\[amount\] additional \[selector\] \[each combat\]?" —
            // Foriysian Brigade / Coastline Chimera. "each combat"
            // is flavor since Additional implies per-combat.
            sequence(
                            anyOf(phrase("[an|a]").thenReturn(Amount.exact(1)), AMOUNT)
                                    .followedBy(word("additional")),
                            SELECTOR,
                            (count, what) ->
                                    (Effect.CanBlock.Capability) new Effect.CanBlock.Capability.Additional(count, what))
                    .optionallyFollowedBy(phrase("each combat"), (c, _) -> c),
            // "\[selector\] as though it had \[keyword\]" — Heartwood
            // Dryad, Foriysian Interceptor.
            sequence(SELECTOR.followedBy(phrase("as though it had")), KeywordParsers.SIMPLE, (what, kw) ->
                    (Effect.CanBlock.Capability) new Effect.CanBlock.Capability.AsThoughHad(what, kw)),
            // "only \[selector\]" — Gloomwidow: "can block only creatures
            // with flying."
            word("only").then(SELECTOR).<Effect.CanBlock.Capability>map(Effect.CanBlock.Capability.Only::new));

    /// "[subject] can block \[capability\] \[duration\]?" — unified
    /// positive block-capability expansion. Covers the five oracle
    /// shapes captured by [Effect.CanBlock.Capability] variants
    /// (AnyNumberOf, Additional, AsThoughHad, AsThoughState, Only).
    static final Parser<Effect.CanBlock> CAN_BLOCK = Parser.sequence(
                    SubjectParsers.SUBJECT.followedBy(phrase("can block")), CAN_BLOCK_CAPABILITY, Effect.CanBlock::new)
            .optionallyFollowedBy(DURATION, Effect.CanBlock::withDuration);

    // Counterspell

    /// A condition-clause token — like a word but also accepts mana symbols
    /// (`{X}`, `{2}{R}`) and apostrophes so predicates such as "its
    /// controller pays {X}" round-trip verbatim. `~` admits self-name
    /// references (Lava Blister: "unless its controller has ~ deal 6
    /// damage to them.").
    private static final Parser<String> CONDITION_TOKEN =
            consecutive(CharacterSet.charsIn("[A-Za-z0-9'{}+/~-]"), "condition token");

    /// Single cost body following an already-consumed "pay\[s\]" verb
    /// — a mana payment or a life payment. Distinct from
    /// [CostParsers#COST_EXPRESSION] in that the verb has already been
    /// consumed by the surrounding clause.
    private static final Parser<Cost> POST_PAY_COMPONENT = anyOf(
            sequence(AMOUNT, word("life"), (amt, _) -> (Cost) new Cost.PayLife(amt)),
            MANA_SYMBOL.atLeastOnce().<Cost>map(Cost.Mana::new));

    /// One or more [#POST_PAY_COMPONENT]s joined by "and" — handles
    /// compound payments like Mundungu's "{1} and 1 life".
    private static final Parser<Cost> POST_PAY_COMPOUND = POST_PAY_COMPONENT
            .atLeastOnceDelimitedBy(phrase("and"), Collectors.toUnmodifiableList())
            .map(parts -> parts.size() == 1 ? parts.getFirst() : new Cost.AllOf(parts));

    /// One or more [#POST_PAY_COMPOUND]s joined by "or" — handles
    /// alternative payments like Thrull Wizard's "{B} or {3}".
    private static final Parser<Cost> POST_PAY_COST = POST_PAY_COMPOUND
            .atLeastOnceDelimitedBy(phrase("or"), Collectors.toUnmodifiableList())
            .map(opts -> opts.size() == 1 ? opts.getFirst() : new Cost.AnyOf(opts));

    /// "plus an additional \<mana\> for each \<scope\>" — the per-each
    /// increment extension on a payment condition (Spell Stutter:
    /// "pays {2} plus an additional {1} for each Faerie you control").
    /// Produces a pair of (additionalPerEach cost, scaleBy amount).
    private static final Parser<Map.Entry<Cost.Mana, Amount.CountOf>> PLUS_ADDITIONAL_FOR_EACH = sequence(
            phrase("plus an additional").then(MANA_SYMBOL.atLeastOnce()).map(Cost.Mana::new),
            CountOfParsers.FOR_EACH,
            Map::entry);

    /// "\[unless\|if\] \[player\] pay\[s\] \<cost\> \[plus an additional \<incr\>\]?
    /// \[for each \<scope\>\]?"
    /// — typed payment-gate condition (Clash of Wills, Mana Leak,
    /// Tyrannize, Qal Sisma Behemoth, Thrull Wizard's "or"-cost,
    /// Oppressive Will / Override "for each" multiplier,
    /// Spell Stutter "plus an additional {1} for each" base+increment form).
    static final Parser<Condition> PLAYER_PAYS_CONDITION = sequence(
                    anyOf(
                            phrase("Unless").thenReturn(Condition.Kind.UNLESS),
                            phrase("If").thenReturn(Condition.Kind.IF)),
                    SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("pay(s)")),
                    POST_PAY_COST,
                    (kind, who, cost) -> new Condition.PlayerPays(kind, who, cost))
            // "plus an additional {X} for each <scope>" — must precede plain
            // FOR_EACH since both phrases end in "for each"; this one has the
            // longer prefix and must win.
            .optionallyFollowedBy(
                    PLUS_ADDITIONAL_FOR_EACH, (c, entry) -> c.withAdditionalPerEach(entry.getKey(), entry.getValue()))
            .optionallyFollowedBy(
                    CountOfParsers.FOR_EACH,
                    (c, scaleBy) -> new Condition.PlayerPays(c.kind(), c.who(), c.cost(), scaleBy))
            .map(c -> (Condition) c);

    /// "\[unless\|if\] \[player\] [doesn't|don't]? control\[s\] \<subject\>"
    /// — typed possession-gate condition (Mindless Null, Desperate
    /// Castaways, War Falcon's "a Knight or a Soldier", Scourge of
    /// Numai's "if you don't control an Ogre"). Uses [SubjectParsers#SUBJECT]
    /// rather than [SelectorParsers#SELECTOR] so the "a X or a Y"
    /// or-conjunction collapses into [Subject.OneOf] naturally.
    static final Parser<Condition> PLAYER_CONTROLS_CONDITION = anyOf(
            // past tense: "if you controlled that permanent" — must precede present-tense arm
            sequence(
                    anyOf(
                            phrase("Unless").thenReturn(Condition.Kind.UNLESS),
                            phrase("If").thenReturn(Condition.Kind.IF),
                            phrase("while").thenReturn(Condition.Kind.AS_LONG_AS),
                            phrase("As long as").thenReturn(Condition.Kind.AS_LONG_AS)),
                    SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("controlled")),
                    SubjectParsers.SUBJECT,
                    (kind, who, what) -> (Condition) new Condition.PlayerControls(kind, who, false, true, what)),
            sequence(
                    anyOf(
                            phrase("Unless").thenReturn(Condition.Kind.UNLESS),
                            phrase("If").thenReturn(Condition.Kind.IF),
                            phrase("while").thenReturn(Condition.Kind.AS_LONG_AS),
                            phrase("As long as").thenReturn(Condition.Kind.AS_LONG_AS)),
                    sequence(
                            SubjectParsers.PLAYER_LIKE_SUBJECT,
                            anyOf(
                                    phrase("[doesn't|don't] control").thenReturn(true),
                                    phrase("control(s)").thenReturn(false)),
                            Map::entry),
                    SubjectParsers.SUBJECT,
                    (kind, wn, what) ->
                            (Condition) new Condition.PlayerControls(kind, wn.getKey(), wn.getValue(), what)));

    /// Shared structural-comparator parser used by every count-
    /// comparison condition (HasLife, HasOpponents, CountOf,
    /// CardsInHand). Returns an [AmountMatcher] that bundles the
    /// comparator and the bound value into one structural predicate.
    /// Uses [AmountParsers#NUMBER] (a plain integer / word-number)
    /// rather than the polymorphic [#AMOUNT] so AMOUNT's own "N or
    /// more" / range arms don't greedily eat the comparator suffix
    /// before the outer sequence sees it.
    static final Parser<AmountMatcher> AMOUNT_MATCHER = AmountParsers.AMOUNT_MATCHER;

    /// "\[unless\|if\] \[player\] ha\[s\|ve\] \<count\> card\[s\] in hand"
    /// — hand-size gate (Idle Thoughts: "if you have no cards in
    /// hand."; Imaginary Pet: "if you have a card in hand"). The
    /// matcher folds "no" → `Exactly(exact(0))` and "a"/"an" →
    /// `AtLeast(exact(1))` along with the usual "N or more" / "N or
    /// less" / "exactly N" forms.
    static final Parser<Condition> CARDS_IN_HAND_CONDITION = sequence(
            anyOf(
                    phrase("Unless").thenReturn(Condition.Kind.UNLESS),
                    phrase("If").thenReturn(Condition.Kind.IF)),
            SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("[has|have]")),
            anyOf(
                    phrase("[a|an]")
                            .followedBy(phrase("card in hand"))
                            .<AmountMatcher>thenReturn(new AmountMatcher.AtLeast(Amount.exact(1))),
                    AMOUNT_MATCHER.followedBy(phrase("card(s) in hand"))),
            (kind, who, m) -> (Condition) new Condition.CardsInHand(kind, who, m));

    /// "\[unless\|if\] \[player\] [has|have] \<count\> cards in
    /// [their|your] library" — library-size gate (Battle of Wits:
    /// "if you have 200 or more cards in your library").
    static final Parser<Condition> CARDS_IN_LIBRARY_CONDITION = sequence(
            anyOf(
                    phrase("Unless").thenReturn(Condition.Kind.UNLESS),
                    phrase("If").thenReturn(Condition.Kind.IF)),
            SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("[has|have]")),
            AMOUNT_MATCHER.followedBy(phrase("card(s) in [your|their] library")),
            (kind, who, m) -> (Condition) new Condition.CardsInLibrary(kind, who, m));

    /// "\[unless\|if\] a \<zone\> has \<amount\> cards in it" —
    /// existential count over any single zone instance (Visions of
    /// Beyond: "If a graveyard has twenty or more cards in it, draw
    /// three cards instead.").
    static final Parser<Condition> ANY_ZONE_HAS_CARDS_CONDITION = sequence(
            anyOf(
                    phrase("Unless").thenReturn(Condition.Kind.UNLESS),
                    phrase("If").thenReturn(Condition.Kind.IF)),
            phrase("a").then(SelectorParsers.ZONE_NAME).followedBy(phrase("has")),
            AMOUNT_MATCHER.followedBy(phrase("card(s) in it")),
            (kind, zone, m) -> (Condition) new Condition.AnyZoneHasCards(kind, zone, m));

    /// "\[unless\|if\] \<self\> was kicked" — kicker-status gate
    /// (Ertai's Trickery).
    static final Parser<Condition> WAS_KICKED_CONDITION = sequence(
            anyOf(
                    phrase("Unless").thenReturn(Condition.Kind.UNLESS),
                    phrase("If").thenReturn(Condition.Kind.IF)),
            SubjectParsers.SUBJECT.followedBy(phrase("was kicked")),
            (kind, what) -> (Condition) new Condition.WasKicked(kind, what));

    /// "\[unless\|if\] \<self\> is equipped" — equipped-state gate
    /// (Training Drone).
    static final Parser<Condition> IS_EQUIPPED_CONDITION = sequence(
            anyOf(
                    phrase("Unless").thenReturn(Condition.Kind.UNLESS),
                    phrase("If").thenReturn(Condition.Kind.IF)),
            SubjectParsers.SUBJECT.followedBy(phrase("[is|'s] equipped")),
            (kind, what) -> (Condition) new Condition.IsEquipped(kind, what));

    /// "\[unless\|if\] \<self\> is enchanted" — enchanted-state gate
    /// (Krond the Dawn-Clad).
    static final Parser<Condition> IS_ENCHANTED_CONDITION = sequence(
            anyOf(
                    phrase("Unless").thenReturn(Condition.Kind.UNLESS),
                    phrase("If").thenReturn(Condition.Kind.IF),
                    phrase("As long as").thenReturn(Condition.Kind.AS_LONG_AS)),
            SubjectParsers.SUBJECT.followedBy(phrase("[is|'s] enchanted")),
            (kind, what) -> (Condition) new Condition.IsEnchanted(kind, what));

    /// "\[unless\|if\] \<self\> is paired with \<selector\>" —
    /// soulbond pairing gate (Flowering Lumberknot).
    static final Parser<Condition> IS_PAIRED_WITH_CONDITION = sequence(
            anyOf(
                    phrase("Unless").thenReturn(Condition.Kind.UNLESS),
                    phrase("If").thenReturn(Condition.Kind.IF)),
            SubjectParsers.SUBJECT.followedBy(phrase("[is|'s] paired with")),
            SELECTOR,
            (kind, what, pair) -> (Condition) new Condition.IsPairedWith(kind, what, pair));

    /// "\[unless\|if\] \[player\] is poisoned" — poison-status gate
    /// (Corrupted Resolve). Rule 704.5c.
    static final Parser<Condition> IS_POISONED_CONDITION = sequence(
            anyOf(
                    phrase("Unless").thenReturn(Condition.Kind.UNLESS),
                    phrase("If").thenReturn(Condition.Kind.IF)),
            SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("is poisoned")),
            (kind, who) -> (Condition) new Condition.IsPoisoned(kind, who));

    /// "\[unless\|if\|as long as\] \[player\] [is|'re] the monarch" —
    /// monarchy-status gate (Throne Warden). Rule 716.
    static final Parser<Condition> IS_THE_MONARCH_CONDITION = sequence(
            anyOf(
                    phrase("Unless").thenReturn(Condition.Kind.UNLESS),
                    phrase("If").thenReturn(Condition.Kind.IF),
                    phrase("As long as").thenReturn(Condition.Kind.AS_LONG_AS)),
            SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("[is|'re|are] the monarch")),
            (kind, who) -> (Condition) new Condition.IsTheMonarch(kind, who));

    /// "\[unless\|if\] no mana was spent to cast \<spell\>" — Nix.
    static final Parser<Condition> NO_MANA_SPENT_CONDITION = sequence(
            anyOf(
                    phrase("Unless").thenReturn(Condition.Kind.UNLESS),
                    phrase("If").thenReturn(Condition.Kind.IF)),
            phrase("no mana was spent to cast").then(SubjectParsers.SUBJECT),
            (kind, spell) -> (Condition) new Condition.NoManaSpentToCast(kind, spell));

    /// "\[if|unless\] that mana is spent on \<subject\>" — mana-rider gate
    /// (Carnelian Orb of Dragonkind, Boseiju, Generator Servant).
    static final Parser<Condition> THAT_MANA_SPENT_ON_CONDITION = sequence(
            anyOf(
                    phrase("Unless").thenReturn(Condition.Kind.UNLESS),
                    phrase("If").thenReturn(Condition.Kind.IF)),
            phrase("that mana is spent on").then(SubjectParsers.SUBJECT),
            (kind, target) -> (Condition) new Condition.ThatManaSpentOn(kind, target));

    /// Helper: leading "If" / "Unless" / "As long as" →
    /// [Condition.Kind]. The "As long as" arm lets typed condition
    /// arms double as continuous-predicate gates (Centaur Omenreader:
    /// "As long as this creature is tapped, …").
    private static final Parser<Condition.Kind> CONDITION_KIND = anyOf(
            phrase("Unless").thenReturn(Condition.Kind.UNLESS),
            phrase("If").thenReturn(Condition.Kind.IF),
            // "while" is semantically "as long as" and appears as a
            // trigger-qualifier before the comma (Seasoned Warrenguard:
            // "attacks while you control a token").
            phrase("while").thenReturn(Condition.Kind.AS_LONG_AS),
            phrase("As long as").thenReturn(Condition.Kind.AS_LONG_AS));

    /// "\[unless\|if\] \[player\] both own\[s\] and control\[s\] X and Y …" —
    /// meld-gate condition (rule 701.39, Gisela). Reuses
    /// [SubjectParsers#SUBJECT]'s "and"-collapse into a [Subject.Multiple];
    /// requires ≥ 2 parts.
    static final Parser<Condition> OWNS_AND_CONTROLS_CONDITION = sequence(
                    CONDITION_KIND.followedBy(phrase("you both own and control")),
                    SubjectParsers.SUBJECT,
                    (kind, s) -> {
                        if (!(s instanceof Subject.Multiple m) || m.parts().size() < 2) return null;
                        return (Condition)
                                new Condition.OwnsAndControls(kind, Subject.player(PlayerRef.Pronoun.YOU), m.parts());
                    })
            .suchThat(Objects::nonNull, "conjunction of ≥ 2 own-and-controlled subjects");

    /// "\[unless\|if\] it's your turn" — Fated Retribution.
    static final Parser<Condition> ITS_YOUR_TURN_CONDITION =
            phrase("If it's your turn").<Condition>thenReturn(Condition.ItsYourTurn.IT_IS_YOUR_TURN_IF);

    /// "\[unless\|if\] \[player\] attacked this turn" — Chart a Course.
    static final Parser<Condition> ATTACKED_THIS_TURN_CONDITION = sequence(
            CONDITION_KIND, SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("attacked this turn")), (kind, who) ->
                    (Condition) new Condition.AttackedThisTurn(kind, who));

    /// "\[if\] \<attacker\> attacked \<targets\>" — checks that the attacker
    /// directed attacks at the given targets (Ever-Watching Threshold:
    /// "if they attacked you and/or a planeswalker you control").
    /// Must follow ATTACKED_THIS_TURN_CONDITION in the dispatcher so the more
    /// specific "this turn" suffix is tried first.
    static final Parser<Condition.AttackedTarget> ATTACKED_TARGET_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.SUBJECT.followedBy(word("attacked")),
            SubjectParsers.SUBJECT,
            Condition.AttackedTarget::new);

    /// "\[unless\|if\] \<subject\> blocked this turn" — combat-
    /// history check over a creature subject.
    static final Parser<Condition> BLOCKED_THIS_TURN_CONDITION =
            sequence(CONDITION_KIND, SubjectParsers.SUBJECT.followedBy(phrase("blocked this turn")), (kind, who) ->
                    (Condition) new Condition.BlockedThisTurn(kind, who));

    /// "\[unless\|if\] \<subject\> \[has\|hasn't\] dealt damage yet"
    /// — game-history damage check (Palladia-Mors: "has hexproof if
    /// it hasn't dealt damage yet."). `negated=true` for the
    /// "hasn't" wording.
    static final Parser<Condition> HAS_DEALT_DAMAGE_YET_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.SUBJECT,
            anyOf(
                    phrase("hasn't dealt damage yet").thenReturn(true),
                    phrase("has dealt damage yet").thenReturn(false)),
            (kind, who, negated) -> (Condition) new Condition.HasDealtDamageYet(kind, who, negated));

    /// "\[unless\|if\] \<player\> \[has|have\] \<source\> deal
    /// \<amount\> damage to \<target\>" — pay-with-damage gate
    /// (Dwarven Driller: "unless its controller has this creature
    /// deal 2 damage to them."; Lava Blister).
    static final Parser<Condition> PLAYER_CAUSES_DAMAGE_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.SUBJECT.followedBy(phrase("[has|have]")),
            sequence(SubjectParsers.SUBJECT.followedBy(phrase("deal")), AMOUNT, Map::entry),
            phrase("damage to").then(SubjectParsers.SUBJECT),
            (kind, who, sa, target) ->
                    (Condition) new Condition.PlayerCausesDamage(kind, who, sa.getKey(), sa.getValue(), target));

    /// "\[unless\|if\] \<player\> gained \<amount\> life this turn"
    /// — life-gain history check (The Gaffer: "if you gained 3 or
    /// more life this turn, draw a card.").
    static final Parser<Condition> GAINED_LIFE_THIS_TURN_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(word("gained")),
            anyOf(
                    AMOUNT_MATCHER.followedBy(phrase("life this turn")),
                    phrase("life this turn").<AmountMatcher>thenReturn(new AmountMatcher.AtLeast(Amount.exact(1)))),
            (kind, who, amt) -> (Condition) new Condition.GainedLifeThisTurn(kind, who, amt));

    /// "\[unless\|if\] \<player\> lost \<amount\>? life this turn" —
    /// life-loss history check (Mounted Dreadknight: "if an opponent
    /// lost life this turn"). Bare "lost life" maps to
    /// [AmountMatcher.AtLeast]\(1\) (any positive amount).
    static final Parser<Condition> LOST_LIFE_THIS_TURN_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(word("lost")),
            anyOf(
                    AMOUNT_MATCHER.followedBy(phrase("life this turn")),
                    phrase("life this turn").<AmountMatcher>thenReturn(new AmountMatcher.AtLeast(Amount.exact(1)))),
            (kind, who, amt) -> (Condition) new Condition.LostLifeThisTurn(kind, who, amt));

    /// "\[unless\|if\] \<player\> lost \<amount\>? life last turn" —
    /// life-loss history check covering the previous turn (First
    /// Response: "if you lost life last turn"). Bare "lost life" maps
    /// to [AmountMatcher.AtLeast]\(1\). Sibling of
    /// [#LOST_LIFE_THIS_TURN_CONDITION] for the past-turn case.
    static final Parser<Condition> LOST_LIFE_LAST_TURN_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(word("lost")),
            anyOf(
                    AMOUNT_MATCHER.followedBy(phrase("life last turn")),
                    phrase("life last turn").<AmountMatcher>thenReturn(new AmountMatcher.AtLeast(Amount.exact(1)))),
            (kind, who, amt) -> (Condition) new Condition.LostLifeLastTurn(kind, who, amt));

    /// "\[unless\|if\] \<subject\> [is|are|'s] \<color\>" — color
    /// check (Hydroblast: "Counter target spell if it's red.";
    /// Zealots en-Dal: "if all nonland permanents you control are
    /// white").
    static final Parser<Condition> IS_COLOR_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.SUBJECT.followedBy(phrase("[is|are|'s]")),
            SelectorParsers.COLOR,
            (kind, what, color) -> (Condition) new Condition.IsColor(kind, what, color));

    /// "\[unless\|if\] \<subject\> regenerates this way" — back-
    /// reference to a preceding regenerate effect (Debt of Loyalty:
    /// "You gain control of that creature if it regenerates this
    /// way.").
    static final Parser<Condition> REGENERATES_THIS_WAY_CONDITION =
            sequence(CONDITION_KIND, SubjectParsers.SUBJECT.followedBy(phrase("regenerates this way")), (kind, who) ->
                    (Condition) new Condition.RegeneratesThisWay(kind, who));

    /// "\[unless\|if\] \<subject\> was \<color\> [or \<color\>]*" —
    /// past-tense color check (Filigree Fracture: "If that permanent
    /// was blue or black, draw a card."). Disjunctions compose via
    /// [Condition.AnyOf] over individual [Condition.WasColor]
    /// leaves.
    static final Parser<Condition> WAS_COLOR_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.SUBJECT.followedBy(word("was")),
            SelectorParsers.COLOR.atLeastOnceDelimitedBy(word("or"), Collectors.toUnmodifiableList()),
            (kind, what, colors) -> {
                if (colors.size() == 1) {
                    return (Condition) new Condition.WasColor(kind, what, colors.getFirst());
                }
                var alts = colors.stream()
                        .map(c -> (Condition) new Condition.WasColor(Condition.Kind.IF, what, c))
                        .toList();
                return (Condition) new Condition.AnyOf(kind, alts);
            });

    /// "\[unless\|if\] \<subject\> attacked or blocked this turn" —
    /// combat-history disjunction (Lurker: "This creature can't be
    /// the target of spells unless it attacked or blocked this
    /// turn."). Composed via [Condition.AnyOf] over the existing
    /// `AttackedThisTurn` / `BlockedThisTurn` leaves rather than as
    /// a dedicated combo record. Must precede ATTACKED_THIS_TURN so
    /// the longer "or blocked" suffix wins.
    static final Parser<Condition> ATTACKED_OR_BLOCKED_THIS_TURN_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.SUBJECT.followedBy(phrase("attacked or blocked this turn")),
            (kind, who) -> (Condition) new Condition.AnyOf(
                    kind,
                    List.of(
                            new Condition.AttackedThisTurn(Condition.Kind.IF, who),
                            new Condition.BlockedThisTurn(Condition.Kind.IF, who))));

    /// "\[unless\|if\] \[player\] [didn't]? play\[ed\]? a land this
    /// turn" — land-play history check (River of Tears: "if you
    /// played a land this turn"; Mercadian Atlas: "if you didn't
    /// play a land this turn").
    static final Parser<Condition> PLAYED_LAND_THIS_TURN_CONDITION = sequence(
            CONDITION_KIND,
            sequence(
                    SubjectParsers.PLAYER_LIKE_SUBJECT,
                    anyOf(
                            phrase("[didn't|haven't] play a land this turn").thenReturn(true),
                            phrase("played a land this turn").thenReturn(false)),
                    Map::entry),
            (kind, wn) -> (Condition) new Condition.PlayedLandThisTurn(kind, wn.getKey(), wn.getValue()));

    /// "\[unless\|if\] \<self\> attacked during \[your|their|its\]
    /// last turn" — Giant Turtle. The owner of the last turn is named
    /// by a possessive pronoun, mapped to a [Subject.Player].
    static final Parser<Condition> ATTACKED_DURING_LAST_TURN_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.SUBJECT.followedBy(phrase("attacked during")),
            anyOf(
                            word("your").thenReturn(Subject.player(PlayerRef.Pronoun.YOU)),
                            word("their").thenReturn(Subject.player(PlayerRef.Pronoun.THEY)))
                    .followedBy(phrase("last turn")),
            (kind, who, owner) -> (Condition) new Condition.AttackedDuringLastTurn(kind, who, owner));

    /// Keyword parser used in "has [keyword]" conditions. Tries the
    /// full KEYWORD first (handles "toxic N", "flying", etc.); falls
    /// back to bare "toxic" (no N) producing [Ability.Toxic#n]=null.
    private static final Parser<Ability> HAS_ABILITY_KEYWORD =
            anyOf(KeywordParsers.KEYWORD, phrase("toxic").thenReturn(new Ability.Toxic(null)));

    /// "\[if|unless\] \<subject\> ha\[s|ve\] \<keyword\>" — keyword-
    /// presence check (Compleat Devotion: "If that creature has toxic").
    static final Parser<Condition.HasAbility> HAS_ABILITY_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.SUBJECT.followedBy(phrase("[has|have]")),
            HAS_ABILITY_KEYWORD,
            Condition.HasAbility::new);

    /// "\[unless\|if\] \<self\> ha\[s\|ve\] a \<counter\> counter on
    /// \<self\>" — Pipsqueak, Rebel Strongarm.
    static final Parser<Condition> HAS_COUNTER_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.SUBJECT.followedBy(phrase("[has|have] a")),
            sequence(SelectorParsers.COUNTER_TYPE.followedBy(phrase("counter on")), SubjectParsers.SUBJECT, Map::entry),
            (kind, who, ts) -> (Condition) new Condition.HasCounter(kind, who, ts.getKey(), ts.getValue()));

    /// "\[unless\|if\] \<self\> [is|'s] [a|an] \<type\>" — type
    /// check on a demonstrative (Topple the Statue: "If it's an
    /// artifact, …"; Holy Justiciar: "If that creature is a
    /// Zombie, …"; Eye Gouge: "If it's a Cyclops, …"). Card type
    /// is tried first so "Kobold" doesn't shadow "Land". Subtype
    /// is the fallback.
    ///
    /// Also handles the article-free supertype form (Gimli's Fury:
    /// "If it's legendary, …") via a second arm.
    static final Parser<Condition> IS_TYPE_CONDITION = anyOf(
            sequence(
                            CONDITION_KIND,
                            SubjectParsers.SUBJECT.followedBy(phrase("[is|'s] [a|an]")),
                            anyOf(
                                    SelectorParsers.CARD_TYPE.<TypeMatcher>map(TypeMatcher.IsCardType::new),
                                    SelectorParsers.SUBTYPE.map(TypeMatcher.IsSubtype::new)),
                            Condition.IsType::new)
                    // Trailing game-object type — Elven Farsight: "If it's
                    // a creature card, …" combines into a [TypeMatcher.All]
                    // alongside the card-type matcher.
                    .optionallyFollowedBy(
                            SelectorParsers.GAME_OBJECT_TYPE,
                            (it, ot) -> new Condition.IsType(
                                    it.kind(),
                                    it.what(),
                                    new TypeMatcher.All(List.of(it.matcher(), new TypeMatcher.IsGameObject(ot)))))
                    .map(c -> (Condition) c),
            // "[if|unless] [subject] is/are/'s [supertype]" — no article
            // required before supertypes (Gimli's Fury: "If it's legendary").
            sequence(
                    CONDITION_KIND,
                    SubjectParsers.SUBJECT.followedBy(phrase("[is|are|'s]")),
                    SelectorParsers.SUPERTYPE.<TypeMatcher>map(TypeMatcher.IsSupertype::new),
                    Condition.IsType::new));

    /// "\[unless\|if\] \<self\> was \[a\|an\] \<supertype\>?
    /// \<subtype\>? \<card-type\> spell?" — past-state type check
    /// (Thermokarst: "If that land was a snow land, …"; Jace's
    /// Defeat: "If it was a Jace planeswalker spell, …"). Tries the
    /// most-specific shape first.
    /// One [TypeMatcher] leaf — supertype, subtype, or card type —
    /// for the "was a/an X" condition arms below.
    private static final Parser<TypeMatcher> WAS_CARD_TYPE_PART = anyOf(
            SelectorParsers.SUPERTYPE.map(TypeMatcher.IsSupertype::new),
            SelectorParsers.CARD_TYPE.map(TypeMatcher.IsCardType::new),
            SelectorParsers.SUBTYPE.map(TypeMatcher.IsSubtype::new));

    /// Folds a non-empty list of leaves into a single matcher
    /// (`All[...]` for ≥2, the leaf itself for 1).
    private static TypeMatcher allOf(List<TypeMatcher> leaves) {
        return leaves.size() == 1 ? leaves.getFirst() : new TypeMatcher.All(List.copyOf(leaves));
    }

    static final Parser<Condition> WAS_CARD_TYPE_CONDITION = anyOf(
            // "was nonbasic" / "was noncreature" / etc. (Helldozer:
            // "If that land was nonbasic, untap this creature."). The
            // QUALIFIER parser already emits a [TypeMatcher.Not] leaf
            // for the "non-" prefix; we wrap it in WasCardType.
            sequence(
                    CONDITION_KIND,
                    SubjectParsers.SUBJECT.followedBy(word("was")),
                    SelectorParsers.QUALIFIER.suchThat(
                            q -> q instanceof Selector.Qualifier.Types, "negated type qualifier"),
                    (kind, what, q) -> (Condition)
                            new Condition.WasCardType(kind, what, ((Selector.Qualifier.Types) q).matcher())),
            // "was [a|an] X (Y …)? spell?" — accept any non-empty
            // sequence of supertype / subtype / card-type leaves
            // followed by an optional "spell" suffix. Folds the
            // leaves into a single `All` matcher; the "spell" suffix
            // sets `asSpell=true`.
            sequence(
                            CONDITION_KIND,
                            SubjectParsers.SUBJECT.followedBy(phrase("was [a|an]")),
                            WAS_CARD_TYPE_PART.atLeastOnce(),
                            (kind, what, leaves) -> new Condition.WasCardType(kind, what, allOf(leaves), false))
                    .optionallyFollowedBy(
                            word("spell"), (c, _) -> new Condition.WasCardType(c.kind(), c.what(), c.matcher(), true))
                    .map(c -> (Condition) c));

    /// "\[unless\|if\] \[player\] cast \<self\> \[from \<zone\>\]?" —
    /// cast-by check with optional source zone (Iridescent Tiger;
    /// Coal Stoker: "if you cast it from your hand").
    static final Parser<Condition> WAS_CAST_BY_CONDITION = sequence(
                    CONDITION_KIND,
                    SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(word("cast")),
                    SubjectParsers.SUBJECT,
                    (kind, who, what) -> new Condition.WasCastBy(kind, who, what))
            .optionallyFollowedBy(
                    ZoneExpressionParsers.IN_ZONE_FROM,
                    (c, z) -> new Condition.WasCastBy(c.kind(), c.who(), c.what(), Zone.Source.fromZone(z)))
            .map(c -> (Condition) c);

    /// "\[unless\|if\] \[player\] win\[s\] the flip" — Tavern
    /// Swindler. Also handles the negated form "lose\[s\] the flip"
    /// (Wild Wurm: "If you lose the flip, return this creature …"),
    /// which maps to the same condition with the kind negated
    /// (e.g., "if you lose" → [Condition.Kind#UNLESS] you win).
    static final Parser<Condition> WON_FLIP_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.PLAYER_LIKE_SUBJECT,
            anyOf(
                    phrase("win(s) the flip").thenReturn(false),
                    phrase("lose(s) the flip").thenReturn(true)),
            (kind, who, lost) -> (Condition) new Condition.WonFlip(lost ? kind.negate() : kind, who));

    /// "\[unless\|if\] \[player\] sacrifice\[s\] \<subject\>" —
    /// Plant Elemental, Mold Demon.
    static final Parser<Condition> PLAYER_SACRIFICES_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("sacrifice(s)")),
            SubjectParsers.SUBJECT,
            (kind, who, what) -> (Condition) new Condition.PlayerSacrifices(kind, who, what));

    /// "\[unless\|if\] \[player\] exile\[s\] \<subject\> [from
    /// \[possessive\] \<zone\>]?" — exile-as-cost condition (Grip
    /// of Amnesia: "Counter target spell unless its controller
    /// exiles all cards from their graveyard"). The optional
    /// "from \<zone\>" tail is consumed but not yet stored
    /// structurally on PlayerExiles; downstream consumers can
    /// re-derive it from oracle context if needed.
    static final Parser<Condition> PLAYER_EXILES_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("exile(s)")),
            SubjectParsers.SUBJECT.optionallyFollowedBy(
                    phrase("from [your|their|its|his|her]").then(ZONE_NAME), (s, _) -> s),
            (kind, who, what) -> (Condition) new Condition.PlayerExiles(kind, who, what));

    /// "\[unless\|if\] \[player\] return(s) \<subject\> to \[its\|their\]
    /// owner's hand" — player-bounce condition (Tragic Lesson:
    /// "discard a card unless you return a land you control to its
    /// owner's hand").
    static final Parser<Condition> PLAYER_RETURNS_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("return(s)")),
            SubjectParsers.SUBJECT.followedBy(phrase("to [its|their|his|her] owner's hand")),
            (kind, who, what) -> (Condition) new Condition.PlayerReturns(kind, who, what));

    /// "\[unless\|if\] \[player\] discard\[s\] \<subject\>
    /// \[at random\]?" — Wrench Mind, Fallow Wurm; Balduvian Horde
    /// adds the "at random" tail.
    static final Parser<Condition> PLAYER_DISCARDS_CONDITION = sequence(
                    CONDITION_KIND,
                    SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("discard(s)")),
                    SubjectParsers.SUBJECT,
                    (kind, who, what) -> new Condition.PlayerDiscards(kind, who, what))
            .optionallyFollowedBy(
                    phrase("at random"), (c, _) -> new Condition.PlayerDiscards(c.kind(), c.who(), c.what(), true))
            .map(c -> (Condition) c);

    /// "\[unless\|if\] \[player\] \[has|have|'s|'ve\] cast \<selector\>
    /// this turn" — cast-history check (Gigastorm Titan, Goblin
    /// Cohort). Also handles the count-based bare-past-tense form:
    /// "\[if\] \[player\] cast \<amountMatcher\> \<selector\> this turn"
    /// (Ertai's Scorn: "if an opponent cast two or more spells this
    /// turn").
    static final Parser<Condition> CAST_THIS_TURN_CONDITION = Parser.<Condition>anyOf(
            // Count-based form: "cast two or more spells this turn" (bare past tense + AmountMatcher)
            sequence(
                    CONDITION_KIND,
                    SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("cast")),
                    AmountParsers.AMOUNT_MATCHER,
                    SelectorParsers.SELECTOR.followedBy(phrase("this turn")),
                    (kind, who, count, what) -> new Condition.CastThisTurn(kind, who, count, what)),
            // Simple form: "has/have/'s/'ve cast [selector] this turn"
            sequence(
                    CONDITION_KIND,
                    SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("[has|have|'s|'ve] cast")),
                    SelectorParsers.SELECTOR.followedBy(phrase("this turn")),
                    (kind, who, what) -> Condition.CastThisTurn.atLeastOne(kind, who, what)));

    /// "\[unless\|if\] \[player\] \[has|have|'s|'ve\] discarded
    /// \<subject\> this turn" — discard-history check (Gilt-Blade
    /// Prowler: "Activate only if you've discarded a card this
    /// turn.").
    static final Parser<Condition> DISCARDED_THIS_TURN_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("[has|have|'s|'ve] discarded")),
            SubjectParsers.SUBJECT.followedBy(phrase("this turn")),
            (kind, who, what) -> (Condition) new Condition.DiscardedThisTurn(kind, who, what));

    /// "\[unless\|if\] \[player\] discarded \<subject\> this way" —
    /// back-reference to a discard performed by the preceding effect in
    /// the same resolution (Fanatic of the Harrowing: "If you discarded
    /// a card this way, draw a card."). Active past-tense without an
    /// auxiliary — must precede [#DISCARDED_THIS_TURN_CONDITION] if both
    /// are registered, but they share no prefix ambiguity since this one
    /// uses bare "discarded" while the turn variant uses
    /// "has/have/'s/'ve discarded".
    static final Parser<Condition> DISCARDED_THIS_WAY_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("discarded")),
            SubjectParsers.SUBJECT.followedBy(phrase("this way")),
            (kind, who, what) -> (Condition) new Condition.DiscardedThisWay(kind, who, what));

    /// "\[unless\|if\] \<self\> isn't \[a|an\] \<type\>" — negated
    /// type check (Fa'adiyah Seer / Sindbad: "If it isn't a land
    /// card, …").
    static final Parser<Condition> IS_NOT_TYPE_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.SUBJECT.followedBy(phrase("isn't [a|an]")),
            anyOf(
                    SelectorParsers.CARD_TYPE
                            .followedBy(phrase("card").optional())
                            .<TypeMatcher>map(TypeMatcher.IsCardType::new),
                    SelectorParsers.SUBTYPE.<TypeMatcher>map(TypeMatcher.IsSubtype::new)),
            (kind, what, inner) -> (Condition) new Condition.IsType(kind, what, new TypeMatcher.Not(inner)));

    /// "\[unless\|if\] \<subject\> attack\[s\]" — combat-action
    /// check (Viashino Bey, Ekundu Cyclops).
    static final Parser<Condition> SUBJECT_ATTACKS_CONDITION =
            sequence(CONDITION_KIND, SubjectParsers.SUBJECT.followedBy(phrase("attack(s)")), (kind, who) ->
                    (Condition) new Condition.SubjectAttacks(kind, who));

    /// "\[unless\|if\] \<subject\> also attack\[s\]" — co-attacker
    /// check (Scarred Puma).
    static final Parser<Condition> ALSO_ATTACKS_CONDITION =
            sequence(CONDITION_KIND, SubjectParsers.SUBJECT.followedBy(phrase("also attack(s)")), (kind, who) ->
                    (Condition) new Condition.AlsoAttacks(kind, who));

    /// "\[unless\|if\] \<subject\> was blocked this turn" — Fyndhorn
    /// Druid.
    static final Parser<Condition> WAS_BLOCKED_THIS_TURN_CONDITION =
            sequence(CONDITION_KIND, SubjectParsers.SUBJECT.followedBy(phrase("was blocked this turn")), (kind, who) ->
                    (Condition) new Condition.WasBlockedThisTurn(kind, who));

    /// "\[unless\|if\] \<subject\> is blocked" — present-tense block-
    /// state predicate (Cinder Crawler: "Activate only if this
    /// creature is blocked.").
    static final Parser<Condition> IS_BLOCKED_CONDITION =
            sequence(CONDITION_KIND, SubjectParsers.SUBJECT.followedBy(phrase("is blocked")), (kind, who) ->
                    (Condition) new Condition.IsBlocked(kind, who));

    /// "\[unless\|if\] \<subject\> was milled this way" — back-
    /// reference to a preceding mill effect (Saprazzan Breaker:
    /// "If a land card was milled this way, …").
    static final Parser<Condition> WAS_MILLED_THIS_WAY_CONDITION =
            sequence(CONDITION_KIND, SubjectParsers.SUBJECT.followedBy(phrase("was milled this way")), (kind, what) ->
                    (Condition) new Condition.WasMilledThisWay(kind, what));

    /// "\[unless\|if\] \<subject\> was destroyed this way" — back-
    /// reference to a preceding destroy effect (Break the Spell:
    /// "If a permanent you controlled or a token was destroyed
    /// this way, draw a card."; Dire-Strain Rampage: "If a land
    /// was destroyed this way, …").
    static final Parser<Condition> WAS_DESTROYED_THIS_WAY_CONDITION = sequence(
            CONDITION_KIND, SubjectParsers.SUBJECT.followedBy(phrase("was destroyed this way")), (kind, what) ->
                    (Condition) new Condition.WasDestroyedThisWay(kind, what));

    /// "\[unless\|if\] X is \<matcher\>" — X-value gate (Martial Coup:
    /// "If X is 5 or more, destroy all other creatures.").
    static final Parser<Condition> X_VALUE_CONDITION =
            sequence(CONDITION_KIND.followedBy(phrase("X is")), AMOUNT_MATCHER, (kind, amt) ->
                    (Condition) new Condition.XValue(kind, amt));

    /// "\[unless\|if\] \<subject\> \[was|wasn't\] blocking" —
    /// Guildsworn Prowler.
    static final Parser<Condition> WAS_BLOCKING_CONDITION = sequence(
            CONDITION_KIND,
            sequence(
                    SubjectParsers.SUBJECT,
                    anyOf(
                            phrase("was blocking").thenReturn(false),
                            phrase("wasn't blocking").thenReturn(true)),
                    Map::entry),
            (kind, sn) -> (Condition) new Condition.WasBlocking(kind, sn.getKey(), sn.getValue()));

    /// "\[unless\|if\] \<subject\> died this turn" — Life Goes On.
    static final Parser<Condition> DIED_THIS_TURN_CONDITION =
            sequence(CONDITION_KIND, SubjectParsers.SUBJECT.followedBy(phrase("died this turn")), (kind, who) ->
                    (Condition) new Condition.DiedThisTurn(kind, who));

    /// "\[unless\|if\] \<subject\> had a \<counter\> counter on
    /// \<subject\>" — Promising Duskmage.
    static final Parser<Condition> HAD_COUNTER_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.SUBJECT.followedBy(phrase("had a")),
            sequence(SelectorParsers.COUNTER_TYPE.followedBy(phrase("counter on")), SubjectParsers.SUBJECT, Map::entry),
            (kind, who, ts) -> (Condition) new Condition.HadCounter(kind, who, ts.getKey(), ts.getValue()));

    /// "\[unless\|if\] \<subject\> share\[s\] a color with \<subject\>"
    /// — Jaded Response.
    static final Parser<Condition.SharesColorWith> SHARES_COLOR_WITH_CONDITION = sequence(
                    CONDITION_KIND,
                    SubjectParsers.SUBJECT.followedBy(phrase("share(s) a color")),
                    Condition.SharesColorWith::new)
            .optionallyFollowedBy(
                    word("with").then(SubjectParsers.SUBJECT),
                    (c, other) -> new Condition.SharesColorWith(c.kind(), c.who(), other));

    /// "\[unless\|if\] \[player\] [has|have] \<matcher\> life" —
    /// life-total comparison (Convalescence, Near-Death Experience,
    /// Spell Snuff, Test of Endurance).
    static final Parser<Condition> HAS_LIFE_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("[has|have]")),
            AMOUNT_MATCHER.followedBy(word("life")),
            (kind, who, m) -> (Condition) new Condition.HasLife(kind, who, m));

    /// "\[unless\|if\] \[player\] [has|have] \<matcher\> opponents" —
    /// opponent-count check (Bountiful Promenade and the other
    /// Battlebond double-control lands).
    static final Parser<Condition> HAS_OPPONENTS_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("[has|have]")),
            AMOUNT_MATCHER.followedBy(word("opponents")),
            (kind, who, m) -> (Condition) new Condition.HasOpponents(kind, who, m));

    /// "\[unless\|if\|as long as\] \<subject\> is \[tapped|untapped\]"
    /// — tap-state check (Centaur Omenreader, Nim Abomination).
    static final Parser<Condition> IS_TAPPED_CONDITION = sequence(
            CONDITION_KIND,
            sequence(
                    SubjectParsers.SUBJECT.followedBy(word("is")),
                    anyOf(word("tapped").thenReturn(true), word("untapped").thenReturn(false)),
                    Map::entry),
            (kind, sn) -> (Condition) new Condition.IsTapped(kind, sn.getKey(), sn.getValue()));

    /// Possessive owner pronoun ("your" / "their") or possessive
    /// player reference ("that player's") → Subject.Player.
    private static final Parser<Subject> TURN_OWNER_PRONOUN = anyOf(
            word("your").thenReturn(Subject.player(PlayerRef.Pronoun.YOU)),
            word("their").thenReturn(Subject.player(PlayerRef.Pronoun.THEY)),
            SubjectParsers.PLAYER_REF.followedBy(string("'s")).map(Subject::player));

    /// "\[unless\|if\] it's \[not\]? \[player\]'s turn" — turn-owner
    /// check (Glademuse: "if it's not their turn"). Two arms — the
    /// negated form first so the "not" wins.
    static final Parser<Condition> IS_TURN_OWNER_CONDITION = anyOf(
            sequence(
                    CONDITION_KIND.followedBy(phrase("it's not")),
                    TURN_OWNER_PRONOUN.followedBy(word("turn")),
                    (kind, who) -> (Condition) new Condition.IsTurnOwner(kind, who, true)),
            sequence(
                    CONDITION_KIND.followedBy(phrase("it's")),
                    TURN_OWNER_PRONOUN.followedBy(word("turn")),
                    (kind, who) -> (Condition) new Condition.IsTurnOwner(kind, who, false)));

    /// "\[unless\|if\] it's \[day\|night\]" — day/night state check
    /// (Moonrager's Slash: "This spell costs {2} less to cast if it's night.").
    static final Parser<Condition> IS_DAY_NIGHT_CONDITION = sequence(
            CONDITION_KIND.followedBy(phrase("it's")),
            anyOf(
                    word("day").thenReturn(Effect.BecomeDayNight.DayNight.DAY),
                    word("night").thenReturn(Effect.BecomeDayNight.DayNight.NIGHT)),
            Condition.IsDayNight::new);

    /// "\[unless\|if\] \<mana-symbol\>+ was spent to cast \<self\>"
    /// — Tin Street Hooligan ("{X}"); Mythos of Nethroi ("{G}{W}").
    static final Parser<Condition> MANA_SPENT_TO_CAST_CONDITION = sequence(
            CONDITION_KIND,
            MANA_SYMBOL.atLeastOnce().followedBy(phrase("was spent to cast")),
            SubjectParsers.SUBJECT,
            (kind, syms, what) -> (Condition) new Condition.ManaSpentToCast(kind, syms, what));

    /// "\[unless\|if\] \<matcher\> \<color\> mana was spent to cast
    /// \<spell\>" — Adamant-style color-and-amount check
    /// (Unexplained Vision: "If at least three blue mana was spent
    /// to cast this spell, scry 3.").
    static final Parser<Condition> COLOR_MANA_SPENT_TO_CAST_CONDITION = sequence(
            CONDITION_KIND,
            AMOUNT_MATCHER,
            sequence(
                    SelectorParsers.COLOR.followedBy(phrase("mana was spent to cast")),
                    SubjectParsers.SUBJECT,
                    Map::entry),
            (kind, amt, cs) -> (Condition) new Condition.ColorManaSpentToCast(kind, amt, cs.getKey(), cs.getValue()));

    /// "\[unless\|if\] \<matcher\> colored mana was spent to cast
    /// \<spell\>" — color-agnostic colored-mana check (Void Mirror:
    /// "if no colored mana was spent to cast it").
    static final Parser<Condition> COLORED_MANA_SPENT_TO_CAST_CONDITION = sequence(
            CONDITION_KIND,
            AMOUNT_MATCHER.followedBy(phrase("colored mana was spent to cast")),
            SubjectParsers.SUBJECT,
            (kind, amt, what) -> (Condition) new Condition.ColoredManaSpentToCast(kind, amt, what));

    /// "\[unless\|if\] there are \<matcher\> \<subject\>" —
    /// count-of-selector existence (Deep-Sea Terror: "unless there
    /// are seven or more cards in your graveyard."). Also handles
    /// the singular contraction "there's \<subject\>" — treated as
    /// AtLeast(1) (Walltop Sentries: "if there's a Lesson card in
    /// your graveyard").
    static final Parser<Condition> COUNT_OF_CONDITION = anyOf(
            sequence(
                    CONDITION_KIND, phrase("there are").then(AMOUNT_MATCHER), SubjectParsers.SUBJECT, (kind, m, what) ->
                            (Condition) new Condition.CountOf(kind, m, what)),
            sequence(CONDITION_KIND, phrase("there's").then(SubjectParsers.SUBJECT), (kind, what) ->
                    (Condition) new Condition.CountOf(kind, new AmountMatcher.AtLeast(Amount.exact(1)), what)));

    /// "\[unless\|if\] \[player\] control[s] \[more|fewer\]
    /// \<selector\> than \<subject\>" — comparison count (Unified
    /// Will: "if you control more creatures than that spell's
    /// controller.").
    static final Parser<Condition> PLAYER_CONTROLS_COMPARED_CONDITION = sequence(
            CONDITION_KIND,
            sequence(
                    SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("control(s)")),
                    anyOf(
                            word("more").thenReturn(Condition.PlayerControlsCompared.ComparatorMore.MORE),
                            word("fewer").thenReturn(Condition.PlayerControlsCompared.ComparatorMore.FEWER)),
                    Map::entry),
            sequence(SubjectParsers.SUBJECT.followedBy(word("than")), SubjectParsers.SUBJECT, Map::entry),
            (kind, wm, ws) -> (Condition)
                    new Condition.PlayerControlsCompared(kind, wm.getKey(), wm.getValue(), ws.getKey(), ws.getValue()));

    /// "\[unless\|if\] \[player\] [has|have] \[more|fewer\] cards in
    /// hand than \[other-player\]" — hand-size comparison (Balance
    /// of Power: "If target opponent has more cards in hand than
    /// you, …").
    static final Parser<Condition> CARDS_IN_HAND_COMPARED_CONDITION = sequence(
            CONDITION_KIND,
            sequence(
                    SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("[has|have]")),
                    anyOf(
                            word("more").thenReturn(Condition.PlayerControlsCompared.ComparatorMore.MORE),
                            word("fewer").thenReturn(Condition.PlayerControlsCompared.ComparatorMore.FEWER)),
                    Map::entry),
            phrase("cards in hand than").then(SubjectParsers.PLAYER_LIKE_SUBJECT),
            (kind, wm, other) ->
                    (Condition) new Condition.CardsInHandCompared(kind, wm.getKey(), wm.getValue(), other));

    /// "\[unless\|if\] \<spell\> targets \<selector\>" — Dragon's
    /// Prey: "if it targets a Dragon."
    static final Parser<Condition> SPELL_TARGETS_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.SUBJECT.followedBy(word("targets")),
            SubjectParsers.SUBJECT,
            (kind, spell, what) -> (Condition) new Condition.SpellTargets(kind, spell, what));

    /// "\[unless\|if\] \<spell\> would destroy \<subject\>" —
    /// destruction-check condition (Equinox: "Counter target spell
    /// if it would destroy a land you control.").
    static final Parser<Condition.SpellWouldDestroy> SPELL_WOULD_DESTROY_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.SUBJECT.followedBy(phrase("would destroy")),
            SubjectParsers.SUBJECT,
            Condition.SpellWouldDestroy::new);

    /// "\[unless\|if\] \<subject\> [is|are]\[n't\]? on the
    /// battlefield" — selector-existence check (Wirecat: "if an
    /// enchantment is on the battlefield").
    static final Parser<Condition> SELECTOR_ON_BATTLEFIELD_CONDITION = sequence(
            CONDITION_KIND,
            sequence(
                    SubjectParsers.SUBJECT,
                    anyOf(
                            phrase("[isn't|aren't] on the battlefield").thenReturn(true),
                            phrase("[is|are] on the battlefield").thenReturn(false)),
                    Map::entry),
            (kind, sn) -> (Condition) new Condition.SelectorOnBattlefield(kind, sn.getKey(), sn.getValue()));

    /// "\[unless\|if\] they're mana abilities" — Suppression Field.
    static final Parser<Condition> ARE_MANA_ABILITIES_CONDITION =
            sequence(CONDITION_KIND, phrase("they're mana abilities"), (kind, _) ->
                    (Condition) new Condition.AreManaAbilities(kind));

    /// "\[unless\|if\] \[player\] [has|have] been dealt damage this
    /// turn" — Bloodcrazed Goblin.
    static final Parser<Condition> HAS_BEEN_DEALT_DAMAGE_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("[has|have] been dealt damage this turn")),
            (kind, who) -> (Condition) new Condition.HasBeenDealtDamageThisTurn(kind, who));

    /// "\[unless\|if\] \<amount\> damage was dealt to \<subject\>
    /// this turn" — damage-dealt-with-amount check (Rushing-Tide
    /// Zubera: "if 4 or more damage was dealt to it this turn,
    /// draw three cards.").
    static final Parser<Condition> DAMAGE_DEALT_THIS_TURN_CONDITION = sequence(
            CONDITION_KIND,
            AMOUNT_MATCHER.followedBy(phrase("damage was dealt to")),
            SubjectParsers.SUBJECT.followedBy(phrase("this turn")),
            (kind, amt, who) -> (Condition) new Condition.DamageDealtThisTurn(kind, amt, who));

    /// "\[unless\|if\] \<subject\> is \<P\>/\<T\>" — P/T equality
    /// (Sigil Captain: "if that creature is 1/1").
    static final Parser<Condition> HAS_PT_CONDITION =
            sequence(CONDITION_KIND, SubjectParsers.SUBJECT.followedBy(word("is")), PT_VALUE, (kind, who, pt) ->
                    (Condition) new Condition.HasPT(kind, who, pt));

    /// "\[unless\|if\] \<subject\> has the same mana value as \<other\>"
    /// — mana-value equality comparison (Hisoka, Minamo Sensei: "if it
    /// has the same mana value as the discarded card"). Must precede
    /// [#HAS_MANA_VALUE_CONDITION] in CONDITION_LEAF (longer prefix).
    static final Parser<Condition.SameManaValueAs> SAME_MANA_VALUE_AS_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.SUBJECT.followedBy(phrase("has the same mana value as")),
            SubjectParsers.SUBJECT,
            Condition.SameManaValueAs::new);

    /// "\[unless\|if\] [its|\<subject\>'s] mana value [is|was]
    /// \<matcher\>" — mana-value check (Extinguish the Light: "If its
    /// mana value was 3 or less, you gain 3 life.").
    static final Parser<Condition> HAS_MANA_VALUE_CONDITION = sequence(
            CONDITION_KIND,
            anyOf(
                            word("its").thenReturn(Subject.pronoun(PronounType.IT)),
                            SubjectParsers.SUBJECT.followedBy(string("'s")))
                    .followedBy(phrase("mana value [is|was]")),
            AMOUNT_MATCHER,
            (kind, what, amt) -> (Condition) new Condition.HasManaValue(kind, what, amt));

    /// "\[unless\|if\] \<subject\>'s \[power\|toughness\] is
    /// \<matcher\>" — numeric property comparison (Depressurize: "if
    /// that creature's power is 0 or less, destroy it."). Uses
    /// [Property#POWER] / [Property#TOUGHNESS] as the property slot.
    static final Parser<Condition> HAS_PROPERTY_VALUE_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.SUBJECT.followedBy(string("'s")),
            anyOf(word("power").thenReturn(Property.POWER), word("toughness").thenReturn(Property.TOUGHNESS))
                    .followedBy(word("is")),
            AMOUNT_MATCHER,
            (kind, what, property, amt) -> (Condition) new Condition.HasPropertyValue(kind, what, property, amt));

    /// "\[unless\|if\] \<subject\> have total \[power\|toughness\]
    /// \<matcher\>" — cumulative property threshold across multiple
    /// permanents (Owlbear Shepherd: "if creatures you control have
    /// total power 8 or greater"). Uses [Property#POWER] /
    /// [Property#TOUGHNESS] as the property slot.
    static final Parser<Condition> TOTAL_PROPERTY_OF_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.SUBJECT.followedBy(phrase("have total")),
            anyOf(word("power").thenReturn(Property.POWER), word("toughness").thenReturn(Property.TOUGHNESS)),
            AMOUNT_MATCHER,
            (kind, what, property, amt) -> (Condition) new Condition.TotalPropertyOf(kind, what, property, amt));

    /// Phase reference with possessive owner ("your main phase",
    /// "that player's main phase"). Builds on
    /// [TriggerEventParsers#PHASE_NAME] and overlays the owner
    /// from [#DURING_OWNER]; the underlying [TriggerEvent.AtPhase]
    /// already exposes a `withOwner` wither.
    private static final Parser<TriggerEvent.AtPhase> OWNED_PHASE =
            sequence(DURING_OWNER.map(Subject::player), TriggerEventParsers.PHASE_NAME, (owner, phase) ->
                    (TriggerEvent.AtPhase) phase.withOwner(owner, false));

    /// "\[unless\|if\] \<player\> cast \<spell\> during \<phase\>" —
    /// Addendum-style timing predicate (Sphinx's Insight: "If you
    /// cast this spell during your main phase, you gain 2 life.").
    static final Parser<Condition> CAST_DURING_PHASE_CONDITION = sequence(
            CONDITION_KIND,
            SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(word("cast")),
            SubjectParsers.SUBJECT.followedBy(word("during")),
            OWNED_PHASE,
            (kind, who, what, phase) -> (Condition) new Condition.CastDuringPhase(kind, who, what, phase));

    /// Single-condition leaf — the typed condition arm dispatcher
    /// before the "or"-disjunction wrapper. Use [#CONDITION_TAIL]
    /// from outside; this internal parser exists so the wrapper
    /// can compose two leaves into a [Condition.AnyOf].
    private static final Parser<Condition> CONDITION_LEAF = anyOf(
            // Player-state arms — try most specific first
            PLAYER_PAYS_CONDITION,
            PLAYER_SACRIFICES_CONDITION,
            PLAYER_EXILES_CONDITION,
            PLAYER_RETURNS_CONDITION,
            PLAYER_DISCARDS_CONDITION,
            CAST_THIS_TURN_CONDITION,
            DISCARDED_THIS_TURN_CONDITION,
            DISCARDED_THIS_WAY_CONDITION, // bare "discarded … this way" — no aux verb, distinct from
            // DISCARDED_THIS_TURN
            CARDS_IN_HAND_CONDITION,
            CARDS_IN_LIBRARY_CONDITION,
            ANY_ZONE_HAS_CARDS_CONDITION,
            ATTACKED_OR_BLOCKED_THIS_TURN_CONDITION, // must precede ATTACKED_THIS_TURN (longer suffix)
            ATTACKED_THIS_TURN_CONDITION,
            ATTACKED_TARGET_CONDITION, // must follow ATTACKED_THIS_TURN ("this turn" suffix tried first)
            PLAYER_CAUSES_DAMAGE_CONDITION, // must precede HAS_DEALT_DAMAGE_YET (longer "has X deal …" prefix)
            HAS_DEALT_DAMAGE_YET_CONDITION,
            GAINED_LIFE_THIS_TURN_CONDITION,
            LOST_LIFE_THIS_TURN_CONDITION,
            LOST_LIFE_LAST_TURN_CONDITION,
            IS_COLOR_CONDITION,
            REGENERATES_THIS_WAY_CONDITION,
            WAS_COLOR_CONDITION, // must precede WAS_CARD_TYPE — both start with "was X"
            PLAYED_LAND_THIS_TURN_CONDITION,
            ATTACKED_DURING_LAST_TURN_CONDITION,
            HAS_BEEN_DEALT_DAMAGE_CONDITION,
            DAMAGE_DEALT_THIS_TURN_CONDITION,
            SAME_MANA_VALUE_AS_CONDITION, // must precede HAS_MANA_VALUE ("same mana value as" longer match)
            HAS_MANA_VALUE_CONDITION,
            HAS_PROPERTY_VALUE_CONDITION, // "[subject]'s power/toughness is [matcher]" (Depressurize)
            TOTAL_PROPERTY_OF_CONDITION, // "[subject] have total power/toughness [matcher]" (Owlbear Shepherd)
            HAS_PT_CONDITION, // must precede IS_COLOR/IS_TAPPED ("is" prefix shared)
            CAST_DURING_PHASE_CONDITION,
            HAS_LIFE_CONDITION,
            HAS_OPPONENTS_CONDITION,
            WON_FLIP_CONDITION,
            WAS_CAST_BY_CONDITION,
            IS_POISONED_CONDITION,
            IS_THE_MONARCH_CONDITION,
            OWNS_AND_CONTROLS_CONDITION, // must precede PLAYER_CONTROLS ("you both own and control" prefix shared)
            CARDS_IN_HAND_COMPARED_CONDITION, // must precede CARDS_IN_HAND (longer "more/fewer" prefix)
            PLAYER_CONTROLS_COMPARED_CONDITION, // must precede PLAYER_CONTROLS (longer "more/fewer" prefix)
            PLAYER_CONTROLS_CONDITION,
            HAD_COUNTER_CONDITION, // must precede HAS_COUNTER ("had" longer than "has")
            HAS_COUNTER_CONDITION,
            HAS_ABILITY_CONDITION,
            SHARES_COLOR_WITH_CONDITION,
            // Self/object-state arms
            DIED_THIS_TURN_CONDITION,
            WAS_BLOCKED_THIS_TURN_CONDITION,
            IS_BLOCKED_CONDITION,
            WAS_MILLED_THIS_WAY_CONDITION,
            WAS_DESTROYED_THIS_WAY_CONDITION,
            X_VALUE_CONDITION,
            WAS_BLOCKING_CONDITION,
            ALSO_ATTACKS_CONDITION, // must precede SUBJECT_ATTACKS (longer "also" prefix)
            SUBJECT_ATTACKS_CONDITION,
            IS_TAPPED_CONDITION,
            IS_EQUIPPED_CONDITION,
            IS_ENCHANTED_CONDITION,
            IS_PAIRED_WITH_CONDITION,
            WAS_KICKED_CONDITION,
            WAS_CARD_TYPE_CONDITION,
            IS_NOT_TYPE_CONDITION, // must precede IS_TYPE (longer "isn't" prefix)
            IS_TYPE_CONDITION,
            SELECTOR_ON_BATTLEFIELD_CONDITION,
            ITS_YOUR_TURN_CONDITION,
            IS_TURN_OWNER_CONDITION,
            IS_DAY_NIGHT_CONDITION,
            ARE_MANA_ABILITIES_CONDITION,
            // Spell/event arms
            MANA_SPENT_TO_CAST_CONDITION,
            COLOR_MANA_SPENT_TO_CAST_CONDITION,
            COLORED_MANA_SPENT_TO_CAST_CONDITION, // must precede MANA_SPENT_TO_CAST when "colored" qualifies "mana"
            COUNT_OF_CONDITION,
            SPELL_WOULD_DESTROY_CONDITION,
            SPELL_TARGETS_CONDITION,
            NO_MANA_SPENT_CONDITION,
            THAT_MANA_SPENT_ON_CONDITION);

    /// Trailing condition tail used by [#COUNTER_SPELL] and effect
    /// suffixes. Wraps [#CONDITION_LEAF] with an "or [if]?" suffix
    /// so disjunctive conditions (Mythos of Nethroi: "if it's a
    /// creature or if {G}{W} was spent to cast this spell") compose
    /// into a [Condition.AnyOf]. Both leaves must share the same
    /// kind (IF / UNLESS).
    static final Parser<Condition> CONDITION_TAIL = CONDITION_LEAF.optionallyFollowedBy(
            word("or").then(CONDITION_LEAF),
            (a, b) -> a.kind() == b.kind() ? new Condition.AnyOf(a.kind(), List.of(a, b)) : a);

    /// "If \<typed-condition\>," — prefix conditional that gates the
    /// following effect (Idle Thoughts: "Draw a card if you have no
    /// cards in hand."; Artificer's Epiphany: "If you control no
    /// artifacts, discard a card."). Filters [#CONDITION_TAIL] to the
    /// IF-kind arms and consumes the trailing comma.
    static final Parser<Condition> IF_PREFIX_CONDITION = CONDITION_TAIL
            .suchThat(c -> c.kind() == Condition.Kind.IF, "if-kind condition")
            .followedBy(string(","));

    /// "Unless \<typed-condition\>," — Rhystic Syphon-style negation
    /// gate. Filters [#CONDITION_TAIL] to the UNLESS-kind arms and
    /// consumes the trailing comma.
    static final Parser<Condition> UNLESS_PREFIX_CONDITION = CONDITION_TAIL
            .suchThat(c -> c.kind() == Condition.Kind.UNLESS, "unless-kind condition")
            .followedBy(string(","));

    /// "while \<typed-condition\>" — trigger-qualifier form of an
    /// AS_LONG_AS intervening condition (Seasoned Warrenguard:
    /// "attacks while you control a token"). No trailing comma — the
    /// comma separating the trigger event from the effect body is
    /// consumed by the trigger-event parser in [OracleParser#TRIGGERED].
    static final Parser<Condition> WHILE_CONDITION =
            CONDITION_TAIL.suchThat(c -> c.kind() == Condition.Kind.AS_LONG_AS, "while-kind condition");

    static final Parser<Effect.CounterSpell> COUNTER_SPELL = phrase("Counter")
            .then(SubjectParsers.SUBJECT)
            .map(Effect.CounterSpell::new)
            .optionallyFollowedBy(CONDITION_TAIL, Effect.CounterSpell::withCondition)
            // Trailing ", where X is <def>" — Rethink: "Counter target
            // spell unless its controller pays {X}, where X is its
            // mana value."
            .optionallyFollowedBy(CountOfParsers.WHERE_X_IS, Effect.CounterSpell::withXDefinition);

    /// "During your turn," — duration prefix that applies
    /// [Duration#duringYourTurn()] to the effect it precedes
    /// (e.g., Sporeback Wolf, Daggersail Aeronaut). Package-visible
    /// so extracted sibling parsers can reference it.
    static final Parser<Duration> DURING_YOUR_TURN =
            phrase("During your turn,").thenReturn(Duration.Fixed.DURING_YOUR_TURN);

    /// "During turns other than yours," — duration prefix scoped to
    /// turns belonging to another player (e.g., Mesa Lynx).
    /// Package-visible so extracted sibling parsers can reference it.
    static final Parser<Duration> DURING_OTHERS_TURN = phrase("During turns other than yours")
            .followedBy(string(","))
            .thenReturn(Duration.Fixed.DURING_OTHERS_TURN);

    /// "As long as <predicate>," — duration prefix applied to the following
    /// effect (e.g., Dwarfhold Champion: "As long as this creature is
    /// equipped, it gets +0/+2."). Mirrors the suffix form [#AS_LONG_AS]
    /// but fronts the clause before the effect; the predicate runs to the
    /// comma. Package-visible for extracted sibling parsers.
    static final Parser<Duration.ForAsLongAs> AS_LONG_AS_PREFIX = phrase("As long as")
            // Use AS_LONG_AS_TOKEN (admits "+/-") so signed P/T markers
            // round-trip (Tenacious Hunter: "as long as a creature has
            // a -1/-1 counter on it, …").
            .then(AS_LONG_AS_TOKEN.atLeastOnce().map(words -> String.join(" ", words)))
            .followedBy(string(","))
            .map(Duration.ForAsLongAs::new);

    /// Inlined form of the "Until end of turn," prefix — package-
    /// visible so extracted sibling parsers can reference it.
    static final Parser<Duration> UNTIL_END_OF_TURN_PREFIX_INLINE =
            phrase("Until end of turn,").thenReturn(Duration.Fixed.UNTIL_END_OF_TURN);

    /// "[subject] <verb-body>" — a subject-less object-verb body, rebound
    /// to the subject captured by [#SUBJECT_AND_VERB_CHAIN]. Mirrors
    /// [#PLAYER_VERB_BODY] but for object-targeting verbs (gains /
    /// has / loses abilities, can't attack/block, must-attack each
    /// combat).
    private static Parser<Effect> objectVerbBody(Subject subj) {
        return Parser.<Effect>anyOf(
                // "get +P/+T [for each X]?" — optional count-scaled tail on
                // the P/T modifier so a chained "and <verb2>" can follow
                // (Hold the Gates: "Creatures you control get +0/+1 for
                // each Gate you control and have vigilance.").
                phrase("get(s)")
                        .then(PtModifierParsers.PT_MODIFIER)
                        .<Effect>map(mod -> new Effect.ModifyPT(subj, mod))
                        .optionallyFollowedBy(
                                CountOfParsers.FOR_EACH, (e, each) -> ((Effect.ModifyPT) e).withScaleBy(each)),
                phrase("[is|are|becomes|become] every")
                        .then(anyOf(
                                phrase("creature type").thenReturn(Effect.IsEveryType.TypeKind.CREATURE),
                                // "basic land type" must precede "land
                                // type" (Prismatic Omen: "Lands you
                                // control are every basic land type…").
                                phrase("basic land type").thenReturn(Effect.IsEveryType.TypeKind.BASIC_LAND),
                                phrase("land type").thenReturn(Effect.IsEveryType.TypeKind.LAND),
                                phrase("enchantment type").thenReturn(Effect.IsEveryType.TypeKind.ENCHANTMENT),
                                phrase("artifact type").thenReturn(Effect.IsEveryType.TypeKind.ARTIFACT),
                                phrase("planeswalker type").thenReturn(Effect.IsEveryType.TypeKind.PLANESWALKER)))
                        .map(kind -> new Effect.IsEveryType(subj, kind)),
                // "gain(s) all <kind> types" — plural all-types form
                // semantically equivalent to "is every <kind> type"
                // (Volatile Claws: "creatures you control get +2/+0
                // and gain all creature types.").
                phrase("gain(s) all")
                        .then(anyOf(
                                phrase("creature types").thenReturn(Effect.IsEveryType.TypeKind.CREATURE),
                                phrase("land types").thenReturn(Effect.IsEveryType.TypeKind.LAND),
                                phrase("enchantment types").thenReturn(Effect.IsEveryType.TypeKind.ENCHANTMENT),
                                phrase("artifact types").thenReturn(Effect.IsEveryType.TypeKind.ARTIFACT),
                                phrase("planeswalker types").thenReturn(Effect.IsEveryType.TypeKind.PLANESWALKER)))
                        .map(kind -> new Effect.IsEveryType(subj, kind)),
                // "is/are/becomes/become <supertype>" — supertype-granting
                // arm inside a chain (On Serra's Wings: "Enchanted
                // creature is legendary, gets +1/+1, and has flying,
                // vigilance, and lifelink."). Must precede the color arm
                // since both share "[is|are|becomes|become]" head.
                phrase("[is|are|becomes|become]").then(SUPERTYPE).map(st -> new Effect.SetSupertype(subj, st)),
                // "is/are/becomes/become <colors>" — color-set (Sinister
                // Strength: "Enchanted creature gets +3/+1 and is
                // black."; Disciple of Kangee: "Target creature gains
                // flying and becomes blue until end of turn."; Wild
                // Mongrel: "becomes the color of your choice until end
                // of turn."). Uses the full SET_COLORS_BODY so all
                // color forms (explicit, colorless, all colors,
                // choice) are handled uniformly.
                phrase("[is|are|becomes|become]")
                        .then(SET_COLORS_BODY)
                        .map(colors -> new Effect.SetColors(subj, colors))
                        .optionallyFollowedBy(
                                phrase("in addition to [its|their] other colors"), (sc, _) -> sc.asAdditional()),
                // "is/are \[a|an\]? <card-type> in addition to its
                // other types" — additive type set inside a chain
                // (Silverskin Armor: "Equipped creature gets +1/+1 and
                // is an artifact in addition to its other types.").
                phrase("[is|are]")
                        .then(anyOf(
                                phrase("[a|an]").then(MtgParsers.andList(CARD_TYPE)), MtgParsers.andList(CARD_TYPE)))
                        .followedBy(phrase("in addition to [its|their] other types"))
                        .map(types -> new Effect.AddCardType(subj, types)),
                // "are [subtype] in addition to their other [CARD_TYPE]?
                // types" — additive subtype assignment inside a chain
                // (Kudo, King Among Bears: "are Bears in addition to
                // their other types."; Dralnu's Crusade: "are Zombies
                // in addition to their other creature types.").
                phrase("[is|are]")
                        .then(MtgParsers.andList(SUBTYPE_WITH_ARTICLE))
                        .followedBy(phrase("in addition to [its|their] other")
                                .then(CARD_TYPE.optional())
                                .followedBy(word("types")))
                        .map(subtypes -> new Effect.AddSubtype(subj, subtypes)),
                // "become(s) a(n) <subtype>" — subtype-set (Wishful
                // Merfolk: "This creature loses defender and becomes
                // a Human until end of turn."). Captured via
                // SetCharacteristic until a structured SetSubtype
                // variant exists.
                phrase("become(s) a(n)")
                        .then(SUBTYPE)
                        .map(st -> new Effect.SetCharacteristic(subj, st.texts().getFirst())),
                phrase("[has|have|gains|gain]")
                        .then(AbilityGainLoseEffectParsers.KEYWORD_OR_QUOTED_LIST)
                        .map(abils -> new Effect.GainAbility(subj, abils)),
                phrase("lose(s)")
                        .then(KeywordParsers.KEYWORD_LIST)
                        .map(abils -> new Effect.LoseAbility(subj, new Effect.LoseAbility.Lost.Specific(abils))),
                // "lose all abilities" tail inside a chain (Humility:
                // "All creatures lose all abilities and have base
                // power and toughness 1/1.").
                phrase("lose(s)")
                        .then(phrase("all abilities"))
                        .thenReturn(new Effect.LoseAbility(subj, Effect.LoseAbility.Lost.All.ALL)),
                // "have/has base \[power|toughness|power and toughness\]" tail
                // (Humility). Mirrors SET_BASE_PT without the subject
                // prefix since the chain has already captured it.
                phrase("[have|has]").then(BASE_PT).map(base -> new Effect.SetBasePT(subj, base)),
                // "attacks <who> <duration>? if able" — directed
                // attack forcing (Alluring Siren: "Target creature an
                // opponent controls attacks you this turn if able.").
                phrase("attack(s)")
                        .then(SubjectParsers.ATOMIC_SUBJECT)
                        .followedBy(phrase("this turn if able"))
                        .map(who -> new Effect.AttackRestriction(
                                subj,
                                new Effect.AttackRestriction.Capability.Must(who),
                                Duration.Fixed.UNTIL_END_OF_TURN)),
                // "attacks this turn if able" — bare duration form,
                // no attack target (Incite: "… becomes red until end
                // of turn and attacks this turn if able.").
                phrase("attack(s) this turn if able")
                        .thenReturn(new Effect.AttackRestriction(
                                subj,
                                new Effect.AttackRestriction.Capability.Must(),
                                Duration.Fixed.UNTIL_END_OF_TURN)),
                phrase("attack(s) each combat")
                        .optionallyFollowedBy(phrase("if able"), (_, _) -> "")
                        .thenReturn(new Effect.AttackRestriction(subj, new Effect.AttackRestriction.Capability.Must())),
                phrase("can't attack")
                        .thenReturn(new Effect.AttackRestriction(subj, Effect.AttackRestriction.Capability.Cant.CANT)),
                // CANT_BE_BLOCKED must precede CANT_BLOCK since both start
                // with "can't b".
                phrase("can't be blocked")
                        .thenReturn(new Effect.CantBeBlocked(subj))
                        // Optional "by/except by/by more than" tail (Dust
                        // Corona: "can't be blocked by creatures with
                        // flying.") — same shape as the standalone
                        // [#CANT_BE_BLOCKED] but shared with the chain body.
                        .optionallyFollowedBy(CANT_BE_BLOCKED_BY, Effect.CantBeBlocked::withBy),
                phrase("can't block").thenReturn(new Effect.CantBlock(subj, ALL_CREATURES)),
                // "can block <capability>" — chain-body parallel of
                // [#CAN_BLOCK] (Give No Ground: "Target creature gets
                // +2/+6 until end of turn and can block any number of
                // creatures this turn.").
                phrase("can block").then(CAN_BLOCK_CAPABILITY).<Effect>map(cap -> new Effect.CanBlock(subj, cap)),
                // "can only attack alone" — Errantry: "Enchanted creature
                // gets +3/+0 and can only attack alone."
                phrase("can only attack alone")
                        .thenReturn(new Effect.AttackRestriction(
                                subj, Effect.AttackRestriction.Capability.OnlyAlone.ONLY_ALONE)),
                // "must be blocked [by <blocker>]? [<duration>]? [if
                // able]?" — shared-subject chain body (Slayer's
                // Cleaver: "Equipped creature gets +3/+1 and must be
                // blocked by an Eldrazi if able."; Compelled Duel:
                // "… and must be blocked this turn if able."). Narrow
                // Parser<MustBeBlocked> so withBy / withDuration land
                // on the record; outer Parser.<Effect>anyOf widens
                // covariantly.
                phrase("must be blocked")
                        .thenReturn(new Effect.MustBeBlocked(subj))
                        .optionallyFollowedBy(word("by").then(SELECTOR), Effect.MustBeBlocked::withBy)
                        .optionallyFollowedBy(DURATION, Effect.MustBeBlocked::withDuration)
                        .optionallyFollowedBy(phrase("if able"), (mb, _) -> mb),
                // "can't be the target of spells or abilities" /
                // "can't be the target of <selector>" — shared-subject
                // chain body (Spectral Shield: "Enchanted creature
                // gets +0/+2 and can't be the target of spells.").
                phrase("can't be the target(s) of spells or abilities")
                        .<Effect>thenReturn(new Effect.CantBeTargeted(subj, null)),
                phrase("can't be the target(s) of")
                        .then(SubjectParsers.SUBJECT)
                        .<Effect>map(by -> new Effect.CantBeTargeted(subj, by)),
                // "can't be enchanted by <selector>" — Aura-binding
                // restriction (Consecrate Land: "Enchanted land …
                // can't be enchanted by other Auras."). The "other"
                // qualifier on the Auras selector is load-bearing —
                // it preserves that the current enchantment is
                // exempt.
                phrase("can't be enchanted by").then(SELECTOR).<Effect>map(by -> new Effect.CantBeEnchanted(subj, by)),
                // "deals [amount] damage to [target]" — chain body for
                // the shared-source damage clause (Reveka, Wizard
                // Savant: "~ deals 2 damage to any target and doesn't
                // untap during your next untap step.").
                sequence(
                        phrase("deal(s)").then(AMOUNT).followedBy(phrase("damage to")),
                        SubjectParsers.SUBJECT,
                        (amt, target) -> new Effect.DealDamage(subj, amt, target)),
                // "[doesn't|don't] untap [during <scope>]?" — chain
                // body (Reveka). Stays Parser<DontUntap>; outer anyOf
                // widens covariantly.
                phrase("[doesn't|don't] untap")
                        .thenReturn(new Effect.DontUntap(subj))
                        .optionallyFollowedBy(DONT_UNTAP_SCOPE, Effect.DontUntap::withScope));
    }

    /// Applies a [to every effect in `list][Duration`] that
    /// carries its own duration slot — ModifyPT, GainAbility, LoseAbility,
    /// CantAttack, CantBlock. Used by [#SUBJECT_AND_VERB_CHAIN] to
    /// fan a trailing "until end of turn" out to both halves of the chain
    /// (Flowstone Strike: "Target creature gets +1/-1 and gains haste
    /// until end of turn.").
    private static List<Effect> applyDurationToAll(List<Effect> list, Duration d) {
        return list.stream()
                .map(e -> switch (e) {
                    case Effect.ModifyPT pt -> pt.withDuration(d);
                    case Effect.GainAbility ga -> ga.withDuration(d);
                    case Effect.LoseAbility la -> la.withDuration(d);
                    case Effect.AttackRestriction ar -> ar.withDuration(d);
                    case Effect.SetBasePT bpt -> bpt.withDuration(d);
                    case Effect.SetColors sc -> sc.withDuration(d);
                    case Effect.SetCharacteristic ch -> ch.withDuration(d);
                    default -> e;
                })
                .toList();
    }

    /// "[subject] <verb1> and <verb2> [until end of turn]?" — a shared
    /// object subject distributed over two object-verb bodies joined by
    /// "and" (Sky Tether: "Enchanted creature has defender and loses
    /// flying."; Flowstone Strike: "Target creature gets +1/-1 and gains
    /// haste until end of turn."). Returns the pair as a `List<Effect>`
    /// so [OracleParser#EFFECT_SEQUENCE] flattens it — the chain
    /// itself isn't a single effect. An optional trailing [#DURATION]
    /// is fanned out to every half that carries a duration field.
    /// Applies an optional trailing DURATION to one object-verb body
    /// — so each half of a [#SUBJECT_AND_VERB_CHAIN_CORE] can carry
    /// its own duration (Incite: "Target creature becomes red until
    /// end of turn and attacks this turn if able.").
    private static Parser<Effect> objectVerbBodyWithDuration(Subject subj) {
        return objectVerbBody(subj).optionallyFollowedBy(DURATION, (e, d) -> applyDurationToAll(List.of(e), d)
                .getFirst());
    }

    /// Core chain body without any duration prefix/suffix — two
    /// subject-less verb bodies joined by "and" under a shared subject.
    /// Each half optionally absorbs its own trailing duration so
    /// asymmetric chains ("becomes red until end of turn and attacks
    /// this turn if able") round-trip with per-half durations.
    /// An optional "each" distributive marker before the first verb
    /// is accepted (Press the Advantage: "Up to two target creatures
    /// each get +2/+2 and gain trample until end of turn.").
    private static final Parser<List<Effect>> SUBJECT_AND_VERB_CHAIN_CORE = SubjectParsers.SUBJECT.flatMap(subj -> {
        var body = objectVerbBodyWithDuration(subj);
        var first = anyOf(word("each").then(body), body);
        // Oxford-comma list: A and B, A, B, and C (Arachnoform:
        // "Enchanted creature gets +2/+2, has reach, and is every
        // creature type."). Require ≥2 bodies so the bare
        // single-effect case falls through to the outer EFFECT.
        return anyOf(
                sequence(first.followedBy(",").atLeastOnce(), word("and").then(body), (heads, tail) -> {
                    var list = new ArrayList<Effect>(heads);
                    list.add(tail);
                    return List.copyOf(list);
                }),
                sequence(first.followedBy(word("and")), body, (a, b) -> List.of(a, b)));
    });

    static final Parser<List<Effect>> SUBJECT_AND_VERB_CHAIN = anyOf(
                    // "As long as [cond], [subject] <v1> and <v2>." —
                    // Kitesail Apprentice: "As long as this creature is
                    // equipped, it gets +1/+1 and has flying."
                    sequence(AS_LONG_AS_PREFIX, SUBJECT_AND_VERB_CHAIN_CORE, (d, list) -> applyDurationToAll(list, d)),
                    // "During your turn, [subject] <v1> and <v2>." —
                    // Street Riot.
                    sequence(DURING_YOUR_TURN, SUBJECT_AND_VERB_CHAIN_CORE, (d, list) -> applyDurationToAll(list, d)),
                    // "Until end of turn, [subject] <v1> and <v2>." —
                    // Turtle-Duck: "Until end of turn, this creature has
                    // base power 4 and gains trample."
                    sequence(
                            UNTIL_END_OF_TURN_PREFIX_INLINE,
                            SUBJECT_AND_VERB_CHAIN_CORE,
                            (d, list) -> applyDurationToAll(list, d)),
                    SUBJECT_AND_VERB_CHAIN_CORE)
            .optionallyFollowedBy(DURATION, EffectParsers::applyDurationToAll);

    // P/T modification

    /// MODIFY_PT core — accepts the distributive "each" between a plural
    /// subject and the verb (Sick and Tired / Symbiosis: "Two target
    /// creatures each get …") via the shared [#each] helper.
    private static final Parser<Effect.ModifyPT> MODIFY_PT_CORE = Parser.sequence(
                    SubjectParsers.SUBJECT.followedBy(DamageEffectParsers.each(phrase("get(s)"))),
                    PtModifierParsers.PT_MODIFIER,
                    Effect.ModifyPT::new)
            .optionallyFollowedBy(CountOfParsers.FOR_EACH_AMOUNT, Effect.ModifyPT::withScaleBy)
            .optionallyFollowedBy(CountOfParsers.WHERE_X_IS, Effect.ModifyPT::withXDefinition);

    static final Parser<Effect.ModifyPT> MODIFY_PT = anyOf(
                    sequence(DURING_YOUR_TURN, MODIFY_PT_CORE, (d, m) -> m.withDuration(d)),
                    sequence(DURING_OTHERS_TURN, MODIFY_PT_CORE, (d, m) -> m.withDuration(d)),
                    sequence(AS_LONG_AS_PREFIX, MODIFY_PT_CORE, (d, m) -> m.withDuration(d)),
                    // "Until end of turn, [subject] gets +N/+M" —
                    // duration-prefixed form (Rookie Mistake).
                    sequence(UNTIL_END_OF_TURN_PREFIX_INLINE, MODIFY_PT_CORE, (d, m) -> m.withDuration(d)),
                    MODIFY_PT_CORE)
            .optionallyFollowedBy(DURATION, Effect.ModifyPT::withDuration)
            // Allow "for each X" and "where X is …" clauses after a
            // trailing duration (Might of the Nephilim / Mutilate:
            // "gets +N/+M until end of turn for each …"; Rush of Blood:
            // "gets +X/+0 until end of turn, where X is its power.").
            .optionallyFollowedBy(CountOfParsers.FOR_EACH_AMOUNT, Effect.ModifyPT::withScaleBy)
            .optionallyFollowedBy(CountOfParsers.WHERE_X_IS, Effect.ModifyPT::withXDefinition);

    // Control

    /// "[player] gain[s] control of [target] [duration]." — active-voice
    /// transfer (e.g., Mind Control; Crag Saurian: "that source's controller
    /// gains control of this creature"). Accepts any player-like subject,
    /// including possessives ("that source's controller"), so it also covers
    /// Crag Saurian-style triggers where the damage source's controller
    /// is the actor.
    private static final Parser<Effect.GainControl> GAIN_CONTROL_ACTIVE = Parser.sequence(
            SubjectParsers.PLAYER_LIKE_SUBJECT,
            phrase("gain(s) control of").then(SubjectParsers.SUBJECT),
            Effect.GainControl::new);

    /// "Gain control of [target]." — implicit-you variant (e.g., Entrancing
    /// Melody: "Gain control of target creature with mana value X.").
    private static final Parser<Effect.GainControl> GAIN_CONTROL_YOU = phrase("Gain")
            .then(phrase("control of"))
            .then(SubjectParsers.SUBJECT)
            .map(target -> new Effect.GainControl(YOU, target));

    /// "[player] control[s] [target]." — static-ability control grant (e.g.,
    /// Conquer: "You control enchanted land.").
    private static final Parser<Effect.GainControl> GAIN_CONTROL_STATIC = Parser.sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("control(s)")),
            SubjectParsers.SUBJECT,
            Effect.GainControl::new);

    static final Parser<Effect.GainControl> GAIN_CONTROL = anyOf(
                    GAIN_CONTROL_ACTIVE, GAIN_CONTROL_YOU, GAIN_CONTROL_STATIC)
            .optionallyFollowedBy(DURATION, Effect.GainControl::withDuration);

    // Tokens

    /// "Create \[tapped\]? \[amount\] \[token\]" — subject-less imperative
    /// body. Reused by the shared-actor MAY chain ("you may create a
    /// 1/1 …" — Lys Alana Huntmaster) so the "may" actor rebinds as the
    /// token's creator via the enclosing subject.
    static final Parser<Effect.CreateToken> CREATE_TOKEN_NO_PLAYER = anyOf(
            sequence(
                    phrase("Create").then(AMOUNT).followedBy(word("tapped")),
                    TokenDescriptionParsers.TOKEN_DESCRIPTION,
                    (amt, td) -> new Effect.CreateToken(amt, td, true)),
            sequence(
                    phrase("Create").then(AMOUNT), TokenDescriptionParsers.TOKEN_DESCRIPTION, Effect.CreateToken::new));

    static final Parser<Effect.CreateToken> CREATE_TOKEN = anyOf(
                    // "[player] create(s) [tapped]? [token]" — explicit
                    // player-actor form (Seed the Land: "its controller
                    // creates a 1/1 green Snake creature token."). Must
                    // precede the imperative arms since PLAYER_LIKE_SUBJECT
                    // is matched first. `tapped` arm comes first so the
                    // literal "tapped" isn't left unconsumed.
                    sequence(
                            SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("create(s)")),
                            AMOUNT.followedBy(word("tapped")),
                            TokenDescriptionParsers.TOKEN_DESCRIPTION,
                            (creator, amt, td) -> new Effect.CreateToken(creator, amt, td, true)),
                    sequence(
                            SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("create(s)")),
                            AMOUNT,
                            TokenDescriptionParsers.TOKEN_DESCRIPTION,
                            (creator, amt, td) -> new Effect.CreateToken(creator, amt, td, false)),
                    CREATE_TOKEN_NO_PLAYER)
            // Optional scaling "for each X" tail (Howl of the Night Pack:
            // "Create a 2/2 green Wolf creature token for each Forest you
            // control."). Replaces the base count with the count-of.
            .optionallyFollowedBy(
                    CountOfParsers.FOR_EACH,
                    (ct, each) -> new Effect.CreateToken(ct.creator(), each, ct.token(), ct.tapped()))
            // Optional ", where X is <def>" tail — binds the X in a
            // variable count (Storm Herd: "Create X 1/1 white Pegasus
            // creature tokens with flying, where X is your life
            // total.").
            .optionallyFollowedBy(CountOfParsers.WHERE_X_IS, Effect.CreateToken::withXDefinition);

    /// "Create [amt] [token] and [amt] [token]" — two distinct tokens in
    /// one instruction (Forbidden Friendship: "Create a 1/1 red Dinosaur
    /// creature token with haste and a 1/1 white Human Soldier creature
    /// token."). The second token has no "Create" verb, so EFFECT_SEQUENCE's
    /// "and" delimiter can't split it — it's a within-sentence list.
    /// Debt: generalize to `phrase("Create").then(
    /// sequence(AMOUNT, TOKEN_DESCRIPTION).atLeastOnceDelimitedBy(andList))`
    /// if three-token create instructions appear.
    static final Parser<List<Effect>> CREATE_TOKEN_PAIR = sequence(
            phrase("Create").then(AMOUNT),
            TokenDescriptionParsers.TOKEN_DESCRIPTION,
            sequence(word("and").then(AMOUNT), TokenDescriptionParsers.TOKEN_DESCRIPTION, Map::entry),
            (amt1, td1, second) -> List.of(
                    new Effect.CreateToken(amt1, td1), new Effect.CreateToken(second.getKey(), second.getValue())));

    // Mana parsing — see [ManaParsers] for [Mana], [Effect.AddMana],
    // and [Effect.SpendThisManaOnly] / [Restriction.SpendOnly].

    private static List<Zone.Named> addZone(List<Zone.Named> list, Zone.Named more) {
        var all = new ArrayList<Zone.Named>(list);
        all.add(more);
        return List.copyOf(all);
    }

    // Transform/Copy

    static final Parser<Effect.Transform> TRANSFORM =
            phrase("Transform").then(SubjectParsers.SUBJECT).map(Effect.Transform::new);

    static final Parser<Effect.Copy> COPY =
            phrase("Copy").then(SubjectParsers.SUBJECT).map(Effect.Copy::new);

    // Combat

    static final Parser<Effect.Fight> FIGHT = sequence(
                    SubjectParsers.SUBJECT.followedBy(phrase("fight(s)")), SubjectParsers.SUBJECT, Effect.Fight::new)
            // "chosen at random" — random-target variant (Scab-Clan
            // Giant: "fights target creature an opponent controls
            // chosen at random.").
            .optionallyFollowedBy(phrase("chosen at random"), (f, _) -> f.asRandom());

    /// "[subject] phase(s) in" / "[subject] phase(s) out" — phasing
    /// flip effects (rule 702.26; Time and Tide).
    static final Parser<Effect.PhaseIn> PHASE_IN =
            SubjectParsers.SUBJECT.followedBy(phrase("phase(s) in")).map(Effect.PhaseIn::new);

    static final Parser<Effect.PhaseOut> PHASE_OUT =
            SubjectParsers.SUBJECT.followedBy(phrase("phase(s) out")).map(Effect.PhaseOut::new);

    /// "Investigate [count]?" — Investigate keyword action (rule 701.27).
    /// "Investigate an additional time" (Erdwal Illuminator) yields
    /// count=1 — the trigger frequency-limit handles the chained extras.
    static final Parser<Effect.Investigate> INVESTIGATE = anyOf(
            phrase("Investigate an additional time").thenReturn(new Effect.Investigate(Amount.exact(1))),
            phrase("Investigate twice").thenReturn(new Effect.Investigate(Amount.exact(2))),
            phrase("Investigate").thenReturn(new Effect.Investigate()));

    /// "Proliferate [twice]?" — proliferate keyword action (rule 701.25).
    /// "proliferate twice" (Ezuri, Stalker of Spheres) yields count=2.
    static final Parser<Effect.Proliferate> PROLIFERATE = anyOf(
            phrase("Proliferate twice").thenReturn(new Effect.Proliferate(Amount.exact(2))),
            phrase("Proliferate").thenReturn(new Effect.Proliferate()));

    /// "discover [again]? [N|for the same value]" — discover keyword action
    /// (rule 701.52). "discover again for the same value" (Curator of Sun's
    /// Creation) back-references the triggering discover value via
    /// [Amount.reference].
    static final Parser<Effect.Discover> DISCOVER = anyOf(
            phrase("discover again for the same value")
                    .thenReturn(new Effect.Discover(Amount.reference("the same value"))),
            phrase("Discover").then(AmountParsers.AMOUNT).map(Effect.Discover::new));

    // Win/Loss

    private static final Parser<String> WIN_GAME_NO_PLAYER = phrase("win(s) the game");

    static final Parser<Effect.WinGame> WIN_GAME = anyOf(
            SubjectParsers.PLAYER_SUBJECT.followedBy(WIN_GAME_NO_PLAYER).map(Effect.WinGame::new),
            WIN_GAME_NO_PLAYER.thenReturn(new Effect.WinGame(YOU)));

    private static final Parser<String> LOSE_GAME_NO_PLAYER = phrase("lose(s) the game");

    static final Parser<Effect.LoseGame> LOSE_GAME = anyOf(
            SubjectParsers.PLAYER_SUBJECT.followedBy(LOSE_GAME_NO_PLAYER).map(Effect.LoseGame::new),
            LOSE_GAME_NO_PLAYER.thenReturn(new Effect.LoseGame(YOU)));

    /// "[player] can't win the game." / "[player] can't lose the game."
    /// — Platinum Angel. Must precede [#WIN_GAME] / [#LOSE_GAME]
    /// so the "can't" prefix wins before the bare verb does.
    static final Parser<Effect.CantWinGame> CANT_WIN_GAME = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("can't")),
            phrase("win the game"),
            (player, _) -> new Effect.CantWinGame(player));

    static final Parser<Effect.CantLoseGame> CANT_LOSE_GAME = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("can't")),
            phrase("lose the game"),
            (player, _) -> new Effect.CantLoseGame(player));

    // Zone movement

    /// "\[player\]? put\[s\] \[subject\] \[from \[zone\]\]? \[destination\]." —
    /// covers both the bare "Put \[subject\] \[destination\]" form and the
    /// sourced / player-actor variants (Reclaim: "Put target card from
    /// your graveyard on top of your library."; Exhume: "Each player
    /// puts a creature card from their graveyard onto the
    /// battlefield."). The player actor is consumed as flavor; only
    /// the subject + source + destination are preserved.
    static final Parser<Effect.ZoneMove> ZONE_MOVE = Parser.<Effect.ZoneMove>anyOf(
                    // "[player] puts [subject] from [zone] [destination]" —
                    // player-actor + from-zone (Exhume).
                    sequence(
                            SubjectParsers.PLAYER_LIKE_SUBJECT
                                    .followedBy(phrase("put(s)"))
                                    .then(SubjectParsers.SUBJECT),
                            ZoneExpressionParsers.IN_ZONE_FROM.<Zone.Source>map(Zone.Source::fromZone),
                            ZoneParsers.ZONE_DESTINATION,
                            Effect.ZoneMove::new),
                    // "[player] puts [subject] [destination]" — player-actor,
                    // no from-zone (Misleading Motes: "Target creature's owner
                    // puts it on their choice of the top or bottom of their
                    // library."). Uses PLAYER_LIKE_SUBJECT so possessives
                    // ("target creature's owner") fit.
                    sequence(
                            SubjectParsers.PLAYER_LIKE_SUBJECT
                                    .followedBy(phrase("put(s)"))
                                    .then(SubjectParsers.SUBJECT),
                            ZoneParsers.ZONE_DESTINATION,
                            (subject, dest) -> new Effect.ZoneMove(subject, null, dest)),
                    sequence(
                            phrase("Put").then(SubjectParsers.SUBJECT),
                            // PLAYER_ZONE_FROM ("from an opponent's
                            // graveyard" — Ashen Powder) precedes the
                            // generic IN_ZONE_FROM since it consumes a
                            // strictly longer player-possessive prefix.
                            anyOf(
                                    ZoneExpressionParsers.PLAYER_ZONE_FROM,
                                    ZoneExpressionParsers.IN_ZONE_FROM.<Zone.Source>map(Zone.Source::fromZone)),
                            ZoneParsers.ZONE_DESTINATION,
                            Effect.ZoneMove::new),
                    // "Put [subject] [destination] from [source]" —
                    // destination-first variant (Command Beacon: "Put
                    // your commander into your hand from the command
                    // zone."). Must precede the bare destination-only
                    // arm so the trailing "from <zone>" isn't dropped.
                    sequence(
                            phrase("Put").then(SubjectParsers.SUBJECT),
                            ZoneParsers.ZONE_DESTINATION,
                            ZoneParsers.ZONE_SOURCE,
                            (subject, dest, source) -> new Effect.ZoneMove(subject, source, dest)),
                    sequence(
                            phrase("Put").then(SubjectParsers.SUBJECT),
                            ZoneParsers.ZONE_DESTINATION,
                            (subject, dest) -> new Effect.ZoneMove(subject, null, dest)))
            // Optional "in any order" tail — signals player orders the
            // moved group on an ordered destination (Brainsurge: "put
            // two cards from your hand on top of your library in any
            // order.").
            .optionallyFollowedBy(phrase("in any order"), (zm, _) -> zm.inAnyOrder());

    // Combat restrictions

    /// Universal "all creatures" subject used when a block restriction
    /// omits an explicit target ("can't block" == "can't block any
    /// creature").
    private static final Subject ALL_CREATURES = Subject.select(new Selector(
            Selector.Quantifier.all(),
            List.of(new Selector.Qualifier.Types(TypeMatcher.CREATURE)),
            GameObjectType.PERMANENT));

    /// "[subject] can't block [what] [duration]." — what defaults to
    /// [duration defaults to null. `what][#ALL_CREATURES`,] is
    /// a full [Subject] so "this creature" / "that creature" /
    /// self-references parse alongside selectors.
    static final Parser<Effect> CANT_BLOCK = SubjectParsers.SUBJECT
            .followedBy(phrase("can't block"))
            .map(s -> new Effect.CantBlock(s, ALL_CREATURES))
            .optionallyFollowedBy(SubjectParsers.SUBJECT, Effect.CantBlock::withWhat)
            .optionallyFollowedBy(DURATION, Effect.CantBlock::withDuration)
            .map(e -> (Effect) e);

    static final Parser<Effect.AttackRestriction> CANT_ATTACK = SubjectParsers.SUBJECT
            .followedBy(phrase("can't attack"))
            .map(s -> new Effect.AttackRestriction(s, Effect.AttackRestriction.Capability.Cant.CANT))
            .optionallyFollowedBy(DURATION, Effect.AttackRestriction::withDuration);

    /// "Except for [except], [subject] can't attack." — Akron Legionnaire:
    /// "Except for creatures named ~ and artifact creatures, creatures you
    /// control can't attack." The leading "Except for [except]," names the
    /// subjects exempt from the restriction; they are stored in
    /// [Effect.AttackRestriction#except].
    static final Parser<Effect.AttackRestriction> CANT_ATTACK_EXCEPT_FOR = sequence(
            phrase("Except for").then(SubjectParsers.SUBJECT).followedBy(string(",")),
            SubjectParsers.SUBJECT.followedBy(phrase("can't attack")),
            (except, restricted) -> new Effect.AttackRestriction(
                            restricted, Effect.AttackRestriction.Capability.Cant.CANT)
                    .withExcept(except));

    /// "[player] take[s] the initiative." — Aarakocra Sneak.
    static final Parser<Effect.TakeInitiative> TAKE_INITIATIVE = SubjectParsers.PLAYER_SUBJECT
            .followedBy(phrase("take(s) the initiative"))
            .map(Effect.TakeInitiative::new);

    /// "[player]? venture[s] into the dungeon." — Clattering Skeletons.
    /// The player defaults to YOU when omitted (most trigger bodies).
    static final Parser<Effect.VentureIntoDungeon> VENTURE_INTO_DUNGEON = anyOf(
            SubjectParsers.PLAYER_SUBJECT
                    .followedBy(phrase("venture(s) into the dungeon"))
                    .map(Effect.VentureIntoDungeon::new),
            phrase("venture(s) into the dungeon").thenReturn(new Effect.VentureIntoDungeon(YOU)));

    /// "[subject] don't untap [during <scope>]?." — Choke. The optional
    /// scope (e.g., "during their controllers' untap steps") is captured
    /// as free text as a compact placeholder for the structured timing.
    private static final Parser<String> DONT_UNTAP_SCOPE =
            phrase("During").then(CONDITION_TOKEN.atLeastOnce().map(words -> String.join(" ", words)));

    static final Parser<Effect.DontUntap> DONT_UNTAP = SubjectParsers.SUBJECT
            .followedBy(phrase("[doesn't|don't] untap"))
            .map(Effect.DontUntap::new)
            .optionallyFollowedBy(DONT_UNTAP_SCOPE, Effect.DontUntap::withScope);

    /// "[player] can't untap more than [amount] [selector] [during scope]?."
    /// — Mungha Wurm.
    static final Parser<Effect.UntapLimit> UNTAP_LIMIT = sequence(
                    SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("can't untap more than")),
                    AMOUNT,
                    SELECTOR,
                    Effect.UntapLimit::new)
            .optionallyFollowedBy(DONT_UNTAP_SCOPE, Effect.UntapLimit::withScope);

    /// "You can cast only [amount] more spell[s] [duration]?." —
    /// Irencrag Feat.
    static final Parser<Effect.CastCountLimit> CAST_COUNT_LIMIT = Parser.sequence(
                    SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("can cast only")),
                    AMOUNT.followedBy(word("more")).followedBy(phrase("spell(s)")),
                    Effect.CastCountLimit::new)
            .optionallyFollowedBy(DURATION, (cl, d) -> new Effect.CastCountLimit(cl.player(), cl.max(), d));

    /// "[player] can't play lands [duration]?." — Turf Wound.
    static final Parser<Effect.CantPlayLands> CANT_PLAY_LANDS = SubjectParsers.SUBJECT
            .followedBy(phrase("can't play lands"))
            .map(Effect.CantPlayLands::new)
            .optionallyFollowedBy(DURATION, Effect.CantPlayLands::withDuration);

    /// "base \[power|toughness|power and toughness\] \[value\]" — the
    /// value payload for a [Effect.SetBasePT] (rule 613.4, layer
    /// 7b). The word *base* is required; it's the signal that this
    /// sets rather than modifies. Asymmetric shapes leave the
    /// unspecified side null in the resulting [PtValue].
    private static final Parser<PtValue> BASE_PT = anyOf(
            phrase("base power and toughness").then(PT_VALUE),
            phrase("base power").then(AMOUNT).map(p -> new PtValue(p, null)),
            phrase("base toughness").then(AMOUNT).map(t -> new PtValue(null, t)));

    static final Parser<Effect.SetBasePT> SET_BASE_PT = Parser.sequence(
                    SubjectParsers.SUBJECT.followedBy(phrase("[have|has]")),
                    BASE_PT,
                    (t, pt) -> new Effect.SetBasePT(t, pt))
            .optionallyFollowedBy(DURATION, Effect.SetBasePT::withDuration)
            // "where X is …" — binds X in a variable base P/T (Aettir
            // and Priwen: "has base power and toughness X/X, where X
            // is your life total.").
            .optionallyFollowedBy(CountOfParsers.WHERE_X_IS, Effect.SetBasePT::withXDefinition);

    /// "[subject] has base power N or base toughness M" — disjunctive
    /// base-P/T set (Vhati il-Dal: "{T}: Until end of turn, target
    /// creature has base power 1 or base toughness 1."). Captured as
    /// a free-text [Effect.SetCharacteristic] so the OR-semantic
    /// (chooser picks one) is preserved in the rendered description.
    private static final Parser<Effect.SetCharacteristic> SET_BASE_PT_OR_CORE = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("[have|has] base power")),
            AMOUNT.followedBy(phrase("or base toughness")),
            AMOUNT,
            (subj, p, t) -> new Effect.SetCharacteristic(subj, "base power " + p + " or base toughness " + t));

    static final Parser<Effect.SetCharacteristic> SET_BASE_PT_OR = anyOf(
                    // "Until end of turn, <subject> has base power N or base toughness M."
                    sequence(UNTIL_END_OF_TURN_PREFIX_INLINE, SET_BASE_PT_OR_CORE, (d, e) -> e.withDuration(d)),
                    SET_BASE_PT_OR_CORE)
            .optionallyFollowedBy(DURATION, Effect.SetCharacteristic::withDuration);

    /// "Change \[subject\]'s base power to \[amount\]." — sets only the
    /// base power of a permanent to a new value that may be a property
    /// reference (Riptide Mangler: "Change this creature's base power to
    /// target creature's power."). The toughness side is left null in
    /// [PtValue] since the oracle text does not mention it.
    static final Parser<Effect.SetBasePT> CHANGE_BASE_POWER = sequence(
            phrase("Change")
                    .then(SubjectParsers.SUBJECT)
                    .followedBy(string("'s"))
                    .followedBy(phrase("base power to")),
            anyOf(CountOfParsers.PROPERTY_OF_AMOUNT, AMOUNT),
            (target, newPower) -> new Effect.SetBasePT(target, new PtValue(newPower, null)));

    /// "Players don't lose unspent mana as steps and phases end." —
    /// Upwelling. Captures the full phrase shape; the unique effect
    /// doesn't parameterize further.
    static final Parser<Effect.ManaPoolPersists> MANA_POOL_PERSISTS = SubjectParsers.PLAYER_SUBJECT
            .followedBy(phrase("don't lose unspent mana as steps and phases end"))
            .map(Effect.ManaPoolPersists::new);

    /// "\[player\] loses all unspent mana." — empties the player's
    /// mana pool (Mana Short).
    static final Parser<Effect.LoseUnspentMana> LOSE_UNSPENT_MANA = SubjectParsers.PLAYER_SUBJECT
            .followedBy(phrase("loses all unspent mana"))
            .map(Effect.LoseUnspentMana::new);

    /// "Spend only mana produced by [selector] to cast this spell." —
    /// Myr Superion.
    static final Parser<Effect.ManaSpendRestriction> MANA_SPEND_RESTRICTION = phrase("Spend only mana produced by")
            .then(SELECTOR)
            .followedBy(phrase("to cast this spell"))
            .map(Effect.ManaSpendRestriction::new);

    /// "Spend only \[color\] mana on X." — Crimson Hellkite. Restricts
    /// which mana may pay the X portion of the activation cost. Distinct
    /// from [#MANA_SPEND_RESTRICTION] (whole-cost restriction by source).
    static final Parser<Effect.SpendOnlyOnX> SPEND_ONLY_ON_X =
            phrase("Spend only").then(COLOR).followedBy(phrase("mana on X")).map(Effect.SpendOnlyOnX::new);

    /// "As an additional cost to cast this spell, [cost]." — Mardu
    /// Outrider. The leading "As an additional cost to cast this spell,"
    /// is consumed as flavor; only the cost expression is retained.
    static final Parser<Effect.AdditionalCost> ADDITIONAL_COST = phrase("As an additional cost to cast this spell")
            .then(string(","))
            .then(CostParsers.COST_EXPRESSION)
            .map(Effect.AdditionalCost::new);

    /// "No more than N creatures can attack \[whom\] each combat." —
    /// Crawlspace (with player), Dueling Grounds/Silent Arbiter (no player).
    /// The trailing "each combat" is consumed as flavor.
    static final Parser<Effect.AttackLimit> ATTACK_LIMIT = phrase("No more than")
            .then(AMOUNT)
            .followedBy(phrase("creature(s) can attack"))
            .map(Effect.AttackLimit::new)
            .optionallyFollowedBy(SubjectParsers.PLAYER_SUBJECT, Effect.AttackLimit::withWhom)
            .followedBy(phrase("each combat"));

    /// "No more than N creature(s) can block each combat." — block-count limit
    /// (Dueling Grounds, Silent Arbiter).
    static final Parser<Effect.BlockLimit> BLOCK_LIMIT = phrase("No more than")
            .then(AMOUNT)
            .followedBy(phrase("creature(s) can block each combat"))
            .map(Effect.BlockLimit::new);

    /// "[player] must attack with at least [N] [selector] [each combat|turn]? [if able]?" —
    /// minimum-attacker requirement on players (Seeker of Slaanesh: "Each opponent
    /// must attack with at least one creature each combat if able."). `minimum` is
    /// [Amount.AtLeast] built from the inline "at least N" prefix;
    /// `attackerType` captures the creature selector that follows.
    static final Parser<Effect.AttackMinimum> MUST_ATTACK_MINIMUM = sequence(
                    SubjectParsers.PLAYER_SUBJECTS.followedBy(phrase("must attack with at least")),
                    NUMBER,
                    SELECTOR,
                    (players, n, sel) -> new Effect.AttackMinimum(players, new Amount.AtLeast(n), sel))
            .optionallyFollowedBy(phrase("each [combat|turn]"), (e, _) -> e)
            .optionallyFollowedBy(phrase("if able"), (e, _) -> e);

    /// "Until end of turn," — duration prefix used before certain
    /// temporary effects (e.g., Exponential Growth: "Until end of turn,
    /// double target creature's power X times.").
    private static final Parser<Duration> UNTIL_END_OF_TURN_PREFIX =
            phrase("Until end of turn,").thenReturn(Duration.Fixed.UNTIL_END_OF_TURN);

    /// Core of a "double …" P/T phrase. Supports both orders that appear
    /// in oracle text: "double the [stat] of [subject]" (Unleash Fury) and
    /// "double [subject]'s [stat]" (Exponential Growth).
    private static final Parser<boolean[]> DOUBLE_STAT_CHOICE = anyOf(
            phrase("Power and toughness").thenReturn(new boolean[] {true, true}),
            phrase("Power").thenReturn(new boolean[] {true, false}),
            phrase("Toughness").thenReturn(new boolean[] {false, true}));

    private static final Parser<Effect.DoublePT> DOUBLE_PT_CORE = anyOf(
            sequence(
                    phrase("Double").then(word("the")).then(DOUBLE_STAT_CHOICE),
                    word("of").then(SubjectParsers.SUBJECT),
                    (flags, subj) -> new Effect.DoublePT(subj, flags[0], flags[1])),
            sequence(
                    phrase("Double").then(SubjectParsers.SUBJECT).followedBy(string("'s")),
                    DOUBLE_STAT_CHOICE,
                    (subj, flags) -> new Effect.DoublePT(subj, flags[0], flags[1])));

    /// "[Until end of turn,]? Double the [power|toughness|…] of [subject]
    /// [N times]? [duration]?." — Unleash Fury, Berserk, Exponential
    /// Growth.
    static final Parser<Effect.DoublePT> DOUBLE_PT = anyOf(
                    sequence(UNTIL_END_OF_TURN_PREFIX, DOUBLE_PT_CORE, (d, m) -> m.withDuration(d)), DOUBLE_PT_CORE)
            .optionallyFollowedBy(AMOUNT.followedBy(phrase("time(s)")), Effect.DoublePT::withTimes)
            .optionallyFollowedBy(DURATION, Effect.DoublePT::withDuration);

    /// "Double \[player\]'s life total." — Beacon of Immortality. Distinct
    /// record from [Effect.DoublePT] / [Effect.DoubleMana]; player life is
    /// its own concept under rule 119.
    static final Parser<Effect.DoubleLifeTotal> DOUBLE_LIFE_TOTAL = phrase("Double")
            .then(SubjectParsers.SUBJECT)
            .followedBy(string("'s"))
            .followedBy(phrase("life total"))
            .map(Effect.DoubleLifeTotal::new);

    /// "Double the number of each kind of counter on \[target\]." — Vorel
    /// of the Hull Clade. Doubles every counter type's count on the
    /// target. The verb-amount-pattern wording is fixed; only the
    /// target subject varies.
    static final Parser<Effect.DoubleCountersOn> DOUBLE_COUNTERS_ON = phrase(
                    "Double the number of each kind of counter on")
            .then(SubjectParsers.SUBJECT)
            .map(Effect.DoubleCountersOn::new);

    /// "[subject] enter[s] [tapped]? as a copy of [target]." — Essence
    /// of the Wild (plain), Vesuva ("enter tapped as a copy"). The
    /// optional `tapped` modifier sets [Effect.EnterAsCopy#tapped].
    static final Parser<Effect.EnterAsCopy> ENTER_AS_COPY = anyOf(
                    sequence(
                            SubjectParsers.SUBJECT.followedBy(phrase("enter(s) tapped as a copy of")),
                            SubjectParsers.SUBJECT,
                            (subj, copy) -> new Effect.EnterAsCopy(subj, copy, Selector.Qualifier.Status.TAPPED)),
                    sequence(
                            SubjectParsers.SUBJECT.followedBy(phrase("enter(s) as a copy of")),
                            SubjectParsers.SUBJECT,
                            Effect.EnterAsCopy::new))
            // Optional ", except it's <P>/<T>" — Quicksilver
            // Gargantuan: "… except it's 7/7."
            .optionallyFollowedBy(
                    string(",").then(phrase("except it's")).then(PT_VALUE), Effect.EnterAsCopy::withOverridePt);

    /// "[subject] become[s] a copy of [source]" — live copy effect
    /// (Mirrorform: "Each nonland permanent you control becomes a
    /// copy of target non-Aura permanent."). Distinct from
    /// [#ENTER_AS_COPY], which fires on entry.
    static final Parser<Effect.BecomeCopy> BECOME_COPY = sequence(
                    SubjectParsers.SUBJECT.followedBy(phrase("become(s) a copy of")),
                    SubjectParsers.SUBJECT,
                    Effect.BecomeCopy::new)
            // "except [it|they] [has|have] this ability" — Thespian's
            // Stage: "This land becomes a copy of target land, except
            // it has this ability." The copy keeps the source card's
            // ability so the chain remains activatable.
            .optionallyFollowedBy(
                    phrase(", except [it|they] [has|have] this ability"), (bc, _) -> bc.keepingThisAbility())
            .optionallyFollowedBy(DURATION, Effect.BecomeCopy::withDuration);

    // Enter tapped

    static final Parser<Effect.EnterTapped> ENTER_TAPPED = SubjectParsers.SUBJECT
            .followedBy(phrase("enter(s) tapped"))
            .map(Effect.EnterTapped::new)
            .optionallyFollowedBy(DURATION, Effect.EnterTapped::withDuration);

    /// "Support N" — activated-ability form of rule 702.115
    /// (Joraga Auxiliary: "{4}{G}{W}: Support 2."). Distinct from the
    /// printed-keyword ETB trigger [Ability.Support], which fires
    /// automatically on entry.
    static final Parser<Effect.Support> SUPPORT = phrase("Support").then(AMOUNT).map(Effect.Support::new);

    /// "[subject] enter[s] with \[an additional\]? [count] [type] counters
    /// on it \[for each X\]?." — Endless One ("This creature enters
    /// with X +1/+1 counters on it."); Gatekeeper Gargoyle scales the
    /// count via "for each Gate you control."; Grumgully, the Generous
    /// ("an additional +1/+1 counter" — stacks on top of other ETB
    /// counter effects).
    static final Parser<Effect.EnterWithCounters> ENTER_WITH_COUNTERS = anyOf(
                    // "an additional [type] counter" — Grumgully.
                    // Count is implicit 1 (the "an" is the article on
                    // "additional", not a counter count).
                    sequence(
                            SubjectParsers.SUBJECT.followedBy(phrase("enter(s) with an additional")),
                            COUNTER_TYPE.followedBy(phrase("counter(s) on [it|them]")),
                            (subj, type) -> new Effect.EnterWithCounters(subj, Amount.exact(1), type).asAdditional()),
                    // Base form without "additional".
                    sequence(
                            SubjectParsers.SUBJECT.followedBy(phrase("enter(s) with")),
                            AMOUNT,
                            COUNTER_TYPE.followedBy(phrase("counter(s) on [it|them]")),
                            (subj, amt, type) -> new Effect.EnterWithCounters(subj, amt, type)))
            .optionallyFollowedBy(
                    CountOfParsers.FOR_EACH,
                    (ewc, each) -> new Effect.EnterWithCounters(ewc.subject(), each, ewc.type(), ewc.additional()))
            .optionallyFollowedBy(CountOfParsers.WHERE_X_IS, Effect.EnterWithCounters::withXDefinition);

    /// "[subject] enter[s] with [chooser]'s choice of [a|an] X counter or
    /// [a|an] Y counter on it." — Flycatcher Giraffid (chooser = "your").
    /// One counter of the chosen type; the chooser is a player subject
    /// picking from the listed counter types at ETB.
    static final Parser<Effect.EnterWithChosenCounter> ENTER_WITH_CHOSEN_COUNTER = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("enter(s) with")),
            anyOf(
                            word("your").thenReturn(Subject.player(PlayerRef.Pronoun.YOU)),
                            anyOf(word("their"), word("his"), word("her"))
                                    .thenReturn(Subject.player(PlayerRef.Pronoun.THEY)),
                            word("its").thenReturn(Subject.pronoun(PronounType.IT)))
                    .followedBy(phrase("choice of")),
            MtgParsers.orList(phrase("[a|an]").then(COUNTER_TYPE).followedBy(phrase("counter(s)")))
                    .followedBy(phrase("on [it|them]")),
            Effect.EnterWithChosenCounter::new);

    // Characteristic-setting: "[subject] are/is [colors|colorless|subtype]"

    /// Head of a characteristic-setting clause: the subject followed by a
    /// copula verb — "are" / "is" for static characteristics or "becomes" /
    /// "become" for target-acquires forms (e.g., Moonlace: "Target spell or
    /// permanent becomes colorless."). Shared by SET_COLORS / SET_COLORLESS /
    /// SET_SUBTYPE.
    private static final Parser<Subject> ARE_SUBJECT = anyOf(
            SubjectParsers.SUBJECT.followedBy(phrase("[are|is|becomes|become]")),
            // Contractions — "it's X", "they're X" (Cyber Conversion:
            // "It's a 2/2 Cyberman artifact creature.").
            caseInsensitive("it's").thenReturn(Subject.pronoun(PronounType.IT)),
            caseInsensitive("they're").thenReturn(Subject.pronoun(PronounType.THEY)));

    private static final List<Color> ALL_COLORS = List.of(Color.WHITE, Color.BLUE, Color.BLACK, Color.RED, Color.GREEN);

    /// "\[colors\] / colorless / all colors / the color of \[poss\] choice"
    /// — the right-hand side of a "X are/is …" color-set clause. Each
    /// arm emits a [Effect.SetColors.Colors] variant so a single
    /// SET_COLORS parser covers every form. "colorless" is the empty
    /// Fixed list; "all colors" is the five basic colors; "the color
    /// of your choice" is OfChoice.
    private static final Parser<Effect.SetColors.Colors> SET_COLORS_BODY = Parser.<Effect.SetColors.Colors>anyOf(
            // "colorless \[sources of damage\]?" — Ancient Kavu;
            // "Ghostly Flame" trailing "sources of damage" is flavor.
            word("colorless")
                    .thenReturn(new Effect.SetColors.Colors.Fixed(List.of()))
                    .optionallyFollowedBy(phrase("sources of damage"), (c, _) -> c),
            phrase("all colors").thenReturn(new Effect.SetColors.Colors.Fixed(ALL_COLORS)),
            // "the chosen color" — back-reference to a preceding
            // ChooseColor (Shifting Sky).
            phrase("the chosen color").thenReturn(Effect.SetColors.Colors.Chosen.CHOSEN),
            // "the color \[or colors\]? of \[poss\] choice" — Vodalian Mystic
            // ("the color of"); Quickchange ("the color or colors of").
            // The "or colors" tail flips OfChoice.multi true (any subset
            // of the five); single "color" stays false (one color only).
            sequence(
                            phrase("the color")
                                    .thenReturn(false)
                                    .optionallyFollowedBy(phrase("or colors"), (_, _) -> true)
                                    .followedBy(word("of")),
                            anyOf(
                                    word("your").thenReturn(Subject.player(PlayerRef.Pronoun.YOU)),
                                    anyOf(word("their"), word("his"), word("her"))
                                            .thenReturn(Subject.player(PlayerRef.Pronoun.THEY)),
                                    word("its").thenReturn(Subject.pronoun(PronounType.IT))),
                            (multi, chooser) -> new Effect.SetColors.Colors.OfChoice(chooser, multi))
                    .followedBy(word("choice")),
            // "\[color\] \[and \[color\]\]*" — explicit fixed color set.
            MtgParsers.andList(COLOR).map(Effect.SetColors.Colors.Fixed::new));

    private static final Parser<Effect.SetColors> SET_COLORS_CORE = Parser.sequence(
                    ARE_SUBJECT, SET_COLORS_BODY, Effect.SetColors::new)
            // "in addition to [its|their] other colors" — additive form
            // (Indigo Faerie: "Target permanent becomes blue in addition
            // to its other colors until end of turn."). Adds the new
            // color(s) to the existing color set rather than replacing.
            .optionallyFollowedBy(phrase("in addition to [its|their] other colors"), (sc, _) -> sc.asAdditional())
            .optionallyFollowedBy(DURATION, Effect.SetColors::withDuration);

    /// Same as [#SET_COLORS_CORE] but also accepts a leading
    /// "Until end of turn, …" prefix (Nightcreep: "Until end of turn,
    /// all creatures become black …").
    static final Parser<Effect.SetColors> SET_COLORS = anyOf(
            sequence(UNTIL_END_OF_TURN_PREFIX_INLINE, SET_COLORS_CORE, (d, sc) -> sc.withDuration(d)), SET_COLORS_CORE);

    /// One subtype with optional leading article ("a"/"an") and
    /// optional "colorless" color marker — used inside [#SET_SUBTYPE]'s
    /// and-list. An optional trailing card-type ("land", "creature") is
    /// consumed as flavor so forms like "a colorless Forest land" (Song
    /// of the Dryads) round-trip as the subtype alone.
    private static final Parser<Subtype> SUBTYPE_WITH_ARTICLE = anyOf(
                    phrase("[a|an]").then(word("colorless").optional()).then(SUBTYPE), SUBTYPE)
            .optionallyFollowedBy(CARD_TYPE, (subtype, _) -> subtype);

    /// "[subject] are/is \[a|an\]? [card type] in addition to their other
    /// types." — Enchanted Evening: "All permanents are enchantments in
    /// addition to their other types." (plural, no article); Myr
    /// Landshaper: "Target land becomes an artifact in addition to its
    /// other types." (singular, with article).
    static final Parser<Effect.AddCardType> ADD_CARD_TYPE = sequence(
                    ARE_SUBJECT,
                    anyOf(phrase("[a|an]").then(MtgParsers.andList(CARD_TYPE)), MtgParsers.andList(CARD_TYPE))
                            .followedBy(phrase("in addition to [its|their] other types")),
                    Effect.AddCardType::new)
            .optionallyFollowedBy(DURATION, (e, d) -> new Effect.AddCardType(e.subject(), e.types(), d));

    /// "\[subject\] are/is \[subtype\]+ in addition to their other types." —
    /// additive subtype assignment standalone form (parallels [#ADD_CARD_TYPE]).
    static final Parser<Effect.AddSubtype> ADD_SUBTYPE = anyOf(
                    sequence(
                            ARE_SUBJECT,
                            MtgParsers.andList(SUBTYPE_WITH_ARTICLE)
                                    .followedBy(phrase("in addition to [its|their] other")
                                            .then(CARD_TYPE.optional())
                                            .followedBy(word("types"))),
                            Effect.AddSubtype::new),
                    // "[subject] is also a [subtype list]." — Stonework Packbeast form.
                    sequence(
                            SubjectParsers.SUBJECT.followedBy(phrase("is also [a|an]")),
                            MtgParsers.andList(SUBTYPE),
                            Effect.AddSubtype::new))
            .optionallyFollowedBy(DURATION, Effect.AddSubtype::withDuration);

    /// "[subject] are/is every creature/land/… type." — Runed Stalactite form.
    /// The "every X type" shape can't enumerate all subtypes at parse time, so
    /// it falls back to a free-text [Effect.SetCharacteristic].
    static final Parser<Effect.SetCharacteristic> SET_EVERY_SUBTYPE_TYPE = sequence(
                    ARE_SUBJECT,
                    word("every")
                            .then(anyOf(
                                    phrase("creature type"),
                                    // "basic land type" must precede "land
                                    // type" (Prismatic Omen).
                                    phrase("basic land type"),
                                    phrase("land type"),
                                    phrase("enchantment type"),
                                    phrase("artifact type"),
                                    phrase("planeswalker type"))),
                    (subject, kind) -> new Effect.SetCharacteristic(subject, "every " + kind))
            // "in addition to [its|their] other types" — additive-rather-
            // than-replacement modifier absorbed as flavor (Prismatic Omen).
            .optionallyFollowedBy(phrase("in addition to [its|their] other types"), (e, _) -> e)
            .optionallyFollowedBy(DURATION, Effect.SetCharacteristic::withDuration);

    /// The five basic land types, used by "all basic land types" bodies
    /// (Energybending: "Lands you control gain all basic land types
    /// until end of turn.").
    private static final List<Subtype> ALL_BASIC_LAND_TYPES =
            List.of(LandType.PLAINS, LandType.ISLAND, LandType.SWAMP, LandType.MOUNTAIN, LandType.FOREST);

    /// Subject prefix accepting either the copula verbs ("are", "is",
    /// "becomes", "become") already handled by [#ARE_SUBJECT] or
    /// "gain(s)"/"gain all" for additive forms (Energybending:
    /// "Lands you control gain all basic land types until end of
    /// turn."). Both shapes leave the subject on the stack for the
    /// subtype body.
    private static final Parser<Subject> SUBTYPE_GAIN_SUBJECT =
            anyOf(ARE_SUBJECT, SubjectParsers.SUBJECT.followedBy(phrase("gain(s)")));

    static final Parser<Effect.SetSubtype> SET_SUBTYPE = sequence(
                    SUBTYPE_GAIN_SUBJECT,
                    anyOf(
                            // "all basic land types" — Energybending.
                            phrase("all basic land types").thenReturn(ALL_BASIC_LAND_TYPES),
                            // "X, Y, and Z" — Lush Growth.
                            MtgParsers.andList(SUBTYPE_WITH_ARTICLE)
                                    .suchThat(l -> l.size() >= 2, "and-list of subtypes"),
                            // "X or Y" — player chooses one (Tundra Kavu:
                            // "becomes a Plains or an Island until end of
                            // turn.").
                            MtgParsers.orList(SUBTYPE_WITH_ARTICLE).suchThat(l -> l.size() >= 2, "or-list of subtypes"),
                            SUBTYPE_WITH_ARTICLE.map(List::of)),
                    Effect.SetSubtype::new)
            // "in addition to its other land/creature/… types" — flavor
            // indicating the new subtype is additive, not replacing
            // (e.g., Blanket of Night: "Each land is a Swamp in addition to
            // its other land types.").
            .optionallyFollowedBy(
                    phrase("in addition to [its|their] other").then(CARD_TYPE).then(word("types")), (s, _) -> s)
            .optionallyFollowedBy(DURATION, Effect.SetSubtype::withDuration);

    /// "\[a|an\] \[color\]? \[subtype\]" — typed becomes-creature head used
    /// by [#BECOMES_SUBTYPE_WITH_BASE_PT]. The color slot lifts a
    /// leading "blue" / "red" / etc. (Serpentine Ambush) into a
    /// peer SetColors effect.
    private record ColoredSubtype(@Nullable Color color, Subtype subtype) {}

    private static final Parser<ColoredSubtype> COLORED_SUBTYPE_WITH_ARTICLE = anyOf(
            sequence(phrase("[a|an]").then(COLOR), SUBTYPE, ColoredSubtype::new),
            SUBTYPE_WITH_ARTICLE.map(s -> new ColoredSubtype(null, s)));

    /// "[subject] becomes a [color]? [subtype] with base \[power and toughness\] P/T
    /// [duration]?" — compound type+base-P/T (+ optional color) set
    /// (Omnibian: "becomes a Frog with base power and toughness 3/3
    /// until end of turn."; Serpentine Ambush: "becomes a blue Serpent
    /// with base power and toughness 5/5"). Peer-list exception to the
    /// no-combo-parsers rule: SetColors (layer 5), SetSubtype (layer 4)
    /// and SetBasePT (layer 7b) are intentionally distinct records
    /// applied at different layers, so they share a subject and
    /// duration as separate effects. The outer CLAUSE list flattens.
    private static final Parser<List<Effect>> BECOMES_SUBTYPE_WITH_BASE_PT_CORE = sequence(
            SUBTYPE_GAIN_SUBJECT,
            COLORED_SUBTYPE_WITH_ARTICLE.followedBy(word("with")),
            BASE_PT,
            (subj, colorAndType, pt) -> {
                var effects = new ArrayList<Effect>();
                if (colorAndType.color() != null) {
                    effects.add(new Effect.SetColors(
                            subj, new Effect.SetColors.Colors.Fixed(List.of(colorAndType.color()))));
                }
                effects.add(new Effect.SetSubtype(subj, List.of(colorAndType.subtype())));
                effects.add(new Effect.SetBasePT(subj, pt));
                return List.<Effect>copyOf(effects);
            });

    private static List<Effect> applyDurationToSubtypeBasePtList(List<Effect> list, Duration d) {
        return list.stream()
                .<Effect>map(e -> switch (e) {
                    case Effect.SetColors sc -> sc.withDuration(d);
                    case Effect.SetSubtype st -> st.withDuration(d);
                    case Effect.SetBasePT bp -> bp.withDuration(d);
                    default -> e;
                })
                .toList();
    }

    static final Parser<List<Effect>> BECOMES_SUBTYPE_WITH_BASE_PT = anyOf(
                    // Leading "Until end of turn," prefix — Serpentine
                    // Ambush: "Until end of turn, target creature becomes
                    // a blue Serpent with base power and toughness 5/5.".
                    sequence(
                            UNTIL_END_OF_TURN_PREFIX_INLINE,
                            BECOMES_SUBTYPE_WITH_BASE_PT_CORE,
                            (d, list) -> applyDurationToSubtypeBasePtList(list, d)),
                    BECOMES_SUBTYPE_WITH_BASE_PT_CORE)
            .optionallyFollowedBy(DURATION, EffectParsers::applyDurationToSubtypeBasePtList);

    /// "[subject] are [P/T] [type] [that are still [type]]." — become a
    /// permanent type with a stated P/T (e.g., Living Plane: "All lands are
    /// 1/1 creatures that are still lands."). Emits a
    /// [Effect.SetCharacteristic] with the characteristic as free text
    /// because the full shape (P/T + added type + retained types) is beyond
    /// what the current structured types model.
    /// Core of a become-permanent characteristic: [P/T] [color]? [card
    /// type]. The optional color allows forms like Kormus Bell: "1/1 black
    /// creatures". Returned as free text for the SetCharacteristic
    /// description.
    private static final Parser<String> BECOME_PT_TYPE_TAIL = anyOf(
            // COLOR + SUBTYPE+ + CARD_TYPE+ — Stuffed Bear: "becomes a
            // 4/4 green Bear artifact creature"; Lurking Evil: "becomes
            // a 4/4 Phyrexian Horror creature". Must precede the
            // COLOR + CARD_TYPE arm so subtypes are consumed here
            // rather than leaving them dangling.
            sequence(
                    COLOR,
                    SUBTYPE.atLeastOnce(),
                    CARD_TYPE.atLeastOnce(),
                    (c, sts, ts) -> c.name().toLowerCase() + " "
                            + sts.stream().map(st -> st.texts().getFirst()).collect(Collectors.joining(" ")) + " "
                            + ts.stream().map(t -> t.name().toLowerCase()).collect(Collectors.joining(" "))),
            sequence(
                    COLOR,
                    CARD_TYPE.atLeastOnce(),
                    (c, ts) -> c.name().toLowerCase() + " "
                            + ts.stream().map(t -> t.name().toLowerCase()).collect(Collectors.joining(" "))),
            sequence(
                    SUBTYPE.atLeastOnce(),
                    CARD_TYPE.atLeastOnce(),
                    (sts, ts) -> sts.stream().map(st -> st.texts().getFirst()).collect(Collectors.joining(" ")) + " "
                            + ts.stream().map(t -> t.name().toLowerCase()).collect(Collectors.joining(" "))),
            CARD_TYPE
                    .atLeastOnce()
                    .map(ts -> ts.stream().map(t -> t.name().toLowerCase()).collect(Collectors.joining(" "))));

    /// Optional " with <keyword-list>" suffix on a becomes-creature
    /// description (Xanthic Statue: "becomes an 8/8 Golem artifact
    /// creature with trample."). Routes typed [Ability] keywords into
    /// [Effect.SetCharacteristic.abilities] — the rest of the
    /// description text stays in the free-text fallback.
    private static final Parser<List<Ability>> BECOME_PT_TYPE_WITH_KEYWORDS = phrase("with")
            .then(KeywordParsers.KEYWORD.atLeastOnceDelimitedBy(
                    anyOf(string(",").then(phrase("and")), word("and"), string(",")), Collectors.toUnmodifiableList()));

    private static final Parser<String> BECOME_PT_TYPE_CORE = anyOf(
            sequence(phrase("[a|an]").then(PT_VALUE), BECOME_PT_TYPE_TAIL, (pt, rest) -> pt + " " + rest),
            sequence(PT_VALUE, BECOME_PT_TYPE_TAIL, (pt, rest) -> pt + " " + rest));

    private static final Parser<Effect.SetCharacteristic> BECOME_PT_TYPE_CORE_PARSER = Parser.sequence(
                    ARE_SUBJECT, BECOME_PT_TYPE_CORE, Effect.SetCharacteristic::new)
            .optionallyFollowedBy(BECOME_PT_TYPE_WITH_KEYWORDS, Effect.SetCharacteristic::withAbilities)
            .optionallyFollowedBy(DURATION, Effect.SetCharacteristic::withDuration)
            .optionallyFollowedBy(
                    anyOf(phrase("that are still"), phrase("that's still"))
                            .then(phrase("[a|an]").orElse(""))
                            .then(CARD_TYPE),
                    (sc, still) -> new Effect.SetCharacteristic(
                            sc.target(),
                            sc.description() + " (still " + still.name().toLowerCase() + ")",
                            sc.abilities(),
                            sc.duration()));

    /// Accepts the "Until end of turn, …" / "During your turn, …"
    /// prefix in front of a BECOME_PT_TYPE effect (Animate Land:
    /// "Until end of turn, target land becomes a 3/3 creature that's
    /// still a land.").
    static final Parser<Effect.SetCharacteristic> BECOME_PT_TYPE = anyOf(
            sequence(UNTIL_END_OF_TURN_PREFIX_INLINE, BECOME_PT_TYPE_CORE_PARSER, (d, sc) -> sc.withDuration(d)),
            sequence(DURING_YOUR_TURN, BECOME_PT_TYPE_CORE_PARSER, (d, sc) -> sc.withDuration(d)),
            BECOME_PT_TYPE_CORE_PARSER);

    /// "It's still \[a|an\] \[cardtype\]." / "They're still \[cardtype\]s." —
    /// standalone clarification sentence following a "becomes a
    /// creature" effect (Balduvian Conjurer: "… becomes a 2/2
    /// creature until end of turn. It's still a land."). Rule 305.7
    /// already guarantees the type is retained, so the sentence is
    /// pure flavor. Emits the empty list so it slots into the clause
    /// chain without adding an effect.
    static final Parser<List<Effect>> STILL_A_CARDTYPE_FLAVOR = anyOf(
                    phrase("It's still [a|an]").then(CARD_TYPE),
                    phrase("They're still").then(CARD_TYPE))
            .<List<Effect>>thenReturn(List.of());

    /// "[subject] become\[s\] [that\|the chosen] type \[duration\]?." —
    /// back-reference to a preceding [Effect.ChooseType] (Terraformer:
    /// "Each land you control becomes that type until end of turn.";
    /// Convincing Mirage: "Enchanted land is the chosen type.").
    static final Parser<Effect.BecomesChosenType> BECOMES_CHOSEN_TYPE = ARE_SUBJECT
            .followedBy(phrase("[that|the chosen] type"))
            .map(Effect.BecomesChosenType::new)
            .optionallyFollowedBy(DURATION, Effect.BecomesChosenType::withDuration);

    /// "[subject] becomes the \[basic land type|X type\] of
    /// \[possessive\] choice \[duration\]?." — type-choice become
    /// (Reef Shaman / Sea Snidd: "Target land becomes the basic land
    /// type of your choice until end of turn."). Distinct from
    /// BECOME_PT_TYPE since there's no P/T attached — just a
    /// type-choice back-reference.
    static final Parser<Effect.SetCharacteristic> BECOME_TYPE_OF_CHOICE = sequence(
                    ARE_SUBJECT,
                    phrase("the")
                            .then(anyOf(
                                    phrase("basic land type"),
                                    phrase("creature type"),
                                    phrase("land type"),
                                    phrase("enchantment type"),
                                    phrase("artifact type"),
                                    phrase("planeswalker type")))
                            .followedBy(phrase("of [your|their] choice")),
                    (subj, kind) -> new Effect.SetCharacteristic(subj, "the " + kind + " of your choice"))
            .optionallyFollowedBy(DURATION, Effect.SetCharacteristic::withDuration);

    /// "They're still [type]." / "They are still [type]." — flavor
    /// follow-up on mass type-change effects (Natural Affinity: "All
    /// lands become 2/2 creatures until end of turn. They're still
    /// lands."). Modelled as a [Effect.SetCharacteristic] with the
    /// "they" pronoun as target.
    static final Parser<Effect.SetCharacteristic> STILL_TYPE = anyOf(phrase("They're still"), phrase("They are still"))
            .then(CARD_TYPE)
            .map(t -> new Effect.SetCharacteristic(
                    Subject.pronoun(PronounType.THEY), "still " + t.name().toLowerCase()));

    /// "[subject] are [supertype]" — add a supertype (Rootpath Purifier).
    static final Parser<Effect.SetSupertype> SET_SUPERTYPE = sequence(ARE_SUBJECT, SUPERTYPE, Effect.SetSupertype::new);

    /// Single property name — power / toughness / strength / life
    /// total / hand size. Used as the leaf parser for the andList in
    /// [#SET_PROPERTY_VALUES].
    private static final Parser<Property> PROPERTY_NAME_FOR_SET = anyOf(
            word("power").thenReturn(Property.POWER),
            word("toughness").thenReturn(Property.TOUGHNESS),
            word("strength").thenReturn(Property.STRENGTH),
            phrase("life total").thenReturn(Property.LIFE_TOTAL),
            phrase("hand size").thenReturn(Property.HAND_SIZE));

    /// Possessive-pronoun subject for SET_PROPERTY_VALUES — accepts
    /// "your" / "their" / "its" as a bare possessive without the
    /// intervening `'s`, mapping to the corresponding player subject
    /// (Invincible Hymn: "Your life total becomes that number.").
    private static final Parser<Subject> SET_PROPERTY_SUBJECT = anyOf(
            SubjectParsers.SUBJECT.followedBy(string("'s")),
            phrase("Your").thenReturn(Subject.player(PlayerRef.Pronoun.YOU)),
            phrase("Their").thenReturn(Subject.player(PlayerRef.Pronoun.THEY)),
            phrase("Its").thenReturn(Subject.pronoun(PronounType.IT)));

    /// "[subject] [prop[, prop, and prop]*] [is|are each] equal to
    /// [amount]." — characteristic-defining property assignment (Sima
    /// Yi: "Sima Yi's power is equal to the number of Swamps you
    /// control."; Maro: "Maro's power and toughness are each equal to
    /// the number of cards in your hand."; Biorhythm: "Each player's
    /// life total becomes the number of creatures they control.";
    /// Invincible Hymn: "Your life total becomes that number.").
    /// The subject is either a possessive noun phrase ("X's") or a
    /// bare possessive pronoun ("your"/"their"/"its"). Emits one
    /// [Effect.SetPropertyValue] per property in the list; the
    /// single-property case is a singleton.
    static final Parser<List<Effect>> SET_PROPERTY_VALUES = sequence(
            SET_PROPERTY_SUBJECT,
            MtgParsers.andList(PROPERTY_NAME_FOR_SET)
                    .followedBy(anyOf(phrase("is equal to"), phrase("are each equal to"), word("becomes"))),
            anyOf(
                    // "N plus the number of X" / "N plus the [prop] of X" —
                    // arithmetic base + property/count-of tail (An-Havva
                    // Constable: "equal to 1 plus the number of green
                    // creatures on the battlefield."). Must precede bare
                    // AMOUNT so the "plus" continuation wins.
                    sequence(
                            AmountParsers.ATOMIC_AMOUNT.followedBy(word("plus")),
                            CountOfParsers.PROPERTY_OF_AMOUNT,
                            Amount.Plus::new),
                    CountOfParsers.PROPERTY_OF_AMOUNT,
                    AMOUNT),
            (subj, props, amt) -> props.stream()
                    .<Effect>map(prop -> new Effect.SetPropertyValue(subj, prop, amt))
                    .toList());

    /// "You can't spend this mana to cast spells." — Thran Turbine.
    static final Parser<Effect.CantSpendThisManaToCastSpells> CANT_SPEND_THIS_MANA_TO_CAST_SPELLS = phrase(
                    "You can't spend this mana to cast spells")
            .thenReturn(Effect.CantSpendThisManaToCastSpells.CANT_SPEND_TO_CAST_SPELLS);

    /// "\[Activate\]? only \[N\]? time(s)|once each turn" — the activation-
    /// limit body. The leading "Activate" is optional so the limit can
    /// chain after a prior "Activate only …" clause (Thunderhead Gunner:
    /// "Activate only as a sorcery and only once each turn.") via
    /// the generic effect-sequence combinator.
    static final Parser<Effect.ActivationLimit> ACTIVATION_LIMIT = anyOf(
                    phrase("Activate").then(anyOf(word("only"), phrase("no more than"))),
                    anyOf(word("only"), phrase("no more than")))
            .then(anyOf(
                    word("once").thenReturn(Amount.exact(1)),
                    word("twice").thenReturn(Amount.exact(2)),
                    AMOUNT.followedBy(phrase("time(s)"))))
            .followedBy(phrase("each turn"))
            .map(Effect.ActivationLimit::new);

    /// "Activate only as a sorcery." — sorcery-speed restriction
    /// (Fractured Powerstone).
    static final Parser<Effect.ActivateOnly> ACTIVATE_ONLY_AS_SORCERY =
            phrase("Activate only as a sorcery").thenReturn(Effect.ActivateOnly.AsSorcery.AS_SORCERY);

    /// "Activate only as an instant." — instant-speed restriction
    /// (Rhystic Cave).
    static final Parser<Effect.ActivateOnly> ACTIVATE_ONLY_AS_INSTANT =
            phrase("Activate only as an instant").thenReturn(Effect.ActivateOnly.AsInstant.AS_INSTANT);

    /// "Activate only if \<condition\>." — activation-time gate
    /// (Temple of the False God: "Activate only if you control five
    /// or more lands."; Fool's Tome: "Activate only if you have no
    /// cards in hand."). Reuses the typed [#CONDITION_TAIL] filtered
    /// to IF-kind conditions; non-typed shapes don't parse.
    static final Parser<Effect.ActivateOnly.If> ACTIVATE_ONLY_IF = phrase("Activate only")
            .then(CONDITION_TAIL.suchThat(c -> c.kind() == Condition.Kind.IF, "if-kind condition"))
            .map(Effect.ActivateOnly.If::new);

    /// "Activate only during [when][, [refinement]]?." — timing-window
    /// restriction (Disrupting Scepter: "Activate only during your
    /// turn."; Lu Su, Wu Advisor: "Activate only during your turn,
    /// before attackers are declared."). The trailing clause is
    /// captured verbatim, including any comma-separated refinement.
    static final Parser<Effect.ActivateOnly.During> ACTIVATE_ONLY_DURING = phrase("Activate only during")
            .then(WORD_OR_CONTRACTION.atLeastOnce().map(words -> String.join(" ", words)))
            .optionallyFollowedBy(
                    string(",").then(WORD_OR_CONTRACTION.atLeastOnce().map(words -> String.join(" ", words))),
                    (first, refinement) -> first + ", " + refinement)
            .map(Effect.ActivateOnly.During::new);

    /// "\[until duration, \]? \[player\] may play those cards." — back-reference
    /// play permission (Commune with Lava: "Until the end of your next
    /// turn, you may play those cards."). The duration is the window during
    /// which the permission is valid; the `what` back-references the cards
    /// moved by the preceding exile effect.
    static final Parser<Effect.PlayCards> PLAY_CARDS = anyOf(
            sequence(
                    DURATION.followedBy(string(",")),
                    SubjectParsers.PLAYER_SUBJECTS.followedBy(phrase("may play those cards")),
                    (dur, player) -> new Effect.PlayCards(player, Subject.pronoun(PronounType.THOSE_CARDS), dur)),
            SubjectParsers.PLAYER_SUBJECTS
                    .followedBy(phrase("may play those cards"))
                    .map(player -> new Effect.PlayCards(player, Subject.pronoun(PronounType.THOSE_CARDS))),
            SubjectParsers.PLAYER_SUBJECTS
                    .followedBy(phrase("may play that card"))
                    .map(player -> new Effect.PlayCards(player, Subject.demonstrative("that", "card")))
                    .optionallyFollowedBy(DURATION, Effect.PlayCards::withDuration));

    /// "\[duration, \]? \[player\] may tap \[target\] for mana." — grants a player
    /// permission to activate mana abilities of permanents they don't control
    /// (Piracy: "Until end of turn, you may tap lands you don't control for mana.").
    /// The duration-prefix arm must come first so "Until end of turn," is consumed
    /// before the bare form is tried.
    static final Parser<Effect.TapForMana> TAP_FOR_MANA = anyOf(
            sequence(
                    DURATION.followedBy(string(",")),
                    SubjectParsers.PLAYER_SUBJECTS.followedBy(phrase("may tap")),
                    SubjectParsers.SUBJECT.followedBy(phrase("for mana")),
                    (dur, actor, target) -> new Effect.TapForMana(actor, target, dur)),
            sequence(
                    SubjectParsers.PLAYER_SUBJECTS.followedBy(phrase("may tap")),
                    SubjectParsers.SUBJECT.followedBy(phrase("for mana")),
                    (actor, target) -> new Effect.TapForMana(actor, target)));

    /// "You may play a card you own from outside the game this turn." —
    /// Wish. The subject defaults to "you"; oracle text naming another
    /// player isn't yet supported.
    static final Parser<Effect.PlayFromOutside> PLAY_FROM_OUTSIDE = phrase(
                    "You may play a card you own from outside the game")
            .thenReturn(new Effect.PlayFromOutside(YOU))
            .optionallyFollowedBy(DURATION, Effect.PlayFromOutside::withDuration);

    /// "Turn [subject] face up." — Break Open.
    static final Parser<Effect.TurnFaceUp> TURN_FACE_UP = phrase("Turn")
            .then(SubjectParsers.SUBJECT)
            .followedBy(phrase("face up"))
            .map(Effect.TurnFaceUp::new);

    /// "Turn [subject] face down." — Cyber Conversion.
    static final Parser<Effect.TurnFaceDown> TURN_FACE_DOWN = phrase("Turn")
            .then(SubjectParsers.SUBJECT)
            .followedBy(phrase("face down"))
            .map(Effect.TurnFaceDown::new);

    /// "X are/is no longer [supertype]" — remove a supertype.
    static final Parser<Effect.LoseSupertype> LOSE_SUPERTYPE =
            sequence(ARE_SUBJECT, phrase("no longer").then(SUPERTYPE), Effect.LoseSupertype::new);

    // Regeneration (701.15)

    static final Parser<Effect.Regenerate> REGENERATE =
            phrase("Regenerate").then(SubjectParsers.SUBJECT).map(Effect.Regenerate::new);

    // Action restrictions

    static final Parser<Effect.CantCycle> CANT_CYCLE =
            SubjectParsers.SUBJECT.followedBy(phrase("can't cycle cards")).map(Effect.CantCycle::new);

    /// "X is [amount]." — binds the ability-level X variable to an
    /// amount expression (Bargaining Table: "X is the number of cards
    /// in an opponent's hand.").
    static final Parser<Effect.DefineX> DEFINE_X = phrase("X")
            .followedBy(word("is"))
            .then(anyOf(CountOfParsers.PROPERTY_OF_AMOUNT, AMOUNT))
            .map(Effect.DefineX::new);

    static final Parser<Effect.GainEnergy> GAIN_ENERGY = anyOf(
            sequence(
                    SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("get(s)")), ENERGY_SYMBOLS, Effect.GainEnergy::new),
            phrase("get(s)").then(ENERGY_SYMBOLS).map(n -> new Effect.GainEnergy(YOU, n)));

    /// "[subject] crews [selector] using [property] rather than
    /// [property]." — Giant Ox.
    private static final Parser<Property> POWER_OR_TOUGHNESS =
            anyOf(word("power").thenReturn(Property.POWER), word("toughness").thenReturn(Property.TOUGHNESS));

    static final Parser<Effect.CrewsUsing> CREWS_USING = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("crew(s)")),
            SELECTOR.followedBy(phrase("using [its|their]")),
            POWER_OR_TOUGHNESS.followedBy(phrase("rather than [its|their]")),
            POWER_OR_TOUGHNESS,
            (subj, what, used, replaced) -> new Effect.CrewsUsing(subj, what, used, replaced));

    /// "Until [event]," — prefix duration that applies an
    /// [Duration.UntilEvent] to the effect it precedes (Spatial
    /// Binding: "Until your next upkeep, target permanent can't phase
    /// out."). The tokens between "until" and the comma are captured
    /// verbatim as the event description.
    private static final Parser<Duration> UNTIL_EVENT_PREFIX = phrase("Until")
            .then(WORD_OR_CONTRACTION.atLeastOnce().map(words -> String.join(" ", words)))
            .followedBy(string(","))
            .map(Duration::untilEvent);

    /// "[subject] can't phase out [duration]?" — Spatial Binding.
    static final Parser<Effect.CantPhaseOut> CANT_PHASE_OUT = anyOf(
            sequence(
                    UNTIL_EVENT_PREFIX,
                    SubjectParsers.SUBJECT.followedBy(phrase("can't phase out")),
                    (d, subj) -> new Effect.CantPhaseOut(subj, d)),
            SubjectParsers.SUBJECT
                    .followedBy(phrase("can't phase out"))
                    .map(Effect.CantPhaseOut::new)
                    .optionallyFollowedBy(DURATION, Effect.CantPhaseOut::withDuration));

    /// "Activated abilities of \[subject\] can't be activated." (Collector
    /// Ouphe, Cursed Totem) and the possessive variant
    /// "\[its\|their\] activated abilities can't be activated." (Viper's
    /// Kiss: "Enchanted creature gets -1/-1, and its activated abilities
    /// can't be activated.").
    static final Parser<Effect.CantActivate> CANT_ACTIVATE = anyOf(
                    phrase("Activated abilities of").then(SubjectParsers.SUBJECT),
                    anyOf(
                                    word("its").<Subject>thenReturn(Subject.pronoun(PronounType.IT)),
                                    word("their").<Subject>thenReturn(Subject.pronoun(PronounType.THEM)))
                            .followedBy(phrase("activated abilities")))
            .followedBy(phrase("can't be activated"))
            .map(Effect.CantActivate::new);

    /// "[subject] can't be blocked [by|except by X] [duration]." Structured
    /// as a single CantBeBlocked effect with an optional [Effect.CantBeBlocked.By]
    /// variant: [Effect.CantBeBlocked.By.Matching] for "by X"
    /// (e.g., "can't be blocked by Walls") and
    /// [Effect.CantBeBlocked.By.Except] for "except by X"
    /// (e.g., Shifting Sliver).
    private static final Parser<Effect.CantBeBlocked.By> CANT_BE_BLOCKED_BY = anyOf(
            phrase("Except by").then(SubjectParsers.SUBJECT).map(Effect.CantBeBlocked.By.Except::new),
            // "by more than N X" — an upper bound on the number of blockers
            // (Huang Zhong: "can't be blocked by more than one creature.").
            sequence(phrase("By more than").then(AMOUNT), SubjectParsers.SUBJECT, Effect.CantBeBlocked.By.LimitOf::new),
            phrase("By").then(SubjectParsers.SUBJECT).map(Effect.CantBeBlocked.By.Matching::new));

    static final Parser<Effect.CantBeBlocked> CANT_BE_BLOCKED = SubjectParsers.SUBJECT
            .followedBy(phrase("can't be blocked"))
            .map(Effect.CantBeBlocked::new)
            // Duration can land either before the by-clause (Joven's
            // Tools: "can't be blocked this turn except by Walls.") or
            // after it. Accept both orders.
            .optionallyFollowedBy(DURATION, Effect.CantBeBlocked::withDuration)
            .optionallyFollowedBy(CANT_BE_BLOCKED_BY, Effect.CantBeBlocked::withBy)
            .optionallyFollowedBy(DURATION, Effect.CantBeBlocked::withDuration);

    static final Parser<Effect.CantBlockAlone> CANT_BLOCK_ALONE =
            SubjectParsers.SUBJECT.followedBy(phrase("can't block alone")).map(Effect.CantBlockAlone::new);

    static final Parser<Effect.AttackRestriction> CANT_ATTACK_ALONE = SubjectParsers.SUBJECT
            .followedBy(phrase("can't attack alone"))
            .map(s -> new Effect.AttackRestriction(s, Effect.AttackRestriction.Capability.CantAlone.CANT_ALONE));

    /// "[subject] can only attack alone." — Errantry.
    static final Parser<Effect.AttackRestriction> ONLY_ATTACK_ALONE = SubjectParsers.SUBJECT
            .followedBy(phrase("can only attack alone"))
            .map(s -> new Effect.AttackRestriction(s, Effect.AttackRestriction.Capability.OnlyAlone.ONLY_ALONE));

    /// "Populate." — Wake the Reflections. Rule 701.28.
    static final Parser<Effect.Populate> POPULATE = phrase("Populate").thenReturn(Effect.Populate.POPULATE);

    /// "Count the number of \[subject\]." — prelude effect that binds a
    /// "that number" back-reference for the next sentence (Invincible
    /// Hymn).
    static final Parser<Effect.Count> COUNT_NUMBER_OF = phrase("Count the number of")
            .then(SubjectParsers.SUBJECT)
            .<Amount>map(subj -> new Amount.CountOf(subj, null))
            .map(Effect.Count::new);

    /// "create one of each" — replacement-effect shorthand bound to
    /// the enclosing "If you would create A, B, or C token" event
    /// (Academy Manufactor).
    static final Parser<Effect.CreateOneOfEach> CREATE_ONE_OF_EACH =
            phrase("Create one of each").thenReturn(Effect.CreateOneOfEach.CREATE_ONE_OF_EACH);

    /// "[subject] can't attack [whom]" — e.g., "Creatures can't attack you."
    static final Parser<Effect.AttackRestriction> CANT_ATTACK_WHOM = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("can't attack")),
            SubjectParsers.PLAYER_SUBJECT,
            (s, whom) -> new Effect.AttackRestriction(s, new Effect.AttackRestriction.Capability.CantWhom(whom)));

    /// "[player] takes/take [count] extra turn(s) [after this one]." —
    /// `count` accepts the indefinite article ("an extra turn") as 1 or
    /// an explicit number (Time Stretch: "two extra turns"). Player is
    /// either named (Time Warp) or implicitly "you".
    static final Parser<Effect.TakeExtraTurn> TAKE_EXTRA_TURN = sequence(
                    anyOf(
                            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("Take(s)")),
                            phrase("Take(s)").thenReturn(YOU)),
                    anyOf(phrase("[an|a]").thenReturn(Amount.exact(1)), AMOUNT),
                    word("extra").followedBy(phrase("turn(s)")),
                    (player, count, _) -> new Effect.TakeExtraTurn(player, count))
            .optionallyFollowedBy(phrase("after this one"), (eff, _) -> eff);

    /// "[player] may play lands from [zone]." — e.g., Crucible of Worlds:
    /// "You may play lands from your graveyard."
    static final Parser<Effect.PlayLandsFrom> PLAY_LANDS_FROM = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("may play lands")),
            ZoneExpressionParsers.IN_ZONE_FROM,
            Effect.PlayLandsFrom::new);

    /// `[up to] N additional land(s)` — amount for a "play additional lands"
    /// effect. "Up to" bounds the max; a bare amount is an exact count.
    private static final Parser<Amount> ADDITIONAL_LANDS_AMOUNT =
            anyOf(phrase("Up to").then(AMOUNT), AMOUNT).followedBy(phrase("additional land(s)"));

    /// Body of a "play additional lands" clause, starting at the verb. Used
    /// both directly in [#PLAY_ADDITIONAL_LANDS] and by [#MAY]
    /// for the "may" form (Summer Bloom).
    private static final Parser<Amount> PLAY_ADDITIONAL_LANDS_NO_PLAYER =
            phrase("play(s)").then(ADDITIONAL_LANDS_AMOUNT);

    static final Parser<Effect.PlayAdditionalLands> PLAY_ADDITIONAL_LANDS = Parser.anyOf(
                    Parser.sequence(
                            SubjectParsers.PLAYER_SUBJECT,
                            PLAY_ADDITIONAL_LANDS_NO_PLAYER,
                            Effect.PlayAdditionalLands::new),
                    PLAY_ADDITIONAL_LANDS_NO_PLAYER.map(amount -> new Effect.PlayAdditionalLands(YOU, amount)))
            .optionallyFollowedBy(DURATION, Effect.PlayAdditionalLands::withDuration);

    /// "The Ring tempts [you]." — rule 716.
    static final Parser<Effect.RingTempts> RING_TEMPTS =
            phrase("The Ring tempts").then(SubjectParsers.PLAYER_SUBJECT).map(Effect.RingTempts::new);

    /// "[player]'s hand" — a possessive reference used in "Look at target
    /// player's hand" etc. Packaged as a [Subject.PossessiveSubject]
    /// so LOOK_AT's target is always a [Subject]. Also covers the
    /// possessive form "its controller's hand" / "its owner's hand" (Lay
    /// Bare: "Counter target spell. Look at its controller's hand.").
    private static final Parser<Subject> PLAYER_HAND_SUBJECT = anyOf(
            SubjectParsers.PLAYER_REF
                    .followedBy(string("'s"))
                    .followedBy(word("hand"))
                    .map(ref -> Subject.possessiveSubject(ref.displayName() + "'s", "hand")),
            sequence(
                    anyOf(word("its"), word("their"), word("your")),
                    anyOf(word("controller"), word("owner"))
                            .followedBy(string("'s"))
                            .followedBy(word("hand")),
                    (pronoun, role) -> Subject.possessiveSubject(pronoun + " " + role + "'s", "hand")));

    /// "the top card of [player]'s library" — a positional card reference
    /// used as a LOOK_AT target (e.g., Rootwater Mystic). Rendered as a
    /// [Subject.PossessiveSubject] with role "top card of library".
    private static final Parser<Subject> TOP_CARD_OF_LIBRARY = phrase("The top card of")
            .then(SubjectParsers.PLAYER_REF)
            .followedBy(string("'s"))
            .followedBy(word("library"))
            .map(ref -> Subject.possessiveSubject(ref.displayName() + "'s", "top card of library"));

    /// What can appear after "look at" / "and at" — the positional forms
    /// (TOP_CARD_OF_LIBRARY, PLAYER_HAND_SUBJECT) before falling back to
    /// the general SUBJECT.
    private static final Parser<Subject> LOOK_AT_TARGET =
            anyOf(TOP_CARD_OF_LIBRARY, PLAYER_HAND_SUBJECT, SubjectParsers.SUBJECT);

    /// "Look at [target]." — reveal-to-looker. Covers player-hand targets
    /// ("target player's hand"), top-of-library ("the top card of target
    /// player's library"), and game-object targets ("target face-down
    /// creature", Smoke Teller). Also handles a two-target conjunction via
    /// "and at [second subject]" (Lens of Clarity: "look at the top card of
    /// your library and at face-down creatures you don't control").
    static final Parser<Effect.LookAt> LOOK_AT = phrase("Look at")
            .then(LOOK_AT_TARGET)
            .map(Effect.LookAt::new)
            // Optional second target — "and at [subject]" conjunction
            // (Lens of Clarity: "and at face-down creatures you don't control").
            .optionallyFollowedBy(phrase("and at").then(LOOK_AT_TARGET), Effect.LookAt::withAdditionalTarget)
            // Optional trailing timing flavor — "any time" / "at any time"
            // (Keeper of the Lens: "You may look at face-down creatures you
            // don't control any time."). Consumed as flavor since the
            // permission itself is the LookAt effect.
            .optionallyFollowedBy(anyOf(phrase("At any time"), phrase("Any time")), (la, _) -> la);

    /// "Put \[subject\] back in any order." — put-back operation (Index:
    /// "… then put them back in any order.").
    static final Parser<Effect.PutBack> PUT_BACK = phrase("Put")
            .then(SubjectParsers.SUBJECT)
            .followedBy(phrase("back in any order"))
            .map(Effect.PutBack::new);

    /// "[player] may cast [what] from [zone]+." — permission to cast from
    /// one or more zones (Misthollow Griffin; Squee, the Immortal: "…
    /// from your graveyard or from exile.").
    static final Parser<Effect.CastFromZone> CAST_FROM_ZONE = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("may cast")),
            SubjectParsers.SUBJECT,
            ZoneExpressionParsers.IN_ZONE_FROM
                    .<List<Zone.Named>>map(List::of)
                    .optionallyFollowedBy(word("or").then(ZoneExpressionParsers.IN_ZONE_FROM), EffectParsers::addZone),
            Effect.CastFromZone::new);

    /// "While [player] [is|'re] searching [their] library, [player]
    /// may cast [what] from [their] library." — search-window cast
    /// permission (Panglacial Wurm). The leading clause names the
    /// search context; the trailing clause is the cast permission.
    static final Parser<Effect.MayCastWhileSearching> MAY_CAST_WHILE_SEARCHING = sequence(
            phrase("While")
                    .then(SubjectParsers.PLAYER_SUBJECT)
                    .followedBy(phrase("[is|'re] searching [your|their|his|her] library,")),
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("may cast")),
            SubjectParsers.SUBJECT,
            ZoneExpressionParsers.IN_ZONE_FROM,
            (_, who, what, from) -> new Effect.MayCastWhileSearching(who, what, from));

    /// "[player] chooses a card in their hand and discards the rest." —
    /// Monomania. Keeps the chosen card, discards all others in hand.
    static final Parser<Effect.DiscardAllButOne> DISCARD_ALL_BUT_ONE = SubjectParsers.PLAYER_SUBJECTS
            .followedBy(phrase("choose(s) a card in [their|his|her|its] hand and discard(s) the rest"))
            .map(Effect.DiscardAllButOne::new);

    /// "[player] become[s] the monarch." — rule 716 (e.g., Palace Sentinels).
    static final Parser<Effect.BecomeMonarch> BECOME_MONARCH = SubjectParsers.PLAYER_SUBJECT
            .followedBy(phrase("become(s) the monarch"))
            .map(Effect.BecomeMonarch::new);

    /// Marker-counter token: `{E}` (energy), `{TK}` (ticket), or `{A}`
    /// (acorn) — the closed set of player-scoped markers shipped today.
    private static final Parser<Marker> MARKER_TOKEN = anyOf(
            string("{E}").thenReturn(Marker.ENERGY),
            string("{TK}").thenReturn(Marker.TICKET),
            string("{A}").thenReturn(Marker.ACORN));

    /// Amount paired with a marker-counter token. Accepts four oracle
    /// shapes: explicit count (`"2 {TK}"`), multi-symbol count
    /// (`"{E}{E}"` → 2× `{E}`, common on energy/ticket payouts such as
    /// Tune the Narrative), bare single (`"{TK}"` → 1×), and the
    /// indirect "an amount of {X} equal to Y" form (Electrosiphon:
    /// "You get an amount of {E} equal to its mana value.").
    private static final Parser<Map.Entry<Amount, Marker>> AMOUNT_MARKER = anyOf(
            sequence(AMOUNT, MARKER_TOKEN, Map::entry),
            sequence(
                    phrase("an amount of")
                            .then(MARKER_TOKEN)
                            // Optional parenthesized reminder text
                            // (Electrosiphon: "{E} (energy counters)").
                            .optionallyFollowedBy(OracleParser.REMINDER, (m, _) -> m),
                    phrase("equal to").then(CountOfParsers.PROPERTY_OF_AMOUNT),
                    (marker, amount) -> Map.entry(amount, marker)),
            MARKER_TOKEN.atLeastOnce().map(tokens -> {
                var head = tokens.getFirst();
                return Map.entry(Amount.exact(tokens.size()), head);
            }));

    /// "[player] get[s] [N] [marker]." — e.g., Blorbian Buddy: "You get
    /// {TK}." When the amount is omitted ("You get {E}."), it defaults to 1.
    static final Parser<Effect.GetMarker> GET_MARKER = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("get(s)")),
            AMOUNT_MARKER,
            (player, pair) -> new Effect.GetMarker(player, pair.getKey(), pair.getValue()));

    /// "[subject] can't be countered." — spell counter-immunity.
    static final Parser<Effect.CantBeCountered> CANT_BE_COUNTERED =
            SubjectParsers.SUBJECT.followedBy(phrase("can't be countered")).map(Effect.CantBeCountered::new);

    /// "[subject] must be blocked [by <blocker>]? [duration]? [if able]?."
    /// — combat must-block restriction. Optional "by <blocker>" narrows
    /// the blocker set (Slayer's Cleaver).
    static final Parser<Effect.MustBeBlocked> MUST_BE_BLOCKED = SubjectParsers.SUBJECT
            .followedBy(phrase("must be blocked"))
            .map(Effect.MustBeBlocked::new)
            .optionallyFollowedBy(word("by").then(SELECTOR), Effect.MustBeBlocked::withBy)
            .optionallyFollowedBy(DURATION, Effect.MustBeBlocked::withDuration)
            .optionallyFollowedBy(phrase("if able"), (mb, _) -> mb);

    /// "[blockers] able to block [target] do so." — e.g., Taunting Elf,
    /// Elvish Bard. The blockers subject is discarded as flavor (it's
    /// always "all creatures"-like); semantically this forces `target`
    /// to be blocked by any creature that can.
    static final Parser<Effect.MustBeBlocked> ABLE_TO_BLOCK_DO_SO = SubjectParsers.SUBJECT
            .followedBy(phrase("able to block"))
            .then(SubjectParsers.SUBJECT)
            .map(Effect.MustBeBlocked::new)
            .optionallyFollowedBy(DURATION, Effect.MustBeBlocked::withDuration)
            .followedBy(phrase("do so"));

    /// "[subject] can't attack or block [duration]." — fans out into
    /// two peer restrictions sharing the subject: an AttackRestriction
    /// (can't attack) and a CantBlock (can't block). The outer
    /// [#CLAUSE] level flattens the list so each restriction lands as
    /// a peer.
    static final Parser<List<Effect>> CANT_ATTACK_OR_BLOCK = SubjectParsers.SUBJECT
            .followedBy(phrase("can't attack or block"))
            .map(subj -> List.<Effect>of(
                    new Effect.AttackRestriction(subj, Effect.AttackRestriction.Capability.Cant.CANT),
                    new Effect.CantBlock(subj, ALL_CREATURES)))
            .optionallyFollowedBy(
                    DURATION,
                    (list, d) -> List.<Effect>of(
                            ((Effect.AttackRestriction) list.get(0)).withDuration(d),
                            ((Effect.CantBlock) list.get(1)).withDuration(d)))
            // Trailing typed condition — gates both peer restrictions
            // on the same condition (Qal Sisma Behemoth: "… can't
            // attack or block unless you pay {2}.").
            .optionallyFollowedBy(CONDITION_TAIL, (list, c) -> list.stream()
                    .<Effect>map(e -> new Effect.Conditional(e, c))
                    .toList());

    /// Post-"can't" verb-body registry. Each entry is a subject-less
    /// restriction body that takes the shared subject and an optional
    /// duration via `apply`. The list is the single source of truth for
    /// what "can't X" can expand to; extending it with a new arm
    /// automatically makes it composable in the [#CANT_CHAIN] "or"-fan-out
    /// without any hand-rolled combo parser. The Duration parameter is
    /// forwarded only by arms that semantically support it; existing
    /// attack/block arms ignore it (no known card uses "During combat, X
    /// can't attack or block"; if one appears, extend those arms too).
    private static final Parser<BiFunction<Subject, @Nullable Duration, Effect>> CANT_VERB =
            Parser.<BiFunction<Subject, @Nullable Duration, Effect>>anyOf(
                    // "be blocked [by <selector>]?" — Sneaky Homunculus.
                    phrase("be blocked")
                            .<BiFunction<Subject, @Nullable Duration, Effect>>thenReturn(
                                    (subj, _) -> new Effect.CantBeBlocked(subj))
                            .optionallyFollowedBy(
                                    word("by").then(SubjectParsers.SUBJECT),
                                    (fn, sub) -> (subj, dur) -> ((Effect.CantBeBlocked) fn.apply(subj, null))
                                            .withBy(new Effect.CantBeBlocked.By.Matching(sub))),
                    word("block").thenReturn((BiFunction<Subject, @Nullable Duration, Effect>)
                            (subj, _) -> new Effect.CantBlock(subj, ALL_CREATURES)),
                    word("attack").thenReturn((BiFunction<Subject, @Nullable Duration, Effect>) (subj, _) ->
                            new Effect.AttackRestriction(subj, Effect.AttackRestriction.Capability.Cant.CANT)),
                    // "cast <SELECTOR> [spells]?" — creates a CantCast restriction carrying
                    // the duration from the enclosing CANT_CHAIN (e.g., Hand to Hand).
                    word("cast")
                            .then(SELECTOR)
                            .optionallyFollowedBy(word("spells"), (sel, _) -> sel)
                            .map(sel -> (BiFunction<Subject, @Nullable Duration, Effect>)
                                    (subj, dur) -> new Effect.CantCast(subj, sel, dur)),
                    // "activate abilities that aren't mana abilities" — non-mana activation
                    // restriction (Hand to Hand, Abeyance).
                    phrase("activate abilities that aren't mana abilities")
                            .thenReturn((BiFunction<Subject, @Nullable Duration, Effect>)
                                    (subj, dur) -> new Effect.CantActivateNonManaAbilities(subj, dur)));

    private static final Parser<List<BiFunction<Subject, @Nullable Duration, Effect>>> CANT_VERBS_LIST = CANT_VERB
            .atLeastOnceDelimitedBy(word("or"), Collectors.toUnmodifiableList())
            .suchThat(fns -> fns.size() >= 2, "two or more can't-verbs");

    /// "[subject] can't <verb> [or <verb>]*" — generic negation-chain
    /// combinator. "Can't" distributes over the "or"-joined verb list
    /// (Sneaky Homunculus: "This creature can't block or be blocked
    /// by creatures with power 2 or greater."), so each verb produces
    /// one peer restriction in the output list. Single-verb cases
    /// remain handled by the standalone `CANT_*` parsers (which carry
    /// duration / selector tails); this chain fires only for
    /// multi-verb variants. Supports an optional `[duration,]` prefix
    /// (Hand to Hand: "During combat, players can't cast … or activate …").
    static final Parser<List<Effect>> CANT_CHAIN = anyOf(
            sequence(
                    DURATION.followedBy(string(",")),
                    SubjectParsers.SUBJECT.followedBy(phrase("can't")),
                    CANT_VERBS_LIST,
                    (dur, subj, fns) ->
                            fns.stream().<Effect>map(fn -> fn.apply(subj, dur)).toList()),
            sequence(SubjectParsers.SUBJECT.followedBy(phrase("can't")), CANT_VERBS_LIST, (subj, fns) -> fns.stream()
                    .<Effect>map(fn -> fn.apply(subj, null))
                    .toList()));

    /// "[subject] can't have/get counters [put on it]." / "Counters can't be
    /// put on [selector]." — prevents counter placement on a subject.
    /// Three phrasings all map to [Effect.CantHaveCounters]:
    /// - "[subject] can't have counters put on it." (Melira's Keepers)
    /// - "[subject] can't get counters." (Solemnity — player form)
    /// - "Counters can't be put on [selector]." (Solemnity — permanent form)
    static final Parser<Effect.CantHaveCounters> CANT_HAVE_COUNTERS = Parser.<Effect.CantHaveCounters>anyOf(
            SubjectParsers.SUBJECT
                    .followedBy(phrase("can't have counters put on [it|them]"))
                    .map(Effect.CantHaveCounters::new),
            SubjectParsers.SUBJECT.followedBy(phrase("can't get counters")).map(Effect.CantHaveCounters::new),
            phrase("Counters can't be put on")
                    .then(SELECTOR)
                    .map(sel -> new Effect.CantHaveCounters(Subject.select(sel))));

    /// "[subject] can't be regenerated [duration]." — e.g., Tunnel
    /// (static) and Furnace Brood ("this turn").
    static final Parser<Effect.CantBeRegenerated> CANT_BE_REGENERATED = SubjectParsers.SUBJECT
            .followedBy(phrase("can't be regenerated"))
            .map(Effect.CantBeRegenerated::new)
            .optionallyFollowedBy(DURATION, Effect.CantBeRegenerated::withDuration);

    /// "[subject] [entering|dying|entering or dying] don't cause abilities
    /// [of [selector]]? to trigger." — suppresses ETB/death triggers on the
    /// named permanents. Covers Tocatli Honor Guard / Torpor Orb (entering,
    /// all abilities), Hushbringer ("entering or dying"), and Elesh Norn,
    /// Mother of Machines (entering, scoped to abilities of opponents'
    /// permanents). "entering or dying" is tried before the single-word
    /// forms so the longer match wins.
    private static final Parser<Effect.SuppressEtbTriggers.Event> ETB_SUPPRESS_EVENT = anyOf(
            phrase("Entering or dying").thenReturn(Effect.SuppressEtbTriggers.Event.ENTERING_OR_DYING),
            phrase("Entering").thenReturn(Effect.SuppressEtbTriggers.Event.ENTERING),
            phrase("Dying").thenReturn(Effect.SuppressEtbTriggers.Event.DYING));

    static final Parser<Effect.SuppressEtbTriggers> SUPPRESS_ETB_TRIGGERS = Parser.sequence(
                    SubjectParsers.SUBJECT,
                    ETB_SUPPRESS_EVENT.followedBy(phrase("don't cause abilities")),
                    Effect.SuppressEtbTriggers::new)
            .optionallyFollowedBy(word("of").then(SELECTOR), Effect.SuppressEtbTriggers::withScope)
            .followedBy(phrase("to trigger"));

    /// "If <trigger-clause>, that ability triggers [N] additional time[s]."
    /// — Elesh Norn, Mother of Machines. Also accepts "it triggers" as the
    /// pronoun form (Twinflame Travelers: "it triggers an additional time").
    /// The trigger clause is captured as free word tokens up to the comma;
    /// `additional` defaults to 1 for "an additional time". Because it shares
    /// the "If …," prefix with [#IF_PREFIX_CONDITION] and must win when the
    /// tail is the Panharmonicon-style ", that ability triggers …", it is
    /// dispatched in [#EFFECT] ahead of the generic if-prefix conditional.
    static final Parser<Effect.AdditionalEtbTriggers> ADDITIONAL_ETB_TRIGGERS = sequence(
            phrase("If").then(WORD_OR_CONTRACTION.atLeastOnce().map(words -> String.join(" ", words))),
            string(",")
                    .then(phrase("[that ability|it] triggers"))
                    .then(anyOf(phrase("[an|a]").thenReturn(Amount.exact(1)), AMOUNT))
                    .followedBy(phrase("additional time(s)")),
            Effect.AdditionalEtbTriggers::new);

    /// "[kind] abilities of [scope] trigger [N] additional time[s]." —
    /// Hama Pashar, Ruin Seeker: "Room abilities of dungeons you own
    /// trigger an additional time."
    static final Parser<Effect.AbilityKindTriggersAdditional> ABILITY_KIND_TRIGGERS_ADDITIONAL = sequence(
            word().followedBy(word("abilities")).followedBy(word("of")),
            SELECTOR.followedBy(word("trigger")),
            anyOf(phrase("[an|a]").thenReturn(Amount.exact(1)), AMOUNT).followedBy(phrase("additional time(s)")),
            Effect.AbilityKindTriggersAdditional::new);

    /// "[subject] can't be equipped." — e.g., Goblin Brawler.
    static final Parser<Effect.CantBeEquipped> CANT_BE_EQUIPPED =
            SubjectParsers.SUBJECT.followedBy(phrase("can't be equipped")).map(Effect.CantBeEquipped::new);

    /// "Unattach [selector] [from [target]]?" — e.g., Disarm: "Unattach all
    /// Equipment from target creature."; Carry Away: "unattach enchanted Equipment."
    static final Parser<Effect.Unattach> UNATTACH = phrase("Unattach")
            .then(SELECTOR)
            .map(Effect.Unattach::new)
            .optionallyFollowedBy(phrase("From").then(SubjectParsers.SUBJECT), Effect.Unattach::withFrom);

    /// "[player] may cast [what] [duration]? as though [clause]." —
    /// Vedalken Orrery, Borne Upon a Wind ("this turn as though they had
    /// flash"). The as-though clause is captured as free text; the
    /// optional duration between the subject and the as-though tail is
    /// consumed as flavor for now.
    private static final Parser<Effect.CastAsThough> CAST_AS_THOUGH_CORE = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("may cast")),
            SubjectParsers.SUBJECT.optionallyFollowedBy(DURATION, (s, _) -> s).followedBy(phrase("as though")),
            word().atLeastOnce().map(words -> String.join(" ", words)),
            Effect.CastAsThough::new);

    /// "[player] may cast [what] [duration]? as though [clause]." —
    /// Vedalken Orrery, Borne Upon a Wind ("this turn as though they had
    /// flash"). The as-though clause is captured as free text. Final-
    /// Word Phantom adds a leading "During each opponent's end step,"
    /// duration prefix; the recurring [Duration.DuringStep] scope binds
    /// onto the resulting [Effect.CastAsThough].
    static final Parser<Effect.CastAsThough> CAST_AS_THOUGH = anyOf(
            sequence(DURING_STEP.followedBy(","), CAST_AS_THOUGH_CORE, (d, c) -> c.withDuration(d)),
            sequence(DURING_NEXT_TURN.followedBy(","), CAST_AS_THOUGH_CORE, (d, c) -> c.withDuration(d)),
            CAST_AS_THOUGH_CORE);

    /// "[player] may cast [what] without paying [its|their] mana cost[s]."
    /// — Dracogenesis. Optional "from [zone]" scope consumed as flavor:
    /// Omniscience ("from your hand"), Memory Plunder ("from an opponent's
    /// graveyard").
    static final Parser<Effect.CastWithoutPaying> CAST_WITHOUT_PAYING = sequence(
                    SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("may cast")),
                    SELECTOR,
                    Effect.CastWithoutPaying::new)
            .optionallyFollowedBy(
                    Parser.<Object>anyOf(ZoneExpressionParsers.IN_ZONE_FROM, ZoneExpressionParsers.PLAYER_ZONE_FROM),
                    (cwp, _) -> cwp)
            .followedBy(phrase("without paying [its|their] mana cost(s)"));

    /// "[player] may pay \[alternative\] rather than pay the mana cost
    /// for [spells]." — Fist of Suns: "You may pay {W}{U}{B}{R}{G}
    /// rather than pay the mana cost for spells you cast." Emits an
    /// [Effect.AlternativeCostForSpells] that carries the structured
    /// alternative [Cost] and the selector naming the affected
    /// spells (typically "spells you cast").
    static final Parser<Effect.AlternativeCostForSpells> ALTERNATIVE_COST_FOR_SPELLS = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("may pay")),
            MANA_SYMBOL.atLeastOnce().<Cost>map(Cost.Mana::new).followedBy(phrase("rather than pay the mana cost for")),
            SELECTOR,
            Effect.AlternativeCostForSpells::new);

    /// "[player] may spend [X] mana as though it were [Y] mana." — color
    /// substitution on mana spend (Sunglasses of Urza). Only color-to-color
    /// substitution is captured here; broader forms ("mana of any color")
    /// can grow new arms as they appear.
    static final Parser<Effect.SpendManaAsThough> SPEND_MANA_AS_THOUGH = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("may spend")),
            COLOR.followedBy(word("mana")).followedBy(phrase("as though it were")),
            COLOR.followedBy(word("mana")),
            Effect.SpendManaAsThough::new);

    /// "[subject] can't attack or block alone." — Ember Beast. Fans
    /// out into two peer restrictions: an AttackRestriction
    /// (CantAlone) and a CantBlockAlone. The outer [#CLAUSE] level
    /// flattens the list.
    static final Parser<List<Effect>> CANT_ATTACK_OR_BLOCK_ALONE = SubjectParsers.SUBJECT
            .followedBy(phrase("can't attack or block alone"))
            .map(subj -> List.<Effect>of(
                    new Effect.AttackRestriction(subj, Effect.AttackRestriction.Capability.CantAlone.CANT_ALONE),
                    new Effect.CantBlockAlone(subj)));

    /// "[subject] can't attack, block, or crew [selector]." — Revoke
    /// Privileges. Fans out into three separate effects sharing the
    /// same subject: an AttackRestriction (can't attack), a CantBlock
    /// (can't block), and a CantCrew (can't crew [selector]). The
    /// outer EFFECT_SEQUENCE flattens the list so each restriction
    /// lands as a peer in the ability body.
    static final Parser<List<Effect>> CANT_ATTACK_BLOCK_OR_CREW = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("can't attack, block, or crew")),
            SELECTOR,
            (subj, crewTarget) -> List.of(
                    new Effect.AttackRestriction(subj, Effect.AttackRestriction.Capability.Cant.CANT),
                    new Effect.CantBlock(subj, ALL_CREATURES),
                    new Effect.CantCrew(subj, crewTarget)));

    /// "[subject] attacks or blocks each combat if able." — Iron Golem,
    /// Relentless Raptor. Tried before MUST_ATTACK so "attacks or blocks"
    /// isn't truncated to a bare MUST_ATTACK.
    static final Parser<Effect.MustAttackOrBlock> MUST_ATTACK_OR_BLOCK = SubjectParsers.SUBJECT
            .followedBy(phrase("attacks or blocks each [combat|turn] if able"))
            .map(Effect.MustAttackOrBlock::new);

    /// "[subject] can attack \[this turn\]? as though \[they|it\] didn't
    /// have [ability]." — Rolling Stones (static form); Krotiq
    /// Nestguard / Returned Phalanx ("can attack this turn as though
    /// it didn't have defender" granted by an activated ability). The
    /// optional "this turn" lands on the [AttackRestriction.duration]
    /// slot as [Duration.Fixed#THIS_TURN].
    static final Parser<Effect.AttackRestriction> CAN_ATTACK_AS_THOUGH_WITHOUT = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("can attack")),
            anyOf(
                    phrase("this turn as though").thenReturn(true),
                    phrase("as though").thenReturn(false)),
            phrase("[they|it]")
                    .then(anyOf(word("didn't"), phrase("did not")))
                    .then(word("have"))
                    .then(KeywordParsers.SIMPLE),
            (subj, thisTurn, ability) -> {
                var restriction = new Effect.AttackRestriction(
                        subj, new Effect.AttackRestriction.Capability.AsThoughWithout(ability));
                return thisTurn ? restriction.withDuration(Duration.Fixed.THIS_TURN) : restriction;
            });

    /// "[subject] can be blocked as though they didn't have [ability]."
    /// — Quagmire ("Creatures with swampwalk can be blocked as though
    /// they didn't have swampwalk."). Captured as a free-text
    /// [Effect.SetCharacteristic] since the game-side interaction
    /// (ignore the attacker's unblockable clause) doesn't have its
    /// own typed effect yet.
    static final Parser<Effect.SetCharacteristic> CAN_BE_BLOCKED_AS_THOUGH_WITHOUT = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("can be blocked as though")),
            phrase("[they|it]")
                    .then(anyOf(word("didn't"), phrase("did not")))
                    .then(word("have"))
                    .then(anyOf(
                            // "those abilities" — back-reference to the
                            // selector's with-clauses (Staff of the Ages:
                            // "Creatures with landwalk abilities can be
                            // blocked as though they didn't have those
                            // abilities.").
                            phrase("those abilities").thenReturn("those abilities"), word())),
            (subj, ability) ->
                    new Effect.SetCharacteristic(subj, "can be blocked as though without " + ability.toLowerCase()));

    /// "[subject] can be played as though it had [ability]." — Scout's
    /// Warning: "The next creature card you play this turn can be
    /// played as though it had flash.". The grant applies at play time
    /// (rule 305 covers lands too), distinct from [Effect.CastAsThough]
    /// (player-side cast permission) and from a direct gain.
    static final Parser<Effect.CanBePlayedAsThoughHad> CAN_BE_PLAYED_AS_THOUGH_HAD = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("can be played as though [it|they] had")),
            KeywordParsers.KEYWORD,
            Effect.CanBePlayedAsThoughHad::new);

    /// "[subject] [also]? attack(s)" — shared subject + verb prefix
    /// for the must-attack variants below. The optional "also" is
    /// consumed as flavor (Ekundu Cyclops: "Ekundu Cyclops also
    /// attacks each turn if able.").
    private static final Parser<Subject> SUBJECT_ATTACKS = SubjectParsers.SUBJECT
            .optionallyFollowedBy(word("also"), (s, _) -> s)
            .followedBy(phrase("attack(s)"));

    /// "[subject] attack[s] [attackTarget]? [each combat/turn | this turn]? if able." —
    /// static or temporary must-attack restriction. Alluring Siren
    /// names a directed attack target ("attacks you this turn if
    /// able."); the bare forms (Viashino Bey, Ekundu Cyclops) omit
    /// it. The "if able" tail alone is also accepted as the static
    /// form.
    private static Effect.AttackRestriction mustAttack(Subject subject) {
        return new Effect.AttackRestriction(subject, new Effect.AttackRestriction.Capability.Must());
    }

    private static Effect.AttackRestriction mustAttack(Subject subject, Subject whom) {
        return new Effect.AttackRestriction(subject, new Effect.AttackRestriction.Capability.Must(whom));
    }

    static final Parser<Effect.AttackRestriction> MUST_ATTACK = anyOf(
                    // "During <player>'s next turn, <subject> attack
                    // <whom>? if able" — duration-prefixed forced-attack
                    // (Taunt: "During target player's next turn,
                    // creatures that player controls attack you if
                    // able.").
                    sequence(
                            DURING_NEXT_TURN.followedBy(","),
                            SUBJECT_ATTACKS,
                            SubjectParsers.PLAYER_LIKE_SUBJECT,
                            (d, s, who) -> mustAttack(s, who).withDuration(d)),
                    sequence(DURING_NEXT_TURN.followedBy(","), SUBJECT_ATTACKS, (d, s) -> mustAttack(s)
                            .withDuration(d)),
                    // "[subject] attacks [attackTarget] <duration>" —
                    // directed attack (Alluring Siren).
                    sequence(SUBJECT_ATTACKS, SubjectParsers.PLAYER_LIKE_SUBJECT, DURATION, (s, who, d) -> mustAttack(
                                    s, who)
                            .withDuration(d)),
                    sequence(SUBJECT_ATTACKS, DURATION, (s, d) -> mustAttack(s).withDuration(d)),
                    SUBJECT_ATTACKS.followedBy(phrase("each [combat|turn]")).map(EffectParsers::mustAttack),
                    // Bare "[subject] attack if able" — no per-combat
                    // scope (Viashino Bey). The "if able" tail is
                    // consumed via the trailing optionallyFollowedBy
                    // below.
                    SUBJECT_ATTACKS.followedBy(phrase("if able")).map(EffectParsers::mustAttack))
            .optionallyFollowedBy(phrase("if able"), (s, _) -> s);

    /// "[player]'s life total becomes N."
    static final Parser<Effect.LifeTotalBecomes> LIFE_TOTAL_BECOMES = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(string("'s")).followedBy(phrase("life total becomes")),
            AMOUNT,
            Effect.LifeTotalBecomes::new);

    /// Named game rule inside quotes — for now only the "legend rule".
    private static final Parser<GameRule> RULE_NAME =
            anyOf(phrase("Legend rule").thenReturn(GameRule.LEGEND_RULE));

    /// `The "<rule>" doesn't apply.` — e.g., Mirror Gallery.
    static final Parser<Effect.RuleDoesntApply> RULE_DOESNT_APPLY = phrase("The")
            .then(string("\""))
            .then(RULE_NAME)
            .followedBy(string("\""))
            .followedBy(phrase("doesn't apply"))
            .map(Effect.RuleDoesntApply::new);

    /// `This effect doesn't remove this Aura.` — protection-Aura self-exemption
    /// clarification (Red Ward, Blue Ward, etc.). The protection granted by this
    /// Aura won't cause the Aura itself to fall off.
    static final Parser<Effect.EffectDoesntRemoveThisAura> EFFECT_DOESNT_REMOVE_THIS_AURA =
            phrase("This effect doesn't remove this Aura").thenReturn(new Effect.EffectDoesntRemoveThisAura());

    // Skip (rule 614.10)

    /// Bare step-name parser — no trailing "step(s)" suffix. Used by the
    /// demonstrative skip form ("skip that draw").
    private static final Parser<Step> STEP_NAME = anyOf(
            phrase("Beginning of combat").thenReturn(Step.BEGINNING_OF_COMBAT),
            phrase("Declare attackers").thenReturn(Step.DECLARE_ATTACKERS),
            phrase("Declare blockers").thenReturn(Step.DECLARE_BLOCKERS),
            phrase("Combat damage").thenReturn(Step.COMBAT_DAMAGE),
            phrase("End of combat").thenReturn(Step.END_OF_COMBAT),
            phrase("Untap").thenReturn(Step.UNTAP),
            phrase("Upkeep").thenReturn(Step.UPKEEP),
            phrase("Draw").thenReturn(Step.DRAW),
            phrase("End").thenReturn(Step.END),
            phrase("Cleanup").thenReturn(Step.CLEANUP));

    /// Matches a Step name followed by "step"/"steps". Multi-word step
    /// names come first via [#STEP_NAME]'s ordering so their first
    /// word isn't consumed by a shorter alternative.
    private static final Parser<Step> SKIPPABLE_STEP = STEP_NAME.followedBy(phrase("step(s)"));

    private static final Parser<Phase> SKIPPABLE_PHASE = anyOf(
                    phrase("Beginning").thenReturn(Phase.BEGINNING),
                    phrase("Main").thenReturn(Phase.MAIN),
                    phrase("Combat").thenReturn(Phase.COMBAT),
                    phrase("Ending").thenReturn(Phase.ENDING))
            .followedBy(phrase("phase(s)"));

    private static final Parser<Skippable> SKIPPABLE = anyOf(
            SKIPPABLE_STEP.<Skippable>map(Skippable.OfStep::new),
            SKIPPABLE_PHASE.<Skippable>map(Skippable.OfPhase::new),
            // "<count> turns" (Eater of Days: "skip your next two
            // turns.") — count-bearing plural form, tried before the
            // bare singular.
            sequence(AMOUNT, phrase("turn(s)"), (count, _) -> new Skippable.Turn(count)),
            phrase("turn(s)").thenReturn(Skippable.Turn.one()));

    /// Tail of the "all X of [possessive] [next]? turn" skip form — the
    /// possessive-prefixed turn reference used by [#SKIP_NO_PLAYER]'s
    /// distributive arm. Consumed as flavor.
    private static final Parser<String> OF_NEXT_TURN_TAIL = word("of")
            .then(phrase("[your|their|his|her|its]"))
            .then(anyOf(word("next").followedBy(phrase("turn(s)")), phrase("turn(s)")));

    /// "[player] skip[s] [target] [what]." — rule 614.10. `target` is
    /// either a possessive pronoun ("your/their/…") optionally preceded by
    /// "next", or the all-of-type form "all X of [possessive] [next]? turn"
    /// (e.g., False Peace: "skips all combat phases of their next turn.").
    private static final Parser<Skippable> SKIP_NO_PLAYER = phrase("Skip(s)")
            .then(anyOf(
                    // "all [X] of [possessive] [next]? turn(s)" — emits the
                    // same Skippable as the short form; the "of … turn" tail
                    // is consumed as flavor since per-turn scope is implicit.
                    sequence(phrase("All").then(SKIPPABLE), OF_NEXT_TURN_TAIL, (s, _) -> s),
                    // "the [step-name] step of that turn" — demonstrative
                    // variant (Savor the Moment: "Skip the untap step of
                    // that turn."). The "of that turn" tail is flavor
                    // since the scope is implicit.
                    sequence(
                            word("the").then(SKIPPABLE),
                            word("of").then(phrase("[that|this]")).then(word("turn")),
                            (s, _) -> s),
                    // "that [step-name]" — demonstrative form referring back
                    // to an event in the triggering clause (Obstinate
                    // Familiar: "If you would draw a card, you may skip
                    // that draw instead."). Matches a bare step name
                    // without requiring the "step" suffix.
                    word("that").then(STEP_NAME).<Skippable>map(Skippable.OfStep::new),
                    phrase("[your|their|his|her|its]").then(anyOf(word("next").then(SKIPPABLE), SKIPPABLE))));

    static final Parser<Effect.Skip> SKIP = Parser.anyOf(
                    Parser.sequence(SubjectParsers.PLAYER_SUBJECT, SKIP_NO_PLAYER, Effect.Skip::new),
                    SKIP_NO_PLAYER.map(s -> new Effect.Skip(YOU, s)))
            .optionallyFollowedBy(DURATION, Effect.Skip::withDuration);

    static final Parser<Effect.CantSearchLibraries> CANT_SEARCH_LIBRARIES = SubjectParsers.SUBJECT
            .followedBy(phrase("can't search libraries"))
            .map(Effect.CantSearchLibraries::new)
            .optionallyFollowedBy(DURATION, Effect.CantSearchLibraries::withDuration);

    /// "[players] can cast spells \[and activate abilities\]? only during
    /// [timing]." — e.g., Dosan the Falling Leaf ("spells only"); City
    /// of Solitude ("spells and activate abilities only"). Timing is
    /// captured as free text.
    static final Parser<Effect.RestrictSpellTiming> RESTRICT_SPELL_TIMING = sequence(
            SubjectParsers.PLAYER_SUBJECT
                    .followedBy(phrase("can cast spells"))
                    .optionallyFollowedBy(phrase("and activate abilities"), (s, _) -> s)
                    .followedBy(phrase("only during")),
            WORD_OR_CONTRACTION.atLeastOnce().map(words -> String.join(" ", words)),
            Effect.RestrictSpellTiming::new);

    private static final Parser<Effect.CantCast> CANT_CAST_CORE = Parser.sequence(
                    SubjectParsers.SUBJECT
                            .followedBy(phrase("can't cast"))
                            .optionallyFollowedBy(word("additional"), (s, _) -> s),
                    SELECTOR,
                    Effect.CantCast::new)
            .optionallyFollowedBy(word("spells"), (cc, ign) -> cc)
            // Trailing zone restriction — "from anywhere other than
            // [zone]" (Drannith Magistrate: "Your opponents can't cast
            // spells from anywhere other than their hands."). Consumed
            // as flavor; CantCast carries no explicit zone field yet.
            .optionallyFollowedBy(
                    phrase("from anywhere other than")
                            .then(phrase("[your|their|its]"))
                            .then(PLURAL_ZONE_NAME),
                    (cc, _) -> cc);

    /// "\<spells\> can't be cast." — Meddling Mage.
    static final Parser<Effect.CantBeCast> CANT_BE_CAST =
            SubjectParsers.SUBJECT.followedBy(phrase("can't be cast")).map(Effect.CantBeCast::new);

    static final Parser<Effect.CantCast> CANT_CAST = anyOf(
                    // "As long as <predicate>, <subject> can't cast …" —
                    // conditional-duration prefix (Wardscale Dragon:
                    // "As long as this creature is attacking, defending
                    // player can't cast spells.").
                    sequence(AS_LONG_AS_PREFIX, CANT_CAST_CORE, (d, cc) -> cc.withDuration(d)), CANT_CAST_CORE)
            .optionallyFollowedBy(DURATION, Effect.CantCast::withDuration);

    /// Ordinal vocabulary for early-game turn counting ("first", "second",
    /// "third", "fourth"). Used by [#CANT_CAST_SELF].
    private static final Parser<Integer> GAME_TURN_ORDINAL = anyOf(
            phrase("first").thenReturn(1),
            phrase("second").thenReturn(2),
            phrase("third").thenReturn(3),
            phrase("fourth").thenReturn(4));

    /// "You can't cast ~ during your first[, second, or third]? turns of
    /// the game." — static self-casting restriction banning the card during
    /// the caster's first N turns. Subject (you) and object (~) are
    /// implicit; `upThrough` is the maximum banned turn (e.g., 3 for Serra
    /// Avenger: "first, second, or third turns").
    static final Parser<Effect.CantCastSelf> CANT_CAST_SELF = phrase("You can't cast")
            .followedBy(string("~"))
            .followedBy(phrase("during your"))
            .then(MtgParsers.orList(GAME_TURN_ORDINAL))
            .followedBy(phrase("turns of the game"))
            .map(ordinals -> new Effect.CantCastSelf(
                    ordinals.stream().mapToInt(Integer::intValue).max().orElse(1)));

    /// "[subject] can't [draw|cast] more than [N] [cards|spells] each turn."
    /// — a per-turn upper limit (Spirit of the Labyrinth, Arcane Laboratory,
    /// Eidolon of Rhetoric).
    static final Parser<Effect.PerTurnLimit> PER_TURN_LIMIT = sequence(
            SubjectParsers.SUBJECT.followedBy(word("can't")),
            anyOf(
                    word("draw").followedBy(phrase("more than")).thenReturn(Effect.PerTurnLimit.Action.DRAW_CARDS),
                    word("cast").followedBy(phrase("more than")).thenReturn(Effect.PerTurnLimit.Action.CAST_SPELLS)),
            AMOUNT
                    // Allow optional qualifiers between the count and the
                    // noun (Deafening Silence: "more than one noncreature
                    // spell each turn."). Qualifiers are consumed as flavor
                    // since {@link Effect.PerTurnLimit} captures only the
                    // action and count for now.
                    .followedBy(QUALIFIER.atLeastOnce().optional())
                    .followedBy(phrase("[cards|card|spells|spell] each turn")),
            Effect.PerTurnLimit::new);

    /// "[player] have no maximum hand size" — MaximumHandSize.None variant.
    /// Maximum hand size is a player-only concept, so the subject is a player.
    static final Parser<Effect.MaximumHandSize> NO_MAXIMUM_HAND_SIZE = SubjectParsers.PLAYER_SUBJECT
            .followedBy(phrase("[have|has] no maximum hand size"))
            .map(s -> new Effect.MaximumHandSize(s, Effect.MaximumHandSize.HandSize.None.NONE));

    /// Possessive player prefix used in phrases like "Your maximum hand size"
    /// or "Each opponent's maximum hand size". Accepts the bare pronouns
    /// "your" / "their" as well as an explicit [Subject.PlayerRef]
    /// followed by `'s` (e.g., "each opponent's", "each player's", "target
    /// player's").
    private static final Parser<Subject> POSSESSIVE_PLAYER = anyOf(
            phrase("Your").thenReturn(Subject.player(PlayerRef.Pronoun.YOU)),
            phrase("Their").thenReturn(Subject.player(PlayerRef.Pronoun.THEY)),
            SubjectParsers.PLAYER_SUBJECT.followedBy(string("'s")));

    /// "\[possessive\] life total can't change." — Platinum Emperion.
    static final Parser<Effect.LifeTotalCantChange> LIFE_TOTAL_CANT_CHANGE =
            POSSESSIVE_PLAYER.followedBy(phrase("life total can't change")).map(Effect.LifeTotalCantChange::new);

    /// "[possessive] maximum hand size is reduced/increased by N." — Delta
    /// variant (e.g., Thought Nibbler: "Your maximum hand size is reduced by
    /// two.").
    static final Parser<Effect.MaximumHandSize> MAXIMUM_HAND_SIZE_DELTA = sequence(
            POSSESSIVE_PLAYER.followedBy(phrase("maximum hand size is")),
            anyOf(
                    word("reduced").followedBy(word("by")).then(NUMBER).map(n -> -n),
                    word("increased").followedBy(word("by")).then(NUMBER)),
            (p, delta) -> new Effect.MaximumHandSize(p, new Effect.MaximumHandSize.HandSize.Delta(delta)));

    /// "[possessive] maximum hand size is N." — Fixed variant (Null
    /// Profusion / Recycle: "Your maximum hand size is two."; Cursed
    /// Rack: "The chosen player's maximum hand size is four."). Must
    /// be tried after [#MAXIMUM_HAND_SIZE_DELTA] so the longer
    /// "is reduced/increased by N" wording wins for delta cards.
    static final Parser<Effect.MaximumHandSize> MAXIMUM_HAND_SIZE_FIXED = sequence(
            POSSESSIVE_PLAYER.followedBy(phrase("maximum hand size is")),
            NUMBER,
            (p, n) -> new Effect.MaximumHandSize(p, new Effect.MaximumHandSize.HandSize.Fixed(n)));

    // Cost modification: "<subject> cost[s] <mana> more/less [to cast]"

    private static final Parser<CostDelta> COST_DELTA =
            anyOf(phrase("More").thenReturn(CostDelta.MORE), phrase("Less").thenReturn(CostDelta.LESS));

    /// Known cost-keyword names ("buyback", "kicker", "cycling", …).
    /// Underscored enum constants (LEVEL_UP) map to their oracle-text form.
    private static final Parser<CostKeyword> COST_KEYWORD = anyOf(
            phrase("Level up").thenReturn(CostKeyword.LEVEL_UP),
            phrase("Aura swap").thenReturn(CostKeyword.AURA_SWAP),
            phrase("Buyback").thenReturn(CostKeyword.BUYBACK),
            phrase("Kicker").thenReturn(CostKeyword.KICKER),
            phrase("Multikicker").thenReturn(CostKeyword.MULTIKICKER),
            phrase("Flashback").thenReturn(CostKeyword.FLASHBACK),
            phrase("Madness").thenReturn(CostKeyword.MADNESS),
            phrase("Echo").thenReturn(CostKeyword.ECHO),
            phrase("Cycling").thenReturn(CostKeyword.CYCLING),
            phrase("Equip").thenReturn(CostKeyword.EQUIP),
            phrase("Fortify").thenReturn(CostKeyword.FORTIFY),
            phrase("Ward").thenReturn(CostKeyword.WARD),
            phrase("Bestow").thenReturn(CostKeyword.BESTOW),
            phrase("Dash").thenReturn(CostKeyword.DASH),
            phrase("Entwine").thenReturn(CostKeyword.ENTWINE),
            phrase("Splice").thenReturn(CostKeyword.SPLICE),
            phrase("Replicate").thenReturn(CostKeyword.REPLICATE),
            phrase("Suspend").thenReturn(CostKeyword.SUSPEND),
            phrase("Transmute").thenReturn(CostKeyword.TRANSMUTE),
            phrase("Transfigure").thenReturn(CostKeyword.TRANSFIGURE),
            phrase("Recover").thenReturn(CostKeyword.RECOVER),
            phrase("Ninjutsu").thenReturn(CostKeyword.NINJUTSU),
            phrase("Outlast").thenReturn(CostKeyword.OUTLAST),
            phrase("Scavenge").thenReturn(CostKeyword.SCAVENGE),
            phrase("Unearth").thenReturn(CostKeyword.UNEARTH),
            phrase("Reinforce").thenReturn(CostKeyword.REINFORCE),
            phrase("Awaken").thenReturn(CostKeyword.AWAKEN),
            phrase("Emerge").thenReturn(CostKeyword.EMERGE),
            phrase("Escape").thenReturn(CostKeyword.ESCAPE),
            phrase("Embalm").thenReturn(CostKeyword.EMBALM),
            phrase("Eternalize").thenReturn(CostKeyword.ETERNALIZE),
            phrase("Unlock").thenReturn(CostKeyword.UNLOCK),
            phrase("Morph").thenReturn(CostKeyword.MORPH));

    /// Cost source for a modify-cost effect: a keyword ability ("buyback
    /// costs"), "[Keyword] abilities you activate" (Fluctuator), or a
    /// spell-matching subject ("spells you cast"). Keyword variants are
    /// tried first so their trailing "costs"/"abilities" isn't consumed by
    /// the subject grammar.
    /// Optional "\[payer\] pay(s)" tail on a keyword-costs head —
    /// Inquisitive Glimmer: "Unlock costs you pay cost {1} less.";
    /// Catalyst Stone: "Flashback costs your opponents pay cost {2}
    /// more." Narrows the cost-modifier to a specific payer.
    private static final Parser<Subject> COST_PAYER = anyOf(
                    phrase("you").thenReturn(Subject.player(PlayerRef.Pronoun.YOU)),
                    phrase("your opponent(s)").thenReturn(Subject.player(PlayerRef.Pronoun.YOUR_OPPONENTS)),
                    phrase("any player").thenReturn(Subject.player(PlayerRef.Pronoun.ANY_PLAYER)))
            .followedBy(phrase("pay(s)"));

    private static final Parser<CostSource> COST_SOURCE = Parser.<CostSource>anyOf(
            // "All [Keyword] costs" — "All morph costs" (Exiled Doomsayer).
            // "All" is flavor for totality; the semantic is the same as
            // "[Keyword] costs". Must precede the bare keyword arm.
            phrase("All").then(COST_KEYWORD).followedBy(word("costs")).map(CostSource.Ability::new),
            // "[Keyword] costs [payer] pay(s)" — keyword ability cost
            // optionally narrowed to a specific payer.
            COST_KEYWORD
                    .followedBy(word("costs"))
                    .map(CostSource.Ability::new)
                    .optionallyFollowedBy(COST_PAYER, (ab, payer) -> new CostSource.Ability(ab.keyword(), payer)),
            // "[Keyword] abilities you activate" — treats the keyword's
            // activation costs collectively (e.g., Fluctuator).
            COST_KEYWORD.followedBy(phrase("abilities you activate")).map(CostSource.Ability::new),
            // "Loyalty abilities of [subject]" — cost modifier scoped to
            // loyalty abilities (Eidolon of Obstruction). Must precede
            // SUBJECT arm so "Loyalty abilities of …" is not consumed as
            // a bare selector.
            phrase("Loyalty abilities of").then(SubjectParsers.SUBJECT).map(CostSource.LoyaltyAbility::new),
            SubjectParsers.SUBJECT.map(CostSource.Spell::new));

    /// "[source] cost[s] <mana> more/less [to cast | to activate]." Handles
    /// both spell-subject forms (`Spells you cast cost {1` more to
    /// cast}) and keyword-ability forms (`Buyback costs cost {2`
    /// less}, `Cycling abilities you activate cost {2` less to
    /// activate}).
    /// Optional leading duration prefix on a cost-modifier (Naiad of
    /// Hidden Coves: "During turns other than yours, spells you cast cost
    /// {1} less to cast."). The prefix is flavor for now — [Effect.ModifyCost] has no duration slot yet.
    private static final Parser<Duration> MODIFY_COST_DURATION_PREFIX = anyOf(DURING_YOUR_TURN, DURING_OTHERS_TURN);

    /// "Activated abilities of [selector] cost …" — shared prefix for two
    /// distinct cost-modification effects. Dispatches after the shared
    /// prefix on the cost form:
    /// - "{N} more/less [to cast|to activate]" → [Effect.ModifyCost]
    ///   (Gloom: "Activated abilities of white enchantments cost {3} more to activate.")
    /// - "an additional '[quoted cost]' to activate/cast" → [Effect.AdditionalCostOnAbility]
    ///   (Brutal Suppression: "Activated abilities of nontoken Rebels cost an additional
    ///   'Sacrifice a land' to activate.")
    /// Both share the "Activated abilities of [selector]" prefix, so they are unified here
    /// to avoid the non-backtracking problem when the prefix is consumed and the two
    /// continuations diverge.
    static final Parser<Effect> ACTIVATED_ABILITIES_OF_COST_EFFECT = sequence(
            phrase("Activated abilities of").then(SELECTOR),
            Parser.<Function<Selector, Effect>>anyOf(
                    // mana cost modification: "cost {N} more/less [to cast|to activate]"
                    sequence(
                                    phrase("cost(s)").then(MANA_SYMBOL.atLeastOnce()),
                                    COST_DELTA,
                                    (symbols, delta) -> (Function<Selector, Effect>) sel -> new Effect.ModifyCost(
                                            new CostSource.Spell(Subject.select(sel)), symbols, delta))
                            .optionallyFollowedBy(anyOf(phrase("to cast"), phrase("to activate")), (fn, _) -> fn),
                    // non-mana additional cost: "cost an additional '[quoted]' to activate/cast"
                    sequence(
                            phrase("cost an additional"),
                            CostParsers.COST_EXPRESSION.between("\"", "\""),
                            phrase("to")
                                    .then(anyOf(word("activate"), word("cast")))
                                    .thenReturn((Void) null),
                            (ign, cost, _) -> (Function<Selector, Effect>) sel -> new Effect.AdditionalCostOnAbility(
                                    new CostSource.Spell(Subject.select(sel)), cost))),
            (sel, fn) -> fn.apply(sel));

    /// Filter: typed condition with kind = AS_LONG_AS, used by the
    /// MODIFY_COST as-long-as prefix arm (Centaur Omenreader: "As
    /// long as this creature is tapped, creature spells you cast
    /// cost {2} less to cast.").
    private static final Parser<Condition> AS_LONG_AS_CONDITION_PREFIX = CONDITION_TAIL
            .suchThat(c -> c.kind() == Condition.Kind.AS_LONG_AS, "as-long-as condition")
            .followedBy(string(","));

    static final Parser<Effect.ModifyCost> MODIFY_COST = anyOf(
                    // "As long as <typed-condition>, <cost source> cost
                    // {N} more/less" — continuous gate (Centaur
                    // Omenreader). Typed-only; free-text predicates
                    // simply don't parse.
                    sequence(
                            AS_LONG_AS_CONDITION_PREFIX,
                            sequence(
                                    COST_SOURCE.followedBy(phrase("cost(s)")),
                                    MANA_SYMBOL.atLeastOnce(),
                                    COST_DELTA,
                                    Effect.ModifyCost::new),
                            (cond, mc) -> mc.withCondition(cond)),
                    sequence(
                            MODIFY_COST_DURATION_PREFIX,
                            sequence(
                                    COST_SOURCE.followedBy(phrase("cost(s)")),
                                    MANA_SYMBOL.atLeastOnce(),
                                    COST_DELTA,
                                    Effect.ModifyCost::new),
                            (dur, mc) -> mc.withDuration(dur)),
                    sequence(
                            COST_SOURCE.followedBy(phrase("cost(s)")),
                            MANA_SYMBOL.atLeastOnce(),
                            COST_DELTA,
                            Effect.ModifyCost::new),
                    // "[cost source] costs N life more/less to cast" —
                    // life-payment cost modifier (Phyrexian Purge: "This
                    // spell costs 3 life more to cast for each target.").
                    // Distinct payment unit from the mana-symbol arms;
                    // routed through the same [Effect.ModifyCost] via
                    // [CostAdjustment.Life].
                    sequence(
                            COST_SOURCE.followedBy(phrase("cost(s)")),
                            Parser.digits().<Integer>map(Integer::parseInt).followedBy(word("life")),
                            COST_DELTA,
                            (src, life, delta) ->
                                    new Effect.ModifyCost(src, new Effect.ModifyCost.CostAdjustment.Life(life), delta)),
                    // Dedicated arm for "Activated abilities cost
                    // {N} more/less" (Suppression Field) — some
                    // upstream SELECTOR behavior was clipping the
                    // ABILITY game-object type for this specific
                    // qualifier combo.
                    sequence(
                            phrase("Activated abilities")
                                    .thenReturn((CostSource) new CostSource.Spell(Subject.select(new Selector(
                                            Selector.Quantifier.one(),
                                            List.of(Selector.Qualifier.AbilitySource.ACTIVATED),
                                            GameObjectType.ABILITY)))),
                            phrase("cost(s)").then(MANA_SYMBOL.atLeastOnce()),
                            COST_DELTA,
                            Effect.ModifyCost::new))
            .optionallyFollowedBy(anyOf(phrase("To cast"), phrase("To activate")), (mc, ign) -> mc)
            // Trailing "during your turn" / "during turns other than yours" —
            // trailing scope qualifier (Mental Modulation: "costs {1} less to
            // cast during your turn"). Preserves the duration via withDuration.
            .optionallyFollowedBy(
                    anyOf(
                            phrase("during your turn").thenReturn(Duration.Fixed.DURING_YOUR_TURN),
                            phrase("during turns other than yours").thenReturn(Duration.Fixed.DURING_OTHERS_TURN)),
                    Effect.ModifyCost::withDuration)
            // Trailing "except during [phrase] turn" — a duration-
            // exclusion tail (Defense Grid: "Each spell costs {3} more
            // to cast except during its controller's turn."). Consumed
            // as flavor via free-text capture up to the trailing "turn".
            .optionallyFollowedBy(
                    phrase("except during")
                            .then(WORD_OR_CONTRACTION
                                    .suchThat(w -> !w.equalsIgnoreCase("turn"), "non-turn word")
                                    .atLeastOnce())
                            .followedBy(word("turn")),
                    (mc, _) -> mc)
            // Trailing "for each …" multiplier — Ghoultree: "This
            // spell costs {1} less to cast for each creature card in
            // your graveyard."
            .optionallyFollowedBy(CountOfParsers.FOR_EACH, Effect.ModifyCost::withScaleBy)
            // Trailing ", where X is …" — defines the {X} cost variable
            // (Fungal Colossus: "costs {X} less, where X is the number
            // of differently named lands you control.").
            .optionallyFollowedBy(CountOfParsers.WHERE_X_IS, Effect.ModifyCost::withXDefinition)
            // Trailing "if <condition>" — conditional cost reduction
            // (Moonrager's Slash: "costs {2} less to cast if it's night.").
            .optionallyFollowedBy(
                    CONDITION_TAIL.suchThat(c -> c.kind() == Condition.Kind.IF, "if-condition"),
                    Effect.ModifyCost::withCondition);

    /// Post-spell-subject verb-body registry for the generic
    /// [#SPELL_SUBJECT_VERB_CHAIN]. Each entry consumes a subject-less
    /// verb tail and returns a `Function<Subject, Effect>` that binds
    /// the shared spell subject at chain time. Extending this list
    /// with a new arm automatically makes it composable without any
    /// hand-rolled combo parser.
    private static final Parser<Function<Subject, Effect>> SPELL_SUBJECT_VERB = Parser.<Function<Subject, Effect>>anyOf(
            // "cost(s) \<mana\> more/less [to cast|to activate]?" —
            // binds the subject into a [CostSource.Spell] at apply time.
            Parser.sequence(
                            phrase("cost(s)").then(MANA_SYMBOL.atLeastOnce()),
                            COST_DELTA,
                            (amount, delta) -> (Function<Subject, Effect>)
                                    subj -> new Effect.ModifyCost(new CostSource.Spell(subj), amount, delta))
                    .optionallyFollowedBy(anyOf(phrase("to cast"), phrase("to activate")), (fn, _) -> fn),
            // "can't be countered"
            phrase("can't be countered").<Function<Subject, Effect>>thenReturn(Effect.CantBeCountered::new));

    /// "\[spell-subject\] \<verb\> and \<verb\> [and \<verb\>]*" — generic
    /// shared-spell-subject effect chain (Cunning Nightbonder: "Spells
    /// with flash you cast cost {1} less to cast and can't be
    /// countered."). Restricted to ≥2 verbs so single-verb cases
    /// stay reachable via the specialized [#MODIFY_COST] and
    /// [#CANT_BE_COUNTERED] parsers that carry extra tails.
    static final Parser<List<Effect>> SPELL_SUBJECT_VERB_CHAIN = sequence(
            SubjectParsers.SUBJECT,
            SPELL_SUBJECT_VERB
                    .atLeastOnceDelimitedBy(word("and"), Collectors.toUnmodifiableList())
                    .suchThat(fns -> fns.size() >= 2, "two or more spell-subject verbs"),
            (subj, fns) -> fns.stream().<Effect>map(fn -> fn.apply(subj)).toList());

    // Lose ability

    /// "[subject] can't be the target[s] of spells or abilities \[from
    /// [source]\]? / of [what]." Optional "from <source>" tail narrows
    /// the restriction to spells/abilities controlled by sources matching
    /// the selector (Spellbane Centaur: "can't be the targets of blue
    /// spells or abilities from blue sources.").
    static final Parser<Effect.CantBeTargeted> CANT_BE_TARGETED = Parser.<Effect.CantBeTargeted>anyOf(
                    SubjectParsers.SUBJECT
                            .followedBy(phrase("can't be the target(s) of spells or abilities"))
                            .map(Effect.CantBeTargeted::new),
                    sequence(
                            SubjectParsers.SUBJECT.followedBy(phrase("can't be the target(s) of")),
                            SubjectParsers.SUBJECT,
                            Effect.CantBeTargeted::new))
            .optionallyFollowedBy(word("from").then(SELECTOR), Effect.CantBeTargeted::withFromSource);

    /// "[subject] must be blocked [if able]." / "[subject] blocks [if able]
    /// [this turn]." — the former already exists as MUST_BE_BLOCKED; this is
    /// the must-block-as-blocker variant. Also handles the "blocks each
    /// combat if able" adverbial form (Razorgrass Screen) distinct from
    /// a "blocks <subject>" target.
    static final Parser<Effect.MustBlock> MUST_BLOCK = anyOf(
            SubjectParsers.SUBJECT
                    .followedBy(phrase("block(s) each combat"))
                    .map(Effect.MustBlock::new)
                    .optionallyFollowedBy(phrase("if able"), (mb, _) -> mb),
            SubjectParsers.SUBJECT
                    .followedBy(phrase("block(s)"))
                    .map(Effect.MustBlock::new)
                    .optionallyFollowedBy(SubjectParsers.SUBJECT, Effect.MustBlock::withTarget)
                    .optionallyFollowedBy(DURATION, Effect.MustBlock::withDuration)
                    .optionallyFollowedBy(phrase("if able"), (mb, _) -> mb));

    /// "[players] play with [their/its/your] hands revealed."
    static final Parser<Effect.PlayWithHandsRevealed> PLAY_WITH_HANDS_REVEALED = SubjectParsers.PLAYER_SUBJECT
            .followedBy(phrase("play with [your|their|its] hand(s) revealed"))
            .map(Effect.PlayWithHandsRevealed::new);

    /// "Remove [subject] from combat." — Labyrinth of Skophos. Rule 506.4.
    static final Parser<Effect.RemoveFromCombat> REMOVE_FROM_COMBAT = phrase("Remove")
            .then(SubjectParsers.SUBJECT)
            .followedBy(phrase("from combat"))
            .map(Effect.RemoveFromCombat::new);

    /// "[sources] can't cause [player] to sacrifice [what]." —
    /// Tajuru Preserver. Rule 701.16.
    static final Parser<Effect.CantBeForcedToSacrifice> CANT_BE_FORCED_TO_SACRIFICE = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("can't cause")),
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("to sacrifice")),
            SELECTOR,
            Effect.CantBeForcedToSacrifice::new);

    /// "It becomes \[day|night\]." — rule 726 day/night designator flip
    /// (Into the Night). Distinct from daybound/nightbound keyword
    /// triggers; this is a direct state change.
    static final Parser<Effect.BecomeDayNight> BECOME_DAY_NIGHT = phrase("It becomes")
            .then(anyOf(
                    word("day").thenReturn(Effect.BecomeDayNight.DayNight.DAY),
                    word("night").thenReturn(Effect.BecomeDayNight.DayNight.NIGHT)))
            .map(Effect.BecomeDayNight::new);

    /// "[subject] assigns combat damage equal to [its|their] [stat]
    /// rather than [its|their] [stat]." — damage-assignment
    /// substitution (Doran, the Siege Tower). Each stat lands on the
    /// typed [Effect.AssignDamageUsing.Stat] enum.
    private static final Parser<Effect.AssignDamageUsing.Stat> PT_STAT = anyOf(
            word("power").thenReturn(Effect.AssignDamageUsing.Stat.POWER),
            word("toughness").thenReturn(Effect.AssignDamageUsing.Stat.TOUGHNESS));

    static final Parser<Effect.AssignDamageUsing> ASSIGN_DAMAGE_USING = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("assign(s) combat damage equal to [its|their]")),
            PT_STAT.followedBy(phrase("rather than [its|their]")),
            PT_STAT,
            Effect.AssignDamageUsing::new);

    /// "\[subject\] assign\[s\] [its|their] combat damage \[duration\]?
    /// as though [it|they] weren't blocked." — Outmaneuver: "X
    /// target blocked creatures assign their combat damage this
    /// turn as though they weren't blocked."
    static final Parser<Effect.AssignDamageAsThoughUnblocked> ASSIGN_DAMAGE_AS_THOUGH_UNBLOCKED = SubjectParsers.SUBJECT
            .followedBy(phrase("assign(s) [its|their] combat damage"))
            .map(Effect.AssignDamageAsThoughUnblocked::new)
            .optionallyFollowedBy(DURATION, Effect.AssignDamageAsThoughUnblocked::withDuration)
            .followedBy(phrase("as though [it|they] weren't blocked"));

    /// "Have \[subject\] \<verb\>" — causative effect form. The actor of
    /// the inner action is the subject named after "Have", not the
    /// surrounding may-chooser. Extracted from the legacy [#MAY] body so
    /// causatives are reachable from any effect-imperative context (in
    /// practice oracle text only uses them under "may", but the structural
    /// independence makes [Effect#withActor] a no-op for them — the inner
    /// actor is bound at parse time to a non-YOU subject).
    static final Parser<Effect> HAVE_CAUSATIVE = Parser.<Effect>anyOf(
            // "Have [player] <verb>" — Jace's Erasure: "you may have target
            // player mill a card." Dispatches via PLAYER_VERB_BODY plus a
            // dedicated MILL arm since the verb chain doesn't include Mill.
            phrase("Have")
                    .then(SubjectParsers.PLAYER_SUBJECTS)
                    .flatMap(p -> Parser.<Effect>anyOf(
                            PLAYER_VERB_BODY.map(fn -> fn.apply(p)),
                            CardManipulationEffectParsers.MILL_NO_PLAYER.map(a -> new Effect.Mill(p, a)))),
            // "Have [source] deal N damage to [target]" — Goblin Arsonist,
            // Aether Charge.
            sequence(
                    word("have").then(SubjectParsers.ATOMIC_SUBJECT).followedBy(phrase("deal(s)")),
                    AMOUNT.followedBy(phrase("damage to")),
                    SubjectParsers.SUBJECT,
                    Effect.DealDamage::new),
            // "Have [subject] enter tapped as a copy of [target]" — tapped
            // form (must precede plain enter-as-copy because of longer prefix).
            sequence(
                    phrase("Have").then(SubjectParsers.SUBJECT).followedBy(phrase("enter tapped as a copy of")),
                    SubjectParsers.SUBJECT,
                    (subj, src) -> new Effect.EnterAsCopy(subj, src, Selector.Qualifier.Status.TAPPED)),
            // "Have [subject] enter as a copy of [target] [, except it's <P>/<T>]?"
            // — Mirror Image, Quicksilver Gargantuan.
            sequence(
                            phrase("Have").then(SubjectParsers.SUBJECT).followedBy(phrase("enter as a copy of")),
                            SubjectParsers.SUBJECT,
                            Effect.EnterAsCopy::new)
                    .optionallyFollowedBy(
                            string(",").then(phrase("except it's")).then(PT_VALUE), Effect.EnterAsCopy::withOverridePt),
            // "Have [subject] assign its combat damage [duration]? as though it weren't blocked"
            // — Deathcoil Wurm, Lone Wolf, Pride of Lions (no duration);
            // Predatory Focus (with "this turn").
            phrase("Have")
                    .then(SubjectParsers.SUBJECT)
                    .followedBy(phrase("assign [its|their] combat damage"))
                    .map(Effect.AssignDamageAsThoughUnblocked::new)
                    .optionallyFollowedBy(DURATION, Effect.AssignDamageAsThoughUnblocked::withDuration)
                    .followedBy(phrase("as though [it|they] weren't blocked")),
            // "Have [target] <object-verb-body> [duration]?" — Undead
            // Executioner: "have target creature get -2/-2 until end of turn."
            phrase("Have").then(SubjectParsers.SUBJECT).flatMap(EffectParsers::objectVerbBodyWithDuration),
            // "Have [fighter] fight [target]" — Somberwald Stag.
            sequence(
                    phrase("Have").then(SubjectParsers.SUBJECT).followedBy(phrase("fight(s)")),
                    SubjectParsers.SUBJECT,
                    Effect.Fight::new),
            // "Have [blocker] block [attacker] [duration]? [if able]?"
            // — Giant Ambush Beetle.
            sequence(
                            phrase("Have").then(SubjectParsers.SUBJECT).followedBy(phrase("block(s)")),
                            SubjectParsers.SUBJECT,
                            (subj, target) -> new Effect.MustBlock(subj).withTarget(target))
                    .optionallyFollowedBy(DURATION, Effect.MustBlock::withDuration)
                    .optionallyFollowedBy(phrase("if able"), (mb, _) -> mb)
                    .map(mb -> (Effect) mb));

    // ── Master dispatcher ──────────────────────────────────────────────

    static final Parser<Effect> BASE_EFFECT = Parser.<Effect>anyOf(
            // Causative `have <subject> <verb>` — placed near the top so
            // `Have …` claims its prefix before plainer arms try.
            HAVE_CAUSATIVE,
            RemovalEffectParsers.DESTROY,
            RemovalEffectParsers.EXILE,
            RemovalEffectParsers.BOUNCE,
            RemovalEffectParsers.MELD,
            RemovalEffectParsers.SACRIFICE_WITH_SCALE,
            DamageEffectParsers
                    .DEAL_DAMAGE_DOUBLE, // must precede DEAL_DIVIDED_DAMAGE/DEAL_DAMAGE (shares "[subject] deals"
            // prefix)
            DamageEffectParsers.DEAL_DIVIDED_DAMAGE, // must precede DEAL_DAMAGE (shares "deals N damage" prefix)
            DamageEffectParsers.DEAL_DAMAGE,
            DamageEffectParsers.GAIN_LIFE,
            DamageEffectParsers.LOSE_LIFE,
            CardManipulationEffectParsers.DRAW,
            CardManipulationEffectParsers.DISCARD,
            CardManipulationEffectParsers.MILL,
            CardManipulationEffectParsers.SCRY,
            CardManipulationEffectParsers.ADAPT,
            CardManipulationEffectParsers.SURVEIL,
            CardManipulationEffectParsers.SEARCH,
            CardManipulationEffectParsers.SHUFFLE,
            CardManipulationEffectParsers.REORDER_ZONE,
            SWITCH_PT,
            CREWS_WITH_BOOSTED_POWER,
            ATTACH,
            FLIP_COINS,
            ROLL_PLANAR_DIE,
            DOUBLE_MANA,
            MOVE_COUNTERS,
            MAY_ACTIVATE_ANY_TIME,
            CardManipulationEffectParsers.REVEAL,
            PLAY_WITH_TOP_REVEALED,
            CAN_BLOCK, // must precede CANT_BLOCK — both share "can[…]block" prefix
            // "tap or untap [SUBJECT]" — chooser-at-resolution pair
            // (Tolarian Kraken's "When you do, you may tap or untap
            // target creature."). CHANGE_TAP_STATES emits the
            // alternatives as peer effects; [Effect.OneOf] models the
            // at-resolution choice between them as a single Effect.
            // Must precede bare TAP/UNTAP — same prefix.
            TapEffectParsers.CHANGE_TAP_STATES.<Effect>map(Effect.OneOf::new),
            TapEffectParsers.TAP,
            TapEffectParsers.UNTAP,
            CounterEffectParsers.ADD_COUNTERS,
            CounterEffectParsers.DISTRIBUTE_COUNTERS,
            REMOVE_FROM_COMBAT, // must precede REMOVE_COUNTERS (shares "Remove" prefix)
            CANT_BE_FORCED_TO_SACRIFICE,
            ASSIGN_DAMAGE_USING,
            ASSIGN_DAMAGE_AS_THOUGH_UNBLOCKED,
            BECOME_DAY_NIGHT,
            CounterEffectParsers.REMOVE_ALL_COUNTERS, // must precede REMOVE_COUNTERS (shares "remove" prefix)
            CounterEffectParsers.LOSES_ALL_COUNTERS, // "[subject] loses all [type] counters" — Leeches
            CounterEffectParsers.REMOVE_COUNTERS,
            COUNTER_SPELL,
            AbilityGainLoseEffectParsers
                    .HAS_ALL_ABILITIES_OF, // must precede GAIN_ABILITY ("has all <kind> abilities of" prefix)
            AbilityGainLoseEffectParsers.GAIN_ABILITY_CHOICE, // must precede GAIN_ABILITY
            AbilityGainLoseEffectParsers.GAIN_ABILITY,
            MODIFY_PT,
            GAIN_CONTROL,
            ExchangeEffectParsers.EXCHANGE_CONTROL,
            CREATE_ONE_OF_EACH, // must precede CREATE_TOKEN ("Create" prefix)
            CREATE_TOKEN,
            ManaParsers.ADD_MANA,
            TRANSFORM,
            COPY,
            FIGHT,
            PHASE_IN,
            PHASE_OUT,
            INVESTIGATE,
            PROLIFERATE,
            DISCOVER,
            CANT_WIN_GAME, // must precede WIN_GAME so "can't" prefix wins
            CANT_LOSE_GAME, // must precede LOSE_GAME so "can't" prefix wins
            WIN_GAME,
            LOSE_GAME,
            ZONE_MOVE,
            PreventionEffectParsers.PREVENT_NEXT_DAMAGE, // must precede PREVENT (structured shield form)
            sequence(
                    AS_LONG_AS_CONDITION_PREFIX,
                    PreventionEffectParsers.PREVENT_UNIVERSAL,
                    (cond, p) -> p.withCondition(cond)), // must precede PREVENT (same PREVENT_ALL_KIND prefix)
            PreventionEffectParsers.PREVENT,
            PreventionEffectParsers.DAMAGE_CANT_BE_PREVENTED,
            PreventionEffectParsers.THAT_DAMAGE_CANT_BE_PREVENTED,
            CANT_HAVE_COUNTERS,
            CANT_BE_REGENERATED,
            CANT_BE_EQUIPPED,
            UNATTACH,
            CAST_WITHOUT_PAYING, // must precede CAST_AS_THOUGH / CAST_FROM_ZONE (same "may cast" prefix)
            ALTERNATIVE_COST_FOR_SPELLS, // "may pay X rather than pay the mana cost for Y"
            CAST_AS_THOUGH, // must precede CAST_FROM_ZONE (both start with "may cast")
            SPEND_MANA_AS_THOUGH,
            SUPPRESS_ETB_TRIGGERS,
            CANT_BLOCK_ALONE, // must precede CANT_BLOCK
            ONLY_ATTACK_ALONE, // must precede CANT_ATTACK_ALONE — both end with "attack alone"
            POPULATE,
            COUNT_NUMBER_OF,
            CREATE_ONE_OF_EACH, // must precede CREATE_TOKEN ("Create" prefix overlap)
            CANT_ATTACK_ALONE, // must precede CANT_ATTACK
            sequence(
                    AS_LONG_AS_CONDITION_PREFIX,
                    CAN_ATTACK_AS_THOUGH_WITHOUT,
                    (cond, ar) -> ar.withCondition(cond)), // must precede CAN_ATTACK_AS_THOUGH_WITHOUT
            CAN_ATTACK_AS_THOUGH_WITHOUT,
            CAN_BE_BLOCKED_AS_THOUGH_WITHOUT,
            CAN_BE_PLAYED_AS_THOUGH_HAD,
            MUST_ATTACK_OR_BLOCK, // must precede MUST_ATTACK (shares "[subject] attacks" prefix)
            MUST_ATTACK,
            CANT_ATTACK_WHOM, // must precede CANT_ATTACK
            CANT_BLOCK,
            CANT_ATTACK_EXCEPT_FOR, // must precede CANT_ATTACK — leading "Except for" prefix
            CANT_ATTACK,
            CANT_PLAY_LANDS,
            TAKE_INITIATIVE,
            VENTURE_INTO_DUNGEON,
            // "As long as <condition>, [player] can't untap more than <n>
            // <selector> [during <scope>]?" — Winter Orb. Must precede
            // bare UNTAP_LIMIT (longer "as long as" prefix).
            sequence(AS_LONG_AS_CONDITION_PREFIX, UNTAP_LIMIT, (cond, ul) -> ul.withCondition(cond)),
            UNTAP_LIMIT, // must precede DONT_UNTAP (longer "can't untap more than" prefix)
            DONT_UNTAP,
            CAST_COUNT_LIMIT,
            ChooseEffectParsers.CHOOSE_MODAL, // must precede CHOOSE — "Choose one —" prefix
            ChooseEffectParsers.CHOOSE_PLAYER_VOTE, // must precede CHOOSE (starts with "choose")
            ChooseEffectParsers.CHOOSE_TYPE, // must precede CHOOSE
            ChooseEffectParsers.CHOOSE_COLOR, // must precede CHOOSE — "a color" would otherwise match Subject
            ChooseEffectParsers
                    .CHOOSE_NUMBER, // must precede generic CHOOSE — "Choose a number between" is more specific
            ChooseEffectParsers.CHOOSE_QUALITY,
            ChooseEffectParsers.CHOOSE_CARD_NAME,
            SET_BASE_PT_OR, // must precede SET_BASE_PT ("has base power" prefix shared)
            SET_BASE_PT,
            ExchangeEffectParsers.EXCHANGE_ZONES,
            ExchangeEffectParsers.EXCHANGE_LIFE_WITH_PROPERTY,
            MANA_POOL_PERSISTS,
            LOSE_UNSPENT_MANA,
            MANA_SPEND_RESTRICTION,
            SPEND_ONLY_ON_X,
            ADDITIONAL_COST,
            ATTACK_LIMIT,
            BLOCK_LIMIT,
            DOUBLE_COUNTERS_ON, // must precede DOUBLE_PT (shares "Double" prefix; longer "Double the number of each
            // kind of counter" phrase wins)
            DOUBLE_PT,
            DOUBLE_LIFE_TOTAL,
            CHANGE_BASE_POWER, // must precede CHANGE_THE_TARGET — both start with "Change"
            ChooseEffectParsers.CHANGE_THE_TARGET,
            ChooseEffectParsers.NEW_TARGET_MUST_BE,
            ENTER_AS_COPY,
            BECOME_COPY,
            CANT_CYCLE,
            CANT_PHASE_OUT,
            DEFINE_X,
            GAIN_ENERGY,
            CREWS_USING,
            SUPPORT, // "Support N" activation-body form (Joraga Auxiliary)
            CANT_BE_BLOCKED,
            CANT_SEARCH_LIBRARIES,
            PER_TURN_LIMIT, // must precede CANT_CAST (shares "can't cast" prefix)
            CANT_CAST_SELF, // must precede CANT_CAST (self-ref "~" after "can't cast")
            CANT_BE_CAST, // must precede CANT_CAST (shares prefix)
            CANT_CAST,
            RESTRICT_SPELL_TIMING,
            NO_MAXIMUM_HAND_SIZE,
            MAXIMUM_HAND_SIZE_DELTA, // must precede FIXED ("is reduced/increased by N" longer match)
            MAXIMUM_HAND_SIZE_FIXED,
            TAKE_EXTRA_TURN,
            PLAY_LANDS_FROM,
            PLAY_ADDITIONAL_LANDS,
            SKIP,
            RING_TEMPTS,
            LOOK_AT,
            PUT_BACK, // must precede ZONE_MOVE / BOUNCE (shares "put" prefix)
            MAY_CAST_WHILE_SEARCHING, // must precede CAST_FROM_ZONE — both end in "may cast … from …"
            CAST_FROM_ZONE,
            ChooseEffectParsers.CHOOSE_NEW_TARGETS,
            ChooseEffectParsers.CHANGE_ANY_TARGETS,
            DISCARD_ALL_BUT_ONE, // must precede CHOOSE (shares "[player] chooses" head)
            ChooseEffectParsers.CHOOSE,
            BECOME_MONARCH,
            ExchangeEffectParsers.REDISTRIBUTE_LIFE_TOTALS, // must precede EXCHANGE_LIFE_TOTALS (both handle life-total
            // redistribution)
            ExchangeEffectParsers.EXCHANGE_LIFE_TOTALS,
            GET_MARKER,
            CANT_BE_COUNTERED,
            CANT_BE_TARGETED,
            MUST_ATTACK_MINIMUM, // must precede MUST_BE_BLOCKED (shares "must" keyword; player-specific)
            ABLE_TO_BLOCK_DO_SO, // must precede MUST_BE_BLOCKED
            MUST_BE_BLOCKED,
            MUST_BLOCK,
            PLAY_WITH_HANDS_REVEALED,
            LIFE_TOTAL_CANT_CHANGE, // must precede LIFE_TOTAL_BECOMES (shares "[possessive] life total" prefix)
            LIFE_TOTAL_BECOMES,
            RULE_DOESNT_APPLY,
            EFFECT_DOESNT_REMOVE_THIS_AURA,
            ENTER_WITH_CHOSEN_COUNTER, // must precede ENTER_WITH_COUNTERS (more specific: "your choice of …")
            ENTER_WITH_COUNTERS, // must precede ENTER_TAPPED
            ENTER_TAPPED,
            LOSE_SUPERTYPE, // must precede SET_* since all share "are/is" head
            SET_SUPERTYPE,
            TURN_FACE_UP,
            TURN_FACE_DOWN,
            ManaParsers.SPEND_THIS_MANA_ONLY,
            CANT_SPEND_THIS_MANA_TO_CAST_SPELLS,
            ACTIVATION_LIMIT,
            ACTIVATE_ONLY_IF,
            ACTIVATE_ONLY_AS_SORCERY,
            ACTIVATE_ONLY_AS_INSTANT,
            ACTIVATE_ONLY_DURING,
            PLAY_FROM_OUTSIDE,
            STILL_TYPE, // must precede BECOME_PT_TYPE so "they're still" wins
            BECOMES_CHOSEN_TYPE, // must precede BECOME_TYPE_OF_CHOICE ("becomes that …" vs "becomes the …")
            BECOME_TYPE_OF_CHOICE, // must precede BECOME_PT_TYPE ("becomes the …" prefix)
            BECOME_PT_TYPE, // must precede SET_COLORS since both start with "are/is"
            SET_COLORS,
            ADD_CARD_TYPE, // must precede ADD_SUBTYPE and SET_SUBTYPE (shares "are X" head)
            ADD_SUBTYPE, // must precede SET_SUBTYPE (shares "are X" head, adds "in addition to" suffix)
            SET_EVERY_SUBTYPE_TYPE, // must precede SET_SUBTYPE (shares "are every X type" head)
            SET_SUBTYPE,
            REGENERATE,
            AbilityGainLoseEffectParsers.LOSE_ABILITY,
            // ACTIVATED_ABILITIES_OF_COST_EFFECT unifies "Activated abilities of [selector]
            // cost {N} more/less" (ModifyCost) and "cost an additional '[quoted]'" (AdditionalCostOnAbility)
            // behind a shared prefix so neither arm commits to the wrong continuation.
            ACTIVATED_ABILITIES_OF_COST_EFFECT,
            MODIFY_COST,
            // CANT_ACTIVATE's "Activated abilities of …" prefix
            // collides with "Activated abilities cost …" (Suppression
            // Field). Tried last so MODIFY_COST gets first crack at
            // the shared lead-in.
            CANT_ACTIVATE);

    /// "Roll a d\<sides\>." with an outcome table (rule 706.3). Consumes
    /// "Roll a dN. <min>[—<max>]? | <effect>. …" as a single
    /// [Effect.RollDie] whose `outcomes` list captures each row with its
    /// inclusive range (Djinni Windseer: "Roll a d20. 1—9 | Scry 1.
    /// 10—19 | Scry 2. 20 | Scry 3."). Declared after [#BASE_EFFECT]
    /// since each row's result is itself a base effect.
    private static final Parser<Effect.RollDie.Outcome> ROLL_DIE_OUTCOME = anyOf(
            sequence(
                    INTEGER.followedBy(anyOf(string("—"), string("-"))),
                    INTEGER,
                    string("|").then(BASE_EFFECT),
                    Effect.RollDie.Outcome::new),
            sequence(INTEGER, string("|").then(BASE_EFFECT), (n, effect) -> new Effect.RollDie.Outcome(n, n, effect)));

    /// Die spec as a single token — "d20", "d6", etc. — since the "d"
    /// is glued to the digits without a space. Also accepts English
    /// forms like "six-sided die" → 6. Returns the numeric sides.
    private static final Parser<Integer> DIE_SIDES = anyOf(
            Parser.word()
                    .suchThat(
                            w -> w.length() >= 2
                                    && w.charAt(0) == 'd'
                                    && w.substring(1).chars().allMatch(Character::isDigit),
                            "dN die spec")
                    .map(w -> Integer.parseInt(w.substring(1))),
            anyOf(
                            phrase("four-sided").thenReturn(4),
                            phrase("six-sided").thenReturn(6),
                            phrase("eight-sided").thenReturn(8),
                            phrase("ten-sided").thenReturn(10),
                            phrase("twelve-sided").thenReturn(12),
                            phrase("twenty-sided").thenReturn(20))
                    .followedBy(phrase("die")));

    static final Parser<Effect.RollDie> ROLL_DIE = phrase("Roll a")
            .then(DIE_SIDES)
            .map(sides -> new Effect.RollDie(sides, List.of()))
            .optionallyFollowedBy(
                    string(".").then(ROLL_DIE_OUTCOME.followedBy(string(".")).atLeastOnce()),
                    Effect.RollDie::withOutcomes);

    /// `. If you/they do, [effect]` — follow-up clause that attaches to a
    /// preceding [Effect.MayDo] (action wrapped by "you may …").
    /// Consumes the preceding sentence-terminating period so downstream
    /// `EFFECT_SEQUENCE` delimiters see a clean boundary.
    static final Parser<Effect> IF_DO_CONTINUATION =
            phrase(". If [you|they] do,").then(BASE_EFFECT);

    /// `. When you/they do, \[effect\]` — delayed-trigger follow-up
    /// to a preceding [Effect.MayDo] (Thousand Moons Crackshot:
    /// "you may pay {2}{W}. When you do, tap target creature."). The
    /// effect fires as a delayed trigger when the optional payment
    /// resolves; reuses the [Effect.MayDo#ifDone()] slot since
    /// the semantic is identical to "if you do" for the current
    /// engine (both conditional on the optional's completion).
    static final Parser<Effect> WHEN_DO_CONTINUATION = string(".")
            .then(phrase("When [you|they] do"))
            .followedBy(string(","))
            .then(BASE_EFFECT);

    /// `If you/they do, [effect]` — standalone clause-level form of
    /// the "if you do" predication. Unlike [#IF_DO_CONTINUATION] it
    /// doesn't consume a preceding period — EFFECT_SEQUENCE's `.`
    /// delimiter separates the sentences — and it wraps the effect
    /// in a [Effect.Conditional] whose condition references the
    /// prior action (Woeleecher: "Remove a -1/-1 counter from target
    /// creature. If you do, you gain 2 life.").
    static final Parser<Effect.Conditional> IF_YOU_DO_CLAUSE = phrase("If [you|they|the player] do(es)")
            .followedBy(string(","))
            .then(BASE_EFFECT)
            .map(e -> new Effect.Conditional(e, Condition.YouDidIt.YOU_DID_IT));

    /// "Otherwise, \<effect\>." — clause that fires when the previous
    /// clause's gating condition does *not* hold (Phyrexian Boon:
    /// "Enchanted creature gets +2/+1 as long as it's black.
    /// Otherwise, it gets -1/-2."). Wraps the effect in a
    /// [Effect.Conditional] tagged with [Condition.Otherwise], which
    /// the engine resolves against the immediately preceding
    /// conditional clause's predicate.
    static final Parser<Effect.Conditional> OTHERWISE_CLAUSE = phrase("Otherwise")
            .then(string(","))
            .then(BASE_EFFECT)
            .map(e -> new Effect.Conditional(e, Condition.Otherwise.OTHERWISE));

    /// "If [you|they] don't, \<effect\>." / "If no one does, \<effect\>." —
    /// back-reference to a preceding [Effect.MayDo] *not* taken (Blood
    /// Crypt: "you may pay 2 life. If you don't, it enters tapped.";
    /// Browbeat: "Any player may have ~ deal 5 damage to them. If no one
    /// does, target player draws three cards."). "If no one does" is the
    /// multi-player phrasing of the same concept: no player chose to take
    /// the optional action.
    static final Parser<Effect.Conditional> IF_YOU_DONT_CLAUSE = anyOf(
                    phrase("If [you|they] don't"), phrase("If no one does"))
            .followedBy(string(","))
            .then(BASE_EFFECT)
            .map(e -> new Effect.Conditional(e, Condition.YouDidNotDoIt.YOU_DID_NOT_DO_IT));

    /// "When \<event\>, \<action\>" — delayed triggered ability created
    /// by the enclosing effect (rule 603.7b). Distinct from top-level
    /// triggered abilities (which are parsed as whole abilities by
    /// [OracleParser#TRIGGERED]); this form appears mid-body inside an
    /// activated or spell ability, scheduling `action` on the next
    /// matching event (Matopi Golem: "{1}: Regenerate this creature.
    /// When it regenerates this way, put a -1/-1 counter on it.").
    /// The trigger-event list flattens into one DelayedTrigger per
    /// event so peer-event disjunctions (like "enters or dies") fan
    /// out the same way they do at the top level.
    static final Parser<List<Effect>> DELAYED_TRIGGER_CLAUSE = sequence(
            phrase("When").then(TriggerEventParsers.TRIGGER_EVENT).followedBy(string(",")),
            BASE_EFFECT,
            (events, action) -> events.stream()
                    .<Effect>map(ev -> new Effect.DelayedTrigger(ev, action))
                    .toList());

    /// "At \<beginning-of-step\>, \<effect\>." — delayed trigger
    /// scheduled at a step boundary (False Memories: "At the
    /// beginning of the next end step, exile seven cards from your
    /// graveyard."). Distinct from a top-level triggered ability —
    /// this clause appears inside a spell's effect body.
    static final Parser<Effect.DelayedTrigger> AT_DELAYED_TRIGGER_CLAUSE = sequence(
            phrase("At").then(TriggerEventParsers.TRIGGER_EVENT).followedBy(string(",")),
            BASE_EFFECT,
            (events, action) -> new Effect.DelayedTrigger(events.getFirst(), action));

    /// "\<duration\>, whenever \<event\>, \<action\>." — recurring
    /// floating trigger (Bubbling Muck: "Until end of turn, whenever
    /// a player taps a Swamp for mana, that player adds an
    /// additional {B}."). Also: "Whenever \<event\> this turn,
    /// \<action\>." (Death Frenzy: "Whenever a creature dies this
    /// turn, you gain 1 life.") with implicit
    /// [Duration.Fixed#THIS_TURN].
    static final Parser<Effect.FloatingTrigger> FLOATING_TRIGGER_CLAUSE = anyOf(
            sequence(
                    DURATION.followedBy(string(",")),
                    phrase("whenever").then(TriggerEventParsers.TRIGGER_EVENT).followedBy(string(",")),
                    BASE_EFFECT,
                    (dur, events, action) -> new Effect.FloatingTrigger(dur, events.getFirst(), action)),
            sequence(
                    phrase("Whenever")
                            .then(TriggerEventParsers.TRIGGER_EVENT)
                            .followedBy(phrase("this turn"))
                            .followedBy(string(",")),
                    BASE_EFFECT,
                    (events, action) ->
                            new Effect.FloatingTrigger(Duration.Fixed.THIS_TURN, events.getFirst(), action)));

    /// "You may \[alternative\] rather than pay \[this spell's|the\] mana cost."
    /// — inline alternative casting cost (rule 117.9). The `alternative`
    /// is a sacrifice effect today (Delraich, Crash, Pulverize, Flare of
    /// Denial); other alt-cost shapes ("pay colored mana", "exile cards")
    /// will slot into the inner `anyOf` as each shape gets a typed parser.
    /// The implicit actor is the spell's controller (`YOU`).
    static final Parser<Effect.AlternativeCastingCost> ALTERNATIVE_CASTING_COST = phrase("You may")
            .then(Parser.<Effect>anyOf(phrase("Sacrifice(s)")
                    .then(SubjectParsers.SUBJECT)
                    .map(what -> new Effect.Sacrifice(Subject.player(PlayerRef.Pronoun.YOU), what))))
            .followedBy(phrase("rather than pay [this spell's|the] mana cost"))
            .map(Effect.AlternativeCastingCost::new);

    /// "\<chooser\> may \<effect\>" — optional effect-action. The chooser
    /// is captured before the verb and pushed into the inner action via
    /// [Effect#withActor], which rebinds any `YOU`-placeholder actor in
    /// the inner tree to the captured chooser. The body is the full
    /// [#BASE_EFFECT] dispatcher so any verb-imperative the engine
    /// recognises is automatically may-able. Causative
    /// `have <other> <verb>` effects bind their own actor at parse time;
    /// `withActor` is a no-op for them.
    private static final Parser<Effect.MayDo> MAY_DO = sequence(
                    SubjectParsers.PLAYER_SUBJECTS.followedBy(word("may")),
                    BASE_EFFECT,
                    (chooser, action) -> new Effect.MayDo(chooser, action.withActor(chooser)))
            .optionallyFollowedBy(IF_DO_CONTINUATION, Effect.MayDo::withIfDone)
            .optionallyFollowedBy(WHEN_DO_CONTINUATION, Effect.MayDo::withIfDone);

    /// Cost-imperative may-payment, wired with this class's `if/when
    /// you do` continuations. Built by calling [CostParsers#may]
    /// rather than referenced as a static field, because CostParsers
    /// initializes before EffectParsers and a direct cross-class
    /// field reference would NPE on the continuations.
    private static final Parser<Effect.MayPay> MAY_PAY = CostParsers.may(IF_DO_CONTINUATION, WHEN_DO_CONTINUATION);

    /// Top-level `<chooser> may …` dispatcher. Tries the cost-imperative
    /// path ([#MAY_PAY] → [Effect.MayPay]) before the effect-imperative
    /// path ([#MAY_DO] → [Effect.MayDo]) so genuinely-cost shapes
    /// (mana payment, life payment, multi-cost "or"-disjunctions) are
    /// caught structurally; verbs whose surface form overlaps with
    /// effect-imperative parsers (bare "discard a card" / "sacrifice
    /// a creature" / "exile target X") fall through to `MAY_DO`
    /// because of [CostParsers#may]'s `suchThat` filter.
    static final Parser<Effect> MAY = Parser.<Effect>anyOf(MAY_PAY, MAY_DO);

    /// "\<duration\>, any time you could activate a mana ability, \<may-action\>."
    /// — duration-scoped optional action at mana-ability speed (Channel:
    /// "Until end of turn, any time you could activate a mana ability, you may
    /// pay 1 life."). The "If you do, …" consequence is a separate [Conditional]
    /// clause in the sequence.
    static final Parser<Effect.MayPayAnyTimeForMana> MAY_PAY_ANY_TIME_FOR_MANA = sequence(
            DURATION.followedBy(string(",")),
            phrase("any time you could activate a mana ability")
                    .followedBy(string(","))
                    .then(MAY),
            Effect.MayPayAnyTimeForMana::new);

    /// Scope of a delayed "beginning of \[step\]" timing — whose next
    /// occurrence counts.
    private static final Parser<DelayedTiming.Scope> DELAYED_SCOPE = anyOf(
            phrase("the next turn's").thenReturn(DelayedTiming.Scope.NEXT_TURN),
            phrase("your next").thenReturn(DelayedTiming.Scope.YOUR_NEXT),
            phrase("an opponent's next").thenReturn(DelayedTiming.Scope.OPPONENT_NEXT),
            // "the next" before a bare step name — Ideas Unbound:
            // "Discard three cards at the beginning of the next end
            // step." Treated as the next occurrence regardless of
            // whose turn it is.
            phrase("the next").thenReturn(DelayedTiming.Scope.NEXT_TURN),
            phrase("each").thenReturn(DelayedTiming.Scope.EACH));

    /// "at \<timing\>" suffix creating a delayed triggered ability
    /// (rule 603.7, Blessed Wine: "Draw a card at the beginning of the
    /// next turn's upkeep."). Covers the "beginning of \[scope\]
    /// \[step\]" family plus the bare "end of turn" / "end of combat"
    /// windows.
    private static final Parser<DelayedTiming> AT_DELAYED_TIMING = phrase("at")
            .then(anyOf(
                    sequence(
                            phrase("the beginning of").then(DELAYED_SCOPE),
                            STEP_NAME.followedBy(phrase("step").orElse("")),
                            DelayedTiming.BeginningOfStep::new),
                    phrase("end of turn").thenReturn(DelayedTiming.EndOfTurn.END_OF_TURN),
                    phrase("end of combat").thenReturn(DelayedTiming.EndOfCombat.END_OF_COMBAT)));

    public static final Parser<Effect> EFFECT = Parser.<Effect>anyOf(
                    // Must precede MAY — MAY's "pay <cost>" arm would
                    // otherwise absorb the leading mana cost and leave
                    // "rather than pay the mana cost for …" dangling
                    // (Fist of Suns).
                    ALTERNATIVE_COST_FOR_SPELLS,
                    ALTERNATIVE_CASTING_COST, // must precede MAY — "You may <alt> rather than pay" is not a generic MAY
                    // "[player] may …" — single entry point for every
                    // may-wrapped action. Must precede BASE_EFFECT so "you
                    // may X" is captured as Effect.MayDo rather than a
                    // plain Effect.
                    MAY,
                    ReplacementEffectParsers.REDIRECT_DAMAGE, // specialized shape, try before generic REPLACE
                    ReplacementEffectParsers.REPLACE_LIFE_FLOOR, // specialized shape, try before generic REPLACE
                    ReplacementEffectParsers.REPLACE_MANA_DOUBLE, // Mana Reflection
                    ReplacementEffectParsers.REPLACE_NEXT_TIME,
                    // "<duration>, <REPLACE>." — duration-scoped
                    // replacement (Hallowed Moonlight: "Until end
                    // of turn, if a creature would enter and it
                    // wasn't cast, exile it instead."). Must
                    // precede the bare REPLACE arm.
                    sequence(
                            DURATION.followedBy(string(",")),
                            ReplacementEffectParsers.REPLACE,
                            (dur, r) -> r.withDuration(dur)),
                    ReplacementEffectParsers.REPLACE,
                    // Panharmonicon-style trigger duplication; shares the
                    ADDITIONAL_ETB_TRIGGERS,
                    ReplacementEffectParsers.FOR_EACH_PLAYER_EFFECT, // must precede FOR_EACH_EFFECT
                    ReplacementEffectParsers.FOR_EACH_AMONG_EFFECT, // must precede FOR_EACH_EFFECT
                    ReplacementEffectParsers.FOR_EACH_EFFECT, // must precede BASE_EFFECT
                    // "If <typed-condition>, <override> instead." —
                    // conditional-override replacement (River of
                    // Tears). Must precede the bare prefix arm since
                    // the trailing "instead" is what distinguishes
                    // the override.
                    ReplacementEffectParsers.CONDITIONAL_OVERRIDE,
                    // "If \<typed-condition\>, \<effect\>" — typed
                    // prefix conditional (Artificer's Epiphany,
                    // Tezzeret's Ambition, Idle Thoughts, Fated
                    // Retribution, Tavern Swindler, …). Tried before
                    // BASE_EFFECT since the leading "If" otherwise
                    // doesn't decompose.
                    sequence(IF_PREFIX_CONDITION, BASE_EFFECT, (c, e) -> (Effect) new Effect.Conditional(e, c)),
                    sequence(UNLESS_PREFIX_CONDITION, BASE_EFFECT, (c, e) -> (Effect) new Effect.Conditional(e, c)),
                    BASE_EFFECT,
                    // "[kind] abilities of [scope] trigger N additional
                    // time(s)." — placed after BASE_EFFECT since its
                    // greedy word() prefix would otherwise eat
                    // unrelated "\[word\] abilities …" openings (e.g.,
                    // Suppression Field: "Activated abilities cost
                    // {2} more …") before MODIFY_COST gets to try.
                    ABILITY_KIND_TRIGGERS_ADDITIONAL)
            // Optional "at …" delayed-trigger suffix wraps the action
            // in a delayed-trigger schedule (Blessed Wine).
            .optionallyFollowedBy(AT_DELAYED_TIMING, (e, when) -> new Effect.Delayed(e, when))
            // "<effect> instead if <condition>" — effect-first conditional
            // override (Feed the Clan: "You gain 10 life instead if you
            // control a creature with power 4 or greater."). Must precede
            // the bare CONDITION_TAIL so "instead" is consumed before "if"
            // is seen as a trailing condition.
            .optionallyFollowedBy(
                    phrase("instead")
                            .then(CONDITION_TAIL.suchThat(c -> c.kind() == Condition.Kind.IF, "if-kind condition")),
                    (e, c) -> (Effect) new Effect.ConditionalOverride(c, e))
            // Trailing typed condition — "unless [player] pays [cost]"
            // (Mana Leak / Rhystic Deluge / Tyrannize / Qal Sisma
            // Behemoth) and "unless [player] controls [selector]"
            // (Mindless Null, Desperate Castaways). Wraps the effect
            // in a [Effect.Conditional]; only structurally-recognized
            // shapes attach.
            .optionallyFollowedBy(CONDITION_TAIL, (e, c) -> new Effect.Conditional(e, c));

    // ── Tie the recursive knot (rule CLAUSE) ──────────────────────────

    /// Populates [#CLAUSE] with a multi-effect clause — either a
    /// shared-subject chain that produces several effects
    /// ([#PLAYER_ACTOR_AND_CHAIN], [#SUBJECT_AND_VERB_CHAIN]) or a
    /// single [#EFFECT]. The [OracleParser#EFFECT_SEQUENCE] level
    /// flattens these lists so a trigger or spell body sees a flat
    /// `List<Effect>` regardless of whether each clause parsed one
    /// or many effects.
    static {
        CLAUSE.definedAs(Parser.anyOf(
                // Syntactic chains — distribute a shared subject over multiple
                // verb bodies joined by "and".
                PLAYER_ACTOR_AND_CHAIN,
                // GAIN_CONTROL must precede SUBJECT_AND_VERB_CHAIN because
                // SUBJECT_AND_VERB_CHAIN uses flatMap and commits to the
                // subject before discovering it can't parse "gains control of"
                // as a chain body (Crag Saurian: "that source's controller
                // gains control of this creature").
                GAIN_CONTROL.map(List::of),
                SUBJECT_AND_VERB_CHAIN,
                // Two-effect clauses that must win over their bare single-effect
                // counterparts (the trailing "and X" would otherwise be left for
                // EFFECT_SEQUENCE's delimiter, losing context).
                DamageEffectParsers
                        .DEAL_DAMAGE_SPLIT_THREE, // must precede DEAL_DAMAGE_SPLIT (three-target form shares "deals N
                // damage to A, M damage to B" prefix)
                DamageEffectParsers
                        .DEAL_DAMAGE_SPLIT, // must precede DEAL_DAMAGE (shares "[source] deals N damage to A" prefix)
                CounterEffectParsers
                        .ADD_COUNTERS_SEPARATE_PAIR, // must precede ADD_COUNTERS_PAIR (longer "on S1 and" match)
                CounterEffectParsers.ADD_COUNTERS_CHOICE.map(
                        List::<Effect>of), // must precede ADD_COUNTERS ("Put ... or ..." shared-target choice)
                CounterEffectParsers.ADD_COUNTERS_LIST, // 3+ pairs; must precede ADD_COUNTERS_PAIR
                CounterEffectParsers.ADD_COUNTERS_PAIR, // must precede ADD_COUNTERS
                RemovalEffectParsers.EXILE_OBJECT_AND_ZONE, // must precede EXILE (possessive-zone second target)
                BECOMES_SUBTYPE_WITH_BASE_PT, // emits SetSubtype + SetBasePT peer effects (Omnibian)
                CANT_ATTACK_BLOCK_OR_CREW, // emits three peer restrictions (attack/block/crew)
                CANT_ATTACK_OR_BLOCK_ALONE, // emits two peer restrictions (CantAttack-Alone + CantBlockAlone)
                CANT_ATTACK_OR_BLOCK, // emits two peer restrictions (CantAttack + CantBlock)
                CANT_CHAIN, // "<subj> can't <verb> or <verb>" peer-restriction fan-out
                SPELL_SUBJECT_VERB_CHAIN, // "<spell-subj> <verb> and <verb>" peer-effect fan-out
                SET_PROPERTY_VALUES, // "<subj>'s <prop[, and prop]*> [is|are each] equal to <amount>"
                TapEffectParsers.CHANGE_TAP_STATES, // "Tap or untap X" → Tap + Untap pair; must precede TAP
                STILL_A_CARDTYPE_FLAVOR, // no-op flavor clarification — emits List.of()
                IF_YOU_DO_CLAUSE.map(List::<Effect>of), // "If you do, <effect>" — wraps in Conditional
                IF_YOU_DONT_CLAUSE.map(List::<Effect>of), // "If you don't, <effect>"
                OTHERWISE_CLAUSE.map(List::<Effect>of), // "Otherwise, <effect>" — negates prior clause's condition
                DELAYED_TRIGGER_CLAUSE, // "When <event>, <effect>" mid-body delayed trigger (Matopi Golem)
                AT_DELAYED_TRIGGER_CLAUSE.map(
                        List::<Effect>of), // "At the beginning of <step>, <effect>" (False Memories)
                FLOATING_TRIGGER_CLAUSE.map(List::<Effect>of), // "<duration>, whenever..." Bubbling Muck
                MAY_PAY_ANY_TIME_FOR_MANA.map(
                        List::<Effect>of), // "<duration>, any time you could activate a mana ability, you may..."
                // Channel
                PLAY_CARDS.map(List::<Effect>of), // "Until <dur>, you may play those cards" (Commune with Lava)
                TAP_FOR_MANA.map(List::<Effect>of), // "Until <dur>, you may tap [target] for mana" (Piracy)
                CREATE_TOKEN_PAIR, // "Create [amt] [token] and [amt] [token]" — two distinct tokens (Forbidden
                // Friendship)
                ROLL_DIE.map(List::<Effect>of), // "Roll a dN" + outcome table (Djinni Windseer)
                // Fallback — a single effect produced by the usual EFFECT dispatcher.
                EFFECT.map(List::of)));
    }
}
