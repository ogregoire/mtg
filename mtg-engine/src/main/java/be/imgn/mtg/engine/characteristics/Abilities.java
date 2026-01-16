package be.imgn.mtg.engine.characteristics;

import be.imgn.mtg.engine.characteristics.internal.Characteristics;
import be.imgn.mtg.engine.characteristics.internal.DefaultAbilities;

/// A collection of abilities.
public interface Abilities extends Characteristics<Ability> {

    /// Returns an empty Abilities collection.
    static Abilities empty() {
        return DefaultAbilities.empty();
    }

    /// Returns an Abilities collection containing the specified abilities.
    static Abilities of(Ability... abilities) {
        return DefaultAbilities.of(abilities);
    }

    /// Returns a new builder for Abilities.
    static Builder builder() {
        return DefaultAbilities.builder();
    }

    @Override
    Builder toBuilder();

    /// Builder for Abilities.
    interface Builder extends Characteristics.Builder<Ability, Abilities, Builder> {}
}
