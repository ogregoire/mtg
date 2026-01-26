package be.imgn.mtg.engine.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.game.Player;

class GameResultTest {

    @Nested
    class WinnerTests {

        @Test
        void createWinner() {
            var player = mock(Player.class);
            var condition = new WinCondition.LastPlayerStanding(player);

            var result = new GameResult.Winner(player, condition);

            assertThat(result.winner()).isSameAs(player);
            assertThat(result.condition()).isEqualTo(condition);
        }

        @Test
        void winnerEquality() {
            var player = mock(Player.class);
            var condition = new WinCondition.LastPlayerStanding(player);

            var result1 = new GameResult.Winner(player, condition);
            var result2 = new GameResult.Winner(player, condition);

            assertThat(result1).isEqualTo(result2);
            assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        }
    }

    @Nested
    class WinnersTests {

        @Test
        void createWinners() {
            var player1 = mock(Player.class);
            var player2 = mock(Player.class);
            var winners = List.of(player1, player2);
            var condition = new WinCondition.TeamWin("team1");

            var result = new GameResult.Winners(winners, condition);

            assertThat(result.winners()).containsExactly(player1, player2);
            assertThat(result.condition()).isEqualTo(condition);
        }
    }

    @Nested
    class DrawTests {

        @Test
        void createDraw() {
            var condition = new DrawCondition.InfiniteLoop();

            var result = new GameResult.Draw(condition);

            assertThat(result.condition()).isEqualTo(condition);
        }
    }

    @Nested
    class RestartTests {

        @Test
        void createRestart() {
            var result = new GameResult.Restart("Karn Liberated");

            assertThat(result.cause()).isEqualTo("Karn Liberated");
        }
    }

    @Nested
    class WinConditionTests {

        @Test
        void lastPlayerStanding() {
            var player = mock(Player.class);

            var condition = new WinCondition.LastPlayerStanding(player);

            assertThat(condition.winner()).isSameAs(player);
        }

        @Test
        void effectWin() {
            var player = mock(Player.class);

            var condition = new WinCondition.EffectWin(player, "Laboratory Maniac");

            assertThat(condition.winner()).isSameAs(player);
            assertThat(condition.cause()).isEqualTo("Laboratory Maniac");
        }

        @Test
        void teamWin() {
            var condition = new WinCondition.TeamWin("team-alpha");

            assertThat(condition.winningTeamId()).isEqualTo("team-alpha");
        }
    }

    @Nested
    class DrawConditionTests {

        @Test
        void simultaneousLoss() {
            var player1 = mock(Player.class);
            var player2 = mock(Player.class);

            var condition = new DrawCondition.SimultaneousLoss(List.of(player1, player2));

            assertThat(condition.players()).containsExactly(player1, player2);
        }

        @Test
        void infiniteLoop() {
            var condition = new DrawCondition.InfiniteLoop();

            assertThat(condition).isNotNull();
        }

        @Test
        void effectDraw() {
            var condition = new DrawCondition.EffectDraw("Divine Intervention");

            assertThat(condition.cause()).isEqualTo("Divine Intervention");
        }
    }

    @Nested
    class LossReasonTests {

        @Test
        void allReasonsExist() {
            assertThat(LossReason.values())
                    .containsExactly(
                            LossReason.CONCESSION,
                            LossReason.ZERO_LIFE,
                            LossReason.EMPTY_LIBRARY_DRAW,
                            LossReason.POISON,
                            LossReason.EFFECT,
                            LossReason.COMMANDER_DAMAGE);
        }

        @Test
        void valueOfWorks() {
            assertThat(LossReason.valueOf("ZERO_LIFE")).isEqualTo(LossReason.ZERO_LIFE);
        }
    }
}
