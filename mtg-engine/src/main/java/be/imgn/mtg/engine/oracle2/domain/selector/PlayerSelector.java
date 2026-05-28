package be.imgn.mtg.engine.oracle2.domain.selector;

import static java.util.Objects.requireNonNull;

/// Selects one or more players — "you", "target opponent", "each
/// other player", etc. Sealed: every arm is one of the listed
/// player-axis records, the "any player" sentinel, or the player-side
/// Aura host (Curses).
public sealed interface PlayerSelector extends Selector
        permits CombatRoleSelector,
                ControllerSelector,
                OwnerSelector,
                PlayerCounterSelector,
                PlayerDesignationSelector,
                PlayerRelationSelector,
                PlayerTurnRoleSelector,
                OtherPlayerSelector,
                PlayerSelector.Anyone,
                PlayerSelector.Enchanted,
                PlayerSelector.Bound,
                PlayerSelector.Target {

    /// Always-true predicate over players. Canonical filler for the
    /// `owner` slot when oracle text imposes no further constraint
    /// ("any graveyard" → `Graveyard(ANYONE, Card(ANYTHING))`).
    enum Anyone implements PlayerSelector {
        ANYONE
    }

    /// Back-reference to the player bound by the nearest enclosing
    /// binding scope. Two producer paths funnel into the same marker
    /// so a single engine-side walk resolves both:
    ///
    /// - **Leaf-emitted** at anaphoric pronoun sites — "they",
    ///   "their", "that player", "that opponent" — by
    ///   [be.imgn.mtg.engine.oracle2.parser.selector.PlayerSelectorParser]
    ///   and [be.imgn.mtg.engine.oracle2.parser.selector.ZoneParser]
    ///   (for the possessive `their (zone)` forms). The binding
    ///   scope is whatever wrapper introduces the antecedent (a
    ///   trigger event's player subject, a sibling-shared subject,
    ///   a previously-chosen target player).
    /// - **Synthesized** by [be.imgn.mtg.engine.oracle2.parser.effect.EffectParser]
    ///   when fanning a player-axis subject across the sibling
    ///   clauses of [be.imgn.mtg.engine.oracle2.domain.effect.SharedSubjectEffect]
    ///   or the verb-choice alternatives of
    ///   [be.imgn.mtg.engine.oracle2.domain.effect.ChoiceEffect]. The
    ///   binding scope here is the wrapper itself, with its `subject`
    ///   field holding the bound value.
    ///
    /// The engine resolves [#PLAYER] at evaluation time by walking
    /// the surrounding AST to the nearest binding scope and reading
    /// its bound subject. The axis (PlayerSelector) is preserved so
    /// axis-narrowed slots can hold it without a cast.
    enum Bound implements PlayerSelector {
        PLAYER
    }

    /// "enchanted player" — Aura host on a player ({@mtg.rule 303.4}
    /// — Curses). The host is selected; `by` references the Aura
    /// (typically [SelfSelector#SELF] when a Curse refers to "the
    /// player this Aura enchants").
    record Enchanted(ObjectSelector by) implements PlayerSelector {
        public Enchanted {
            requireNonNull(by);
        }
    }

    /// "target X" on a player axis ({@mtg.rule 115.1}). The wrapped
    /// `inner` is itself a [PlayerSelector] so the targeting marker
    /// composes uniformly with every other player arm.
    record Target(PlayerSelector inner) implements PlayerSelector {
        public Target {
            requireNonNull(inner);
        }
    }
}
