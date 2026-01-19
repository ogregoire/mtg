package be.imgn.mtg.engine.zone;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Permanent;

/// Event representing a permanent leaving the battlefield.
///
/// This event is used for leaves-the-battlefield (LTB) triggers. It fires whenever
/// a permanent moves from the battlefield to any other zone (graveyard, exile, hand, etc.).
///
/// Note: [DiesEvent] is a more specific event for battlefield → graveyard transitions.
/// Both events fire when a permanent dies.
///
/// @param permanent the permanent leaving the battlefield
/// @param to the zone it's going to
public record LeavesBattlefieldEvent(Permanent permanent, ZoneType to) implements ZoneChangeEvent {

    @Override
    public ZoneType from() {
        return ZoneType.BATTLEFIELD;
    }

    @Override
    public Player affectedPlayer() {
        return permanent.controller();
    }
}
