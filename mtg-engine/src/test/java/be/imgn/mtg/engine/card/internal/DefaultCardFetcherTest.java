package be.imgn.mtg.engine.card.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.Mockito.mock;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.CreatureType;
import be.imgn.mtg.engine.game.Player;

@DisplayName("DefaultCardFetcher")
class DefaultCardFetcherTest {

    private static final Path DB_PATH =
            Path.of(System.getProperty("user.home"), "Library", "Application Support", "mtg-engine", "cards.mv.db");

    private final DefaultCardFetcher fetcher = new DefaultCardFetcher();
    private final Player owner = mock(Player.class);

    @BeforeAll
    static void requireDatabase() {
        assumeThat(Files.exists(DB_PATH))
                .as("Card database must exist at %s", DB_PATH)
                .isTrue();
    }

    @Nested
    @DisplayName("fetchByName")
    class FetchByName {

        @Test
        @DisplayName("fetches Grizzly Bears with correct types")
        void grizzlyBears() {
            var card = fetcher.fetchByName("Grizzly Bears", owner);
            assertThat(card).isPresent();
            var c = card.get();
            assertThat(c.name()).isEqualTo("Grizzly Bears");
            assertThat(c.types().isCreature()).isTrue();
            assertThat(c.subtypes().contains(CreatureType.BEAR)).isTrue();
        }

        @Test
        @DisplayName("fetches Lightning Bolt as instant")
        void lightningBolt() {
            var card = fetcher.fetchByName("Lightning Bolt", owner);
            assertThat(card).isPresent();
            assertThat(card.get().types().isInstant()).isTrue();
            assertThat(card.get().manaCost()).isNotNull();
        }

        @Test
        @DisplayName("returns empty for nonexistent card")
        void notFound() {
            var card = fetcher.fetchByName("Nonexistent Card XYZ", owner);
            assertThat(card).isEmpty();
        }

        @Test
        @DisplayName("case-insensitive name match")
        void caseInsensitive() {
            var card = fetcher.fetchByName("grizzly bears", owner);
            assertThat(card).isPresent();
        }
    }
}
