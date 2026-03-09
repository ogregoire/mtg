package be.imgn.mtg.engine.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.turn.Phase;

@DisplayName("TestGame")
class TestGameTest {

    private static final Path DB_PATH =
            Path.of(System.getProperty("user.home"), "Library", "Application Support", "mtg-engine", "cards.mv.db");

    private TestGame game;

    @BeforeAll
    static void requireDatabase() {
        assumeThat(Files.exists(DB_PATH))
                .as("Card database must exist at %s", DB_PATH)
                .isTrue();
    }

    @BeforeEach
    void setUp() {
        game = TestGame.create();
    }

    @Nested
    @DisplayName("GameSetup")
    class GameSetup {

        @Test
        @DisplayName("creates game with two players")
        void twoPlayers() {
            assertThat(game.player1()).isNotNull();
            assertThat(game.player2()).isNotNull();
            assertThat(game.player1()).isNotSameAs(game.player2());
        }

        @Test
        @DisplayName("provides game state")
        void gameState() {
            assertThat(game.gameState()).isNotNull();
        }

        @Test
        @DisplayName("provides battlefield")
        void battlefield() {
            assertThat(game.battlefield()).isNotNull();
            assertThat(game.battlefield()).isSameAs(game.gameState().battlefield());
        }
    }

    @Nested
    @DisplayName("CreatePermanentTests")
    class CreatePermanentTests {

        @Test
        @DisplayName("creates a permanent from a card name")
        void createsPermanent() {
            var permanent = game.createPermanent("Grizzly Bears");
            assertThat(permanent).isNotNull();
            assertThat(permanent.name()).isEqualTo("Grizzly Bears");
        }

        @Test
        @DisplayName("permanent appears on the battlefield")
        void onBattlefield() {
            var permanent = game.createPermanent("Grizzly Bears");
            assertThat(game.battlefield().all()).contains(permanent);
        }
    }

    @Nested
    @DisplayName("Advancement")
    class Advancement {

        @Test
        @DisplayName("advances to main phase")
        void advanceToMain() {
            game.advanceTo(Phase.MAIN);
            assertThat(game.gameState().battlefield()).isNotNull();
        }
    }
}
