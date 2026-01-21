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
import be.imgn.mtg.engine.turn.PhaseStartedEvent;
import be.imgn.mtg.engine.turn.PhaseType;
import be.imgn.mtg.engine.turn.PrioritySystem;
import be.imgn.mtg.engine.zone.Battlefield;
import be.imgn.mtg.engine.zone.Stack;

/// Handler for loyalty abilities on planeswalkers ({@mtg.rule 606}).
///
/// Loyalty abilities have special restrictions:
/// 1. Can only be activated at sorcery speed (main phase, empty stack, priority)
/// 2. Can only be activated once per planeswalker per turn
/// 3. The cost involves adding or removing loyalty counters
final class LoyaltyAbilityHandler {

    private final PrioritySystem prioritySystem;
    private final EventTracker eventTracker;
    private final EventBus eventBus;

    LoyaltyAbilityHandler(PrioritySystem prioritySystem, EventTracker eventTracker, EventBus eventBus) {
        this.prioritySystem = prioritySystem;
        this.eventTracker = eventTracker;
        this.eventBus = eventBus;
    }

    /// Checks if a loyalty ability can be activated.
    boolean canActivate(ActivatedAbility ability, GameObject source, GameState state) {
        if (!ability.isLoyaltyAbility()) {
            return false;
        }

        // Check timing - must be at sorcery speed (during your main phase with empty stack)
        var controller = source.controller();
        var priorityHolder = prioritySystem.currentPriorityHolder();
        if (priorityHolder == null || !priorityHolder.equals(controller)) {
            return false;
        }

        // Must be the active player
        if (!state.activePlayer().equals(controller)) {
            return false;
        }

        // Stack must be empty for sorcery speed
        if (!state.stack().isEmpty()) {
            return false;
        }

        // Must be in main phase
        if (!isMainPhase()) {
            return false;
        }

        // Source must be on the battlefield
        var zone = state.findZone(source.id());
        if (zone.isEmpty() || !(zone.get() instanceof Battlefield)) {
            return false;
        }

        // Check if a loyalty ability has already been activated on this planeswalker this turn
        if (hasActivatedLoyaltyAbilityThisTurn(source)) {
            return false;
        }

        // TODO: Check if costs can be paid (loyalty counter changes)

        return true;
    }

    /// Activates a loyalty ability.
    ActivationResult activate(ActivatedAbility ability, GameObject source, AbilityContext context, Stack stack) {
        if (!canActivate(ability, source, context.state())) {
            return new ActivationResult.Illegal("Loyalty ability cannot be activated");
        }

        // TODO: Pay costs (add/remove loyalty counters)

        // Put ability on the stack
        var abilityOnStack = AbilityOnStack.from(ability, source)
                .name(formatAbilityName(source))
                .build();
        stack.push(abilityOnStack);

        // Fire the activation event (marks this planeswalker as having used a loyalty ability)
        var event = new AbilityActivatedEvent(ability, source, context.controller());
        eventBus.post(event);

        return new ActivationResult.Success(abilityOnStack, List.of());
    }

    /// Checks if we're currently in a main phase by finding the most recent PhaseStartedEvent.
    private boolean isMainPhase() {
        return eventTracker
                .eventsFromThisTurn(PhaseStartedEvent.class)
                .reduce((first, second) -> second) // Get the last (most recent) event
                .map(event -> event.phase() == PhaseType.MAIN)
                .orElse(false);
    }

    /// Checks if a loyalty ability has been activated on the given planeswalker this turn.
    private boolean hasActivatedLoyaltyAbilityThisTurn(GameObject planeswalker) {
        return eventTracker
                .eventsFromThisTurn(AbilityActivatedEvent.class)
                .anyMatch(
                        event -> event.isLoyaltyAbility() && event.source().id().equals(planeswalker.id()));
    }

    private String formatAbilityName(GameObject source) {
        var sourceName = source.name();
        if (sourceName.isEmpty()) {
            return "Loyalty ability";
        }
        return sourceName + "'s loyalty ability";
    }
}
