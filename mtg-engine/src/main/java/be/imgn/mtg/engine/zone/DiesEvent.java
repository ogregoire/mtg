package be.imgn.mtg.engine.zone;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Permanent;

/// Event representing a permanent dying ({@mtg.rule 700.4}).
///
/// A permanent "dies" when it is put into a graveyard from the battlefield.
/// This includes destruction, sacrifice, and state-based actions (0 toughness, lethal damage).
///
/// Dies is distinct from other ways to leave the battlefield (exile, return to hand, etc.).
///
/// @param permanent the permanent that is dying
/// @param cause the reason for death
public record DiesEvent(Permanent permanent, DeathCause cause) implements ZoneChangeEvent {

    @Override
    public ZoneType from() {
        return ZoneType.BATTLEFIELD;
    }

    @Override
    public ZoneType to() {
        return ZoneType.GRAVEYARD;
    }

    @Override
    public Player affectedPlayer() {
        return permanent.controller();
    }

    /// The cause of a permanent's death.
    public sealed interface DeathCause {

        /// The permanent was destroyed by an effect.
        record Destroyed() implements DeathCause {}

        /// The permanent was sacrificed by a player.
        ///
        /// @param player the player who sacrificed the permanent
        record Sacrificed(Player player) implements DeathCause {}

        /// The permanent died due to a state-based action (0 toughness, lethal damage, etc.).
        record StateBasedAction() implements DeathCause {}
    }
}
