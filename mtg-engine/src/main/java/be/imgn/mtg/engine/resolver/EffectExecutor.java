package be.imgn.mtg.engine.resolver;

import be.imgn.mtg.engine.effect.Effect;

/// Executes parsed effects against the game state during spell/ability resolution.
public interface EffectExecutor {

    /// Executes the given effect.
    ///
    /// @param effect the effect to execute
    /// @param context the resolution context
    void execute(Effect effect, ResolutionContext context);
}
