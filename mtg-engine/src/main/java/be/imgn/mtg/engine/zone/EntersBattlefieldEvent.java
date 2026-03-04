package be.imgn.mtg.engine.zone;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.object.Token;

/// Event representing a permanent entering the battlefield ({@mtg.rule 614.12}).
///
/// Permanents enter the battlefield when a permanent spell resolves, when a land is played,
/// when an effect puts a permanent onto the battlefield, or other game actions.
///
/// Enters-the-battlefield (ETB) events are commonly replaced (e.g., entering tapped,
/// entering with counters, entering under a different controller).
///
/// @param permanent the permanent entering the battlefield
/// @param from the zone it's entering from
public record EntersBattlefieldEvent(Permanent permanent, ZoneType from) implements ZoneChangeEvent {

    @Override
    public ZoneType to() {
        return ZoneType.BATTLEFIELD;
    }

    @Override
    public Player affectedPlayer() {
        return permanent.controller();
    }

    /// Returns a copy of this event with a different controller for the permanent.
    ///
    /// @param newController the new controller
    /// @return a new event with the modified permanent
    public EntersBattlefieldEvent withController(Player newController) {
        var modifiedPermanent =
                switch (permanent.source()) {
                    case Card cardSource ->
                        Permanent.fromCard(cardSource, newController).build();
                    case Token tokenSource ->
                        Permanent.fromToken(tokenSource, newController).build();
                };
        return new EntersBattlefieldEvent(modifiedPermanent, from);
    }
}
