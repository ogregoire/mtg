package be.imgn.mtg.engine.oracle2.parser.ability;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.sequence;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.ability.Ability;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parser for [Ability.LoseAbility] — continuous ability-removal
/// sentences of the form "[SUBJECT] [lose|loses] [keyword]."
/// ({@mtg.rule 613.1f}, layer 6). Mirrors [GainAbilityParser] and
/// reuses [KeywordAbilityParser#KEYWORD] so every keyword that can be
/// granted can also be removed.
public final class LoseAbilityParser {
    private LoseAbilityParser() {}

    /// "[subject] [lose|loses] [keyword]." — sentence-closing period.
    public static final Parser<Ability.LoseAbility> LOSE_ABILITY = sequence(
                    SelectorParser.SELECTOR.followedBy(phrase("[lose|loses]")),
                    KeywordAbilityParser.KEYWORD,
                    Ability.LoseAbility::new)
            .followedBy(phrase("."));
}
