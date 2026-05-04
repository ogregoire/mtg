package be.imgn.mtg.tournament.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.tournament.NamedPlayerId;
import be.imgn.mtg.tournament.Pairing;
import be.imgn.mtg.tournament.PlayerId;

class SwissPairerTest {

    private final SwissPairer pairer = new SwissPairer();

    static PlayerId player(String name) {
        return new NamedPlayerId(name, UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)));
    }

    @Nested
    class FirstRound {

        @Test
        void pairsEvenNumberOfPlayers() {
            var players = List.of(player("A"), player("B"), player("C"), player("D"));
            var pairings = pairer.pair(players, Set.of(), Set.of(), Map.of());

            assertThat(pairings).hasSize(2);
            assertThat(pairings).allSatisfy(p -> assertThat(p).isInstanceOf(Pairing.Match.class));
        }

        @Test
        void allPlayersArePaired() {
            var players = List.of(player("A"), player("B"), player("C"), player("D"));
            var pairings = pairer.pair(players, Set.of(), Set.of(), Map.of());

            var paired = new HashSet<PlayerId>();
            for (var p : pairings) {
                paired.add(p.player1());
                if (p instanceof Pairing.Match m) paired.add(m.player2());
            }
            assertThat(paired).containsExactlyInAnyOrderElementsOf(players);
        }
    }

    @Nested
    class ByeAssignment {

        @Test
        void assignsByeForOddCount() {
            var players = List.of(player("A"), player("B"), player("C"));
            var pairings = pairer.pair(players, Set.of(), Set.of(), Map.of());

            var byes = pairings.stream().filter(p -> p instanceof Pairing.Bye).toList();
            assertThat(byes).hasSize(1);
        }

        @Test
        void byeGoesToLowestRanked() {
            var players = List.of(player("A"), player("B"), player("C"));
            var pairings = pairer.pair(players, Set.of(), Set.of(), Map.of());

            var bye = pairings.stream()
                    .filter(p -> p instanceof Pairing.Bye)
                    .findFirst()
                    .orElseThrow();
            assertThat(bye.player1()).isEqualTo(player("C"));
        }

        @Test
        void byeSkipsPlayerWhoAlreadyHadOne() {
            var a = player("A");
            var b = player("B");
            var c = player("C");
            var pairings = pairer.pair(List.of(a, b, c), Set.of(), Set.of(c), Map.of());

            var bye = pairings.stream()
                    .filter(p -> p instanceof Pairing.Bye)
                    .findFirst()
                    .orElseThrow();
            assertThat(bye.player1()).isEqualTo(b);
        }

        @Test
        void byeFallsBackToLowestWhenAllHadByes() {
            var a = player("A");
            var b = player("B");
            var c = player("C");
            var pairings = pairer.pair(List.of(a, b, c), Set.of(), Set.of(a, b, c), Map.of());

            var bye = pairings.stream()
                    .filter(p -> p instanceof Pairing.Bye)
                    .findFirst()
                    .orElseThrow();
            assertThat(bye.player1()).isEqualTo(c);
        }
    }

    @Nested
    class RematchAvoidance {

        @Test
        void avoidsRematches() {
            var a = player("A");
            var b = player("B");
            var c = player("C");
            var d = player("D");
            var previousPairs = Set.of(Set.of(a, b), Set.of(c, d));

            var pairings = pairer.pair(List.of(a, b, c, d), previousPairs, Set.of(), Map.of());

            for (var p : pairings) {
                if (p instanceof Pairing.Match(var p1, var p2)) {
                    var pair = Set.of(p1, p2);
                    assertThat(previousPairs).doesNotContain(pair);
                }
            }
        }

        @Test
        void throwsWhenNoValidPairingExists() {
            var a = player("A");
            var b = player("B");
            var previousPairs = Set.<Set<PlayerId>>of(Set.of(a, b));

            assertThatThrownBy(() -> pairer.pair(List.of(a, b), previousPairs, Set.of(), Map.of()))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class PointBasedGrouping {

        @Test
        void pairsWithinSamePointGroup() {
            var a = player("A");
            var b = player("B");
            var c = player("C");
            var d = player("D");
            var matchPoints = Map.of(a, 3, b, 3, c, 0, d, 0);

            var pairings = pairer.pair(List.of(a, b, c, d), Set.of(), Set.of(), matchPoints);

            var pairs = pairings.stream()
                    .filter(p -> p instanceof Pairing.Match)
                    .map(p -> {
                        var m = (Pairing.Match) p;
                        return Set.of(m.player1(), m.player2());
                    })
                    .toList();
            assertThat(pairs).contains(Set.of(a, b));
            assertThat(pairs).contains(Set.of(c, d));
        }
    }
}
