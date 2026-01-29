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
        var object = event.object();

        // The actual counter modification would be done here
        // This requires the object to have mutable counters
        // if (object instanceof Permanent permanent) {
        //     if (event.amount() > 0) {
        //         permanent.counters().add(event.counterType(), event.amount());
        //     } else {
        //         permanent.counters().remove(event.counterType(), -event.amount());
        //     }
        // }
    }
}
