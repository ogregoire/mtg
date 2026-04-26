package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.AMOUNT;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.CARD_TYPE;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.GAME_OBJECT_TYPE;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.PARTICIPIAL_CLAUSE_RULE;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.SELECTOR;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.SUBTYPE;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.TYPE_EXPRESSION;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.WORD_NUMBER;
import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.ArrayList;
import java.util.List;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle.domain.PronounType;
import be.imgn.mtg.engine.oracle.domain.Subject;

/// Parsers for subjects and player references in oracle text.
final class SubjectParsers {
    private SubjectParsers() {}

    // ── Player references ──────────────────────────────────────────────

    public static final Parser<Subject.PlayerRef> PLAYER_REF = anyOf(
            phrase("Target opponent").thenReturn(Subject.PlayerRef.TARGET_OPPONENT),
            phrase("Any number of target players").thenReturn(Subject.PlayerRef.TARGET_PLAYER),
            // "Any number of target opponents" — Wheel and Deal:
            // "Any number of target opponents each discard their hands,
            // then draw seven cards.".
            phrase("Any number of target opponents").thenReturn(Subject.PlayerRef.TARGET_OPPONENT),
            // "up to [word-number] target players" — upper-bound on
            // count (Donatello's Science Lesson: "Up to two target
            // players each draw a card.").
            phrase("Up to")
                    .then(WORD_NUMBER)
                    .followedBy(phrase("target players"))
                    .thenReturn(Subject.PlayerRef.TARGET_PLAYER),
            phrase("Two target players").thenReturn(Subject.PlayerRef.TARGET_PLAYER),
            phrase("Target players").thenReturn(Subject.PlayerRef.TARGET_PLAYER),
            phrase("Target player").thenReturn(Subject.PlayerRef.TARGET_PLAYER),
            phrase("Each opponent").thenReturn(Subject.PlayerRef.EACH_OPPONENT),
            // "each other player" — includes teammates; must precede
            // "each player" so the longer match wins.
            phrase("Each other player").thenReturn(Subject.PlayerRef.EACH_OTHER_PLAYER),
            phrase("Each player").thenReturn(Subject.PlayerRef.EACH_PLAYER),
            // "a player" / "an opponent" — existential, typically a
            // trigger subject. Must precede "each" or similar to avoid
            // ambiguity at longer matches.
            phrase("A player").thenReturn(Subject.PlayerRef.A_PLAYER),
            // "any player" — treated as the existential "a player" since
            // it functions identically in oracle text (Quick Sliver:
            // "Any player may cast Sliver spells …").
            phrase("Any player").thenReturn(Subject.PlayerRef.A_PLAYER),
            phrase("An opponent").thenReturn(Subject.PlayerRef.AN_OPPONENT),
            // "one of your opponents" — existential over the
            // controller's opponents, equivalent to "an opponent"
            // (Calculating Lich: "Whenever a creature attacks one of
            // your opponents, …").
            phrase("One of your opponents").thenReturn(Subject.PlayerRef.AN_OPPONENT),
            phrase("That player").thenReturn(Subject.PlayerRef.THAT_PLAYER),
            phrase("Those players").thenReturn(Subject.PlayerRef.THOSE_PLAYERS),
            phrase("That opponent").thenReturn(Subject.PlayerRef.THAT_OPPONENT),
            phrase("Defending player").thenReturn(Subject.PlayerRef.DEFENDING_PLAYER),
            phrase("Enchanted player").thenReturn(Subject.PlayerRef.ENCHANTED_PLAYER),
            phrase("The chosen player").thenReturn(Subject.PlayerRef.CHOSEN_PLAYER),
            phrase("The chosen opponent").thenReturn(Subject.PlayerRef.CHOSEN_OPPONENT),
            phrase("Your opponents").thenReturn(Subject.PlayerRef.YOUR_OPPONENTS),
            // Bare plural "Players" at sentence start = "each player"
            // (e.g., "Players can't cycle cards.").
            phrase("Players").thenReturn(Subject.PlayerRef.EACH_PLAYER),
            phrase("You").thenReturn(Subject.PlayerRef.YOU),
            phrase("They").thenReturn(Subject.PlayerRef.THEY));

    // ── Self reference ─────────────────────────────────────────────────

    private static final Parser<Subject> SELF_REF = anyOf(
            string("~").thenReturn(Subject.selfRef(null)),
            phrase("This")
                    .then(anyOf(
                            CARD_TYPE.map(ct -> Subject.selfRef(ct.name().toLowerCase())),
                            GAME_OBJECT_TYPE.map(
                                    got -> Subject.selfRef(got.name().toLowerCase())),
                            // "this Aura", "this Equipment", "this
                            // Saga" — subtype-named self-references
                            // (Tainted Well: "When this Aura
                            // enters, draw a card.").
                            SUBTYPE.map(sub ->
                                    Subject.selfRef(sub.texts().getFirst().toLowerCase())))));

    // ── Ordinal spell reference ───────────────────────────────────────

    /// "The [first|second|third|fourth] spell you cast each turn" —
    /// positional spell reference used mainly by cost-modifying effects
    /// (Uthros Psionicist). Represented as a
    /// [Subject.PossessiveSubject] with role "spell you cast each
    /// turn" and the ordinal embedded in the possessive string.
    private static final Parser<Integer> SPELL_ORDINAL = anyOf(
            phrase("first").thenReturn(1),
            phrase("second").thenReturn(2),
            phrase("third").thenReturn(3),
            phrase("fourth").thenReturn(4));

    /// Maps the integer ordinal to the typed [Subject.PositionalSpell.Position]
    /// enum. Used by [#ORDINAL_SPELL] / [#NEXT_SPELL].
    private static Subject.PositionalSpell.Position ordinalPosition(int n) {
        return switch (n) {
            case 1 -> Subject.PositionalSpell.Position.FIRST;
            case 2 -> Subject.PositionalSpell.Position.SECOND;
            case 3 -> Subject.PositionalSpell.Position.THIRD;
            case 4 -> Subject.PositionalSpell.Position.FOURTH;
            default -> throw new IllegalArgumentException("unsupported ordinal " + n);
        };
    }

    private static final Parser<Subject> ORDINAL_SPELL = phrase("The")
            .then(SPELL_ORDINAL)
            .followedBy(phrase("spell(s) you cast each turn"))
            .<Subject>map(n -> new Subject.PositionalSpell(
                    ordinalPosition(n), List.of(), Subject.PositionalSpell.Window.YOU_CAST_EACH_TURN));

    /// "The \[ordinal\] \[qualifier\]? spell of a turn" — first-of-turn
    /// spell trigger subject, player-agnostic (Nullstone Gargoyle:
    /// "Whenever the first noncreature spell of a turn is cast, …").
    /// Distinct from [#ORDINAL_SPELL] which binds to "you cast each
    /// turn".
    private static final Parser<Subject> ORDINAL_SPELL_OF_TURN = anyOf(
            sequence(
                    phrase("The").then(SPELL_ORDINAL),
                    SelectorParsers.NEGATED_CARD_TYPE_Q.followedBy(phrase("spell of a turn")),
                    (ord, q) -> Subject.possessiveSubject("the " + ord, q + " spell of a turn")),
            phrase("The")
                    .then(SPELL_ORDINAL)
                    .followedBy(phrase("spell of a turn"))
                    .map(ord -> Subject.possessiveSubject("the " + ord, "spell of a turn")));

    /// "The next \[type \[or type\]\]? spell you cast this turn" —
    /// positional reference to the controller's next spell, optionally
    /// narrowed to one or more card types (Insist: "The next creature
    /// spell …"; Overmaster: "The next instant or sorcery spell you
    /// cast this turn can't be countered."; Hardened Berserker: "the
    /// next spell you cast this turn costs {1} less to cast.").
    private static final Parser<Subject> NEXT_SPELL = anyOf(
            phrase("The next")
                    .then(MtgParsers.orList(CARD_TYPE))
                    .followedBy(phrase("spell(s) you cast this turn"))
                    .<Subject>map(types -> new Subject.PositionalSpell(
                            Subject.PositionalSpell.Position.NEXT,
                            types,
                            Subject.PositionalSpell.Window.YOU_CAST_THIS_TURN)),
            phrase("The next spell(s) you cast this turn")
                    .<Subject>thenReturn(new Subject.PositionalSpell(
                            Subject.PositionalSpell.Position.NEXT,
                            List.of(),
                            Subject.PositionalSpell.Window.YOU_CAST_THIS_TURN)),
            // "The next \[type\]? card you play this turn" — Scout's
            // Warning. Distinct from spell forms above (rule 305: lands
            // are played, not cast).
            phrase("The next")
                    .then(MtgParsers.orList(CARD_TYPE))
                    .followedBy(phrase("card(s) you play this turn"))
                    .<Subject>map(types -> new Subject.PositionalCard(Subject.PositionalSpell.Position.NEXT, types)),
            phrase("The next card(s) you play this turn")
                    .<Subject>thenReturn(new Subject.PositionalCard(Subject.PositionalSpell.Position.NEXT, List.of())));

    // ── Top card of library / graveyard ───────────────────────────────

    /// "the top card of [owner]'s [zone]" / "the top card of [your|their|
    /// its] [zone]" — a positional card reference (e.g., Royal Herbalist:
    /// "Exile the top card of your library"; Rootwater Mystic: "Look at
    /// the top card of target player's library."; Soldevi Digger: "Put
    /// the top card of your graveyard on the bottom of your library.").
    /// Returned as a [Subject.PossessiveSubject] with role
    /// "top card of <zone>".
    private static final Parser<String> LIBRARY_OWNER = anyOf(
            PLAYER_REF.followedBy(string("'s")).map(ref -> ref.name().toLowerCase() + "'s"),
            word("your"),
            word("their"),
            word("its"));

    private static final Parser<String> TOP_ZONE_NAME = anyOf(word("library"), word("graveyard"));

    private static final Parser<Subject> TOP_CARD_OF_LIBRARY = anyOf(
            // "the top [type] card of [owner]'s [zone]" — typed positional
            // reference (Zombie Scavengers: "the top creature card of
            // your graveyard").
            sequence(
                    phrase("the top").then(CARD_TYPE).followedBy(word("card")),
                    word("of").then(LIBRARY_OWNER),
                    TOP_ZONE_NAME,
                    (type, poss, zone) ->
                            Subject.possessiveSubject(poss, "top " + type.name().toLowerCase() + " card of " + zone)),
            sequence(
                    phrase("the top card of").then(LIBRARY_OWNER),
                    TOP_ZONE_NAME,
                    (poss, zone) -> Subject.possessiveSubject(poss, "top card of " + zone)),
            // "the top N cards of [owner]'s [zone]" — Orcish Spy.
            sequence(
                    phrase("the top").then(WORD_NUMBER),
                    sequence(
                            phrase("card(s)").then(word("of")).then(LIBRARY_OWNER),
                            TOP_ZONE_NAME,
                            (poss, zone) -> poss + "|" + zone),
                    (n, combo) -> {
                        var parts = combo.split("\\|", 2);
                        return Subject.possessiveSubject(parts[0], "top " + n + " cards of " + parts[1]);
                    }));

    // ── Pronouns ───────────────────────────────────────────────────────

    private static final Parser<Subject.Pronoun> PRONOUN_BASE = anyOf(
            // Multi-word pronouns first so longer matches win.
            phrase("The rest").thenReturn(Subject.pronoun(PronounType.THE_REST)),
            // "the copy" / "the copies" — reference to a copy/copies
            // created earlier in the same resolution (Reverberate /
            // Twincast: "Copy target instant or sorcery spell. You
            // may choose new targets for the copy.").
            phrase("The copy").thenReturn(Subject.pronoun(PronounType.THE_COPY)),
            phrase("The copies").thenReturn(Subject.pronoun(PronounType.THE_COPIES)),
            // "one of them" / "both of them" — pick-one / pick-both
            // back-references to a prior target group (Wild Swing:
            // "Choose three target nonenchantment permanents. Destroy
            // one of them at random.").
            phrase("One of them").thenReturn(Subject.pronoun(PronounType.ONE_OF_THEM)),
            phrase("Both of them").thenReturn(Subject.pronoun(PronounType.BOTH_OF_THEM)),
            // "any of them" — subset back-reference (Blow Your House
            // Down: "Destroy any of them that are Walls.").
            phrase("Any of them").thenReturn(Subject.pronoun(PronounType.ANY_OF_THEM)),
            // Reflexive self-reference (e.g., Solar Blaze: "Each creature
            // deals damage to itself equal to its power.").
            phrase("Itself").thenReturn(Subject.pronoun(PronounType.ITSELF)),
            phrase("It").thenReturn(Subject.pronoun(PronounType.IT)),
            phrase("Them").thenReturn(Subject.pronoun(PronounType.THEM)),
            // Gendered pronouns on legendary characters (Pipsqueak,
            // Rebel Strongarm: "unless he has a +1/+1 counter on him.").
            // All resolve to the same back-reference semantics as
            // [PronounType#IT] / [PronounType#ITSELF] for game-engine
            // purposes; the gender is flavor.
            phrase("[He|She]").thenReturn(Subject.pronoun(PronounType.IT)),
            phrase("[Him|Her]").thenReturn(Subject.pronoun(PronounType.ITSELF)));

    /// Pronoun + optional restrictive that-clause. Stays narrow; widens
    /// via covariance at [#ATOMIC_SUBJECT].
    private static final Parser<Subject.Pronoun> PRONOUN =
            PRONOUN_BASE.optionallyFollowedBy(SelectorParsers.THAT_CLAUSE, Subject.Pronoun::withThat);

    // ── Any target ─────────────────────────────────────────────────────

    private static final Parser<Subject.AnyTarget> ANY_TARGET_BASE = anyOf(
            phrase("any other target").thenReturn(((Subject.AnyTarget) Subject.anyTarget()).asOther()),
            phrase("any target").thenReturn((Subject.AnyTarget) Subject.anyTarget()));

    /// "any \[other\]? target \[that-clause\]?" — Needle Drop: "any
    /// target that was dealt damage this turn". Stays narrow; widens
    /// via covariance at [#ATOMIC_SUBJECT].
    private static final Parser<Subject.AnyTarget> ANY_TARGET =
            ANY_TARGET_BASE.optionallyFollowedBy(SelectorParsers.THAT_CLAUSE, Subject.AnyTarget::withThat);

    // ── Demonstrative: "that creature", "those cards", "the creature" ──

    /// Demonstratives that may start a sentence ("That spell's controller
    /// draws …", "Those creatures …"). Uses `phrase` templates to get
    /// title-or-lower matching; `.thenReturn(...)` normalizes the
    /// captured value to the lowercase form.
    private static final Parser<String> THAT_THOSE_THE = anyOf(
            phrase("That").thenReturn("that"),
            phrase("Those").thenReturn("those"),
            phrase("The").thenReturn("the"));

    private static final Parser<Subject> DEMONSTRATIVE = anyOf(
            // Non-type demonstratives ("that mana", "that damage",
            // "that much") used in replacement/reference phrases (Horizon
            // Stone: "that mana becomes colorless instead.").
            sequence(THAT_THOSE_THE, anyOf(word("mana"), word("damage"), word("amount")), Subject::demonstrative),
            // "the sacrificed \[creature|land|card|permanent|artifact|
            // enchantment|planeswalker\]" — back-reference to the just-
            // sacrificed object (Diamond Valley: "life equal to the
            // sacrificed creature's toughness."; Faith Healer: "life
            // equal to the sacrificed enchantment's mana value.").
            phrase("the sacrificed")
                    .then(anyOf(
                            word("creature"),
                            word("land"),
                            word("card"),
                            word("permanent"),
                            word("artifact"),
                            word("enchantment"),
                            word("planeswalker")))
                    .map(t -> Subject.demonstrative("the sacrificed", t)),
            sequence(THAT_THOSE_THE, TYPE_EXPRESSION, (det, type) -> Subject.demonstrative(det, type.toString())));

    // ── Possessive subject: "its controller", "its owner" ──────────────

    private static final Parser<String> CONTROLLER_OR_OWNER = anyOf(word("controller"), word("owner"));

    /// Possessive pronouns that may start a sentence ("Its controller …",
    /// "Their owner …", "Your opponent …") or appear mid-sentence. The
    /// `phrase("Its")` template handles title-or-lower matching;
    /// `.thenReturn(...)` normalizes the captured value to the lowercase form.
    private static final Parser<String> POSSESSIVE_PRONOUN = anyOf(
            phrase("Its").thenReturn("its"),
            phrase("Their").thenReturn("their"),
            phrase("Your").thenReturn("your"));

    static final Parser<Subject> POSSESSIVE = anyOf(
            // "each of [player-ref]'s opponent(s)" — distributor over
            // the referenced player's opponents (Heartwood Storyteller:
            // "each of that player's opponents may draw a card."). The
            // "each of" is a distributor; the underlying OpponentsOf
            // already names the plural set.
            sequence(
                    phrase("each of").then(PLAYER_REF).followedBy(string("'s")),
                    anyOf(word("opponents"), word("opponent")),
                    (ref, _) -> new Subject.OpponentsOf(Subject.player(ref))),
            // "[player-ref]'s opponent(s)" — opponents of a referenced
            // player without the "each of" distributor prefix. Must
            // precede the controller/owner arms so "that player's"
            // doesn't get partially consumed.
            sequence(
                    PLAYER_REF.followedBy(string("'s")),
                    anyOf(word("opponents"), word("opponent")),
                    (ref, _) -> new Subject.OpponentsOf(Subject.player(ref))),
            sequence(POSSESSIVE_PRONOUN, CONTROLLER_OR_OWNER, Subject::possessiveSubject),
            // "target <type>'s controller/owner" — the controller/owner
            // of a targeted permanent (Misleading Motes: "Target creature's
            // owner puts it …"). Separate arm from the demonstratives so
            // "target creature" alone still parses as a Select subject.
            sequence(
                    phrase("Target").then(TYPE_EXPRESSION).followedBy(string("'s")),
                    CONTROLLER_OR_OWNER,
                    (type, role) -> Subject.possessiveSubject("target " + type, role)),
            // "that spell's controller" / "that creature's owner" —
            // demonstrative possessive used by Vex: "That spell's controller
            // may draw a card."
            sequence(
                    THAT_THOSE_THE,
                    TYPE_EXPRESSION.followedBy(string("'s")),
                    CONTROLLER_OR_OWNER,
                    (det, type, role) -> Subject.possessiveSubject(det + " " + type, role)),
            // "this creature's owner" / "this card's controller" —
            // self-referential possessive (Cerulean Sphinx: "This creature's
            // owner shuffles it into their library.").
            sequence(
                    phrase("This")
                            .then(anyOf(
                                    word("creature"),
                                    word("card"),
                                    word("artifact"),
                                    word("enchantment"),
                                    word("permanent"),
                                    word("land")))
                            .followedBy(string("'s")),
                    CONTROLLER_OR_OWNER,
                    (type, role) -> Subject.possessiveSubject("this " + type, role)),
            // "The \[controller|owner\] of \[selector\]" — inverse-possessive
            // form (Oblation: "The owner of target nonland permanent
            // shuffles it into their library, then draws two cards.").
            // Same shape as the prefix forms above but with the
            // possessive role appearing before the referenced object.
            // Uses SELECTOR (not SUBJECT) to avoid the static-init cycle
            // that would arise from POSSESSIVE referring to SUBJECT.
            sequence(
                    phrase("The").then(CONTROLLER_OR_OWNER).followedBy(word("of")),
                    SelectorParsers.SELECTOR,
                    (role, sel) -> Subject.possessiveSubject(sel.toString(), role)));

    /// A player reference wrapped as a [Subject].
    public static final Parser<Subject> PLAYER_SUBJECT = PLAYER_REF.map(Subject::player);

    /// "[player-ref] [participial-clause]" — a player target narrowed
    /// by a resolution-history participle (Wicked Akuba: "Target
    /// player dealt damage by this creature this turn loses 1
    /// life."). Declared before PLAYER_LIKE_SUBJECT so it can feed
    /// both [#PLAYER_LIKE_SUBJECT] (for PLAYER_SUBJECTS-style effects
    /// like LoseLife) and [#ATOMIC_SUBJECT] (for generic subjects).
    private static final Parser<Subject> PLAYER_WITH_PARTICIPLE =
            sequence(PLAYER_REF, PARTICIPIAL_CLAUSE_RULE, Subject.PlayerWithParticiple::new);

    /// One or more player-like subjects joined by "and" — a plain player
    /// reference ([#PLAYER_SUBJECT]) or a possessive that resolves to
    /// a player ("its owner", "its controller", "this creature's owner").
    /// Used by effects whose actor is a player (Secret Rendezvous,
    /// Misfortune's Gain, Cerulean Sphinx).
    public static final Parser<Subject> PLAYER_LIKE_SUBJECT = anyOf(PLAYER_WITH_PARTICIPLE, PLAYER_SUBJECT, POSSESSIVE);

    /// One or more player subjects joined by "and" (e.g., Secret Rendezvous:
    /// "You and target opponent each draw three cards."). Multiple players
    /// collapse into a [Subject.Multiple].
    public static final Parser<Subject> PLAYER_SUBJECTS = PLAYER_LIKE_SUBJECT.optionallyFollowedBy(
            word("and").then(PLAYER_LIKE_SUBJECT), SubjectParsers::joinMultiple);

    /// "each of [count] targets" — bare form (e.g., Meteor Blast).
    /// "each of [count] target [type]" — typed form (e.g., Thrive: "each of
    /// X target creatures"). Count accepts a bare amount or the upper-bound
    /// form "up to N" (Gird for Battle: "each of up to two target
    /// creatures"). The type is captured as plural text for now.
    private static final Parser<Subject> EACH_OF_TARGETS = anyOf(
            sequence(
                    phrase("each of").then(anyOf(phrase("up to").then(AMOUNT), AMOUNT)),
                    anyOf(
                            word("targets").thenReturn((String) null),
                            word("target").then(CARD_TYPE).map(t -> t.name().toLowerCase() + "s")),
                    Subject.EachOfTargets::new),
            // "each of them" — distributes a previous target group (Hope
            // and Glory: "Untap two target creatures. Each of them gets
            // +1/+1 until end of turn.").
            phrase("Each of them").thenReturn(Subject.pronoun(PronounType.EACH_OF_THEM)),
            // "each of those <type> with <ability>" — distributes
            // over a typed subset of the previous target group (Winter
            // Blast: "Winter Blast deals 2 damage to each of those
            // creatures with flying."). Matches the demonstrative
            // back-ref pattern carrying a with-clause.
            sequence(
                    phrase("Each of those").then(TYPE_EXPRESSION).followedBy(phrase("with")),
                    word(),
                    (type, ability) -> Subject.demonstrative("each of those", type + " with " + ability)));

    // ── Combined subject ───────────────────────────────────────────────

    /// A single subject — one of the atomic forms, without "and" chaining.
    /// Order matters: more specific patterns first.
    static final Parser<Subject> ATOMIC_SUBJECT = anyOf(
            ANY_TARGET,
            SELF_REF,
            POSSESSIVE,
            ORDINAL_SPELL, // must precede DEMONSTRATIVE (both start with "the")
            ORDINAL_SPELL_OF_TURN, // must precede DEMONSTRATIVE (both start with "the")
            NEXT_SPELL, // must precede DEMONSTRATIVE (both start with "the")
            TOP_CARD_OF_LIBRARY, // must precede DEMONSTRATIVE (both start with "the")
            EACH_OF_TARGETS,
            DEMONSTRATIVE,
            PRONOUN,
            // Player with a trailing participial clause must precede the
            // bare PLAYER_SUBJECT so the clause isn't dropped.
            PLAYER_WITH_PARTICIPLE,
            PLAYER_SUBJECT,
            SELECTOR.map(Subject::select));

    /// Player-verb keywords that mark the start of a new subject-less
    /// effect body in an "and"-joined continuation (Essence Drain:
    /// "… and you gain 3 life."). Subject conjunction refuses to absorb
    /// the chained atom when it would leave a dangling verb on the other
    /// side — that "and" belongs to the enclosing effect sequence.
    private static final Parser<String> PLAYER_VERB_LOOKAHEAD = anyOf(
            phrase("gain(s)"),
            phrase("lose(s)"),
            phrase("draw(s)"),
            phrase("discard(s)"),
            phrase("reveal(s)"),
            phrase("mill(s)"),
            phrase("sacrifice(s)"),
            phrase("add(s)"),
            // Object-verb starters — if the chained atom is immediately
            // followed by one of these, the "and" belongs to the effect
            // sequence, not the subject conjunction (Suffocating Blast:
            // "Counter target spell and ~ deals 3 damage to target
            // creature.").
            phrase("deal(s)"),
            phrase("get(s)"));

    /// ATOMIC subject used inside an "and"/"or" chain — rejects a bare
    /// player followed by a player-verb so the "and" stays available as
    /// an effect-sequence delimiter.
    private static final Parser<Subject> CHAINED_ATOMIC_SUBJECT =
            ATOMIC_SUBJECT.notFollowedBy(PLAYER_VERB_LOOKAHEAD, "player verb");

    /// "or"-tail of an Oxford-comma list: parses either ", X, ..., or
    /// Y" (Oxford form, two or more middle terms), "or Y" (non-
    /// Oxford 2-element), or "and/or Y" (Mass Manipulation /
    /// "artifact creatures and/or red creatures" — semantically the
    /// same disjunction shape). Yields the list of alternatives that
    /// follow the leading subject (the leading subject is supplied
    /// by [#joinOneOfList]).
    private static final Parser<List<Subject>> OXFORD_OR_TAIL = anyOf(
            // Oxford form: ", X" repeated, then ", or Y"
            sequence(
                    string(",").then(CHAINED_ATOMIC_SUBJECT).atLeastOnce(),
                    string(",").then(word("or")).then(CHAINED_ATOMIC_SUBJECT),
                    (mids, last) -> {
                        var l = new ArrayList<>(mids);
                        l.add(last);
                        return List.copyOf(l);
                    }),
            // Non-Oxford 2-element: "or Y" / "and/or Y"
            anyOf(string("and/or"), word("or")).then(CHAINED_ATOMIC_SUBJECT).map(List::of));

    /// A subject, possibly a conjunction of multiple atomic subjects.
    /// "and" produces [Subject.Multiple] (all targets); "or" produces
    /// [Subject.OneOf] (one target matching any alternative; supports
    /// Oxford-comma lists for 3+ branches like "Aura, Equipment, or
    /// creature").
    public static final Parser<Subject> SUBJECT = ATOMIC_SUBJECT
            .optionallyFollowedBy(word("and").then(CHAINED_ATOMIC_SUBJECT), SubjectParsers::joinMultiple)
            .optionallyFollowedBy(OXFORD_OR_TAIL, SubjectParsers::joinOneOfList);

    private static Subject joinMultiple(Subject first, Subject next) {
        if (first instanceof Subject.Multiple existing) {
            var parts = new ArrayList<>(existing.parts());
            parts.add(next);
            return new Subject.Multiple(List.copyOf(parts));
        }
        return new Subject.Multiple(List.of(first, next));
    }

    private static Subject joinOneOfList(Subject first, List<Subject> rest) {
        var alts = new ArrayList<Subject>(rest.size() + 1);
        if (first instanceof Subject.OneOf existing) {
            alts.addAll(existing.alternatives());
        } else {
            alts.add(first);
        }
        alts.addAll(rest);
        return new Subject.OneOf(List.copyOf(alts));
    }
}
