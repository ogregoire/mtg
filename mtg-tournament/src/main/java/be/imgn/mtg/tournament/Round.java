package be.imgn.mtg.tournament;

import java.util.List;
import java.util.Map;

/// A round in a Swiss tournament.
///
/// A round starts as [Pending] with pairings, and becomes [Completed]
/// when all match results have been submitted.
public sealed interface Round {

    /// The 1-based round number.
    int number();

    /// The pairings for this round.
    List<Pairing> pairings();

    /// A round awaiting results.
    record Pending(int number, List<Pairing> pairings) implements Round {}

    /// A completed round with all results submitted.
    ///
    /// @param number   the round number
    /// @param pairings the pairings for this round
    /// @param results  match results keyed by pairing
    record Completed(int number, List<Pairing> pairings, Map<Pairing, MatchResultRecord> results) implements Round {}
}
