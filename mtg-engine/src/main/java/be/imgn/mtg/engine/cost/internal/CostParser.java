package be.imgn.mtg.engine.cost.internal;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

import be.imgn.mtg.engine.ability.internal.parser.AmountParser;
import be.imgn.mtg.engine.ability.internal.parser.CommonParsers;
import be.imgn.mtg.engine.ability.internal.parser.ObjectSelectorParser;
import be.imgn.mtg.engine.cost.Cost;
import be.imgn.mtg.engine.mana.internal.ManaParser;

/// Parser for cost text in activated abilities ({@mtg.rule 118}).
///
/// Handles all cost types:
/// - {T} → TapCost
/// - {Q} → UntapCost
/// - [+N], [-N], [0] → LoyaltyCost
/// - Pay N life → LifeCost
/// - Sacrifice SELECTOR → SacrificeCost
/// - Discard SELECTOR → DiscardCost
/// - Tap SELECTOR → TapPermanentsCost
/// - {mana symbols} → ManaCost (fallback)
/// - Multiple costs separated by ", " → CompoundCost
public final class CostParser {

    private CostParser() {}

    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    /// Parses the tap symbol cost {T}.
    private static final Parser<Cost> TAP_SYMBOL = string("{T}").thenReturn(new TapCost());

    /// Parses the untap symbol cost {Q}.
    private static final Parser<Cost> UNTAP_SYMBOL = string("{Q}").thenReturn(new UntapCost());

    /// Parses a loyalty cost [+N], [-N], or [0].
    @SuppressWarnings("unchecked")
    private static final Parser<Cost> LOYALTY = string("[")
            .then(anyOf(
                    string("+").then(CommonParsers.INTEGER),
                    string("0").thenReturn(0),
                    string("-").then(CommonParsers.INTEGER).map(n -> -n)))
            .followedBy("]")
            .map(amount -> new LoyaltyCost(amount));

    /// Parses "Pay N life" → LifeCost.
    private static final Parser<Cost> LIFE =
            word("Pay").then(AmountParser.NUMERIC).followedBy("life").map(amount -> new LifeCost(amount));

    /// Parses "Sacrifice SELECTOR" → SacrificeCost.
    private static final Parser<Cost> SACRIFICE =
            word("Sacrifice").then(ObjectSelectorParser.OBJECT_SELECTOR).map(selector -> new SacrificeCost(selector));

    /// Parses "Discard SELECTOR" → DiscardCost.
    private static final Parser<Cost> DISCARD =
            word("Discard").then(ObjectSelectorParser.OBJECT_SELECTOR).map(selector -> new DiscardCost(selector));

    /// Parses "Tap SELECTOR" → TapPermanentsCost.
    private static final Parser<Cost> TAP_PERMANENTS =
            word("Tap").then(ObjectSelectorParser.OBJECT_SELECTOR).map(selector -> new TapPermanentsCost(selector));

    /// Parses mana symbols as a ManaCost (fallback).
    private static final Parser<Cost> MANA = ManaParser.MANA_COST.map(cost -> cost);

    /// Parses a single cost (any type).
    @SuppressWarnings("unchecked")
    public static final Parser<Cost> SINGLE_COST =
            anyOf(TAP_SYMBOL, UNTAP_SYMBOL, LOYALTY, LIFE, SACRIFICE, DISCARD, TAP_PERMANENTS, MANA);

    /// Parses one or more costs separated by ", ", producing a CompoundCost if multiple.
    public static final Parser<Cost> COST = SINGLE_COST
            .atLeastOnceDelimitedBy(",")
            .map(costs -> costs.size() == 1 ? costs.get(0) : new CompoundCost(costs));

    /// Parses a cost string, skipping whitespace between tokens.
    ///
    /// @param costText the cost text to parse
    /// @return the parsed cost
    public static Cost parse(String costText) {
        return COST.parseSkipping(WHITESPACE, costText);
    }
}
