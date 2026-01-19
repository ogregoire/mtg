package be.imgn.mtg.engine.resolver.internal;

import be.imgn.mtg.engine.game.LifeChangeEvent;
import be.imgn.mtg.engine.resolver.EventResolver;
import be.imgn.mtg.engine.state.GameState;

/// Resolver for life change events.
///
/// Modifies a player's life total according to the event.
public final class LifeChangeResolver implements EventResolver<LifeChangeEvent> {

    @Override
    public Class<LifeChangeEvent> eventType() {
        return LifeChangeEvent.class;
    }

    @Override
    public void resolve(LifeChangeEvent event, GameState state) {
        // The actual life total modification would be done here
        // This requires Player to have a mutable life total or a PlayerState
        // For now, this is a placeholder for the actual implementation
        // player.setLifeTotal(player.getLifeTotal() + event.amount());
    }
}
