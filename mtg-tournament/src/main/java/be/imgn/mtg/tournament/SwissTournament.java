package be.imgn.mtg.tournament;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.tournament.internal.SwissPairer;
import be.imgn.mtg.tournament.internal.TiebreakerCalculator;

/// A Swiss rounds tournament simulator.
///
/// Usage:
/// ```java
/// var tournament = SwissTournament.of(List.of(player1, player2, player3, player4));
/// var round = tournament.nextRound();
/// for (var pairing : round.pairings()) {
///     switch (pairing) {
///         case Pairing.Match m -> tournament.submitResult(m,
///             new ResultSheet(2, 0, false),
///             new ResultSheet(1, 0, false));
///         case Pairing.Bye _ -> {} // auto-resolved
///     }
/// }
/// var standings = tournament.standings();
/// ```
public final class SwissTournament {

    private final List<PlayerId> players;
    private final List<Round> rounds = new ArrayList<>();
    private final Set<Set<PlayerId>> previousPairs = new HashSet<>();
    private final Set<PlayerId> byeReceivers = new HashSet<>();
    private final Set<PlayerId> droppedPlayers = new HashSet<>();
    private final Map<Pairing, MatchResultRecord> pendingResults = new LinkedHashMap<>();
    private final SwissPairer pairer = new SwissPairer();
    private final TiebreakerCalculator calculator = new TiebreakerCalculator();

    private Round.@Nullable Pending currentRound;

    private SwissTournament(List<PlayerId> players) {
        this.players = List.copyOf(players);
    }

    /// Creates a new Swiss tournament with the given players.
    ///
    /// @param players the list of players (at least 2, no duplicates)
    /// @return a new tournament
    public static SwissTournament of(List<PlayerId> players) {
        if (players.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 players, got " + players.size());
        }
        if (new HashSet<>(players).size() != players.size()) {
            throw new IllegalArgumentException("Duplicate players");
        }
        return new SwissTournament(players);
    }

    /// Generates pairings for the next round.
    ///
    /// @return the pending round with pairings
    /// @throws IllegalStateException if the current round is not yet complete
    public Round.Pending nextRound() {
        if (currentRound != null) {
            throw new IllegalStateException("Current round " + currentRound.number() + " is not yet complete");
        }

        var activePlayers =
                players.stream().filter(p -> !droppedPlayers.contains(p)).toList();

        if (activePlayers.size() < 2) {
            throw new IllegalStateException("Not enough active players for a round: " + activePlayers.size());
        }

        // Sort active players by current standings
        var standingsList = standings();
        var standingsOrder = new HashMap<PlayerId, Integer>();
        for (var s : standingsList) {
            standingsOrder.put(s.player(), s.rank());
        }
        var sorted = new ArrayList<>(activePlayers);
        sorted.sort((a, b) -> Integer.compare(
                standingsOrder.getOrDefault(a, Integer.MAX_VALUE), standingsOrder.getOrDefault(b, Integer.MAX_VALUE)));

        var matchPoints = new HashMap<PlayerId, Integer>();
        for (var s : standingsList) {
            matchPoints.put(s.player(), s.matchPoints());
        }

        var pairings = pairer.pair(sorted, previousPairs, byeReceivers, matchPoints);

        var roundNumber = rounds.size() + 1;
        currentRound = new Round.Pending(roundNumber, pairings);

        // Auto-resolve byes
        for (var pairing : pairings) {
            if (pairing instanceof Pairing.Bye(var player)) {
                byeReceivers.add(player);
                pendingResults.put(
                        pairing, new MatchResultRecord(new ResultSheet(2, 0, false), new ResultSheet(0, 0, false)));
            }
        }

        return currentRound;
    }

    /// Submits the result of a match.
    ///
    /// @param match        the match pairing to submit results for
    /// @param player1Sheet player 1's result sheet
    /// @param player2Sheet player 2's result sheet
    public void submitResult(Pairing.Match match, ResultSheet player1Sheet, ResultSheet player2Sheet) {
        if (currentRound == null) {
            throw new IllegalStateException("No round in progress");
        }
        if (!currentRound.pairings().contains(match)) {
            throw new IllegalArgumentException("Pairing not in current round: " + match);
        }
        if (pendingResults.containsKey(match)) {
            throw new IllegalArgumentException("Result already submitted for pairing: " + match);
        }

        var result = new MatchResultRecord(player1Sheet, player2Sheet);
        pendingResults.put(match, result);

        // Track drops
        if (player1Sheet.quit()) droppedPlayers.add(match.player1());
        if (player2Sheet.quit()) droppedPlayers.add(match.player2());

        // Check if round is complete
        if (pendingResults.size() == currentRound.pairings().size()) {
            completeRound();
        }
    }

    /// Returns the current standings.
    public List<PlayerStanding> standings() {
        var completedRounds = rounds.stream()
                .filter(r -> r instanceof Round.Completed)
                .map(r -> (Round.Completed) r)
                .toList();
        return calculator.calculate(players, completedRounds);
    }

    /// Returns all completed and pending rounds.
    public List<Round> rounds() {
        var result = new ArrayList<>(rounds);
        if (currentRound != null) {
            result.add(currentRound);
        }
        return List.copyOf(result);
    }

    /// Returns the current round number (0 if no round has started).
    public int currentRoundNumber() {
        if (currentRound != null) return currentRound.number();
        return rounds.size();
    }

    /// Returns the recommended number of Swiss rounds based on player count.
    ///
    /// Computed as ceil(log2(playerCount)).
    public int recommendedRounds() {
        return Integer.SIZE - Integer.numberOfLeadingZeros(players.size() - 1);
    }

    /// Returns the number of players in the tournament.
    public int playerCount() {
        return players.size();
    }

    private void completeRound() {
        var round = requireNonNull(currentRound);

        // Record all pairs for rematch prevention
        for (var pairing : round.pairings()) {
            if (pairing instanceof Pairing.Match(var p1, var p2)) {
                previousPairs.add(Set.of(p1, p2));
            }
        }

        rounds.add(new Round.Completed(round.number(), round.pairings(), Map.copyOf(pendingResults)));
        pendingResults.clear();
        currentRound = null;
    }
}
