package be.imgn.mtg.engine.event.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import be.imgn.mtg.engine.event.Event;
import be.imgn.mtg.engine.event.EventBus;
import be.imgn.mtg.engine.event.EventTracker;
import be.imgn.mtg.engine.turn.TurnStartedEvent;
import be.imgn.mtg.engine.util.ListMultimap;

/// Default implementation of [EventTracker].
final class DefaultEventTracker implements EventTracker {

    private final List<Event> events = new ArrayList<>();
    private final ListMultimap<Integer, Event> eventsPerTurn = ListMultimap.newTreeListMultimap();
    private int currentTurn = 0;

    DefaultEventTracker(EventBus eventBus) {
        eventBus.observe(Event.class, this::recordEvent);
    }

    private void recordEvent(Event event) {
        if (event instanceof TurnStartedEvent(var turn)) {
            this.currentTurn = turn;
        }
        events.add(event);
        eventsPerTurn.put(currentTurn, event);
    }

    @Override
    public Stream<Event> eventsFromThisTurn() {
        return eventsPerTurn.get(currentTurn).stream();
    }

    @Override
    public Stream<Event> eventsFromPreviousTurn() {
        return eventsPerTurn.get(currentTurn - 1).stream();
    }
}
