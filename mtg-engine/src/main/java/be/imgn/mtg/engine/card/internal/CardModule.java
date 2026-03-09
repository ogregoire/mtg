package be.imgn.mtg.engine.card.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import be.imgn.mtg.engine.card.CardFetcher;

/// Guice module for card-related bindings.
public final class CardModule extends AbstractModule {

    @Provides
    @Singleton
    CardFetcher provideCardFetcher() {
        return new DefaultCardFetcher();
    }
}
