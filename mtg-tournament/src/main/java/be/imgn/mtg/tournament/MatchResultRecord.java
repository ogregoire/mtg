package be.imgn.mtg.tournament;

/// The stored result of a completed match, capturing both players' result sheets.
///
/// @param player1Sheet player 1's reported results
/// @param player2Sheet player 2's reported results
public record MatchResultRecord(ResultSheet player1Sheet, ResultSheet player2Sheet) {

    /// The outcome of the match from player 1's perspective.
    public MatchOutcome outcome() {
        if (player1Sheet.quit() && player2Sheet.quit()) {
            return MatchOutcome.DRAW;
        }
        if (player1Sheet.quit()) {
            return MatchOutcome.PLAYER2_WIN;
        }
        if (player2Sheet.quit()) {
            return MatchOutcome.PLAYER1_WIN;
        }
        var cmp = Integer.compare(player1Sheet.wins(), player2Sheet.wins());
        if (cmp > 0) return MatchOutcome.PLAYER1_WIN;
        if (cmp < 0) return MatchOutcome.PLAYER2_WIN;
        return MatchOutcome.DRAW;
    }

    /// Total games played in this match.
    public int totalGames() {
        return player1Sheet.wins() + player2Sheet.wins() + player1Sheet.draws();
    }

    /// The possible outcomes of a match.
    public enum MatchOutcome {
        PLAYER1_WIN,
        PLAYER2_WIN,
        DRAW
    }
}
