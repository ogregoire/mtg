package be.imgn.mtg.engine.oracle2.parser.selector;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.CombatRole;
import be.imgn.mtg.engine.oracle2.domain.PlayerDesignation;
import be.imgn.mtg.engine.oracle2.domain.PlayerRelation;
import be.imgn.mtg.engine.oracle2.domain.PlayerTurnRole;
import be.imgn.mtg.engine.oracle2.domain.selector.CombatRoleSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ControllerSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.OtherPlayerSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.OwnerSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerDesignationSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerRelationSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerTurnRoleSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.SelfSelector;

/// Parser for [PlayerSelector] — every arm of the sealed hierarchy
/// except [PlayerCounterSelector][be.imgn.mtg.engine.oracle2.domain.selector.PlayerCounterSelector],
/// which is a bare marker interface in the domain (see plan gap list).
///
/// **Quantification policy.** "Each X" / "All X" / "Every X" forms
/// are NOT handled here — they're parsed at the top
/// [SelectorParser] level via `QUANTIFIER + INNER`, which wraps the
/// inner [PlayerSelector] in a [be.imgn.mtg.engine.oracle2.domain.selector.QuantifierSelector]
/// of [be.imgn.mtg.engine.oracle2.domain.StandardQuantifier#ALL]. To
/// support that path each axis exposes a bare-noun form
/// ("opponent(s)", "teammate(s)", "active player", "nonactive
/// player(s)", "ring-tempted player", "player with the city's
/// blessing", "player") so the inner parser matches once the
/// quantifier eats the `Each`. Article forms ("An opponent", "A
/// teammate", "Your opponent", …) match the singular semantic
/// directly without a wrapping quantifier.
///
/// Dispatch is longest-match-first within each arm so the
/// article+noun form wins over the bare noun.
public final class PlayerSelectorParser {
    private PlayerSelectorParser() {}

    // ── PlayerRelationSelector ─────────────────────────────────────

    private static final Parser<PlayerRelationSelector> RELATION = anyOf(
                    phrase("Your? opponent(s)").thenReturn(PlayerRelation.OPPONENT),
                    phrase("Your? teammate(s)").thenReturn(PlayerRelation.TEAMMATE),
                    phrase("Your team").thenReturn(PlayerRelation.TEAM),
                    phrase("You").thenReturn(PlayerRelation.YOU))
            .map(PlayerRelationSelector::new);

    // ── PlayerTurnRoleSelector ─────────────────────────────────────

    private static final Parser<PlayerTurnRoleSelector> TURN_ROLE = anyOf(
                    phrase("The? active player").thenReturn(PlayerTurnRole.ACTIVE),
                    phrase("The? nonactive player(s)").thenReturn(PlayerTurnRole.NONACTIVE))
            .map(PlayerTurnRoleSelector::new);

    // ── CombatRoleSelector ─────────────────────────────────────────

    private static final Parser<CombatRoleSelector> COMBAT_ROLE = anyOf(
                    phrase("[The|That]? attacking player").thenReturn(CombatRole.ATTACKING),
                    phrase("[The|That]? defending player").thenReturn(CombatRole.DEFENDING))
            .map(CombatRoleSelector::new);

    // ── PlayerDesignationSelector ──────────────────────────────────

    private static final Parser<PlayerDesignationSelector> DESIGNATION = anyOf(
                    phrase("The monarch").thenReturn(PlayerDesignation.MONARCH),
                    phrase("The player with the initiative").thenReturn(PlayerDesignation.INITIATIVE),
                    phrase("player with the city's blessing").thenReturn(PlayerDesignation.CITY_BLESSING),
                    phrase("ring-tempted player").thenReturn(PlayerDesignation.RING_TEMPTED))
            .map(PlayerDesignationSelector::new);

    // ── ControllerSelector / OwnerSelector ─────────────────────────

    /// "the controller of X" — [ControllerSelector]. Recursive on
    /// [be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector].
    private static final Parser<PlayerSelector> CONTROLLER =
            phrase("the controller of").then(Refs.OBJECT_SELECTOR).map(ControllerSelector::new);

    /// "the owner of X" — [OwnerSelector].
    private static final Parser<OwnerSelector> OWNER =
            phrase("the owner of").then(Refs.OBJECT_SELECTOR).map(OwnerSelector::new);

    // ── OtherPlayerSelector ────────────────────────────────────────

    /// "another player" — [OtherPlayerSelector] with `than = YOU` (the
    /// implicit "than you").
    private static final Parser<OtherPlayerSelector> OTHER_PLAYER = phrase("Another player")
            .thenReturn(new OtherPlayerSelector(new PlayerRelationSelector(PlayerRelation.YOU)));

    // ── Anyone / Enchanted ─────────────────────────────────────────

    /// `Any? player(s)`. The bare `player(s)` form covers "target
    /// player" and the inner of a quantified form ("each player" →
    /// `QuantifierSelector(ALL, ANYONE)`); the `Any?` prefix covers
    /// "any player" directly. The article-quantified forms
    /// ("a player" / "an opponent") are intentionally NOT matched
    /// here — they go through the top-level
    /// [SelectorParser]'s quantifier path so "a/an" wraps in
    /// `QuantifierSelector(Exact(1), …)` consistently with how
    /// "a creature" is handled on the object side.
    private static final Parser<PlayerSelector.Anyone> ANYONE =
            phrase("Any? Player(s)").thenReturn(PlayerSelector.Anyone.ANYONE);

    /// "enchanted player" — [PlayerSelector.Enchanted] with `by = SELF`.
    private static final Parser<PlayerSelector.Enchanted> ENCHANTED =
            phrase("Enchanted player").thenReturn(new PlayerSelector.Enchanted(SelfSelector.SELF));

    // ── Anaphoric back-reference ───────────────────────────────────

    /// Anaphoric pronouns and demonstratives on the player axis —
    /// "they", "them", "that player", "those players", "that
    /// opponent". All collapse to [PlayerSelector.Bound#PLAYER]
    /// because the antecedent is recoverable from the enclosing
    /// binding scope at engine evaluation time.
    private static final Parser<PlayerSelector> BOUND = anyOf(
            phrase("they").thenReturn(PlayerSelector.Bound.PLAYER),
            phrase("them").thenReturn(PlayerSelector.Bound.PLAYER),
            phrase("that player").thenReturn(PlayerSelector.Bound.PLAYER),
            phrase("those players").thenReturn(PlayerSelector.Bound.PLAYER),
            phrase("that opponent").thenReturn(PlayerSelector.Bound.PLAYER));

    /// Bare player parser — every arm except recursive postfix
    /// wrappers like [PlayerCounterSelectorParser]'s "with [counters]"
    /// form. Used as the leaf inside such postfix arms to avoid
    /// infinite recursion through [Refs#PLAYER_SELECTOR]. [#BOUND]
    /// precedes [#COMBAT_ROLE] so "that opponent" / "that player"
    /// don't get consumed by the `[The|That]? attacking player`
    /// arm's optional `That` prefix when no role word follows.
    static final Parser<PlayerSelector> BARE_PLAYER = anyOf(
            CONTROLLER, OWNER, OTHER_PLAYER, ENCHANTED, DESIGNATION, BOUND, COMBAT_ROLE, TURN_ROLE, ANYONE, RELATION);

    /// Top-level [PlayerSelector]. Order: postfix-wrapper arms
    /// ([PlayerCounterSelector]) first, then [#BARE_PLAYER].
    public static final Parser<PlayerSelector> PLAYER_SELECTOR =
            anyOf(PlayerCounterSelectorParser.PLAYER_COUNTER_SELECTOR, BARE_PLAYER);
}
