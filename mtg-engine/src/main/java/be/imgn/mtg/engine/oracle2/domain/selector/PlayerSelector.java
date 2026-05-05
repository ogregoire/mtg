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
                PlayerSelector.SharedSubject,
                PlayerSelector.Target {

    /// Always-true predicate over players. Canonical filler for the
    /// `owner` slot when oracle text imposes no further constraint
    /// ("any graveyard" → `Graveyard(ANYONE, Card(ANYTHING))`).
    enum Anyone implements PlayerSelector {
        ANYONE
    }

    /// Placeholder for the shared subject of an enclosing
    /// [be.imgn.mtg.engine.oracle2.domain.effect.SharedSubjectEffect].
    /// Never produced by a parser directly; only synthesized by the
    /// effect parser when fanning a player-axis subject across
    /// multiple verb clauses ("Target player draws two cards and
    /// loses 2 life."). The axis (PlayerSelector) is preserved so
    /// axis-narrowed `Effect.who` slots can hold it without a cast.
    enum SharedSubject implements PlayerSelector {
        INSTANCE
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
