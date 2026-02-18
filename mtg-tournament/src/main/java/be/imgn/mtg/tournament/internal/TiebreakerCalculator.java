package be.imgn.mtg.tournament.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import be.imgn.mtg.tournament.MatchResultRecord;
import be.imgn.mtg.tournament.Pairing;
import be.imgn.mtg.tournament.PlayerId;
import be.imgn.mtg.tournament.PlayerStanding;
import be.imgn.mtg.tournament.ResultSheet;
import be.imgn.mtg.tournament.Round;

/// Calculates tiebreaker statistics for Swiss tournament standings.
public final class TiebreakerCalculator {

    private static final double FLOOR = 1.0 / 3.0;

    /// Calculates standings for all players based on completed rounds.
    public List<PlayerStanding> calculate(List<PlayerId> players, List<Round.Completed> completedRounds) {
        var stats = new HashMap<PlayerId, PlayerStats>();
        var opponents = new HashMap<PlayerId, List<PlayerId>>();

        for (var player : players) {
            stats.put(player, new PlayerStats());
            opponents.put(player, new ArrayList<>());
        }

        for (var round : completedRounds) {
            for (var pairing : round.pairings()) {
                var result = round.results().get(pairing);
                if (result == null) continue;

                switch (pairing) {
                    case Pairing.Bye(var player) -> recordBye(get(stats, player));
                    case Pairing.Match m -> recordMatch(stats, opponents, m, result);
                }
            }
        }

        // Calculate percentages
        var standings = new ArrayList<PlayerStanding>();
        for (var player : players) {
            var s = get(stats, player);
            var matchWinPct = s.roundsPlayed > 0 ? (double) s.matchPoints / (3.0 * s.roundsPlayed) : 0.0;
            var gwPct = s.gamesPlayed > 0 ? (double) s.gamePoints / (3.0 * s.gamesPlayed) : 0.0;

            var omwPct = averageOpponentPct(get(opponents, player), stats, true);
            var ogwPct = averageOpponentPct(get(opponents, player), stats, false);

            standings.add(new PlayerStanding(0, player, s.matchPoints, matchWinPct, omwPct, gwPct, ogwPct));
        }

        standings.sort(PlayerStanding::compareTo);

        // Assign ranks
        var ranked = new ArrayList<PlayerStanding>();
        for (var i = 0; i < standings.size(); i++) {
            var s = standings.get(i);
            ranked.add(new PlayerStanding(
                    i + 1, s.player(), s.matchPoints(), s.matchWinPct(), s.omwPct(), s.gwPct(), s.ogwPct()));
        }
        return List.copyOf(ranked);
    }

    private void recordBye(PlayerStats stats) {
        stats.matchPoints += 3;
        stats.roundsPlayed++;
        // Bye counts as 2-0 for game win percentage
        stats.gamePoints += 6; // 2 wins * 3 points each
        stats.gamesPlayed += 2;
    }

    private void recordMatch(
            Map<PlayerId, PlayerStats> stats,
            Map<PlayerId, List<PlayerId>> opponents,
            Pairing.Match pairing,
            MatchResultRecord result) {
        var p1 = pairing.player1();
        var p2 = pairing.player2();

        get(opponents, p1).add(p2);
        get(opponents, p2).add(p1);

        var s1 = get(stats, p1);
        var s2 = get(stats, p2);
        s1.roundsPlayed++;
        s2.roundsPlayed++;

        switch (result.outcome()) {
            case PLAYER1_WIN -> s1.matchPoints += 3;
            case PLAYER2_WIN -> s2.matchPoints += 3;
            case DRAW -> {
                s1.matchPoints += 1;
                s2.matchPoints += 1;
            }
        }

        recordGamePoints(s1, result.player1Sheet(), result.player2Sheet());
        recordGamePoints(s2, result.player2Sheet(), result.player1Sheet());
    }

    private void recordGamePoints(PlayerStats stats, ResultSheet ownSheet, ResultSheet opponentSheet) {
        var totalGames = ownSheet.wins() + opponentSheet.wins() + ownSheet.draws();
        stats.gamesPlayed += totalGames;
        stats.gamePoints += ownSheet.wins() * 3 + ownSheet.draws();
    }

    private double averageOpponentPct(List<PlayerId> opponentList, Map<PlayerId, PlayerStats> stats, boolean match) {
        if (opponentList.isEmpty()) return 0.0;
        var sum = 0.0;
        for (var opp : opponentList) {
            var s = get(stats, opp);
            double pct;
            if (match) {
                pct = s.roundsPlayed > 0 ? (double) s.matchPoints / (3.0 * s.roundsPlayed) : 0.0;
            } else {
                pct = s.gamesPlayed > 0 ? (double) s.gamePoints / (3.0 * s.gamesPlayed) : 0.0;
            }
            sum += Math.max(pct, FLOOR);
        }
        return sum / opponentList.size();
    }

    private static <V> V get(Map<PlayerId, V> map, PlayerId key) {
        return Objects.requireNonNull(map.get(key));
    }

    static final class PlayerStats {
        int matchPoints;
        int roundsPlayed;
        int gamePoints;
        int gamesPlayed;
    }
}
