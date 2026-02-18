package be.imgn.mtg.tournament;

/// A pairing for a round: either a match between two players, or a bye.
public sealed interface Pairing {

    /// The first player (or the bye recipient).
    PlayerId player1();

    /// A match between two players.
    ///
    /// @param player1 the first player
    /// @param player2 the second player
    record Match(PlayerId player1, PlayerId player2) implements Pairing {}

    /// A bye — player1 receives an automatic 2-0 match win.
    ///
    /// @param player1 the bye recipient
    record Bye(PlayerId player1) implements Pairing {}
}
