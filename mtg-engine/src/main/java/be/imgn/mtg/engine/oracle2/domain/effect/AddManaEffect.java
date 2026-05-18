package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.oracle2.domain.Condition;
import be.imgn.mtg.engine.oracle2.domain.mana.ProducedMana;
import be.imgn.mtg.engine.oracle2.domain.mana.Restriction;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// Add a [ProducedMana] to a player's mana pool ({@mtg.rule 106.4}).
///
/// Three slots:
///
/// - `player` — the actor when oracle text names one (Tangleroot:
///   "that player adds {G}"); null for the common imperative "Add …"
///   form where the controller is implicit.
/// - `payload` — the mana produced. Spend restrictions ({@mtg.rule
///   106.6}) attach via [#withRestriction(Restriction)] — the
///   restriction distributes to every
///   [be.imgn.mtg.engine.oracle2.domain.mana.Mana] instance inside
///   the payload (not to the bundle as a whole).
/// - `replacement` — the baked-in "If \<condition\>, add \<other
///   mana\> instead." rider (River of Tears family). `null` when
///   absent. When present, the [Replacement#condition] gates the
///   substitution at resolution: if it holds, [Replacement#payload]
///   is added instead of the base [#payload].
public record AddManaEffect(
        @Nullable Selector player,
        ProducedMana payload,
        @Nullable Replacement replacement) implements Effect {

    public AddManaEffect {
        requireNonNull(payload);
    }

    public AddManaEffect(ProducedMana payload) {
        this(null, payload, null);
    }

    public AddManaEffect withPlayer(Selector player) {
        return new AddManaEffect(player, payload, replacement);
    }

    /// Distribute `restriction` across every [Mana][be.imgn.mtg.engine.oracle2.domain.mana.Mana]
    /// in [#payload]. Used by the parser to absorb a trailing "Spend
    /// this mana only …" sentence into the preceding `AddManaEffect`.
    public AddManaEffect withRestriction(Restriction restriction) {
        return new AddManaEffect(player, payload.withRestriction(restriction), replacement);
    }

    public AddManaEffect withReplacement(Replacement replacement) {
        return new AddManaEffect(player, payload, replacement);
    }

    /// "If \<condition\>, add \<payload\> instead." rider. Baked into
    /// `AddManaEffect` rather than expressed as a separate
    /// replacement-effect record because the substitution is tightly
    /// coupled to the add it overrides (one resolution event, one
    /// produced-mana bundle).
    public record Replacement(Condition condition, ProducedMana payload) {
        public Replacement {
            requireNonNull(condition);
            requireNonNull(payload);
        }
    }
}
