package be.imgn.mtg.engine.mana.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import be.imgn.mtg.engine.mana.ManaCost;
import be.imgn.mtg.engine.mana.ManaSymbol;

/// Parser for mana cost strings like "{2}{W}{W}".
public final class ManaCostParser {

    /// The empty mana cost singleton.
    public static final ManaCost EMPTY = new DefaultManaCost(List.of());

    private static final Pattern SYMBOL_PATTERN = Pattern.compile("(\\{[^}]+})");
    private static final Pattern GENERIC_PATTERN = Pattern.compile("\\d+");

    private static final Map<String, ManaSymbol> SYMBOL_MAP = buildSymbolMap();

    private ManaCostParser() {}

    /// Parses a mana cost string.
    ///
    /// @param cost the cost string (e.g., "{2}{W}{W}", "{X}{R}")
    /// @return the parsed mana cost
    public static ManaCost parse(String cost) {
        if (cost == null || cost.isEmpty() || cost.equals("{0}")) {
            return EMPTY;
        }

        var symbols = new ArrayList<ManaSymbol>();
        var matcher = SYMBOL_PATTERN.matcher(cost);

        while (matcher.find()) {
            var content = matcher.group(1);
            symbols.add(parseSymbol(content));
        }

        if (symbols.isEmpty()) {
            return EMPTY;
        }

        return new DefaultManaCost(symbols);
    }

    private static ManaSymbol parseSymbol(String content) {
        // Check lookup map first
        var symbol = SYMBOL_MAP.get(content);
        if (symbol != null) {
            return symbol;
        }

        // Generic mana (numbers) must be handled dynamically
        if (GENERIC_PATTERN.matcher(content).matches()) {
            return new ManaSymbol.Generic(Integer.parseInt(content));
        }

        throw new IllegalArgumentException("Unknown mana symbol: {" + content + "}");
    }

    private static Map<String, ManaSymbol> buildSymbolMap() {
        return Stream.of(
                        ManaSymbol.Variable.values(),
                        ManaSymbol.Colorless.values(),
                        ManaSymbol.Snow.values(),
                        ManaSymbol.Colored.values(),
                        ManaSymbol.Phyrexian.values(),
                        ManaSymbol.HybridPhyrexian.values(),
                        ManaSymbol.Hybrid.values(),
                        ManaSymbol.MonoColorHybrid.values(),
                        ManaSymbol.ColorlessHybrid.values())
                .flatMap(Arrays::stream)
                .collect(Collectors.toUnmodifiableMap(ManaSymbol::notation, symbol -> symbol));
    }
}
