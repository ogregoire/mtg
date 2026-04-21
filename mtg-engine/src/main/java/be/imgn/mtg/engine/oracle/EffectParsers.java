package be.imgn.mtg.engine.oracle;

import static be.imgn.mtg.engine.oracle.Words.anyCiWord;
import static be.imgn.mtg.engine.oracle.Words.anyWord;
import static be.imgn.mtg.engine.oracle.Words.ciWords;
import static be.imgn.mtg.engine.oracle.Words.phrase;
import static be.imgn.mtg.engine.oracle.Words.w;
import static be.imgn.mtg.engine.oracle.Words.words;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.caseInsensitive;
import static com.google.common.labs.parse.Parser.consecutive;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.labs.parse.CharacterSet;
import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

import be.imgn.mtg.engine.turn.Phase;
import be.imgn.mtg.engine.turn.Step;

/// Parsers for effect productions in oracle text.
final class EffectParsers {
    private EffectParsers() {}

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
    private static final Parser<String> AS_LONG_AS_TOKEN =
            consecutive(CharacterSet.charsIn("[A-Za-z0-9'+/-]"), "as-long-as word");

    /// "as long as [condition]" — [Duration.ForAsLongAs] captured as
    /// free text (allows English contractions such as "it's").
    private static final Parser<Duration> AS_LONG_AS = ciWords("as long as")
            .then(AS_LONG_AS_TOKEN.atLeastOnce().map(words -> String.join(" ", words)))
            .map(Duration::forAsLongAs);

    /// Owner for "until \[owner\]'s next \[step\]" — a possessive subject
    /// ("its controller") or a possessive pronoun ("your", "their").
    private static final Parser<Subject> UNTIL_NEXT_STEP_OWNER = anyOf(
            SubjectParsers.POSSESSIVE.followedBy(string("'s")),
            word("your").thenReturn(Subject.player(Subject.PlayerRef.YOU)),
            word("their").thenReturn(Subject.player(Subject.PlayerRef.THEY)));

    /// "until \[owner\]'s next \[step-name\] step" — anchored duration that
    /// ends at the next occurrence of a specific step belonging to a
    /// specific player (Orcish Farmer: "until its controller's next
    /// untap step").
    private static final Parser<Duration> UNTIL_NEXT_STEP = sequence(
            phrase("until").then(UNTIL_NEXT_STEP_OWNER),
            word("next").then(TriggerEventParsers.STEP_NAME),
            (owner, step) -> (Duration) new Duration.UntilNextStep(owner, step));

    static final Parser<Duration> DURATION = anyOf(
            ciWords("until end of turn").thenReturn(Duration.untilEndOfTurn()),
            ciWords("until your next turn").thenReturn(Duration.untilYourNextTurn()),
            ciWords("until end of combat").thenReturn(Duration.untilEndOfCombat()),
            UNTIL_NEXT_STEP,
            ciWords("this turn").thenReturn(Duration.thisTurn()),
            ciWords("on each of your turns").thenReturn(Duration.eachYourTurn()),
            AS_LONG_AS);

    private static final Parser<String> KEYWORD_NAME = anyOf(
            ciWords("first strike"),
            ciWords("double strike"),
            ciWords("death touch").thenReturn("deathtouch"),
            w("flying"),
            w("trample"),
            w("haste"),
            w("vigilance"),
            w("lifelink"),
            w("deathtouch"),
            w("hexproof"),
            w("indestructible"),
            w("menace"),
            w("reach"),
            w("defender"),
            w("flash"),
            w("fear"),
            w("intimidate"),
            w("shroud"),
            w("wither"),
            w("infect"),
            w("prowess"),
            word().suchThat(k -> k.length() > 2 && Character.isLowerCase(k.charAt(0)), "keyword name"));

    private static final Parser<List<String>> KEYWORD_LIST = KEYWORD_NAME.atLeastOnceDelimitedBy(",");

    // ── Effects ────────────────────────────────────────────────────────

    /// The implicit "you" subject — used when an effect omits the player
    /// (e.g., "Draw a card." = "you draw a card.").
    private static final Subject YOU = Subject.player(Subject.PlayerRef.YOU);

    /// "Switch [subject]'s power and toughness [duration]?." — swap P/T
    /// (About Face: "Switch target creature's power and toughness until
    /// end of turn.").
    static final Parser<Effect.SwitchPT> SWITCH_PT = w("switch")
            .then(SubjectParsers.SUBJECT)
            .followedBy(string("'s"))
            .followedBy(words("power and toughness"))
            .map(Effect.SwitchPT::new)
            .optionallyFollowedBy(DURATION, Effect.SwitchPT::withDuration);

    /// "[subject] crews [selector] as though its power were N greater." —
    /// Hotshot Mechanic. The power delta is captured as a plain integer.
    static final Parser<Effect.CrewsWithBoostedPower> CREWS_WITH_BOOSTED_POWER = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("crew(s)")),
            SelectorParsers.SELECTOR.followedBy(words("as though its power were")),
            SelectorParsers.INTEGER.followedBy(word("greater")),
            Effect.CrewsWithBoostedPower::new);

    /// "Roll the planar die." — Planechase effect (Fractured Powerstone).
    static final Parser<Effect.RollPlanarDie> ROLL_PLANAR_DIE =
            ciWords("roll the planar die").thenReturn(Effect.RollPlanarDie.ROLL_PLANAR_DIE);

    /// "Move [N|all] [type]? counter(s) from [source] onto [dest]." —
    /// Fate Transfer, Power Conduit. Both the count (integer word or
    /// "all") and the type are optional.
    /// "[player] may activate [kind] abilities any time [player] could
    /// cast [an instant|a sorcery]." — Leonin Shikari: "You may activate
    /// equip abilities any time you could cast an instant." The
    /// instant/sorcery speed is preserved since the two produce very
    /// different timing permissions.
    static final Parser<Effect.MayActivateAnyTime> MAY_ACTIVATE_ANY_TIME = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(words("may activate")),
            word().followedBy(word("abilities")),
            words("any time")
                    .then(SubjectParsers.PLAYER_SUBJECT)
                    .then(words("could cast"))
                    .then(anyOf(
                            words("an instant").thenReturn(Effect.MayActivateAnyTime.Speed.INSTANT),
                            words("a sorcery").thenReturn(Effect.MayActivateAnyTime.Speed.SORCERY))),
            (player, kind, speed) -> new Effect.MayActivateAnyTime(player, kind.toLowerCase(), speed));

    static final Parser<Effect.MoveCounters> MOVE_COUNTERS = sequence(
            w("move").then(anyOf(word("all").thenReturn(Amount.reference("all")), SelectorParsers.AMOUNT)),
            anyOf(
                            SelectorParsers.COUNTER_TYPE.followedBy(phrase("counter(s)")),
                            phrase("counter(s)").thenReturn((CounterType) null))
                    .followedBy(word("from")),
            SubjectParsers.SUBJECT.followedBy(word("onto")),
            SubjectParsers.SUBJECT,
            Effect.MoveCounters::new);

    /// "Double the amount of each type of unspent mana [player] has." —
    /// Doubling Cube / Mana Reflection.
    static final Parser<Effect.DoubleMana> DOUBLE_MANA = ciWords("double the amount of each type of unspent mana")
            .then(SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("[has|have]")))
            .map(Effect.DoubleMana::new);

    /// "Flip [N] coin[s] [and ignore one]?" — Krark's Thumb replacement
    /// body. Captures the count and the ignore tail for later resolution.
    static final Parser<Effect.FlipCoins> FLIP_COINS = w("flip")
            .then(SelectorParsers.AMOUNT)
            .followedBy(phrase("coin(s)"))
            .<Effect.FlipCoins>map(Effect.FlipCoins::new)
            .optionallyFollowedBy(
                    words("and ignore").then(SelectorParsers.NUMBER), (fc, n) -> new Effect.FlipCoins(fc.count(), n));

    /// "Attach [what] to [target]." — move an Aura/Equipment (Aura
    /// Finesse: "Attach target Aura you control to target creature.").
    static final Parser<Effect.Attach> ATTACH = sequence(
            w("attach").then(SubjectParsers.SUBJECT), w("to").then(SubjectParsers.SUBJECT), Effect.Attach::new);

    /// "[player] <verb-body>" — a subject-less player-actor verb body,
    /// rebound to the player captured by [#PLAYER_ACTOR_AND_CHAIN].
    /// Each arm is the same body the bare (YOU-defaulted) parsers use,
    /// just with the actor plumbed in.
    private static Parser<Effect> playerVerbBody(Subject actor) {
        return Parser.<Effect>anyOf(
                // "reveals their hand" — sub-hand reveal; a plain SUBJECT
                // wouldn't match "their hand" since it isn't a card-level
                // selector.
                phrase("reveal(s)")
                        .then(CardManipulationEffectParsers.POSSESSIVE_HAND)
                        .map(hand -> new Effect.Reveal(actor, hand)),
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
            phrase("reveal(s)")
                    .then(CardManipulationEffectParsers.POSSESSIVE_HAND)
                    .map(hand -> actor -> new Effect.Reveal(actor, hand)),
            // Inline "loses N life" without LOSE_LIFE_NO_PLAYER's optional
            // CountOfParsers.FOR_EACH tail — which can swallow "and <verb>" via its
            // trailing subject parser.
            phrase("lose(s)")
                    .then(SelectorParsers.AMOUNT)
                    .followedBy(word("life"))
                    .map(amt -> actor -> new Effect.LoseLife(actor, amt)),
            phrase("gain(s)")
                    .then(SelectorParsers.AMOUNT)
                    .followedBy(word("life"))
                    .map(amt -> actor -> new Effect.GainLife(actor, amt)),
            CardManipulationEffectParsers.DRAW_NO_PLAYER.map(amt -> actor -> new Effect.Draw(actor, amt)),
            CardManipulationEffectParsers.DISCARD_NO_PLAYER.map(d -> actor -> new Effect.Discard(actor, d)),
            // "get {E}..." — energy counter gain (Live Fast).
            phrase("get(s)").then(ENERGY_SYMBOLS).map(n -> actor -> new Effect.GainEnergy(actor, n)));

    /// "[player] <action1>, <action2>, and <actionN>" — a shared player
    /// actor distributed across an Oxford-comma-delimited list of
    /// subject-less verb bodies (Trapfinder's Trick: "[player] X and Y.";
    /// Live Fast: "You draw two cards, lose 2 life, and get {E}{E}.").
    /// Returns the pair as a `List<Effect>` so [OracleParser#EFFECT_SEQUENCE] flattens it into the surrounding
    /// list — the chain is a syntactic clause that produces multiple
    /// effects, not a single compound one.
    static final Parser<List<Effect>> PLAYER_ACTOR_AND_CHAIN = sequence(
            SubjectParsers.PLAYER_SUBJECTS,
            MtgParsers.andList(PLAYER_VERB_BODY).suchThat(list -> list.size() >= 2, "at least two verb bodies"),
            (actor, bodies) -> bodies.stream().map(fn -> fn.apply(actor)).toList());

    /// Tail of a "[player]? play with the top card of [poss] library
    /// revealed" phrase — consumes the verb and its body, leaving only
    /// the optional leading subject for the outer parser.
    private static final Parser<String> PLAY_WITH_TOP_REVEALED_TAIL = anyCiWord("plays", "play")
            .followedBy(phrase("with the top card of [your|their|its] [libraries|library] revealed"));

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
            words("as though")
                    .then(word().atLeastOnce().map(ws -> String.join(" ", ws)))
                    .<Effect.CanBlock.Capability>map(Effect.CanBlock.Capability.AsThoughState::new),
            // "any number of \[selector\]" — Palace Guard.
            words("any number of")
                    .then(SelectorParsers.SELECTOR)
                    .<Effect.CanBlock.Capability>map(Effect.CanBlock.Capability.AnyNumberOf::new),
            // "\[amount\] additional \[selector\] \[each combat\]?" —
            // Foriysian Brigade / Coastline Chimera. "each combat"
            // is flavor since Additional implies per-combat.
            sequence(
                            anyOf(anyWord("an", "a").thenReturn(Amount.exact(1)), SelectorParsers.AMOUNT)
                                    .followedBy(word("additional")),
                            SelectorParsers.SELECTOR,
                            (count, what) ->
                                    (Effect.CanBlock.Capability) new Effect.CanBlock.Capability.Additional(count, what))
                    .optionallyFollowedBy(words("each combat"), (c, _) -> c),
            // "\[selector\] as though it had \[keyword\]" — Heartwood
            // Dryad, Foriysian Interceptor.
            sequence(SelectorParsers.SELECTOR.followedBy(words("as though it had")), KEYWORD_NAME, (what, kw) ->
                    (Effect.CanBlock.Capability) new Effect.CanBlock.Capability.AsThoughHad(what, kw)),
            // "only \[selector\]" — Gloomwidow: "can block only creatures
            // with flying."
            word("only")
                    .then(SelectorParsers.SELECTOR)
                    .<Effect.CanBlock.Capability>map(Effect.CanBlock.Capability.Only::new));

    /// "[subject] can block \[capability\] \[duration\]?" — unified
    /// positive block-capability expansion. Covers the five oracle
    /// shapes captured by [Effect.CanBlock.Capability] variants
    /// (AnyNumberOf, Additional, AsThoughHad, AsThoughState, Only).
    static final Parser<Effect.CanBlock> CAN_BLOCK = sequence(
                    SubjectParsers.SUBJECT.followedBy(words("can block")), CAN_BLOCK_CAPABILITY, Effect.CanBlock::new)
            .optionallyFollowedBy(DURATION, Effect.CanBlock::withDuration);

    // Counterspell

    /// A condition-clause token — like a word but also accepts mana symbols
    /// (`{X}`, `{2}{R}`) and apostrophes so predicates such as "its
    /// controller pays {X}" round-trip verbatim.
    private static final Parser<String> CONDITION_TOKEN =
            consecutive(CharacterSet.charsIn("[A-Za-z0-9'{}+/-]"), "condition token");

    /// Condition tail on a counterspell — either `if [clause]` (Ertai's
    /// Trickery: "if it was kicked") or `unless [clause]` (Clash of Wills:
    /// "unless its controller pays {X}"). The two are semantic negations
    /// of one another and land in [Condition.Kind#IF] /
    /// [Condition.Kind#UNLESS] respectively.
    private static final Parser<Condition> COUNTER_CONDITION = sequence(
            anyOf(w("if").thenReturn(Condition.Kind.IF), w("unless").thenReturn(Condition.Kind.UNLESS)),
            CONDITION_TOKEN.atLeastOnce().map(words -> String.join(" ", words)),
            Condition::new);

    static final Parser<Effect.CounterSpell> COUNTER_SPELL = w("counter")
            .then(SubjectParsers.SUBJECT)
            .map(Effect.CounterSpell::new)
            .optionallyFollowedBy(COUNTER_CONDITION, Effect.CounterSpell::withCondition);

    // Ability modification

    /// Ability enclosed in double quotes — used by [#GAIN_ABILITY] for
    /// granted abilities like `Creatures you control have "{T}: Add {G}."`
    /// Refers to [OracleParser#ABILITY] so any ability shape (activated,
    /// triggered, spell, keyword) is accepted. Wrapped in a one-element list
    /// to match the keyword-list shape downstream.
    private static final Parser<List<Ability>> QUOTED_ABILITY =
            OracleParser.ABILITY.optionallyFollowedBy(".").between("\"", "\"").map(List::of);

    /// "During your turn," — duration prefix that applies
    /// [Duration#duringYourTurn()] to the effect it precedes
    /// (e.g., Sporeback Wolf, Daggersail Aeronaut).
    private static final Parser<Duration> DURING_YOUR_TURN =
            ciWords("during your turn").followedBy(string(",")).thenReturn(Duration.duringYourTurn());

    /// "During turns other than yours," — duration prefix scoped to
    /// turns belonging to another player (e.g., Mesa Lynx).
    private static final Parser<Duration> DURING_OTHERS_TURN =
            ciWords("during turns other than yours").followedBy(string(",")).thenReturn(Duration.duringOthersTurn());

    /// "As long as <predicate>," — duration prefix applied to the following
    /// effect (e.g., Dwarfhold Champion: "As long as this creature is
    /// equipped, it gets +0/+2."). Mirrors the suffix form [#AS_LONG_AS]
    /// but fronts the clause before the effect; the predicate runs to the
    /// comma.
    private static final Parser<Duration> AS_LONG_AS_PREFIX = ciWords("as long as")
            .then(WORD_OR_CONTRACTION.atLeastOnce().map(words -> String.join(" ", words)))
            .followedBy(string(","))
            .map(Duration::forAsLongAs);

    private static final Parser<Effect.GainAbility> GAIN_ABILITY_CORE = sequence(
            SubjectParsers.SUBJECT.followedBy(anyWord("gains", "gain", "has", "have")),
            anyOf(QUOTED_ABILITY, KeywordParsers.KEYWORD_LIST),
            Effect.GainAbility::new);

    /// Inlined form of the "Until end of turn," prefix used by
    /// [#GAIN_ABILITY] — defined here because the canonical
    /// `UNTIL_END_OF_TURN_PREFIX` constant is declared further
    /// down and a forward reference would fail at static-init time.
    private static final Parser<Duration> UNTIL_END_OF_TURN_PREFIX_INLINE =
            ciWords("until end of turn").followedBy(string(",")).thenReturn(Duration.untilEndOfTurn());

    static final Parser<Effect.GainAbility> GAIN_ABILITY = anyOf(
                    sequence(DURING_YOUR_TURN, GAIN_ABILITY_CORE, (d, g) -> g.withDuration(d)),
                    // "During turns other than yours, [subject] have
                    // [ability]." — Oak Street Innkeeper.
                    sequence(DURING_OTHERS_TURN, GAIN_ABILITY_CORE, (d, g) -> g.withDuration(d)),
                    sequence(AS_LONG_AS_PREFIX, GAIN_ABILITY_CORE, (d, g) -> g.withDuration(d)),
                    // "Until end of turn, [subject] gain[s] [ability]." —
                    // Shoving Match: "Until end of turn, all creatures gain
                    // '{T}: Tap target creature.'"
                    sequence(UNTIL_END_OF_TURN_PREFIX_INLINE, GAIN_ABILITY_CORE, (d, g) -> g.withDuration(d)),
                    GAIN_ABILITY_CORE)
            .optionallyFollowedBy(DURATION, Effect.GainAbility::withDuration);

    /// "[subject] <verb-body>" — a subject-less object-verb body, rebound
    /// to the subject captured by [#SUBJECT_AND_VERB_CHAIN]. Mirrors
    /// [#PLAYER_VERB_BODY] but for object-targeting verbs (gains /
    /// has / loses abilities, can't attack/block, must-attack each
    /// combat).
    private static Parser<Effect> objectVerbBody(Subject subj) {
        return Parser.<Effect>anyOf(
                anyCiWord("gets", "get").then(PtModifierParsers.PT_MODIFIER).map(mod -> new Effect.ModifyPT(subj, mod)),
                anyCiWord("is", "are", "becomes", "become")
                        .then(word("every"))
                        .then(anyOf(
                                words("creature type"),
                                words("land type"),
                                words("enchantment type"),
                                words("artifact type"),
                                words("planeswalker type")))
                        .map(kind -> new Effect.SetCharacteristic(subj, "every " + kind)),
                // "is/are/becomes/become <color>" — color-set (Sinister
                // Strength: "Enchanted creature gets +3/+1 and is
                // black."; Disciple of Kangee: "Target creature gains
                // flying and becomes blue until end of turn.").
                anyCiWord("is", "are", "becomes", "become")
                        .then(SelectorParsers.COLOR)
                        .map(c -> new Effect.SetColors(subj, new Effect.SetColors.Colors.Fixed(List.of(c)))),
                anyCiWord("has", "have", "gains", "gain")
                        .then(KeywordParsers.KEYWORD_LIST)
                        .map(abils -> new Effect.GainAbility(subj, abils)),
                anyCiWord("loses", "lose")
                        .then(KeywordParsers.KEYWORD_LIST)
                        .map(abils -> new Effect.LoseAbility(subj, new Effect.LoseAbility.Lost.Specific(abils))),
                // "lose all abilities" tail inside a chain (Humility:
                // "All creatures lose all abilities and have base
                // power and toughness 1/1.").
                anyCiWord("loses", "lose")
                        .then(phrase("all abilities"))
                        .thenReturn(new Effect.LoseAbility(subj, Effect.LoseAbility.Lost.All.ALL)),
                // "have/has base \[power|toughness|power and toughness\]" tail
                // (Humility). Mirrors SET_BASE_PT without the subject
                // prefix since the chain has already captured it.
                anyCiWord("have", "has").then(BASE_PT).map(base -> new Effect.SetBasePT(subj, base)),
                // "attacks <who> <duration>? if able" — directed
                // attack forcing (Alluring Siren: "Target creature an
                // opponent controls attacks you this turn if able.").
                phrase("attack(s)")
                        .then(SubjectParsers.ATOMIC_SUBJECT)
                        .followedBy(phrase("this turn if able"))
                        .map(who -> new Effect.AttackRestriction(
                                subj, new Effect.AttackRestriction.Capability.Must(who), Duration.untilEndOfTurn())),
                // "attacks this turn if able" — bare duration form,
                // no attack target (Incite: "… becomes red until end
                // of turn and attacks this turn if able.").
                phrase("attack(s) this turn if able")
                        .thenReturn(new Effect.AttackRestriction(
                                subj, new Effect.AttackRestriction.Capability.Must(), Duration.untilEndOfTurn())),
                phrase("attack(s) each combat")
                        .optionallyFollowedBy(phrase("if able"), (_, _) -> "")
                        .thenReturn(new Effect.AttackRestriction(subj, new Effect.AttackRestriction.Capability.Must())),
                words("can't attack")
                        .thenReturn(new Effect.AttackRestriction(subj, Effect.AttackRestriction.Capability.Cant.CANT)),
                // CANT_BE_BLOCKED must precede CANT_BLOCK since both start
                // with "can't b".
                words("can't be blocked").thenReturn(new Effect.CantBeBlocked(subj)),
                words("can't block").thenReturn(new Effect.CantBlock(subj, ALL_CREATURES)),
                // "can only attack alone" — Errantry: "Enchanted creature
                // gets +3/+0 and can only attack alone."
                words("can only attack alone")
                        .thenReturn(new Effect.AttackRestriction(
                                subj, Effect.AttackRestriction.Capability.OnlyAlone.ONLY_ALONE)));
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
    private static final Parser<List<Effect>> SUBJECT_AND_VERB_CHAIN_CORE =
            SubjectParsers.SUBJECT.flatMap(subj -> sequence(
                    objectVerbBodyWithDuration(subj).followedBy(word("and")),
                    objectVerbBodyWithDuration(subj),
                    (a, b) -> List.of(a, b)));

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
    private static final Parser<Effect.ModifyPT> MODIFY_PT_CORE = sequence(
                    SubjectParsers.SUBJECT.followedBy(DamageEffectParsers.each(anyCiWord("gets", "get"))),
                    PtModifierParsers.PT_MODIFIER,
                    Effect.ModifyPT::new)
            .optionallyFollowedBy(CountOfParsers.FOR_EACH, Effect.ModifyPT::withScaleBy)
            .optionallyFollowedBy(CountOfParsers.WHERE_X_IS, Effect.ModifyPT::withXDefinition);

    static final Parser<Effect.ModifyPT> MODIFY_PT = anyOf(
                    sequence(DURING_YOUR_TURN, MODIFY_PT_CORE, (d, m) -> m.withDuration(d)),
                    sequence(DURING_OTHERS_TURN, MODIFY_PT_CORE, (d, m) -> m.withDuration(d)),
                    sequence(AS_LONG_AS_PREFIX, MODIFY_PT_CORE, (d, m) -> m.withDuration(d)),
                    MODIFY_PT_CORE)
            .optionallyFollowedBy(DURATION, Effect.ModifyPT::withDuration)
            // Allow "for each X" and "where X is …" clauses after a
            // trailing duration (Might of the Nephilim / Mutilate:
            // "gets +N/+M until end of turn for each …"; Rush of Blood:
            // "gets +X/+0 until end of turn, where X is its power.").
            .optionallyFollowedBy(CountOfParsers.FOR_EACH, Effect.ModifyPT::withScaleBy)
            .optionallyFollowedBy(CountOfParsers.WHERE_X_IS, Effect.ModifyPT::withXDefinition);

    // Control

    /// "[player] gain[s] control of [target] [duration]." — active-voice
    /// transfer (e.g., Mind Control).
    private static final Parser<Effect.GainControl> GAIN_CONTROL_ACTIVE = sequence(
            SubjectParsers.PLAYER_SUBJECT,
            phrase("gain(s)").then(words("control of")).then(SubjectParsers.SUBJECT),
            Effect.GainControl::new);

    /// "Gain control of [target]." — implicit-you variant (e.g., Entrancing
    /// Melody: "Gain control of target creature with mana value X.").
    private static final Parser<Effect.GainControl> GAIN_CONTROL_YOU = w("gain")
            .then(words("control of"))
            .then(SubjectParsers.SUBJECT)
            .map(target -> new Effect.GainControl(YOU, target));

    /// "[player] control[s] [target]." — static-ability control grant (e.g.,
    /// Conquer: "You control enchanted land.").
    private static final Parser<Effect.GainControl> GAIN_CONTROL_STATIC = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("control(s)")),
            SubjectParsers.SUBJECT,
            Effect.GainControl::new);

    static final Parser<Effect.GainControl> GAIN_CONTROL = anyOf(
                    GAIN_CONTROL_ACTIVE, GAIN_CONTROL_YOU, GAIN_CONTROL_STATIC)
            .optionallyFollowedBy(DURATION, Effect.GainControl::withDuration);

    /// "Exchange control of [subject]." — e.g., Switcheroo, Avarice
    /// Totem.
    static final Parser<Effect.ExchangeControl> EXCHANGE_CONTROL =
            ciWords("exchange control of").then(SubjectParsers.SUBJECT).map(Effect.ExchangeControl::new);

    // Tokens

    static final Parser<Effect.CreateToken> CREATE_TOKEN = anyOf(
                    sequence(
                            w("create").then(SelectorParsers.AMOUNT).followedBy(word("tapped")),
                            TokenDescriptionParsers.TOKEN_DESCRIPTION,
                            (amt, td) -> new Effect.CreateToken(amt, td, true)),
                    sequence(
                            w("create").then(SelectorParsers.AMOUNT),
                            TokenDescriptionParsers.TOKEN_DESCRIPTION,
                            Effect.CreateToken::new))
            // Optional scaling "for each X" tail (Howl of the Night Pack:
            // "Create a 2/2 green Wolf creature token for each Forest you
            // control."). Replaces the base count with the count-of.
            .optionallyFollowedBy(
                    CountOfParsers.FOR_EACH, (ct, each) -> new Effect.CreateToken(each, ct.token(), ct.tapped()));

    // Mana

    /// One fixed-mana option — a contiguous run of mana symbols (e.g. `{G}`,
    /// `{G}{G}`, `{2}{U}`). Wraps the symbols in a [ManaOption.Fixed].
    private static final Parser<ManaOption> FIXED_MANA_OPTION =
            MANA_SYMBOL.atLeastOnce().map(ManaOption.Fixed::new);

    private static final List<ManaSymbol> BASIC_COLORS = List.of(
            new ManaSymbol("{W}"),
            new ManaSymbol("{U}"),
            new ManaSymbol("{B}"),
            new ManaSymbol("{R}"),
            new ManaSymbol("{G}"));

    /// Expands "`<amount>` mana of any one color" into one
    /// [ManaOption.Repeated] per basic color.
    private static List<Zone.Named> addZone(List<Zone.Named> list, Zone.Named more) {
        var all = new ArrayList<Zone.Named>(list);
        all.add(more);
        return List.copyOf(all);
    }

    private static List<ManaOption> anyOneColor(Amount count) {
        return BASIC_COLORS.stream()
                .<ManaOption>map(c -> new ManaOption.Repeated(count, c))
                .toList();
    }

    private static final Parser<List<ManaOption>> MANA_OPTIONS = anyOf(
            // "one mana of any color in your commander's color identity" —
            // Command Tower / Arcane Signet. Commander color identity is
            // flavor in the current model; the option is still "any of the
            // five basic colors".
            ciWords("one mana of any color in your commander's color identity")
                    .thenReturn(anyOneColor(Amount.exact(1))),
            // "one mana of any [color|type] that a land you control could
            // produce" — Reflecting Pool / Naga Vitalist / Harvester
            // Druid. The land-produce predicate is flavor; the option is
            // still any of the five basic colors.
            ciWords("one mana of any")
                    .then(anyWord("color", "type"))
                    .followedBy(words("that a land you control could produce"))
                    .thenReturn(anyOneColor(Amount.exact(1))),
            // "one mana of any color" — unambiguous shorthand for one of any basic color.
            ciWords("one mana of any color").thenReturn(anyOneColor(Amount.exact(1))),
            // "\[amount\] mana of that color" — back-reference to a
            // color named earlier in the same resolution (Meteor
            // Crater: "Choose a color of a permanent you control.
            // Add one mana of that color."). The binding source
            // typically is a preceding [Effect.ChooseColor] but
            // isn't guaranteed, so the option only captures the
            // back-reference.
            SelectorParsers.AMOUNT
                    .followedBy(words("mana of that color"))
                    .<List<ManaOption>>map(amt -> List.of(new ManaOption.OfThatColor(amt))),
            // "<amount> mana of any one color" — amount may be a word number,
            // an integer, or variable X.
            SelectorParsers.AMOUNT.followedBy(words("mana of any one color")).map(EffectParsers::anyOneColor),
            // "<amount> mana of different colors" — N distinct colors,
            // player's choice (Firemind Vessel). Modelled the same as "of
            // any one color" for now since we don't yet enforce the
            // distinctness constraint.
            SelectorParsers.AMOUNT.followedBy(words("mana of different colors")).map(EffectParsers::anyOneColor),
            // "<amount> mana in any combination of colors" — each of N
            // mana is chosen independently from the five basic colors
            // (Manamorphose).
            SelectorParsers.AMOUNT
                    .followedBy(words("mana in any combination of colors"))
                    .<List<ManaOption>>map(amt -> List.of(new ManaOption.Combination(amt, BASIC_COLORS))),
            // "<amount> mana in any combination of <symbol> and/or <symbol>..."
            // — restricted-palette combination (Orcish Lumberjack: "three
            // mana in any combination of {R} and/or {G}"). Each of N
            // mana may be any symbol in the palette independently.
            sequence(
                    SelectorParsers.AMOUNT.followedBy(words("mana in any combination of")),
                    MANA_SYMBOL.atLeastOnceDelimitedBy(anyWord("and/or", "and", "or"), Collectors.toUnmodifiableList()),
                    (amt, palette) -> List.<ManaOption>of(new ManaOption.Combination(amt, palette))),
            // "<symbol> for each X" — one Repeated option of count(X) copies of symbol.
            sequence(
                    MANA_SYMBOL,
                    CountOfParsers.FOR_EACH,
                    (sym, count) -> List.<ManaOption>of(new ManaOption.Repeated(count, sym))),
            // "<amount> <symbol>" — amount-scaled repeats of one symbol
            // (e.g., Mana Seism: "add that much {C}").
            sequence(
                    SelectorParsers.AMOUNT,
                    MANA_SYMBOL,
                    (amt, sym) -> List.<ManaOption>of(new ManaOption.Repeated(amt, sym))),
            // "an amount of <symbol> equal to <property>" — Viridian Joiner:
            // "Add an amount of {G} equal to this creature's power.".
            sequence(
                    words("an amount of").then(MANA_SYMBOL),
                    words("equal to").then(CountOfParsers.PROPERTY_OF_AMOUNT),
                    (sym, amt) -> List.<ManaOption>of(new ManaOption.Repeated(amt, sym))),
            // Fallback: an or-list of fixed groups ({G}, {G}{G}, or {1}{R}, …).
            MtgParsers.orList(FIXED_MANA_OPTION));

    static final Parser<Effect.AddMana> ADD_MANA = anyOf(
                    // "[player] adds …" — player-actor form (Tangleroot:
                    // "that player adds {G}.").
                    sequence(
                            SubjectParsers.PLAYER_SUBJECTS
                                    .followedBy(phrase("add(s)"))
                                    .optionallyFollowedBy(phrase("an additional"), (s, _) -> s),
                            MANA_OPTIONS,
                            (actor, opts) -> new Effect.AddMana(opts).withPlayer(actor)),
                    w("add").optionallyFollowedBy(phrase("an additional"), (s, _) -> s)
                            .then(MANA_OPTIONS)
                            .map(Effect.AddMana::new))
            // Optional trailing "where X is …" — binds the X in a
            // variable-mana expression (Mona Lisa: "Add X mana of any
            // one color, where X is Mona Lisa's power."). Consumed as
            // flavor for now since {@link Effect.AddMana} has no X slot.
            .optionallyFollowedBy(CountOfParsers.WHERE_X_IS, (am, _) -> am)
            // Trailing "\[they|you\] choose" — flavor restating the
            // chooser (Spectral Searchlight: "adds one mana of any
            // color they choose"; common on older cards). Consumed as
            // flavor — the chooser is already implied by the player
            // subject.
            .optionallyFollowedBy(anyCiWord("they", "you").followedBy(word("choose")), (am, _) -> am);

    // Transform/Copy

    static final Parser<Effect.Transform> TRANSFORM =
            w("transform").then(SubjectParsers.SUBJECT).map(Effect.Transform::new);

    static final Parser<Effect.Copy> COPY =
            w("copy").then(SubjectParsers.SUBJECT).map(Effect.Copy::new);

    // Combat

    static final Parser<Effect.Fight> FIGHT =
            sequence(SubjectParsers.SUBJECT.followedBy(phrase("fight(s)")), SubjectParsers.SUBJECT, Effect.Fight::new);

    // Win/Loss

    private static final Parser<String> WIN_GAME_NO_PLAYER =
            anyCiWord("wins", "win").then(words("the game"));

    static final Parser<Effect.WinGame> WIN_GAME = anyOf(
            SubjectParsers.PLAYER_SUBJECT.followedBy(WIN_GAME_NO_PLAYER).map(Effect.WinGame::new),
            WIN_GAME_NO_PLAYER.thenReturn(new Effect.WinGame(YOU)));

    private static final Parser<String> LOSE_GAME_NO_PLAYER =
            anyCiWord("loses", "lose").then(words("the game"));

    static final Parser<Effect.LoseGame> LOSE_GAME = anyOf(
            SubjectParsers.PLAYER_SUBJECT.followedBy(LOSE_GAME_NO_PLAYER).map(Effect.LoseGame::new),
            LOSE_GAME_NO_PLAYER.thenReturn(new Effect.LoseGame(YOU)));

    /// "[player] can't win the game." / "[player] can't lose the game."
    /// — Platinum Angel. Must precede [#WIN_GAME] / [#LOSE_GAME]
    /// so the "can't" prefix wins before the bare verb does.
    static final Parser<Effect.CantWinGame> CANT_WIN_GAME = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(words("can't")),
            words("win the game"),
            (player, _) -> new Effect.CantWinGame(player));

    static final Parser<Effect.CantLoseGame> CANT_LOSE_GAME = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(words("can't")),
            words("lose the game"),
            (player, _) -> new Effect.CantLoseGame(player));

    // Zone movement

    /// "\[player\]? put\[s\] \[subject\] \[from \[zone\]\]? \[destination\]." —
    /// covers both the bare "Put \[subject\] \[destination\]" form and the
    /// sourced / player-actor variants (Reclaim: "Put target card from
    /// your graveyard on top of your library."; Exhume: "Each player
    /// puts a creature card from their graveyard onto the
    /// battlefield."). The player actor is consumed as flavor; only
    /// the subject + source + destination are preserved.
    static final Parser<Effect.ZoneMove> ZONE_MOVE = anyOf(
            // "[player] puts [subject] from [zone] [destination]" —
            // player-actor + from-zone (Exhume).
            sequence(
                    SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("put(s)")).then(SubjectParsers.SUBJECT),
                    ZoneExpressionParsers.IN_ZONE_FROM.<Zone.Source>map(Zone.Source::fromZone),
                    ZoneParsers.ZONE_DESTINATION,
                    Effect.ZoneMove::new),
            sequence(
                    w("put").then(SubjectParsers.SUBJECT),
                    ZoneExpressionParsers.IN_ZONE_FROM.<Zone.Source>map(Zone.Source::fromZone),
                    ZoneParsers.ZONE_DESTINATION,
                    Effect.ZoneMove::new),
            sequence(
                    w("put").then(SubjectParsers.SUBJECT),
                    ZoneParsers.ZONE_DESTINATION,
                    (subject, dest) -> new Effect.ZoneMove(subject, null, dest)));

    // Prevent

    /// Optional "During your turn, " prefix applied to a [Effect.Prevent]
    /// — Personal Sanctuary: "During your turn, prevent all damage that
    /// would be dealt to you.". Encoded inside the prevention description
    /// since [Effect.Prevent] has no structured duration field.
    private static Effect.Prevent withDuringYourTurn(Effect.Prevent p) {
        return new Effect.Prevent("during your turn: " + p.description());
    }

    static final Parser<Effect.Prevent> PREVENT_BODY = anyOf(
                    // "prevent the next N damage [that would be dealt
                    // to [subject]]?" — Shield of the Ages: "Prevent the
                    // next 1 damage that would be dealt to you this
                    // turn."
                    sequence(
                            w("prevent")
                                    .then(words("the next")
                                            .then(SelectorParsers.AMOUNT)
                                            .followedBy(word("damage"))),
                            words("that would be dealt to").then(SubjectParsers.SUBJECT),
                            (amount, subj) ->
                                    new Effect.Prevent("prevent the next " + amount + " damage dealt to " + subj)),
                    // "prevent the next N damage"
                    w("prevent")
                            .then(words("the next").then(SelectorParsers.AMOUNT).followedBy(word("damage")))
                            .map(amount -> new Effect.Prevent("prevent the next " + amount + " damage")),
                    // "prevent N of that damage" — shielding subset
                    // (Urza's Armor: "If a source would deal damage to you,
                    // prevent 1 of that damage.").
                    w("prevent")
                            .then(SelectorParsers.AMOUNT)
                            .followedBy(words("of that damage"))
                            .map(amount -> new Effect.Prevent("prevent " + amount + " of that damage")),
                    // "prevent all combat damage that would be dealt this turn" (Fog,
                    // Darkness, Holy Day). Must precede the generic "prevent all
                    // damage" so "combat" isn't left unconsumed.
                    ciWords("prevent all combat damage that would be dealt this turn")
                            .thenReturn(new Effect.Prevent("prevent all combat damage this turn")),
                    // "prevent all damage that would be dealt to [tgt] by
                    // [src]" — Champion Lancer. Must precede the
                    // no-by arms so the trailing "by …" wins.
                    sequence(
                            ciWords("prevent all damage")
                                    .then(words("that would be dealt to"))
                                    .then(SubjectParsers.SUBJECT.followedBy(word("by"))),
                            SubjectParsers.SUBJECT,
                            (tgt, src) -> new Effect.Prevent("prevent all damage dealt to " + tgt + " by " + src)),
                    // "prevent all \[noncombat\]? damage that would be dealt
                    // \[this turn\]? to [subject]" — Divine Light (plain
                    // "damage this turn to …"); Mark of Asylum
                    // ("noncombat damage … to …"). The optional "this
                    // turn" precedes the target here, distinct from the
                    // "… to X this turn" order captured below.
                    sequence(
                            ciWords("prevent all")
                                    .then(anyOf(
                                            words("noncombat damage").thenReturn("noncombat damage"),
                                            word("damage").thenReturn("damage")))
                                    .followedBy(words("that would be dealt")),
                            anyOf(
                                    words("this turn to").thenReturn("this turn "),
                                    word("to").thenReturn("")),
                            SubjectParsers.SUBJECT,
                            (kind, turn, subject) ->
                                    new Effect.Prevent("prevent all " + kind + " " + turn + "dealt to " + subject)),
                    // "prevent all damage that would be dealt to [subject]" (Bubble
                    // Matrix, Cho-Manno; Forfend).
                    sequence(
                            ciWords("prevent all damage").followedBy(words("that would be dealt to")),
                            SubjectParsers.SUBJECT,
                            (_, subject) -> new Effect.Prevent("prevent all damage dealt to " + subject)),
                    // "prevent all damage that would be dealt by [subject]"
                    // (Ethereal Haze).
                    sequence(
                            ciWords("prevent all damage").followedBy(words("that would be dealt by")),
                            SubjectParsers.SUBJECT,
                            (_, subject) -> new Effect.Prevent("prevent all damage dealt by " + subject)),
                    // "prevent all damage that would be dealt this turn by
                    // [subject]" — Repel the Abominable.
                    sequence(
                            ciWords("prevent all damage").followedBy(words("that would be dealt this turn by")),
                            SubjectParsers.SUBJECT,
                            (_, subject) -> new Effect.Prevent("prevent all damage dealt this turn by " + subject)),
                    // "prevent all damage that [source] would deal to
                    // [target]" (Indentured Oaf, Goblin Furrier, Chameleon
                    // Blur). Captures both the source and target subjects
                    // verbatim in the free-text description.
                    sequence(
                            ciWords("prevent all damage that")
                                    .then(SubjectParsers.SUBJECT.followedBy(words("would deal to"))),
                            SubjectParsers.SUBJECT,
                            (src, tgt) -> new Effect.Prevent("prevent all damage dealt by " + src + " to " + tgt)),
                    // "prevent all combat damage that would be dealt to [subject]"
                    // (Everdawn Champion).
                    sequence(
                            ciWords("prevent all combat damage").followedBy(words("that would be dealt to")),
                            SubjectParsers.SUBJECT,
                            (_, subject) -> new Effect.Prevent("prevent all combat damage dealt to " + subject)),
                    // "prevent all combat damage that would be dealt by [subject]".
                    sequence(
                            ciWords("prevent all combat damage").followedBy(words("that would be dealt by")),
                            SubjectParsers.SUBJECT,
                            (_, subject) -> new Effect.Prevent("prevent all combat damage dealt by " + subject)),
                    // "prevent all combat damage [subject] would deal" —
                    // source-specific combat prevention without the "that
                    // … be dealt" passive wording (Serene Sunset: "Prevent
                    // all combat damage X target creatures would deal this
                    // turn.").
                    ciWords("prevent all combat damage")
                            .then(SubjectParsers.SUBJECT.followedBy(words("would deal")))
                            .map(src -> new Effect.Prevent("prevent all combat damage dealt by " + src)),
                    // "prevent all damage a source of your choice would deal
                    // [this turn]" — Pay No Heed. The source is captured
                    // verbatim so the grammar doesn't require a structured
                    // "source of X" subject yet.
                    sequence(
                            ciWords("prevent all damage"),
                            SubjectParsers.SUBJECT.followedBy(words("would deal")),
                            (_, subject) -> new Effect.Prevent("prevent all damage dealt by " + subject)),
                    // "prevent all damage" (no qualifier)
                    ciWords("prevent all damage").thenReturn(new Effect.Prevent("prevent all damage")))
            // Optional trailing duration ("this turn") — Forfend.
            .optionallyFollowedBy(
                    DURATION,
                    (p, d) -> new Effect.Prevent(
                            p.description() + " [" + d.getClass().getSimpleName() + "]"));

    /// Optional "During your turn, " prefix on a prevention effect
    /// (Personal Sanctuary: "During your turn, prevent all damage that
    /// would be dealt to you.").
    static final Parser<Effect.Prevent> PREVENT =
            anyOf(sequence(DURING_YOUR_TURN, PREVENT_BODY, (d, p) -> withDuringYourTurn(p)), PREVENT_BODY);

    /// "Damage that would be dealt [by|to] [subject] can't be prevented."
    /// — Excruciator ("by") / shielding rules ("to").
    static final Parser<Effect.DamageCantBePrevented> DAMAGE_CANT_BE_PREVENTED = sequence(
            ciWords("damage that would be dealt")
                    .then(anyOf(word("by").thenReturn(true), word("to").thenReturn(false))),
            SubjectParsers.SUBJECT.followedBy(words("can't be prevented")),
            (dealtBy, subj) -> new Effect.DamageCantBePrevented(subj, dealtBy));

    // Combat restrictions

    /// Universal "all creatures" subject used when a block restriction
    /// omits an explicit target ("can't block" == "can't block any
    /// creature").
    private static final Subject ALL_CREATURES = Subject.select(new Selector(
            Selector.Quantifier.all(), Selector.TypeExpression.single(Selector.SingleType.ofCard(CardType.CREATURE))));

    /// "[subject] can't block [what] [duration]." — what defaults to
    /// [duration defaults to null. `what][#ALL_CREATURES`,] is
    /// a full [Subject] so "this creature" / "that creature" /
    /// self-references parse alongside selectors.
    static final Parser<Effect.CantBlock> CANT_BLOCK = SubjectParsers.SUBJECT
            .followedBy(words("can't block"))
            .map(s -> new Effect.CantBlock(s, ALL_CREATURES))
            .optionallyFollowedBy(SubjectParsers.SUBJECT, Effect.CantBlock::withWhat)
            .optionallyFollowedBy(DURATION, Effect.CantBlock::withDuration);

    static final Parser<Effect.AttackRestriction> CANT_ATTACK = SubjectParsers.SUBJECT
            .followedBy(words("can't attack"))
            .map(s -> new Effect.AttackRestriction(s, Effect.AttackRestriction.Capability.Cant.CANT))
            .optionallyFollowedBy(DURATION, Effect.AttackRestriction::withDuration);

    /// "[player] take[s] the initiative." — Aarakocra Sneak.
    static final Parser<Effect.TakeInitiative> TAKE_INITIATIVE = SubjectParsers.PLAYER_SUBJECT
            .followedBy(phrase("take(s) the initiative"))
            .map(Effect.TakeInitiative::new);

    /// "[subject] don't untap [during <scope>]?." — Choke. The optional
    /// scope (e.g., "during their controllers' untap steps") is captured
    /// as free text as a compact placeholder for the structured timing.
    private static final Parser<String> DONT_UNTAP_SCOPE =
            w("during").then(CONDITION_TOKEN.atLeastOnce().map(words -> String.join(" ", words)));

    static final Parser<Effect.DontUntap> DONT_UNTAP = SubjectParsers.SUBJECT
            .followedBy(phrase("[doesn't|don't] untap"))
            .map(Effect.DontUntap::new)
            .optionallyFollowedBy(DONT_UNTAP_SCOPE, Effect.DontUntap::withScope);

    /// "[player] can't untap more than [amount] [selector] [during scope]?."
    /// — Mungha Wurm.
    static final Parser<Effect.UntapLimit> UNTAP_LIMIT = sequence(
                    SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("can't untap more than")),
                    SelectorParsers.AMOUNT,
                    SelectorParsers.SELECTOR,
                    Effect.UntapLimit::new)
            .optionallyFollowedBy(DONT_UNTAP_SCOPE, Effect.UntapLimit::withScope);

    /// "You can cast only [amount] more spell[s] [duration]?." —
    /// Irencrag Feat.
    static final Parser<Effect.CastCountLimit> CAST_COUNT_LIMIT = sequence(
                    SubjectParsers.PLAYER_SUBJECT.followedBy(words("can cast only")),
                    SelectorParsers.AMOUNT.followedBy(word("more")).followedBy(phrase("spell(s)")),
                    Effect.CastCountLimit::new)
            .optionallyFollowedBy(DURATION, (cl, d) -> new Effect.CastCountLimit(cl.player(), cl.max(), d));

    /// "[player] can't play lands [duration]?." — Turf Wound.
    static final Parser<Effect.CantPlayLands> CANT_PLAY_LANDS = SubjectParsers.SUBJECT
            .followedBy(words("can't play lands"))
            .map(Effect.CantPlayLands::new)
            .optionallyFollowedBy(DURATION, Effect.CantPlayLands::withDuration);

    /// "[chooser] choose[s] how [voter] vote[s] [duration]?." —
    /// Illusion of Choice.
    static final Parser<Effect.ChoosePlayerVote> CHOOSE_PLAYER_VOTE = sequence(
                    SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("choose(s) how")),
                    SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("vote(s)")),
                    Effect.ChoosePlayerVote::new)
            .optionallyFollowedBy(DURATION, Effect.ChoosePlayerVote::withDuration);

    /// "base \[power|toughness|power and toughness\] \[value\]" — the
    /// value payload for a [Effect.SetBasePT] (rule 613.4, layer
    /// 7b). The word *base* is required; it's the signal that this
    /// sets rather than modifies. Asymmetric shapes leave the
    /// unspecified side null in the resulting [PtValue].
    private static final Parser<PtValue> BASE_PT = anyOf(
            words("base power and toughness").then(SelectorParsers.PT_VALUE),
            words("base power").then(SelectorParsers.AMOUNT).map(p -> new PtValue(p, null)),
            words("base toughness").then(SelectorParsers.AMOUNT).map(t -> new PtValue(null, t)));

    static final Parser<Effect.SetBasePT> SET_BASE_PT = sequence(
                    SubjectParsers.SUBJECT.followedBy(phrase("[have|has]")), BASE_PT, Effect.SetBasePT::new)
            .optionallyFollowedBy(DURATION, Effect.SetBasePT::withDuration);

    /// "Exchange [possessive] [zone] and [zone]." — Harness Infinity.
    static final Parser<Effect.ExchangeZones> EXCHANGE_ZONES = sequence(
            w("exchange")
                    .then(anyOf(
                            anyWord("your", "their", "its")
                                    .map(s -> s.equalsIgnoreCase("your")
                                            ? Subject.player(Subject.PlayerRef.YOU)
                                            : Subject.player(Subject.PlayerRef.THEY)),
                            word("your").thenReturn(Subject.player(Subject.PlayerRef.YOU)))),
            SelectorParsers.ZONE_NAME.followedBy(word("and")),
            SelectorParsers.ZONE_NAME,
            (player, a, b) -> new Effect.ExchangeZones(player, new Zone.Named(a), new Zone.Named(b)));

    /// "Exchange [player]'s life total with [subject]'s [property]." —
    /// Evra, Halcyon Witness.
    static final Parser<Effect.ExchangeLifeWithProperty> EXCHANGE_LIFE_WITH_PROPERTY = sequence(
            ciWords("exchange").then(anyWord("your", "their")).followedBy(words("life total")),
            word("with").then(SubjectParsers.SUBJECT).followedBy(string("'s")),
            anyWord("power", "toughness", "strength"),
            (poss, source, prop) ->
                    new Effect.ExchangeLifeWithProperty(Subject.player(Subject.PlayerRef.YOU), source, prop));

    /// "Players don't lose unspent mana as steps and phases end." —
    /// Upwelling. Captures the full phrase shape; the unique effect
    /// doesn't parameterize further.
    static final Parser<Effect.ManaPoolPersists> MANA_POOL_PERSISTS = SubjectParsers.PLAYER_SUBJECT
            .followedBy(words("don't lose unspent mana as steps and phases end"))
            .map(Effect.ManaPoolPersists::new);

    /// "\[player\] loses all unspent mana." — empties the player's
    /// mana pool (Mana Short).
    static final Parser<Effect.LoseUnspentMana> LOSE_UNSPENT_MANA = SubjectParsers.PLAYER_SUBJECT
            .followedBy(words("loses all unspent mana"))
            .map(Effect.LoseUnspentMana::new);

    /// "Spend only mana produced by [selector] to cast this spell." —
    /// Myr Superion.
    static final Parser<Effect.ManaSpendRestriction> MANA_SPEND_RESTRICTION = ciWords("spend only mana produced by")
            .then(SelectorParsers.SELECTOR)
            .followedBy(words("to cast this spell"))
            .map(Effect.ManaSpendRestriction::new);

    /// "As an additional cost to cast this spell, [cost]." — Mardu
    /// Outrider. The leading "As an additional cost to cast this spell,"
    /// is consumed as flavor; only the cost expression is retained.
    static final Parser<Effect.AdditionalCost> ADDITIONAL_COST = ciWords("as an additional cost to cast this spell")
            .then(string(","))
            .then(CostParsers.COST_EXPRESSION)
            .map(Effect.AdditionalCost::new);

    /// "No more than N creatures can attack [whom] each combat." —
    /// Crawlspace. The trailing "each combat" is consumed as flavor since
    /// the effect is inherently per-combat.
    static final Parser<Effect.AttackLimit> ATTACK_LIMIT = sequence(
                    ciWords("no more than").then(SelectorParsers.AMOUNT),
                    phrase("creature(s)").then(words("can attack")).then(SubjectParsers.PLAYER_SUBJECT),
                    Effect.AttackLimit::new)
            .followedBy(words("each combat"));

    /// "Until end of turn," — duration prefix used before certain
    /// temporary effects (e.g., Exponential Growth: "Until end of turn,
    /// double target creature's power X times.").
    private static final Parser<Duration> UNTIL_END_OF_TURN_PREFIX =
            ciWords("until end of turn").followedBy(string(",")).thenReturn(Duration.untilEndOfTurn());

    /// Core of a "double …" P/T phrase. Supports both orders that appear
    /// in oracle text: "double the [stat] of [subject]" (Unleash Fury) and
    /// "double [subject]'s [stat]" (Exponential Growth).
    private static final Parser<boolean[]> DOUBLE_STAT_CHOICE = anyOf(
            ciWords("power and toughness").thenReturn(new boolean[] {true, true}),
            w("power").thenReturn(new boolean[] {true, false}),
            w("toughness").thenReturn(new boolean[] {false, true}));

    private static final Parser<Effect.DoublePT> DOUBLE_PT_CORE = anyOf(
            sequence(
                    ciWords("double").then(word("the")).then(DOUBLE_STAT_CHOICE),
                    word("of").then(SubjectParsers.SUBJECT),
                    (flags, subj) -> new Effect.DoublePT(subj, flags[0], flags[1])),
            sequence(
                    ciWords("double").then(SubjectParsers.SUBJECT).followedBy(string("'s")),
                    DOUBLE_STAT_CHOICE,
                    (subj, flags) -> new Effect.DoublePT(subj, flags[0], flags[1])));

    /// "[Until end of turn,]? Double the [power|toughness|…] of [subject]
    /// [N times]? [duration]?." — Unleash Fury, Berserk, Exponential
    /// Growth.
    static final Parser<Effect.DoublePT> DOUBLE_PT = anyOf(
                    sequence(UNTIL_END_OF_TURN_PREFIX, DOUBLE_PT_CORE, (d, m) -> m.withDuration(d)), DOUBLE_PT_CORE)
            .optionallyFollowedBy(SelectorParsers.AMOUNT.followedBy(phrase("time(s)")), Effect.DoublePT::withTimes)
            .optionallyFollowedBy(DURATION, Effect.DoublePT::withDuration);

    /// "Change the target of [subject]." — Deflection. Single-target
    /// redirect; a trailing "with a single target" qualifier is consumed
    /// as flavor.
    static final Parser<Effect.ChangeTheTarget> CHANGE_THE_TARGET = ciWords("change the target of")
            .then(SubjectParsers.SUBJECT)
            .map(Effect.ChangeTheTarget::new)
            .optionallyFollowedBy(words("with a single target"), (c, _) -> c);

    /// "[subject] enter[s] as a copy of [target]." — Essence of the Wild.
    /// Replacement-style entry substitution.
    static final Parser<Effect.EnterAsCopy> ENTER_AS_COPY = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("enter(s)")).followedBy(words("as a copy of")),
            SubjectParsers.SUBJECT,
            Effect.EnterAsCopy::new);

    // Enter tapped

    static final Parser<Effect.EnterTapped> ENTER_TAPPED = SubjectParsers.SUBJECT
            .followedBy(phrase("enter(s) tapped"))
            .map(Effect.EnterTapped::new)
            .optionallyFollowedBy(DURATION, Effect.EnterTapped::withDuration);

    /// "[subject] enter[s] with [count] [type] counters on it." — e.g.,
    /// Endless One: "This creature enters with X +1/+1 counters on it."
    static final Parser<Effect.EnterWithCounters> ENTER_WITH_COUNTERS = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("enter(s) with")),
            SelectorParsers.AMOUNT,
            SelectorParsers.COUNTER_TYPE.followedBy(phrase("counter(s) on [it|them]")),
            Effect.EnterWithCounters::new);

    // Characteristic-setting: "[subject] are/is [colors|colorless|subtype]"

    /// Head of a characteristic-setting clause: the subject followed by a
    /// copula verb — "are" / "is" for static characteristics or "becomes" /
    /// "become" for target-acquires forms (e.g., Moonlace: "Target spell or
    /// permanent becomes colorless."). Shared by SET_COLORS / SET_COLORLESS /
    /// SET_SUBTYPE.
    private static final Parser<Subject> ARE_SUBJECT = anyOf(
            SubjectParsers.SUBJECT.followedBy(anyWord("are", "is", "becomes", "become")),
            // Contractions — "it's X", "they're X" (Cyber Conversion:
            // "It's a 2/2 Cyberman artifact creature.").
            caseInsensitive("it's").thenReturn(Subject.pronoun("it")),
            caseInsensitive("they're").thenReturn(Subject.pronoun("they")));

    private static final List<Color> ALL_COLORS = List.of(Color.WHITE, Color.BLUE, Color.BLACK, Color.RED, Color.GREEN);

    /// "\[colors\] / colorless / all colors / the color of \[poss\] choice"
    /// — the right-hand side of a "X are/is …" color-set clause. Each
    /// arm emits a [Effect.SetColors.Colors] variant so a single
    /// SET_COLORS parser covers every form. "colorless" is the empty
    /// Fixed list; "all colors" is the five basic colors; "the color
    /// of your choice" is OfChoice.
    private static final Parser<Effect.SetColors.Colors> SET_COLORS_BODY = anyOf(
            // "colorless \[sources of damage\]?" — Ancient Kavu;
            // "Ghostly Flame" trailing "sources of damage" is flavor.
            word("colorless")
                    .<Effect.SetColors.Colors>thenReturn(new Effect.SetColors.Colors.Fixed(List.of()))
                    .optionallyFollowedBy(words("sources of damage"), (c, _) -> c),
            words("all colors").thenReturn(new Effect.SetColors.Colors.Fixed(ALL_COLORS)),
            // "the color of \[poss\] choice" — Vodalian Mystic.
            words("the color of")
                    .then(anyWord("your", "their", "his", "her", "its"))
                    .followedBy(word("choice"))
                    .<Effect.SetColors.Colors>map(Effect.SetColors.Colors.OfChoice::new),
            // "\[color\] \[and \[color\]\]*" — explicit fixed color set.
            MtgParsers.andList(SelectorParsers.COLOR).<Effect.SetColors.Colors>map(Effect.SetColors.Colors.Fixed::new));

    private static final Parser<Effect.SetColors> SET_COLORS_CORE = sequence(
                    ARE_SUBJECT, SET_COLORS_BODY, Effect.SetColors::new)
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
                    anyWord("a", "an").then(word("colorless").optional()).then(SelectorParsers.SUBTYPE),
                    SelectorParsers.SUBTYPE)
            .optionallyFollowedBy(SelectorParsers.CARD_TYPE, (subtype, _) -> subtype);

    /// "[subject] are/is [card type] in addition to their other types." —
    /// Enchanted Evening: "All permanents are enchantments in addition to
    /// their other types." Card types are pluralized in oracle text.
    static final Parser<Effect.AddCardType> ADD_CARD_TYPE = sequence(
                    ARE_SUBJECT,
                    MtgParsers.andList(SelectorParsers.CARD_TYPE)
                            .followedBy(phrase("in addition to [its|their] other types")),
                    Effect.AddCardType::new)
            .optionallyFollowedBy(DURATION, (e, d) -> new Effect.AddCardType(e.subject(), e.types(), d));

    /// "[subject] are/is every creature/land/… type." — Runed Stalactite form.
    /// The "every X type" shape can't enumerate all subtypes at parse time, so
    /// it falls back to a free-text [Effect.SetCharacteristic].
    static final Parser<Effect.SetCharacteristic> SET_EVERY_SUBTYPE_TYPE = sequence(
                    ARE_SUBJECT,
                    word("every")
                            .then(anyOf(
                                    words("creature type"),
                                    words("land type"),
                                    words("enchantment type"),
                                    words("artifact type"),
                                    words("planeswalker type"))),
                    (subject, kind) -> new Effect.SetCharacteristic(subject, "every " + kind))
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
                            words("all basic land types").thenReturn(ALL_BASIC_LAND_TYPES),
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
                    phrase("in addition to [its|their] other")
                            .then(SelectorParsers.CARD_TYPE)
                            .then(word("types")),
                    (s, _) -> s)
            .optionallyFollowedBy(DURATION, Effect.SetSubtype::withDuration);

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
            sequence(
                    SelectorParsers.COLOR,
                    SelectorParsers.CARD_TYPE.atLeastOnce(),
                    (c, ts) -> c.name().toLowerCase() + " "
                            + ts.stream().map(t -> t.name().toLowerCase()).collect(Collectors.joining(" "))),
            sequence(
                    SelectorParsers.SUBTYPE,
                    SelectorParsers.CARD_TYPE.atLeastOnce(),
                    (st, ts) -> st.texts().getFirst() + " "
                            + ts.stream().map(t -> t.name().toLowerCase()).collect(Collectors.joining(" "))),
            SelectorParsers.CARD_TYPE
                    .atLeastOnce()
                    .map(ts -> ts.stream().map(t -> t.name().toLowerCase()).collect(Collectors.joining(" "))));

    private static final Parser<String> BECOME_PT_TYPE_CORE = anyOf(
            sequence(
                    anyCiWord("a", "an").then(SelectorParsers.PT_VALUE),
                    BECOME_PT_TYPE_TAIL,
                    (pt, rest) -> pt + " " + rest),
            sequence(SelectorParsers.PT_VALUE, BECOME_PT_TYPE_TAIL, (pt, rest) -> pt + " " + rest));

    private static final Parser<Effect.SetCharacteristic> BECOME_PT_TYPE_CORE_PARSER = sequence(
                    ARE_SUBJECT, BECOME_PT_TYPE_CORE, Effect.SetCharacteristic::new)
            .optionallyFollowedBy(DURATION, Effect.SetCharacteristic::withDuration)
            .optionallyFollowedBy(
                    anyOf(words("that are still"), words("that's still"))
                            .then(anyCiWord("a", "an").orElse(""))
                            .then(SelectorParsers.CARD_TYPE),
                    (sc, still) -> new Effect.SetCharacteristic(
                            sc.target(),
                            sc.description() + " (still " + still.name().toLowerCase() + ")",
                            sc.duration()));

    /// Accepts the "Until end of turn, …" / "During your turn, …"
    /// prefix in front of a BECOME_PT_TYPE effect (Animate Land:
    /// "Until end of turn, target land becomes a 3/3 creature that's
    /// still a land.").
    static final Parser<Effect.SetCharacteristic> BECOME_PT_TYPE = anyOf(
            sequence(UNTIL_END_OF_TURN_PREFIX_INLINE, BECOME_PT_TYPE_CORE_PARSER, (d, sc) -> sc.withDuration(d)),
            sequence(DURING_YOUR_TURN, BECOME_PT_TYPE_CORE_PARSER, (d, sc) -> sc.withDuration(d)),
            BECOME_PT_TYPE_CORE_PARSER);

    /// "They're still [type]." / "They are still [type]." — flavor
    /// follow-up on mass type-change effects (Natural Affinity: "All
    /// lands become 2/2 creatures until end of turn. They're still
    /// lands."). Modelled as a [Effect.SetCharacteristic] with the
    /// "they" pronoun as target.
    static final Parser<Effect.SetCharacteristic> STILL_TYPE = anyOf(
                    ciWords("they're still"), ciWords("they are still"))
            .then(SelectorParsers.CARD_TYPE)
            .map(t -> new Effect.SetCharacteristic(
                    Subject.pronoun("they"), "still " + t.name().toLowerCase()));

    /// "[subject] are [supertype]" — add a supertype (Rootpath Purifier).
    static final Parser<Effect.SetSupertype> SET_SUPERTYPE =
            sequence(ARE_SUBJECT, SelectorParsers.SUPERTYPE, Effect.SetSupertype::new);

    /// "[subject]'s [property] is equal to [amount]." — Sima Yi.
    /// "[subject]'s [property] becomes [amount]." — Biorhythm: "Each
    /// player's life total becomes the number of creatures they
    /// control.".
    static final Parser<Effect.SetPropertyValue> SET_PROPERTY_VALUE = sequence(
            SubjectParsers.SUBJECT.followedBy(string("'s")),
            anyOf(anyWord("power", "toughness", "strength"), words("life total"), words("hand size"))
                    .followedBy(anyOf(words("is equal to"), word("becomes"))),
            anyOf(CountOfParsers.PROPERTY_OF_AMOUNT, SelectorParsers.AMOUNT),
            Effect.SetPropertyValue::new);

    /// Token parser for the free-text tail of "Spend this mana only…":
    /// accepts contraction-like words plus mana-symbol braces so forms
    /// like "on costs that contain {X}" (Rosheen Meanderer) round-trip.
    private static final Parser<String> SPEND_MANA_TOKEN = consecutive(
            CharacterSet.charsIn("[A-Za-z0-9'-]").or(CharPredicate.is('{')).or(CharPredicate.is('}')),
            "spend-mana token");

    /// "Spend this mana only to [restriction]." — Omen Hawker.
    /// "Spend this mana only on [restriction]." — Rosheen Meanderer.
    /// The restriction is captured as free text via a token list that
    /// includes mana-symbol braces.
    static final Parser<Effect.SpendThisManaOnly> SPEND_THIS_MANA_ONLY = ciWords("spend this mana only")
            .then(anyWord("to", "on"))
            .then(SPEND_MANA_TOKEN.atLeastOnce().map(words -> String.join(" ", words)))
            .map(Effect.SpendThisManaOnly::new);

    /// "Activate only [N] time[s] each turn." — Salvaged Manaworker — or
    /// "Activate no more than [N] times each turn." — Manaforge Cinder.
    /// "once" is recognized as Amount(1) via a literal.
    static final Parser<Effect.ActivationLimit> ACTIVATION_LIMIT = w("activate")
            .then(anyOf(word("only"), words("no more than")))
            .then(anyOf(word("once").thenReturn(Amount.exact(1)), SelectorParsers.AMOUNT.followedBy(phrase("time(s)"))))
            .followedBy(words("each turn"))
            .map(Effect.ActivationLimit::new);

    /// "Activate only if [condition]." — activation-time gate (Temple of
    /// the False God: "Activate only if you control five or more lands.";
    /// Fool's Tome: "Activate only if you have no cards in hand."). The
    /// condition is captured as a free-text predicate for now.
    static final Parser<Effect.ActivateOnly.If> ACTIVATE_ONLY_IF = phrase("Activate only if")
            .then(WORD_OR_CONTRACTION.atLeastOnce().map(words -> String.join(" ", words)))
            .map(text -> new Effect.ActivateOnly.If(Condition.ifCondition(text)));

    /// "Activate only as a sorcery." — sorcery-speed restriction
    /// (Fractured Powerstone).
    static final Parser<Effect.ActivateOnly> ACTIVATE_ONLY_AS_SORCERY =
            phrase("Activate only as a sorcery").thenReturn(Effect.ActivateOnly.AsSorcery.AS_SORCERY);

    /// "Activate only during [when]." — timing-window restriction
    /// (Disrupting Scepter: "Activate only during your turn."). The
    /// trailing clause is captured verbatim.
    static final Parser<Effect.ActivateOnly.During> ACTIVATE_ONLY_DURING = phrase("Activate only during")
            .then(WORD_OR_CONTRACTION.atLeastOnce().map(words -> String.join(" ", words)))
            .map(Effect.ActivateOnly.During::new);

    /// "You may play a card you own from outside the game this turn." —
    /// Wish. The subject defaults to "you"; oracle text naming another
    /// player isn't yet supported.
    static final Parser<Effect.PlayFromOutside> PLAY_FROM_OUTSIDE = ciWords(
                    "you may play a card you own from outside the game")
            .thenReturn(new Effect.PlayFromOutside(YOU))
            .optionallyFollowedBy(DURATION, Effect.PlayFromOutside::withDuration);

    /// "Turn [subject] face up." — Break Open.
    static final Parser<Effect.TurnFaceUp> TURN_FACE_UP =
            w("turn").then(SubjectParsers.SUBJECT).followedBy(words("face up")).map(Effect.TurnFaceUp::new);

    /// "Turn [subject] face down." — Cyber Conversion.
    static final Parser<Effect.TurnFaceDown> TURN_FACE_DOWN = w("turn")
            .then(SubjectParsers.SUBJECT)
            .followedBy(words("face down"))
            .map(Effect.TurnFaceDown::new);

    /// "X are/is no longer [supertype]" — remove a supertype.
    static final Parser<Effect.LoseSupertype> LOSE_SUPERTYPE =
            sequence(ARE_SUBJECT, words("no longer").then(SelectorParsers.SUPERTYPE), Effect.LoseSupertype::new);

    // Regeneration (701.15)

    static final Parser<Effect.Regenerate> REGENERATE =
            w("regenerate").then(SubjectParsers.SUBJECT).map(Effect.Regenerate::new);

    // Action restrictions

    static final Parser<Effect.CantCycle> CANT_CYCLE =
            SubjectParsers.SUBJECT.followedBy(words("can't cycle cards")).map(Effect.CantCycle::new);

    /// "X is [amount]." — binds the ability-level X variable to an
    /// amount expression (Bargaining Table: "X is the number of cards
    /// in an opponent's hand.").
    static final Parser<Effect.DefineX> DEFINE_X = w("X").followedBy(word("is"))
            .then(anyOf(CountOfParsers.PROPERTY_OF_AMOUNT, SelectorParsers.AMOUNT))
            .map(Effect.DefineX::new);

    static final Parser<Effect.GainEnergy> GAIN_ENERGY = anyOf(
            sequence(
                    SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("get(s)")), ENERGY_SYMBOLS, Effect.GainEnergy::new),
            anyCiWord("gets", "get").then(ENERGY_SYMBOLS).map(n -> new Effect.GainEnergy(YOU, n)));

    /// "[subject] crews [selector] using [property] rather than
    /// [property]." — Giant Ox.
    static final Parser<Effect.CrewsUsing> CREWS_USING = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("crew(s)")),
            SelectorParsers.SELECTOR.followedBy(phrase("using [its|their]")),
            anyWord("power", "toughness").followedBy(phrase("rather than [its|their]")),
            anyWord("power", "toughness"),
            (subj, what, used, replaced) -> new Effect.CrewsUsing(subj, what, used, replaced));

    /// "Until [event]," — prefix duration that applies an
    /// [Duration.UntilEvent] to the effect it precedes (Spatial
    /// Binding: "Until your next upkeep, target permanent can't phase
    /// out."). The tokens between "until" and the comma are captured
    /// verbatim as the event description.
    private static final Parser<Duration> UNTIL_EVENT_PREFIX = ciWords("until")
            .then(WORD_OR_CONTRACTION.atLeastOnce().map(words -> String.join(" ", words)))
            .followedBy(string(","))
            .map(Duration::untilEvent);

    /// "[subject] can't phase out [duration]?" — Spatial Binding.
    static final Parser<Effect.CantPhaseOut> CANT_PHASE_OUT = anyOf(
            sequence(
                    UNTIL_EVENT_PREFIX,
                    SubjectParsers.SUBJECT.followedBy(words("can't phase out")),
                    (d, subj) -> new Effect.CantPhaseOut(subj, d)),
            SubjectParsers.SUBJECT
                    .followedBy(words("can't phase out"))
                    .map(Effect.CantPhaseOut::new)
                    .optionallyFollowedBy(DURATION, Effect.CantPhaseOut::withDuration));

    /// "Activated abilities of [selector] can't be activated." — e.g.,
    /// Collector Ouphe, Cursed Totem.
    static final Parser<Effect.CantActivate> CANT_ACTIVATE = phrase("Activated abilities of")
            .then(SelectorParsers.SELECTOR)
            .followedBy(words("can't be activated"))
            .map(Effect.CantActivate::new);

    /// "[subject] can't be blocked [by|except by X] [duration]." Structured
    /// as a single CantBeBlocked effect with an optional [Effect.CantBeBlocked.By]
    /// variant: [Effect.CantBeBlocked.By.Matching] for "by X"
    /// (e.g., "can't be blocked by Walls") and
    /// [Effect.CantBeBlocked.By.Except] for "except by X"
    /// (e.g., Shifting Sliver).
    private static final Parser<Effect.CantBeBlocked.By> CANT_BE_BLOCKED_BY = anyOf(
            ciWords("except by")
                    .then(SelectorParsers.SELECTOR)
                    .<Effect.CantBeBlocked.By>map(Effect.CantBeBlocked.By.Except::new),
            // "by more than N X" — an upper bound on the number of blockers
            // (Huang Zhong: "can't be blocked by more than one creature.").
            sequence(ciWords("by more than").then(SelectorParsers.AMOUNT), SelectorParsers.SELECTOR, (max, sel) ->
                    (Effect.CantBeBlocked.By) new Effect.CantBeBlocked.By.LimitOf(max, sel)),
            w("by").then(SelectorParsers.SELECTOR).<Effect.CantBeBlocked.By>map(Effect.CantBeBlocked.By.Matching::new));

    static final Parser<Effect.CantBeBlocked> CANT_BE_BLOCKED = SubjectParsers.SUBJECT
            .followedBy(words("can't be blocked"))
            .map(Effect.CantBeBlocked::new)
            // Duration can land either before the by-clause (Joven's
            // Tools: "can't be blocked this turn except by Walls.") or
            // after it. Accept both orders.
            .optionallyFollowedBy(DURATION, Effect.CantBeBlocked::withDuration)
            .optionallyFollowedBy(CANT_BE_BLOCKED_BY, Effect.CantBeBlocked::withBy)
            .optionallyFollowedBy(DURATION, Effect.CantBeBlocked::withDuration);

    static final Parser<Effect.CantBlockAlone> CANT_BLOCK_ALONE =
            SubjectParsers.SUBJECT.followedBy(words("can't block alone")).map(Effect.CantBlockAlone::new);

    static final Parser<Effect.AttackRestriction> CANT_ATTACK_ALONE = SubjectParsers.SUBJECT
            .followedBy(words("can't attack alone"))
            .map(s -> new Effect.AttackRestriction(s, Effect.AttackRestriction.Capability.CantAlone.CANT_ALONE));

    /// "[subject] can only attack alone." — Errantry.
    static final Parser<Effect.AttackRestriction> ONLY_ATTACK_ALONE = SubjectParsers.SUBJECT
            .followedBy(words("can only attack alone"))
            .map(s -> new Effect.AttackRestriction(s, Effect.AttackRestriction.Capability.OnlyAlone.ONLY_ALONE));

    /// "Populate." — Wake the Reflections. Rule 701.28.
    static final Parser<Effect.Populate> POPULATE = phrase("Populate").thenReturn(Effect.Populate.POPULATE);

    /// "[subject] can't attack [whom]" — e.g., "Creatures can't attack you."
    static final Parser<Effect.AttackRestriction> CANT_ATTACK_WHOM = sequence(
            SubjectParsers.SUBJECT.followedBy(words("can't attack")),
            SubjectParsers.PLAYER_SUBJECT,
            (s, whom) -> new Effect.AttackRestriction(s, new Effect.AttackRestriction.Capability.CantWhom(whom)));

    /// "[player] takes/take [count] extra turn(s) [after this one]." —
    /// `count` accepts the indefinite article ("an extra turn") as 1 or
    /// an explicit number (Time Stretch: "two extra turns"). Player is
    /// either named (Time Warp) or implicitly "you".
    static final Parser<Effect.TakeExtraTurn> TAKE_EXTRA_TURN = sequence(
                    anyOf(
                            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("take(s)")),
                            anyCiWord("takes", "take").thenReturn(YOU)),
                    anyOf(anyCiWord("an", "a").thenReturn(Amount.exact(1)), SelectorParsers.AMOUNT),
                    word("extra").followedBy(phrase("turn(s)")),
                    (player, count, _) -> new Effect.TakeExtraTurn(player, count))
            .optionallyFollowedBy(words("after this one"), (eff, _) -> eff);

    /// "[player] may play lands from [zone]." — e.g., Crucible of Worlds:
    /// "You may play lands from your graveyard."
    static final Parser<Effect.PlayLandsFrom> PLAY_LANDS_FROM = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(words("may play lands")),
            ZoneExpressionParsers.IN_ZONE_FROM,
            Effect.PlayLandsFrom::new);

    /// `[up to] N additional land(s)` — amount for a "play additional lands"
    /// effect. "Up to" bounds the max; a bare amount is an exact count.
    private static final Parser<Amount> ADDITIONAL_LANDS_AMOUNT = anyOf(
                    ciWords("up to").then(SelectorParsers.AMOUNT), SelectorParsers.AMOUNT)
            .followedBy(phrase("additional land(s)"));

    /// Body of a "play additional lands" clause, starting at the verb. Used
    /// both directly in [#PLAY_ADDITIONAL_LANDS] and by [#MAY]
    /// for the "may" form (Summer Bloom).
    private static final Parser<Amount> PLAY_ADDITIONAL_LANDS_NO_PLAYER =
            anyCiWord("plays", "play").then(ADDITIONAL_LANDS_AMOUNT);

    static final Parser<Effect.PlayAdditionalLands> PLAY_ADDITIONAL_LANDS = anyOf(
                    sequence(
                            SubjectParsers.PLAYER_SUBJECT,
                            PLAY_ADDITIONAL_LANDS_NO_PLAYER,
                            Effect.PlayAdditionalLands::new),
                    PLAY_ADDITIONAL_LANDS_NO_PLAYER.map(amount -> new Effect.PlayAdditionalLands(YOU, amount)))
            .optionallyFollowedBy(DURATION, Effect.PlayAdditionalLands::withDuration);

    /// "The Ring tempts [you]." — rule 716.
    static final Parser<Effect.RingTempts> RING_TEMPTS =
            ciWords("the Ring tempts").then(SubjectParsers.PLAYER_SUBJECT).map(Effect.RingTempts::new);

    /// "[player]'s hand" — a possessive reference used in "Look at target
    /// player's hand" etc. Packaged as a [Subject.PossessiveSubject]
    /// so LOOK_AT's target is always a [Subject]. Also covers the
    /// possessive form "its controller's hand" / "its owner's hand" (Lay
    /// Bare: "Counter target spell. Look at its controller's hand.").
    private static final Parser<Subject> PLAYER_HAND_SUBJECT = anyOf(
            SubjectParsers.PLAYER_REF
                    .followedBy(string("'s"))
                    .followedBy(word("hand"))
                    .map(ref -> Subject.possessiveSubject(ref.name().toLowerCase() + "'s", "hand")),
            sequence(
                    anyWord("its", "their", "your"),
                    anyWord("controller", "owner").followedBy(string("'s")).followedBy(word("hand")),
                    (pronoun, role) -> Subject.possessiveSubject(pronoun + " " + role + "'s", "hand")));

    /// "the top card of [player]'s library" — a positional card reference
    /// used as a LOOK_AT target (e.g., Rootwater Mystic). Rendered as a
    /// [Subject.PossessiveSubject] with role "top card of library".
    private static final Parser<Subject> TOP_CARD_OF_LIBRARY = ciWords("the top card of")
            .then(SubjectParsers.PLAYER_REF)
            .followedBy(string("'s"))
            .followedBy(word("library"))
            .map(ref -> Subject.possessiveSubject(ref.name().toLowerCase() + "'s", "top card of library"));

    /// "Look at [target]." — reveal-to-looker. Covers player-hand targets
    /// ("target player's hand"), top-of-library ("the top card of target
    /// player's library"), and game-object targets ("target face-down
    /// creature", Smoke Teller).
    static final Parser<Effect.LookAt> LOOK_AT = ciWords("look at")
            .then(anyOf(TOP_CARD_OF_LIBRARY, PLAYER_HAND_SUBJECT, SubjectParsers.SUBJECT))
            .map(Effect.LookAt::new)
            // Optional trailing timing flavor — "any time" / "at any time"
            // (Keeper of the Lens: "You may look at face-down creatures you
            // don't control any time."). Consumed as flavor since the
            // permission itself is the LookAt effect.
            .optionallyFollowedBy(anyOf(ciWords("at any time"), ciWords("any time")), (la, _) -> la);

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
            SubjectParsers.PLAYER_SUBJECT.followedBy(words("may cast")),
            SubjectParsers.SUBJECT,
            ZoneExpressionParsers.IN_ZONE_FROM
                    .<List<Zone.Named>>map(List::of)
                    .optionallyFollowedBy(word("or").then(ZoneExpressionParsers.IN_ZONE_FROM), EffectParsers::addZone),
            Effect.CastFromZone::new);

    /// "[player] may choose new targets for [spell]." — e.g., Redirect.
    static final Parser<Effect.ChooseNewTargets> CHOOSE_NEW_TARGETS = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(words("may choose new targets for")),
            SubjectParsers.SUBJECT,
            Effect.ChooseNewTargets::new);

    /// "\[chooser\]? choose\[s\] \[selector\] \[at random\]?." — e.g.,
    /// Duneblast ("Choose up to one creature."); Imperial Edict
    /// ("Target opponent chooses a creature they control.").
    /// The chooser-prefixed and imperative forms share the same
    /// [Effect.Choose] shape, with `chooser` set only when oracle
    /// text names one.
    static final Parser<Effect.Choose> CHOOSE = anyOf(
                    sequence(
                            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("choose(s)")),
                            SubjectParsers.SUBJECT,
                            (chooser, what) -> new Effect.Choose(what).withChooser(chooser)),
                    w("choose").then(SubjectParsers.SUBJECT).map(Effect.Choose::new))
            .optionallyFollowedBy(words("at random"), (c, _) -> new Effect.Choose(c.chooser(), c.what(), true));

    /// "Choose a color \[of \[scope\]\]?." — color-choice effect (Brave
    /// the Elements; Meteor Crater: "Choose a color of a permanent
    /// you control.").
    static final Parser<Effect.ChooseColor> CHOOSE_COLOR = w("choose")
            .then(anyCiWord("a", "an"))
            .then(word("color"))
            .thenReturn(new Effect.ChooseColor())
            .optionallyFollowedBy(word("of").then(SubjectParsers.SUBJECT), Effect.ChooseColor::withScope);

    /// "[player] chooses a card in their hand and discards the rest." —
    /// Monomania. Keeps the chosen card, discards all others in hand.
    static final Parser<Effect.DiscardAllButOne> DISCARD_ALL_BUT_ONE = SubjectParsers.PLAYER_SUBJECTS
            .followedBy(phrase("choose(s) a card in [their|his|her|its] hand and discard(s) the rest"))
            .map(Effect.DiscardAllButOne::new);

    /// "[player] become[s] the monarch." — rule 716 (e.g., Palace Sentinels).
    static final Parser<Effect.BecomeMonarch> BECOME_MONARCH = SubjectParsers.PLAYER_SUBJECT
            .followedBy(phrase("become(s) the monarch"))
            .map(Effect.BecomeMonarch::new);

    /// "[players] exchange life totals." — e.g., Soul Conduit.
    static final Parser<Effect.ExchangeLifeTotals> EXCHANGE_LIFE_TOTALS =
            SubjectParsers.SUBJECT.followedBy(words("exchange life totals")).map(Effect.ExchangeLifeTotals::new);

    /// "[player] may change any targets of [spell]." — e.g., Sideswipe.
    static final Parser<Effect.ChangeAnyTargets> CHANGE_ANY_TARGETS = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(words("may change any targets of")),
            SubjectParsers.SUBJECT,
            Effect.ChangeAnyTargets::new);

    /// Marker-counter token like `{E}` (energy) or `{TK}` (tickets). Kept as
    /// a raw `{...}` string so new markers slot in without a grammar change.
    private static final Parser<String> MARKER_TOKEN = string("{")
            .then(consecutive(CharPredicate.noneOf(" {}"), "marker content"))
            .followedBy(string("}"))
            .map(s -> "{" + s + "}");

    /// Amount paired with a marker-counter token. Accepts three oracle
    /// shapes: explicit count (`"2 {TK}"`), multi-symbol count
    /// (`"{E}{E}"` → 2× `{E}`, common on energy/ticket payouts such as
    /// Tune the Narrative), and bare single (`"{TK}"` → 1×).
    private static final Parser<Map.Entry<Amount, String>> AMOUNT_MARKER = anyOf(
            sequence(SelectorParsers.AMOUNT, MARKER_TOKEN, Map::entry),
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
            SubjectParsers.SUBJECT.followedBy(words("can't be countered")).map(Effect.CantBeCountered::new);

    /// "[subject] must be blocked [duration]? [if able]?." — combat
    /// must-block restriction.
    static final Parser<Effect.MustBeBlocked> MUST_BE_BLOCKED = SubjectParsers.SUBJECT
            .followedBy(words("must be blocked"))
            .map(Effect.MustBeBlocked::new)
            .optionallyFollowedBy(DURATION, Effect.MustBeBlocked::withDuration)
            .optionallyFollowedBy(words("if able"), (mb, _) -> mb);

    /// "[blockers] able to block [target] do so." — e.g., Taunting Elf,
    /// Elvish Bard. The blockers subject is discarded as flavor (it's
    /// always "all creatures"-like); semantically this forces `target`
    /// to be blocked by any creature that can.
    static final Parser<Effect.MustBeBlocked> ABLE_TO_BLOCK_DO_SO = SubjectParsers.SUBJECT
            .followedBy(words("able to block"))
            .then(SubjectParsers.SUBJECT)
            .map(Effect.MustBeBlocked::new)
            .optionallyFollowedBy(DURATION, Effect.MustBeBlocked::withDuration)
            .followedBy(words("do so"));

    /// "[subject] can't attack or block [duration]." — fans out into
    /// two peer restrictions sharing the subject: an AttackRestriction
    /// (can't attack) and a CantBlock (can't block). The outer
    /// [#CLAUSE] level flattens the list so each restriction lands as
    /// a peer.
    /// Trailing `unless <predicate>` condition on an effect — emits a
    /// [Condition.Kind#UNLESS] with the predicate captured as
    /// free-text tokens (including apostrophes / mana symbols).
    private static final Parser<Condition> UNLESS_PREDICATE = sequence(
            w("unless").thenReturn(Condition.Kind.UNLESS),
            CONDITION_TOKEN.atLeastOnce().map(words -> String.join(" ", words)),
            Condition::new);

    /// Trailing `if <predicate>` condition used locally by the
    /// CANT_ATTACK_OR_BLOCK parser — inlined because the top-level
    /// [#IF_CONDITION] constant is declared further down and a
    /// forward reference would fail at static init.
    private static final Parser<Condition> CANT_ATTACK_OR_BLOCK_IF = w("if").then(
                    CONDITION_TOKEN.atLeastOnce().map(words -> String.join(" ", words)))
            .map(Condition::ifCondition);

    static final Parser<List<Effect>> CANT_ATTACK_OR_BLOCK = SubjectParsers.SUBJECT
            .followedBy(words("can't attack or block"))
            .map(subj -> List.<Effect>of(
                    new Effect.AttackRestriction(subj, Effect.AttackRestriction.Capability.Cant.CANT),
                    new Effect.CantBlock(subj, ALL_CREATURES)))
            .optionallyFollowedBy(
                    DURATION,
                    (list, d) -> List.<Effect>of(
                            ((Effect.AttackRestriction) list.get(0)).withDuration(d),
                            ((Effect.CantBlock) list.get(1)).withDuration(d)))
            // Trailing "unless \[predicate\]" / "if \[predicate\]" — gates
            // both restrictions on the same condition (Qal Sisma
            // Behemoth: "… can't attack or block unless you pay
            // {2}."; Wirecat: "… can't attack or block if an
            // enchantment is on the battlefield."). Each peer gets
            // wrapped in [Effect.Conditional] so the condition rides
            // structurally, not as free-text.
            .optionallyFollowedBy(anyOf(UNLESS_PREDICATE, CANT_ATTACK_OR_BLOCK_IF), (list, cond) -> list.stream()
                    .<Effect>map(e -> new Effect.Conditional(e, cond))
                    .toList());

    /// "[subject] can't have counters put on it." — e.g., Melira's Keepers.
    static final Parser<Effect.CantHaveCounters> CANT_HAVE_COUNTERS = SubjectParsers.SUBJECT
            .followedBy(phrase("can't have counters put on [it|them]"))
            .map(Effect.CantHaveCounters::new);

    /// "[subject] can't be regenerated [duration]." — e.g., Tunnel
    /// (static) and Furnace Brood ("this turn").
    static final Parser<Effect.CantBeRegenerated> CANT_BE_REGENERATED = SubjectParsers.SUBJECT
            .followedBy(words("can't be regenerated"))
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
            ciWords("entering or dying").thenReturn(Effect.SuppressEtbTriggers.Event.ENTERING_OR_DYING),
            w("entering").thenReturn(Effect.SuppressEtbTriggers.Event.ENTERING),
            w("dying").thenReturn(Effect.SuppressEtbTriggers.Event.DYING));

    static final Parser<Effect.SuppressEtbTriggers> SUPPRESS_ETB_TRIGGERS = sequence(
                    SubjectParsers.SUBJECT,
                    ETB_SUPPRESS_EVENT.followedBy(words("don't cause abilities")),
                    Effect.SuppressEtbTriggers::new)
            .optionallyFollowedBy(word("of").then(SelectorParsers.SELECTOR), Effect.SuppressEtbTriggers::withScope)
            .followedBy(words("to trigger"));

    /// "If <trigger-clause>, that ability triggers [N] additional time[s]."
    /// — Elesh Norn, Mother of Machines. The trigger clause is captured as
    /// free word tokens up to the comma; `additional` defaults to 1
    /// for "an additional time". Because it shares the "If …," prefix with
    /// [#IF_PREFIX_CONDITION] and must win when the tail is the
    /// Panharmonicon-style ", that ability triggers …", it is dispatched in
    /// [#EFFECT] ahead of the generic if-prefix conditional.
    static final Parser<Effect.AdditionalEtbTriggers> ADDITIONAL_ETB_TRIGGERS = sequence(
            w("if").then(WORD_OR_CONTRACTION.atLeastOnce().map(words -> String.join(" ", words))),
            string(",")
                    .then(words("that ability triggers"))
                    .then(anyOf(anyWord("an", "a").thenReturn(Amount.exact(1)), SelectorParsers.AMOUNT))
                    .followedBy(phrase("additional time(s)")),
            Effect.AdditionalEtbTriggers::new);

    /// "[kind] abilities of [scope] trigger [N] additional time[s]." —
    /// Hama Pashar, Ruin Seeker: "Room abilities of dungeons you own
    /// trigger an additional time."
    static final Parser<Effect.AbilityKindTriggersAdditional> ABILITY_KIND_TRIGGERS_ADDITIONAL = sequence(
            word().followedBy(word("abilities")).followedBy(word("of")),
            SelectorParsers.SELECTOR.followedBy(word("trigger")),
            anyOf(anyWord("an", "a").thenReturn(Amount.exact(1)), SelectorParsers.AMOUNT)
                    .followedBy(phrase("additional time(s)")),
            Effect.AbilityKindTriggersAdditional::new);

    /// "[subject] can't be equipped." — e.g., Goblin Brawler.
    static final Parser<Effect.CantBeEquipped> CANT_BE_EQUIPPED =
            SubjectParsers.SUBJECT.followedBy(words("can't be equipped")).map(Effect.CantBeEquipped::new);

    /// "Unattach [selector] from [target]." — e.g., Disarm: "Unattach all
    /// Equipment from target creature."
    static final Parser<Effect.Unattach> UNATTACH = sequence(
            w("unattach").then(SelectorParsers.SELECTOR), w("from").then(SubjectParsers.SUBJECT), Effect.Unattach::new);

    /// "[player] may cast [what] [duration]? as though [clause]." —
    /// Vedalken Orrery, Borne Upon a Wind ("this turn as though they had
    /// flash"). The as-though clause is captured as free text; the
    /// optional duration between the subject and the as-though tail is
    /// consumed as flavor for now.
    static final Parser<Effect.CastAsThough> CAST_AS_THOUGH = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(words("may cast")),
            SelectorParsers.SELECTOR.optionallyFollowedBy(DURATION, (s, _) -> s).followedBy(words("as though")),
            word().atLeastOnce().map(words -> String.join(" ", words)),
            Effect.CastAsThough::new);

    /// "[player] may cast [what] without paying [its|their] mana cost[s]."
    /// — Dracogenesis.
    static final Parser<Effect.CastWithoutPaying> CAST_WITHOUT_PAYING = sequence(
                    SubjectParsers.PLAYER_SUBJECT.followedBy(words("may cast")),
                    SelectorParsers.SELECTOR,
                    Effect.CastWithoutPaying::new)
            // Optional "from [your|their] [zone]" scope — Omniscience:
            // "You may cast spells from your hand without paying their
            // mana costs." Consumed as flavor for now.
            .optionallyFollowedBy(
                    word("from")
                            .then(anyWord("your", "their", "its", "a", "any"))
                            .then(SelectorParsers.ZONE_NAME),
                    (cwp, _) -> cwp)
            .followedBy(phrase("without paying [its|their] mana cost(s)"));

    /// "[player] may spend [X] mana as though it were [Y] mana." — color
    /// substitution on mana spend (Sunglasses of Urza). Only color-to-color
    /// substitution is captured here; broader forms ("mana of any color")
    /// can grow new arms as they appear.
    static final Parser<Effect.SpendManaAsThough> SPEND_MANA_AS_THOUGH = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(words("may spend")),
            SelectorParsers.COLOR.followedBy(word("mana")).followedBy(words("as though it were")),
            SelectorParsers.COLOR.followedBy(word("mana")),
            Effect.SpendManaAsThough::new);

    /// "[subject] can't attack or block alone." — Ember Beast. Fans
    /// out into two peer restrictions: an AttackRestriction
    /// (CantAlone) and a CantBlockAlone. The outer [#CLAUSE] level
    /// flattens the list.
    static final Parser<List<Effect>> CANT_ATTACK_OR_BLOCK_ALONE = SubjectParsers.SUBJECT
            .followedBy(words("can't attack or block alone"))
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
            SubjectParsers.SUBJECT.followedBy(words("can't attack, block, or crew")),
            SelectorParsers.SELECTOR,
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

    /// "[subject] can attack as though they didn't have [ability]." —
    /// Rolling Stones. Captures the ignored ability as a bare keyword name.
    static final Parser<Effect.AttackRestriction> CAN_ATTACK_AS_THOUGH_WITHOUT = sequence(
            SubjectParsers.SUBJECT.followedBy(words("can attack as though")),
            anyWord("they", "it")
                    .then(anyOf(word("didn't"), words("did not")))
                    .then(word("have"))
                    .then(word()),
            (subj, ability) -> new Effect.AttackRestriction(
                    subj, new Effect.AttackRestriction.Capability.AsThoughWithout(ability.toLowerCase())));

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
            .optionallyFollowedBy(words("if able"), (s, _) -> s);

    /// "[player]'s life total becomes N."
    static final Parser<Effect.LifeTotalBecomes> LIFE_TOTAL_BECOMES = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(string("'s")).followedBy(words("life total becomes")),
            SelectorParsers.AMOUNT,
            Effect.LifeTotalBecomes::new);

    /// Named game rule inside quotes — for now only the "legend rule".
    private static final Parser<GameRule> RULE_NAME =
            anyOf(ciWords("legend rule").thenReturn(GameRule.LEGEND_RULE));

    /// `The "<rule>" doesn't apply.` — e.g., Mirror Gallery.
    static final Parser<Effect.RuleDoesntApply> RULE_DOESNT_APPLY = ciWords("the")
            .then(string("\""))
            .then(RULE_NAME)
            .followedBy(string("\""))
            .followedBy(words("doesn't apply"))
            .map(Effect.RuleDoesntApply::new);

    // Skip (rule 614.10)

    /// Bare step-name parser — no trailing "step(s)" suffix. Used by the
    /// demonstrative skip form ("skip that draw").
    private static final Parser<Step> STEP_NAME = anyOf(
            ciWords("beginning of combat").thenReturn(Step.BEGINNING_OF_COMBAT),
            ciWords("declare attackers").thenReturn(Step.DECLARE_ATTACKERS),
            ciWords("declare blockers").thenReturn(Step.DECLARE_BLOCKERS),
            ciWords("combat damage").thenReturn(Step.COMBAT_DAMAGE),
            ciWords("end of combat").thenReturn(Step.END_OF_COMBAT),
            w("untap").thenReturn(Step.UNTAP),
            w("upkeep").thenReturn(Step.UPKEEP),
            w("draw").thenReturn(Step.DRAW),
            w("end").thenReturn(Step.END),
            w("cleanup").thenReturn(Step.CLEANUP));

    /// Matches a Step name followed by "step"/"steps". Multi-word step
    /// names come first via [#STEP_NAME]'s ordering so their first
    /// word isn't consumed by a shorter alternative.
    private static final Parser<Step> SKIPPABLE_STEP = STEP_NAME.followedBy(phrase("step(s)"));

    private static final Parser<Phase> SKIPPABLE_PHASE = anyOf(
                    w("beginning").thenReturn(Phase.BEGINNING),
                    w("main").thenReturn(Phase.MAIN),
                    w("combat").thenReturn(Phase.COMBAT),
                    w("ending").thenReturn(Phase.ENDING))
            .followedBy(phrase("phase(s)"));

    private static final Parser<Skippable> SKIPPABLE = anyOf(
            SKIPPABLE_STEP.<Skippable>map(Skippable.OfStep::new),
            SKIPPABLE_PHASE.<Skippable>map(Skippable.OfPhase::new),
            // "<count> turns" (Eater of Days: "skip your next two
            // turns.") — count-bearing plural form, tried before the
            // bare singular.
            sequence(SelectorParsers.AMOUNT, phrase("turn(s)"), (count, _) -> new Skippable.Turn(count)),
            anyCiWord("turns", "turn").thenReturn(Skippable.Turn.one()));

    /// Tail of the "all X of [possessive] [next]? turn" skip form — the
    /// possessive-prefixed turn reference used by [#SKIP_NO_PLAYER]'s
    /// distributive arm. Consumed as flavor.
    private static final Parser<String> OF_NEXT_TURN_TAIL = word("of")
            .then(anyWord("your", "their", "his", "her", "its"))
            .then(anyOf(word("next").followedBy(phrase("turn(s)")), phrase("turn(s)")));

    /// "[player] skip[s] [target] [what]." — rule 614.10. `target` is
    /// either a possessive pronoun ("your/their/…") optionally preceded by
    /// "next", or the all-of-type form "all X of [possessive] [next]? turn"
    /// (e.g., False Peace: "skips all combat phases of their next turn.").
    private static final Parser<Skippable> SKIP_NO_PLAYER = anyCiWord("skips", "skip")
            .then(anyOf(
                    // "all [X] of [possessive] [next]? turn(s)" — emits the
                    // same Skippable as the short form; the "of … turn" tail
                    // is consumed as flavor since per-turn scope is implicit.
                    sequence(ciWords("all").then(SKIPPABLE), OF_NEXT_TURN_TAIL, (s, _) -> s),
                    // "the [step-name] step of that turn" — demonstrative
                    // variant (Savor the Moment: "Skip the untap step of
                    // that turn."). The "of that turn" tail is flavor
                    // since the scope is implicit.
                    sequence(
                            word("the").then(SKIPPABLE),
                            word("of").then(anyWord("that", "this")).then(word("turn")),
                            (s, _) -> s),
                    // "that [step-name]" — demonstrative form referring back
                    // to an event in the triggering clause (Obstinate
                    // Familiar: "If you would draw a card, you may skip
                    // that draw instead."). Matches a bare step name
                    // without requiring the "step" suffix.
                    word("that").then(STEP_NAME).<Skippable>map(Skippable.OfStep::new),
                    anyWord("your", "their", "his", "her", "its")
                            .then(anyOf(word("next").then(SKIPPABLE), SKIPPABLE))));

    static final Parser<Effect.Skip> SKIP = anyOf(
                    sequence(SubjectParsers.PLAYER_SUBJECT, SKIP_NO_PLAYER, Effect.Skip::new),
                    SKIP_NO_PLAYER.map(s -> new Effect.Skip(YOU, s)))
            .optionallyFollowedBy(DURATION, Effect.Skip::withDuration);

    static final Parser<Effect.CantSearchLibraries> CANT_SEARCH_LIBRARIES =
            SubjectParsers.SUBJECT.followedBy(words("can't search libraries")).map(Effect.CantSearchLibraries::new);

    /// "[players] can cast spells \[and activate abilities\]? only during
    /// [timing]." — e.g., Dosan the Falling Leaf ("spells only"); City
    /// of Solitude ("spells and activate abilities only"). Timing is
    /// captured as free text.
    static final Parser<Effect.RestrictSpellTiming> RESTRICT_SPELL_TIMING = sequence(
            SubjectParsers.PLAYER_SUBJECT
                    .followedBy(words("can cast spells"))
                    .optionallyFollowedBy(words("and activate abilities"), (s, _) -> s)
                    .followedBy(words("only during")),
            WORD_OR_CONTRACTION.atLeastOnce().map(words -> String.join(" ", words)),
            Effect.RestrictSpellTiming::new);

    static final Parser<Effect.CantCast> CANT_CAST = sequence(
                    SubjectParsers.SUBJECT.followedBy(words("can't cast")),
                    SelectorParsers.SELECTOR,
                    Effect.CantCast::new)
            .optionallyFollowedBy(word("spells"), (cc, ign) -> cc)
            // Trailing zone restriction — "from anywhere other than
            // [zone]" (Drannith Magistrate: "Your opponents can't cast
            // spells from anywhere other than their hands."). Consumed
            // as flavor; CantCast carries no explicit zone field yet.
            .optionallyFollowedBy(
                    words("from anywhere other than")
                            .then(anyWord("your", "their", "its"))
                            .then(SelectorParsers.PLURAL_ZONE_NAME),
                    (cc, _) -> cc)
            .optionallyFollowedBy(DURATION, Effect.CantCast::withDuration);

    /// "[subject] can't [draw|cast] more than [N] [cards|spells] each turn."
    /// — a per-turn upper limit (Spirit of the Labyrinth, Arcane Laboratory,
    /// Eidolon of Rhetoric).
    static final Parser<Effect.PerTurnLimit> PER_TURN_LIMIT = sequence(
            SubjectParsers.SUBJECT.followedBy(word("can't")),
            anyOf(
                    word("draw").followedBy(words("more than")).thenReturn(Effect.PerTurnLimit.Action.DRAW_CARDS),
                    word("cast").followedBy(words("more than")).thenReturn(Effect.PerTurnLimit.Action.CAST_SPELLS)),
            SelectorParsers.AMOUNT
                    // Allow optional qualifiers between the count and the
                    // noun (Deafening Silence: "more than one noncreature
                    // spell each turn."). Qualifiers are consumed as flavor
                    // since {@link Effect.PerTurnLimit} captures only the
                    // action and count for now.
                    .followedBy(SelectorParsers.QUALIFIER.atLeastOnce().optional())
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
            ciWords("your").thenReturn(Subject.player(Subject.PlayerRef.YOU)),
            ciWords("their").thenReturn(Subject.player(Subject.PlayerRef.THEY)),
            SubjectParsers.PLAYER_SUBJECT.followedBy(string("'s")));

    /// "[possessive] maximum hand size is reduced/increased by N." — Delta
    /// variant (e.g., Thought Nibbler: "Your maximum hand size is reduced by
    /// two.").
    static final Parser<Effect.MaximumHandSize> MAXIMUM_HAND_SIZE_DELTA = sequence(
            POSSESSIVE_PLAYER.followedBy(words("maximum hand size is")),
            anyOf(
                    word("reduced")
                            .followedBy(word("by"))
                            .then(SelectorParsers.NUMBER)
                            .map(n -> -n),
                    word("increased").followedBy(word("by")).then(SelectorParsers.NUMBER)),
            (p, delta) -> new Effect.MaximumHandSize(p, new Effect.MaximumHandSize.HandSize.Delta(delta)));

    /// Trailing `if <predicate>` condition on any effect — emits a
    /// [Condition.Kind#IF]. Used as an optional suffix on
    /// [#EFFECT] so `Draw a card if you have no cards in hand.`
    /// becomes [Effect.Conditional]. Uses
    /// [#CONDITION_TOKEN] so English contractions ("you've",
    /// "don't") round-trip inside the predicate.
    private static final Parser<Condition> IF_CONDITION = w("if").then(
                    CONDITION_TOKEN.atLeastOnce().map(words -> String.join(" ", words)))
            .map(Condition::ifCondition);

    // Cost modification: "<subject> cost[s] <mana> more/less [to cast]"

    private static final Parser<CostDelta> COST_DELTA =
            anyOf(w("more").thenReturn(CostDelta.MORE), w("less").thenReturn(CostDelta.LESS));

    /// Known cost-keyword names ("buyback", "kicker", "cycling", …).
    /// Underscored enum constants (LEVEL_UP) map to their oracle-text form.
    private static final Parser<CostKeyword> COST_KEYWORD = anyOf(
            ciWords("level up").thenReturn(CostKeyword.LEVEL_UP),
            ciWords("aura swap").thenReturn(CostKeyword.AURA_SWAP),
            w("buyback").thenReturn(CostKeyword.BUYBACK),
            w("kicker").thenReturn(CostKeyword.KICKER),
            w("multikicker").thenReturn(CostKeyword.MULTIKICKER),
            w("flashback").thenReturn(CostKeyword.FLASHBACK),
            w("madness").thenReturn(CostKeyword.MADNESS),
            w("echo").thenReturn(CostKeyword.ECHO),
            w("cycling").thenReturn(CostKeyword.CYCLING),
            w("equip").thenReturn(CostKeyword.EQUIP),
            w("fortify").thenReturn(CostKeyword.FORTIFY),
            w("ward").thenReturn(CostKeyword.WARD),
            w("bestow").thenReturn(CostKeyword.BESTOW),
            w("dash").thenReturn(CostKeyword.DASH),
            w("entwine").thenReturn(CostKeyword.ENTWINE),
            w("splice").thenReturn(CostKeyword.SPLICE),
            w("replicate").thenReturn(CostKeyword.REPLICATE),
            w("suspend").thenReturn(CostKeyword.SUSPEND),
            w("transmute").thenReturn(CostKeyword.TRANSMUTE),
            w("transfigure").thenReturn(CostKeyword.TRANSFIGURE),
            w("recover").thenReturn(CostKeyword.RECOVER),
            w("ninjutsu").thenReturn(CostKeyword.NINJUTSU),
            w("outlast").thenReturn(CostKeyword.OUTLAST),
            w("scavenge").thenReturn(CostKeyword.SCAVENGE),
            w("unearth").thenReturn(CostKeyword.UNEARTH),
            w("reinforce").thenReturn(CostKeyword.REINFORCE),
            w("awaken").thenReturn(CostKeyword.AWAKEN),
            w("emerge").thenReturn(CostKeyword.EMERGE),
            w("escape").thenReturn(CostKeyword.ESCAPE),
            w("embalm").thenReturn(CostKeyword.EMBALM),
            w("eternalize").thenReturn(CostKeyword.ETERNALIZE));

    /// Cost source for a modify-cost effect: a keyword ability ("buyback
    /// costs"), "[Keyword] abilities you activate" (Fluctuator), or a
    /// spell-matching subject ("spells you cast"). Keyword variants are
    /// tried first so their trailing "costs"/"abilities" isn't consumed by
    /// the subject grammar.
    private static final Parser<CostSource> COST_SOURCE = anyOf(
            COST_KEYWORD.followedBy(word("costs")).<CostSource>map(CostSource.Ability::new),
            // "[Keyword] abilities you activate" — treats the keyword's
            // activation costs collectively (e.g., Fluctuator).
            COST_KEYWORD.followedBy(phrase("abilities you activate")).<CostSource>map(CostSource.Ability::new),
            SubjectParsers.SUBJECT.<CostSource>map(CostSource.Spell::new));

    /// "[source] cost[s] <mana> more/less [to cast | to activate]." Handles
    /// both spell-subject forms (`Spells you cast cost {1` more to
    /// cast}) and keyword-ability forms (`Buyback costs cost {2`
    /// less}, `Cycling abilities you activate cost {2` less to
    /// activate}).
    /// Optional leading duration prefix on a cost-modifier (Naiad of
    /// Hidden Coves: "During turns other than yours, spells you cast cost
    /// {1} less to cast."). The prefix is flavor for now — [Effect.ModifyCost] has no duration slot yet.
    private static final Parser<?> MODIFY_COST_DURATION_PREFIX = anyOf(DURING_YOUR_TURN, DURING_OTHERS_TURN);

    static final Parser<Effect.ModifyCost> MODIFY_COST = anyOf(
                    sequence(
                            MODIFY_COST_DURATION_PREFIX,
                            sequence(
                                    COST_SOURCE.followedBy(phrase("cost(s)")),
                                    MANA_SYMBOL.atLeastOnce(),
                                    COST_DELTA,
                                    Effect.ModifyCost::new),
                            (_, mc) -> mc),
                    sequence(
                            COST_SOURCE.followedBy(phrase("cost(s)")),
                            MANA_SYMBOL.atLeastOnce(),
                            COST_DELTA,
                            Effect.ModifyCost::new),
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
                                            Selector.TypeExpression.single(
                                                    Selector.SingleType.ofGameObject(GameObjectType.ABILITY)))))),
                            phrase("cost(s)").then(MANA_SYMBOL.atLeastOnce()),
                            COST_DELTA,
                            Effect.ModifyCost::new))
            .optionallyFollowedBy(anyOf(ciWords("to cast"), ciWords("to activate")), (mc, ign) -> mc)
            // Trailing "except during [phrase] turn" — a duration-
            // exclusion tail (Defense Grid: "Each spell costs {3} more
            // to cast except during its controller's turn."). Consumed
            // as flavor via free-text capture up to the trailing "turn".
            .optionallyFollowedBy(
                    words("except during")
                            .then(WORD_OR_CONTRACTION
                                    .suchThat(w -> !w.equalsIgnoreCase("turn"), "non-turn word")
                                    .atLeastOnce())
                            .followedBy(word("turn")),
                    (mc, _) -> mc)
            // Trailing "if [predicate]" condition — Gigastorm Titan:
            // "This spell costs {3} less to cast if you've cast another
            // spell this turn."
            .optionallyFollowedBy(IF_CONDITION, Effect.ModifyCost::withCondition)
            // Trailing "unless [predicate]" condition — Suppression
            // Field: "Activated abilities cost {2} more to activate
            // unless they're mana abilities."
            .optionallyFollowedBy(UNLESS_PREDICATE, Effect.ModifyCost::withCondition)
            // Trailing "for each …" multiplier — Ghoultree: "This
            // spell costs {1} less to cast for each creature card in
            // your graveyard."
            .optionallyFollowedBy(CountOfParsers.FOR_EACH, Effect.ModifyCost::withScaleBy);

    // Lose ability

    /// The tail of a "[subject] lose[s] …" clause. Either "all abilities"
    /// (produces [Effect.LoseAbility.Lost.All]) or a keyword list
    /// (produces [Effect.LoseAbility.Lost.Specific]).
    private static final Parser<Effect.LoseAbility.Lost> LOST_ABILITIES = anyOf(
            ciWords("all abilities").thenReturn(Effect.LoseAbility.Lost.All.ALL),
            KeywordParsers.KEYWORD_LIST.map(Effect.LoseAbility.Lost.Specific::new));

    static final Parser<Effect.LoseAbility> LOSE_ABILITY = sequence(
                    SubjectParsers.SUBJECT.followedBy(phrase("lose(s)")), LOST_ABILITIES, Effect.LoseAbility::new)
            .optionallyFollowedBy(DURATION, Effect.LoseAbility::withDuration);

    /// "[subject] can't be the target[s] of spells or abilities / of [what]."
    static final Parser<Effect.CantBeTargeted> CANT_BE_TARGETED = anyOf(
            SubjectParsers.SUBJECT
                    .followedBy(phrase("can't be the target(s) of spells or abilities"))
                    .map(Effect.CantBeTargeted::new),
            sequence(
                    SubjectParsers.SUBJECT.followedBy(phrase("can't be the target(s) of")),
                    SelectorParsers.SELECTOR,
                    Effect.CantBeTargeted::new));

    /// "[subject] must be blocked [if able]." / "[subject] blocks [if able]
    /// [this turn]." — the former already exists as MUST_BE_BLOCKED; this is
    /// the must-block-as-blocker variant.
    static final Parser<Effect.MustBlock> MUST_BLOCK = SubjectParsers.SUBJECT
            .followedBy(phrase("block(s)"))
            .map(Effect.MustBlock::new)
            .optionallyFollowedBy(SubjectParsers.SUBJECT, Effect.MustBlock::withTarget)
            .optionallyFollowedBy(DURATION, Effect.MustBlock::withDuration)
            .optionallyFollowedBy(words("if able"), (mb, ign) -> mb);

    /// "[players] play with [their/its/your] hands revealed."
    static final Parser<Effect.PlayWithHandsRevealed> PLAY_WITH_HANDS_REVEALED = SubjectParsers.PLAYER_SUBJECT
            .followedBy(phrase("play with [your|their|its] hand(s) revealed"))
            .map(Effect.PlayWithHandsRevealed::new);

    // ── Master dispatcher ──────────────────────────────────────────────

    // IF_CONDITION is defined earlier (before MODIFY_COST) so MODIFY_COST
    // can attach it as a trailing condition.

    /// Trailing `unless <predicate>` condition on any effect — emits a
    /// [Uses [#CONDITION_TOKEN][Condition.Kind#UNLESS].] so the
    /// predicate can contain mana symbols (e.g., Rhystic Deluge: "Tap
    /// target creature unless its controller pays {1}.").
    private static final Parser<Condition> UNLESS_CONDITION = w("unless")
            .then(CONDITION_TOKEN.atLeastOnce().map(words -> String.join(" ", words)))
            .map(Condition::unlessCondition);

    private static final Parser<Effect> BASE_EFFECT = Parser.<Effect>anyOf(
            RemovalEffectParsers.DESTROY,
            RemovalEffectParsers.EXILE,
            RemovalEffectParsers.BOUNCE,
            RemovalEffectParsers.SACRIFICE_WITH_SCALE,
            DamageEffectParsers.DEAL_DIVIDED_DAMAGE, // must precede DEAL_DAMAGE (shares "deals N damage" prefix)
            DamageEffectParsers.DEAL_DAMAGE,
            DamageEffectParsers.GAIN_LIFE,
            DamageEffectParsers.LOSE_LIFE,
            CardManipulationEffectParsers.DRAW,
            CardManipulationEffectParsers.DISCARD,
            CardManipulationEffectParsers.MILL,
            CardManipulationEffectParsers.SCRY,
            CardManipulationEffectParsers.SEARCH,
            CardManipulationEffectParsers.SHUFFLE,
            SWITCH_PT,
            CREWS_WITH_BOOSTED_POWER,
            ATTACH,
            FLIP_COINS,
            ROLL_PLANAR_DIE,
            DOUBLE_MANA,
            MOVE_COUNTERS,
            MAY_ACTIVATE_ANY_TIME,
            CardManipulationEffectParsers.REVEAL,
            TapEffectParsers.TAP_OR_UNTAP, // must precede TAP — "tap or untap" starts with "tap"
            PLAY_WITH_TOP_REVEALED,
            CAN_BLOCK, // must precede CANT_BLOCK — both share "can[…]block" prefix
            TapEffectParsers.TAP,
            TapEffectParsers.UNTAP,
            CounterEffectParsers.ADD_COUNTERS,
            CounterEffectParsers.DISTRIBUTE_COUNTERS,
            CounterEffectParsers.REMOVE_ALL_COUNTERS, // must precede REMOVE_COUNTERS (shares "remove" prefix)
            CounterEffectParsers.REMOVE_COUNTERS,
            COUNTER_SPELL,
            GAIN_ABILITY,
            MODIFY_PT,
            GAIN_CONTROL,
            EXCHANGE_CONTROL,
            CREATE_TOKEN,
            ADD_MANA,
            TRANSFORM,
            COPY,
            FIGHT,
            CANT_WIN_GAME, // must precede WIN_GAME so "can't" prefix wins
            CANT_LOSE_GAME, // must precede LOSE_GAME so "can't" prefix wins
            WIN_GAME,
            LOSE_GAME,
            ZONE_MOVE,
            PREVENT,
            DAMAGE_CANT_BE_PREVENTED,
            CANT_HAVE_COUNTERS,
            CANT_BE_REGENERATED,
            CANT_BE_EQUIPPED,
            UNATTACH,
            CAST_WITHOUT_PAYING, // must precede CAST_AS_THOUGH / CAST_FROM_ZONE (same "may cast" prefix)
            CAST_AS_THOUGH, // must precede CAST_FROM_ZONE (both start with "may cast")
            SPEND_MANA_AS_THOUGH,
            SUPPRESS_ETB_TRIGGERS,
            CANT_BLOCK_ALONE, // must precede CANT_BLOCK
            ONLY_ATTACK_ALONE, // must precede CANT_ATTACK_ALONE — both end with "attack alone"
            POPULATE,
            CANT_ATTACK_ALONE, // must precede CANT_ATTACK
            CAN_ATTACK_AS_THOUGH_WITHOUT,
            MUST_ATTACK_OR_BLOCK, // must precede MUST_ATTACK (shares "[subject] attacks" prefix)
            MUST_ATTACK,
            CANT_ATTACK_WHOM, // must precede CANT_ATTACK
            CANT_BLOCK,
            CANT_ATTACK,
            CANT_PLAY_LANDS,
            TAKE_INITIATIVE,
            UNTAP_LIMIT, // must precede DONT_UNTAP (longer "can't untap more than" prefix)
            DONT_UNTAP,
            CAST_COUNT_LIMIT,
            CHOOSE_PLAYER_VOTE, // must precede CHOOSE (starts with "choose")
            CHOOSE_COLOR, // must precede CHOOSE — "a color" would otherwise match Subject
            SET_BASE_PT,
            EXCHANGE_ZONES,
            EXCHANGE_LIFE_WITH_PROPERTY,
            MANA_POOL_PERSISTS,
            LOSE_UNSPENT_MANA,
            MANA_SPEND_RESTRICTION,
            ADDITIONAL_COST,
            ATTACK_LIMIT,
            DOUBLE_PT,
            CHANGE_THE_TARGET,
            ENTER_AS_COPY,
            CANT_CYCLE,
            CANT_PHASE_OUT,
            DEFINE_X,
            GAIN_ENERGY,
            CREWS_USING,
            CANT_BE_BLOCKED,
            CANT_SEARCH_LIBRARIES,
            PER_TURN_LIMIT, // must precede CANT_CAST (shares "can't cast" prefix)
            CANT_CAST,
            RESTRICT_SPELL_TIMING,
            NO_MAXIMUM_HAND_SIZE,
            MAXIMUM_HAND_SIZE_DELTA,
            TAKE_EXTRA_TURN,
            PLAY_LANDS_FROM,
            PLAY_ADDITIONAL_LANDS,
            SKIP,
            RING_TEMPTS,
            LOOK_AT,
            PUT_BACK, // must precede ZONE_MOVE / BOUNCE (shares "put" prefix)
            CAST_FROM_ZONE,
            CHOOSE_NEW_TARGETS,
            CHANGE_ANY_TARGETS,
            DISCARD_ALL_BUT_ONE, // must precede CHOOSE (shares "[player] chooses" head)
            CHOOSE,
            BECOME_MONARCH,
            EXCHANGE_LIFE_TOTALS,
            GET_MARKER,
            CANT_BE_COUNTERED,
            CANT_BE_TARGETED,
            ABLE_TO_BLOCK_DO_SO, // must precede MUST_BE_BLOCKED
            MUST_BE_BLOCKED,
            MUST_BLOCK,
            PLAY_WITH_HANDS_REVEALED,
            LIFE_TOTAL_BECOMES,
            RULE_DOESNT_APPLY,
            ENTER_WITH_COUNTERS, // must precede ENTER_TAPPED
            ENTER_TAPPED,
            LOSE_SUPERTYPE, // must precede SET_* since all share "are/is" head
            SET_SUPERTYPE,
            SET_PROPERTY_VALUE,
            TURN_FACE_UP,
            TURN_FACE_DOWN,
            SPEND_THIS_MANA_ONLY,
            ACTIVATION_LIMIT,
            ACTIVATE_ONLY_IF,
            ACTIVATE_ONLY_AS_SORCERY,
            ACTIVATE_ONLY_DURING,
            PLAY_FROM_OUTSIDE,
            STILL_TYPE, // must precede BECOME_PT_TYPE so "they're still" wins
            BECOME_PT_TYPE, // must precede SET_COLORS since both start with "are/is"
            SET_COLORS,
            ADD_CARD_TYPE, // must precede SET_SUBTYPE (shares "are X" head)
            SET_EVERY_SUBTYPE_TYPE, // must precede SET_SUBTYPE (shares "are every X type" head)
            SET_SUBTYPE,
            REGENERATE,
            LOSE_ABILITY,
            MODIFY_COST,
            // CANT_ACTIVATE's "Activated abilities of …" prefix
            // collides with "Activated abilities cost …" (Suppression
            // Field). Tried last so MODIFY_COST gets first crack at
            // the shared lead-in.
            CANT_ACTIVATE);

    /// Prefix "If [condition], [effect]" — e.g., Idle Thoughts: "If you
    /// have no cards in hand." The predicate runs to the comma. Does NOT
    /// match "If you do," / "If they do," — those tails are consumed by
    /// [#IF_DO_CONTINUATION] as follow-ups to a preceding
    /// [Effect.Optional].
    private static final Parser<Condition> IF_PREFIX_CONDITION = sequence(
                    w("if").then(CONDITION_TOKEN.atLeastOnce().map(words -> String.join(" ", words))),
                    string(","),
                    (text, _) -> text)
            .suchThat(s -> !s.equalsIgnoreCase("you do") && !s.equalsIgnoreCase("they do"), "non-may-linked if")
            .map(Condition::ifCondition);

    /// Prefix "Unless [predicate], [effect]" — Rhystic Syphon:
    /// "Unless target player pays {3}, that player loses 5 life and
    /// you gain 5 life."
    private static final Parser<Condition> UNLESS_PREFIX_CONDITION = sequence(
            w("unless").then(CONDITION_TOKEN.atLeastOnce().map(words -> String.join(" ", words))),
            string(","),
            (text, _) -> new Condition(Condition.Kind.UNLESS, text));

    /// `. If you/they do, [effect]` — follow-up clause that attaches to a
    /// preceding [Effect.Optional] (action wrapped by "you may …").
    /// Consumes the preceding sentence-terminating period so downstream
    /// `EFFECT_SEQUENCE` delimiters see a clean boundary.
    private static final Parser<Effect> IF_DO_CONTINUATION =
            string(".").then(phrase("If [you|they] do")).followedBy(string(",")).then(BASE_EFFECT);

    /// `[player] may <action>` — a single generic parser. Uses
    /// [Parser#flatMap] to capture the already-parsed player subject
    /// in a closure and dispatch to any player-scoped action tail. The
    /// optional `. If you/they do, …` continuation attaches to the produced
    /// [Effect.Optional] so the "may" and its conditional stay
    /// structurally linked. Adding a new may-able effect = one more branch
    /// in the inner `anyOf`.
    static final Parser<Effect.Optional> MAY = SubjectParsers.PLAYER_SUBJECTS
            .followedBy(word("may"))
            .flatMap(subject -> Parser.<Effect>anyOf(
                    CardManipulationEffectParsers.DRAW_NO_PLAYER.map(amount -> new Effect.Draw(subject, amount)),
                    CardManipulationEffectParsers.DISCARD_NO_PLAYER.map(d -> new Effect.Discard(subject, d)),
                    DamageEffectParsers.GAIN_LIFE_NO_PLAYER.map(amount -> new Effect.GainLife(subject, amount)),
                    DamageEffectParsers.LOSE_LIFE_NO_PLAYER.map(amount -> new Effect.LoseLife(subject, amount)),
                    PLAY_ADDITIONAL_LANDS_NO_PLAYER
                            .map(amount -> new Effect.PlayAdditionalLands(subject, amount))
                            .optionallyFollowedBy(DURATION, Effect.PlayAdditionalLands::withDuration)
                            .map(e -> (Effect) e),
                    // Effects where the "may" actor is the implicit source,
                    // not a parameter on the effect — the target comes from
                    // the parser directly (e.g., "may tap target creature",
                    // "may destroy target Aura", "may add {R}{R}", "may
                    // skip that draw", "may counter target spell").
                    TapEffectParsers.TAP,
                    TapEffectParsers.UNTAP,
                    RemovalEffectParsers.BOUNCE,
                    CardManipulationEffectParsers.SHUFFLE,
                    RemovalEffectParsers.DESTROY,
                    ADD_MANA,
                    SKIP,
                    COUNTER_SPELL,
                    LOOK_AT,
                    ZONE_MOVE,
                    // "may put a -1/-1 counter on target creature"
                    // (Festering Mummy) — the "may" actor is the
                    // implicit source; the counter target comes from the
                    // parser directly.
                    CounterEffectParsers.ADD_COUNTERS,
                    CounterEffectParsers.REMOVE_COUNTERS,
                    // "pay <cost>" — optional payment (Inheritance:
                    // "Whenever a creature dies, you may pay {3}. If you
                    // do, draw a card."). Reuses the full cost expression
                    // so alternative / compound costs work here too.
                    w("pay").then(CostParsers.COST_EXPRESSION).map(c -> (Effect) new Effect.Pay(subject, c)),
                    // "have [player] <verb>" — causative form (Jace's
                    // Erasure: "you may have target player mill a card.").
                    // Dispatches via {@link #PLAYER_VERB_BODY} so the
                    // inner verb set matches the shared-actor chain used
                    // elsewhere. Also accepts a MILL body specifically
                    // since the chain doesn't include Mill yet.
                    w("have")
                            .then(SubjectParsers.PLAYER_SUBJECTS)
                            .flatMap(p -> Parser.<Effect>anyOf(
                                    PLAYER_VERB_BODY.map(fn -> fn.apply(p)),
                                    CardManipulationEffectParsers.MILL_NO_PLAYER.map(
                                            a -> (Effect) new Effect.Mill(p, a)))),
                    // "have [source] deal N damage to [target]" —
                    // causative damage form where the source is a non-
                    // player subject (Goblin Arsonist: "you may have it
                    // deal 1 damage to any target.").
                    sequence(
                            word("have").then(SubjectParsers.ATOMIC_SUBJECT).followedBy(phrase("deal(s)")),
                            SelectorParsers.AMOUNT.followedBy(words("damage to")),
                            SubjectParsers.ATOMIC_SUBJECT,
                            (src, amt, tgt) -> (Effect) new Effect.DealDamage(src, amt, tgt)),
                    // "have [subject] enter as a copy of [target]" —
                    // causative enter-as-copy (Mirror Image: "You may
                    // have this creature enter as a copy of a creature
                    // you control."). All mid-sentence tokens are
                    // lowercase: "enter" (never "enters" — the have-
                    // causative always uses the base form) and the fixed
                    // connective "as a copy of".
                    sequence(
                            w("have").then(SubjectParsers.SUBJECT).followedBy(words("enter as a copy of")),
                            SubjectParsers.SUBJECT,
                            (subj, src) -> (Effect) new Effect.EnterAsCopy(subj, src))))
            .map(Effect.Optional::new)
            .optionallyFollowedBy(IF_DO_CONTINUATION, Effect.Optional::withIfDone);

    /// "If [subject] would [event], [replacement] instead." — replacement
    /// effect (rule 614, e.g., Thought Reflection: "If you would draw a
    /// card, draw two cards instead."). Must precede the generic if-prefix
    /// conditional so the "instead" suffix is honored.
    private static final Parser<Effect.Replace> REPLACE = sequence(
            w("if").then(SubjectParsers.SUBJECT),
            word("would")
                    .then(WORD_OR_CONTRACTION.atLeastOnce().map(words -> String.join(" ", words)))
                    .followedBy(string(",")),
            // Replacement allows either a plain effect or a "may"-wrapped
            // one (e.g., Obstinate Familiar: "… you may skip that draw
            // instead."), with the `instead` keyword on either end (Krark's
            // Thumb: "If you would flip a coin, instead flip two coins …").
            anyOf(
                    Parser.<Effect>anyOf(MAY, BASE_EFFECT).followedBy(word("instead")),
                    word("instead").then(Parser.<Effect>anyOf(MAY, BASE_EFFECT))),
            Effect.Replace::new);

    /// "For each <selector>, <effect>." — per-object loop (Cleansing:
    /// "For each land, destroy that land unless any player pays 1 life.").
    /// The loop body is a [#BASE_EFFECT] with its own optional trailing
    /// `unless`/`if` condition, so "destroy that land unless any
    /// player pays 1 life" round-trips with the condition attached to the
    /// destroy rather than the for-each wrapper.
    private static final Parser<Effect.ForEach> FOR_EACH_EFFECT = sequence(
            ciWords("for each").then(SelectorParsers.SELECTOR).followedBy(","),
            BASE_EFFECT
                    .<Effect>map(e -> e)
                    .optionallyFollowedBy(UNLESS_CONDITION, (e, c) -> new Effect.Conditional(e, c))
                    .optionallyFollowedBy(IF_CONDITION, (e, c) -> new Effect.Conditional(e, c)),
            Effect.ForEach::new);

    /// "For each \[player-ref\], \[body\]." — per-player loop (Blatant
    /// Thievery: "For each opponent, gain control of target permanent
    /// that player controls.").
    private static final Parser<Subject.PlayerRef> FOR_EACH_PLAYER_REF = anyOf(
            phrase("each opponent").thenReturn(Subject.PlayerRef.EACH_OPPONENT),
            phrase("each other player").thenReturn(Subject.PlayerRef.EACH_OTHER_PLAYER),
            phrase("each player").thenReturn(Subject.PlayerRef.EACH_PLAYER));

    private static final Parser<Effect.ForEachPlayer> FOR_EACH_PLAYER_EFFECT = sequence(
            phrase("For").then(FOR_EACH_PLAYER_REF).followedBy(","),
            BASE_EFFECT
                    .<Effect>map(e -> e)
                    .optionallyFollowedBy(UNLESS_CONDITION, (e, c) -> new Effect.Conditional(e, c))
                    .optionallyFollowedBy(IF_CONDITION, (e, c) -> new Effect.Conditional(e, c)),
            Effect.ForEachPlayer::new);

    /// "If <condition>, <override> instead." — shorthand replacement that
    /// overrides the previously-stated effect without spelling out a `would`
    /// event (River of Tears: "Add {U}. If you played a land this turn, add
    /// {B} instead."). Must precede the generic if-prefix conditional so the
    /// trailing "instead" is consumed.
    private static final Parser<Effect.ConditionalOverride> CONDITIONAL_OVERRIDE = sequence(
            IF_PREFIX_CONDITION,
            BASE_EFFECT.followedBy(word("instead")),
            (c, e) -> new Effect.ConditionalOverride(c, e));

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
                    // "[player] may …" — single entry point for every
                    // may-wrapped action. Must precede BASE_EFFECT so "you
                    // may X" is captured as Effect.Optional rather than a
                    // plain Effect.
                    MAY,
                    REPLACE,
                    // Panharmonicon-style trigger duplication; shares the
                    // "If …," prefix with IF_PREFIX_CONDITION so must come
                    // first. The ", that ability triggers …" tail is what
                    // distinguishes it.
                    ADDITIONAL_ETB_TRIGGERS,
                    FOR_EACH_PLAYER_EFFECT, // must precede FOR_EACH_EFFECT (player-ref vs selector)
                    FOR_EACH_EFFECT, // must precede BASE_EFFECT — its body already includes BASE_EFFECT
                    CONDITIONAL_OVERRIDE, // must precede IF_PREFIX + BASE_EFFECT (shares prefix, "instead" tail is
                    // distinguishing)
                    sequence(IF_PREFIX_CONDITION, BASE_EFFECT, (c, e) -> new Effect.Conditional(e, c)),
                    sequence(UNLESS_PREFIX_CONDITION, BASE_EFFECT, (c, e) -> new Effect.Conditional(e, c)),
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
            .optionallyFollowedBy(IF_CONDITION, (e, c) -> new Effect.Conditional(e, c))
            .optionallyFollowedBy(UNLESS_CONDITION, (e, c) -> new Effect.Conditional(e, c));

    /// A multi-effect clause — either a shared-subject chain that produces
    /// several effects ([#PLAYER_ACTOR_AND_CHAIN],
    /// [or a single [#EFFECT][#SUBJECT_AND_VERB_CHAIN])]. The
    /// [OracleParser#EFFECT_SEQUENCE] level flattens these lists so
    /// a trigger or spell body sees a flat `List<Effect>` regardless
    /// of whether each clause parsed one or many effects.
    public static final Parser<List<Effect>> CLAUSE = Parser.<List<Effect>>anyOf(
            // Syntactic chains — distribute a shared subject over multiple
            // verb bodies joined by "and".
            PLAYER_ACTOR_AND_CHAIN,
            SUBJECT_AND_VERB_CHAIN,
            // Two-effect clauses that must win over their bare single-effect
            // counterparts (the trailing "and X" would otherwise be left for
            // EFFECT_SEQUENCE's delimiter, losing context).
            DamageEffectParsers
                    .DEAL_DAMAGE_SPLIT, // must precede DEAL_DAMAGE (shares "[source] deals N damage to A" prefix)
            CounterEffectParsers.ADD_COUNTERS_PAIR, // must precede ADD_COUNTERS
            RemovalEffectParsers.EXILE_OBJECT_AND_ZONE, // must precede EXILE (two targets with possessive-zone second)
            CANT_ATTACK_BLOCK_OR_CREW, // emits three peer restrictions (attack/block/crew)
            CANT_ATTACK_OR_BLOCK_ALONE, // emits two peer restrictions (CantAttack-Alone + CantBlockAlone)
            CANT_ATTACK_OR_BLOCK, // emits two peer restrictions (CantAttack + CantBlock)
            // Fallback — a single effect produced by the usual EFFECT dispatcher.
            EFFECT.map(List::of));
}
