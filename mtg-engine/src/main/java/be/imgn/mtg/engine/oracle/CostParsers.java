package be.imgn.mtg.engine.oracle;

import static be.imgn.mtg.engine.oracle.Words.anyCiWord;
import static be.imgn.mtg.engine.oracle.Words.ciWords;
import static be.imgn.mtg.engine.oracle.Words.w;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;

import java.util.stream.Collectors;

import com.google.common.labs.parse.Parser;

/// Parsers for costs in oracle text.
final class CostParsers {
    private CostParsers() {}

    // ── Cost components ────────────────────────────────────────────────

    static final Parser<Cost.TapSelf> TAP = string("{T}").thenReturn(Cost.TapSelf.TAP_SELF);

    static final Parser<Cost.UntapSelf> UNTAP = string("{Q}").thenReturn(Cost.UntapSelf.UNTAP_SELF);

    static final Parser<Cost.Mana> MANA_COST =
            EffectParsers.MANA_SYMBOL.atLeastOnce().map(Cost.Mana::new);

    /// "Pay {cost}" — explicit-verb mana cost (Bloodthorn Flail:
    /// "Equip—Pay {3} or discard a card."). Parses the same mana-symbol
    /// body as {@link #MANA_COST} but consumes a leading "Pay" keyword.
    static final Parser<Cost.Mana> PAY_MANA_COST = w("pay").then(MANA_COST);

    static final Parser<Cost.PayLife> PAY_LIFE =
            w("pay").then(SelectorParsers.AMOUNT).followedBy(w("life")).map(Cost.PayLife::new);

    /// Sacrifice cost. Accepts either a self-reference (`~`, `this creature`)
    /// or a full subject / selector (`a creature you control`).
    static final Parser<Cost.SacrificePermanent> SACRIFICE_COST =
            w("sacrifice").then(SubjectParsers.SUBJECT).map(Cost.SacrificePermanent::new);

    static final Parser<Cost.DiscardCard> DISCARD_COST = w("discard")
            .then(SelectorParsers.SELECTOR)
            .map(Cost.DiscardCard::new)
            .optionallyFollowedBy(ciWords("at random"), (d, _) -> new Cost.DiscardCard(d.what(), true));

    /// "Discard your hand" — whole-hand discard cost (Null Brooch).
    static final Parser<Cost.DiscardHand> DISCARD_HAND_COST = w("discard")
            .then(anyCiWord("your", "their", "his", "her", "its"))
            .followedBy(w("hand"))
            .thenReturn(Cost.DiscardHand.DISCARD_HAND);

    static final Parser<Cost.TapPermanent> TAP_PERMANENT =
            w("tap").then(SelectorParsers.SELECTOR).map(Cost.TapPermanent::new);

    /// "from [possessive] [zone]" suffix used by {@link #EXILE_COST} — e.g.,
    /// "exile this card from your hand" (Simian Spirit Guide).
    private static final Parser<Zone.Source> EXILE_FROM_ZONE = sequence(
                    w("from").then(anyCiWord("your", "their", "its", "a", "any")),
                    SelectorParsers.ZONE_NAME,
                    Zone.Named::new)
            .map(Zone.Source::fromZone);

    static final Parser<Cost.Exile> EXILE_COST = w("exile")
            .then(SubjectParsers.SUBJECT)
            .map(Cost.Exile::new)
            .optionallyFollowedBy(EXILE_FROM_ZONE, Cost.Exile::withFrom);

    static final Parser<Cost.RemoveCounter> REMOVE_COUNTER = sequence(
            w("remove").then(SelectorParsers.AMOUNT),
            SelectorParsers.COUNTER_TYPE
                    .followedBy(anyCiWord("counters", "counter"))
                    .followedBy(w("from")),
            SubjectParsers.SUBJECT,
            Cost.RemoveCounter::new);

    static final Parser<Cost.Loyalty> LOYALTY_COST = sequence(
            anyOf(string("+").thenReturn(1), string("-").thenReturn(-1)),
            SelectorParsers.INTEGER,
            (sign, n) -> new Cost.Loyalty(sign * n));

    // ── Single cost component ──────────────────────────────────────────

    static final Parser<Cost> COST_COMPONENT = anyOf(
            TAP,
            UNTAP,
            LOYALTY_COST,
            PAY_LIFE,
            PAY_MANA_COST, // must precede PAY_LIFE-fallback/MANA_COST (shares "pay" prefix)
            SACRIFICE_COST,
            DISCARD_HAND_COST, // must precede DISCARD_COST (shares "discard" prefix)
            DISCARD_COST,
            TAP_PERMANENT,
            EXILE_COST,
            REMOVE_COUNTER,
            MANA_COST);

    // ── Compound cost: components separated by commas, alternatives by "or" ──

    private static final Parser<Cost> COMMA_LIST = COST_COMPONENT
            .atLeastOnceDelimitedBy(",")
            .map(costs -> costs.size() == 1 ? costs.getFirst() : new Cost.Compound(costs));

    /// Activation cost — one or more {@link #COST_COMPONENT}s joined by
    /// commas ({@link Cost.Compound}), optionally with "or"-joined
    /// alternatives ({@link Cost.Or}, e.g., Bloodthorn Flail:
    /// "Equip—Pay {3} or discard a card.").
    public static final Parser<Cost> COST_EXPRESSION = COMMA_LIST
            .atLeastOnceDelimitedBy(w("or"), Collectors.toUnmodifiableList())
            .map(options -> options.size() == 1 ? options.getFirst() : new Cost.Or(options));
}
