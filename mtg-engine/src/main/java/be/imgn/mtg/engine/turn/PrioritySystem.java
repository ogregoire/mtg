package be.imgn.mtg.engine.turn;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.game.Player;

/// Manages priority passing during the game ({@mtg.rule 117}).
///
/// Priority determines which player may take an action. Only one player
/// has priority at any given time. When all players pass in succession,
/// the top object on the stack resolves or the phase/step ends.
///
/// @see APNAPOrder
public interface PrioritySystem {

    /// Returns the player who currently has priority, or null if no one has priority.
    ///
    /// @return the current priority holder, or null during certain steps
    @Nullable
    Player currentPriorityHolder();

    /// Records that a player has passed priority.
    ///
    /// When a player passes, priority moves to the next player in APNAP order.
    /// If all players pass in succession, the priority round ends.
    ///
    /// @param player the player who passed
    void pass(Player player);

    /// Returns whether all players have passed priority in succession.
    ///
    /// @return true if all players have passed since the last action
    boolean allPassed();

    /// Resets the pass state for all players.
    ///
    /// This is called when any player takes an action (casts a spell,
    /// activates an ability, etc.) or when a new object resolves.
    void reset();

    /// Gives priority to a specific player.
    ///
    /// The active player receives priority at the beginning of most steps
    /// and phases, and whenever an object resolves ({@mtg.rule 117.3a}).
    ///
    /// @param player the player to give priority to
    void givePriority(Player player);

    /// Clears priority so no player has it.
    ///
    /// Used during steps that don't allow priority, like the untap step.
    void clearPriority();
}
