package be.imgn.mtg.engine.cost;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.TypedObject;

/// Context for paying costs ({@mtg.rule 118}).
///
/// Provides the game state information needed to determine if a cost can be paid and to
/// execute the payment of that cost. Every cost payment must have a source - the object
/// being paid for (spell being cast, ability being activated).
///
/// @param player the player who is paying the cost
/// @param source the game object being paid for (spell being cast, ability being activated)
public record CostContext(Player player, TypedObject source) {

    /// Creates a new cost context.
    ///
    /// @throws NullPointerException if player or source is null
    public CostContext {
        requireNonNull(player, "player");
        requireNonNull(source, "source");
    }
}
