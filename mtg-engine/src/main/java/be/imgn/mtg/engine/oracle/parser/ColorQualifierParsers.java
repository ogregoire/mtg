package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;

import java.util.ArrayList;
import java.util.List;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle.domain.ColorMatcher;
import be.imgn.mtg.engine.oracle.domain.Selector;

/// Color-qualifier parsers and the post-collection merge step that
/// folds multiple adjacent color qualifiers into one. Extracted from
/// [SelectorParsers] so the [ColorMatcher] boolean tree — and the
/// invariant that a [Selector] holds at most one [Selector.Qualifier.Colors]
/// — lives in one place.
final class ColorQualifierParsers {
    private ColorQualifierParsers() {}

    /// One color atom — `Blue`, `Nonblue`, `Multicolored`, `Colorless`, …
    /// Returns a [ColorMatcher] atom (Is / Not / enum singleton).
    /// Negations precede positives so the longer "Nonblue" wins over
    /// the literal "blue" inside the same dispatch.
    static final Parser<ColorMatcher> COLOR_FILTER = Parser.<ColorMatcher>anyOf(
            phrase("Nonwhite").thenReturn(ColorMatcher.NON_WHITE),
            phrase("Nonblue").thenReturn(ColorMatcher.NON_BLUE),
            phrase("Nonblack").thenReturn(ColorMatcher.NON_BLACK),
            phrase("Nonred").thenReturn(ColorMatcher.NON_RED),
            phrase("Nongreen").thenReturn(ColorMatcher.NON_GREEN),
            phrase("Colorless").thenReturn(ColorMatcher.COLORLESS),
            phrase("Multicolored").thenReturn(ColorMatcher.MULTICOLORED),
            phrase("Monocolored").thenReturn(ColorMatcher.MONOCOLORED),
            phrase("White").thenReturn(ColorMatcher.WHITE),
            phrase("Blue").thenReturn(ColorMatcher.BLUE),
            phrase("Black").thenReturn(ColorMatcher.BLACK),
            phrase("Red").thenReturn(ColorMatcher.RED),
            phrase("Green").thenReturn(ColorMatcher.GREEN));

    /// "[color] [or [color]]…" or "[color] and/or [color]" — a single
    /// color qualifier. The or-list arm collapses 1-element lists to a
    /// bare atom (no `Any` wrapper for "blue creature"). The
    /// and/or-list arm fires only on 2+ elements ("blue and/or green",
    /// inclusive-or) and emits `Any` since "and/or" is semantically
    /// disjunctive.
    static final Parser<Selector.Qualifier> COLOR_Q = anyOf(
            MtgParsers.andOrList(COLOR_FILTER)
                    .suchThat(l -> l.size() >= 2, "and/or-list of colors")
                    .map(l -> (Selector.Qualifier) new Selector.Qualifier.Colors(new ColorMatcher.Any(l))),
            MtgParsers.orList(COLOR_FILTER).map(l -> (Selector.Qualifier)
                    new Selector.Qualifier.Colors(l.size() == 1 ? l.getFirst() : new ColorMatcher.Any(l))));

    /// Folds every [Selector.Qualifier.Colors] in `qs` into a single
    /// `Colors(All[...])` qualifier, placed at the position of the first
    /// color qualifier; non-color qualifiers keep their relative order.
    /// 0–1 color qualifiers → input returned unchanged. Idempotent.
    ///
    /// This is what gives "nonblue, nongreen creature" the structured
    /// `Colors(All[Not(BLUE), Not(GREEN)])` shape — the parser itself
    /// emits two `Colors(Not(...))` qualifiers (the comma between them
    /// is consumed by [SelectorParsers#QUALIFIER_LIST]'s separator),
    /// and this fold collapses them into one conjunction.
    static List<Selector.Qualifier> mergeColorQualifiers(List<Selector.Qualifier> qs) {
        var matchers = new ArrayList<ColorMatcher>();
        for (var q : qs) {
            if (q instanceof Selector.Qualifier.Colors c) matchers.add(c.matcher());
        }
        if (matchers.size() <= 1) return qs;
        var merged = new Selector.Qualifier.Colors(new ColorMatcher.All(List.copyOf(matchers)));
        var out = new ArrayList<Selector.Qualifier>(qs.size() - matchers.size() + 1);
        var inserted = false;
        for (var q : qs) {
            if (q instanceof Selector.Qualifier.Colors) {
                if (!inserted) {
                    out.add(merged);
                    inserted = true;
                }
            } else {
                out.add(q);
            }
        }
        return out;
    }
}
