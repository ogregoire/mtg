package be.imgn.mtg.engine.oracle2.parser.ability;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.ability.Ability;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parser for [Ability.EnterTapped] — continuous replacement
/// sentences of the form "[SUBJECT] enter(s) tapped."
/// ({@mtg.rule 614}).
public final class EnterTappedParser {
    private EnterTappedParser() {}

    /// "[subject] enter(s) tapped." — verb inflection handled by the
    /// `(s)` plural marker on the phrase template; sentence-closing
    /// period is consumed here.
    public static final Parser<Ability.EnterTapped> ENTER_TAPPED = SelectorParser.SELECTOR
            .followedBy(phrase("enter(s) tapped"))
            .map(Ability.EnterTapped::new)
            .followedBy(phrase("."));
}
