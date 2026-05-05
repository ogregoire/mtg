package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.DiscardEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.Effect;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;
import be.imgn.mtg.engine.oracle2.parser.selector.ZoneParser;

/// Parser for [DiscardEffect] ({@mtg.rule 701.8}). Always subject-led.
///
/// Reuses the central [SelectorParser#selectorWith(be.imgn.mtg.engine.oracle2.parser.selector.ZoneParser.CardZoneHint)]
/// with the hand-of-anyone hint so a bare "card" noun phrase ("a
/// card", "a blue card", "two cards") falls back to
/// [be.imgn.mtg.engine.oracle2.domain.selector.ZoneSelector.Hand]
/// owned by [be.imgn.mtg.engine.oracle2.domain.selector.PlayerSelector.Anyone#ANYONE]
/// (the engine resolves the actual owner — the discarding player —
/// from context). Quantifier parsing comes for free from the central
/// selector grammar; no more hand-rolled `wrapInHand` shape.
///
/// The optional `at random` suffix ({@mtg.rule 701.8d}) sets the
/// `atRandom` flag on [DiscardEffect] — the game (not the discarding
/// player) picks the card.
public final class DiscardEffectParser {
    private DiscardEffectParser() {}

    /// Carrier for the (card, atRandom) pair while parsing. The
    /// `optionallyFollowedBy` combinator returns the same type as
    /// its receiver, so we wrap before it and unwrap after.
    private record Discardable(Selector card, boolean atRandom) {
        Discardable withAtRandom() {
            return new Discardable(card, true);
        }
    }

    private static final Parser<Discardable> DISCARDABLE = phrase("discard(s)")
            .then(SelectorParser.selectorWith(ZoneParser.HAND_OF_ANYONE))
            .map(card -> new Discardable(card, false))
            .optionallyFollowedBy(phrase("at random"), (d, _) -> d.withAtRandom());

    /// "discard(s) CARD [at random]" — subject-led wrapper.
    public static final Parser<Function<Selector, Effect>> DISCARDS_FN =
            DISCARDABLE.map(d -> subject -> new DiscardEffect(subject, d.card, d.atRandom));
}
