package be.imgn.mtg.engine.resolver.internal;

import be.imgn.mtg.engine.characteristics.CounterEvent;
import be.imgn.mtg.engine.resolver.EventResolver;
import be.imgn.mtg.engine.state.GameState;

/// Resolver for counter events.
///
/// Adds or removes counters from game objects.
public final class CounterResolver implements EventResolver<CounterEvent> {

    @Override
    public Class<CounterEvent> eventType() {
        return CounterEvent.class;
    }

    @Override
    public void resolve(CounterEvent event, GameState state) {
        var objectId = event.objectId();
        var counterType = event.counterType();
        var amount = event.amount();

        var object = state.findObject(objectId);
        if (object.isEmpty()) {
            return;
        }

        // The actual counter modification would be done here
        // This requires the object to have mutable counters
        // if (object.get() instanceof Permanent permanent) {
        //     if (amount > 0) {
        //         permanent.counters().add(counterType, amount);
        //     } else {
        //         permanent.counters().remove(counterType, -amount);
        //     }
        // }
    }
}
