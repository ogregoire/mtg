package be.imgn.mtg.engine.zone;

/// Enumeration of zone types in Magic: The Gathering ({@mtg.rule 400}).
///
/// Each zone has specific properties that determine how objects interact with it:
/// - **Public/Hidden**: Whether contents are visible to all players
/// - **Ordered/Unordered**: Whether the order of objects matters
/// - **Shared/Per-player**: Whether all players share the zone or each has their own
///
/// @see Zone
public enum ZoneType {

    /// The library zone ({@mtg.rule 401}).
    ///
    /// A player's deck. Hidden, ordered, per-player.
    LIBRARY(false, true, false),

    /// The hand zone ({@mtg.rule 402}).
    ///
    /// Cards held by a player. Hidden, unordered, per-player.
    HAND(false, false, false),

    /// The battlefield zone ({@mtg.rule 403}).
    ///
    /// Where permanents exist. Public, unordered, shared.
    BATTLEFIELD(true, false, true),

    /// The graveyard zone ({@mtg.rule 404}).
    ///
    /// A player's discard pile. Public, ordered, per-player.
    GRAVEYARD(true, true, false),

    /// The stack zone ({@mtg.rule 405}).
    ///
    /// Where spells and abilities wait to resolve. Public, ordered (LIFO), shared.
    STACK(true, true, true),

    /// The exile zone ({@mtg.rule 406}).
    ///
    /// Where exiled cards go. Public, unordered, shared.
    EXILE(true, false, true),

    /// The command zone ({@mtg.rule 408}).
    ///
    /// Where commanders and emblems exist. Public, unordered, shared.
    COMMAND(true, false, true);

    private final boolean isPublic;
    private final boolean isOrdered;
    private final boolean isShared;

    ZoneType(boolean isPublic, boolean isOrdered, boolean isShared) {
        this.isPublic = isPublic;
        this.isOrdered = isOrdered;
        this.isShared = isShared;
    }

    /// Returns true if the zone's contents are visible to all players.
    ///
    /// @return true if public, false if hidden
    public boolean isPublic() {
        return isPublic;
    }

    /// Returns true if the zone's contents are hidden from some players.
    ///
    /// @return true if hidden, false if public
    public boolean isHidden() {
        return !isPublic;
    }

    /// Returns true if the order of objects in this zone matters.
    ///
    /// @return true if ordered, false if unordered
    public boolean isOrdered() {
        return isOrdered;
    }

    /// Returns true if all players share a single instance of this zone.
    ///
    /// @return true if shared, false if per-player
    public boolean isShared() {
        return isShared;
    }

    /// Returns true if each player has their own instance of this zone.
    ///
    /// @return true if per-player, false if shared
    public boolean isPerPlayer() {
        return !isShared;
    }
}
