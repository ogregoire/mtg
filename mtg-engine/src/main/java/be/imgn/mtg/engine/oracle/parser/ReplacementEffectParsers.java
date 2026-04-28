package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.EffectParsers.BASE_EFFECT;
import static be.imgn.mtg.engine.oracle.parser.EffectParsers.IF_PREFIX_CONDITION;
import static be.imgn.mtg.engine.oracle.parser.EffectParsers.MAY;
import static be.imgn.mtg.engine.oracle.parser.EffectParsers.WORD_OR_CONTRACTION;
import static be.imgn.mtg.engine.oracle.parser.PreventionEffectParsers.PREVENT;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.AMOUNT;
import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle.domain.Duration;
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

    /// Single-segment event capture (no internal commas) — Thought
    /// Reflection: "draw a card".
    private static final Parser<String> REPLACE_EVENT_SIMPLE =
            REPLACE_EVENT_WORD.atLeastOnce().map(ws -> String.join(" ", ws));

    /// Multi-segment Oxford-comma event capture — Academy Manufactor:
    /// "create a Clue, Food, or Treasure token". Each segment is a
    /// word-run; the joined form preserves the inner commas.
    private static final Parser<String> REPLACE_EVENT_MULTI =
            REPLACE_EVENT_SIMPLE.atLeastOnceDelimitedBy(",", Collectors.joining(", "));

    /// Replacement-clause body: "\[may\]? \<effect\>" before/after the
    /// terminal "instead". Reused by [#REPLACE] and [#REPLACE_NEXT_TIME].
    private static final Parser<Effect> REPLACE_BODY = anyOf(
            Parser.<Effect>anyOf(MAY, BASE_EFFECT).followedBy(word("instead")),
            word("instead").then(Parser.<Effect>anyOf(MAY, BASE_EFFECT)));

    /// "If \[subject\] would \[event\], \[replacement\] instead." —
    /// replacement effect (rule 614, e.g., Thought Reflection: "If
    /// you would draw a card, draw two cards instead."; Academy
    /// Manufactor: "If you would create a Clue, Food, or Treasure
    /// token, instead create one of each."). Two arms — simple
    /// (single-segment) event first so a verb-led continuation like
    /// Thought Reflection's "draw two cards" doesn't get pulled into
    /// the event; the multi-segment Oxford-comma arm (Academy) is
    /// the fallback. Each arm is a complete sequence so dot-parse
    /// backtracks across the event/replacement boundary when the
    /// simple-event match leaves a non-replacement continuation.
    /// Structured damage-event capture for the prevention shape:
    /// "deal \[\<amount\> \[or less|or more\]\]? damage to \<target\>".
    /// The optional amount-comparator (Callous Giant: "deal 3 or less
    /// damage to this creature") is folded into the event string.
    /// Distinct from [#REPLACE_EVENT_SIMPLE] in that the trailing
    /// target is a [Subject] (admitting "you" / "this creature" /
    /// etc., which REPLACE_EVENT_STOP_WORDS would otherwise reject).
    private static final Parser<String> DAMAGE_EVENT = anyOf(
            sequence(
                    phrase("deal").then(AMOUNT),
                    anyOf(phrase("or less"), phrase("or more")),
                    phrase("damage to").then(SubjectParsers.SUBJECT),
                    (amt, cmp, target) -> "deal " + amt + " " + cmp + " damage to " + target),
            phrase("deal damage to").then(SubjectParsers.SUBJECT).map(target -> "deal damage to " + target));

    static final Parser<Effect.Replace> REPLACE = anyOf(
            // Damage-prevention shape — "If [source] would deal damage
            // to [target], prevent N of that damage." (Urza's Armor,
            // Sphere of Purity, Sphere of Law). The event has "you"
            // as the damage target, which REPLACE_EVENT_SIMPLE rejects;
            // a dedicated [#DAMAGE_EVENT] captures the structured
            // form. The PREVENT replacement omits "instead".
            sequence(
                    phrase("If").then(SubjectParsers.SUBJECT).followedBy(word("would")),
                    DAMAGE_EVENT.followedBy(string(",")),
                    PREVENT.<Effect>map(p -> p),
                    Effect.Replace::new),
            // Passive damage-prevention — "If damage would be dealt
            // to [target], prevent that damage [and <effect>]?."
            // (Angel of Suffering: "If damage would be dealt to you,
            // prevent that damage and mill twice that many cards.").
            // PREVENT is the replacement leaf; an optional "and
            // <effect>" tail composes a peer effect that fires when
            // the replacement triggers. Distinct from the generic
            // passive arm below since PREVENT omits the terminal
            // "instead".
            sequence(
                    phrase("If damage would be dealt to")
                            .then(SubjectParsers.SUBJECT)
                            .followedBy(string(",")),
                    PREVENT.<List<Effect>>map(p -> List.of(p))
                            .optionallyFollowedBy(word("and").then(BASE_EFFECT), (list, eff) -> {
                                var r = new ArrayList<Effect>(list);
                                r.add(eff);
                                return List.copyOf(r);
                            }),
                    (target, body) -> new Effect.Replace(target, "damage would be dealt", body)),
            // Passive damage-replacement — "If damage would be dealt
            // to [target], [replacement] instead." (Phytohydra:
            // "If damage would be dealt to this creature, put that
            // many +1/+1 counters on it instead."). The grammatical
            // subject of "would" is "damage"; we capture [target] as
            // the Replace.what (damage recipient).
            sequence(
                    phrase("If damage would be dealt to")
                            .then(SubjectParsers.SUBJECT)
                            .followedBy(string(",")),
                    REPLACE_BODY,
                    (target, replacement) -> new Effect.Replace(target, "damage would be dealt", replacement)),
            sequence(
                    phrase("If").then(SubjectParsers.SUBJECT).followedBy(word("would")),
                    REPLACE_EVENT_SIMPLE.followedBy(string(",")),
                    REPLACE_BODY,
                    Effect.Replace::new),
            sequence(
                    phrase("If").then(SubjectParsers.SUBJECT).followedBy(word("would")),
                    REPLACE_EVENT_MULTI.followedBy(string(",")),
                    REPLACE_BODY,
                    Effect.Replace::new));

    /// "If you tap a permanent for mana, it produces \<factor\> times
    /// as much of that mana instead." — mana-multiplier replacement
    /// (Mana Reflection: "twice"; Nyxbloom Ancient: "three times").
    static final Parser<Effect.Replace> REPLACE_MANA_DOUBLE = phrase("If you tap a permanent for mana, it produces")
            .then(anyOf(
                    word("twice").thenReturn(2),
                    phrase("three times").thenReturn(3),
                    phrase("four times").thenReturn(4)))
            .followedBy(phrase("as much of that mana instead"))
            .map(factor -> new Effect.Replace(
                    Subject.player(Subject.PlayerRef.YOU),
                    "tap a permanent for mana",
                    new Effect.ManaProducedMultiplier(factor)));

    /// "All \[combat|noncombat\]? damage that would be dealt to \[from\]
    /// \[this turn\]? is dealt to \[to\] instead." — damage-redirection
    /// replacement (Pariah: "All damage that would be dealt to you is dealt
    /// to ~ instead."; Turn the Tables: "All combat damage that would be
    /// dealt to you this turn is dealt to target attacking creature instead.").
    /// Emits [Effect.RedirectDamage] so the engine sees a typed
    /// source/target pair rather than a free-text event. The `duration`
    /// field is `THIS_TURN` when oracle text explicitly scopes it.
    private static final Parser<Effect.Prevent.Kind> REDIRECT_DAMAGE_KIND = phrase("All")
            .then(anyOf(
                    phrase("combat damage").thenReturn(Effect.Prevent.Kind.COMBAT),
                    phrase("noncombat damage").thenReturn(Effect.Prevent.Kind.NONCOMBAT),
                    word("damage").thenReturn(Effect.Prevent.Kind.ANY)))
            .followedBy(phrase("that would be dealt to"));

    static final Parser<Effect.RedirectDamage> REDIRECT_DAMAGE = anyOf(
            // "this turn" scoped form — Turn the Tables
            sequence(
                    REDIRECT_DAMAGE_KIND,
                    SubjectParsers.SUBJECT.followedBy(phrase("this turn is dealt to")),
                    SubjectParsers.SUBJECT.followedBy(word("instead")),
                    (kind, from, to) -> new Effect.RedirectDamage(kind, from, to, Duration.Fixed.THIS_TURN)),
            // unscoped form — Pariah, Palisade Giant, …
            sequence(
                    REDIRECT_DAMAGE_KIND,
                    SubjectParsers.SUBJECT.followedBy(phrase("is dealt to")),
                    SubjectParsers.SUBJECT.followedBy(word("instead")),
                    Effect.RedirectDamage::new));

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

    /// "For each \<subject\>, \<effect\>." — per-object loop (Cleansing:
    /// "For each land, destroy that land unless any player pays 1
    /// life."; Whirlwind Denial: "For each spell and ability your
    /// opponents control, …" — multi-head SUBJECT.OneOf).
    static final Parser<Effect.ForEach> FOR_EACH_EFFECT = sequence(
            anyOf(
                            phrase("For each of").then(SubjectParsers.SUBJECT),
                            phrase("For each").then(SubjectParsers.SUBJECT))
                    .followedBy(","),
            BASE_EFFECT,
            Effect.ForEach::new);

    /// The property-kind vocabulary for [#FOR_EACH_AMONG_EFFECT].
    private static final Parser<Effect.ForEachAmong.AmongKind> AMONG_KIND = phrase("For each")
            .then(anyOf(
                    word("color").thenReturn(Effect.ForEachAmong.AmongKind.COLOR),
                    phrase("basic land type").thenReturn(Effect.ForEachAmong.AmongKind.BASIC_LAND_TYPE),
                    phrase("creature type").thenReturn(Effect.ForEachAmong.AmongKind.CREATURE_TYPE)));

    /// "For each \[kind\] \[among \[scope\]\]?, \[effect\]." —
    /// per-distinct-property loop (Bloom Tender: "For each color among
    /// permanents you control, …"; Rogues' Gallery: "For each color,
    /// return up to one target creature card of that color …"). When
    /// "among \[scope\]" is absent the scope is null — the loop ranges
    /// over all instances of the kind universally.
    ///
    /// The with-scope arm must precede the bare arm so that "For each
    /// color among …" is not misread as kind=COLOR, scope=null with the
    /// trailing "among …" left unconsumed.
    static final Parser<Effect.ForEachAmong> FOR_EACH_AMONG_EFFECT = anyOf(
            // "For each <kind> among <scope>, <body>" — scoped form.
            sequence(
                    AMONG_KIND.followedBy(word("among")),
                    SubjectParsers.SUBJECT.followedBy(","),
                    BASE_EFFECT,
                    Effect.ForEachAmong::new),
            // "For each <kind>, <body>" — universal form (no scope).
            sequence(
                    AMONG_KIND.followedBy(","),
                    BASE_EFFECT,
                    (kind, body) -> new Effect.ForEachAmong(kind, null, body)));

    private static final Parser<Subject.PlayerRef> FOR_EACH_PLAYER_REF = anyOf(
            phrase("each opponent").thenReturn(Subject.PlayerRef.EACH_OPPONENT),
            phrase("each other player").thenReturn(Subject.PlayerRef.EACH_OTHER_PLAYER),
            phrase("each player").thenReturn(Subject.PlayerRef.EACH_PLAYER),
            phrase("any number of opponents").thenReturn(Subject.PlayerRef.ANY_NUMBER_OF_OPPONENTS));

    static final Parser<Effect.ForEachPlayer> FOR_EACH_PLAYER_EFFECT =
            sequence(phrase("For").then(FOR_EACH_PLAYER_REF).followedBy(","), BASE_EFFECT, Effect.ForEachPlayer::new);

    /// "If \<typed-condition\>, \<override\> instead." — conditional-
    /// override replacement (River of Tears: "If you played a land
    /// this turn, add {B} instead."). Reuses the typed
    /// [EffectParsers#IF_PREFIX_CONDITION]; the override is a single
    /// effect followed by the literal "instead".
    static final Parser<Effect.ConditionalOverride> CONDITIONAL_OVERRIDE =
            sequence(IF_PREFIX_CONDITION, BASE_EFFECT.followedBy(word("instead")), Effect.ConditionalOverride::new);
}
