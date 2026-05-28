package be.imgn.mtg.engine.oracle2.parser.ability;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.or;
import static com.google.common.labs.parse.Parser.sequence;

import java.util.Locale;
import java.util.stream.Stream;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.Supertype;
import be.imgn.mtg.engine.oracle2.domain.ability.Ability;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parser for [Ability.RemoveSupertype] — continuous supertype-removal
/// sentences of the form "[SUBJECT] [are|is] no longer [supertype]."
/// ({@mtg.rule 613.1d}, layer 4). The supertype is always written
/// lowercase in this position ("no longer snow"), so each
/// [Supertype] is matched from its lowercased `text()`.
public final class RemoveSupertypeParser {
    private RemoveSupertypeParser() {}

    /// Mid-sentence [Supertype] match — one arm per value, lowercased
    /// because "no longer X" is strictly mid-sentence.
    private static final Parser<Supertype> SUPERTYPE_LOWER = Stream.of(Supertype.values())
            .map(s -> phrase(s.text().toLowerCase(Locale.ROOT)).thenReturn(s))
            .collect(or());

    /// "[subject] [are|is] no longer [supertype]." — sentence-closing
    /// period.
    public static final Parser<Ability.RemoveSupertype> REMOVE_SUPERTYPE = sequence(
                    SelectorParser.SELECTOR.followedBy(phrase("[are|is] no longer")),
                    SUPERTYPE_LOWER,
                    Ability.RemoveSupertype::new)
            .followedBy(phrase("."));
}
