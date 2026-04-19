package be.imgn.mtg.engine.oracle;

import static be.imgn.mtg.engine.oracle.Words.anyCiWord;
import static be.imgn.mtg.engine.oracle.Words.w;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;

import com.google.common.labs.parse.Parser;

/// Parsers for costs in oracle text.
final class CostParsers {
    private CostParsers() {}

    // ── Cost components ────────────────────────────────────────────────

    static final Parser<Cost.TapSelf> TAP = string("{T}").thenReturn(Cost.TapSelf.TAP_SELF);

    static final Parser<Cost.UntapSelf> UNTAP = string("{Q}").thenReturn(Cost.UntapSelf.UNTAP_SELF);

    static final Parser<Cost.Mana> MANA_COST =
            EffectParsers.MANA_SYMBOL.atLeastOnce().map(Cost.Mana::new);

    static final Parser<Cost.PayLife> PAY_LIFE =
            w("pay").then(SelectorParsers.AMOUNT).followedBy(w("life")).map(Cost.PayLife::new);

    /// Sacrifice cost. Accepts either a self-reference (`~`, `this creature`)
    /// or a full subject / selector (`a creature you control`).
    static final Parser<Cost.SacrificePermanent> SACRIFICE_COST =
            w("sacrifice").then(SubjectParsers.SUBJECT).map(Cost.SacrificePermanent::new);

    static final Parser<Cost.DiscardCard> DISCARD_COST =
            w("discard").then(SelectorParsers.SELECTOR).map(Cost.DiscardCard::new);

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
            SACRIFICE_COST,
            DISCARD_COST,
            TAP_PERMANENT,
            EXILE_COST,
            REMOVE_COUNTER,
            MANA_COST);

    // ── Compound cost: components separated by commas ──────────────────

    public static final Parser<Cost> COST_EXPRESSION = COST_COMPONENT
            .atLeastOnceDelimitedBy(",")
            .map(costs -> costs.size() == 1 ? costs.getFirst() : new Cost.Compound(costs));
}
