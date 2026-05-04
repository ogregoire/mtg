package be.imgn.mtg.engine.oracle2.parser.selector;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// Forward-declared [Parser.Rule] handles for the three recursive
/// selector axes. Downstream parsers (Color/Subtype/CardType
/// shares-with, Other*, AttachesTo, Controlled-by/Owned-by, …)
/// reference these slots at construction time; [SelectorParser] wires
/// them via `definedAs(...)` once all leaf parsers exist.
///
/// Package-private — this is a wiring detail of the selector parser
/// tree, not a public API.
final class Refs {
    private Refs() {}

    /// Top-level [Selector] — wired in [SelectorParser].
    static final Parser.Rule<Selector> SELECTOR = new Parser.Rule<>();

    /// [ObjectSelector] — wired to [ObjectSelectorParser#OBJECT_SELECTOR].
    static final Parser.Rule<ObjectSelector> OBJECT_SELECTOR = new Parser.Rule<>();

    /// [PlayerSelector] — wired to [PlayerSelectorParser#PLAYER_SELECTOR].
    static final Parser.Rule<PlayerSelector> PLAYER_SELECTOR = new Parser.Rule<>();
}
