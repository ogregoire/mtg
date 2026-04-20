package be.imgn.mtg.engine.oracle;

import static be.imgn.mtg.engine.oracle.Words.anyCiWord;
import static be.imgn.mtg.engine.oracle.Words.anyWord;
import static be.imgn.mtg.engine.oracle.Words.ciWords;
import static be.imgn.mtg.engine.oracle.Words.phrase;
import static be.imgn.mtg.engine.oracle.Words.w;
import static be.imgn.mtg.engine.oracle.Words.words;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.ArrayList;
import java.util.List;

import com.google.common.labs.parse.Parser;

/// Parsers for subjects and player references in oracle text.
final class SubjectParsers {
    private SubjectParsers() {}

    // ── Player references ──────────────────────────────────────────────

    public static final Parser<Subject.PlayerRef> PLAYER_REF = anyOf(
            ciWords("target opponent").thenReturn(Subject.PlayerRef.TARGET_OPPONENT),
            ciWords("any number of target players").thenReturn(Subject.PlayerRef.TARGET_PLAYER),
            ciWords("two target players").thenReturn(Subject.PlayerRef.TARGET_PLAYER),
            ciWords("target players").thenReturn(Subject.PlayerRef.TARGET_PLAYER),
            ciWords("target player").thenReturn(Subject.PlayerRef.TARGET_PLAYER),
            ciWords("each opponent").thenReturn(Subject.PlayerRef.EACH_OPPONENT),
            // "each other player" — includes teammates; must precede
            // "each player" so the longer match wins.
            ciWords("each other player").thenReturn(Subject.PlayerRef.EACH_OTHER_PLAYER),
            ciWords("each player").thenReturn(Subject.PlayerRef.EACH_PLAYER),
            // "a player" / "an opponent" — existential, typically a
            // trigger subject. Must precede "each" or similar to avoid
            // ambiguity at longer matches.
            ciWords("a player").thenReturn(Subject.PlayerRef.A_PLAYER),
            // "any player" — treated as the existential "a player" since
            // it functions identically in oracle text (Quick Sliver:
            // "Any player may cast Sliver spells …").
            ciWords("any player").thenReturn(Subject.PlayerRef.A_PLAYER),
            ciWords("an opponent").thenReturn(Subject.PlayerRef.AN_OPPONENT),
            ciWords("that player").thenReturn(Subject.PlayerRef.THAT_PLAYER),
            ciWords("defending player").thenReturn(Subject.PlayerRef.DEFENDING_PLAYER),
            ciWords("your opponents").thenReturn(Subject.PlayerRef.YOUR_OPPONENTS),
            // Bare plural "Players" at sentence start = "each player"
            // (e.g., "Players can't cycle cards.").
            w("players").thenReturn(Subject.PlayerRef.EACH_PLAYER),
            w("you").thenReturn(Subject.PlayerRef.YOU),
            w("they").thenReturn(Subject.PlayerRef.THEY));

    // ── Self reference ─────────────────────────────────────────────────

    private static final Parser<Subject> SELF_REF = anyOf(
            string("~").thenReturn(Subject.selfRef(null)),
            w("this")
                    .then(anyOf(
                            SelectorParsers.CARD_TYPE.map(
                                    ct -> Subject.selfRef(ct.name().toLowerCase())),
                            SelectorParsers.GAME_OBJECT_TYPE.map(
                                    got -> Subject.selfRef(got.name().toLowerCase())))));

    // ── Ordinal spell reference ───────────────────────────────────────

    /// "The [first|second|third|fourth] spell you cast each turn" —
    /// positional spell reference used mainly by cost-modifying effects
    /// (Uthros Psionicist). Represented as a
    /// [Subject.PossessiveSubject] with role "spell you cast each
    /// turn" and the ordinal embedded in the possessive string.
    private static final Parser<Integer> SPELL_ORDINAL = anyOf(
            w("first").thenReturn(1),
            w("second").thenReturn(2),
            w("third").thenReturn(3),
            w("fourth").thenReturn(4));

    private static final Parser<Subject> ORDINAL_SPELL = ciWords("the")
            .then(SPELL_ORDINAL)
            .followedBy(phrase("spell(s) you cast each turn"))
            .map(n -> Subject.possessiveSubject("the " + n, "spell you cast each turn"));

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
            anyCiWord("your", "their", "its"));

    private static final Parser<String> TOP_ZONE_NAME = anyWord("library", "graveyard");

    private static final Parser<Subject> TOP_CARD_OF_LIBRARY = anyOf(
            sequence(
                    ciWords("the top card of").then(LIBRARY_OWNER),
                    TOP_ZONE_NAME,
                    (poss, zone) -> Subject.possessiveSubject(poss, "top card of " + zone)),
            // "the top N cards of [owner]'s [zone]" — Orcish Spy.
            sequence(
                    ciWords("the top").then(SelectorParsers.WORD_NUMBER),
                    sequence(
                            phrase("card(s)").then(word("of")).then(LIBRARY_OWNER),
                            TOP_ZONE_NAME,
                            (poss, zone) -> poss + "|" + zone),
                    (n, combo) -> {
                        var parts = combo.split("\\|", 2);
                        return Subject.possessiveSubject(parts[0], "top " + n + " cards of " + parts[1]);
                    }));

    // ── Pronouns ───────────────────────────────────────────────────────

    private static final Parser<Subject> PRONOUN = anyOf(
            // Multi-word pronouns first so longer matches win.
            ciWords("the rest").thenReturn(Subject.pronoun("the rest")),
            // Reflexive self-reference (e.g., Solar Blaze: "Each creature
            // deals damage to itself equal to its power.").
            w("itself").thenReturn(Subject.pronoun("itself")),
            w("it").thenReturn(Subject.pronoun("it")),
            w("them").thenReturn(Subject.pronoun("them")));

    // ── Any target ─────────────────────────────────────────────────────

    private static final Parser<Subject> ANY_TARGET = anyOf(
            phrase("any other target").thenReturn(((Subject.AnyTarget) Subject.anyTarget()).asOther()),
            phrase("any target").thenReturn(Subject.anyTarget()));

    // ── Demonstrative: "that creature", "those cards", "the creature" ──

    private static final Parser<Subject> DEMONSTRATIVE = anyOf(
            // Non-type demonstratives ("that mana", "that damage",
            // "that much") used in replacement/reference phrases (Horizon
            // Stone: "that mana becomes colorless instead.").
            sequence(anyCiWord("that", "those", "the"), anyCiWord("mana", "damage", "amount"), Subject::demonstrative),
            sequence(
                    anyCiWord("that", "those", "the"),
                    SelectorParsers.TYPE_EXPRESSION,
                    (det, type) -> Subject.demonstrative(det, type.toString())));

    // ── Possessive subject: "its controller", "its owner" ──────────────

    static final Parser<Subject> POSSESSIVE = anyOf(
            sequence(anyCiWord("its", "their", "your"), anyCiWord("controller", "owner"), Subject::possessiveSubject),
            // "that spell's controller" / "that creature's owner" —
            // demonstrative possessive used by Vex: "That spell's controller
            // may draw a card."
            sequence(
                    anyCiWord("that", "those", "the"),
                    SelectorParsers.TYPE_EXPRESSION.followedBy(string("'s")),
                    anyCiWord("controller", "owner"),
                    (det, type, role) -> Subject.possessiveSubject(det + " " + type, role)),
            // "this creature's owner" / "this card's controller" —
            // self-referential possessive (Cerulean Sphinx: "This creature's
            // owner shuffles it into their library.").
            sequence(
                    ciWords("this")
                            .then(anyWord("creature", "card", "artifact", "enchantment", "permanent", "land"))
                            .followedBy(string("'s")),
                    anyWord("controller", "owner"),
                    (type, role) -> Subject.possessiveSubject("this " + type, role)));

    /// A player reference wrapped as a [Subject].
    public static final Parser<Subject> PLAYER_SUBJECT = PLAYER_REF.map(Subject::player);

    /// One or more player-like subjects joined by "and" — a plain player
    /// reference ([#PLAYER_SUBJECT]) or a possessive that resolves to
    /// a player ("its owner", "its controller", "this creature's owner").
    /// Used by effects whose actor is a player (Secret Rendezvous,
    /// Misfortune's Gain, Cerulean Sphinx).
    public static final Parser<Subject> PLAYER_LIKE_SUBJECT = anyOf(PLAYER_SUBJECT, POSSESSIVE);

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
                    ciWords("each of").then(anyOf(words("up to").then(SelectorParsers.AMOUNT), SelectorParsers.AMOUNT)),
                    anyOf(
                            word("targets").thenReturn((String) null),
                            word("target")
                                    .then(SelectorParsers.CARD_TYPE)
                                    .map(t -> t.name().toLowerCase() + "s")),
                    Subject.EachOfTargets::new),
            // "each of them" — distributes a previous target group (Hope
            // and Glory: "Untap two target creatures. Each of them gets
            // +1/+1 until end of turn.").
            ciWords("each of them").thenReturn(Subject.pronoun("each of them")));

    // ── Combined subject ───────────────────────────────────────────────

    /// A single subject — one of the atomic forms, without "and" chaining.
    /// Order matters: more specific patterns first.
    static final Parser<Subject> ATOMIC_SUBJECT = anyOf(
            ANY_TARGET,
            SELF_REF,
            POSSESSIVE,
            ORDINAL_SPELL, // must precede DEMONSTRATIVE (both start with "the")
            TOP_CARD_OF_LIBRARY, // must precede DEMONSTRATIVE (both start with "the")
            EACH_OF_TARGETS,
            DEMONSTRATIVE,
            PRONOUN,
            PLAYER_SUBJECT,
            SelectorParsers.SELECTOR.map(Subject::select));

    /// Player-verb keywords that mark the start of a new subject-less
    /// effect body in an "and"-joined continuation (Essence Drain:
    /// "… and you gain 3 life."). Subject conjunction refuses to absorb
    /// the chained atom when it would leave a dangling verb on the other
    /// side — that "and" belongs to the enclosing effect sequence.
    private static final Parser<String> PLAYER_VERB_LOOKAHEAD = anyCiWord(
            "gain", "gains",
            "lose", "loses",
            "draw", "draws",
            "discard", "discards",
            "reveal", "reveals",
            "mill", "mills",
            "sacrifice", "sacrifices",
            "add", "adds");

    /// ATOMIC subject used inside an "and"/"or" chain — rejects a bare
    /// player followed by a player-verb so the "and" stays available as
    /// an effect-sequence delimiter.
    private static final Parser<Subject> CHAINED_ATOMIC_SUBJECT =
            ATOMIC_SUBJECT.notFollowedBy(PLAYER_VERB_LOOKAHEAD, "player verb");

    /// A subject, possibly a conjunction of multiple atomic subjects.
    /// "and" produces [Subject.Multiple] (all targets); "or" produces
    /// [Subject.OneOf] (one target matching any alternative).
    public static final Parser<Subject> SUBJECT = ATOMIC_SUBJECT
            .optionallyFollowedBy(word("and").then(CHAINED_ATOMIC_SUBJECT), SubjectParsers::joinMultiple)
            .optionallyFollowedBy(word("or").then(CHAINED_ATOMIC_SUBJECT), SubjectParsers::joinOneOf);

    private static Subject joinMultiple(Subject first, Subject next) {
        if (first instanceof Subject.Multiple existing) {
            var parts = new ArrayList<>(existing.parts());
            parts.add(next);
            return new Subject.Multiple(List.copyOf(parts));
        }
        return new Subject.Multiple(List.of(first, next));
    }

    private static Subject joinOneOf(Subject first, Subject next) {
        if (first instanceof Subject.OneOf existing) {
            var alts = new ArrayList<>(existing.alternatives());
            alts.add(next);
            return new Subject.OneOf(List.copyOf(alts));
        }
        return new Subject.OneOf(List.of(first, next));
    }
}
