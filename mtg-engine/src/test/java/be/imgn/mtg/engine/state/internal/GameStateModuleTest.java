package be.imgn.mtg.engine.state.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.resolver.EffectExecutor;
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

class GameStateModuleTest {

    @Nested
    class ModuleBindings {

        private Injector injector;
        private Player player1;
        private Player player2;

        @BeforeEach
        void setUp() {
            player1 = mock(Player.class);
            player2 = mock(Player.class);

            // Mock the zones for players
            when(player1.library()).thenReturn(mock(Library.class));
            when(player1.hand()).thenReturn(mock(Hand.class));
            when(player1.graveyard()).thenReturn(mock(Graveyard.class));

            when(player2.library()).thenReturn(mock(Library.class));
            when(player2.hand()).thenReturn(mock(Hand.class));
            when(player2.graveyard()).thenReturn(mock(Graveyard.class));

            injector = Guice.createInjector(binder -> {
                var store = new ObjectStore();
                binder.bind(Battlefield.class)
                        .toInstance(new DefaultBattlefield(store, mock(GameEventProcessor.class)));
                binder.bind(Stack.class)
                        .toInstance(new DefaultStack(
                                store,
                                mock(GameEventProcessor.class),
                                mock(GameState.class),
                                mock(EffectExecutor.class)));
                binder.bind(Exile.class).toInstance(new DefaultExile(store));
                binder.bind(CommandZone.class).toInstance(new DefaultCommandZone(store));
                binder.bind(new TypeLiteral<List<Player>>() {}).toInstance(List.of(player1, player2));
                binder.install(new GameStateModule());
            });
        }

        @Test
        void providesLastKnownInformation() {
            var lki = injector.getInstance(LastKnownInformation.class);

            assertThat(lki).isNotNull();
            assertThat(lki).isInstanceOf(DefaultLastKnownInformation.class);
        }

        @Test
        void providesGameState() {
            var gameState = injector.getInstance(GameState.class);

            assertThat(gameState).isNotNull();
            assertThat(gameState).isInstanceOf(DefaultGameState.class);
        }

        @Test
        void lastKnownInformationIsSingleton() {
            var lki1 = injector.getInstance(LastKnownInformation.class);
            var lki2 = injector.getInstance(LastKnownInformation.class);

            assertThat(lki1).isSameAs(lki2);
        }

        @Test
        void gameStateIsSingleton() {
            var gameState1 = injector.getInstance(GameState.class);
            var gameState2 = injector.getInstance(GameState.class);

            assertThat(gameState1).isSameAs(gameState2);
        }

        @Test
        void gameStateUsesProvidedZones() {
            var gameState = injector.getInstance(GameState.class);
            var battlefield = injector.getInstance(Battlefield.class);
            var stack = injector.getInstance(Stack.class);
            var exile = injector.getInstance(Exile.class);
            var commandZone = injector.getInstance(CommandZone.class);

            assertThat(gameState.battlefield()).isSameAs(battlefield);
            assertThat(gameState.stack()).isSameAs(stack);
            assertThat(gameState.exile()).isSameAs(exile);
            assertThat(gameState.commandZone()).isSameAs(commandZone);
        }

        @Test
        void gameStateUsesProvidedLastKnownInformation() {
            var gameState = injector.getInstance(GameState.class);
            var lki = injector.getInstance(LastKnownInformation.class);

            assertThat(gameState.lastKnownInformation()).isSameAs(lki);
        }

        @Test
        void gameStateUsesProvidedPlayers() {
            var gameState = injector.getInstance(GameState.class);

            assertThat(gameState.players()).containsExactly(player1, player2);
        }
    }
}
