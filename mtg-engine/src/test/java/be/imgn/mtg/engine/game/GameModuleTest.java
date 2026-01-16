package be.imgn.mtg.engine.game;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.format.Format;
import be.imgn.mtg.engine.game.internal.GameModule;

class GameModuleTest {

    private Injector rootInjector;
    private GameFactory factory;

    // Test fixtures
    record TestPlayerId(String id) implements PlayerId {}

    record TestTeam(String name) implements Team {}

    static class TestFormat implements Format {
        private final int expectedPlayerCount;

        TestFormat(int expectedPlayerCount) {
            this.expectedPlayerCount = expectedPlayerCount;
        }

        @Override
        public void checkPlayers(List<PlayerData> playersData) {
            if (playersData.size() != expectedPlayerCount) {
                throw new IllegalArgumentException(
                        "Expected " + expectedPlayerCount + " players, got " + playersData.size());
            }
        }
    }

    @BeforeEach
    void setUp() {
        rootInjector = Guice.createInjector(new GameModule());
        factory = rootInjector.getInstance(GameFactory.class);
    }

    @Nested
    class GameModuleBinding {

        @Test
        void providesGameFactory() {
            assertThat(factory).isNotNull();
        }

        @Test
        void factoryIsSingleton() {
            var factory2 = rootInjector.getInstance(GameFactory.class);
            assertThat(factory).isSameAs(factory2);
        }
    }

    @Nested
    class GameCreation {

        @Test
        void createsGame() {
            var format = new TestFormat(2);
            var players = List.of(
                    new PlayerData(new TestPlayerId("p1"), new TestTeam("team1")),
                    new PlayerData(new TestPlayerId("p2"), new TestTeam("team2")));

            var game = factory.createGame(format, players);

            assertThat(game).isNotNull();
        }
    }

    @Nested
    class MultipleGames {

        @Test
        void canCreateMultipleIndependentGames() {
            var format = new TestFormat(2);
            var players = List.of(
                    new PlayerData(new TestPlayerId("p1"), new TestTeam("team1")),
                    new PlayerData(new TestPlayerId("p2"), new TestTeam("team2")));

            var game1 = factory.createGame(format, players);
            var game2 = factory.createGame(format, players);

            assertThat(game1).isNotSameAs(game2);
        }
    }

    @Nested
    class PlayerCount {

        @Test
        void supportsOnePlayer() {
            var format = new TestFormat(1);
            var players = List.of(new PlayerData(new TestPlayerId("p1"), new TestTeam("team1")));

            var game = factory.createGame(format, players);

            assertThat(game).isNotNull();
        }

        @Test
        void supportsFourPlayers() {
            var format = new TestFormat(4);
            var players = List.of(
                    new PlayerData(new TestPlayerId("p1"), new TestTeam("team1")),
                    new PlayerData(new TestPlayerId("p2"), new TestTeam("team2")),
                    new PlayerData(new TestPlayerId("p3"), new TestTeam("team3")),
                    new PlayerData(new TestPlayerId("p4"), new TestTeam("team4")));

            var game = factory.createGame(format, players);

            assertThat(game).isNotNull();
        }
    }
}
