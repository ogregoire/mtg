package be.imgn.mtg.engine.state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.resolver.EffectExecutor;
import be.imgn.mtg.engine.state.internal.DefaultGameState;
import be.imgn.mtg.engine.state.internal.DefaultLastKnownInformation;
import be.imgn.mtg.engine.state.internal.ObjectStore;
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
import be.imgn.mtg.engine.zone.internal.DefaultStack;

class GameStateTest {

    private GameState gameState;
    private Player player1;
    private Player player2;
    private Library library1;
    private Library library2;
    private Hand hand1;
    private Hand hand2;
    private Graveyard graveyard1;
    private Graveyard graveyard2;
    private Battlefield battlefield;
    private Stack stack;
    private Exile exile;
    private CommandZone commandZone;
    private LastKnownInformation lki;

    @BeforeEach
    void setUp() {
        var store = new ObjectStore();
        battlefield = new DefaultBattlefield(store, mock(GameEventProcessor.class));
        stack = new DefaultStack(
                store, mock(GameEventProcessor.class), mock(GameState.class), mock(EffectExecutor.class));
        exile = new DefaultExile(store);
        commandZone = new DefaultCommandZone(store);
        lki = new DefaultLastKnownInformation();

        // Create mocked players with mocked zones
        player1 = mock(Player.class);
        player2 = mock(Player.class);

        library1 = mock(Library.class);
        library2 = mock(Library.class);
        hand1 = mock(Hand.class);
        hand2 = mock(Hand.class);
        graveyard1 = mock(Graveyard.class);
        graveyard2 = mock(Graveyard.class);

        when(player1.library()).thenReturn(library1);
        when(player1.hand()).thenReturn(hand1);
        when(player1.graveyard()).thenReturn(graveyard1);

        when(player2.library()).thenReturn(library2);
        when(player2.hand()).thenReturn(hand2);
        when(player2.graveyard()).thenReturn(graveyard2);

        when(library1.owner()).thenReturn(player1);
        when(hand1.owner()).thenReturn(player1);
        when(graveyard1.owner()).thenReturn(player1);

        when(library2.owner()).thenReturn(player2);
        when(hand2.owner()).thenReturn(player2);
        when(graveyard2.owner()).thenReturn(player2);

        gameState = new DefaultGameState(store, battlefield, stack, exile, commandZone, lki, List.of(player1, player2));
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
            assertThat(gameState.library(player1)).isSameAs(library1);
            assertThat(gameState.hand(player1)).isSameAs(hand1);
            assertThat(gameState.graveyard(player1)).isSameAs(graveyard1);

            assertThat(gameState.library(player2)).isSameAs(library2);
            assertThat(gameState.hand(player2)).isSameAs(hand2);
            assertThat(gameState.graveyard(player2)).isSameAs(graveyard2);
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
            var unknownPlayer = mock(Player.class);

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
