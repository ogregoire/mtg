package be.imgn.mtg.engine.oracle2.parser.ability;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.List;

import com.google.common.labs.parse.Parser;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.oracle2.domain.Condition;
import be.imgn.mtg.engine.oracle2.domain.ability.Ability;
import be.imgn.mtg.engine.oracle2.domain.ability.TriggerEvent;
import be.imgn.mtg.engine.oracle2.domain.effect.Effect;
import be.imgn.mtg.engine.oracle2.parser.ConditionParser;
import be.imgn.mtg.engine.oracle2.parser.effect.EffectParser;

/// Top-level [Ability] dispatcher. Produces a single ability per
/// invocation; the OracleParser-side paragraph splitter wraps the
/// result into `List<Ability>` (multi-keyword paragraphs use
/// [KeywordAbilityParser#KEYWORD_LIST] directly).
///
/// Arm precedence — documented because dot-parse is non-
/// backtracking, so a misordering would silently swallow input:
/// 1. [KeywordAbilityParser#KEYWORD] — keyword shortcuts have no
///    overlap with verb/cost prefixes, but are checked first
///    because the no-param table is the cheapest to fail.
/// 2. [#TRIGGERED] — `When|Whenever|At` prefix is unambiguous.
/// 3. [#ACTIVATED] — distinguished by the cost-prefixed `:` token.
/// 4. [#SPELL] — pure-effect fallback for instant/sorcery bodies.
public final class AbilityParser {
    private AbilityParser() {}

    /// `When|Whenever|At [event], [if condition,]? [effects].`
    /// Triggered ability ({@mtg.rule 603}). The trigger word is
    /// encoded by which arm of [Ability.TriggeredAbility] is produced
    /// — [Ability.TriggeredAbility.When],
    /// [Ability.TriggeredAbility.Whenever], or
    /// [Ability.TriggeredAbility.At]. The optional intervening-if
    /// clause ({@mtg.rule 603.4}) is parsed by [ConditionParser#CONDITION]
    /// when present and stored on the resulting record's
    /// `interveningIf` slot; absent, the slot is `null`.
    public static final Parser<Ability.TriggeredAbility> TRIGGERED = anyOf(
            triggeredArm("When", Ability.TriggeredAbility.When::new),
            triggeredArm("Whenever", Ability.TriggeredAbility.Whenever::new),
            triggeredArm("At", Ability.TriggeredAbility.At::new));

    /// One trigger-word arm — `[word] [event], [if condition,]?
    /// [effects]`. Factored out so all three arms share the same
    /// post-event optional-condition shape.
    private static <T extends Ability.TriggeredAbility> Parser<T> triggeredArm(String word, TriggerCtor<T> ctor) {
        var prefix = phrase(word).then(TriggerEventParser.TRIGGER_EVENT).followedBy(string(","));
        var bodyWithCondition = sequence(
                phrase("if").then(ConditionParser.CONDITION).followedBy(string(",")),
                EffectParser.EFFECT.atLeastOnce(),
                Body::with);
        var bodyWithoutCondition = EffectParser.EFFECT.atLeastOnce().map(Body::without);
        return sequence(
                prefix,
                anyOf(bodyWithCondition, bodyWithoutCondition),
                (event, body) -> ctor.construct(event, body.condition(), body.effects()));
    }

    /// 3-arg constructor handle for the three [Ability.TriggeredAbility]
    /// arms. Each record's canonical constructor matches this shape.
    @FunctionalInterface
    private interface TriggerCtor<T extends Ability.TriggeredAbility> {
        T construct(TriggerEvent event, @Nullable Condition condition, List<Effect> effects);
    }

    /// Carrier for the post-event tail — bundles the optional
    /// intervening-if and the effects so [#triggeredArm] can dispatch
    /// on either shape and hand both to the record constructor at the
    /// end.
    private record Body(@Nullable Condition condition, List<Effect> effects) {
        static Body with(Condition condition, List<Effect> effects) {
            return new Body(condition, effects);
        }

        static Body without(List<Effect> effects) {
            return new Body(null, effects);
        }
    }

    /// Optional flavor-word prefix on an activated ability
    /// ({@mtg.rule 207.2d}, Tymora's Invoker: "Sleight of Hand —
    /// {8}: Draw two cards."). Per the rule, flavor words have no
    /// game function — the parser consumes the prefix and the em-
    /// dash separator, then discards both. Captures one-or-more
    /// words up to the em-dash; the em-dash is the disambiguator
    /// (costs start with `{` / `Sacrifice` / `Pay` / `Discard`, none
    /// of which are bare alphabetic words, so a flavor-less
    /// activated ability's `[cost]` won't be misread as a long
    /// flavor word).
    private static final Parser<?> FLAVOR_WORD_PREFIX = word().atLeastOnce().followedBy(string("—"));

    /// `[flavor-word —]? [cost]: [effects].` Activated ability
    /// ({@mtg.rule 602}). The optional flavor-word prefix is
    /// consumed and discarded; flavor-less activated abilities (the
    /// common case) fall through to the no-prefix arm.
    public static final Parser<Ability.ActivatedAbility> ACTIVATED = anyOf(
            sequence(
                    FLAVOR_WORD_PREFIX.then(CostParser.COST),
                    string(":").then(EffectParser.EFFECT.atLeastOnce()),
                    Ability.ActivatedAbility::new),
            sequence(
                    CostParser.COST,
                    string(":").then(EffectParser.EFFECT.atLeastOnce()),
                    Ability.ActivatedAbility::new));

    /// `[effects].` Spell ability ({@mtg.rule 113.3a}) — bare effect
    /// sentence(s).
    public static final Parser<Ability.SpellAbility> SPELL =
            EffectParser.EFFECT.atLeastOnce().map(Ability.SpellAbility::new);

    /// One ability — keyword, triggered, activated, can't-act
    /// restriction, P/T modifier, or spell. The static-restriction /
    /// modifier arms precede [#SPELL] because their subject-led
    /// sentences would otherwise be rejected by `EffectParser`'s
    /// verb-led / subject-led dispatch and fail the whole paragraph.
    public static final Parser<Ability> ABILITY = anyOf(
            KeywordAbilityParser.KEYWORD,
            TRIGGERED,
            ACTIVATED,
            ModifyCostParser.MODIFY_COST,
            ModifyPTParser.MODIFY_PT,
            GainAbilityParser.GAIN_ABILITY,
            LoseAbilityParser.LOSE_ABILITY,
            MaximumHandSizeParser.MAXIMUM_HAND_SIZE,
            RemoveSupertypeParser.REMOVE_SUPERTYPE,
            SetColorsParser.SET_COLORS,
            SetBasicLandTypeParser.SET_BASIC_LAND_TYPE,
            EnterTappedParser.ENTER_TAPPED,
            SPELL);

    /// One paragraph of oracle text — either a comma-separated
    /// keyword list or a single non-keyword ability. Keyword lists
    /// take precedence so multi-keyword paragraphs ("Flying,
    /// vigilance") expand to a list rather than committing the
    /// first keyword as an [Ability] singleton.
    public static final Parser<List<Ability>> PARAGRAPH =
            anyOf(KeywordAbilityParser.KEYWORD_LIST, ABILITY.map(List::of));
}
