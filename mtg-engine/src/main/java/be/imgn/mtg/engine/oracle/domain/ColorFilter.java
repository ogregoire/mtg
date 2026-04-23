package be.imgn.mtg.engine.oracle.domain;

/// A color-related filter for game objects in oracle text.
public sealed interface ColorFilter {
    /// Matches objects of a specific color (e.g., "red creature").
    record Is(Color color) implements ColorFilter {}

    /// Matches objects not of a specific color (e.g., "nonblack creature").
    record Not(Color color) implements ColorFilter {}

    /// Matches colorless objects.
    enum Colorless implements ColorFilter {
        COLORLESS
    }

    /// Matches multicolored objects.
    enum Multicolored implements ColorFilter {
        MULTICOLORED
    }

    /// Matches monocolored objects.
    enum Monocolored implements ColorFilter {
        MONOCOLORED
    }

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

    // Color categories — enum singletons.
    ColorFilter COLORLESS = Colorless.COLORLESS;
    ColorFilter MULTICOLORED = Multicolored.MULTICOLORED;
    ColorFilter MONOCOLORED = Monocolored.MONOCOLORED;
}
