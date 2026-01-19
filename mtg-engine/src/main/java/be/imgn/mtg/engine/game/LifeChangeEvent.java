package be.imgn.mtg.engine.game;

import be.imgn.mtg.engine.event.GameEvent;

/// Event representing a change in a player's life total ({@mtg.rule 119}).
///
/// Life changes include gaining life, losing life, and paying life. This event
/// represents the fundamental change and can be subject to replacement effects
/// (e.g., "If you would gain life, instead...").
///
/// @param player the player whose life total is changing
/// @param amount the amount of change (positive for gain, negative for loss)
/// @param cause the reason for the life change
public record LifeChangeEvent(Player player, int amount, LifeChangeCause cause) implements GameEvent {

    @Override
    public Player affectedPlayer() {
        return player;
    }

    /// Returns true if this is life gain.
    ///
    /// @return true if amount is positive
    public boolean isGain() {
        return amount > 0;
    }

    /// Returns true if this is life loss.
    ///
    /// @return true if amount is negative
    public boolean isLoss() {
        return amount < 0;
    }

    /// The cause of a life change.
    public sealed interface LifeChangeCause {

        /// Life gained from an effect.
        record LifeGain() implements LifeChangeCause {}

        /// Life lost from an effect.
        record LifeLoss() implements LifeChangeCause {}

        /// Life paid as a cost.
        record LifePaid() implements LifeChangeCause {}

        /// Life lost due to damage.
        record Damage() implements LifeChangeCause {}

        /// Life set to a specific value (e.g., "your life total becomes 10").
        record LifeSet() implements LifeChangeCause {}
    }
}
