package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.AmountParser.AMOUNT;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.sequence;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.SetLifeTotalEffect;
import be.imgn.mtg.engine.oracle2.parser.selector.ZoneParser;

/// Parser for [SetLifeTotalEffect] ({@mtg.rule 119.6}). Surface form
/// "(player)'s life total becomes (amount)." Reuses
/// [ZoneParser#POSSESSIVE_OWNER] for the possessive-player prefix —
/// "your", "target player's", "an opponent's", "their", etc.
public final class SetLifeTotalEffectParser {
    private SetLifeTotalEffectParser() {}

    /// "(player)'s life total becomes (amount)" — full sentence;
    /// period is consumed at the [EffectParser] level.
    public static final Parser<SetLifeTotalEffect> SET_LIFE_TOTAL = sequence(
            ZoneParser.POSSESSIVE_OWNER.followedBy(phrase("life total becomes")), AMOUNT, SetLifeTotalEffect::new);
}
