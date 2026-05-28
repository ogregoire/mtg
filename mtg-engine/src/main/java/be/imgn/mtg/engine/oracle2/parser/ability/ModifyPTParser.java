package be.imgn.mtg.engine.oracle2.parser.ability;

import static be.imgn.mtg.engine.oracle2.parser.NumberParser.SIGNED_INT;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.ability.Ability;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;

/// Parser for [Ability.ModifyPT] — continuous P/T modifier abilities of
/// the form "[SUBJECT] get(s) ±N/±N." ({@mtg.rule 613.1d}, layer 7c).
/// The trailing period is consumed here so the parser is a self-
/// contained sentence at the [AbilityParser] dispatch level.
public final class ModifyPTParser {
    private ModifyPTParser() {}

    /// "[subject] get(s) ±N/±N." — both halves of the P/T delta are
    /// signed integers ({@code +1/+1}, {@code -1/-1}, {@code +0/+2},
    /// …), even when the value is zero.
    public static final Parser<Ability.ModifyPT> MODIFY_PT = sequence(
                    SelectorParser.SELECTOR.followedBy(phrase("get(s)")),
                    SIGNED_INT.followedBy(string("/")),
                    SIGNED_INT,
                    Ability.ModifyPT::new)
            .followedBy(phrase("."));
}
