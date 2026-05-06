package be.imgn.mtg.engine.oracle2.parser;

import static be.imgn.mtg.engine.oracle2.parser.AmountParser.AMOUNT;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.digits;
import static com.google.common.labs.parse.Parser.one;
import static com.google.common.labs.parse.Parser.string;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Map;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.Cost;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Colored;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Colorless;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.ColorlessHybrid;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Generic;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Hybrid;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.HybridPhyrexian;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.MonoColorHybrid;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Phyrexian;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Snow;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Variable;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaType;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parsers for [Cost]. Today the grammar covers four primitive
/// shapes — mana cost ({@mtg.rule 107.4}, {@mtg.rule 117}), tap-self
/// ({@mtg.rule 118.12}), sacrifice ({@mtg.rule 701.16}), and pay-life
/// ({@mtg.rule 118.8}) — plus comma-joined [Cost.CompoundCost]
/// composition. Other primitives (discard, exile-from-zone, mill,
/// loyalty, …) land as new arms when oracle text needs them.
public final class CostParser {
    private CostParser() {}

    // ── Mana symbols ──────────────────────────────────────────────────

    /// The 10 hybrid pairings of {@mtg.rule 107.4e}, encoded
    /// structurally: each color pairs with the next two colors in
    /// WUBRG order. The same 10 pairings generate the 10 hybrid
    /// Phyrexian symbols of {@mtg.rule 107.4f}.
    private static final Map<Character, String> AFFIXES_PER_COLOR =
            Map.of('W', "UB", 'U', "BR", 'B', "RG", 'R', "GW", 'G', "WU");

    /// Maps a color letter to its [ManaType].
    private static final Map<Character, ManaType> COLORED_LETTERS = Map.of(
            'W', ManaType.WHITE,
            'U', ManaType.BLUE,
            'B', ManaType.BLACK,
            'R', ManaType.RED,
            'G', ManaType.GREEN);

    /// Per-color family. After the leading color letter, the only
    /// possible suffixes are:
    ///
    /// - `ε` → bare [Colored]
    /// - `/P` → [Phyrexian]
    /// - `/<partner>` → [Hybrid]
    /// - `/<partner>/P` → [HybridPhyrexian]
    ///
    /// Every (Colored, Phyrexian, Hybrid, HybridPhyrexian) value for
    /// this color is precomputed once at parser-build time and emitted
    /// via `thenReturn(constant)` — no parse-time `of(...)` lookup, no
    /// string concatenation. The leading `/` is consumed once by the
    /// outer `optionallyFollowedBy`; the inner `anyOf` then dispatches
    /// on the next single char.
    private static Parser<ManaSymbol> coloredFamily(char letter) {
        var m = requireNonNull(COLORED_LETTERS.get(letter));
        Colored bare = Colored.of(m);
        Phyrexian phyrexian = Phyrexian.of(m);

        var affixes = requireNonNull(AFFIXES_PER_COLOR.get(letter));
        char firstPartnerLetter = affixes.charAt(0);
        char secondPartnerLetter = affixes.charAt(1);
        var firstPartner = requireNonNull(COLORED_LETTERS.get(firstPartnerLetter));
        var secondPartner = requireNonNull(COLORED_LETTERS.get(secondPartnerLetter));

        Hybrid firstHybrid = Hybrid.of(m, firstPartner);
        HybridPhyrexian firstHybridPhyrexian = HybridPhyrexian.of(m, firstPartner);
        Hybrid secondHybrid = Hybrid.of(m, secondPartner);
        HybridPhyrexian secondHybridPhyrexian = HybridPhyrexian.of(m, secondPartner);

        Parser<ManaSymbol> afterSlash = anyOf(
                one('P').<ManaSymbol>thenReturn(phyrexian),
                one(firstPartnerLetter)
                        .<ManaSymbol>thenReturn(firstHybrid)
                        .optionallyFollowedBy(string("/P"), (_, _) -> firstHybridPhyrexian),
                one(secondPartnerLetter)
                        .<ManaSymbol>thenReturn(secondHybrid)
                        .optionallyFollowedBy(string("/P"), (_, _) -> secondHybridPhyrexian));

        return one(letter)
                .<ManaSymbol>thenReturn(bare)
                .optionallyFollowedBy(one('/').then(afterSlash), (_, withSuffix) -> withSuffix);
    }

    private static Parser<ManaSymbol> colored() {
        var arms = new ArrayList<Parser<ManaSymbol>>(AFFIXES_PER_COLOR.size());
        for (char c : AFFIXES_PER_COLOR.keySet()) {
            arms.add(coloredFamily(c));
        }
        return anyOf(arms.toArray(Parser[]::new));
    }

    /// All five colored families joined.
    private static final Parser<ManaSymbol> COLORED_FAMILIES = colored();

    /// Per-partner precomputed [ColorlessHybrid]: dispatches on the
    /// partner letter and yields the matching constant.
    private static Parser<ManaSymbol> colorlessHybridTail() {
        var arms = new ArrayList<Parser<ManaSymbol>>(COLORED_LETTERS.size());
        for (var e : COLORED_LETTERS.entrySet()) {
            arms.add(one(e.getKey()).<ManaSymbol>thenReturn(ColorlessHybrid.of(e.getValue())));
        }
        return anyOf(arms.toArray(Parser[]::new));
    }

    /// Per-partner precomputed [MonoColorHybrid].
    private static Parser<ManaSymbol> monoColorHybridTail() {
        var arms = new ArrayList<Parser<ManaSymbol>>(COLORED_LETTERS.size());
        for (var e : COLORED_LETTERS.entrySet()) {
            arms.add(one(e.getKey()).<ManaSymbol>thenReturn(MonoColorHybrid.of(e.getValue())));
        }
        return anyOf(arms.toArray(Parser[]::new));
    }

    /// `C` → [Colorless]; `C/<colored>` → precomputed [ColorlessHybrid].
    private static final Parser<ManaSymbol> COLORLESS_FAMILY = one('C').<ManaSymbol>thenReturn(Colorless.COLORLESS)
            .optionallyFollowedBy(one('/').then(colorlessHybridTail()), (_, hybrid) -> hybrid);

    /// `2/<colored>` → precomputed [MonoColorHybrid].
    private static final Parser<ManaSymbol> MONO_COLOR_HYBRID = string("2/").then(monoColorHybridTail());

    private static final Parser<ManaSymbol> SNOW = one('S').<ManaSymbol>thenReturn(Snow.SNOW);
    private static final Parser<ManaSymbol> VARIABLE = one('X').<ManaSymbol>thenReturn(Variable.X);
    private static final Parser<ManaSymbol> GENERIC = digits().<ManaSymbol>map(d -> new Generic(Integer.parseInt(d)));

    /// One mana symbol ({@mtg.rule 107.4}). Inner-content `anyOf`,
    /// wrapped in `{ }` once at the outer level. Order matters because
    /// dot-parse is non-backtracking:
    ///
    /// - [#MONO_COLOR_HYBRID] (`2/...`) precedes [#GENERIC] (`2`, `12`, …)
    /// - [#COLORLESS_FAMILY] internally absorbs `C/<colored>`, covering
    ///   both bare `{C}` and `{C/W}`-shaped symbols in one arm.
    public static final Parser<ManaSymbol> MANA_SYMBOL = Parser.<ManaSymbol>anyOf(
                    MONO_COLOR_HYBRID, COLORLESS_FAMILY, COLORED_FAMILIES, SNOW, VARIABLE, GENERIC)
            .immediatelyBetween("{", "}");

    // ── Costs ─────────────────────────────────────────────────────────

    /// "{T}" — tap-self ({@mtg.rule 118.12}).
    public static final Parser<Cost> TAP_SELF = string("{T}").thenReturn(Cost.TapSelf.TAP_SELF);

    /// "{1}{G}{W}…" — one or more concatenated [ManaSymbol]s.
    public static final Parser<Cost> MANA_COST = MANA_SYMBOL.atLeastOnce().map(Cost.ManaCost::new);

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
