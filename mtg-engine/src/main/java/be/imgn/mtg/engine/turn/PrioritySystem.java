package be.imgn.mtg.engine.turn;

import java.util.List;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.game.Player;

/// Manages priority passing and APNAP order during the game ({@mtg.rule 117}, {@mtg.rule 101.4}).
///
/// Priority determines which player may take an action. Only one player
/// has priority at any given time. When all players pass in succession,
/// the top object on the stack resolves or the phase/step ends.
///
/// Also provides APNAP (Active Player, Non-Active Player) ordering for
/// simultaneous choices and effects.
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

    // ===== APNAP Order (merged from APNAPOrder) =====

    /// Returns all players in APNAP order based on the current active player.
    ///
    /// APNAP order is used for simultaneous choices and effects. The active player
    /// makes choices first, followed by each other player in turn order.
    ///
    /// @return players in APNAP order (active player first)
    List<Player> getAPNAPOrder();

    /// Returns all players in APNAP order with a specific active player.
    ///
    /// This is used when the active player for APNAP order differs from
    /// the current turn's active player (rare edge cases).
    ///
    /// @param activePlayer the player to treat as active for APNAP order
    /// @return players in APNAP order starting with the specified player
    List<Player> getAPNAPOrder(Player activePlayer);
}
