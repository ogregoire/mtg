package be.imgn.mtg.engine.oracle;

import static be.imgn.mtg.engine.oracle.Words.anyWord;
import static be.imgn.mtg.engine.oracle.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.one;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

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
    /// body as [#MANA_COST] but consumes a leading "Pay" keyword.
    static final Parser<Cost.Mana> PAY_MANA_COST = phrase("Pay").then(MANA_COST);

    static final Parser<Cost.PayLife> PAY_LIFE =
            phrase("Pay").then(SelectorParsers.AMOUNT).followedBy(word("life")).map(Cost.PayLife::new);

    /// Sacrifice cost. Accepts either a self-reference (`~`, `this creature`)
    /// or a full subject / selector (`a creature you control`).
    static final Parser<Cost.SacrificePermanent> SACRIFICE_COST =
            phrase("Sacrifice").then(SubjectParsers.SUBJECT).map(Cost.SacrificePermanent::new);

    static final Parser<Cost.DiscardCard> DISCARD_COST = phrase("Discard")
            .then(SubjectParsers.SUBJECT)
            .map(Cost.DiscardCard::new)
            .optionallyFollowedBy(phrase("at random"), (d, _) -> new Cost.DiscardCard(d.what(), true));

    /// "Discard your hand" — whole-hand discard cost (Null Brooch).
    static final Parser<Cost.DiscardHand> DISCARD_HAND_COST = phrase("Discard")
            .then(anyWord("your", "their", "his", "her", "its"))
            .followedBy(word("hand"))
            .thenReturn(Cost.DiscardHand.DISCARD_HAND);

    static final Parser<Cost.TapPermanent> TAP_PERMANENT =
            phrase("Tap").then(SelectorParsers.SELECTOR).map(Cost.TapPermanent::new);

    /// "from [possessive] [zone]" suffix used by [#EXILE_COST] — e.g.,
    /// "exile this card from your hand" (Simian Spirit Guide).
    private static final Parser<Zone.Source> EXILE_FROM_ZONE = sequence(
                    word("from").then(anyWord("your", "their", "its", "a", "any")),
                    SelectorParsers.ZONE_NAME,
                    Zone.Named::new)
            .map(Zone.Source::fromZone);

    static final Parser<Cost.Exile> EXILE_COST = phrase("Exile")
            .then(SubjectParsers.SUBJECT)
            .map(Cost.Exile::new)
            .optionallyFollowedBy(EXILE_FROM_ZONE, Cost.Exile::withFrom);

    static final Parser<Cost.RemoveCounter> REMOVE_COUNTER = sequence(
            phrase("Remove").then(SelectorParsers.AMOUNT),
            // Typed: "remove N <type> counter(s) from X". Untyped (O'aka,
            // Traveling Merchant: "Remove a counter from a nonland
            // permanent you control") falls back to a generic "any" type.
            anyOf(
                    SelectorParsers.COUNTER_TYPE.followedBy(phrase("counter(s) from")),
                    phrase("counter(s) from").thenReturn(CounterType.named("any"))),
            SubjectParsers.SUBJECT,
            Cost.RemoveCounter::new);

    /// Planeswalker loyalty cost — "+N", "-N"/"−N" (handled by
    /// [AmountParsers#SIGNED_INT] which accepts both ASCII hyphen and
    /// U+2212 minus), or the unsigned "0" form used for zero-cost
    /// abilities (Gideon, Ally of Zendikar; Jace, Memory Adept). The
    /// signed zero variants "+0" / "-0" never appear in oracle text.
    static final Parser<Cost.Loyalty> LOYALTY_COST =
            anyOf(AmountParsers.SIGNED_INT.map(Cost.Loyalty::new), one('0').thenReturn(new Cost.Loyalty(0)));

    /// "Return [subject] to [poss] owner's hand" — bounce cost
    /// (Broken Fall: "Return this enchantment to its owner's hand:
    /// Regenerate target creature."; Molting Skin). The possessive
    /// is consumed as flavor since owner-scope is implicit in the
    /// bounce.
    static final Parser<Cost.ReturnToHand> RETURN_TO_HAND_COST = phrase("Return")
            .then(SubjectParsers.SUBJECT)
            .followedBy(phrase("to [its|their|your|his|her] owner's hand"))
            .map(Cost.ReturnToHand::new);

    /// "Put a [type] counter on [subject]" — counter placement cost
    /// (Devoted Druid: "Put a -1/-1 counter on this creature: Untap
    /// this creature."). Typed counter required; the untyped
    /// fallback isn't useful at cost position.
    static final Parser<Cost.AddCounter> ADD_COUNTER_COST = sequence(
            phrase("Put").then(SelectorParsers.AMOUNT),
            SelectorParsers.COUNTER_TYPE.followedBy(phrase("counter(s) on")),
            SubjectParsers.SUBJECT,
            Cost.AddCounter::new);

    /// "Reveal \[N\] cards from your hand \[that share X\]?" — reveal
    /// cost (Illuminated Folio: "Reveal two cards from your hand that
    /// share a color"). The "that share …" tail captures the
    /// constraint as free text until a structured variant is needed.
    static final Parser<Cost.Reveal> REVEAL_COST = phrase("Reveal")
            .then(SelectorParsers.AMOUNT)
            .followedBy(phrase("card(s) from your hand"))
            .map(n -> new Cost.Reveal(n, null))
            .optionallyFollowedBy(
                    phrase("that share").then(word().atLeastOnce().map(ws -> String.join(" ", ws))),
                    (r, constraint) -> new Cost.Reveal(r.count(), "share " + constraint));

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
            RETURN_TO_HAND_COST,
            ADD_COUNTER_COST,
            REVEAL_COST,
            MANA_COST);

    // ── Compound cost: components separated by commas, alternatives by "or" ──

    private static final Parser<Cost> COMMA_LIST = COST_COMPONENT
            .atLeastOnceDelimitedBy(",")
            .map(costs -> costs.size() == 1 ? costs.getFirst() : new Cost.Compound(costs));

    /// Activation cost — one or more [#COST_COMPONENT]s joined by
    /// commas ([Cost.Compound]), optionally with "or"-joined
    /// alternatives ([Cost.Or], e.g., Bloodthorn Flail:
    /// "Equip—Pay {3} or discard a card.").
    public static final Parser<Cost> COST_EXPRESSION = COMMA_LIST
            .atLeastOnceDelimitedBy(phrase("or"), Collectors.toUnmodifiableList())
            .map(options -> options.size() == 1 ? options.getFirst() : new Cost.Or(options));
}
