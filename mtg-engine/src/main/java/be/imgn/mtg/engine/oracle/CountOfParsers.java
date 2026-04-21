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

import com.google.common.labs.parse.Parser;

/// Count-of and property-of amount expressions. Produces [Amount] values
/// consumed by almost every leaf effect parser that scales output by a
/// scope (`for each …`) or references an object property (`equal to
/// target creature's power`).
final class CountOfParsers {
    private CountOfParsers() {}

    /// Trailing "on the battlefield" zone scope — common in count-of phrases
    /// like "for each Goblin on the battlefield".
    private static final Parser<Zone.Named> ON_BATTLEFIELD =
            ciWords("on the battlefield").thenReturn(new Zone.Named(null, ZoneName.BATTLEFIELD));

    /// "for each [subject] [in zone | on the battlefield]" — a count-of
    /// expression. Produces an [Amount.CountOf] equal to the number of
    /// matching objects. The "of [poss] [property]" alternative (Civic Saber:
    /// "for each of its colors") counts values of a named characteristic of
    /// a referenced object; modelled as a count-of over a
    /// [Subject.PossessiveSubject] holding that property.
    static final Parser<Amount.CountOf> FOR_EACH = ciWords("for each")
            .then(anyOf(
                    sequence(
                                    word("of").then(anyWord("its", "their", "your")),
                                    anyWord("colors", "types", "subtypes", "supertypes"),
                                    Subject::possessiveSubject)
                            .map(Amount.CountOf::new),
                    // "different <property> among <selector>" — count of
                    // distinct property values in the referenced set
                    // (Golden Ratio: "for each different power among
                    // creatures you control."). Captured as a possessive-
                    // style amalgam subject so downstream code can see the
                    // property name ("different <prop>") alongside the
                    // scope selector.
                    sequence(
                            w("different")
                                    .then(anyOf(
                                            anyWord("power", "toughness", "strength"),
                                            words("life total"),
                                            words("mana value")))
                                    .followedBy(word("among")),
                            SubjectParsers.SUBJECT,
                            (prop, scope) -> new Amount.CountOf(
                                    Subject.possessiveSubject("different " + prop + " among", scope.toString()), null)),
                    // "basic land type among <selector>" — Domain
                    // counter (Wandering Stream: "You gain 2 life for
                    // each basic land type among lands you control.").
                    // Rule 702.71. Captured as a CountOf whose
                    // possessive carries the property name and scope.
                    sequence(
                            words("basic land type among"),
                            SubjectParsers.SUBJECT,
                            (_, scope) -> new Amount.CountOf(
                                    Subject.possessiveSubject("basic land types among", scope.toString()), null)),
                    sequence(SubjectParsers.SUBJECT, ZoneExpressionParsers.IN_ZONE, Amount.CountOf::new),
                    sequence(SubjectParsers.SUBJECT, ON_BATTLEFIELD, Amount.CountOf::new),
                    // "for each [type] counter [poss] has/have" — count of
                    // a specific counter kind across a player's
                    // permanents (Mycosynth Fiend: "for each poison
                    // counter your opponents have."). Modelled as a
                    // possessive-style amalgam subject capturing the
                    // counter name and the holder.
                    sequence(
                            SelectorParsers.COUNTER_TYPE.followedBy(phrase("counter(s)")),
                            SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("[has|have]")),
                            (type, owner) -> new Amount.CountOf(
                                    Subject.possessiveSubject(owner.toString(), type + " counters"), null)),
                    SubjectParsers.SUBJECT.map(Amount.CountOf::new)));

    /// A property name in a property-of expression.
    private static final Parser<String> PROPERTY_NAME = anyOf(
            anyCiWord("power", "toughness", "strength"),
            ciWords("life total"),
            ciWords("mana value"),
            ciWords("converted mana cost"));

    /// Possessive pronouns ("your", "their", "its") mapped to a
    /// [Subject] — used as the owner of a property without the
    /// intervening `'s` (e.g., "your life total").
    private static final Parser<Subject> POSSESSIVE_OWNER = anyOf(
            ciWords("your").thenReturn(Subject.player(Subject.PlayerRef.YOU)),
            ciWords("their").thenReturn(Subject.player(Subject.PlayerRef.THEY)),
            ciWords("its").thenReturn(Subject.pronoun("it")));

    /// "the \[greatest|lowest\] [property] among [subject]" — extremum of
    /// a property across a subject group (One with the Machine: "the
    /// greatest mana value among artifacts you control"; Repay in Kind:
    /// "the lowest life total among all players").
    private static final Parser<Amount.Extremum.Kind> EXTREMUM_KIND = anyOf(
            word("greatest").thenReturn(Amount.Extremum.Kind.GREATEST),
            word("lowest").thenReturn(Amount.Extremum.Kind.LOWEST));

    /// "[owner] [property]" / "[owner]'s [property]" / "the [property]
    /// of [owner]" / "the number of [subject]" / "the
    /// \[greatest|lowest\] [property] among [subject]" — a property,
    /// count-of, or extremum reference as an amount. Covers "target
    /// creature's power", "your life total", "the power of target
    /// creature you control" (Soul's Majesty), "the number of Swamps
    /// you control" (Sima Yi), and "the greatest mana value among
    /// artifacts you control" (One with the Machine).
    static final Parser<Amount> PROPERTY_OF_AMOUNT = anyOf(
            sequence(
                    word("the").then(EXTREMUM_KIND),
                    PROPERTY_NAME.followedBy(word("among")),
                    SubjectParsers.SUBJECT,
                    (kind, prop, subj) -> (Amount) new Amount.Extremum(kind, prop, subj)),
            ciWords("the number of").then(SubjectParsers.SUBJECT).<Amount>map(Amount.CountOf::new),
            sequence(POSSESSIVE_OWNER, PROPERTY_NAME, Amount.PropertyOf::new),
            sequence(SubjectParsers.SUBJECT.followedBy(string("'s")), PROPERTY_NAME, Amount.PropertyOf::new),
            sequence(
                    word("the").then(PROPERTY_NAME).followedBy(word("of")),
                    SubjectParsers.SUBJECT,
                    (prop, subj) -> new Amount.PropertyOf(subj, prop)));
}
