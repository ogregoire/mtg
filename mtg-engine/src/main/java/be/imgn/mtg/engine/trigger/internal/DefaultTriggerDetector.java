package be.imgn.mtg.engine.trigger.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import be.imgn.mtg.engine.event.GameEvent;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.trigger.TriggerDetector;
import be.imgn.mtg.engine.trigger.TriggeredAbility;
import be.imgn.mtg.engine.trigger.TriggeredAbilityInstance;

/// Default implementation of the trigger detector.
///
/// Maintains a registry of triggered abilities indexed by source and checks
/// events against all registered abilities to find those that trigger.
public final class DefaultTriggerDetector implements TriggerDetector {

    private final Map<GameObject, List<RegisteredTrigger>> triggersBySource = new ConcurrentHashMap<>();

    @Override
    public void register(TriggeredAbility ability, GameObject source, Player controller) {
        triggersBySource
                .computeIfAbsent(source, k -> new ArrayList<>())
                .add(new RegisteredTrigger(ability, source, controller));
    }

    @Override
    public void unregister(GameObject source) {
        triggersBySource.remove(source);
    }

    @Override
    public List<TriggeredAbilityInstance> detect(GameEvent event, GameState state) {
        var triggered = new ArrayList<TriggeredAbilityInstance>();

        for (var triggers : triggersBySource.values()) {
            for (var registered : triggers) {
                if (shouldTrigger(registered, event, state)) {
                    triggered.add(new TriggeredAbilityInstance(
                            registered.ability(), registered.source(), registered.controller(), event));
                }
            }
        }

        return triggered;
    }

    private boolean shouldTrigger(RegisteredTrigger registered, GameEvent event, GameState state) {
        var ability = registered.ability();

        // Check if the source is in a zone where the ability functions
        var sourceZone = state.findZone(registered.source());
        if (sourceZone.isEmpty()) {
            return false;
        }

        var zoneType = sourceZone.get().type();
        if (!ability.triggersFrom().contains(zoneType)) {
            return false;
        }

        // Check the trigger condition
        var condition = ability.condition();
        if (!condition.matches(event, state)) {
            return false;
        }

        // Check intervening-if clause if present
        var interveningIf = ability.interveningIf();
        if (interveningIf.isPresent() && !interveningIf.get().test(state)) {
            return false;
        }

        return true;
    }

    private record RegisteredTrigger(TriggeredAbility ability, GameObject source, Player controller) {}
}
