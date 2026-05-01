package be.imgn.mtg.engine.oracle.domain;

import static java.util.Objects.requireNonNull;

import java.util.List;

import org.jspecify.annotations.Nullable;

/// Zone reference in oracle text (rule 400). Two-axis split:
///
/// - [Shared] — zones with no owner (battlefield, stack, exile,
///   command). One canonical instance per kind.
/// - [Owned] — zones each player has their own of (library, hand,
///   graveyard). Carry a [Subject] `owner`.
///
/// `Zone` itself represents a *single* zone reference; [Zone.Name]
/// returns its canonical name. Multi-zone references (e.g.,
/// "target player's hand and graveyard") are not modelled as a Zone
/// variant; instead, the consumer holds `List<Zone>` or uses
/// [Source.FromZones].
public sealed interface Zone permits Zone.Shared, Zone.Owned {

    /// The canonical zone name (rule 400.1). Named `kind` rather
    /// than `name` because [Shared] is itself a Java enum and
    /// `Enum#name()` is final, so a method called `name()` here
    /// would clash on the enum subtype.
    Name kind();

    /// All seven MTG zones (rule 400.1).
    enum Name implements Parseable {
        LIBRARY("[Library|Libraries]"),
        HAND("Hand(s)"),
        GRAVEYARD("Graveyard(s)"),
        BATTLEFIELD("Battlefield"),
        STACK("Stack"),
        EXILE("Exile"),
        COMMAND("Command zone");

        private final String text;

        Name(String text) {
            this.text = text;
        }

        @Override
        public String text() {
            return text;
        }
    }

    // ── Shared zones (no owner) ──────────────────────────────────────

    /// Battlefield, stack, exile, command — no per-player instance.
    enum Shared implements Zone {
        BATTLEFIELD(Name.BATTLEFIELD),
        STACK(Name.STACK),
        EXILE(Name.EXILE),
        COMMAND(Name.COMMAND);

        private final Name name;

        Shared(Name name) {
            this.name = name;
        }

        @Override
        public Name kind() {
            return name;
        }
    }

    // ── Owned zones (per-player) ─────────────────────────────────────

    /// Library, hand, graveyard — each player has their own. The
    /// `owner` is the player whose copy of the zone is referenced
    /// and is **required** (rejecting `null` at construction). The
    /// universal "all hands" case uses
    /// `Subject.player(PlayerRef.Pronoun.EACH_PLAYER)`. The
    /// implicit-from-context case ("on top" with no library named)
    /// belongs on [Zone.Destination.TopOfLibrary] / similar (where
    /// it stays `@Nullable`) — `Owned` itself never represents an
    /// implicit owner.
    ///
    /// `Owned` represents a *single* zone. For multi-zone references
    /// ("hand and graveyard"), use [Zone.Source.FromZones] or pass a
    /// `List<Zone>` directly — `Zone` itself never aggregates.
    sealed interface Owned extends Zone permits Owned.Library, Owned.Hand, Owned.Graveyard {

        Subject owner();

        record Library(Subject owner) implements Owned {
            public Library {
                requireNonNull(owner, "Owned.Library.owner");
            }

            @Override
            public Name kind() {
                return Name.LIBRARY;
            }
        }

        record Hand(Subject owner) implements Owned {
            public Hand {
                requireNonNull(owner, "Owned.Hand.owner");
            }

            @Override
            public Name kind() {
                return Name.HAND;
            }
        }

        record Graveyard(Subject owner) implements Owned {
            public Graveyard {
                requireNonNull(owner, "Owned.Graveyard.owner");
            }

            @Override
            public Name kind() {
                return Name.GRAVEYARD;
            }
        }
    }

    /// Convenience: the battlefield singleton.
    static Zone battlefield() {
        return Shared.BATTLEFIELD;
    }

    /// Convenience: the exile singleton.
    static Zone exile() {
        return Shared.EXILE;
    }

    // ── Destinations ─────────────────────────────────────────────────

    /// A zone-with-context destination — used by zone-move effects
    /// (`Bounce`, `ZoneMove`, etc.). Some variants carry context the
    /// engine needs at resolution (e.g., the chooser of a "top or
    /// bottom" pick) and a nullable owner because the library /
    /// graveyard is implicit from a preceding clause.
    sealed interface Destination {

        record Battlefield(boolean tapped, @Nullable Subject controller) implements Destination {
            public Battlefield() {
                this(false, null);
            }

            public Battlefield withTapped(boolean tapped) {
                return new Battlefield(tapped, controller);
            }

            public Battlefield withController(Subject controller) {
                return new Battlefield(tapped, controller);
            }
        }

        /// "\[ordinal\] from the \[top|bottom\]" — position-based
        /// library destination (Long-Term Plans). Implicit owner is
        /// the controller's own library.
        record NthFromLibraryEnd(int ordinal, End end) implements Destination {
            public enum End {
                TOP,
                BOTTOM
            }
        }

        /// "on top of \[owner\]'s library" or just "on top"
        /// (`owner = null` — implicit from a preceding Search clause).
        record TopOfLibrary(@Nullable Subject owner) implements Destination {}

        /// "on the bottom of \[owner\]'s library" or just "on the
        /// bottom" (`owner = null` when implicit).
        record BottomOfLibrary(@Nullable Subject owner) implements Destination {}

        /// "\[chooser\]'s choice of the top or bottom of \[owner\]'s
        /// library" — actor picks which end at resolution
        /// (Misleading Motes).
        record ChoiceOfTopOrBottomOfLibrary(Subject chooser, Subject owner) implements Destination {}

        /// "into \[owner\]'s \[zone\]". `owner = null` when the zone is
        /// implicit (rare — usually for shared zones that don't take
        /// an owner anyway).
        record IntoZone(@Nullable Subject owner, Name name) implements Destination {}

        /// "into \[owner\]'s \[zone\] or \[zone\]" — player-chosen
        /// destination between two zones sharing the same owner
        /// (Dina's Guidance: "put it into your hand or graveyard").
        record ChoiceOfZones(@Nullable Subject owner, Name first, Name second) implements Destination {}

        /// "into \[owner\]'s library just beneath the top N cards of
        /// that library" — variable-depth library insertion
        /// (Unexpectedly Absent).
        record BeneathTopCards(Amount depth, @Nullable Subject owner) implements Destination {}

        /// "to \[owner\]'s hand". Common forms: `owner = YOU` ("to
        /// your hand"), `owner = THEY` ("to their hand"), or the
        /// possessive `Subject.possessiveSubject("its", "owner")`
        /// for "its owner's hand".
        record ToHand(@Nullable Subject owner) implements Destination {}

        static Destination ontoBattlefield(boolean tapped, @Nullable Subject controller) {
            return new Battlefield(tapped, controller);
        }

        static Destination topOfLibrary(@Nullable Subject owner) {
            return new TopOfLibrary(owner);
        }

        static Destination bottomOfLibrary(@Nullable Subject owner) {
            return new BottomOfLibrary(owner);
        }

        static Destination intoZone(@Nullable Subject owner, Name name) {
            return new IntoZone(owner, name);
        }

        static Destination beneathTopCards(Amount depth, @Nullable Subject owner) {
            return new BeneathTopCards(depth, owner);
        }

        static Destination toHand(@Nullable Subject owner) {
            return new ToHand(owner);
        }
    }

    // ── Sources ──────────────────────────────────────────────────────

    /// Source of a zone-move (the "from" side).
    sealed interface Source {

        /// A single-zone source (e.g., "from your graveyard").
        record FromZone(Zone zone) implements Source {}

        /// Multi-zone source (Identity Crisis: "target player's
        /// hand and graveyard"; Worldfire: "from all hands and
        /// graveyards"). Each list element is a fully-typed Zone;
        /// the owner is repeated per element rather than carried by
        /// an aggregating Zone variant.
        record FromZones(List<Zone> zones) implements Source {
            public FromZones {
                zones = List.copyOf(zones);
            }
        }

        /// Free-text flavor source ("from among them") that doesn't
        /// name a specific zone.
        record FromAmong(String description) implements Source {}

        /// "from anywhere other than \[zone\]" — zone-agnostic source
        /// with one excluded zone (Vega, the Watcher).
        record FromAnywhereExcept(Zone except) implements Source {}

        static Source fromZone(Zone zone) {
            return new FromZone(zone);
        }

        static Source fromZones(List<Zone> zones) {
            return new FromZones(zones);
        }

        static Source fromAmong(String description) {
            return new FromAmong(description);
        }
    }
}
