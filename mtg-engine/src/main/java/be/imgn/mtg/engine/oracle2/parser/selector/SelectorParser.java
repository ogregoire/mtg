package be.imgn.mtg.engine.oracle2.parser.selector;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.andList;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.andOrList;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.orList;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.sequence;
import static com.google.common.labs.parse.Parser.anyOf;

import java.util.List;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.CardType;
import be.imgn.mtg.engine.oracle2.domain.Quantifier;
import be.imgn.mtg.engine.oracle2.domain.selector.CardTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectPropertySelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.OtherObjectSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.QuantifierSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.domain.selector.SelfSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ZoneSelector;
import be.imgn.mtg.engine.oracle2.parser.selector.ZoneParser.CardZoneHint;

/// Top-level entry for the oracle2 selector grammar.
///
/// Wires the three forward-declared [Refs] rules to their concrete
/// parsers ([ObjectSelectorParser], [PlayerSelectorParser], and the
/// dispatched-here [#SELECTOR]) inside a single static block. Java's
/// class-loading guarantees this file's static initializer runs once,
/// so referencing [Refs#OBJECT_SELECTOR] / [Refs#PLAYER_SELECTOR]
/// from downstream parsers is safe at parse time.
///
/// Two layers, both built parametrically by
/// [#bareSelectorWith(CardZoneHint)] /
/// [#selectorWith(CardZoneHint)]:
///
/// 1. **bare** — `[Q]? [target]? X` or `[Q]? target X or Y[, or Z]`
///    (cross-axis target union). A [QuantifierParser#QUANTIFIER]
///    optional prefix, then the literal `target` keyword optionally,
///    then a [PlayerSelector] or [ObjectSelector].
/// 2. **top-level** — Oxford-comma list of bare selectors, with a
///    connector dispatch: `and/or` folds 2+ elements into
///    [Selector.OneOrMoreOf] (Chaotic Transformation), `and` folds
///    into [Selector.AllOf], and singletons collapse to the bare
///    selector so the common case stays unwrapped. The `and/or`
///    arm is tried first under a size-≥2 filter so it never steals
///    singleton or `and`-list matches.
///
/// The [CardZoneHint] flows down to [ZoneParser#zoneSelector] so a
/// bare "card" object-type can fall back to the calling effect's
/// natural zone (Discard → Hand, Mill → Library, …) instead of
/// failing the parse.
public final class SelectorParser {
    private SelectorParser() {}

    /// Implicit count for a singular noun phrase with no explicit
    /// quantifier word. "Target creature", "a card", "you" all
    /// resolve to one of the matching object/player. The wrapper
    /// is added unconditionally so the AST always carries the
    /// count explicitly — no engine code has to remember the
    /// "absent Quantifier means 1" convention.
    private static final Quantifier DEFAULT_COUNT = new Amount.Exact(1);

    /// Pre-built selector for `any target` ({@mtg.rule 115.4}) —
    /// `QuantifierSelector(Exact(1), OneOf([Target(creature),
    /// Target(player), Target(planeswalker), Target(battle)]))`.
    /// Constructed once at class-load time; must be declared before
    /// [#SELECTOR] so the static-init chain (`SELECTOR` →
    /// `selectorWith` → `bareSelectorWith`) sees a fully-initialized
    /// value rather than the field-default null.
    private static final QuantifierSelector ANY_TARGET = new QuantifierSelector(
            DEFAULT_COUNT,
            new Selector.OneOf(List.of(
                    new ObjectSelector.Target(permanentOfType(CardType.CREATURE)),
                    new PlayerSelector.Target(PlayerSelector.Anyone.ANYONE),
                    new ObjectSelector.Target(permanentOfType(CardType.PLANESWALKER)),
                    new ObjectSelector.Target(permanentOfType(CardType.BATTLE)))));

    private static ZoneSelector.Battlefield permanentOfType(CardType type) {
        return new ZoneSelector.Battlefield(new ObjectTypeSelector.Permanent(new CardTypeSelector.Is(type)));
    }

    /// Default top-level [Selector] — [ZoneParser#NO_HINT]
    /// semantics (bare "card" without an explicit zone clause
    /// fails). Effects that consume cards from a known zone build a
    /// hint-aware variant via [#selectorWith(CardZoneHint)].
    public static final Parser<Selector> SELECTOR = selectorWith(ZoneParser.NO_HINT);

    /// Bare (non-`and`-listed) form of [#SELECTOR] for recursive
    /// references. Same hint-less default as [#SELECTOR], but stops
    /// short of the top-level Oxford-comma fold so e.g. "attached
    /// to X" doesn't accidentally consume an outer ", and …"
    /// coordinator. Wired into [Refs#SELECTOR].
    private static final Parser<Selector> BARE_SELECTOR = bareSelectorWith(ZoneParser.NO_HINT);

    /// Build a top-level [Selector] parser for the given
    /// [CardZoneHint]. Composes [#bareSelectorWith] with the
    /// Oxford-comma connector dispatch: `and/or` → [Selector.OneOrMoreOf]
    /// (≥2 elements only), else `and` → [Selector.AllOf] (≥2) or the
    /// bare selector (1).
    public static Parser<Selector> selectorWith(CardZoneHint hint) {
        Parser<Selector> bare = bareSelectorWith(hint);
        return anyOf(
                // "X, Y, …, and/or Z" — ≥2 elements. Tried first; for
                // inputs ending in "and" (not "and/or"), the atomic
                // `string("and/or")` connector inside `andOrList`
                // fails cleanly without consuming, letting `andList`
                // recover.
                andOrList(bare)
                        .suchThat(l -> l.size() >= 2, "and/or-list (≥2)")
                        .<Selector>map(Selector.OneOrMoreOf::new),
                // "X" / "X, Y, …, and Z" — the standard `and`-list path.
                andList(bare).map(list -> list.size() == 1 ? list.getFirst() : new Selector.AllOf(list)));
    }

    /// Build a bare-selector parser (no top-level `and`-list fold)
    /// for the given [CardZoneHint]. Used as the leaf inside
    /// recursive selector references via [Refs#SELECTOR] and as the
    /// element of [#selectorWith]'s `and`-list.
    public static Parser<Selector> bareSelectorWith(CardZoneHint hint) {
        // Per-axis dispatch — Player tried first so the multi-word
        // player labels ("the active player", "your opponent") match
        // before the object grammar tries to interpret "the" or
        // "your" as something else.
        Parser<Selector> inner = anyOf(Refs.PLAYER_SELECTOR, ObjectSelectorParser.objectSelector(hint));

        // `[Q]? target X or Y[, or Z]` — cross-axis target union
        // (Firesong & Sunspeaker). Filtered to ≥2 elements so a
        // singleton "target creature" doesn't commit here. Always
        // wraps in QuantifierSelector — singular "target X or Y"
        // implies count = 1.
        Parser<Selector> targetOrCompound = Parser.sequence(
                QuantifierParser.QUANTIFIER.optional(),
                phrase("Target").then(orList(inner).suchThat(list -> list.size() >= 2, "target-or-compound")),
                (optQ, inners) -> {
                    Selector union = new Selector.OneOf(
                            inners.stream().map(SelectorParser::wrapTarget).toList());
                    return new QuantifierSelector(optQ.orElse(DEFAULT_COUNT), union);
                });

        // `[Q]? [target]? X` — single selector, no cross-axis or
        // top-level `and` composition. Always wraps in
        // QuantifierSelector — a bare singular noun ("target
        // creature", "you") implies count = 1.
        Parser<Selector> singleSelector = sequence(
                QuantifierParser.QUANTIFIER.optional(), phrase("Target").optional(), inner, (optQ, optTarget, inn) -> {
                    Selector wrapped = optTarget.isPresent() ? wrapTarget(inn) : inn;
                    return new QuantifierSelector(optQ.orElse(DEFAULT_COUNT), wrapped);
                });

        // `any target` — cross-axis target shortcut. Per CR 115.4,
        // valid targets are: creature, player, planeswalker, battle.
        // Emitted as the explicit OneOf union of those four Target
        // selectors wrapped in the standard quantifier (count = 1),
        // matching the shape produced by the explicit "target
        // creature or player" form. Built once at parser-load time.
        Parser<QuantifierSelector> anyTarget = phrase("any target").thenReturn(ANY_TARGET);

        // `Another (target)? X` — Kiora's Follower: "Untap another
        // target permanent." The "Another" qualifier carries two
        // effects per the [QuantifierSelector] docstring: a
        // `Quantifier.Exact(1)` (handled by the default count) and
        // an [OtherObjectSelector] property on the inner type
        // (handled by [#injectOther]). The "target" word, when
        // present, wraps the inner in an [ObjectSelector.Target].
        Parser<Boolean> anotherPrefix = phrase("Another")
                .then(phrase("Target").<Boolean>thenReturn(true).orElse(false));
        Parser<Selector> anotherTargetX =
                Parser.sequence(anotherPrefix, ObjectSelectorParser.objectSelector(hint), (hasTarget, inn) -> {
                    ObjectSelector withOther = injectOther(inn);
                    ObjectSelector wrapped = hasTarget ? new ObjectSelector.Target(withOther) : withOther;
                    return new QuantifierSelector(new Amount.Exact(1), wrapped);
                });

        // Order: ANY_TARGET first (specific multi-word prefix),
        // ANOTHER_TARGET_X next (also specific multi-word prefix),
        // TARGET_OR_COMPOUND (size-≥2 filter rejects singletons),
        // SINGLE_SELECTOR is the catch-all.
        return Parser.<Selector>anyOf(anyTarget, anotherTargetX, targetOrCompound, singleSelector);
    }

    /// Inject [OtherObjectSelector] (with `than = SELF`) into the
    /// inner object's type-property slot. Walks the
    /// [ZoneSelector.Battlefield] → [ObjectTypeSelector.Permanent]
    /// path that dominates "another (target) X" oracle text;
    /// other zone wrappers fall through unmodified for now (the AST
    /// is parser-emitted but missing the Other property — flag for
    /// extension when a non-battlefield "another" lands).
    private static ObjectSelector injectOther(ObjectSelector inner) {
        var other = new OtherObjectSelector(SelfSelector.SELF);
        return switch (inner) {
            case ZoneSelector.Battlefield bf
            when bf.of() instanceof ObjectTypeSelector.Permanent p ->
                new ZoneSelector.Battlefield(p.withWhere(mergeProperties(p.where(), other)));
            default -> inner;
        };
    }

    private static ObjectPropertySelector mergeProperties(
            ObjectPropertySelector existing, ObjectPropertySelector extra) {
        if (existing == ObjectPropertySelector.Anything.ANYTHING) return extra;
        return new ObjectPropertySelector.AllOf(List.of(existing, extra));
    }

    /// Wrap an inner selector in the matching per-axis Target arm.
    /// [Selector] is sealed; the per-axis `inner` only emits
    /// PlayerSelector or ObjectSelector arms — the other cases are
    /// defensive backstops.
    private static Selector wrapTarget(Selector inner) {
        return switch (inner) {
            case PlayerSelector p -> new PlayerSelector.Target(p);
            case ObjectSelector o -> new ObjectSelector.Target(o);
            case QuantifierSelector q ->
                throw new IllegalStateException("inner should never emit a QuantifierSelector, got: " + q);
            case Selector.AllOf a ->
                throw new IllegalStateException("inner should never emit a Selector.AllOf, got: " + a);
            case Selector.OneOf a ->
                throw new IllegalStateException("inner should never emit a Selector.OneOf, got: " + a);
            case Selector.OneOrMoreOf a ->
                throw new IllegalStateException("inner should never emit a Selector.OneOrMoreOf, got: " + a);
        };
    }

    static {
        Refs.OBJECT_SELECTOR.definedAs(ObjectSelectorParser.OBJECT_SELECTOR);
        Refs.PLAYER_SELECTOR.definedAs(PlayerSelectorParser.PLAYER_SELECTOR);
        // Recursive selector references want a single selector, not
        // a top-level `and`-list — wire them to the bare form so
        // the outer compound is reserved for the public SELECTOR
        // entrypoint.
        Refs.SELECTOR.definedAs(BARE_SELECTOR);
    }
}
