package be.imgn.mtg.tournament.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import be.imgn.mtg.tournament.Pairing;
import be.imgn.mtg.tournament.PlayerId;

/// Generates Swiss pairings for a round.
///
/// Players are received sorted by standings (best first). Uses backtracking
/// to find a valid pairing where no two players are rematched.
public final class SwissPairer {

    /// Creates pairings for the next round.
    ///
    /// @param players       players sorted by current standings (best first)
    /// @param previousPairs set of player-pair sets from previous rounds
    /// @param byeReceivers  players who have already received a bye
    /// @param matchPoints   current match points per player (unused, reserved)
    /// @return list of pairings
    public List<Pairing> pair(
            List<PlayerId> players,
            Set<Set<PlayerId>> previousPairs,
            Set<PlayerId> byeReceivers,
            Map<PlayerId, Integer> matchPoints) {
        var pairings = new ArrayList<Pairing>();
        var pool = new ArrayList<>(players);

        // Handle bye for odd player count
        if (pool.size() % 2 != 0) {
            var byePlayer = selectByePlayer(pool, byeReceivers);
            pool.remove(byePlayer);
            pairings.add(new Pairing.Bye(byePlayer));
        }

        // Find valid pairing using backtracking
        var matchPairings = new ArrayList<Pairing.Match>();
        if (!findPairings(pool, previousPairs, matchPairings)) {
            throw new IllegalStateException("Cannot create valid pairings without rematches");
        }
        pairings.addAll(matchPairings);

        return List.copyOf(pairings);
    }

    private boolean findPairings(
            List<PlayerId> remaining, Set<Set<PlayerId>> previousPairs, List<Pairing.Match> result) {
        if (remaining.isEmpty()) return true;

        var p1 = remaining.getFirst();
        for (var i = 1; i < remaining.size(); i++) {
            var p2 = remaining.get(i);
            if (!previousPairs.contains(Set.of(p1, p2))) {
                // Try this pairing
                var next = new ArrayList<>(remaining);
                next.remove(p1);
                next.remove(p2);
                result.add(new Pairing.Match(p1, p2));
                if (findPairings(next, previousPairs, result)) {
                    return true;
                }
                result.removeLast();
            }
        }
        return false;
    }

    private PlayerId selectByePlayer(List<PlayerId> players, Set<PlayerId> byeReceivers) {
        for (var i = players.size() - 1; i >= 0; i--) {
            if (!byeReceivers.contains(players.get(i))) {
                return players.get(i);
            }
        }
        return players.getLast();
    }
}
