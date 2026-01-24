package be.imgn.mtg.engine.event;

import static be.imgn.mtg.engine.util.MoreGatherers.instanceOf;

import java.util.stream.Stream;

/// Tracks events that have occurred during the game for querying.
public interface EventTracker {

    /// Returns all events from the current turn.
    ///
    /// @return a stream of events from this turn
    Stream<Event> eventsFromThisTurn();

    /// Returns events of a specific type from the current turn.
    ///
    /// @param eventType the event class to filter by
    /// @param <E> the event type
    /// @return a stream of matching events from this turn
    default <E extends Event> Stream<E> eventsFromThisTurn(Class<E> eventType) {
        return eventsFromThisTurn().gather(instanceOf(eventType));
    }

    /// Returns all events from the previous turn.
    ///
    /// @return a stream of events from the previous turn
    Stream<Event> eventsFromPreviousTurn();

    /// Returns events of a specific type from the previous turn.
    ///
    /// @param eventType the event class to filter by
    /// @param <E> the event type
    /// @return a stream of matching events from the previous turn
    default <E extends Event> Stream<E> eventsFromPreviousTurn(Class<E> eventType) {
        return eventsFromPreviousTurn().gather(instanceOf(eventType));
    }
}
