package be.imgn.mtg.engine.characteristics;

import java.util.stream.Collector;

import be.imgn.mtg.engine.characteristics.internal.Characteristics;
import be.imgn.mtg.engine.characteristics.internal.DefaultSupertypes;

/// The supertype characteristic of a game object ({@mtg.rule 205.4}).
///
/// Supertypes are printed before the card type on the type line. An object may have multiple
/// supertypes. Supertypes don't affect what a card does by themselves, but other rules apply
/// to them (e.g., the "legend rule" for legendary permanents).
public interface Supertypes extends Characteristics<Supertype> {

    /// Returns an empty Supertypes collection.
    ///
    /// @return an empty collection
    static Supertypes empty() {
        return DefaultSupertypes.empty();
    }

    /// Returns a Supertypes collection containing the specified supertypes.
    ///
    /// @param supertypes the supertypes to include
    /// @return a collection containing the supertypes
    static Supertypes of(Supertype... supertypes) {
        return DefaultSupertypes.of(supertypes);
    }

    /// Returns a new builder for Supertypes.
    ///
    /// @return a new builder
    static Builder builder() {
        return DefaultSupertypes.builder();
    }

    /// Returns a Collector that accumulates Supertype elements into a Supertypes collection.
    ///
    /// @return a Collector for Supertypes
    static Collector<Supertype, ?, Supertypes> toSupertypes() {
        return DefaultSupertypes.collector();
    }

    @Override
    Builder toBuilder();

    /// Returns true if this contains Basic.
    ///
    /// @return true if basic is present
    default boolean isBasic() {
        return contains(Supertype.BASIC);
    }

    /// Returns true if this contains Legendary.
    ///
    /// @return true if legendary is present
    default boolean isLegendary() {
        return contains(Supertype.LEGENDARY);
    }

    /// Returns true if this contains Snow.
    ///
    /// @return true if snow is present
    default boolean isSnow() {
        return contains(Supertype.SNOW);
    }

    /// Returns true if this contains World.
    ///
    /// @return true if world is present
    default boolean isWorld() {
        return contains(Supertype.WORLD);
    }

    /// Builder for Supertypes.
    interface Builder extends Characteristics.Builder<Supertype, Supertypes, Builder> {}
}
