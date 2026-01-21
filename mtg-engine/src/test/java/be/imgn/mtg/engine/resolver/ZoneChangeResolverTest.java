package be.imgn.mtg.engine.resolver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.Abilities;
import be.imgn.mtg.engine.characteristics.Colors;
import be.imgn.mtg.engine.characteristics.Costs;
import be.imgn.mtg.engine.characteristics.Subtypes;
import be.imgn.mtg.engine.characteristics.Supertypes;
import be.imgn.mtg.engine.characteristics.Types;
import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.ObjectId;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.resolver.internal.ZoneChangeResolver;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.state.LastKnownInformation;
import be.imgn.mtg.engine.state.ObjectSnapshot;
import be.imgn.mtg.engine.zone.Battlefield;
import be.imgn.mtg.engine.zone.DiesEvent;
import be.imgn.mtg.engine.zone.DiscardEvent;
import be.imgn.mtg.engine.zone.DrawEvent;
import be.imgn.mtg.engine.zone.EntersBattlefieldEvent;
import be.imgn.mtg.engine.zone.Graveyard;
import be.imgn.mtg.engine.zone.Hand;
import be.imgn.mtg.engine.zone.Library;
import be.imgn.mtg.engine.zone.MillEvent;
import be.imgn.mtg.engine.zone.ShuffleIntoLibraryEvent;
import be.imgn.mtg.engine.zone.Stack;
import be.imgn.mtg.engine.zone.ZoneType;

class ZoneChangeResolverTest {

    private ZoneChangeResolver resolver;
    private LastKnownInformation lki;
    private GameState gameState;
    private Player player;

    // Zones
    private Library library;
    private Hand hand;
    private Graveyard graveyard;
    private Battlefield battlefield;
    private Stack stack;

    @BeforeEach
    void setUp() {
        lki = mock(LastKnownInformation.class);
        gameState = mock(GameState.class);
        player = mock(Player.class);

        library = mock(Library.class);
        hand = mock(Hand.class);
        graveyard = mock(Graveyard.class);
        battlefield = mock(Battlefield.class);
        stack = mock(Stack.class);

        when(gameState.library(any())).thenReturn(library);
        when(gameState.hand(any())).thenReturn(hand);
        when(gameState.graveyard(any())).thenReturn(graveyard);
        when(gameState.battlefield()).thenReturn(battlefield);
        when(gameState.stack()).thenReturn(stack);

        resolver = new ZoneChangeResolver(lki);
    }

    @Nested
    class LkiRecording {

        @Test
        void recordsLkiBeforeZoneChange() {
            var card = createCard();
            var event = new DrawEvent(card, player);

            resolver.resolve(event, gameState);

            verify(lki).record(any(ObjectSnapshot.class));
        }
    }

    @Nested
    class DrawEvents {

        @Test
        void removesCardFromLibrary() {
            var card = createCard();
            var event = new DrawEvent(card, player);

            resolver.resolve(event, gameState);

            verify(library).remove(card.id());
        }

        @Test
        void addsCardToHand() {
            var card = createCard();
            var event = new DrawEvent(card, player);

            resolver.resolve(event, gameState);

            verify(hand).add(card);
        }
    }

    @Nested
    class DiscardEvents {

        @Test
        void removesCardFromHand() {
            var card = createCard();
            var event = new DiscardEvent(card, player);

            resolver.resolve(event, gameState);

            verify(hand).remove(card.id());
        }

        @Test
        void putsCardInGraveyard() {
            var card = createCard();
            var event = new DiscardEvent(card, player);

            resolver.resolve(event, gameState);

            verify(graveyard).put(card);
        }
    }

    @Nested
    class MillEvents {

        @Test
        void removesAllCardsFromLibrary() {
            var card1 = createCard();
            var card2 = createCard();
            var event = new MillEvent(List.of(card1, card2), player);

            resolver.resolve(event, gameState);

            verify(library).remove(card1.id());
            verify(library).remove(card2.id());
        }

        @Test
        void putsAllCardsInGraveyard() {
            var card1 = createCard();
            var card2 = createCard();
            var event = new MillEvent(List.of(card1, card2), player);

            resolver.resolve(event, gameState);

            verify(graveyard).put(card1);
            verify(graveyard).put(card2);
        }

        @Test
        void recordsLkiForEachCard() {
            var card1 = createCard();
            var card2 = createCard();
            var event = new MillEvent(List.of(card1, card2), player);

            resolver.resolve(event, gameState);

            verify(lki, times(2)).record(any());
        }
    }

    @Nested
    class DiesEvents {

        @Test
        void removesPermanentFromBattlefield() {
            var card = createCard();
            var permanent = createPermanent(card);
            var event = new DiesEvent(permanent, new DiesEvent.DeathCause.Destroyed());

            resolver.resolve(event, gameState);

            verify(battlefield).remove(permanent.id());
        }

        @Test
        void putsSourceCardInGraveyard() {
            var card = createCard();
            var permanent = createPermanent(card);
            var event = new DiesEvent(permanent, new DiesEvent.DeathCause.Destroyed());

            resolver.resolve(event, gameState);

            verify(graveyard).put(card);
        }
    }

    @Nested
    class EntersBattlefieldEvents {

        @Test
        void addsPermanentToBattlefield() {
            var card = createCard();
            var permanent = createPermanent(card);
            var event = new EntersBattlefieldEvent(permanent, ZoneType.HAND, new EntersBattlefieldEvent.EtbCause.Put());

            resolver.resolve(event, gameState);

            verify(battlefield).enter(permanent);
        }

        @Test
        void removesSourceCardFromPreviousZone() {
            var card = createCard();
            var permanent = createPermanent(card);
            var event = new EntersBattlefieldEvent(permanent, ZoneType.HAND, new EntersBattlefieldEvent.EtbCause.Put());

            resolver.resolve(event, gameState);

            verify(hand).remove(card.id());
        }
    }

    @Nested
    class ShuffleIntoLibraryEvents {

        @Test
        void putsCardsOnBottomOfLibrary() {
            var card1 = createCard();
            var card2 = createCard();
            var event = new ShuffleIntoLibraryEvent(List.of(card1, card2), ZoneType.GRAVEYARD, player);

            resolver.resolve(event, gameState);

            verify(library).putOnBottom(card1);
            verify(library).putOnBottom(card2);
        }

        @Test
        void shufflesLibrary() {
            var card = createCard();
            var event = new ShuffleIntoLibraryEvent(List.of(card), ZoneType.GRAVEYARD, player);

            resolver.resolve(event, gameState);

            verify(library).shuffle();
        }

        @Test
        void removesCardsFromSourceZone() {
            var card = createCard();
            var event = new ShuffleIntoLibraryEvent(List.of(card), ZoneType.GRAVEYARD, player);

            resolver.resolve(event, gameState);

            verify(graveyard).remove(card.id());
        }
    }

    // Helper methods

    private Card createCard() {
        var card = mock(Card.class);
        var id = new ObjectId();
        when(card.id()).thenReturn(id);
        when(card.owner()).thenReturn(player);
        when(card.controller()).thenReturn(player);
        when(card.name()).thenReturn("Test Card");
        when(card.colors()).thenReturn(Colors.empty());
        when(card.types()).thenReturn(Types.empty());
        when(card.supertypes()).thenReturn(Supertypes.empty());
        when(card.subtypes()).thenReturn(Subtypes.empty());
        when(card.abilities()).thenReturn(Abilities.empty());
        when(card.costs()).thenReturn(Costs.empty());
        when(card.power()).thenReturn(Value.of(0));
        when(card.toughness()).thenReturn(Value.of(0));
        when(card.loyalty()).thenReturn(null);
        return card;
    }

    private Permanent createPermanent(Card sourceCard) {
        var permanent = mock(Permanent.class);
        var id = new ObjectId();
        when(permanent.id()).thenReturn(id);
        when(permanent.source()).thenReturn(sourceCard);
        when(permanent.owner()).thenReturn(player);
        when(permanent.controller()).thenReturn(player);
        when(permanent.name()).thenReturn("Test Permanent");
        when(permanent.colors()).thenReturn(Colors.empty());
        when(permanent.types()).thenReturn(Types.empty());
        when(permanent.supertypes()).thenReturn(Supertypes.empty());
        when(permanent.subtypes()).thenReturn(Subtypes.empty());
        when(permanent.abilities()).thenReturn(Abilities.empty());
        when(permanent.costs()).thenReturn(Costs.empty());
        when(permanent.power()).thenReturn(Value.of(0));
        when(permanent.toughness()).thenReturn(Value.of(0));
        when(permanent.loyalty()).thenReturn(null);
        return permanent;
    }
}
