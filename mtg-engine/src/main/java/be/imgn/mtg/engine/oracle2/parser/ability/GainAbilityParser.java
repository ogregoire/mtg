package be.imgn.mtg.engine.oracle2.parser.ability;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.sequence;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.ability.Ability;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parser for [Ability.GainAbility] — continuous ability-grant
/// sentences of the form "[SUBJECT] have/has [keyword]."
/// ({@mtg.rule 613.1f}, layer 6). Reuses [KeywordAbilityParser#KEYWORD]
/// for the granted ability so every parameterless and parameterised
/// keyword automatically composes.
public final class GainAbilityParser {
    private GainAbilityParser() {}

    /// "[subject] [have|has] [keyword]." — sentence-closing period.
    public static final Parser<Ability.GainAbility> GAIN_ABILITY = sequence(
                    SelectorParser.SELECTOR.followedBy(phrase("[have|has]")),
                    KeywordAbilityParser.KEYWORD,
                    Ability.GainAbility::new)
            .followedBy(phrase("."));
}
