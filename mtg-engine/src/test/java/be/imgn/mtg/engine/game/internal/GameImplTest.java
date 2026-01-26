package be.imgn.mtg.engine.game.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.game.Player;

class GameImplTest {

    @Nested
    class Construction {

        @Test
        void createsGameWithPlayers() {
            var player1 = mock(Player.class);
            var player2 = mock(Player.class);
            var game = new GameImpl(List.of(player1, player2));

            assertThat(game.players()).containsExactly(player1, player2);
        }

        @Test
        void makesDefensiveCopyOfPlayersList() {
            var player1 = mock(Player.class);
            var players = new ArrayList<>(List.of(player1));
            var game = new GameImpl(players);

            // GameImpl makes a defensive copy, so modifying the original shouldn't affect the game
            players.clear();
            assertThat(game.players()).hasSize(1);
            assertThat(game.players()).containsExactly(player1);
        }

        @Test
        void preservesPlayerOrder() {
            var p1 = mock(Player.class);
            var p2 = mock(Player.class);
            var p3 = mock(Player.class);
            var p4 = mock(Player.class);
            var game = new GameImpl(List.of(p1, p2, p3, p4));

            assertThat(game.players()).containsExactly(p1, p2, p3, p4);
        }
    }

    @Nested
    class Players {

        @Test
        void returnsUnmodifiableList() {
            var player = mock(Player.class);
            var game = new GameImpl(List.of(player));
            var players = game.players();

            assertThat(players).isUnmodifiable();
        }

        @Test
        void returnsSameListOnMultipleCalls() {
            var player = mock(Player.class);
            var game = new GameImpl(List.of(player));

            var players1 = game.players();
            var players2 = game.players();

            assertThat(players1).isSameAs(players2);
        }

        @Test
        void supportsEmptyPlayerList() {
            var game = new GameImpl(List.of());

            assertThat(game.players()).isEmpty();
        }

        @Test
        void supportsSinglePlayer() {
            var player = mock(Player.class);
            var game = new GameImpl(List.of(player));

            assertThat(game.players()).hasSize(1);
            assertThat(game.players().get(0)).isEqualTo(player);
        }

        @Test
        void supportsMultiplePlayers() {
            var p1 = mock(Player.class);
            var p2 = mock(Player.class);
            var p3 = mock(Player.class);
            var game = new GameImpl(List.of(p1, p2, p3));

            assertThat(game.players()).hasSize(3);
        }
    }
}
