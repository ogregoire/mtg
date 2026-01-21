package be.imgn.mtg.engine.characteristics;

import java.util.Objects;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.GameObject;

/// Context for paying costs ({@mtg.rule 118}).
///
/// Provides the game state information needed to determine if a cost can be paid and to
/// execute the payment of that cost. Every cost payment must have a source - the object
/// being paid for (spell being cast, ability being activated).
///
/// @param player the player who is paying the cost
/// @param source the game object being paid for (spell being cast, ability being activated)
public record CostContext(Player player, GameObject source) {

    /// Creates a new cost context.
    ///
    /// @throws NullPointerException if player or source is null
    public CostContext {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(source, "source");
    }
}
