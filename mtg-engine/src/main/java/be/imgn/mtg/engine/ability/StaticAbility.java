package be.imgn.mtg.engine.ability;

import java.util.Set;

import be.imgn.mtg.engine.zone.ZoneType;

/// A static ability ({@mtg.rule 113.3d}).
///
/// Static abilities create continuous effects that are always "on" while the object
/// with the ability is in the appropriate zone and meets any applicable conditions.
/// They don't use the stack and can't be activated or triggered.
///
/// Examples of static abilities:
/// - "Creatures you control have flying"
/// - "Lands your opponents control enter the battlefield tapped"
/// - "This creature gets +1/+1 for each artifact you control"
///
/// Some static abilities function from specific zones:
/// - Most static abilities only function on the battlefield
/// - Some function from other zones (e.g., "As ~ enters the battlefield...")
/// - Characteristic-defining abilities ({@mtg.rule 604.3}) function everywhere
///
/// @see Ability
public non-sealed interface StaticAbility extends Ability {

    /// Returns the zones from which this static ability applies.
    ///
    /// Most static abilities only function on the battlefield, but some apply
    /// from other zones (e.g., hand, graveyard) or everywhere (CDAs).
    ///
    /// @return the set of zones where this ability is functional
    Set<ZoneType> functionalZones();

    /// Returns true if this is a characteristic-defining ability ({@mtg.rule 604.3}).
    ///
    /// CDAs define a card's characteristics (color, type, power/toughness, etc.)
    /// rather than modifying them. They function in all zones, not just the battlefield.
    ///
    /// @return true if this is a characteristic-defining ability
    boolean isCharacteristicDefining();
}
