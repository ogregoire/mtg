package be.imgn.mtg.engine.ability;

import java.util.Set;

import be.imgn.mtg.engine.cost.Cost;
import be.imgn.mtg.engine.effect.Effect;
import be.imgn.mtg.engine.zone.ZoneType;

/// An activated ability ({@mtg.rule 113.3b}).
///
/// Activated abilities have the format "Cost: Effect." They are activated by a player
/// who has priority and can pay the cost. Most activated abilities go on the stack,
/// except for mana abilities which resolve immediately.
///
/// Special classifications of activated abilities:
/// - **Mana abilities** ({@mtg.rule 605}): Don't use the stack, resolve immediately
/// - **Loyalty abilities** ({@mtg.rule 606}): Can only be activated at sorcery speed,
///   once per turn per planeswalker
///
/// @see Ability
/// @see ActivationTiming
/// @see ActivationLimit
public non-sealed interface ActivatedAbility extends Ability {

    /// Returns the cost for this ability (e.g., tap, mana, sacrifice).
    ///
    /// @return the cost, never null
    Cost cost();

    /// Returns the effect that this ability produces when resolved.
    ///
    /// @return the effect, never null
    Effect effect();

    /// Returns when this ability can be activated.
    ///
    /// Most abilities can be activated at instant speed (whenever the player has priority).
    /// Some have restrictions like sorcery speed or specific phases.
    ///
    /// @return the activation timing restriction
    ActivationTiming timing();

    /// Returns the activation limit for this ability.
    ///
    /// Most abilities have no limit, but some can only be activated once per turn
    /// or have other restrictions.
    ///
    /// @return the activation limit, never null
    ActivationLimit limit();

    /// Returns the zones from which this ability can be activated.
    ///
    /// Most activated abilities only work on the battlefield, but some can be
    /// activated from other zones (e.g., cycling from hand, unearth from graveyard).
    ///
    /// @return the set of zones where this ability is active
    Set<ZoneType> activatesFrom();

    /// Returns true if this is a mana ability ({@mtg.rule 605.1a}).
    ///
    /// An activated ability is a mana ability if:
    /// - It could add mana to a player's mana pool when it resolves
    /// - It doesn't require a target
    /// - It's not a loyalty ability
    ///
    /// Mana abilities don't use the stack and resolve immediately.
    ///
    /// @return true if this is a mana ability
    default boolean isManaAbility() {
        return !isLoyaltyAbility() && effect().isManaAbilityEffect();
    }

    /// Returns true if this is a loyalty ability ({@mtg.rule 606.1}).
    ///
    /// Loyalty abilities are activated abilities on planeswalkers whose cost
    /// involves adding or removing loyalty counters. They can only be activated:
    /// - At sorcery speed
    /// - Once per planeswalker per turn
    ///
    /// @return true if this is a loyalty ability
    default boolean isLoyaltyAbility() {
        return cost().isLoyaltyCost();
    }

    /// Returns true if this ability can currently be activated ({@mtg.rule 602.2}).
    ///
    /// Checks whether the activation limit allows activation (e.g., loyalty abilities
    /// can only be activated once per turn per planeswalker).
    ///
    /// @param context the activation context providing game state for the check
    /// @return true if the ability can be activated
    default boolean canActivate(ActivationContext context) {
        return limit().canActivate(id(), context.tracker());
    }
}
