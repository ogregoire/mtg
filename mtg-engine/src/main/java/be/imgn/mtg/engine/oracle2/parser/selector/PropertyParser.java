package be.imgn.mtg.engine.oracle2.parser.selector;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.orList;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;

import java.util.ArrayList;
import java.util.List;

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
/// 2. **OR** ([ObjectPropertySelector.OneOf]) — Oxford-comma "or"
///    alternation over atomic properties: "creature or planeswalker"
///    becomes `OneOf(Is(CREATURE), Is(PLANESWALKER))`. Folded into
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

    /// "X cast(s)" — [ObjectPropertySelector.CastBy]. Stack-side
    /// predicate; composes on spell selectors ("Spells you cast",
    /// "instants your opponents cast").
    private static final Parser<ObjectPropertySelector> CAST_BY =
            sequence(Refs.PLAYER_SELECTOR, phrase("cast(s)"), (player, ignored) ->
                    (ObjectPropertySelector) new ObjectPropertySelector.CastBy(player));

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
            CAST_BY,
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
            // Ability/keyword forms — handles "with flying", "with
            // flying or reach", "with flying and vigilance", and
            // "with no abilities". Must precede NUMERIC_ASPECT
            // because both start with `phrase("with")`; ability
            // keywords are a closed set and a successful match here
            // shortcircuits cleanly.
            AbilitySelectorParser.ABILITY_SELECTOR,
            // Numeric-aspect comparisons — handles "with power 3 or
            // greater" alone and shared-matcher disjunctions like
            // "with power or toughness 1 or less". Subsumes the per-
            // axis `Has*` arms.
            NumericAspectParser.NUMERIC_ASPECT,
            // Characteristics — both positive (`Is`) and per-axis
            // negation (`IsNot`); the dispatch lives in the
            // characteristic parsers. CharacteristicSelector is an
            // ObjectPropertySelector subtype — anyOf widens it.
            CharacteristicParser.CHARACTERISTIC_SELECTOR);

    // ── Composition: OR over atomics, AND over OR-groups ────────────

    /// Oxford-comma "or" alternation over atomic properties. Returns
    /// a single atom if the list has one element,
    /// [ObjectPropertySelector.OneOf] otherwise.
    private static final Parser<ObjectPropertySelector> OR_GROUP =
            orList(ATOMIC).map(list -> list.size() == 1 ? list.getFirst() : new ObjectPropertySelector.OneOf(list));

    /// Tail OR-group, optionally preceded by a comma. The
    /// `string(",").then(OR_GROUP)` arm is atomic — `anyOf`
    /// backtracks when the OR_GROUP after the comma fails, so a
    /// trailing comma that belongs to an enclosing list ("up to one
    /// target artifact, up to one target creature, …") is not eaten.
    private static final Parser<ObjectPropertySelector> TAIL_PROPERTY =
            anyOf(string(",").then(OR_GROUP), OR_GROUP);

    /// Implicit AND over juxtaposed OR-groups. Returns a single
    /// OR-group if only one is present,
    /// [ObjectPropertySelector.AllOf] otherwise.
    ///
    /// **Separator** — juxtaposed groups may be either space-separated
    /// ("nonland permanent") or comma-separated without a final
    /// connector ("non-Vampire, non-Werewolf, non-Zombie creature").
    /// The comma is consumed atomically with the following group via
    /// [#TAIL_PROPERTY], so a stray trailing comma stays with the
    /// enclosing list combinator instead of being absorbed here.
    public static final Parser<ObjectPropertySelector> PROPERTY =
            sequence(OR_GROUP, TAIL_PROPERTY.zeroOrMore(), (head, tail) -> {
                if (tail.isEmpty()) return head;
                var all = new ArrayList<ObjectPropertySelector>(1 + tail.size());
                all.add(head);
                all.addAll(tail);
                return new ObjectPropertySelector.AllOf(List.copyOf(all));
            });
}
