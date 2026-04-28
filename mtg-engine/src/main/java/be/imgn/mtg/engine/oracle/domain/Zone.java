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

        /// "\[ordinal\] from the \[top|bottom\]" — position-based library
        /// destination (Long-Term Plans: "put that card third from the
        /// top."). Implicit possessive is the controller's own library.
        record NthFromLibraryEnd(int ordinal, End end) implements Destination {
            public enum End {
                TOP,
                BOTTOM
            }
        }

        /// "on top of \[possessive\] library" or just "on top" (possessive
        /// null — the library is implicit from a preceding Search clause,
        /// e.g., Cruel Tutor: "Search your library … put that card on top.").
        record TopOfLibrary(@Nullable String possessive) implements Destination {}

        /// "on the bottom of \[possessive\] library" or just "on the bottom"
        /// (possessive null when the library is implicit).
        record BottomOfLibrary(@Nullable String possessive) implements Destination {}

        /// "\[chooser\]'s choice of the top or bottom of \[possessive\]
        /// library" — the actor picks which end at resolution (Misleading
        /// Motes: "Target creature's owner puts it on their choice of the
        /// top or bottom of their library.").
        record ChoiceOfTopOrBottomOfLibrary(Subject chooser, String possessive) implements Destination {}

        record IntoZone(@Nullable String possessive, ZoneName name) implements Destination {}

        /// "into \[possessive\] \[zone\] or \[zone\]" — player-chosen destination
        /// between two zones sharing the same possessive (Dina's Guidance:
        /// "put it into your hand or graveyard").
        record ChoiceOfZones(@Nullable String possessive, ZoneName first, ZoneName second) implements Destination {}

        /// "into \[possessive\] library just beneath the top N cards of that
        /// library" — variable-depth library insertion (Unexpectedly Absent:
        /// "Put target nonland permanent into its owner's library just beneath
        /// the top X cards of that library."). The [depth] is the number of
        /// cards above the inserted card; a depth of X binds the spell's X.
        record BeneathTopCards(Amount depth, @Nullable String possessive) implements Destination {}

        record ToHand(String description) implements Destination {}

        /// Creates an [OntoBattlefield] destination.
        static Destination ontoBattlefield(boolean tapped, @Nullable String controller) {
            return new OntoBattlefield(tapped, controller);
        }

        /// Creates a [TopOfLibrary] destination.
        static Destination topOfLibrary(@Nullable String possessive) {
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

        /// Creates a [BeneathTopCards] destination.
        static Destination beneathTopCards(Amount depth, @Nullable String possessive) {
            return new BeneathTopCards(depth, possessive);
        }

        /// Creates a [ToHand] destination.
        static Destination toHand(String description) {
            return new ToHand(description);
        }
    }

    sealed interface Source {
        record FromZone(Zone zone) implements Source {}

        record FromAmong(String description) implements Source {}

        /// "from anywhere other than \[zone\]" — zone-agnostic source
        /// with one excluded zone (Vega, the Watcher: "cast a spell
        /// from anywhere other than your hand"). Captured structurally
        /// so the engine can match against the cast's originating zone.
        record FromAnywhereExcept(Zone except) implements Source {}

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
