package be.imgn.mtg.engine.ability.internal.parser.effect;

import java.util.List;
import java.util.Objects;

/// An effect composed of multiple sub-effects that all resolve together.
///
/// Examples: "Add {G} and draw a card.", "Destroy target creature and you gain 3 life."
///
/// @param effects the sub-effects
public record CompoundEffect(List<Effect> effects) implements Effect {

    /// Creates a new compound effect.
    public CompoundEffect {
        Objects.requireNonNull(effects, "effects");
        if (effects.size() < 2) {
            throw new IllegalArgumentException("A compound effect must have at least two sub-effects");
        }
        effects = List.copyOf(effects);
    }

    @Override
    public boolean addsMana() {
        return effects.stream().anyMatch(Effect::addsMana);
    }

    @Override
    public boolean requiresTarget() {
        return effects.stream().anyMatch(Effect::requiresTarget);
    }
}
