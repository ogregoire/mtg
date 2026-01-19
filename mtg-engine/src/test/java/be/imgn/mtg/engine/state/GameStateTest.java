package be.imgn.mtg.engine.state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.state.internal.DefaultGameState;
import be.imgn.mtg.engine.state.internal.DefaultLastKnownInformation;
import be.imgn.mtg.engine.zone.Battlefield;
import be.imgn.mtg.engine.zone.CommandZone;
import be.imgn.mtg.engine.zone.Exile;
import be.imgn.mtg.engine.zone.Graveyard;
import be.imgn.mtg.engine.zone.Hand;
import be.imgn.mtg.engine.zone.Library;
import be.imgn.mtg.engine.zone.Stack;
import be.imgn.mtg.engine.zone.internal.DefaultBattlefield;
import be.imgn.mtg.engine.zone.internal.DefaultCommandZone;
import be.imgn.mtg.engine.zone.internal.DefaultExile;
import be.imgn.mtg.engine.zone.internal.DefaultGraveyard;
import be.imgn.mtg.engine.zone.internal.DefaultHand;
import be.imgn.mtg.engine.zone.internal.DefaultLibrary;
import be.imgn.mtg.engine.zone.internal.DefaultStack;

class GameStateTest {

    private GameState gameState;
    private Player player1;
    private Player player2;
    private Battlefield battlefield;
    private Stack stack;
    private Exile exile;
    private CommandZone commandZone;
    private LastKnownInformation lki;

    @BeforeEach
    void setUp() {
        battlefield = new DefaultBattlefield();
        stack = new DefaultStack();
        exile = new DefaultExile();
        commandZone = new DefaultCommandZone();
        lki = new DefaultLastKnownInformation();

        // Create players with their zones
        player1 = new TestPlayer("player1");
        player2 = new TestPlayer("player2");

        gameState = new DefaultGameState(battlefield, stack, exile, commandZone, lki, List.of(player1, player2));
    }

    /// Test player implementation that creates its own zones.
    static class TestPlayer implements Player {
        private final String name;
        private final Library library;
        private final Hand hand;
        private final Graveyard graveyard;

        TestPlayer(String name) {
            this.name = name;
            // Create zones that reference this player
            this.library = new DefaultLibrary(this);
            this.hand = new DefaultHand(this);
            this.graveyard = new DefaultGraveyard(this);
        }

        @Override
        public Library library() {
            return library;
        }

        @Override
        public Hand hand() {
            return hand;
        }

        @Override
        public Graveyard graveyard() {
            return graveyard;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Nested
    class SharedZones {

        @Test
        void hasBattlefield() {
            assertThat(gameState.battlefield()).isNotNull();
            assertThat(gameState.battlefield()).isSameAs(battlefield);
        }

        @Test
        void hasStack() {
            assertThat(gameState.stack()).isNotNull();
            assertThat(gameState.stack()).isSameAs(stack);
        }

        @Test
        void hasExile() {
            assertThat(gameState.exile()).isNotNull();
            assertThat(gameState.exile()).isSameAs(exile);
        }

        @Test
        void hasCommandZone() {
            assertThat(gameState.commandZone()).isNotNull();
            assertThat(gameState.commandZone()).isSameAs(commandZone);
        }
    }

    @Nested
    class PlayerZones {

        @Test
        void hasLibraryForEachPlayer() {
            assertThat(gameState.library(player1)).isNotNull();
            assertThat(gameState.library(player2)).isNotNull();
        }

        @Test
        void hasHandForEachPlayer() {
            assertThat(gameState.hand(player1)).isNotNull();
            assertThat(gameState.hand(player2)).isNotNull();
        }

        @Test
        void hasGraveyardForEachPlayer() {
            assertThat(gameState.graveyard(player1)).isNotNull();
            assertThat(gameState.graveyard(player2)).isNotNull();
        }

        @Test
        void playerZonesMatchPlayerAccessors() {
            assertThat(gameState.library(player1)).isSameAs(player1.library());
            assertThat(gameState.hand(player1)).isSameAs(player1.hand());
            assertThat(gameState.graveyard(player1)).isSameAs(player1.graveyard());

            assertThat(gameState.library(player2)).isSameAs(player2.library());
            assertThat(gameState.hand(player2)).isSameAs(player2.hand());
            assertThat(gameState.graveyard(player2)).isSameAs(player2.graveyard());
        }

        @Test
        void eachPlayerHasDistinctZones() {
            assertThat(gameState.library(player1)).isNotSameAs(gameState.library(player2));
            assertThat(gameState.hand(player1)).isNotSameAs(gameState.hand(player2));
            assertThat(gameState.graveyard(player1)).isNotSameAs(gameState.graveyard(player2));
        }

        @Test
        void zoneOwnersAreCorrect() {
            assertThat(gameState.library(player1).owner()).isSameAs(player1);
            assertThat(gameState.hand(player1).owner()).isSameAs(player1);
            assertThat(gameState.graveyard(player1).owner()).isSameAs(player1);

            assertThat(gameState.library(player2).owner()).isSameAs(player2);
            assertThat(gameState.hand(player2).owner()).isSameAs(player2);
            assertThat(gameState.graveyard(player2).owner()).isSameAs(player2);
        }

        @Test
        void throwsForUnknownPlayer() {
            var unknownPlayer = new TestPlayer("unknown");

            assertThatThrownBy(() -> gameState.library(unknownPlayer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown player");

            assertThatThrownBy(() -> gameState.hand(unknownPlayer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown player");

            assertThatThrownBy(() -> gameState.graveyard(unknownPlayer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown player");
        }
    }

    @Nested
    class LKI {

        @Test
        void hasLastKnownInformation() {
            assertThat(gameState.lastKnownInformation()).isNotNull();
            assertThat(gameState.lastKnownInformation()).isSameAs(lki);
        }
    }
}
