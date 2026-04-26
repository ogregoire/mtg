package be.imgn.mtg.engine.oracle.domain;

import java.util.List;

/// Boolean predicate over an object's colors. Atoms cover the basic
/// "is this color" / "is not this color" / "monocolored" / "multicolored"
/// / "colorless" forms; [Any] and [All] combine them.
///
/// Used by [Selector.Qualifier.Colors] to express the full color
/// constraint on a selector — a single atom for "blue creature",
/// `Any` for the disjunctive `blue or green creature`, `All` for the
/// conjunctive `nonblue, nongreen creature` (every color individually
/// excluded).
public sealed interface ColorMatcher {

    /// Matches objects of a specific color (e.g., "red creature").
    record Is(Color color) implements ColorMatcher {}

    /// Matches objects not of a specific color (e.g., "nonblack creature").
    record Not(Color color) implements ColorMatcher {}

    /// Matches colorless objects.
    enum Colorless implements ColorMatcher {
        COLORLESS
    }

    /// Matches multicolored objects.
    enum Multicolored implements ColorMatcher {
        MULTICOLORED
    }

    /// Matches monocolored objects.
    enum Monocolored implements ColorMatcher {
        MONOCOLORED
    }

    /// Disjunction — matches when *any* contained matcher matches.
    /// Emitted by the or-list arm of the color qualifier parser
    /// ("blue or green creature").
    record Any(List<ColorMatcher> matchers) implements ColorMatcher {}

    /// Conjunction — matches when *every* contained matcher matches.
    /// Emitted by the qualifier-list merge step that folds multiple
    /// adjacent color qualifiers into one ("nonblue, nongreen creature").
    record All(List<ColorMatcher> matchers) implements ColorMatcher {}

    // Positive colors
    ColorMatcher WHITE = new Is(Color.WHITE);
    ColorMatcher BLUE = new Is(Color.BLUE);
    ColorMatcher BLACK = new Is(Color.BLACK);
    ColorMatcher RED = new Is(Color.RED);
    ColorMatcher GREEN = new Is(Color.GREEN);

    // Negated colors
    ColorMatcher NON_WHITE = new Not(Color.WHITE);
    ColorMatcher NON_BLUE = new Not(Color.BLUE);
    ColorMatcher NON_BLACK = new Not(Color.BLACK);
    ColorMatcher NON_RED = new Not(Color.RED);
    ColorMatcher NON_GREEN = new Not(Color.GREEN);

    // Color categories — enum singletons.
    ColorMatcher COLORLESS = Colorless.COLORLESS;
    ColorMatcher MULTICOLORED = Multicolored.MULTICOLORED;
    ColorMatcher MONOCOLORED = Monocolored.MONOCOLORED;
}
