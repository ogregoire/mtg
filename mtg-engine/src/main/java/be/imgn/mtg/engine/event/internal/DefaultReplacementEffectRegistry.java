package be.imgn.mtg.engine.event.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import be.imgn.mtg.engine.event.GameEvent;
import be.imgn.mtg.engine.event.ReplacementEffect;
import be.imgn.mtg.engine.event.ReplacementEffectRegistry;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.TypedObject;
import be.imgn.mtg.engine.state.GameState;

/// Default implementation of the replacement effect registry.
///
/// Stores replacement effects indexed by their source object for efficient
/// registration and unregistration. When finding applicable effects, filters
/// and orders them according to Rule 616.
public final class DefaultReplacementEffectRegistry implements ReplacementEffectRegistry {

    private final Map<TypedObject, List<RegisteredEffect>> effectsBySource = new ConcurrentHashMap<>();
    private final GameState gameState;

    public DefaultReplacementEffectRegistry(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public void register(ReplacementEffect effect, TypedObject source, Player controller) {
        effectsBySource
                .computeIfAbsent(source, k -> new ArrayList<>())
                .add(new RegisteredEffect(effect, source, controller));
    }

    @Override
    public void unregister(TypedObject source) {
        effectsBySource.remove(source);
    }

    @Override
    public List<ApplicableReplacement> findApplicable(GameEvent event) {
        var applicable = new ArrayList<ApplicableReplacement>();

        for (var effects : effectsBySource.values()) {
            for (var registered : effects) {
                if (registered.effect().appliesTo(event, gameState)) {
                    applicable.add(new ApplicableReplacement(
                            registered.effect(), registered.source(), registered.controller()));
                }
            }
        }

        // Sort by Rule 616 ordering
        applicable.sort(rule616Comparator());

        return applicable;
    }

    /// Creates a comparator that orders replacement effects according to Rule 616.
    ///
    /// Order:
    /// 1. Self-replacement effects first
    /// 2. Control-changing effects
    /// 3. Copy effects
    /// 4. All others (player chooses among these)
    private Comparator<ApplicableReplacement> rule616Comparator() {
        return Comparator.comparingInt(ar -> {
            var effect = ar.effect();
            if (effect.isSelfReplacement()) {
                return 0;
            }
            if (effect.isControlChanging()) {
                return 1;
            }
            if (effect.isCopyEffect()) {
                return 2;
            }
            return 3;
        });
    }

    private record RegisteredEffect(ReplacementEffect effect, TypedObject source, Player controller) {}
}
