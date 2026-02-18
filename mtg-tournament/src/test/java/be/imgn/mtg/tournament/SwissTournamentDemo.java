package be.imgn.mtg.tournament;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.UUID;

/// Runnable demo of a 133-player Swiss tournament with bottom 5% drops each round.
///
/// Run with: `mvn -pl mtg-tournament exec:java -Dexec.mainClass=be.imgn.mtg.tournament.SwissTournamentDemo`
public final class SwissTournamentDemo {

    public static void main(String[] args) {
        var rng = new Random(42);
        var players = new ArrayList<PlayerId>();
        for (var i = 1; i <= 133; i++) {
            players.add(new NamedPlayerId("P" + i, UUID.nameUUIDFromBytes(("P" + i).getBytes())));
        }

        var tournament = SwissTournament.of(players);
        var totalRounds = tournament.recommendedRounds();
        System.out.printf("Tournament: %d players, %d rounds%n%n", players.size(), totalRounds);

        var dropped = new HashSet<PlayerId>();

        for (var roundNum = 1; roundNum <= totalRounds; roundNum++) {
            // Bottom 5% (rounded down) of active players will quit this round
            var standings = tournament.standings();
            var activeStandings = standings.stream()
                    .filter(s -> !dropped.contains(s.player()))
                    .toList();
            var quitterCount = (int) Math.floor(activeStandings.size() * 0.05);
            var quitters = new HashSet<PlayerId>();
            for (var i = activeStandings.size() - quitterCount; i < activeStandings.size(); i++) {
                quitters.add(activeStandings.get(i).player());
            }

            var round = tournament.nextRound();

            for (var pairing : round.pairings()) {
                if (pairing instanceof Pairing.Match m) {
                    var p1Wins = rng.nextInt(3);
                    var p2Wins = rng.nextInt(3);
                    var draws = rng.nextInt(20) == 0 ? 1 : 0;
                    tournament.submitResult(
                            m,
                            new ResultSheet(p1Wins, draws, quitters.contains(m.player1())),
                            new ResultSheet(p2Wins, draws, quitters.contains(m.player2())));
                }
            }

            dropped.addAll(quitters);

            // Print top 20 standings after each round
            var updated = tournament.standings();
            System.out.printf(
                    "=== After Round %d (%d active, %d dropped this round) ===%n",
                    roundNum, activeStandings.size() - quitterCount, quitterCount);
            System.out.printf(
                    "%-4s  %-6s  %4s  %6s  %6s  %6s  %6s%n", "Rank", "Player", "MP", "MWP%", "OMW%", "GWP%", "OGW%");
            System.out.println("-".repeat(50));
            for (var s : updated.stream().limit(20).toList()) {
                System.out.printf(
                        "%-4d  %-6s  %4d  %5.1f%%  %5.1f%%  %5.1f%%  %5.1f%%",
                        s.rank(),
                        s.player(),
                        s.matchPoints(),
                        s.matchWinPct() * 100,
                        s.omwPct() * 100,
                        s.gwPct() * 100,
                        s.ogwPct() * 100);
                if (dropped.contains(s.player())) {
                    System.out.print("  [dropped]");
                }
                System.out.println();
            }
            System.out.println();
        }

        System.out.printf("=== Final Standings (all %d players) ===%n", players.size());
        System.out.printf(
                "%-4s  %-6s  %4s  %6s  %6s  %6s  %6s%n", "Rank", "Player", "MP", "MWP%", "OMW%", "GWP%", "OGW%");
        System.out.println("-".repeat(50));
        for (var s : tournament.standings().stream().limit(20).toList()) {
            System.out.printf(
                    "%-4d  %-6s  %4d  %5.1f%%  %5.1f%%  %5.1f%%  %5.1f%%",
                    s.rank(),
                    s.player(),
                    s.matchPoints(),
                    s.matchWinPct() * 100,
                    s.omwPct() * 100,
                    s.gwPct() * 100,
                    s.ogwPct() * 100);
            if (dropped.contains(s.player())) {
                System.out.print("  [dropped]");
            }
            System.out.println();
        }
    }
}
