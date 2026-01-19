package be.imgn.mtg.engine.zone.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.zone.Graveyard;
import be.imgn.mtg.engine.zone.Hand;
import be.imgn.mtg.engine.zone.Library;

/// Guice module providing per-player zone implementations.
///
/// Provides singleton instances of zones that are specific to a player:
/// - [Library] - the player's deck
/// - [Hand] - the player's hand
/// - [Graveyard] - the player's discard pile
///
/// This module is installed for each player's child injector. The Player
/// is injected into each @Provides method from the same injector.
public final class PlayerZonesModule extends AbstractModule {

    @Override
    protected void configure() {}

    @Provides
    @Singleton
    Library provideLibrary(Player player) {
        return new DefaultLibrary(player);
    }

    @Provides
    @Singleton
    Hand provideHand(Player player) {
        return new DefaultHand(player);
    }

    @Provides
    @Singleton
    Graveyard provideGraveyard(Player player) {
        return new DefaultGraveyard(player);
    }
}
