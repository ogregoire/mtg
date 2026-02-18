package be.imgn.mtg.tournament;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SwissTournamentTest {

    static PlayerId player(String name) {
        return new NamedPlayerId(name, UUID.nameUUIDFromBytes(name.getBytes()));
    }

    static void submitAllResults(SwissTournament tournament, Round.Pending round) {
        for (var pairing : round.pairings()) {
            if (pairing instanceof Pairing.Match m) {
                tournament.submitResult(m, new ResultSheet(2, 0, false), new ResultSheet(1, 0, false));
            }
        }
    }

    @Nested
    class Creation {

        @Test
        void rejectsFewerThanTwoPlayers() {
            assertThatThrownBy(() -> SwissTournament.of(List.of(player("Alice"))))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejectsEmptyList() {
            assertThatThrownBy(() -> SwissTournament.of(List.of())).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejectsDuplicatePlayers() {
            var p = player("Alice");
            assertThatThrownBy(() -> SwissTournament.of(List.of(p, p))).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void createsWithTwoPlayers() {
            var tournament = SwissTournament.of(List.of(player("Alice"), player("Bob")));
            assertThat(tournament.playerCount()).isEqualTo(2);
            assertThat(tournament.currentRoundNumber()).isZero();
        }
    }

    @Nested
    class Rounds {

        @Test
        void generatesFirstRound() {
            var tournament = SwissTournament.of(List.of(player("A"), player("B"), player("C"), player("D")));
            var round = tournament.nextRound();

            assertThat(round.number()).isEqualTo(1);
            assertThat(round.pairings()).hasSize(2);
            assertThat(round.pairings()).allSatisfy(p -> assertThat(p).isInstanceOf(Pairing.Match.class));
        }

        @Test
        void rejectsNextRoundWhenCurrentIncomplete() {
            var tournament = SwissTournament.of(List.of(player("A"), player("B")));
            tournament.nextRound();

            assertThatThrownBy(tournament::nextRound).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void completesRoundWhenAllResultsSubmitted() {
            var tournament = SwissTournament.of(List.of(player("A"), player("B")));
            var round = tournament.nextRound();
            submitAllResults(tournament, round);

            assertThat(tournament.currentRoundNumber()).isEqualTo(1);
        }

        @Test
        void rejectsResultForUnknownPairing() {
            var tournament = SwissTournament.of(List.of(player("A"), player("B")));
            tournament.nextRound();
            var fakeMatch = new Pairing.Match(player("X"), player("Y"));

            assertThatThrownBy(() -> tournament.submitResult(
                            fakeMatch, new ResultSheet(2, 0, false), new ResultSheet(0, 0, false)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejectsDuplicateResult() {
            var tournament = SwissTournament.of(List.of(player("A"), player("B"), player("C"), player("D")));
            var round = tournament.nextRound();
            var match = (Pairing.Match) round.pairings().getFirst();

            tournament.submitResult(match, new ResultSheet(2, 0, false), new ResultSheet(1, 0, false));

            assertThatThrownBy(() ->
                            tournament.submitResult(match, new ResultSheet(2, 0, false), new ResultSheet(1, 0, false)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejectsResultWhenNoRoundInProgress() {
            var tournament = SwissTournament.of(List.of(player("A"), player("B")));
            var fakeMatch = new Pairing.Match(player("A"), player("B"));

            assertThatThrownBy(() -> tournament.submitResult(
                            fakeMatch, new ResultSheet(2, 0, false), new ResultSheet(0, 0, false)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void tracksRoundsHistory() {
            var tournament = SwissTournament.of(List.of(player("A"), player("B")));
            var round = tournament.nextRound();
            submitAllResults(tournament, round);

            assertThat(tournament.rounds()).hasSize(1);
            assertThat(tournament.rounds().getFirst()).isInstanceOf(Round.Completed.class);
        }
    }

    @Nested
    class ByeHandling {

        @Test
        void assignsByeForOddPlayerCount() {
            var tournament = SwissTournament.of(List.of(player("A"), player("B"), player("C")));
            var round = tournament.nextRound();

            var byes = round.pairings().stream()
                    .filter(p -> p instanceof Pairing.Bye)
                    .toList();
            assertThat(byes).hasSize(1);

            var matches = round.pairings().stream()
                    .filter(p -> p instanceof Pairing.Match)
                    .toList();
            assertThat(matches).hasSize(1);
        }

        @Test
        void byeAutoResolvesAsMatchWin() {
            var tournament = SwissTournament.of(List.of(player("A"), player("B"), player("C")));
            var round = tournament.nextRound();
            submitAllResults(tournament, round);

            var byePlayer = round.pairings().stream()
                    .filter(p -> p instanceof Pairing.Bye)
                    .map(Pairing::player1)
                    .findFirst()
                    .orElseThrow();

            var standings = tournament.standings();
            var byeStanding = standings.stream()
                    .filter(s -> s.player().equals(byePlayer))
                    .findFirst()
                    .orElseThrow();
            assertThat(byeStanding.matchPoints()).isEqualTo(3);
        }

        @Test
        void noPlayerGetsByeTwiceWhileOthersHaveNot() {
            var players = List.of(player("A"), player("B"), player("C"), player("D"), player("E"));
            var tournament = SwissTournament.of(players);

            var byeReceivers = new HashSet<PlayerId>();
            for (var i = 0; i < 3; i++) {
                var round = tournament.nextRound();
                for (var pairing : round.pairings()) {
                    switch (pairing) {
                        case Pairing.Bye(var player) -> byeReceivers.add(player);
                        case Pairing.Match m ->
                            tournament.submitResult(m, new ResultSheet(2, 0, false), new ResultSheet(1, 0, false));
                    }
                }
            }

            assertThat(byeReceivers).hasSize(3);
        }
    }

    @Nested
    class NoRematches {

        @Test
        void noTwoPlayersPlayTwice() {
            var players = List.of(player("A"), player("B"), player("C"), player("D"));
            var tournament = SwissTournament.of(players);

            var allPairs = new HashSet<Set<PlayerId>>();
            for (var i = 0; i < 3; i++) {
                var round = tournament.nextRound();
                for (var pairing : round.pairings()) {
                    if (pairing instanceof Pairing.Match m) {
                        var pair = Set.of(m.player1(), m.player2());
                        assertThat(allPairs.add(pair))
                                .as("Rematch detected: %s in round %d", pair, i + 1)
                                .isTrue();
                        tournament.submitResult(m, new ResultSheet(2, 0, false), new ResultSheet(1, 0, false));
                    }
                }
            }
        }
    }

    @Nested
    class Standings {

        @Test
        void standingsReflectResults() {
            var a = player("A");
            var b = player("B");
            var tournament = SwissTournament.of(List.of(a, b));

            var round = tournament.nextRound();
            var match = (Pairing.Match) round.pairings().getFirst();
            tournament.submitResult(match, new ResultSheet(2, 0, false), new ResultSheet(0, 0, false));

            var standings = tournament.standings();
            assertThat(standings).hasSize(2);

            var winner = standings.stream()
                    .filter(s -> s.player().equals(match.player1()))
                    .findFirst()
                    .orElseThrow();
            var loser = standings.stream()
                    .filter(s -> s.player().equals(match.player2()))
                    .findFirst()
                    .orElseThrow();

            assertThat(winner.matchPoints()).isEqualTo(3);
            assertThat(loser.matchPoints()).isZero();
            assertThat(winner.rank()).isLessThan(loser.rank());
        }

        @Test
        void standingsCallableBeforeAnyRound() {
            var tournament = SwissTournament.of(List.of(player("A"), player("B")));
            var standings = tournament.standings();
            assertThat(standings).hasSize(2);
            assertThat(standings).allSatisfy(s -> assertThat(s.matchPoints()).isZero());
        }

        @Test
        void matchDrawGivesOnePointEach() {
            var tournament = SwissTournament.of(List.of(player("A"), player("B")));
            var round = tournament.nextRound();
            var match = (Pairing.Match) round.pairings().getFirst();

            tournament.submitResult(match, new ResultSheet(1, 0, false), new ResultSheet(1, 0, false));

            var standings = tournament.standings();
            assertThat(standings).allSatisfy(s -> assertThat(s.matchPoints()).isEqualTo(1));
        }
    }

    @Nested
    class QuitHandling {

        @Test
        void quittingPlayerIsExcludedFromFutureRounds() {
            var a = player("A");
            var b = player("B");
            var c = player("C");
            var d = player("D");
            var tournament = SwissTournament.of(List.of(a, b, c, d));

            var round1 = tournament.nextRound();
            for (var pairing : round1.pairings()) {
                if (pairing instanceof Pairing.Match m) {
                    if (m.player1().equals(a)) {
                        tournament.submitResult(m, new ResultSheet(0, 0, true), new ResultSheet(2, 0, false));
                    } else if (m.player2().equals(a)) {
                        tournament.submitResult(m, new ResultSheet(2, 0, false), new ResultSheet(0, 0, true));
                    } else {
                        tournament.submitResult(m, new ResultSheet(2, 0, false), new ResultSheet(1, 0, false));
                    }
                }
            }

            // Round 2: A should not appear in any pairing
            var round2 = tournament.nextRound();
            for (var pairing : round2.pairings()) {
                assertThat(pairing.player1()).isNotEqualTo(a);
                if (pairing instanceof Pairing.Match m) {
                    assertThat(m.player2()).isNotEqualTo(a);
                }
            }
        }

        @Test
        void quittingPlayerOpponentWinsMatch() {
            var a = player("A");
            var b = player("B");
            var tournament = SwissTournament.of(List.of(a, b));

            var round = tournament.nextRound();
            var match = (Pairing.Match) round.pairings().getFirst();

            // player1 quits after winning 1 game
            tournament.submitResult(match, new ResultSheet(1, 0, true), new ResultSheet(0, 0, false));

            var standings = tournament.standings();
            var p2Standing = standings.stream()
                    .filter(s -> s.player().equals(match.player2()))
                    .findFirst()
                    .orElseThrow();
            assertThat(p2Standing.matchPoints()).isEqualTo(3);
        }

        @Test
        void bothQuitIsMatchDraw() {
            var a = player("A");
            var b = player("B");
            var c = player("C");
            var d = player("D");
            var tournament = SwissTournament.of(List.of(a, b, c, d));

            var round = tournament.nextRound();
            for (var pairing : round.pairings()) {
                if (pairing instanceof Pairing.Match m) {
                    tournament.submitResult(m, new ResultSheet(0, 0, true), new ResultSheet(0, 0, true));
                }
            }

            var standings = tournament.standings();
            assertThat(standings).allSatisfy(s -> assertThat(s.matchPoints()).isEqualTo(1));
        }
    }

    @Nested
    class FullTournament {

        @Test
        void fourPlayerThreeRounds() {
            var a = player("A");
            var b = player("B");
            var c = player("C");
            var d = player("D");
            var tournament = SwissTournament.of(List.of(a, b, c, d));

            for (var i = 0; i < 3; i++) {
                var round = tournament.nextRound();
                submitAllResults(tournament, round);
            }

            var standings = tournament.standings();
            assertThat(standings).hasSize(4);
            assertThat(standings.getFirst().rank()).isEqualTo(1);
            assertThat(standings.stream().map(PlayerStanding::rank).distinct()).hasSize(4);
        }

        @Test
        void twoPlayerMinimalTournament() {
            var tournament = SwissTournament.of(List.of(player("A"), player("B")));

            var round = tournament.nextRound();
            var match = (Pairing.Match) round.pairings().getFirst();
            tournament.submitResult(match, new ResultSheet(2, 1, false), new ResultSheet(1, 1, false));

            var standings = tournament.standings();
            assertThat(standings).hasSize(2);
            assertThat(standings.getFirst().matchPoints()).isEqualTo(3);
            assertThat(standings.getLast().matchPoints()).isZero();
        }
    }

    @Nested
    class RecommendedRounds {

        @Test
        void twoPlayers() {
            assertThat(SwissTournament.of(List.of(player("A"), player("B"))).recommendedRounds())
                    .isEqualTo(1);
        }

        @Test
        void fourPlayers() {
            var players = List.of(player("A"), player("B"), player("C"), player("D"));
            assertThat(SwissTournament.of(players).recommendedRounds()).isEqualTo(2);
        }

        @Test
        void eightPlayers() {
            var players = new ArrayList<PlayerId>();
            for (var i = 1; i <= 8; i++) players.add(player("P" + i));
            assertThat(SwissTournament.of(players).recommendedRounds()).isEqualTo(3);
        }

        @Test
        void ninePlayersNeedsFourRounds() {
            var players = new ArrayList<PlayerId>();
            for (var i = 1; i <= 9; i++) players.add(player("P" + i));
            assertThat(SwissTournament.of(players).recommendedRounds()).isEqualTo(4);
        }
    }
}
