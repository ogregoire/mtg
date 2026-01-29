package be.imgn.mtg.engine.resolver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.object.TapEvent;
import be.imgn.mtg.engine.resolver.internal.TapResolver;
import be.imgn.mtg.engine.state.GameState;

class TapResolverTest {

    private TapResolver resolver;
    private GameState gameState;
    private Permanent permanent;
    private Player player;

    @BeforeEach
    void setUp() {
        resolver = new TapResolver();
        gameState = mock(GameState.class);
        player = mock(Player.class);
        permanent = mock(Permanent.class);

        when(permanent.controller()).thenReturn(player);
        when(permanent.name()).thenReturn("Test Permanent");
    }

    @Nested
    class EventType {

        @Test
        void returnsTapEventClass() {
            var result = resolver.eventType();

            assert result == TapEvent.class;
        }
    }

    @Nested
    class TapEvents {

        @Test
        void tapsPermanentWhenTapping() {
            var event = new TapEvent(permanent, true);

            resolver.resolve(event, gameState);

            verify(permanent).tap();
        }

        @Test
        void handlesAlreadyTappedPermanent() {
            // Even if the permanent is already tapped, the event should resolve
            var event = new TapEvent(permanent, true);

            resolver.resolve(event, gameState);

            verify(permanent).tap();
        }
    }

    @Nested
    class UntapEvents {

        @Test
        void untapsPermanentWhenUntapping() {
            var event = new TapEvent(permanent, false);

            resolver.resolve(event, gameState);

            verify(permanent).untap();
        }

        @Test
        void handlesAlreadyUntappedPermanent() {
            // Even if the permanent is already untapped, the event should resolve
            var event = new TapEvent(permanent, false);

            resolver.resolve(event, gameState);

            verify(permanent).untap();
        }
    }

    @Nested
    class MultipleEvents {

        @Test
        void handlesTapFollowedByUntap() {
            var tapEvent = new TapEvent(permanent, true);
            var untapEvent = new TapEvent(permanent, false);

            resolver.resolve(tapEvent, gameState);
            resolver.resolve(untapEvent, gameState);

            verify(permanent).tap();
            verify(permanent).untap();
        }

        @Test
        void handlesUntapFollowedByTap() {
            var untapEvent = new TapEvent(permanent, false);
            var tapEvent = new TapEvent(permanent, true);

            resolver.resolve(untapEvent, gameState);
            resolver.resolve(tapEvent, gameState);

            verify(permanent).untap();
            verify(permanent).tap();
        }
    }
}
