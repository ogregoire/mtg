package be.imgn.mtg.engine.oracle;

import java.util.List;

import org.jspecify.annotations.Nullable;

/// An effect produced by a spell or ability.
public sealed interface Effect {

    // Removal

    record Destroy(Subject target) implements Effect {}

    record Exile(Subject target, Zone.@Nullable Source from) implements Effect {
        Exile(Subject target) {
            this(target, null);
        }
    }

    record Sacrifice(Subject who, Selector what) implements Effect {}

    record Bounce(Subject target, Zone.Destination to) implements Effect {}

    // Damage & Life

    record DealDamage(Subject source, Amount amount, Subject target) implements Effect {}

    record GainLife(Subject player, Amount amount) implements Effect {}

    record LoseLife(Subject player, Amount amount) implements Effect {}

    // Card Manipulation

    record Draw(Subject player, Amount amount) implements Effect {}

    record Discard(Subject player, Amount amount) implements Effect {}

    record Mill(Subject player, Amount amount) implements Effect {}

    record Scry(Amount amount) implements Effect {}

    record Search(String possessive, Selector what) implements Effect {}

    record Shuffle() implements Effect {}

    record Reveal(Subject target) implements Effect {}

    // Tap/Untap

    record Tap(Subject target) implements Effect {}

    record Untap(Subject target) implements Effect {}

    // Counters

    record AddCounters(Amount count, CounterType type, Subject target) implements Effect {}

    record RemoveCounters(Amount count, CounterType type, Subject target) implements Effect {}

    // Ability Modification

    record GainAbility(
            Subject target,
            List<String> abilities,
            @Nullable Duration duration) implements Effect {
        GainAbility(Subject target, List<String> abilities) {
            this(target, abilities, null);
        }

        public GainAbility withDuration(Duration duration) {
            return new GainAbility(target, abilities, duration);
        }
    }

    record LoseAbility(Subject target, List<String> abilities) implements Effect {}

    // P/T Modification

    record ModifyPT(
            Subject target, PtModifier modifier, @Nullable Duration duration) implements Effect {
        ModifyPT(Subject target, PtModifier modifier) {
            this(target, modifier, null);
        }

        public ModifyPT withDuration(Duration duration) {
            return new ModifyPT(target, modifier, duration);
        }
    }

    // Control

    record GainControl(
            Subject player, Subject target, @Nullable Duration duration) implements Effect {
        GainControl(Subject player, Subject target) {
            this(player, target, null);
        }

        public GainControl withDuration(Duration duration) {
            return new GainControl(player, target, duration);
        }
    }

    // Tokens

    record CreateToken(Amount count, TokenDescription token) implements Effect {}

    // Counterspell

    record CounterSpell(Subject target) implements Effect {}

    // Combat

    record Fight(Subject a, Subject b) implements Effect {}

    // Mana

    record AddMana(List<ManaSymbol> symbols) implements Effect {}

    // Zone Movement

    record ZoneMove(Subject what, Zone.Destination to) implements Effect {}

    // Transform/Copy

    record Transform(Subject target) implements Effect {}

    record Copy(Subject target) implements Effect {}

    // Replacement & Prevention

    record Replace(Subject what, String event, Effect replacement) implements Effect {}

    record Prevent(String description) implements Effect {}

    // Win/Loss

    record WinGame(Subject player) implements Effect {}

    record LoseGame(Subject player) implements Effect {}

    // Characteristics

    record SetCharacteristic(
            Subject target, String description, @Nullable Duration duration) implements Effect {
        SetCharacteristic(Subject target, String description) {
            this(target, description, null);
        }

        public SetCharacteristic withDuration(Duration duration) {
            return new SetCharacteristic(target, description, duration);
        }
    }

    // Compound

    record Compound(Effect first, Effect second) implements Effect {}
}
