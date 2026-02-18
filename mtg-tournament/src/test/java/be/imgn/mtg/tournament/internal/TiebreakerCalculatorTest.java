package be.imgn.mtg.tournament.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.tournament.MatchResultRecord;
import be.imgn.mtg.tournament.NamedPlayerId;
import be.imgn.mtg.tournament.Pairing;
import be.imgn.mtg.tournament.PlayerId;
import be.imgn.mtg.tournament.PlayerStanding;
import be.imgn.mtg.tournament.ResultSheet;
import be.imgn.mtg.tournament.Round;

class TiebreakerCalculatorTest {

    private final TiebreakerCalculator calculator = new TiebreakerCalculator();

    static PlayerId player(String name) {
        return new NamedPlayerId(name, UUID.nameUUIDFromBytes(name.getBytes()));
    }

    @Nested
    class MatchPoints {

        @Test
        void winGivesThreePoints() {
            var a = player("A");
            var b = player("B");
            var round = completed(1, a, b, new ResultSheet(2, 0, false), new ResultSheet(0, 0, false));

            var standings = calculator.calculate(List.of(a, b), List.of(round));

            assertThat(findStanding(standings, a).matchPoints()).isEqualTo(3);
            assertThat(findStanding(standings, b).matchPoints()).isZero();
        }

        @Test
        void drawGivesOnePoint() {
            var a = player("A");
            var b = player("B");
            var round = completed(1, a, b, new ResultSheet(1, 0, false), new ResultSheet(1, 0, false));

            var standings = calculator.calculate(List.of(a, b), List.of(round));

            assertThat(findStanding(standings, a).matchPoints()).isEqualTo(1);
            assertThat(findStanding(standings, b).matchPoints()).isEqualTo(1);
        }

        @Test
        void lossGivesZeroPoints() {
            var a = player("A");
            var b = player("B");
            var round = completed(1, a, b, new ResultSheet(0, 0, false), new ResultSheet(2, 0, false));

            var standings = calculator.calculate(List.of(a, b), List.of(round));

            assertThat(findStanding(standings, a).matchPoints()).isZero();
            assertThat(findStanding(standings, b).matchPoints()).isEqualTo(3);
        }
    }

    @Nested
    class MatchWinPercentage {

        @Test
        void oneWinOneRound() {
            var a = player("A");
            var b = player("B");
            var round = completed(1, a, b, new ResultSheet(2, 0, false), new ResultSheet(0, 0, false));

            var standings = calculator.calculate(List.of(a, b), List.of(round));

            assertThat(findStanding(standings, a).matchWinPct()).isCloseTo(1.0, within(0.001));
            assertThat(findStanding(standings, b).matchWinPct()).isCloseTo(0.0, within(0.001));
        }

        @Test
        void oneWinTwoRounds() {
            var a = player("A");
            var b = player("B");
            var c = player("C");
            var round1 = completed(1, a, b, new ResultSheet(2, 0, false), new ResultSheet(1, 0, false));
            var round2 = completed(2, a, c, new ResultSheet(0, 0, false), new ResultSheet(2, 0, false));

            var standings = calculator.calculate(List.of(a, b, c), List.of(round1, round2));

            // A: 1 win, 1 loss = 3 points / 6 max = 0.5
            assertThat(findStanding(standings, a).matchWinPct()).isCloseTo(0.5, within(0.001));
        }
    }

    @Nested
    class GameWinPercentage {

        @Test
        void calculatesFromGameWinsAndDraws() {
            var a = player("A");
            var b = player("B");
            // A wins 2 games, B wins 1. Total games = 3. A's GW% = (2*3)/(3*3) = 6/9 = 0.667
            var round = completed(1, a, b, new ResultSheet(2, 0, false), new ResultSheet(1, 0, false));

            var standings = calculator.calculate(List.of(a, b), List.of(round));

            assertThat(findStanding(standings, a).gwPct()).isCloseTo(2.0 / 3.0, within(0.001));
            // B: (1*3)/(3*3) = 3/9 = 0.333
            assertThat(findStanding(standings, b).gwPct()).isCloseTo(1.0 / 3.0, within(0.001));
        }

        @Test
        void includesDrawPoints() {
            var a = player("A");
            var b = player("B");
            // 1 win, 1 draw, 0 losses for A. Total games = 1+0+1 = 2. A's GP = 3+1 = 4. GW% = 4/6
            var round = completed(1, a, b, new ResultSheet(1, 1, false), new ResultSheet(0, 1, false));

            var standings = calculator.calculate(List.of(a, b), List.of(round));

            assertThat(findStanding(standings, a).gwPct()).isCloseTo(4.0 / 6.0, within(0.001));
        }
    }

    @Nested
    class OpponentPercentages {

        @Test
        void omwFlooredAtOneThird() {
            var a = player("A");
            var b = player("B");
            // A beats B. B has 0% MWP. OMW% for A should be floored at 33%
            var round = completed(1, a, b, new ResultSheet(2, 0, false), new ResultSheet(0, 0, false));

            var standings = calculator.calculate(List.of(a, b), List.of(round));

            assertThat(findStanding(standings, a).omwPct()).isCloseTo(1.0 / 3.0, within(0.001));
        }

        @Test
        void ogwFlooredAtOneThird() {
            var a = player("A");
            var b = player("B");
            // B has 0 game wins. OGW% for A should be floored at 33%
            var round = completed(1, a, b, new ResultSheet(2, 0, false), new ResultSheet(0, 0, false));

            var standings = calculator.calculate(List.of(a, b), List.of(round));

            assertThat(findStanding(standings, a).ogwPct()).isCloseTo(1.0 / 3.0, within(0.001));
        }

        @Test
        void omwAveragesAcrossOpponents() {
            var a = player("A");
            var b = player("B");
            var c = player("C");
            // Round 1: A beats B (A=3pts, B=0pts)
            // Round 2: A beats C, B beats C (A=6pts, B=3pts, C=0pts)
            var round1 = completed(1, a, b, new ResultSheet(2, 0, false), new ResultSheet(0, 0, false));
            var round2Pair1 = new Pairing.Match(a, c);
            var round2Pair2 = new Pairing.Match(b, c);
            var round2 = new Round.Completed(
                    2,
                    List.of(round2Pair1, round2Pair2),
                    Map.of(
                            round2Pair1,
                                    new MatchResultRecord(new ResultSheet(2, 0, false), new ResultSheet(0, 0, false)),
                            round2Pair2,
                                    new MatchResultRecord(new ResultSheet(2, 0, false), new ResultSheet(0, 0, false))));

            var standings = calculator.calculate(List.of(a, b, c), List.of(round1, round2));

            // A's opponents: B (MWP=3/6=0.5) and C (MWP=0/6=0.0, floored to 0.333)
            // A's OMW% = (0.5 + 0.333) / 2 = 0.4167
            assertThat(findStanding(standings, a).omwPct()).isCloseTo((0.5 + 1.0 / 3.0) / 2.0, within(0.001));
        }
    }

    @Nested
    class ByeHandling {

        @Test
        void byeGivesThreeMatchPoints() {
            var a = player("A");
            var b = player("B");
            var c = player("C");

            var byePairing = new Pairing.Bye(c);
            var matchPairing = new Pairing.Match(a, b);
            var round = new Round.Completed(
                    1,
                    List.of(matchPairing, byePairing),
                    Map.of(
                            matchPairing,
                                    new MatchResultRecord(new ResultSheet(2, 0, false), new ResultSheet(1, 0, false)),
                            byePairing,
                                    new MatchResultRecord(new ResultSheet(2, 0, false), new ResultSheet(0, 0, false))));

            var standings = calculator.calculate(List.of(a, b, c), List.of(round));

            assertThat(findStanding(standings, c).matchPoints()).isEqualTo(3);
        }

        @Test
        void byeCountsAsTwoZeroForGameWinPct() {
            var a = player("A");
            var b = player("B");
            var c = player("C");

            var byePairing = new Pairing.Bye(c);
            var matchPairing = new Pairing.Match(a, b);
            var round = new Round.Completed(
                    1,
                    List.of(matchPairing, byePairing),
                    Map.of(
                            matchPairing,
                                    new MatchResultRecord(new ResultSheet(2, 0, false), new ResultSheet(1, 0, false)),
                            byePairing,
                                    new MatchResultRecord(new ResultSheet(2, 0, false), new ResultSheet(0, 0, false))));

            var standings = calculator.calculate(List.of(a, b, c), List.of(round));

            // C got bye: 2 game wins, 2 games played. GWP = 6/6 = 1.0
            assertThat(findStanding(standings, c).gwPct()).isCloseTo(1.0, within(0.001));
        }
    }

    @Nested
    class Ranking {

        @Test
        void ranksAreOneBased() {
            var a = player("A");
            var b = player("B");
            var standings = calculator.calculate(List.of(a, b), List.of());

            assertThat(standings.getFirst().rank()).isEqualTo(1);
            assertThat(standings.getLast().rank()).isEqualTo(2);
        }

        @Test
        void higherPointsRankFirst() {
            var a = player("A");
            var b = player("B");
            var round = completed(1, a, b, new ResultSheet(2, 0, false), new ResultSheet(0, 0, false));

            var standings = calculator.calculate(List.of(a, b), List.of(round));

            assertThat(standings.getFirst().player()).isEqualTo(a);
            assertThat(standings.getFirst().rank()).isEqualTo(1);
        }
    }

    private static Round.Completed completed(
            int number, PlayerId p1, PlayerId p2, ResultSheet sheet1, ResultSheet sheet2) {
        var pairing = new Pairing.Match(p1, p2);
        return new Round.Completed(number, List.of(pairing), Map.of(pairing, new MatchResultRecord(sheet1, sheet2)));
    }

    private static PlayerStanding findStanding(List<PlayerStanding> standings, PlayerId player) {
        return standings.stream()
                .filter(s -> s.player().equals(player))
                .findFirst()
                .orElseThrow();
    }
}
