package be.imgn.mtg.engine.oracle2.parser.selector;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.string;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.SelfSelector;
import be.imgn.mtg.engine.oracle2.parser.selector.ZoneParser.CardZoneHint;

/// Top-level [ObjectSelector] dispatch. Three leaf arms:
/// [SelfSelector] for the `~` self-reference token, [ObjectSelector.Bound]
/// for anaphoric pronouns ("it", "that creature"), and
/// [ZoneSelector][be.imgn.mtg.engine.oracle2.domain.selector.ZoneSelector]
/// for everything else.
public final class ObjectSelectorParser {
    private ObjectSelectorParser() {}

    /// [SelfSelector#SELF] reference forms — the `~` placeholder
    /// (substituted from the card's printed name by [OracleParser])
    /// and the "this creature" / "this permanent" / "this artifact" /
    /// "this enchantment" / "this land" English forms. The type word
    /// is cosmetic — oracle text uses whichever matches the card's
    /// own type — so all forms collapse to the same singleton.
    /// [SELF] is strictly the *source* of the ability; anaphoric
    /// "it" and "that creature" live in [#BOUND] instead.
    public static final Parser<ObjectSelector> SELF = anyOf(
            string("~").thenReturn(SelfSelector.SELF),
            phrase("This [creature|permanent|artifact|enchantment|land]").thenReturn(SelfSelector.SELF));

    /// Anaphoric pronouns and demonstratives on the object axis —
    /// emit [ObjectSelector.Bound#OBJECT] regardless of surface
    /// form. All collapse to the same marker because the antecedent
    /// (and its type) is recoverable from the enclosing binding
    /// scope at engine evaluation time; the type word in "that
    /// creature" / "that planeswalker" is redundant for the common
    /// case (the antecedent is already known to be one of those
    /// types).
    public static final Parser<ObjectSelector> BOUND = anyOf(
            phrase("it").thenReturn(ObjectSelector.Bound.OBJECT),
            phrase("itself").thenReturn(ObjectSelector.Bound.OBJECT),
            phrase("them").thenReturn(ObjectSelector.Bound.OBJECT),
            phrase("themselves").thenReturn(ObjectSelector.Bound.OBJECT),
            phrase("that [creature|permanent|artifact|enchantment|land|planeswalker|battle|instant|sorcery|spell]")
                    .thenReturn(ObjectSelector.Bound.OBJECT));

    /// Hint-aware [ObjectSelector] dispatch. Effects that consume
    /// cards from a known zone (Discard → Hand, Mill → Library, …)
    /// pass their [CardZoneHint] so a bare "card" object-type can
    /// fall back to the right zone wrapper. [#SELF] and [#BOUND]
    /// are tried before the zone parser so their multi-word
    /// prefixes ("This creature", "that planeswalker") don't get
    /// consumed by the zone grammar.
    public static Parser<ObjectSelector> objectSelector(CardZoneHint hint) {
        return anyOf(SELF, BOUND, ZoneParser.zoneSelector(hint));
    }

    /// Default [ObjectSelector] entry — no hint. Bare "card" without
    /// an explicit zone clause fails. Wired into [Refs#OBJECT_SELECTOR]
    /// for top-level / recursive selector references.
    public static final Parser<ObjectSelector> OBJECT_SELECTOR = objectSelector(ZoneParser.NO_HINT);
}
