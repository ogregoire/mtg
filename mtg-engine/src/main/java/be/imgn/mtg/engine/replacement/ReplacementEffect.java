package be.imgn.mtg.engine.replacement;

import be.imgn.mtg.engine.event.GameEvent;
import be.imgn.mtg.engine.event.ReplacementResult;
import be.imgn.mtg.engine.state.GameState;

/// A replacement effect that modifies how an event occurs ({@mtg.rule 614}).
///
/// Replacement effects use "instead" or "as ... enters" wording. They apply once
/// and modify the event as it happens, rather than triggering afterward.
///
/// When multiple replacement effects could apply to the same event, they are
/// ordered according to Rule 616:
/// 1. Self-replacement effects (affecting only the source)
/// 2. Control-changing effects (for ETB events)
/// 3. Copy effects (for ETB events)
/// 4. All other applicable effects (affected player chooses order)
public interface ReplacementEffect {

    /// Checks if this replacement effect applies to the given event.
    ///
    /// @param event the event to check
    /// @param state the current game state
    /// @return true if this replacement effect can modify the event
    boolean appliesTo(GameEvent event, GameState state);

    /// Applies this replacement effect to an event.
    ///
    /// @param event the event to replace
    /// @param state the current game state
    /// @return the result of the replacement
    ReplacementResult replace(GameEvent event, GameState state);

    /// Returns true if this is a self-replacement effect.
    ///
    /// Self-replacement effects only affect the source object and are
    /// applied first ({@mtg.rule 616.1a}).
    ///
    /// @return true if this is a self-replacement effect
    default boolean isSelfReplacement() {
        return false;
    }

    /// Returns true if this is a control-changing replacement effect.
    ///
    /// Control-changing effects modify which player an object enters under
    /// and are applied before copy effects ({@mtg.rule 616.1b}).
    ///
    /// @return true if this changes control
    default boolean isControlChanging() {
        return false;
    }

    /// Returns true if this is a copy effect.
    ///
    /// Copy effects modify what an object enters as a copy of
    /// and are applied after control-changing effects ({@mtg.rule 616.1c}).
    ///
    /// @return true if this is a copy effect
    default boolean isCopyEffect() {
        return false;
    }
}
