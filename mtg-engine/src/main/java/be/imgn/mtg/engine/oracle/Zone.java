package be.imgn.mtg.engine.oracle;

import org.jspecify.annotations.Nullable;

/// Zone reference in oracle text (Rule 400).
public sealed interface Zone {
    enum Battlefield implements Zone {
        BATTLEFIELD
    }

    enum ExileZone implements Zone {
        EXILE
    }

    record Named(@Nullable String possessive, ZoneName name) implements Zone {
        Named(ZoneName name) {
            this(null, name);
        }
    }

    /// Returns the {@link Battlefield} singleton.
    static Zone battlefield() {
        return Battlefield.BATTLEFIELD;
    }

    /// Returns the {@link ExileZone} singleton.
    static Zone exile() {
        return ExileZone.EXILE;
    }

    /// Creates a {@link Named} zone.
    static Zone named(@Nullable String possessive, ZoneName name) {
        return new Named(possessive, name);
    }

    sealed interface Destination {
        record OntoBattlefield(boolean tapped, @Nullable String controller) implements Destination {}

        record TopOfLibrary(String possessive) implements Destination {}

        record BottomOfLibrary(String possessive) implements Destination {}

        record IntoZone(@Nullable String possessive, ZoneName name) implements Destination {}

        record ToHand(String description) implements Destination {}

        /// Creates an {@link OntoBattlefield} destination.
        static Destination ontoBattlefield(boolean tapped, @Nullable String controller) {
            return new OntoBattlefield(tapped, controller);
        }

        /// Creates a {@link TopOfLibrary} destination.
        static Destination topOfLibrary(String possessive) {
            return new TopOfLibrary(possessive);
        }

        /// Creates a {@link BottomOfLibrary} destination.
        static Destination bottomOfLibrary(String possessive) {
            return new BottomOfLibrary(possessive);
        }

        /// Creates an {@link IntoZone} destination.
        static Destination intoZone(@Nullable String possessive, ZoneName name) {
            return new IntoZone(possessive, name);
        }

        /// Creates a {@link ToHand} destination.
        static Destination toHand(String description) {
            return new ToHand(description);
        }
    }

    sealed interface Source {
        record FromZone(Zone zone) implements Source {}

        record FromAmong(String description) implements Source {}

        /// Creates a {@link FromZone} source.
        static Source fromZone(Zone zone) {
            return new FromZone(zone);
        }

        /// Creates a {@link FromAmong} source.
        static Source fromAmong(String description) {
            return new FromAmong(description);
        }
    }
}
