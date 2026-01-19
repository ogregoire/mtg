package be.imgn.mtg.engine.state;

import be.imgn.mtg.engine.characteristics.Abilities;
import be.imgn.mtg.engine.characteristics.Colors;
import be.imgn.mtg.engine.characteristics.Costs;
import be.imgn.mtg.engine.characteristics.Subtypes;
import be.imgn.mtg.engine.characteristics.Supertypes;
import be.imgn.mtg.engine.characteristics.Types;
import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.object.ObjectId;
import be.imgn.mtg.engine.zone.ZoneType;

/// An immutable snapshot of a game object's state ({@mtg.rule 400.7}).
///
/// When an object changes zones, the game needs to remember its last known information
/// for triggered abilities that reference the object that left. This record captures
/// all relevant characteristics at the moment the object left its previous zone.
///
/// @param id the object's unique identifier
/// @param name the object's name
/// @param colors the object's colors
/// @param types the object's types
/// @param supertypes the object's supertypes
/// @param subtypes the object's subtypes
/// @param abilities the object's abilities
/// @param costs the object's costs
/// @param power the object's power (for creatures)
/// @param toughness the object's toughness (for creatures)
/// @param loyalty the object's loyalty (for planeswalkers)
/// @param owner the object's owner
/// @param controller the object's controller
/// @param zone the zone the object was in
public record ObjectSnapshot(
        ObjectId id,
        String name,
        Colors colors,
        Types types,
        Supertypes supertypes,
        Subtypes subtypes,
        Abilities abilities,
        Costs costs,
        Value power,
        Value toughness,
        Value loyalty,
        Player owner,
        Player controller,
        ZoneType zone) {

    /// Creates a snapshot from a game object and its current zone.
    ///
    /// @param object the object to snapshot
    /// @param zone the zone the object is in
    /// @return a new snapshot
    public static ObjectSnapshot of(GameObject object, ZoneType zone) {
        return new ObjectSnapshot(
                object.id(),
                object.name(),
                object.colors(),
                object.types(),
                object.supertypes(),
                object.subtypes(),
                object.abilities(),
                object.costs(),
                object.power() != null ? object.power() : Value.of(0),
                object.toughness() != null ? object.toughness() : Value.of(0),
                object.loyalty() != null ? object.loyalty() : Value.of(0),
                object.owner(),
                object.controller(),
                zone);
    }
}
