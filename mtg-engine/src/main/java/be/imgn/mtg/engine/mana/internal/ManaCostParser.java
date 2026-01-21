package be.imgn.mtg.engine.mana.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import be.imgn.mtg.engine.mana.ManaCost;
import be.imgn.mtg.engine.mana.ManaSymbol;

/// Parser for mana cost strings like "{2}{W}{W}".
public final class ManaCostParser {

    /// The empty mana cost singleton.
    public static final ManaCost EMPTY = new DefaultManaCost(List.of());

    private static final Pattern SYMBOL_PATTERN = Pattern.compile("\\{([^}]+)\\}");

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
        // Check for variable (X)
        if ("X".equals(content)) {
            return ManaSymbol.Variable.X;
        }

        // Check for colorless (C)
        if ("C".equals(content)) {
            return ManaSymbol.Colorless.INSTANCE;
        }

        // Check for snow (S)
        if ("S".equals(content)) {
            return ManaSymbol.Snow.INSTANCE;
        }

        // Check for single colored mana
        if (content.length() == 1) {
            return switch (content) {
                case "W" -> ManaSymbol.Colored.W;
                case "U" -> ManaSymbol.Colored.U;
                case "B" -> ManaSymbol.Colored.B;
                case "R" -> ManaSymbol.Colored.R;
                case "G" -> ManaSymbol.Colored.G;
                default -> throw new IllegalArgumentException("Unknown mana symbol: {" + content + "}");
            };
        }

        // Check for generic mana (number)
        if (content.matches("\\d+")) {
            return new ManaSymbol.Generic(Integer.parseInt(content));
        }

        // Check for Phyrexian mana (X/P)
        if (content.matches("[WUBRG]/P")) {
            return switch (content.charAt(0)) {
                case 'W' -> ManaSymbol.Phyrexian.W_P;
                case 'U' -> ManaSymbol.Phyrexian.U_P;
                case 'B' -> ManaSymbol.Phyrexian.B_P;
                case 'R' -> ManaSymbol.Phyrexian.R_P;
                case 'G' -> ManaSymbol.Phyrexian.G_P;
                default -> throw new IllegalArgumentException("Unknown Phyrexian mana: {" + content + "}");
            };
        }

        // Check for hybrid Phyrexian mana (X/Y/P)
        if (content.matches("[WUBRG]/[WUBRG]/P")) {
            var parts = content.split("/");
            var combo = parts[0] + parts[1] + "_P";
            return ManaSymbol.HybridPhyrexian.valueOf(combo);
        }

        // Check for mono-color hybrid (2/X)
        if (content.matches("2/[WUBRG]")) {
            return switch (content.charAt(2)) {
                case 'W' -> ManaSymbol.Hybrid.TWO_W;
                case 'U' -> ManaSymbol.Hybrid.TWO_U;
                case 'B' -> ManaSymbol.Hybrid.TWO_B;
                case 'R' -> ManaSymbol.Hybrid.TWO_R;
                case 'G' -> ManaSymbol.Hybrid.TWO_G;
                default -> throw new IllegalArgumentException("Unknown mono-hybrid mana: {" + content + "}");
            };
        }

        // Check for colorless hybrid (C/X)
        if (content.matches("C/[WUBRG]")) {
            return switch (content.charAt(2)) {
                case 'W' -> ManaSymbol.Hybrid.CW;
                case 'U' -> ManaSymbol.Hybrid.CU;
                case 'B' -> ManaSymbol.Hybrid.CB;
                case 'R' -> ManaSymbol.Hybrid.CR;
                case 'G' -> ManaSymbol.Hybrid.CG;
                default -> throw new IllegalArgumentException("Unknown colorless-hybrid mana: {" + content + "}");
            };
        }

        // Check for two-color hybrid (X/Y)
        if (content.matches("[WUBRG]/[WUBRG]")) {
            var combo = content.replace("/", "");
            // Try the exact combo first
            try {
                return ManaSymbol.Hybrid.valueOf(combo);
            } catch (IllegalArgumentException e) {
                // Try reversed combo
                var reversed = "" + combo.charAt(1) + combo.charAt(0);
                return ManaSymbol.Hybrid.valueOf(reversed);
            }
        }

        throw new IllegalArgumentException("Unknown mana symbol: {" + content + "}");
    }
}
