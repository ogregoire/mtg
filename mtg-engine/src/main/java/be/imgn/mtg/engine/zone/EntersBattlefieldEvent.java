package be.imgn.mtg.engine.zone;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.ObjectId;
import be.imgn.mtg.engine.object.Permanent;

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
/// @param cause the reason for entering
public record EntersBattlefieldEvent(Permanent permanent, ZoneType from, EtbCause cause) implements ZoneChangeEvent {

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
                Permanent.fromCard((Card) permanent.source(), newController).build();
        return new EntersBattlefieldEvent(modifiedPermanent, from, cause);
    }

    /// The cause of a permanent entering the battlefield.
    public sealed interface EtbCause {

        /// A land was played as the land-per-turn action.
        record LandPlayed() implements EtbCause {}

        /// A permanent spell resolved.
        ///
        /// @param spellId the ID of the spell that resolved
        record SpellResolved(ObjectId spellId) implements EtbCause {}

        /// An ability resolved and put this permanent onto the battlefield.
        ///
        /// @param abilityId the ID of the ability that resolved
        record AbilityResolved(ObjectId abilityId) implements EtbCause {}

        /// An effect put this permanent onto the battlefield directly.
        record Put() implements EtbCause {}
    }
}
