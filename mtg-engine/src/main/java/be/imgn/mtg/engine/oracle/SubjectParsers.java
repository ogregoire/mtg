package be.imgn.mtg.engine.oracle;

import static be.imgn.mtg.engine.oracle.Words.anyCiWord;
import static be.imgn.mtg.engine.oracle.Words.ciWords;
import static be.imgn.mtg.engine.oracle.Words.w;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;

import com.google.common.labs.parse.Parser;

/// Parsers for subjects and player references in oracle text.
final class SubjectParsers {
    private SubjectParsers() {}

    // ── Player references ──────────────────────────────────────────────

    public static final Parser<Subject.PlayerRef> PLAYER_REF = anyOf(
            ciWords("target opponent").thenReturn(Subject.PlayerRef.targetOpponent()),
            ciWords("target player").thenReturn(Subject.PlayerRef.targetPlayer()),
            ciWords("each opponent").thenReturn(Subject.PlayerRef.eachOpponent()),
            ciWords("each player").thenReturn(Subject.PlayerRef.eachPlayer()),
            ciWords("that player").thenReturn(Subject.PlayerRef.thatPlayer()),
            ciWords("defending player").thenReturn(Subject.PlayerRef.defendingPlayer()),
            w("you").thenReturn(Subject.PlayerRef.you()),
            w("they").thenReturn(Subject.PlayerRef.they()));

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

    private static final Parser<Subject> PRONOUN =
            anyOf(w("it").thenReturn(Subject.pronoun("it")), w("them").thenReturn(Subject.pronoun("them")));

    // ── Any target ─────────────────────────────────────────────────────

    private static final Parser<Subject> ANY_TARGET = ciWords("any target").thenReturn(Subject.anyTarget());

    // ── Demonstrative: "that creature", "those cards", "the creature" ──

    private static final Parser<Subject> DEMONSTRATIVE = sequence(
            anyCiWord("that", "those", "the"),
            SelectorParsers.TYPE_EXPRESSION,
            (det, type) -> Subject.demonstrative(det, type.toString()));

    // ── Possessive subject: "its controller", "its owner" ──────────────

    private static final Parser<Subject> POSSESSIVE =
            sequence(anyCiWord("its", "their", "your"), anyCiWord("controller", "owner"), Subject::possessiveSubject);

    /// A player reference wrapped as a {@link Subject}.
    public static final Parser<Subject> PLAYER_SUBJECT = PLAYER_REF.map(Subject::player);

    // ── Combined subject ───────────────────────────────────────────────

    /// The master subject parser. Order matters: more specific patterns first.
    public static final Parser<Subject> SUBJECT = anyOf(
            ANY_TARGET,
            SELF_REF,
            POSSESSIVE,
            DEMONSTRATIVE,
            PRONOUN,
            PLAYER_SUBJECT,
            SelectorParsers.SELECTOR.map(Subject::select));
}
