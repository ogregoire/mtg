package be.imgn.mtg.engine.oracle2.parser.ability;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.ability.Ability;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parser for [Ability.ModifyCost] — continuous cost-modification
/// sentences of the form "[SOURCE] cost [amount] [less|more] [to
/// cast]?." ({@mtg.rule 117.7}). The verb arm ("less" → reduction,
/// "more" → increase) determines which [Ability.ModifyCost] subtype
/// is produced — [Ability.DecreaseCost] or [Ability.IncreaseCost].
///
/// [SOURCE] is either a spell selector ("Spells cost {1} less to
/// cast.") or a keyword-cost reference ("Buyback costs cost {2}
/// less.", {@mtg.rule 702.1a}). The trailing "to cast" is optional
/// — present on spell-cast modifiers, absent on keyword-cost
/// modifiers.
public final class ModifyCostParser {
    private ModifyCostParser() {}

    /// Keyword-cost source — "Buyback costs". The trailing "costs"
    /// belongs to the source phrase (it identifies *which* cost), not
    /// the verb "cost(s)" that follows. New cost-bearing keywords
    /// land as new arms here plus a constant in [Ability.KeywordCost].
    private static final Parser<Ability.KeywordCost> KEYWORD_COST =
            anyOf(phrase("Buyback").followedBy(phrase("costs")).thenReturn(Ability.KeywordCost.BUYBACK));

    /// Spell selector source — "Spells", "Creature spells you cast",
    /// etc. Wrapped in [Ability.CostSource.Spells].
    private static final Parser<Ability.CostSource.Spells> SPELLS =
            SelectorParser.SELECTOR.map(Ability.CostSource.Spells::new);

    /// One source. Keyword-cost arm first so the "Buyback costs"
    /// prefix isn't stolen by the broader spell selector.
    private static final Parser<Ability.CostSource> SOURCE = anyOf(KEYWORD_COST, SPELLS);

    /// "[source] cost [amount] less" — reduction arm.
    private static final Parser<Ability.DecreaseCost> DECREASE = sequence(
            SOURCE.followedBy(phrase("cost(s)")),
            CostParser.MANA_COST.followedBy(phrase("less")),
            Ability.DecreaseCost::new);

    /// "[source] cost [amount] more" — increase arm.
    private static final Parser<Ability.IncreaseCost> INCREASE = sequence(
            SOURCE.followedBy(phrase("cost(s)")),
            CostParser.MANA_COST.followedBy(phrase("more")),
            Ability.IncreaseCost::new);

    /// "[source] cost [amount] [less|more] [to cast]?." — sentence-
    /// closing period. "to cast" is optional because keyword-cost
    /// modifiers ("Buyback costs cost {2} less.") don't carry it.
    public static final Parser<Ability.ModifyCost> MODIFY_COST = Parser.<Ability.ModifyCost>anyOf(DECREASE, INCREASE)
            .optionallyFollowedBy(phrase("to cast"), (mc, _) -> mc)
            .followedBy(phrase("."));
}
