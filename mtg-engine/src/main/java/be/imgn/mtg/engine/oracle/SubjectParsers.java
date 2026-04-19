package be.imgn.mtg.engine.oracle;

import static be.imgn.mtg.engine.oracle.Words.anyCiWord;
import static be.imgn.mtg.engine.oracle.Words.ciWords;
import static be.imgn.mtg.engine.oracle.Words.w;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;

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
            ciWords("each player").thenReturn(Subject.PlayerRef.EACH_PLAYER),
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

    // ── Pronouns ───────────────────────────────────────────────────────

    private static final Parser<Subject> PRONOUN = anyOf(
            // Multi-word pronouns first so longer matches win.
            ciWords("the rest").thenReturn(Subject.pronoun("the rest")),
            w("it").thenReturn(Subject.pronoun("it")),
            w("them").thenReturn(Subject.pronoun("them")));

    // ── Any target ─────────────────────────────────────────────────────

    private static final Parser<Subject> ANY_TARGET = ciWords("any target").thenReturn(Subject.anyTarget());

    // ── Demonstrative: "that creature", "those cards", "the creature" ──

    private static final Parser<Subject> DEMONSTRATIVE = sequence(
            anyCiWord("that", "those", "the"),
            SelectorParsers.TYPE_EXPRESSION,
            (det, type) -> Subject.demonstrative(det, type.toString()));

    // ── Possessive subject: "its controller", "its owner" ──────────────

    static final Parser<Subject> POSSESSIVE =
            sequence(anyCiWord("its", "their", "your"), anyCiWord("controller", "owner"), Subject::possessiveSubject);

    /// A player reference wrapped as a {@link Subject}.
    public static final Parser<Subject> PLAYER_SUBJECT = PLAYER_REF.map(Subject::player);

    /// One or more player-like subjects joined by "and" — a plain player
    /// reference ({@link #PLAYER_SUBJECT}) or a possessive that resolves to
    /// a player ("its owner", "its controller"). Used by effects whose actor
    /// is a player (Secret Rendezvous, Misfortune's Gain).
    private static final Parser<Subject> PLAYER_LIKE_SUBJECT = anyOf(PLAYER_SUBJECT, POSSESSIVE);

    /// One or more player subjects joined by "and" (e.g., Secret Rendezvous:
    /// "You and target opponent each draw three cards."). Multiple players
    /// collapse into a {@link Subject.Multiple}.
    public static final Parser<Subject> PLAYER_SUBJECTS =
            PLAYER_LIKE_SUBJECT.optionallyFollowedBy(w("and").then(PLAYER_LIKE_SUBJECT), SubjectParsers::joinMultiple);

    /// "each of [count] targets" — bare form (e.g., Meteor Blast).
    /// "each of [count] target [type]" — typed form (e.g., Thrive: "each of
    /// X target creatures"). The type is captured as plural text for now.
    private static final Parser<Subject> EACH_OF_TARGETS = sequence(
            ciWords("each of").then(SelectorParsers.AMOUNT),
            anyOf(
                    w("targets").thenReturn((String) null),
                    w("target")
                            .then(SelectorParsers.CARD_TYPE)
                            .map(t -> t.name().toLowerCase() + "s")),
            Subject.EachOfTargets::new);

    // ── Combined subject ───────────────────────────────────────────────

    /// A single subject — one of the atomic forms, without "and" chaining.
    /// Order matters: more specific patterns first.
    private static final Parser<Subject> ATOMIC_SUBJECT = anyOf(
            ANY_TARGET,
            SELF_REF,
            POSSESSIVE,
            EACH_OF_TARGETS,
            DEMONSTRATIVE,
            PRONOUN,
            PLAYER_SUBJECT,
            SelectorParsers.SELECTOR.map(Subject::select));

    /// A subject, possibly a conjunction of multiple atomic subjects.
    /// "and" produces {@link Subject.Multiple} (all targets); "or" produces
    /// {@link Subject.OneOf} (one target matching any alternative).
    public static final Parser<Subject> SUBJECT = ATOMIC_SUBJECT
            .optionallyFollowedBy(w("and").then(ATOMIC_SUBJECT), SubjectParsers::joinMultiple)
            .optionallyFollowedBy(w("or").then(ATOMIC_SUBJECT), SubjectParsers::joinOneOf);

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
