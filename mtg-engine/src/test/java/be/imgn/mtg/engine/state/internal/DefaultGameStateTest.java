package be.imgn.mtg.engine.state.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.Spell;
import be.imgn.mtg.engine.result.DrawCondition;
import be.imgn.mtg.engine.result.GameResult;
import be.imgn.mtg.engine.result.WinCondition;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.state.LastKnownInformation;
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

class DefaultGameStateTest {

    private GameState gameState;
    private Player player1;
    private Player player2;
    private Player player3;
    private Library library1;
    private Library library2;
    private Library library3;
    private Hand hand1;
    private Hand hand2;
    private Hand hand3;
    private Graveyard graveyard1;
    private Graveyard graveyard2;
    private Graveyard graveyard3;
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

        // Create mocked players with mocked zones
        player1 = mock(Player.class);
        player2 = mock(Player.class);
        player3 = mock(Player.class);

        library1 = mock(Library.class);
        library2 = mock(Library.class);
        library3 = mock(Library.class);
        hand1 = mock(Hand.class);
        hand2 = mock(Hand.class);
        hand3 = mock(Hand.class);
        graveyard1 = mock(Graveyard.class);
        graveyard2 = mock(Graveyard.class);
        graveyard3 = mock(Graveyard.class);

        when(player1.library()).thenReturn(library1);
        when(player1.hand()).thenReturn(hand1);
        when(player1.graveyard()).thenReturn(graveyard1);

        when(player2.library()).thenReturn(library2);
        when(player2.hand()).thenReturn(hand2);
        when(player2.graveyard()).thenReturn(graveyard2);

        when(player3.library()).thenReturn(library3);
        when(player3.hand()).thenReturn(hand3);
        when(player3.graveyard()).thenReturn(graveyard3);

        gameState =
                new DefaultGameState(battlefield, stack, exile, commandZone, lki, List.of(player1, player2, player3));
    }

    @Nested
    class SharedZones {

        @Test
        void returnsBattlefield() {
            assertThat(gameState.battlefield()).isSameAs(battlefield);
        }

        @Test
        void returnsStack() {
            assertThat(gameState.stack()).isSameAs(stack);
        }

        @Test
        void returnsExile() {
            assertThat(gameState.exile()).isSameAs(exile);
        }

        @Test
        void returnsCommandZone() {
            assertThat(gameState.commandZone()).isSameAs(commandZone);
        }
    }

    @Nested
    class PlayerZones {

        @Test
        void returnsLibraryForPlayer() {
            assertThat(gameState.library(player1)).isSameAs(library1);
            assertThat(gameState.library(player2)).isSameAs(library2);
            assertThat(gameState.library(player3)).isSameAs(library3);
        }

        @Test
        void returnsHandForPlayer() {
            assertThat(gameState.hand(player1)).isSameAs(hand1);
            assertThat(gameState.hand(player2)).isSameAs(hand2);
            assertThat(gameState.hand(player3)).isSameAs(hand3);
        }

        @Test
        void returnsGraveyardForPlayer() {
            assertThat(gameState.graveyard(player1)).isSameAs(graveyard1);
            assertThat(gameState.graveyard(player2)).isSameAs(graveyard2);
            assertThat(gameState.graveyard(player3)).isSameAs(graveyard3);
        }

        @Test
        void throwsForUnknownPlayerInLibrary() {
            var unknownPlayer = mock(Player.class);

            assertThatThrownBy(() -> gameState.library(unknownPlayer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown player");
        }

        @Test
        void throwsForUnknownPlayerInHand() {
            var unknownPlayer = mock(Player.class);

            assertThatThrownBy(() -> gameState.hand(unknownPlayer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown player");
        }

        @Test
        void throwsForUnknownPlayerInGraveyard() {
            var unknownPlayer = mock(Player.class);

            assertThatThrownBy(() -> gameState.graveyard(unknownPlayer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown player");
        }
    }

    @Nested
    class FindZone {

        @Test
        void returnsEmptyWhenNotFound() {
            var object = mock(Card.class);

            var result = gameState.findZone(object);

            assertThat(result).isEmpty();
        }

        @Test
        void findsZoneForBattlefieldObject() {
            var card = Card.builder()
                    .owner(player1)
                    .controller(player1)
                    .name("Test")
                    .build();
            var permanent = battlefield.enter(card, player1);

            var result = gameState.findZone(permanent);

            assertThat(result).isPresent();
            assertThat(result.get()).isSameAs(battlefield);
        }

        @Test
        void findsZoneForStackObject() {
            var card = Card.builder()
                    .owner(player1)
                    .controller(player1)
                    .name("Test")
                    .build();
            var spell = Spell.fromCard(card, player1).build();
            stack.push(spell);

            var result = gameState.findZone(spell);

            assertThat(result).isPresent();
            assertThat(result.get()).isSameAs(stack);
        }

        @Test
        void findsZoneForExileObject() {
            var card = Card.builder()
                    .owner(player1)
                    .controller(player1)
                    .name("Test")
                    .build();
            exile.exile(card);

            var result = gameState.findZone(card);

            assertThat(result).isPresent();
            assertThat(result.get()).isSameAs(exile);
        }

        @Test
        void findsZoneForCommandZoneObject() {
            var card = Card.builder()
                    .owner(player1)
                    .controller(player1)
                    .name("Test")
                    .build();
            commandZone.addCommander(card, player1);

            var result = gameState.findZone(card);

            assertThat(result).isPresent();
            assertThat(result.get()).isSameAs(commandZone);
        }

        @Test
        void findsZoneForLibraryObject() {
            var card = mock(Card.class);
            when(library2.containsObject(card)).thenReturn(true);

            var result = gameState.findZone(card);

            assertThat(result).isPresent();
            assertThat(result.get()).isSameAs(library2);
        }

        @Test
        void findsZoneForHandObject() {
            var card = mock(Card.class);
            when(hand3.containsObject(card)).thenReturn(true);

            var result = gameState.findZone(card);

            assertThat(result).isPresent();
            assertThat(result.get()).isSameAs(hand3);
        }

        @Test
        void findsZoneForGraveyardObject() {
            var card = mock(Card.class);
            when(graveyard1.containsObject(card)).thenReturn(true);

            var result = gameState.findZone(card);

            assertThat(result).isPresent();
            assertThat(result.get()).isSameAs(graveyard1);
        }
    }

    @Nested
    class LastKnownInformationTracking {

        @Test
        void returnsLastKnownInformation() {
            assertThat(gameState.lastKnownInformation()).isSameAs(lki);
        }
    }

    @Nested
    class ActivePlayer {

        @Test
        void throwsWhenActivePlayerNotSet() {
            assertThatThrownBy(() -> gameState.activePlayer())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Active player not yet set");
        }

        @Test
        void setsActivePlayer() {
            gameState.setActivePlayer(player1);

            assertThat(gameState.activePlayer()).isSameAs(player1);
        }

        @Test
        void changesActivePlayer() {
            gameState.setActivePlayer(player1);
            gameState.setActivePlayer(player2);

            assertThat(gameState.activePlayer()).isSameAs(player2);
        }

        @Test
        void throwsForUnknownPlayer() {
            var unknownPlayer = mock(Player.class);

            assertThatThrownBy(() -> gameState.setActivePlayer(unknownPlayer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown player");
        }
    }

    @Nested
    class Players {

        @Test
        void returnsAllPlayers() {
            var players = gameState.players();

            assertThat(players).containsExactly(player1, player2, player3);
        }

        @Test
        void returnsUnmodifiableList() {
            var players = gameState.players();

            assertThatThrownBy(() -> players.add(mock(Player.class))).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void playerListIsNotAffectedBySourceModification() {
            var originalList = new ArrayList<>(List.of(player1, player2));
            var localGameState = new DefaultGameState(battlefield, stack, exile, commandZone, lki, originalList);

            originalList.add(player3);

            assertThat(localGameState.players()).containsExactly(player1, player2);
        }
    }

    @Nested
    class TurnOrder {

        @Test
        void returnsNextPlayerInOrder() {
            var next = gameState.nextPlayerInTurnOrder(player1);

            assertThat(next).isSameAs(player2);
        }

        @Test
        void wrapsAroundToFirstPlayer() {
            var next = gameState.nextPlayerInTurnOrder(player3);

            assertThat(next).isSameAs(player1);
        }

        @Test
        void worksWithTwoPlayers() {
            var twoPlayerState =
                    new DefaultGameState(battlefield, stack, exile, commandZone, lki, List.of(player1, player2));

            assertThat(twoPlayerState.nextPlayerInTurnOrder(player1)).isSameAs(player2);
            assertThat(twoPlayerState.nextPlayerInTurnOrder(player2)).isSameAs(player1);
        }

        @Test
        void throwsForUnknownPlayer() {
            var unknownPlayer = mock(Player.class);

            assertThatThrownBy(() -> gameState.nextPlayerInTurnOrder(unknownPlayer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown player");
        }
    }

    @Nested
    class GameEnd {

        @Test
        void gameNotOverInitially() {
            assertThat(gameState.isGameOver()).isFalse();
            assertThat(gameState.getResult()).isEmpty();
        }

        @Test
        void setResultMarksGameOver() {
            var result = new GameResult.Winner(player1, new WinCondition.LastPlayerStanding(player1));

            gameState.setResult(result);

            assertThat(gameState.isGameOver()).isTrue();
            assertThat(gameState.getResult()).isPresent();
            assertThat(gameState.getResult().get()).isSameAs(result);
        }

        @Test
        void canSetDrawResult() {
            var result = new GameResult.Draw(new DrawCondition.InfiniteLoop());

            gameState.setResult(result);

            assertThat(gameState.isGameOver()).isTrue();
            assertThat(gameState.getResult()).contains(result);
        }

        @Test
        void canSetWinnersResult() {
            var result = new GameResult.Winners(List.of(player1, player2), new WinCondition.TeamWin("Team A"));

            gameState.setResult(result);

            assertThat(gameState.isGameOver()).isTrue();
            assertThat(gameState.getResult()).contains(result);
        }

        @Test
        void canSetRestartResult() {
            var result = new GameResult.Restart("Karn Liberated");

            gameState.setResult(result);

            assertThat(gameState.isGameOver()).isTrue();
            assertThat(gameState.getResult()).contains(result);
        }

        @Test
        void canOverwriteResult() {
            var firstResult = new GameResult.Winner(player1, new WinCondition.LastPlayerStanding(player1));
            var secondResult = new GameResult.Draw(new DrawCondition.InfiniteLoop());

            gameState.setResult(firstResult);
            gameState.setResult(secondResult);

            assertThat(gameState.getResult()).contains(secondResult);
        }
    }

    @Nested
    class ManaPool {

        @Test
        void emptyManaPoolsDoesNotThrow() {
            // Currently a no-op, but should not throw
            gameState.emptyManaPools();
        }

        @Test
        void emptyManaPoolsCanBeCalledMultipleTimes() {
            gameState.emptyManaPools();
            gameState.emptyManaPools();
            gameState.emptyManaPools();
        }
    }
}
