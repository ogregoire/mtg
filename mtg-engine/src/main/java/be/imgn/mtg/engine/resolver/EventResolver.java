package be.imgn.mtg.engine.resolver;

import be.imgn.mtg.engine.event.GameEvent;
import be.imgn.mtg.engine.state.GameState;

/// Resolver that applies a game event to mutate game state.
///
/// Event resolvers handle the actual state changes for specific event types.
/// They are called after replacement effects have been applied and the final
/// event form has been determined.
///
/// @param <E> the type of event this resolver handles
public interface EventResolver<E extends GameEvent> {

    /// Returns the event type this resolver handles.
    ///
    /// @return the event class
    Class<E> eventType();

    /// Resolves the event by applying state changes.
    ///
    /// This method mutates the game state to reflect the event occurring.
    /// For zone changes, this includes recording LKI and moving objects.
    ///
    /// @param event the event to resolve
    /// @param state the game state to mutate
    void resolve(E event, GameState state);
}
