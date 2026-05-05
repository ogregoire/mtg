package be.imgn.mtg.engine.oracle2.parser.selector;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.string;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.SelfSelector;
import be.imgn.mtg.engine.oracle2.parser.selector.ZoneParser.CardZoneHint;

/// Top-level [ObjectSelector] dispatch. Two arms permitted by the
/// domain: [SelfSelector] for the `~` self-reference token and
/// [ZoneSelector][be.imgn.mtg.engine.oracle2.domain.selector.ZoneSelector]
/// for everything else.
public final class ObjectSelectorParser {
    private ObjectSelectorParser() {}

    /// `~` literal — [SelfSelector#SELF].
    public static final Parser<ObjectSelector> SELF = string("~").thenReturn(SelfSelector.SELF);

    /// Hint-aware [ObjectSelector] dispatch. Effects that consume
    /// cards from a known zone (Discard → Hand, Mill → Library, …)
    /// pass their [CardZoneHint] so a bare "card" object-type can
    /// fall back to the right zone wrapper.
    public static Parser<ObjectSelector> objectSelector(CardZoneHint hint) {
        return anyOf(SELF, ZoneParser.zoneSelector(hint));
    }

    /// Default [ObjectSelector] entry — no hint. Bare "card" without
    /// an explicit zone clause fails. Wired into [Refs#OBJECT_SELECTOR]
    /// for top-level / recursive selector references.
    public static final Parser<ObjectSelector> OBJECT_SELECTOR = objectSelector(ZoneParser.NO_HINT);
}
