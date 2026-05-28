package be.imgn.mtg.engine.oracle2.parser.selector;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.or;

import java.util.Locale;
import java.util.stream.Stream;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.Color;
import be.imgn.mtg.engine.oracle2.domain.selector.ColorSelector;

/// Parser for [ColorSelector]. Covers all five arms:
/// [ColorSelector.Is] (single color), [ColorSelector.IsNot] ("nonblue"),
/// [ColorSelector.Composition] (count-based: colorless / monocolored /
/// multicolored / all colors), [ColorSelector.Standard#CHOSEN] ("the
/// chosen color"), and [ColorSelector.SharesAColorWith] ("shares a
/// color with X").
///
/// Boolean composition ("blue or green") lives at the
/// [be.imgn.mtg.engine.oracle2.domain.selector.ObjectPropertySelector]
/// level via `OneOf` — not here.
public final class ColorSelectorParser {
    private ColorSelectorParser() {}

    /// Bare [Color] enum match — one arm per value, built directly
    /// from the value's `text()` template. Reused by callers that
    /// need a [Color] without the surrounding [ColorSelector.Is]
    /// wrapping (e.g. parameterised keywords like
    /// [be.imgn.mtg.engine.oracle2.domain.ability.Ability.Protection]
    /// or layer-5 color-setting effects).
    public static final Parser<Color> COLOR_NAME =
            Stream.of(Color.values()).map(c -> phrase(c.text()).thenReturn(c)).collect(or());

    /// Single-color match — [ColorSelector.Is].
    private static final Parser<ColorSelector.Is> COLOR_IS = COLOR_NAME.map(ColorSelector.Is::new);

    /// [ColorSelector.IsNot] — `Non{color}` (one-word). One arm per
    /// [Color] value, built from `"Non" + c.text().toLowerCase()`.
    /// "Noncolorless" is unattested in oracle and out of scope.
    private static final Parser<ColorSelector.IsNot> COLOR_IS_NOT = Stream.of(Color.values())
            .map(c -> phrase("Non" + c.text().toLowerCase(Locale.ROOT)).thenReturn(new ColorSelector.IsNot(c)))
            .collect(or());

    /// Count-based color predicates — [ColorSelector.Composition]. "all
    /// colors" must precede the bare-color forms because "all" is also
    /// a quantifier; the multi-word phrase wins by length.
    private static final Parser<ColorSelector.Composition> COMPOSITION = anyOf(
            phrase("All colors").thenReturn(ColorSelector.Composition.ALL_COLORS),
            phrase("Colorless").thenReturn(ColorSelector.Composition.COLORLESS),
            phrase("Monocolored").thenReturn(ColorSelector.Composition.MONOCOLORED),
            phrase("Multicolored").thenReturn(ColorSelector.Composition.MULTICOLORED));

    /// "the chosen color" — [ColorSelector.Standard#CHOSEN]. Back-
    /// reference to a preceding ChooseColor effect; resolved at game
    /// time.
    private static final Parser<ColorSelector.Standard> CHOSEN =
            phrase("the chosen color").thenReturn(ColorSelector.Standard.CHOSEN);

    /// "shares a color with X" — [ColorSelector.SharesAColorWith].
    /// Recursive on [ObjectSelector] via [Refs#OBJECT_SELECTOR].
    private static final Parser<ColorSelector.SharesAColorWith> SHARES_A_COLOR_WITH =
            phrase("shares a color with").then(Refs.OBJECT_SELECTOR).map(ColorSelector.SharesAColorWith::new);

    /// Top-level [ColorSelector]. Order matters:
    /// 1. `the chosen color` (specific multi-word prefix).
    /// 2. `shares a color with X` (specific multi-word prefix).
    /// 3. [#COMPOSITION] (multi-word/longer-prefix forms).
    /// 4. [#COLOR_IS_NOT] (`Non{color}` before bare `{color}`).
    /// 5. Bare [#COLOR_IS] (single-token color words).
    public static final Parser<ColorSelector> COLOR_SELECTOR =
            anyOf(CHOSEN, SHARES_A_COLOR_WITH, COMPOSITION, COLOR_IS_NOT, COLOR_IS);
}
