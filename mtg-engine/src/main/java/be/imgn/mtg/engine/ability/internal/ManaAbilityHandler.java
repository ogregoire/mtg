package be.imgn.mtg.engine.ability.internal;

import java.util.List;

import be.imgn.mtg.engine.ability.AbilityActivatedEvent;
import be.imgn.mtg.engine.ability.AbilityContext;
import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.ability.ActivationResult;
import be.imgn.mtg.engine.cost.internal.TapCost;
import be.imgn.mtg.engine.event.EventBus;
import be.imgn.mtg.engine.mana.AddManaEffect;
import be.imgn.mtg.engine.mana.Mana;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.object.TypedObject;
import be.imgn.mtg.engine.state.GameState;

/// Handler for mana abilities ({@mtg.rule 605}).
///
/// Mana abilities are special because they:
/// 1. Don't use the stack
/// 2. Resolve immediately
/// 3. Can be activated during mana payment (even without priority)
/// 4. Are never affected by the normal timing restrictions
///
/// An activated mana ability meets all of these criteria ({@mtg.rule 605.1a}):
/// - It could add mana to a player's mana pool when it resolves
/// - It doesn't require a target
/// - It's not a loyalty ability
final class ManaAbilityHandler {

    private final EventBus eventBus;

    ManaAbilityHandler(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /// Checks if a mana ability can be activated.
    ///
    /// Mana abilities don't require priority, but the source must be in
    /// a zone where the ability functions.
    boolean canActivate(ActivatedAbility ability, TypedObject source, GameState state) {
        if (!ability.isManaAbility()) {
            return false;
        }

        // Check the source is in a zone where the ability can be activated
        var zone = state.findZone(source);
        if (zone.isEmpty()) {
            return false; // Source no longer exists
        }
        if (!ability.activatesFrom().contains(zone.get().type())) {
            return false;
        }

        // Mana abilities don't have activation limits (they don't use the stack)
        // but some may have restrictions like "Activate only once each turn"
        // For basic mana abilities, we skip this check
        // TODO: Handle "activate only once each turn" mana abilities

        // Check if costs can be paid
        if (ability.cost() instanceof TapCost && source instanceof Permanent p) {
            if (!p.canTap()) {
                return false;
            }
        }

        return true;
    }

    /// Activates a mana ability (resolves immediately, no stack).
    ActivationResult activate(ActivatedAbility ability, TypedObject source, AbilityContext context) {
        if (!canActivate(ability, source, context.state())) {
            return new ActivationResult.Illegal("Mana ability cannot be activated");
        }

        // Pay costs
        if (ability.cost() instanceof TapCost && source instanceof Permanent p) {
            p.tap();
        }

        // Resolve immediately — add mana to controller's mana pool
        if (ability.effect() instanceof AddManaEffect.Exact exact) {
            for (var type : exact.mana()) {
                context.controller().manaPool().add(Mana.of(type, source));
            }
        }
        // Combination and Selection effects require player choices — handled by caller

        // Fire the activation event
        var event = new AbilityActivatedEvent(ability, source, context.controller());
        eventBus.post(event);

        return new ActivationResult.ManaAbilitySuccess(List.of());
    }
}
