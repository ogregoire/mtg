package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.DiscardEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.Effect;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;
import be.imgn.mtg.engine.oracle2.parser.selector.ZoneParser;

/// Parser for [DiscardEffect] ({@mtg.rule 701.9}). Always subject-led.
///
/// Two surface shapes:
///
/// 1. **Whole hand** — "discard your hand" / "discard their hand".
///    Both collapse to [DiscardEffect.What.Hand#HAND] — the hand-
///    owner is the discarding player (parent [DiscardEffect#who] is
///    authoritative; "your" / "their" surface variation carries no
///    AST information).
/// 2. **Picked cards** — "discard a card", "discard two cards [at
///    random]", "discard a blue card". Uses
///    [SelectorParser#selectorWith(be.imgn.mtg.engine.oracle2.parser.selector.ZoneParser.CardZoneHint)]
///    with the hand-of-anyone hint so a bare "card" noun phrase
///    falls back to
///    [be.imgn.mtg.engine.oracle2.domain.selector.ZoneSelector.Hand]
///    owned by [be.imgn.mtg.engine.oracle2.domain.selector.PlayerSelector.Anyone#ANYONE].
///    Produces [DiscardEffect.What.Cards] with the optional
///    `atRandom` flag ({@mtg.rule 701.8d}).
public final class DiscardEffectParser {
    private DiscardEffectParser() {}

    /// "your hand" / "their hand" — both collapse to
    /// [DiscardEffect.What.Hand#HAND]. The possessive variation
    /// ("your" vs "their") is surface flavor only; the hand-owner is
    /// always the discarding subject, which the parent
    /// [DiscardEffect#who] already names.
    private static final Parser<DiscardEffect.What.Hand> WHOLE_HAND =
            anyOf(phrase("your hand"), phrase("their hand")).thenReturn(DiscardEffect.What.Hand.HAND);

    /// Specific card(s) form. The optional `at random` suffix lives
    /// only on this arm — it can't follow a whole-hand discard.
    private static final Parser<DiscardEffect.What.Cards> PICKED_CARDS = SelectorParser.selectorWith(
                    ZoneParser.HAND_OF_ANYONE)
            .map(DiscardEffect.What.Cards::new)
            .optionallyFollowedBy(phrase("at random"), (c, _) -> c.withAtRandom());

    private static final Parser<DiscardEffect.What> DISCARDABLE =
            phrase("discard(s)").then(anyOf(WHOLE_HAND, PICKED_CARDS));

    /// "discard(s) WHAT" — subject-led wrapper.
    public static final Parser<Function<Selector, Effect>> DISCARDS_FN =
            DISCARDABLE.map(what -> subject -> new DiscardEffect(subject, what));
}
