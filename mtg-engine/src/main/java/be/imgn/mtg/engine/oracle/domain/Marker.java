package be.imgn.mtg.engine.oracle.domain;

/// Marker-counter symbols that oracle text places directly on a player
/// rather than on a permanent. Distinct from [CounterType] (permanent-
/// bound) — a marker is carried by its owner and spent via pay costs.
/// Each enum's toString returns the canonical `{symbol}` form used in
/// oracle text.
public enum Marker {
    /// `{E}` — energy (Attune with Aether, …).
    ENERGY("{E}"),
    /// `{TK}` — ticket (Blorbian Buddy, …).
    TICKET("{TK}"),
    /// `{A}` — acorn (Acornelia, Fashionable Filcher).
    ACORN("{A}");

    private final String symbol;

    Marker(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return symbol;
    }
}
