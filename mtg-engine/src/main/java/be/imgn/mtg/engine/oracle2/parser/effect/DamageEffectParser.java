package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.AmountParser.AMOUNT;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.sequence;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.effect.DamageEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.Effect;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parser for [DamageEffect] ({@mtg.rule 119}). Subject-led:
/// "(source) deal(s) (amount) damage to (target)." The source is
/// typically `~` (self-reference, e.g., Shock / Blaze); other
/// sources land when oracle text needs them. The target uses the
/// general [SelectorParser#SELECTOR] grammar so "any target",
/// "target creature", "target player", etc. all fit.
public final class DamageEffectParser {
    private DamageEffectParser() {}

    /// "deal(s) AMOUNT damage to TARGET" — subject-led wrapper.
    /// The verb body sequences amount and target into a carrier; the
    /// outer map threads in the source subject.
    public static final Parser<Function<Selector, Effect>> DEALS_DAMAGE_FN = sequence(
                    phrase("deal(s)").then(AMOUNT).followedBy(phrase("damage to")),
                    SelectorParser.SELECTOR,
                    DamageBody::new)
            .map(body -> source -> new DamageEffect(source, body.amount(), body.target()));

    /// Internal carrier — pairs the damage amount with the target
    /// before the source is threaded in.
    private record DamageBody(Amount amount, Selector target) {}
}
