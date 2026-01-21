package be.imgn.mtg.engine.ability.internal;

import be.imgn.mtg.engine.ability.Ability;
import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.ability.SpellAbility;
import be.imgn.mtg.engine.ability.StaticAbility;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.trigger.TriggerDetector;
import be.imgn.mtg.engine.trigger.TriggeredAbility;

/// Scans permanents for static and triggered abilities and registers them.
///
/// When a permanent enters the battlefield:
/// - Static abilities are registered with the continuous effect system
/// - Triggered abilities are registered with the trigger detector
///
/// When a permanent leaves the battlefield:
/// - Its abilities are unregistered
final class StaticAbilityScanner {

    private final TriggerDetector triggerDetector;

    StaticAbilityScanner(TriggerDetector triggerDetector) {
        this.triggerDetector = triggerDetector;
    }

    /// Registers all abilities from a permanent that just entered the battlefield.
    void registerAbilities(Permanent permanent, GameState state) {
        for (var ability : permanent.abilities()) {
            registerAbility(ability, permanent, state);
        }
    }

    /// Unregisters all abilities from a permanent leaving the battlefield.
    void unregisterAbilities(Permanent permanent, GameState state) {
        for (var ability : permanent.abilities()) {
            unregisterAbility(ability, permanent, state);
        }
    }

    private void registerAbility(Ability ability, Permanent permanent, GameState state) {
        switch (ability) {
            case StaticAbility staticAbility -> registerStaticAbility(staticAbility, permanent, state);
            case TriggeredAbility triggeredAbility -> registerTriggeredAbility(triggeredAbility, permanent, state);
            case ActivatedAbility ignored -> {
                // Activated abilities don't need registration - they're checked on demand
            }
            case SpellAbility ignored -> {
                // Spell abilities only exist on instants/sorceries, not permanents
            }
        }
    }

    private void unregisterAbility(Ability ability, Permanent permanent, GameState state) {
        switch (ability) {
            case StaticAbility staticAbility -> unregisterStaticAbility(staticAbility, permanent, state);
            case TriggeredAbility triggeredAbility -> unregisterTriggeredAbility(triggeredAbility, permanent, state);
            case ActivatedAbility ignored -> {
                // Activated abilities don't need unregistration
            }
            case SpellAbility ignored -> {
                // Spell abilities only exist on instants/sorceries
            }
        }
    }

    private void registerStaticAbility(StaticAbility ability, Permanent permanent, GameState state) {
        // TODO: Register with the continuous effect system (layer system)
        // This will be implemented when the layer system is added
    }

    private void unregisterStaticAbility(StaticAbility ability, Permanent permanent, GameState state) {
        // TODO: Unregister from the continuous effect system
    }

    private void registerTriggeredAbility(TriggeredAbility ability, Permanent permanent, GameState state) {
        // TODO: Register with the trigger detector
        // The trigger detector needs to track which permanents have which triggered abilities
        // so it can detect when they trigger
    }

    private void unregisterTriggeredAbility(TriggeredAbility ability, Permanent permanent, GameState state) {
        // TODO: Unregister from the trigger detector
    }
}
