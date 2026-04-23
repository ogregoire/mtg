package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.AMOUNT;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.COUNTER_TYPE;
import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle.domain.Amount;
import be.imgn.mtg.engine.oracle.domain.Subject;
import be.imgn.mtg.engine.oracle.domain.Zone;
import be.imgn.mtg.engine.oracle.domain.ZoneName;

/// Count-of and property-of amount expressions. Produces [Amount] values
/// consumed by almost every leaf effect parser that scales output by a
/// scope (`for each …`) or references an object property (`equal to
/// target creature's power`).
final class CountOfParsers {
    private CountOfParsers() {}

    /// Trailing "on the battlefield" zone scope — common in count-of phrases
    /// like "for each Goblin on the battlefield".
    private static final Parser<Zone.Named> ON_BATTLEFIELD =
            phrase("on the battlefield").thenReturn(new Zone.Named(null, ZoneName.BATTLEFIELD));

    /// "for each [subject] [in zone | on the battlefield]" — a count-of
    /// expression. Produces an [Amount.CountOf] equal to the number of
    /// matching objects. The "of [poss] [property]" alternative (Civic Saber:
    /// "for each of its colors") counts values of a named characteristic of
    /// a referenced object; modelled as a count-of over a
    /// [Subject.PossessiveSubject] holding that property.
    static final Parser<Amount.CountOf> FOR_EACH = phrase("for each")
            .then(Parser.anyOf(
                    sequence(
                                    word("of").then(anyOf(word("its"), word("their"), word("your"))),
                                    anyOf(word("colors"), word("types"), word("subtypes"), word("supertypes")),
                                    Subject::possessiveSubject)
                            .map(Amount.CountOf::new),
                    // "different <property> among <selector>" — count of
                    // distinct property values in the referenced set
                    // (Golden Ratio: "for each different power among
                    // creatures you control."). Captured as a possessive-
                    // style amalgam subject so downstream code can see the
                    // property name ("different <prop>") alongside the
                    // scope selector.
                    Parser.sequence(
                            phrase("different")
                                    .then(anyOf(
                                            word("power"),
                                            word("toughness"),
                                            word("strength"),
                                            phrase("life total"),
                                            phrase("mana value")))
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
                            phrase("basic land type among"),
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
                            COUNTER_TYPE.followedBy(phrase("counter(s)")),
                            SubjectParsers.PLAYER_LIKE_SUBJECT.followedBy(phrase("[has|have]")),
                            (type, owner) -> new Amount.CountOf(
                                    Subject.possessiveSubject(owner.toString(), type + " counters"), null)),
                    SubjectParsers.SUBJECT.map(Amount.CountOf::new)));

    /// A property name in a property-of expression.
    private static final Parser<String> PROPERTY_NAME = anyOf(
            word("power"),
            word("toughness"),
            word("strength"),
            phrase("life total"),
            phrase("mana value"),
            phrase("converted mana cost"));

    /// Possessive pronouns ("your", "their", "its") mapped to a
    /// [Subject] — used as the owner of a property without the
    /// intervening `'s` (e.g., "your life total").
    private static final Parser<Subject> POSSESSIVE_OWNER = anyOf(
            phrase("your").thenReturn(Subject.player(Subject.PlayerRef.YOU)),
            phrase("their").thenReturn(Subject.player(Subject.PlayerRef.THEY)),
            phrase("its").thenReturn(Subject.pronoun("it")));

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
            // "the number of card types among <selector>" — count of
            // distinct card types found across a set of cards (Lucid
            // Dreams: "the number of card types among cards in your
            // graveyard."). Modelled as an Extremum-like amalgam via
            // a possessive-subject carrying the scope.
            sequence(phrase("the number of card types among"), SubjectParsers.SUBJECT, (_, scope) ->
                    (Amount) new Amount.CountOf(Subject.possessiveSubject("card types among", scope.toString()), null)),
            phrase("the number of").then(SubjectParsers.SUBJECT).<Amount>map(Amount.CountOf::new),
            sequence(POSSESSIVE_OWNER, PROPERTY_NAME, Amount.PropertyOf::new),
            sequence(SubjectParsers.SUBJECT.followedBy(string("'s")), PROPERTY_NAME, Amount.PropertyOf::new),
            sequence(
                    word("the").then(PROPERTY_NAME).followedBy(word("of")),
                    SubjectParsers.SUBJECT,
                    (prop, subj) -> new Amount.PropertyOf(subj, prop)));

    /// ", where X is <amount>" — defines the X used by an effect whose
    /// count is [Amount#variable()]. Consumes the leading comma so it
    /// can be chained as an `optionallyFollowedBy`. Used by effects
    /// whose count is variable (MODIFY_PT for Death's Shadow-style P/T;
    /// MILL for Dreadwaters; ADD_COUNTERS; ADD_MANA).
    public static final Parser<Amount> WHERE_X_IS =
            string(",").then(phrase("where X is")).then(anyOf(PROPERTY_OF_AMOUNT, AMOUNT));

    /// Optional trailing "\[, rounded up\|down\]" suffix on a half
    /// amount. Returns the [Amount.Half.Rounding] enum so callers can
    /// fold it via `halfParser.optionallyFollowedBy(ROUNDING_DIRECTION,
    /// Amount.Half::withRounding)`. Used by both "half your life" and
    /// "half their library" count-of expressions.
    public static final Parser<Amount.Half.Rounding> ROUNDING_DIRECTION = string(",")
            .then(phrase("rounded"))
            .then(anyOf(
                    word("up").thenReturn(Amount.Half.Rounding.UP),
                    word("down").thenReturn(Amount.Half.Rounding.DOWN)));
}
