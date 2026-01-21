package be.imgn.mtg.engine.mana;

import be.imgn.mtg.engine.event.Event;
import be.imgn.mtg.engine.game.Player;

/// Event fired when mana is removed from a player's mana pool ({@mtg.rule 106}).
///
/// @param player the player whose mana was removed
/// @param mana the mana that was removed
/// @param reason the reason for removal
public record ManaRemovedEvent(Player player, Mana mana, RemovalReason reason) implements Event {

    /// The reason mana was removed from the pool.
    public enum RemovalReason {
        /// Mana was spent to pay a cost.
        SPENT,
        /// Mana pool was emptied at step/phase end.
        EMPTIED
    }
}
