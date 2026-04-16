package be.imgn.mtg.engine.oracle;

/// A color-related filter for game objects in oracle text.
public sealed interface ColorFilter {
    /// Matches objects of a specific color (e.g., "red creature").
    record Is(Color color) implements ColorFilter {}

    /// Matches objects not of a specific color (e.g., "nonblack creature").
    record Not(Color color) implements ColorFilter {}

    /// Matches colorless objects.
    record Colorless() implements ColorFilter {}

    /// Matches multicolored objects.
    record Multicolored() implements ColorFilter {}

    /// Matches monocolored objects.
    record Monocolored() implements ColorFilter {}

    // Positive colors
    ColorFilter WHITE = new Is(Color.WHITE);
    ColorFilter BLUE = new Is(Color.BLUE);
    ColorFilter BLACK = new Is(Color.BLACK);
    ColorFilter RED = new Is(Color.RED);
    ColorFilter GREEN = new Is(Color.GREEN);

    // Negated colors
    ColorFilter NON_WHITE = new Not(Color.WHITE);
    ColorFilter NON_BLUE = new Not(Color.BLUE);
    ColorFilter NON_BLACK = new Not(Color.BLACK);
    ColorFilter NON_RED = new Not(Color.RED);
    ColorFilter NON_GREEN = new Not(Color.GREEN);

    // Color categories
    ColorFilter COLORLESS = new Colorless();
    ColorFilter MULTICOLORED = new Multicolored();
    ColorFilter MONOCOLORED = new Monocolored();
}
