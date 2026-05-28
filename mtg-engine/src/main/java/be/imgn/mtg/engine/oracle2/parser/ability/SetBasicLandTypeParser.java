package be.imgn.mtg.engine.oracle2.parser.ability;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.or;
import static com.google.common.labs.parse.Parser.sequence;

import java.util.stream.Stream;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.BasicLandType;
import be.imgn.mtg.engine.oracle2.domain.ability.Ability;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parser for [Ability.SetBasicLandType] — continuous type-change
/// sentences of the form "[SUBJECT] [are|is] [BasicLandType]."
/// ({@mtg.rule 613.1d}, layer 4). Each [BasicLandType] is matched
/// from its `text()` template (`"Island(s)"`, `"Mountain(s)"`, …) so
/// both singular and plural forms work.
public final class SetBasicLandTypeParser {
    private SetBasicLandTypeParser() {}

    /// Bare [BasicLandType] match — one arm per value.
    private static final Parser<BasicLandType> BASIC_LAND_TYPE = Stream.of(BasicLandType.values())
            .map(t -> phrase(t.text()).thenReturn(t))
            .collect(or());

    /// "[subject] [are|is] [a|an]? [BasicLandType]." — sentence-
    /// closing period. The optional "a"/"an" article handles Evil
    /// Presence ("Enchanted land is a Swamp.") alongside the
    /// article-less plural forms (Harbinger of the Seas: "Nonbasic
    /// lands are Islands.").
    public static final Parser<Ability.SetBasicLandType> SET_BASIC_LAND_TYPE = sequence(
                    SelectorParser.SELECTOR.followedBy(phrase("[are|is] [a|an]?")),
                    BASIC_LAND_TYPE,
                    Ability.SetBasicLandType::new)
            .followedBy(phrase("."));
}
