package be.imgn.mtg.engine.oracle2.parser.ability;

import static be.imgn.mtg.engine.oracle2.parser.AmountParser.AMOUNT;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.string;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.ability.Cost;
import be.imgn.mtg.engine.oracle2.domain.selector.SelfSelector;
import be.imgn.mtg.engine.oracle2.parser.ManaParser;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;
import be.imgn.mtg.engine.oracle2.parser.selector.ZoneParser;

/// Parsers for [Cost]. Today the grammar covers four primitive
/// shapes — mana cost ({@mtg.rule 107.4}, {@mtg.rule 202}), tap-self
/// ({@mtg.rule 107.5}), sacrifice ({@mtg.rule 701.21}), and pay-life
/// ({@mtg.rule 118.3b}) — plus comma-joined [Cost.CompoundCost]
/// composition. Other primitives (discard, exile-from-zone, mill,
/// loyalty, …) land as new arms when oracle text needs them.
///
/// Mana-symbol parsing lives in [ManaParser] and is reused by
/// non-cost callers as well (e.g. mana-producing abilities).
public final class CostParser {
    private CostParser() {}

    /// "{T}" — tap this permanent ({@mtg.rule 107.5}). Returns a
    /// constant `Cost.Tap(SELF)` allocated once at parser-build time
    /// and reused on every successful parse via `thenReturn`.
    public static final Parser<Cost.Tap> TAP = string("{T}").thenReturn(new Cost.Tap(SelfSelector.SELF));

    /// "{1}{G}{W}…" — one or more concatenated [be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol]s.
    public static final Parser<Cost.ManaCost> MANA_COST = ManaParser.SYMBOLS.map(Cost.ManaCost::new);

    /// "Sacrifice X" — sacrifice permanents matching X.
    public static final Parser<Cost.Sacrifice> SACRIFICE =
            phrase("Sacrifice").then(SelectorParser.SELECTOR).map(Cost.Sacrifice::new);

    /// "Pay N life" — pay N life.
    public static final Parser<Cost.PayLife> PAY_LIFE =
            phrase("Pay").then(AMOUNT).followedBy(phrase("life")).map(Cost.PayLife::new);

    /// "Discard X" — discard cards matching X ({@mtg.rule 118.5}). A
    /// bare `card` noun phrase has no explicit zone, so the
    /// [ZoneParser#HAND_OF_ANYONE] hint resolves it to a hand-zone
    /// selector (the controller's hand at resolution time).
    public static final Parser<Cost.Discard> DISCARD = phrase("Discard")
            .then(SelectorParser.selectorWith(ZoneParser.HAND_OF_ANYONE))
            .map(Cost.Discard::new);

    /// One primitive cost. Order: [#TAP] before [#MANA_COST]
    /// (the mana char-set doesn't include `T`, but explicit ordering
    /// documents the priority).
    private static final Parser<Cost> PRIMITIVE_COST = anyOf(TAP, MANA_COST, SACRIFICE, PAY_LIFE, DISCARD);

    /// Comma-separated cost list. A singleton list collapses to its
    /// only element; ≥2 wraps in [Cost.CompoundCost].
    public static final Parser<Cost> COST = PRIMITIVE_COST
            .atLeastOnceDelimitedBy(",")
            .map(list -> list.size() == 1 ? list.getFirst() : new Cost.CompoundCost(list));
}
