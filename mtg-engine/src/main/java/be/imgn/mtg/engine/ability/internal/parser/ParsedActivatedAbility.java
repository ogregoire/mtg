package be.imgn.mtg.engine.ability.internal.parser;

import java.util.Set;

import be.imgn.mtg.engine.ability.AbilityId;
import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.ability.ActivationLimit;
import be.imgn.mtg.engine.ability.ActivationTiming;
import be.imgn.mtg.engine.ability.OncePerTurn;
import be.imgn.mtg.engine.cost.Cost;
import be.imgn.mtg.engine.effect.Effect;
import be.imgn.mtg.engine.zone.ZoneType;

/// An activated ability parsed from oracle text.
///
/// Determines timing and limit automatically based on the cost and effect:
/// - Loyalty costs → sorcery timing, once per turn
/// - Mana ability effects → mana ability timing
/// - Otherwise → instant timing, unlimited
///
/// @param id the unique ability identifier
/// @param oracleText the original oracle text
/// @param cost the activation cost
/// @param effect the effect produced on resolution
/// @param timing when this ability can be activated
/// @param limit how often this ability can be activated
/// @param activatesFrom the zones this ability can be activated from
public record ParsedActivatedAbility(
        AbilityId id,
        String oracleText,
        Cost cost,
        Effect effect,
        ActivationTiming timing,
        ActivationLimit limit,
        Set<ZoneType> activatesFrom)
        implements ActivatedAbility {

    /// Creates a ParsedActivatedAbility from a parsed cost and effect.
    ///
    /// Automatically determines timing and limit:
    /// - Loyalty cost → SORCERY timing, once per turn
    /// - Mana ability effect (no target, adds mana) → MANA_ABILITY timing
    /// - Otherwise → INSTANT timing, unlimited
    ///
    /// @param cost the parsed cost
    /// @param effect the parsed effect
    /// @return the constructed activated ability
    static ParsedActivatedAbility create(Cost cost, Effect effect) {
        var timing = cost.isLoyaltyCost()
                ? ActivationTiming.SORCERY
                : effect.isManaAbilityEffect() ? ActivationTiming.MANA_ABILITY : ActivationTiming.INSTANT;
        var limit = cost.isLoyaltyCost() ? OncePerTurn.INSTANCE : ActivationLimit.UNLIMITED;
        var oracleText = cost.description() + ": " + "...";
        return new ParsedActivatedAbility(
                new AbilityId(), oracleText, cost, effect, timing, limit, Set.of(ZoneType.BATTLEFIELD));
    }
}
