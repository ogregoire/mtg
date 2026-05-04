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
                    phrase("[An|Your] opponent(s)").thenReturn(PlayerRelation.OPPONENT),
                    phrase("opponent(s)").thenReturn(PlayerRelation.OPPONENT),
                    phrase("[A|Your] teammate(s)").thenReturn(PlayerRelation.TEAMMATE),
                    phrase("teammate(s)").thenReturn(PlayerRelation.TEAMMATE),
                    phrase("Your team").thenReturn(PlayerRelation.TEAM),
                    phrase("You").thenReturn(PlayerRelation.YOU))
            .map(PlayerRelationSelector::new);

    // ── PlayerTurnRoleSelector ─────────────────────────────────────

    private static final Parser<PlayerTurnRoleSelector> TURN_ROLE = anyOf(
                    phrase("The active player").thenReturn(PlayerTurnRole.ACTIVE),
                    phrase("active player").thenReturn(PlayerTurnRole.ACTIVE),
                    phrase("The nonactive player(s)").thenReturn(PlayerTurnRole.NONACTIVE),
                    phrase("nonactive player(s)").thenReturn(PlayerTurnRole.NONACTIVE))
            .map(PlayerTurnRoleSelector::new);

    // ── CombatRoleSelector ─────────────────────────────────────────

    private static final Parser<CombatRoleSelector> COMBAT_ROLE = anyOf(
                    phrase("[The|That] attacking player").thenReturn(CombatRole.ATTACKING),
                    phrase("attacking player").thenReturn(CombatRole.ATTACKING),
                    phrase("[The|That] defending player").thenReturn(CombatRole.DEFENDING),
                    phrase("defending player").thenReturn(CombatRole.DEFENDING))
            .map(CombatRoleSelector::new);

    // ── PlayerDesignationSelector ──────────────────────────────────

    private static final Parser<PlayerDesignationSelector> DESIGNATION = anyOf(
                    phrase("The monarch").thenReturn(PlayerDesignation.MONARCH),
                    phrase("The player with the initiative").thenReturn(PlayerDesignation.INITIATIVE),
                    phrase("A player with the city's blessing").thenReturn(PlayerDesignation.CITY_BLESSING),
                    phrase("player with the city's blessing").thenReturn(PlayerDesignation.CITY_BLESSING),
                    phrase("A ring-tempted player").thenReturn(PlayerDesignation.RING_TEMPTED),
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

    private static final Parser<PlayerSelector.Anyone> ANYONE = anyOf(
            phrase("[Any|A] player").thenReturn(PlayerSelector.Anyone.ANYONE),
            // Bare "player(s)" — for use after "target" or as the
            // inner of a quantified form ("each player" →
            // `QuantifierSelector(ALL, ANYONE)` via the top-level
            // SelectorParser dispatch).
            phrase("Player(s)").thenReturn(PlayerSelector.Anyone.ANYONE));

    /// "enchanted player" — [PlayerSelector.Enchanted] with `by = SELF`.
    private static final Parser<PlayerSelector.Enchanted> ENCHANTED =
            phrase("Enchanted player").thenReturn(new PlayerSelector.Enchanted(SelfSelector.SELF));

    /// Top-level [PlayerSelector]. Order: multi-word designations and
    /// roles (longest prefix wins); single-word "you" / "a player"
    /// last; "another player" tried before "a player" so the longer
    /// "Another" prefix matches.
    public static final Parser<PlayerSelector> PLAYER_SELECTOR =
            anyOf(CONTROLLER, OWNER, OTHER_PLAYER, ENCHANTED, DESIGNATION, COMBAT_ROLE, TURN_ROLE, ANYONE, RELATION);
}
