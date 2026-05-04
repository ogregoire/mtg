package be.imgn.mtg.engine.oracle2.parser.selector;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.string;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.SelfSelector;

/// Top-level [ObjectSelector] dispatch. Two arms permitted by the
/// domain: [SelfSelector] for the `~` self-reference token and
/// [ZoneSelector][be.imgn.mtg.engine.oracle2.domain.selector.ZoneSelector]
/// for everything else.
public final class ObjectSelectorParser {
    private ObjectSelectorParser() {}

    /// `~` literal — [SelfSelector#SELF].
    public static final Parser<ObjectSelector> SELF = string("~").thenReturn(SelfSelector.SELF);

    /// [ObjectSelector] entry — `~` first (single-character literal,
    /// unambiguous), then any [ZoneSelector] arm.
    public static final Parser<ObjectSelector> OBJECT_SELECTOR = anyOf(SELF, ZoneParser.ZONE_SELECTOR);
}
