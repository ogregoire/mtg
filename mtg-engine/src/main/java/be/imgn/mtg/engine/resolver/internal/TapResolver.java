package be.imgn.mtg.engine.resolver.internal;

import be.imgn.mtg.engine.object.TapEvent;
import be.imgn.mtg.engine.resolver.EventResolver;
import be.imgn.mtg.engine.state.GameState;

/// Resolver for tap/untap events.
///
/// Changes the tap status of permanents.
public final class TapResolver implements EventResolver<TapEvent> {

    @Override
    public Class<TapEvent> eventType() {
        return TapEvent.class;
    }

    @Override
    public void resolve(TapEvent event, GameState state) {
        var permanent = event.permanent();

        if (event.isTapping()) {
            permanent.tap();
        } else {
            permanent.untap();
        }
    }
}
