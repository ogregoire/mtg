package be.imgn.mtg.engine.oracle.domain;

import java.util.List;

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
        public Named(ZoneName name) {
            this(null, name);
        }
    }

    /// A source that names multiple zones jointly (e.g., Identity Crisis:
    /// "target player's hand and graveyard"). The named zones share the
    /// same possessive; each is a full zone in its own right.
    record Multi(@Nullable String possessive, List<ZoneName> names) implements Zone {}

    /// Returns the [Battlefield] singleton.
    static Zone battlefield() {
        return Battlefield.BATTLEFIELD;
    }

    /// Returns the [ExileZone] singleton.
    static Zone exile() {
        return ExileZone.EXILE;
    }

    /// Creates a [Named] zone.
    static Zone named(@Nullable String possessive, ZoneName name) {
        return new Named(possessive, name);
    }

    sealed interface Destination {
        record OntoBattlefield(boolean tapped, @Nullable String controller) implements Destination {}

        record TopOfLibrary(String possessive) implements Destination {}

        record BottomOfLibrary(String possessive) implements Destination {}

        record IntoZone(@Nullable String possessive, ZoneName name) implements Destination {}

        record ToHand(String description) implements Destination {}

        /// Creates an [OntoBattlefield] destination.
        static Destination ontoBattlefield(boolean tapped, @Nullable String controller) {
            return new OntoBattlefield(tapped, controller);
        }

        /// Creates a [TopOfLibrary] destination.
        static Destination topOfLibrary(String possessive) {
            return new TopOfLibrary(possessive);
        }

        /// Creates a [BottomOfLibrary] destination.
        static Destination bottomOfLibrary(String possessive) {
            return new BottomOfLibrary(possessive);
        }

        /// Creates an [IntoZone] destination.
        static Destination intoZone(@Nullable String possessive, ZoneName name) {
            return new IntoZone(possessive, name);
        }

        /// Creates a [ToHand] destination.
        static Destination toHand(String description) {
            return new ToHand(description);
        }
    }

    sealed interface Source {
        record FromZone(Zone zone) implements Source {}

        record FromAmong(String description) implements Source {}

        /// Creates a [FromZone] source.
        static Source fromZone(Zone zone) {
            return new FromZone(zone);
        }

        /// Creates a [FromAmong] source.
        static Source fromAmong(String description) {
            return new FromAmong(description);
        }
    }
}
