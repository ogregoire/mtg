package be.imgn.mtg.tournament;

/// What one player reports for a match.
///
/// @param wins  games this player won
/// @param draws games that ended in a draw
/// @param quit  whether this player is leaving the tournament
public record ResultSheet(int wins, int draws, boolean quit) {

    public ResultSheet {
        if (wins < 0) throw new IllegalArgumentException("wins must be non-negative: " + wins);
        if (draws < 0) throw new IllegalArgumentException("draws must be non-negative: " + draws);
    }

    /// Total games this player participated in (wins + draws + losses from opponent's wins).
    /// Must be combined with the opponent's sheet to get the full picture.
    public int gamesReported() {
        return wins + draws;
    }
}
