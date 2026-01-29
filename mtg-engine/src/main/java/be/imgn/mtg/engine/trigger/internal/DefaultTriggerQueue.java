package be.imgn.mtg.engine.trigger.internal;

import java.util.ArrayList;
import java.util.List;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.AbilityOnStack;
import be.imgn.mtg.engine.object.TypedObject;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.trigger.TriggerQueue;
import be.imgn.mtg.engine.trigger.TriggeredAbilityInstance;
import be.imgn.mtg.engine.zone.Stack;

/// Default implementation of the trigger queue.
///
/// Holds triggered abilities until they are flushed to the stack in APNAP order.
public final class DefaultTriggerQueue implements TriggerQueue {

    private final List<TriggeredAbilityInstance> pending = new ArrayList<>();

    @Override
    public void add(TriggeredAbilityInstance instance) {
        pending.add(instance);
    }

    @Override
    public void addAll(List<TriggeredAbilityInstance> instances) {
        pending.addAll(instances);
    }

    @Override
    public boolean hasPending() {
        return !pending.isEmpty();
    }

    @Override
    public void flushToStack(Stack stack, GameState state, Player activePlayer) {
        if (pending.isEmpty()) {
            return;
        }

        // Group triggers by controller
        var byController = new ArrayList<TriggeredAbilityInstance>();
        var byOthers = new ArrayList<TriggeredAbilityInstance>();

        for (var instance : pending) {
            if (instance.controller().equals(activePlayer)) {
                byController.add(instance);
            } else {
                byOthers.add(instance);
            }
        }

        // APNAP order: Active player's triggers go on the stack first (bottom)
        // Then non-active player's triggers on top
        // This means non-active player's triggers resolve first
        putOnStack(stack, state, byController);
        putOnStack(stack, state, byOthers);

        pending.clear();
    }

    @Override
    public void clear() {
        pending.clear();
    }

    private void putOnStack(Stack stack, GameState state, List<TriggeredAbilityInstance> instances) {
        // Each player chooses the order of their triggers.
        // TODO: Let player choose order when multiple triggers
        // For now, use the order they were added.
        for (var instance : instances) {
            var source = instance.source();

            // Create the ability on stack
            var abilityOnStack = AbilityOnStack.from(instance.ability(), source)
                    .name(formatTriggerName(source))
                    .build();

            stack.push(abilityOnStack);
        }
    }

    private String formatTriggerName(TypedObject source) {
        var sourceName = source.name();
        if (sourceName.isEmpty()) {
            return "Triggered ability";
        }
        return sourceName + "'s triggered ability";
    }
}
