package be.imgn.mtg.tournament;

/// A player's standing in the tournament.
///
/// Ordered by match points, then OMW%, then GW%, then OGW% (all descending).
///
/// @param rank        1-based rank
/// @param player      the player
/// @param matchPoints total match points (3 per win, 1 per draw, 0 per loss)
/// @param matchWinPct match win percentage
/// @param omwPct      opponent match win percentage (floored at 0.33)
/// @param gwPct       game win percentage
/// @param ogwPct      opponent game win percentage (floored at 0.33)
public record PlayerStanding(
        int rank, PlayerId player, int matchPoints, double matchWinPct, double omwPct, double gwPct, double ogwPct)
        implements Comparable<PlayerStanding> {

    @Override
    public int compareTo(PlayerStanding other) {
        var cmp = Integer.compare(other.matchPoints, matchPoints);
        if (cmp != 0) return cmp;
        cmp = Double.compare(other.omwPct, omwPct);
        if (cmp != 0) return cmp;
        cmp = Double.compare(other.gwPct, gwPct);
        if (cmp != 0) return cmp;
        return Double.compare(other.ogwPct, ogwPct);
    }
}
