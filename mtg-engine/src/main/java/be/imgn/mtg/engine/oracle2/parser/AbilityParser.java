package be.imgn.mtg.engine.oracle2.parser;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;

import java.util.List;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.Ability;
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

    /// `When|Whenever|At [event], [effects].` Triggered ability
    /// ({@mtg.rule 603}). The trigger word is encoded by which arm of
    /// [Ability.TriggeredAbility] is produced — [Ability.TriggeredAbility.When],
    /// [Ability.TriggeredAbility.Whenever], or [Ability.TriggeredAbility.At].
    public static final Parser<Ability.TriggeredAbility> TRIGGERED = anyOf(
            sequence(
                    phrase("When").then(TriggerEventParser.TRIGGER_EVENT),
                    string(",").then(EffectParser.EFFECT.atLeastOnce()),
                    Ability.TriggeredAbility.When::new),
            sequence(
                    phrase("Whenever").then(TriggerEventParser.TRIGGER_EVENT),
                    string(",").then(EffectParser.EFFECT.atLeastOnce()),
                    Ability.TriggeredAbility.Whenever::new),
            sequence(
                    phrase("At").then(TriggerEventParser.TRIGGER_EVENT),
                    string(",").then(EffectParser.EFFECT.atLeastOnce()),
                    Ability.TriggeredAbility.At::new));

    /// `[cost]: [effects].` Activated ability ({@mtg.rule 602}).
    public static final Parser<Ability.ActivatedAbility> ACTIVATED = sequence(
            CostParser.COST, string(":").then(EffectParser.EFFECT.atLeastOnce()), Ability.ActivatedAbility::new);

    /// `[effects].` Spell ability ({@mtg.rule 113.3a}) — bare effect
    /// sentence(s).
    public static final Parser<Ability.SpellAbility> SPELL =
            EffectParser.EFFECT.atLeastOnce().map(Ability.SpellAbility::new);

    /// One ability — keyword, triggered, activated, or spell.
    public static final Parser<Ability> ABILITY = anyOf(KeywordAbilityParser.KEYWORD, TRIGGERED, ACTIVATED, SPELL);

    /// One paragraph of oracle text — either a comma-separated
    /// keyword list or a single non-keyword ability. Keyword lists
    /// take precedence so multi-keyword paragraphs ("Flying,
    /// vigilance") expand to a list rather than committing the
    /// first keyword as an [Ability] singleton.
    public static final Parser<List<Ability>> PARAGRAPH =
            anyOf(KeywordAbilityParser.KEYWORD_LIST, ABILITY.map(List::of));
}
