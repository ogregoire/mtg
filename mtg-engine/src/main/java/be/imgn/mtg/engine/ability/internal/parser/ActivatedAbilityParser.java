package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.parse.Parser.sequence;

import be.imgn.mtg.engine.cost.internal.CostParser;
import be.imgn.mtg.parse.CharPredicate;
import be.imgn.mtg.parse.Parser;

/// Parser for activated abilities in oracle text ({@mtg.rule 113.3b}).
///
/// Parses the "Cost: Effect." format of activated abilities, automatically determining
/// timing and activation limits based on the cost and effect types.
///
/// Examples:
/// - "{T}: Add {G}." → mana ability
/// - "[-3]: Destroy target creature." → loyalty ability
/// - "{2}{B}, Sacrifice a creature: Destroy target creature." → regular ability
public final class ActivatedAbilityParser {

    private ActivatedAbilityParser() {}

    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    /// Parses an activated ability: "Cost: Effect."
    public static final Parser<ParsedActivatedAbility> ACTIVATED_ABILITY =
            sequence(CostParser.COST.followedBy(":"), EffectParser.EFFECT, ParsedActivatedAbility::create);

    /// Parses an activated ability from oracle text, skipping whitespace.
    ///
    /// @param oracleText the oracle text to parse
    /// @return the parsed activated ability
    public static ParsedActivatedAbility parse(String oracleText) {
        return ACTIVATED_ABILITY.parseSkipping(WHITESPACE, oracleText);
    }
}
