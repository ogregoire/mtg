package be.imgn.mtg.engine.oracle2.parser.selector;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.orList;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.CombatStatus;
import be.imgn.mtg.engine.oracle2.domain.ObjectDesignation;
import be.imgn.mtg.engine.oracle2.domain.selector.AttachesToSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.CombatStatusSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ControlledBySelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectDesignationSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectPropertySelector;
import be.imgn.mtg.engine.oracle2.domain.selector.OtherObjectSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.OwnedBySelector;
import be.imgn.mtg.engine.oracle2.domain.selector.SelfSelector;

/// Parser for [ObjectPropertySelector] — predicates over a single
/// game object that fill the `where` slot of an
/// [be.imgn.mtg.engine.oracle2.domain.selector.ObjectTypeSelector] arm.
///
/// Three composition layers, outermost first:
///
/// 1. **AND** ([ObjectPropertySelector.AllOf]) — implicit juxtaposition
///    of multiple atomic properties: "legendary nontoken creature you
///    control" stacks `Is(LEGENDARY)` + (no token domain arm yet) +
///    `Is(CREATURE)` + `ControlledBy(YOU)`.
/// 2. **OR** ([ObjectPropertySelector.AnyOf]) — Oxford-comma "or"
///    alternation over atomic properties: "creature or planeswalker"
///    becomes `AnyOf(Is(CREATURE), Is(PLANESWALKER))`. Folded into
///    one slot of the outer AND chain.
/// 3. **Per-axis negation** lives inside each characteristic parser
///    ([TypeSelectorParser], [ColorSelectorParser]) via the per-axis
///    `IsNot` arms. The generic [ObjectPropertySelector.Not] is only
///    used here for non-axis negation ("you don't control").
public final class PropertyParser {
    private PropertyParser() {}

    // ── Postfix property arms ───────────────────────────────────────

    /// "X control(s)" — [ControlledBySelector]. The verb agrees with
    /// the player count ("you control" vs "X controls").
    private static final Parser<ObjectPropertySelector> CONTROLLED_BY =
            sequence(Refs.PLAYER_SELECTOR, phrase("control(s)"), (player, ignored) ->
                    (ObjectPropertySelector) new ControlledBySelector(player));

    /// "X own(s)" — [OwnedBySelector].
    private static final Parser<ObjectPropertySelector> OWNED_BY =
            sequence(Refs.PLAYER_SELECTOR, phrase("own(s)"), (player, ignored) ->
                    (ObjectPropertySelector) new OwnedBySelector(player));

    /// "X don't/doesn't control" → `Not(ControlledBy(X))`.
    private static final Parser<ObjectPropertySelector> NOT_CONTROLLED_BY =
            sequence(Refs.PLAYER_SELECTOR, phrase("[don't|doesn't] control"), (player, ignored) ->
                    (ObjectPropertySelector) new ObjectPropertySelector.Not(new ControlledBySelector(player)));

    /// "X don't/doesn't own" → `Not(OwnedBy(X))`.
    private static final Parser<ObjectPropertySelector> NOT_OWNED_BY =
            sequence(Refs.PLAYER_SELECTOR, phrase("[don't|doesn't] own"), (player, ignored) ->
                    (ObjectPropertySelector) new ObjectPropertySelector.Not(new OwnedBySelector(player)));

    // ── Combat-status / designation atoms ───────────────────────────

    /// [CombatStatusSelector] — the five combat-status adjectives.
    /// "Unblocked" must precede "Blocked" and "Attacked" must precede
    /// "Attacking" so the longer prefixes win.
    private static final Parser<CombatStatusSelector> COMBAT_STATUS = anyOf(
                    phrase("Unblocked").thenReturn(CombatStatus.UNBLOCKED),
                    phrase("Attacked").thenReturn(CombatStatus.ATTACKED),
                    phrase("Attacking").thenReturn(CombatStatus.ATTACKING),
                    phrase("Blocking").thenReturn(CombatStatus.BLOCKING),
                    phrase("Blocked").thenReturn(CombatStatus.BLOCKED))
            .map(CombatStatusSelector::new);

    /// [ObjectDesignationSelector] — "commander", "Ring-bearer".
    private static final Parser<ObjectDesignationSelector> DESIGNATION = anyOf(
                    phrase("Commander").thenReturn(ObjectDesignation.COMMANDER),
                    phrase("Ring-bearer").thenReturn(ObjectDesignation.RING_BEARER))
            .map(ObjectDesignationSelector::new);

    // ── Aura / Equipment / Fortification host arms ──────────────────

    /// "enchanted" — [ObjectPropertySelector.Enchanted] with `by =
    /// SELF`. The `by` slot points at the enchantment; oracle text
    /// referring to "enchanted creature" implicitly references the
    /// card's own self (`~`).
    private static final Parser<ObjectPropertySelector> ENCHANTED =
            phrase("Enchanted").thenReturn(new ObjectPropertySelector.Enchanted(SelfSelector.SELF));

    /// "equipped" — [ObjectPropertySelector.Equipped].
    private static final Parser<ObjectPropertySelector> EQUIPPED =
            phrase("Equipped").thenReturn(new ObjectPropertySelector.Equipped(SelfSelector.SELF));

    /// "fortified" — [ObjectPropertySelector.Fortified].
    private static final Parser<ObjectPropertySelector> FORTIFIED =
            phrase("Fortified").thenReturn(new ObjectPropertySelector.Fortified(SelfSelector.SELF));

    // ── Relational arms ─────────────────────────────────────────────

    /// "attached to X" — [AttachesToSelector]. Recursive on [Selector]
    /// (the bearer can be either an object or a player).
    private static final Parser<ObjectPropertySelector> ATTACHED_TO =
            phrase("attached to").then(Refs.SELECTOR).map(AttachesToSelector::new);

    /// "other" — [OtherObjectSelector] with `than = SELF`. Combined
    /// with a quantifier upstream ("another creature" → Quantifier(1,
    /// Other(...))). Treated as a property here because it composes
    /// with other adjectives ("another legendary creature").
    private static final Parser<ObjectPropertySelector> OTHER =
            phrase("Other").thenReturn(new OtherObjectSelector(SelfSelector.SELF));

    // ── Atomic property dispatch ────────────────────────────────────

    /// One indivisible property — no AND/OR composition. Order is
    /// longest-match-first: "X don't control" before "X control" so
    /// the longer match wins; bare characteristics last (they include
    /// per-axis negation via `IsNot`).
    private static final Parser<ObjectPropertySelector> ATOMIC = anyOf(
            // Compound postfix forms — "X don't control" before
            // "X control" so the longer match wins.
            NOT_CONTROLLED_BY,
            NOT_OWNED_BY,
            CONTROLLED_BY,
            OWNED_BY,
            // Aura/Equipment/Fortification/Sticker host markers.
            ENCHANTED,
            EQUIPPED,
            FORTIFIED,
            StickerSelectorParser.STICKER_SELECTOR,
            // Relational arms.
            ATTACHED_TO,
            OTHER,
            // Combat status / designations — multi-token labels with no
            // overlap with characteristics.
            COMBAT_STATUS,
            DESIGNATION,
            // Object-property arms with their own dispatch.
            StatusSelectorParser.STATUS_SELECTOR,
            ObjectCounterSelectorParser.OBJECT_COUNTER_SELECTOR,
            // Numeric-aspect comparisons — handles "with power 3 or
            // greater" alone and shared-matcher disjunctions like
            // "with power or toughness 1 or less". Subsumes the per-
            // axis `Has*` arms.
            NumericAspectParser.NUMERIC_ASPECT,
            // Characteristics — both positive (`Is`) and per-axis
            // negation (`IsNot`); the dispatch lives in the
            // characteristic parsers.
            CharacteristicParser.CHARACTERISTIC_SELECTOR.map(c -> c));

    // ── Composition: OR over atomics, AND over OR-groups ────────────

    /// Oxford-comma "or" alternation over atomic properties. Returns
    /// a single atom if the list has one element, [AnyOf][ObjectPropertySelector.AnyOf]
    /// otherwise.
    private static final Parser<ObjectPropertySelector> OR_GROUP =
            orList(ATOMIC).map(list -> list.size() == 1 ? list.getFirst() : new ObjectPropertySelector.AnyOf(list));

    /// Implicit AND over juxtaposed OR-groups, with optional comma
    /// separator between groups ("noncreature, nonland permanent").
    /// Returns a single OR-group if only one is present, [AllOf][ObjectPropertySelector.AllOf]
    /// otherwise.
    public static final Parser<ObjectPropertySelector> PROPERTY = OR_GROUP.optionallyFollowedBy(",")
            .atLeastOnce()
            .map(list -> list.size() == 1 ? list.getFirst() : new ObjectPropertySelector.AllOf(list));
}
