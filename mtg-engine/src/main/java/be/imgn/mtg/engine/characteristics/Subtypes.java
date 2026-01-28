package be.imgn.mtg.engine.characteristics;

import java.util.stream.Collector;

import be.imgn.mtg.engine.characteristics.internal.Characteristics;
import be.imgn.mtg.engine.characteristics.internal.DefaultSubtypes;

/// The subtype characteristic of a game object ({@mtg.rule 205.3}).
///
/// Subtypes are printed on the type line after a long dash. An object may have multiple subtypes.
/// Subtypes are always a single word and are listed after the card type and a long dash.
public interface Subtypes extends Characteristics<Subtype> {

    /// Returns an empty Subtypes collection.
    ///
    /// @return an empty collection
    static Subtypes empty() {
        return DefaultSubtypes.empty();
    }

    /// Returns a Subtypes collection containing the specified subtypes.
    ///
    /// @param subtypes the subtypes to include
    /// @return a collection containing the subtypes
    static Subtypes of(Subtype... subtypes) {
        return DefaultSubtypes.of(subtypes);
    }

    /// Returns a new builder for Subtypes.
    ///
    /// @return a new builder
    static Builder builder() {
        return DefaultSubtypes.builder();
    }

    /// Returns a Collector that accumulates Subtype elements into a Subtypes collection.
    ///
    /// @return a Collector for Subtypes
    static Collector<Subtype, ?, Subtypes> toSubtypes() {
        return DefaultSubtypes.collector();
    }

    @Override
    Builder toBuilder();

    /// Returns true if this contains any basic land type.
    ///
    /// @return true if any basic land type is present
    default boolean hasBasicLandType() {
        return stream().anyMatch(BasicLandType.class::isInstance);
    }

    /// Builder for Subtypes.
    interface Builder extends Characteristics.Builder<Subtype, Subtypes, Builder> {}
}
