package be.imgn.mtg.engine.oracle;

import java.util.List;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.turn.Phase;
import be.imgn.mtg.engine.turn.Step;

/// Structured trigger event for [Ability.TriggeredAbility]. Replaces
/// the prior free-text capture so trigger conditions are recognized by the
/// grammar rather than absorbed verbatim. Each variant corresponds to a
/// common oracle-text shape; add new variants when oracle text introduces
/// new event forms.
public sealed interface TriggerEvent {

    /// "\[subject\] enter\[s\] \[tapped\]?" (rule 603.6a). `tapped=true`
    /// for shapes like "a permanent you control enters tapped" (Amulet
    /// of Vigor).
    record Enters(Subject subject, boolean tapped) implements TriggerEvent {
        Enters(Subject subject) {
            this(subject, false);
        }

        public Enters withTapped() {
            return new Enters(subject, true);
        }
    }

    /// "\[subject\] die\[s\]" (rule 603.6c-d — put into graveyard from
    /// battlefield).
    record Dies(Subject subject) implements TriggerEvent {}

    /// "\[subject\] attack\[s\] \[target\]? \[alone\]?" (rule 603.6e). `target`
    /// is the attacked player or planeswalker when oracle names one (e.g.,
    /// "a creature attacks you"); null for the common agent-only form.
    record Attacks(Subject subject, @Nullable Subject target, boolean alone) implements TriggerEvent {
        Attacks(Subject subject) {
            this(subject, null, false);
        }

        public Attacks withTarget(Subject target) {
            return new Attacks(subject, target, alone);
        }

        public Attacks attackingAlone() {
            return new Attacks(subject, target, true);
        }
    }

    /// "\[subject\] block\[s\] \[target\]?" (rule 603.6e). `target` is
    /// the attacker when named (e.g., "this creature blocks a creature");
    /// null for the agent-only form.
    record Blocks(Subject subject, @Nullable Subject target) implements TriggerEvent {
        Blocks(Subject subject) {
            this(subject, null);
        }

        public Blocks withTarget(Subject target) {
            return new Blocks(subject, target);
        }
    }

    /// "\[subject\] become\[s\] blocked \[by X\]?" (rule 509).
    record BecomesBlocked(Subject subject, @Nullable Subject by) implements TriggerEvent {
        BecomesBlocked(Subject subject) {
            this(subject, null);
        }

        public BecomesBlocked withBy(Subject by) {
            return new BecomesBlocked(subject, by);
        }
    }

    /// "\[subject\] become\[s\] \[tapped|untapped\]" — status-change trigger.
    record BecomesStatus(Subject subject, Status status) implements TriggerEvent {
        public enum Status {
            TAPPED,
            UNTAPPED
        }
    }

    static BecomesStatus becomesTapped(Subject subject) {
        return new BecomesStatus(subject, BecomesStatus.Status.TAPPED);
    }

    static BecomesStatus becomesUntapped(Subject subject) {
        return new BecomesStatus(subject, BecomesStatus.Status.UNTAPPED);
    }

    /// "\[subject\] becomes the target of \[selector\]" (rule 603.6m).
    record BecomesTargetOf(Subject subject, Selector what) implements TriggerEvent {}

    /// "\[source\] deals \[combat\]? damage [to \[target\]]?" (rule 603.6h).
    /// `target` is null for the agent-only form ("this creature deals
    /// damage" — Chalice of Life, Sliver damage triggers).
    record DealsDamage(
            Subject source, boolean combat, @Nullable Subject target) implements TriggerEvent {
        DealsDamage(Subject source, boolean combat) {
            this(source, combat, null);
        }

        public DealsDamage withTarget(Subject target) {
            return new DealsDamage(source, combat, target);
        }
    }

    /// "\[subject\] is cast" (rule 603.6i — cast trigger on the stack).
    record IsCast(Subject subject) implements TriggerEvent {}

    /// "\[subject\] is countered".
    record IsCountered(Subject subject) implements TriggerEvent {}

    /// "\[subject\] is dealt damage" — received-damage trigger.
    record IsDealtDamage(Subject subject, boolean combat) implements TriggerEvent {
        IsDealtDamage(Subject subject) {
            this(subject, false);
        }
    }

    /// "\[subject\] is put into \[zone source\]" — zone-change trigger for
    /// cards/permanents (rule 603.6c, 603.10).
    record PutInto(Subject subject, Zone.Source from) implements TriggerEvent {}

    /// "\[subject\] leave\[s\] \[zone\]" — zone-leaving trigger.
    record Leaves(Subject subject, Zone zone) implements TriggerEvent {}

    /// "\[player\] cast\[s\] \[spell\] \[from zone\]? \[this turn\]? \[ordinal\]?."
    /// - `from`: zone-of-casting restriction — a spell can be cast
    ///   from hand, graveyard (flashback), exile (suspend, foretell), or
    ///   library (cascade); the trigger only fires when the cast origin
    ///   matches (Secrets of the Dead: "from your graveyard").
    /// - `thisTurn`: temporal scope — the trigger is only live
    ///   during the current turn (Glimpse of Nature: "this turn").
    /// - `nthEachTurn`: the "your first/second/… spell each turn"
    ///   qualifier (Rodeo Pyromancers).
    record PlayerCasts(
            Subject player,
            Selector spell,
            Zone.@Nullable Source from,
            boolean thisTurn,
            @Nullable Integer nthEachTurn) implements TriggerEvent {
        PlayerCasts(Subject player, Selector spell) {
            this(player, spell, null, false, null);
        }

        public PlayerCasts withFrom(Zone.Source from) {
            return new PlayerCasts(player, spell, from, thisTurn, nthEachTurn);
        }

        public PlayerCasts scopedToThisTurn() {
            return new PlayerCasts(player, spell, from, true, nthEachTurn);
        }

        public PlayerCasts nth(int n) {
            return new PlayerCasts(player, spell, from, thisTurn, n);
        }
    }

    /// "\[subject\] is turned face up" — morph/manifest flip trigger.
    record IsTurnedFaceUp(Subject subject) implements TriggerEvent {}

    /// "\[subject\] mutates" — mutate stack event (Ikoria).
    record Mutates(Subject subject) implements TriggerEvent {}

    /// "\[player\] give\[s\] a gift" — Aetherdrift Gifts mechanic.
    record PlayerGivesGift(Subject player) implements TriggerEvent {}

    /// "\[player\] attack\[s\] with \[amount\] \[creatures\]?." — attack
    /// formation trigger (e.g., Raiding Horde: "Whenever you attack with
    /// two or more creatures, …").
    record AttacksWith(Subject player, Amount amount) implements TriggerEvent {}

    /// "\[player\] control\[s\] no \[selector\]" — existential state check used
    /// as a trigger condition (Barbarian Outcast: "When you control no
    /// Swamps, sacrifice this creature."). Not strictly an event; fires
    /// whenever the state first becomes true (rule 603.6d / 603.10).
    record ControlsNone(Subject player, Selector what) implements TriggerEvent {}

    /// "\[player\] play\[s\] \[selector\]" — the generic land-play trigger with
    /// an explicit selector, distinct from the common "plays a land" form
    /// (e.g., "When you play another land").
    record PlayerPlays(Subject player, Selector what) implements TriggerEvent {}

    /// "\[player\] cycle\[s\] \[card\]".
    record PlayerCycles(Subject player, Selector card) implements TriggerEvent {}

    /// "\[player\] discard\[s\] \[card\]".
    record PlayerDiscards(Subject player, Selector card) implements TriggerEvent {}

    /// "\[player\] draw\[s\] \[amount\]".
    record PlayerDraws(Subject player, Amount amount) implements TriggerEvent {}

    /// "\[player\] gain\[s\] life".
    record PlayerGainsLife(Subject player) implements TriggerEvent {}

    /// "\[player\] lose\[s\] life".
    record PlayerLosesLife(Subject player) implements TriggerEvent {}

    /// "\[player\] play\[s\] a land".
    record PlayerPlaysLand(Subject player) implements TriggerEvent {}

    /// "\[player\] sacrifice\[s\] \[selector\]".
    record PlayerSacrifices(Subject player, Selector what) implements TriggerEvent {}

    /// "\[subject\] tap\[s\] \[land\] for mana".
    record TapsForMana(Subject subject, Selector what) implements TriggerEvent {}

    /// "\[player\] activate\[s\] \[ability\]".
    record PlayerActivates(Subject player, String description) implements TriggerEvent {}

    /// "at the beginning of \[owner\]'s/each \[step|phase\] …" — the
    /// phase/step-scoped triggers that share an owner + each-player
    /// prefix. Enables polymorphic dispatch on [#withOwner].
    sealed interface OwnerScoped extends TriggerEvent permits AtStep, AtPhase {
        OwnerScoped withOwner(@Nullable Subject owner, boolean each);
    }

    /// "at the beginning of \[owner\]'s/each \[step\] step" — rule 603.6g
    /// beginning-of-step trigger.
    record AtStep(@Nullable Subject owner, boolean each, Step step) implements OwnerScoped {
        AtStep(Step step) {
            this(null, false, step);
        }

        @Override
        public AtStep withOwner(@Nullable Subject owner, boolean each) {
            return new AtStep(owner, each, step);
        }
    }

    /// Ordinal qualifier on a twin-main-phase trigger — Hulking Raptor
    /// ("first main phase"), Vernal Equinox ("precombat main phase").
    /// The rules engine consults this when a turn exposes two main
    /// phases so the trigger fires on the right one.
    enum PhaseQualifier {
        FIRST,
        SECOND,
        PRECOMBAT,
        POSTCOMBAT
    }

    /// "at the beginning of \[owner\]'s/each \[qualifier\]? \[phase\] phase".
    /// `qualifier` is `null` for unqualified phases.
    record AtPhase(
            @Nullable Subject owner,
            boolean each,
            Phase phase,
            @Nullable PhaseQualifier qualifier) implements OwnerScoped {
        AtPhase(Phase phase) {
            this(null, false, phase, null);
        }

        AtPhase(Phase phase, @Nullable PhaseQualifier qualifier) {
            this(null, false, phase, qualifier);
        }

        @Override
        public AtPhase withOwner(@Nullable Subject owner, boolean each) {
            return new AtPhase(owner, each, phase, qualifier);
        }
    }

    /// "at end of combat".
    enum EndOfCombat implements TriggerEvent {
        END_OF_COMBAT
    }

    /// "at end of turn".
    enum EndOfTurn implements TriggerEvent {
        END_OF_TURN
    }

    /// "\[event\] or \[event\]" — a disjunction of triggering events sharing a
    /// single triggered ability (e.g., "when this creature enters or dies").
    record Or(List<TriggerEvent> events) implements TriggerEvent {}
}
