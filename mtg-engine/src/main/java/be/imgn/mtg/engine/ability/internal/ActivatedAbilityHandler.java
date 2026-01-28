package be.imgn.mtg.engine.ability.internal;

import java.util.List;

import be.imgn.mtg.engine.ability.AbilityActivatedEvent;
import be.imgn.mtg.engine.ability.AbilityContext;
import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.ability.ActivationResult;
import be.imgn.mtg.engine.event.EventBus;
import be.imgn.mtg.engine.event.EventTracker;
import be.imgn.mtg.engine.object.AbilityOnStack;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.TurnTracker;
import be.imgn.mtg.engine.zone.Stack;

/// Handler for non-mana, non-loyalty activated abilities ({@mtg.rule 602}).
///
/// This handler processes regular activated abilities:
/// 1. Validates timing (player has priority)
/// 2. Validates the ability can be activated from the source's current zone
/// 3. Validates activation limits
/// 4. Pays costs
/// 5. Puts the ability on the stack
/// 6. Fires the AbilityActivatedEvent
final class ActivatedAbilityHandler {

    private final TurnTracker turnTracker;
    private final EventTracker eventTracker;
    private final EventBus eventBus;

    ActivatedAbilityHandler(TurnTracker turnTracker, EventTracker eventTracker, EventBus eventBus) {
        this.turnTracker = turnTracker;
        this.eventTracker = eventTracker;
        this.eventBus = eventBus;
    }

    /// Checks if a non-mana activated ability can be activated.
    boolean canActivate(ActivatedAbility ability, GameObject source, GameState state) {
        // Check timing - player must have priority
        var controller = source.controller();
        if (!turnTracker.hasPriority(controller)) {
            return false;
        }

        // Check the source is in a zone where the ability can be activated
        var zone = state.findZone(source.id());
        if (zone.isEmpty()) {
            return false; // Source no longer exists
        }
        if (!ability.activatesFrom().contains(zone.get().type())) {
            return false;
        }

        // Check activation limits
        if (!ability.limit().canActivate(ability.id(), eventTracker)) {
            return false;
        }

        // TODO: Check if costs can be paid
        // TODO: Check restriction effects

        return true;
    }

    /// Activates a non-mana activated ability.
    ActivationResult activate(ActivatedAbility ability, GameObject source, AbilityContext context, Stack stack) {
        if (!canActivate(ability, source, context.state())) {
            return new ActivationResult.Illegal("Ability cannot be activated");
        }

        // TODO: Pay costs

        // Put ability on the stack
        var abilityOnStack = AbilityOnStack.from(ability, source)
                .name(formatAbilityName(source))
                .build();
        stack.push(abilityOnStack);

        // Fire the activation event
        var event = new AbilityActivatedEvent(ability, source, context.controller());
        eventBus.post(event);

        return new ActivationResult.Success(abilityOnStack, List.of());
    }

    private String formatAbilityName(GameObject source) {
        var sourceName = source.name();
        if (sourceName.isEmpty()) {
            return "Activated ability";
        }
        return sourceName + "'s ability";
    }
}
