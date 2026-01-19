package be.imgn.mtg.engine.event;

import java.util.List;

/// Central orchestrator for all game state changes.
///
/// The GameEventProcessor is responsible for:
/// 1. Applying replacement effects (looping until no more apply)
/// 2. Resolving the event (mutating game state)
/// 3. Detecting triggered abilities
/// 4. Posting the event to the EventBus for notification
///
/// All game state changes should flow through this processor to ensure
/// replacement effects and triggers are handled correctly.
public interface GameEventProcessor {

    /// Processes a single game event.
    ///
    /// The event goes through the full processing pipeline:
    /// 1. Replacement effects are applied in Rule 616 order until stable
    /// 2. The final event is resolved to mutate game state
    /// 3. Triggered abilities are detected and added to the trigger queue
    /// 4. The event is posted to the EventBus
    ///
    /// @param event the event to process
    void process(GameEvent event);

    /// Processes multiple events as a batch.
    ///
    /// Batched events are processed together with EventBus notifications
    /// deferred until all events have been processed. This is useful for
    /// simultaneous events like multiple creatures dying at once.
    ///
    /// @param events the events to process
    void processBatch(List<GameEvent> events);
}
