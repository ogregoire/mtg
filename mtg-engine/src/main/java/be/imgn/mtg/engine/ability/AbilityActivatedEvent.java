package be.imgn.mtg.engine.ability;

import be.imgn.mtg.engine.event.Event;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.GameObject;

/// Event fired when an activated ability is activated.
///
/// This event is fired after the ability activation process is complete:
/// - For mana abilities: after immediate resolution
/// - For non-mana abilities: after the ability is put on the stack
///
/// The ability and source are stored directly so that all properties can be queried
/// from the event without needing to look up by ID.
///
/// @param ability the activated ability
/// @param source the source object that has the ability
/// @param controller the player who activated the ability
public record AbilityActivatedEvent(ActivatedAbility ability, GameObject source, Player controller) implements Event {

    /// Returns the ability ID.
    public AbilityId abilityId() {
        return ability.id();
    }

    /// Returns true if this was a mana ability activation.
    public boolean isManaAbility() {
        return ability.isManaAbility();
    }

    /// Returns true if this was a loyalty ability activation.
    public boolean isLoyaltyAbility() {
        return ability.isLoyaltyAbility();
    }
}
