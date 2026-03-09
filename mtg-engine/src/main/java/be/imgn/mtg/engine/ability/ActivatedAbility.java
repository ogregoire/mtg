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
/// @param id the unique ability identifier
/// @param oracleText the original oracle text
/// @param cost the activation cost
/// @param effect the effect produced on resolution
/// @param timing when this ability can be activated
/// @param limit how often this ability can be activated
/// @param activatesFrom the zones this ability can be activated from
///
/// @see Ability
/// @see ActivationTiming
/// @see ActivationLimit
public record ActivatedAbility(
        AbilityId id,
        String oracleText,
        Cost cost,
        Effect effect,
        ActivationTiming timing,
        ActivationLimit limit,
        Set<ZoneType> activatesFrom)
        implements Ability {

    /// Creates an ActivatedAbility from a parsed cost and effect.
    ///
    /// Automatically determines timing and limit:
    /// - Loyalty cost → SORCERY timing, once per turn
    /// - Mana ability effect (no target, adds mana) → MANA_ABILITY timing
    /// - Otherwise → INSTANT timing, unlimited
    ///
    /// @param cost the parsed cost
    /// @param effect the parsed effect
    /// @return the constructed activated ability
    public static ActivatedAbility create(Cost cost, Effect effect) {
        var timing = cost.isLoyaltyCost()
                ? ActivationTiming.SORCERY
                : effect.isManaAbilityEffect() ? ActivationTiming.MANA_ABILITY : ActivationTiming.INSTANT;
        var limit = cost.isLoyaltyCost() ? OncePerTurn.INSTANCE : ActivationLimit.UNLIMITED;
        var oracleText = cost.description() + ": " + "...";
        return new ActivatedAbility(
                new AbilityId(), oracleText, cost, effect, timing, limit, Set.of(ZoneType.BATTLEFIELD));
    }

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
    public boolean isManaAbility() {
        return !isLoyaltyAbility() && effect.isManaAbilityEffect();
    }

    /// Returns true if this is a loyalty ability ({@mtg.rule 606.1}).
    ///
    /// Loyalty abilities are activated abilities on planeswalkers whose cost
    /// involves adding or removing loyalty counters. They can only be activated:
    /// - At sorcery speed
    /// - Once per planeswalker per turn
    ///
    /// @return true if this is a loyalty ability
    public boolean isLoyaltyAbility() {
        return cost.isLoyaltyCost();
    }

    /// Returns true if this ability can currently be activated ({@mtg.rule 602.2}).
    ///
    /// Checks whether the activation limit allows activation (e.g., loyalty abilities
    /// can only be activated once per turn per planeswalker).
    ///
    /// @param context the activation context providing game state for the check
    /// @return true if the ability can be activated
    public boolean canActivate(ActivationContext context) {
        return limit.canActivate(id, context.tracker());
    }
}
