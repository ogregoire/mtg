package be.imgn.mtg.engine.characteristics;

import be.imgn.mtg.engine.characteristics.internal.Characteristics;
import be.imgn.mtg.engine.characteristics.internal.DefaultSubtypes;

/// A collection of subtypes for a game object.
public interface Subtypes extends Characteristics<Subtype> {

    /// Returns an empty Subtypes collection.
    static Subtypes empty() {
        return DefaultSubtypes.empty();
    }

    /// Returns a Subtypes collection containing the specified subtypes.
    static Subtypes of(Subtype... subtypes) {
        return DefaultSubtypes.of(subtypes);
    }

    /// Returns a new builder for Subtypes.
    static Builder builder() {
        return DefaultSubtypes.builder();
    }

    @Override
    Builder toBuilder();

    /// Returns true if this contains any basic land type.
    default boolean hasBasicLandType() {
        return stream().anyMatch(BasicLandType.class::isInstance);
    }

    /// Builder for Subtypes.
    interface Builder extends Characteristics.Builder<Subtype, Subtypes, Builder> {}
}
