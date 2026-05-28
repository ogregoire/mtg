package be.imgn.mtg.engine.oracle2.domain.ability;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.selector.PlayerSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.turn.Step;

/// The event-shape that fires a triggered ability ({@mtg.rule 603}).
/// Today the hierarchy covers a starter set wide enough for
/// `~ enters / dies / attacks`-style triggers and the simplest
/// `at the beginning of [step]` shape; richer events
/// (DealsDamage, BecomesTarget, AtPhase, …) land as new permitted
/// records when oracle text needs them.
public sealed interface TriggerEvent
        permits TriggerEvent.Enters,
                TriggerEvent.Dies,
                TriggerEvent.Attacks,
                TriggerEvent.AtBeginningOf,
                TriggerEvent.GiveAGift,
                TriggerEvent.HasAbility {

    /// "When [subject] enters" — battlefield-entry trigger
    /// ({@mtg.rule 603.6a}). The slot is the broad [Selector] so a
    /// count-bearing or quantified subject ("a creature", "another
    /// creature you control") can sit naturally — same as
    /// [be.imgn.mtg.engine.oracle2.domain.effect.SacrificeEffect#what]
    /// uses on the effect axis.
    record Enters(Selector subject) implements TriggerEvent {
        public Enters {
            requireNonNull(subject);
        }
    }

    /// "When [subject] dies" ({@mtg.rule 700.4}, {@mtg.rule 603.6c}).
    record Dies(Selector subject) implements TriggerEvent {
        public Dies {
            requireNonNull(subject);
        }
    }

    /// "When [subject] attacks" ({@mtg.rule 506.5}, {@mtg.rule 603.6e}).
    record Attacks(Selector subject) implements TriggerEvent {
        public Attacks {
            requireNonNull(subject);
        }
    }

    /// "At the beginning of [step]" — phase/step trigger
    /// ({@mtg.rule 603.6b}). Today only the step is captured;
    /// the active-player axis ("your upkeep" vs "each player's
    /// upkeep") will land when oracle2 has a richer player-slot
    /// grammar at the trigger layer.
    record AtBeginningOf(Step step) implements TriggerEvent {
        public AtBeginningOf {
            requireNonNull(step);
        }
    }

    /// "Whenever [player] gives a gift" — gift-mechanic trigger
    /// (Bloomburrow). Fires every time `who` performs the optional
    /// "Gift a (target) (gift-type)" action printed on the spell.
    /// Jolly Gerbils: "Whenever you give a gift, draw a card.".
    record GiveAGift(PlayerSelector who) implements TriggerEvent {
        public GiveAGift {
            requireNonNull(who);
        }
    }

    /// "When [subject] has [ability]" — state-based trigger that
    /// fires when the predicate becomes true ({@mtg.rule 603.6f}).
    /// Student of Elements: "When this creature has flying, flip
    /// it." `ability` is the keyword whose presence triggers the
    /// effect (Flying, Trample, etc.); the engine watches the
    /// subject for keyword-set transitions.
    record HasAbility(Selector subject, Ability ability) implements TriggerEvent {
        public HasAbility {
            requireNonNull(subject);
            requireNonNull(ability);
        }
    }
}
