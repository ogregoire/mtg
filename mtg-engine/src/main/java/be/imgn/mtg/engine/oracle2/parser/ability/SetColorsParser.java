package be.imgn.mtg.engine.oracle2.parser.ability;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.ability.Ability;
import be.imgn.mtg.engine.oracle2.parser.selector.ColorSelectorParser;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parser for [Ability.SetColors] — continuous color-set sentences of
/// the form "[SUBJECT] are/is [color]." ({@mtg.rule 613.1c}, layer 5).
public final class SetColorsParser {
    private SetColorsParser() {}

    /// Single-color body — "black", "red", … — wrapped in
    /// [Ability.SetColors.Colors.Of].
    private static final Parser<Ability.SetColors.Colors.Of> COLORS_OF =
            ColorSelectorParser.COLOR_NAME.map(Ability.SetColors.Colors.Of::new);

    /// Bare-word color markers — "colorless", "all colors".
    private static final Parser<Ability.SetColors.Colors.Standard> COLORS_STANDARD = anyOf(
            phrase("colorless").thenReturn(Ability.SetColors.Colors.Standard.COLORLESS),
            phrase("all colors").thenReturn(Ability.SetColors.Colors.Standard.ALL_COLORS));

    private static final Parser<Ability.SetColors.Colors> COLORS = anyOf(COLORS_STANDARD, COLORS_OF);

    /// "[subject] [are|is] [colors]." — sentence-closing period.
    public static final Parser<Ability.SetColors> SET_COLORS = sequence(
                    SelectorParser.SELECTOR.followedBy(phrase("[are|is]")), COLORS, Ability.SetColors::new)
            .followedBy(phrase("."));
}
