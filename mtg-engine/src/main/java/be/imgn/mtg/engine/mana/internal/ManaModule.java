package be.imgn.mtg.engine.mana.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import be.imgn.mtg.engine.mana.ManaPool;
import be.imgn.mtg.engine.mana.PaymentChoiceProvider;

/// Guice module for mana system bindings.
///
/// Provides per-player mana pool and payment choice providers.
public class ManaModule extends AbstractModule {

    @Override
    protected void configure() {
        // Bindings configured via @Provides methods
    }

    @Provides
    @Singleton
    ManaPool provideManaPool(PaymentChoiceProvider choiceProvider) {
        return new DefaultManaPool(choiceProvider);
    }

    @Provides
    @Singleton
    PaymentChoiceProvider providePaymentChoiceProvider() {
        return AutoPayChoiceProvider.INSTANCE;
    }
}
