package be.imgn.mtg.engine.ability;

import java.util.Set;

import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.zone.ZoneType;

/// Determines which zones an ability functions from ({@mtg.rule 113.6}).
///
/// Rule 113.6 defines where abilities function:
/// - Most abilities function only on the battlefield
/// - Characteristic-defining abilities function in all zones
/// - Some abilities specify where they function (e.g., "~ has this ability only while...")
/// - Abilities that prevent/modify entering the battlefield work from all zones
///
/// @see StaticAbility
public interface ZoneFunctionality {

    /// Determines the zones from which a static ability functions.
    ///
    /// @param ability the static ability to check
    /// @param source the object with the ability
    /// @return the set of zones where this ability functions
    Set<ZoneType> functionalZones(StaticAbility ability, GameObject source);
}
