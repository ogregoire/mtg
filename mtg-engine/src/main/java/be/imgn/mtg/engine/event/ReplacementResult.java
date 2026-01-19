package be.imgn.mtg.engine.event;

import java.util.List;

/// Result of applying a replacement effect to a game event ({@mtg.rule 614}).
///
/// Replacement effects modify how events occur. A replacement can:
/// - Modify the event (e.g., entering tapped instead of untapped)
/// - Prevent the event entirely (e.g., prevention shields)
/// - Split the event into multiple events (e.g., doubling effects)
public sealed interface ReplacementResult {

    /// The event was modified and should continue with the new form.
    ///
    /// @param event the modified event
    record Modified(GameEvent event) implements ReplacementResult {}

    /// The event was prevented entirely and should not occur.
    record Prevented() implements ReplacementResult {}

    /// The event was split into multiple events that should all occur.
    ///
    /// @param events the list of events to process instead
    record Multiple(List<GameEvent> events) implements ReplacementResult {
        /// Creates a Multiple result with a defensive copy of the events list.
        public Multiple {
            events = List.copyOf(events);
        }
    }
}
