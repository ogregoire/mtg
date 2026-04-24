package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.EffectParsers.BASE_EFFECT;
import static be.imgn.mtg.engine.oracle.parser.EffectParsers.IF_CONDITION;
import static be.imgn.mtg.engine.oracle.parser.EffectParsers.IF_PREFIX_CONDITION;
import static be.imgn.mtg.engine.oracle.parser.EffectParsers.MAY;
import static be.imgn.mtg.engine.oracle.parser.EffectParsers.UNLESS_CONDITION;
import static be.imgn.mtg.engine.oracle.parser.EffectParsers.WORD_OR_CONTRACTION;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.AMOUNT;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.SELECTOR;
import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle.domain.Effect;
import be.imgn.mtg.engine.oracle.domain.Property;
import be.imgn.mtg.engine.oracle.domain.Subject;

/// Leaf-effect parsers for replacement effects (rule 614) and
/// per-object loops: REPLACE, REPLACE_NEXT_TIME, REPLACE_MANA_DOUBLE,
/// REPLACE_LIFE_FLOOR, FOR_EACH_EFFECT, FOR_EACH_AMONG_EFFECT,
/// FOR_EACH_PLAYER_EFFECT, CONDITIONAL_OVERRIDE. Extracted from
/// [EffectParsers] to keep that file under the per-file soft limit.
final class ReplacementEffectParsers {
    private ReplacementEffectParsers() {}

    /// Words that can't appear in an [#REPLACE] event capture —
    /// they mark the start of the replacement clause and must stay
    /// available for the outer parser.
    private static final Set<String> REPLACE_EVENT_STOP_WORDS = Set.of("instead", "may", "you", "they");

    /// A single word admitted inside a replacement-event capture.
    /// [#REPLACE_EVENT_STOP_WORDS] are rejected so the event list
    /// doesn't swallow the replacement clause's leading keyword
    /// (Obstinate Familiar: "If you would draw a card, you may skip
    /// that draw instead." — "may" marks the replacement boundary).
    private static final Parser<String> REPLACE_EVENT_WORD = WORD_OR_CONTRACTION.suchThat(
            w -> !REPLACE_EVENT_STOP_WORDS.contains(w.toLowerCase()), "non-replacement-starter event word");

    /// Event capture for [#REPLACE]. Parses one or more comma-separated
    /// word-runs and rejoins them with commas so Oxford-comma lists
    /// like "Clue, Food, or Treasure token" (Academy Manufactor)
    /// round-trip. The delimiter form naturally leaves the final ","
    /// before "instead" for the outer parser to consume as the
    /// event/replacement boundary.
    private static final Parser<String> REPLACE_EVENT = REPLACE_EVENT_WORD
            .atLeastOnce()
            .map(ws -> String.join(" ", ws))
            .atLeastOnceDelimitedBy(",", Collectors.joining(", "));

    /// "If \[subject\] would \[event\], \[replacement\] instead." —
    /// replacement effect (rule 614, e.g., Thought Reflection: "If
    /// you would draw a card, draw two cards instead."; Academy
    /// Manufactor: "If you would create a Clue, Food, or Treasure
    /// token, instead create one of each."). Must precede the
    /// generic if-prefix conditional so the "instead" suffix is
    /// honored.
    static final Parser<Effect.Replace> REPLACE = sequence(
            phrase("If").then(SubjectParsers.SUBJECT),
            word("would").then(REPLACE_EVENT).followedBy(string(",")),
            anyOf(
                    Parser.<Effect>anyOf(MAY, BASE_EFFECT).followedBy(word("instead")),
                    word("instead").then(Parser.<Effect>anyOf(MAY, BASE_EFFECT))),
            Effect.Replace::new);

    /// "If you tap a permanent for mana, it produces twice as much of
    /// that mana instead." — mana-doubling replacement (Mana
    /// Reflection).
    static final Parser<Effect.Replace> REPLACE_MANA_DOUBLE = phrase(
                    "If you tap a permanent for mana, it produces twice as much of that mana instead")
            .thenReturn(new Effect.Replace(
                    Subject.player(Subject.PlayerRef.YOU),
                    "tap a permanent for mana",
                    new Effect.DoubleManaProduced()));

    /// "Damage that would reduce your life total to less than N reduces
    /// it to N instead." — life-floor replacement (Ali from Cairo).
    static final Parser<Effect.Replace> REPLACE_LIFE_FLOOR = sequence(
            phrase("Damage that would reduce your life total to less than").then(AMOUNT),
            phrase("reduces it to").then(AMOUNT).followedBy(word("instead")),
            (threshold, floor) -> new Effect.Replace(
                    Subject.player(Subject.PlayerRef.YOU),
                    "reduce your life total to less than " + threshold,
                    new Effect.SetPropertyValue(Subject.player(Subject.PlayerRef.YOU), Property.LIFE_TOTAL, floor)));

    /// "The next time \[subject\] would \[event\] \[this turn\]?,
    /// \[replacement\] instead." — next-occurrence replacement
    /// (Words of Worship: "The next time you would draw a card this
    /// turn, you gain 5 life instead.").
    static final Parser<Effect.Replace> REPLACE_NEXT_TIME = sequence(
                    phrase("The next time").then(SubjectParsers.SUBJECT),
                    word("would")
                            .then(WORD_OR_CONTRACTION
                                    .suchThat(w -> !w.equals("this") && !w.equals("instead"), "event token")
                                    .atLeastOnce()
                                    .map(words -> String.join(" ", words)))
                            .followedBy(phrase("this turn").optional())
                            .followedBy(string(",")),
                    anyOf(
                            Parser.<Effect>anyOf(MAY, BASE_EFFECT).followedBy(word("instead")),
                            word("instead").then(Parser.<Effect>anyOf(MAY, BASE_EFFECT))),
                    Effect.Replace::new)
            .map(Effect.Replace::asOnlyNextTime);

    /// "For each \<selector\>, \<effect\>." — per-object loop (Cleansing:
    /// "For each land, destroy that land unless any player pays 1 life.").
    static final Parser<Effect.ForEach> FOR_EACH_EFFECT = sequence(
            anyOf(phrase("For each of").then(SELECTOR), phrase("For each").then(SELECTOR))
                    .followedBy(","),
            BASE_EFFECT
                    .<Effect>map(e -> e)
                    .optionallyFollowedBy(UNLESS_CONDITION, (e, c) -> new Effect.Conditional(e, c))
                    .optionallyFollowedBy(IF_CONDITION, (e, c) -> new Effect.Conditional(e, c)),
            Effect.ForEach::new);

    /// "For each \[kind\] among \[scope\], \[effect\]." —
    /// per-distinct-property loop (Bloom Tender).
    static final Parser<Effect.ForEachAmong> FOR_EACH_AMONG_EFFECT = sequence(
            phrase("For each")
                    .then(anyOf(
                            word("color").thenReturn(Effect.ForEachAmong.AmongKind.COLOR),
                            phrase("basic land type").thenReturn(Effect.ForEachAmong.AmongKind.BASIC_LAND_TYPE),
                            phrase("creature type").thenReturn(Effect.ForEachAmong.AmongKind.CREATURE_TYPE)))
                    .followedBy(word("among")),
            SubjectParsers.SUBJECT.followedBy(","),
            BASE_EFFECT,
            Effect.ForEachAmong::new);

    private static final Parser<Subject.PlayerRef> FOR_EACH_PLAYER_REF = anyOf(
            phrase("each opponent").thenReturn(Subject.PlayerRef.EACH_OPPONENT),
            phrase("each other player").thenReturn(Subject.PlayerRef.EACH_OTHER_PLAYER),
            phrase("each player").thenReturn(Subject.PlayerRef.EACH_PLAYER),
            phrase("any number of opponents").thenReturn(Subject.PlayerRef.ANY_NUMBER_OF_OPPONENTS));

    static final Parser<Effect.ForEachPlayer> FOR_EACH_PLAYER_EFFECT = sequence(
            phrase("For").then(FOR_EACH_PLAYER_REF).followedBy(","),
            BASE_EFFECT
                    .<Effect>map(e -> e)
                    .optionallyFollowedBy(UNLESS_CONDITION, (e, c) -> new Effect.Conditional(e, c))
                    .optionallyFollowedBy(IF_CONDITION, (e, c) -> new Effect.Conditional(e, c)),
            Effect.ForEachPlayer::new);

    /// "If \<condition\>, \<override\> instead." — shorthand replacement
    /// that overrides the previously-stated effect without spelling
    /// out a `would` event (River of Tears).
    static final Parser<Effect.ConditionalOverride> CONDITIONAL_OVERRIDE =
            sequence(IF_PREFIX_CONDITION, BASE_EFFECT.followedBy(word("instead")), Effect.ConditionalOverride::new);
}
