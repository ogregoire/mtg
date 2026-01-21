package be.imgn.mtg.engine.ability.internal;

import java.util.Set;

import be.imgn.mtg.engine.ability.StaticAbility;
import be.imgn.mtg.engine.ability.ZoneFunctionality;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.zone.ZoneType;

/// Default implementation of [ZoneFunctionality].
///
/// Implements Rule 113.6:
/// - 113.6a: Characteristic-defining abilities (CDAs) function in all zones
/// - 113.6b-c: Abilities that prevent/modify entering zones work from all zones
/// - 113.6d: Abilities that modify how an object comes into play function from zones it could come from
/// - 113.6e-m: Various other zone-specific rules
final class DefaultZoneFunctionality implements ZoneFunctionality {

    /// All zones - used for abilities that function everywhere (like CDAs).
    private static final Set<ZoneType> ALL_ZONES = Set.of(ZoneType.values());

    /// Just the battlefield - the default for most static abilities.
    private static final Set<ZoneType> BATTLEFIELD_ONLY = Set.of(ZoneType.BATTLEFIELD);

    @Override
    public Set<ZoneType> functionalZones(StaticAbility ability, GameObject source) {
        // Characteristic-defining abilities (Rule 604.3, 113.6a)
        // These define characteristics and function in all zones
        if (ability.isCharacteristicDefining()) {
            return ALL_ZONES;
        }

        // Check if the ability specifies its own functional zones
        var specifiedZones = ability.functionalZones();
        if (!specifiedZones.isEmpty()) {
            return specifiedZones;
        }

        // Default: most static abilities only function on the battlefield
        return BATTLEFIELD_ONLY;
    }
}
