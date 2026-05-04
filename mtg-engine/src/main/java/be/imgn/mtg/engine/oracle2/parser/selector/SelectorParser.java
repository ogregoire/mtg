package be.imgn.mtg.engine.oracle2.parser.selector;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.selector.QuantifierSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.domain.selector.Target;

/// Top-level entry for the oracle2 selector grammar.
///
/// Wires the three forward-declared [Refs] rules to their concrete
/// parsers ([ObjectSelectorParser], [PlayerSelectorParser], and the
/// dispatched-here [#SELECTOR]) inside a single static block. Order
/// matters: the rules must be `definedAs(...)`'d in the order their
/// dependents need them. Java's class-loading guarantees this file's
/// static initializer runs once, so referencing
/// [Refs#OBJECT_SELECTOR] / [Refs#PLAYER_SELECTOR] from downstream
/// parsers is safe at parse time.
///
/// Top-level dispatch (longest-match-first):
///
/// 1. [Refs#PLAYER_SELECTOR] / [Refs#OBJECT_SELECTOR] — bare nouns
///    that absorb their own articles ("you", "an opponent", "the
///    active player", "creature you control").
/// 2. `target X` — [Target] wrapper around an inner selector.
/// 3. `Q target X` — [QuantifierSelector] wrapping `Target(X)` (e.g.,
///    "two target creatures" → `Quantifier(2, Target(creature))`).
/// 4. `Q X` — [QuantifierSelector] over a bare inner selector ("two
///    creatures", "all creatures you control").
public final class SelectorParser {
    private SelectorParser() {}

    /// Bare inner selector — either a [PlayerSelector] or an
    /// [ObjectSelector]. Player tried first so the multi-word player
    /// labels ("the active player", "an opponent") match before the
    /// object grammar tries to interpret "the" or "an" as something else.
    private static final Parser<Selector> INNER = Parser.anyOf(Refs.PLAYER_SELECTOR, Refs.OBJECT_SELECTOR);

    /// Top-level [Selector]. See class doc for dispatch ordering.
    public static final Parser<Selector> SELECTOR = anyOf(
            INNER,
            phrase("Target").then(INNER).map(Target::new),
            sequence(QuantifierParser.QUANTIFIER, phrase("target").then(INNER), (q, inner) ->
                    (Selector) new QuantifierSelector(q, new Target(inner))),
            sequence(QuantifierParser.QUANTIFIER, INNER, (q, inner) -> (Selector) new QuantifierSelector(q, inner)));

    static {
        Refs.OBJECT_SELECTOR.definedAs(ObjectSelectorParser.OBJECT_SELECTOR);
        Refs.PLAYER_SELECTOR.definedAs(PlayerSelectorParser.PLAYER_SELECTOR);
        Refs.SELECTOR.definedAs(SELECTOR);
    }
}
