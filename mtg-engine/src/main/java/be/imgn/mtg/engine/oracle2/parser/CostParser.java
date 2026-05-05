package be.imgn.mtg.engine.oracle2.parser;

import static be.imgn.mtg.engine.oracle2.parser.AmountParser.AMOUNT;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.consecutive;
import static com.google.common.labs.parse.Parser.string;

import com.google.common.labs.parse.CharacterSet;
import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.Cost;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parsers for [Cost]. Today the grammar covers four primitive
/// shapes — mana cost ({@mtg.rule 107.4}, {@mtg.rule 117}), tap-self
/// ({@mtg.rule 118.12}), sacrifice ({@mtg.rule 701.16}), and pay-life
/// ({@mtg.rule 118.8}) — plus comma-joined [Cost.CompoundCost]
/// composition. Other primitives (discard, exile-from-zone, mill,
/// loyalty, …) land as new arms when oracle text needs them.
public final class CostParser {
    private CostParser() {}

    /// "{T}" — tap-self ({@mtg.rule 118.12}).
    public static final Parser<Cost> TAP_SELF = string("{T}").thenReturn(Cost.TapSelf.TAP_SELF);

    /// One mana symbol: `{X}` where X is a run of valid inner
    /// mana characters (colors W/U/B/R/G/C, snow S, generic digits,
    /// variables X/Y/Z, Phyrexian P, hybrid slash). `T` and `Q` are
    /// excluded because those are tap/untap symbols, not mana.
    private static final Parser<String> MANA_SYMBOL = consecutive(
                    CharacterSet.charsIn("[WUBRGCSXYZP/0-9]"), "mana symbol")
            .immediatelyBetween("{", "}")
            .map(content -> "{" + content + "}");

    /// "{1}{G}{W}…" — one or more concatenated mana symbols. The
    /// raw oracle string is captured verbatim into
    /// [Cost.ManaCost#symbols] so the engine can defer mana-symbol
    /// modelling.
    public static final Parser<Cost> MANA_COST =
            MANA_SYMBOL.atLeastOnce().map(symbols -> new Cost.ManaCost(String.join("", symbols)));

    /// "Sacrifice X" — sacrifice permanents matching X.
    public static final Parser<Cost> SACRIFICE =
            phrase("Sacrifice").then(SelectorParser.SELECTOR).map(Cost.Sacrifice::new);

    /// "Pay N life" — pay N life.
    public static final Parser<Cost> PAY_LIFE =
            phrase("Pay").then(AMOUNT).followedBy(phrase("life")).map(Cost.PayLife::new);

    /// One primitive cost. Order: [#TAP_SELF] before [#MANA_COST]
    /// (the mana char-set doesn't include `T`, but explicit ordering
    /// documents the priority).
    private static final Parser<Cost> PRIMITIVE_COST = anyOf(TAP_SELF, MANA_COST, SACRIFICE, PAY_LIFE);

    /// Comma-separated cost list. A singleton list collapses to its
    /// only element; ≥2 wraps in [Cost.CompoundCost].
    public static final Parser<Cost> COST = PRIMITIVE_COST
            .atLeastOnceDelimitedBy(",")
            .map(list -> list.size() == 1 ? list.getFirst() : new Cost.CompoundCost(list));
}
