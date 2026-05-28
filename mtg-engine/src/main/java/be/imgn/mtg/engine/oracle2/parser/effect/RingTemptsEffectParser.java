package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.RingTemptsEffect;
import be.imgn.mtg.engine.oracle2.parser.selector.PlayerSelectorParser;

/// Parser for [RingTemptsEffect] ({@mtg.rule 701.54}). Verb-first:
/// "The Ring tempts (player)." The fixed prefix "The Ring tempts"
/// is consumed for its side effect; the player slot is whatever
/// [PlayerSelectorParser#PLAYER_SELECTOR] yields.
public final class RingTemptsEffectParser {
    private RingTemptsEffectParser() {}

    /// "The Ring tempts (player)." — full sentence; period is
    /// consumed at the [EffectParser] level.
    public static final Parser<RingTemptsEffect> RING_TEMPTS =
            phrase("The Ring tempts").then(PlayerSelectorParser.PLAYER_SELECTOR).map(RingTemptsEffect::new);
}
