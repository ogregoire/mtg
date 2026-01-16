package be.imgn.mtg.engine.characteristics;

import be.imgn.mtg.engine.characteristics.internal.Characteristics;
import be.imgn.mtg.engine.characteristics.internal.DefaultSupertypes;

/// A collection of supertypes for a game object.
public interface Supertypes extends Characteristics<Supertype> {

    /// Returns an empty Supertypes collection.
    static Supertypes empty() {
        return DefaultSupertypes.empty();
    }

    /// Returns a Supertypes collection containing the specified supertypes.
    static Supertypes of(Supertype... supertypes) {
        return DefaultSupertypes.of(supertypes);
    }

    /// Returns a new builder for Supertypes.
    static Builder builder() {
        return DefaultSupertypes.builder();
    }

    @Override
    Builder toBuilder();

    /// Returns true if this contains Basic.
    default boolean isBasic() {
        return contains(Supertype.BASIC);
    }

    /// Returns true if this contains Legendary.
    default boolean isLegendary() {
        return contains(Supertype.LEGENDARY);
    }

    /// Returns true if this contains Snow.
    default boolean isSnow() {
        return contains(Supertype.SNOW);
    }

    /// Returns true if this contains World.
    default boolean isWorld() {
        return contains(Supertype.WORLD);
    }

    /// Builder for Supertypes.
    interface Builder extends Characteristics.Builder<Supertype, Supertypes, Builder> {}
}
