package be.imgn.mtg.engine.oracle.domain2.selector;

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
                PlayerSelector.Enchanted {

    /// Always-true predicate over players. Canonical filler for the
    /// `owner` slot when oracle text imposes no further constraint
    /// ("any graveyard" → `Graveyard(ANYONE, Card(ANYTHING))`).
    enum Anyone implements PlayerSelector {
        ANYONE
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
}
