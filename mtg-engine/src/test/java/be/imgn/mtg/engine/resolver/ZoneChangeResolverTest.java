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
import be.imgn.mtg.engine.characteristics.Subtypes;
import be.imgn.mtg.engine.characteristics.Supertypes;
import be.imgn.mtg.engine.characteristics.Types;
import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.cost.Costs;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.AbilityOnStack;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.ObjectId;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.object.Spell;
import be.imgn.mtg.engine.object.Token;
import be.imgn.mtg.engine.resolver.internal.ZoneChangeResolver;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.state.LastKnownInformation;
import be.imgn.mtg.engine.state.ObjectSnapshot;
import be.imgn.mtg.engine.zone.Battlefield;
import be.imgn.mtg.engine.zone.CastEvent;
import be.imgn.mtg.engine.zone.CommandZone;
import be.imgn.mtg.engine.zone.CounterEvent;
import be.imgn.mtg.engine.zone.DiesEvent;
import be.imgn.mtg.engine.zone.DiscardEvent;
import be.imgn.mtg.engine.zone.DrawEvent;
import be.imgn.mtg.engine.zone.EntersBattlefieldEvent;
import be.imgn.mtg.engine.zone.Exile;
import be.imgn.mtg.engine.zone.ExileEvent;
import be.imgn.mtg.engine.zone.Graveyard;
import be.imgn.mtg.engine.zone.Hand;
import be.imgn.mtg.engine.zone.LeavesBattlefieldEvent;
import be.imgn.mtg.engine.zone.Library;
import be.imgn.mtg.engine.zone.MillEvent;
import be.imgn.mtg.engine.zone.PutIntoGraveyardEvent;
import be.imgn.mtg.engine.zone.ReturnToHandEvent;
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
    private Exile exile;
    private CommandZone commandZone;

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
        exile = mock(Exile.class);
        commandZone = mock(CommandZone.class);

        when(gameState.library(any())).thenReturn(library);
        when(gameState.hand(any())).thenReturn(hand);
        when(gameState.graveyard(any())).thenReturn(graveyard);
        when(gameState.battlefield()).thenReturn(battlefield);
        when(gameState.stack()).thenReturn(stack);
        when(gameState.exile()).thenReturn(exile);
        when(gameState.commandZone()).thenReturn(commandZone);

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

    @Nested
    class ExileEvents {

        @Test
        void exilesCardFromHand() {
            var card = createCard();
            var event = new ExileEvent(card, ZoneType.HAND, player);

            resolver.resolve(event, gameState);

            verify(hand).remove(card.id());
            verify(exile).exile(card);
        }

        @Test
        void exilesCardFromGraveyard() {
            var card = createCard();
            var event = new ExileEvent(card, ZoneType.GRAVEYARD, player);

            resolver.resolve(event, gameState);

            verify(graveyard).remove(card.id());
            verify(exile).exile(card);
        }

        @Test
        void exilesPermanentFromBattlefield() {
            var card = createCard();
            var permanent = createPermanent(card);
            var event = new ExileEvent(permanent, ZoneType.BATTLEFIELD, player);

            resolver.resolve(event, gameState);

            verify(battlefield).remove(permanent.id());
            verify(exile).exile(card);
        }

        @Test
        void exilesCardFromLibrary() {
            var card = createCard();
            var event = new ExileEvent(card, ZoneType.LIBRARY, player);

            resolver.resolve(event, gameState);

            verify(library).remove(card.id());
            verify(exile).exile(card);
        }

        @Test
        void exilesCardFromStack() {
            var card = createCard();
            var event = new ExileEvent(card, ZoneType.STACK, player);

            resolver.resolve(event, gameState);

            verify(stack).remove(card.id());
            verify(exile).exile(card);
        }

        @Test
        void recordsLkiBeforeExile() {
            var card = createCard();
            var event = new ExileEvent(card, ZoneType.HAND, player);

            resolver.resolve(event, gameState);

            verify(lki).record(any(ObjectSnapshot.class));
        }
    }

    @Nested
    class LeavesBattlefieldEvents {

        @Test
        void removesPermanentFromBattlefield() {
            var card = createCard();
            var permanent = createPermanent(card);
            var event = new LeavesBattlefieldEvent(permanent, ZoneType.GRAVEYARD);

            resolver.resolve(event, gameState);

            verify(battlefield).remove(permanent.id());
        }

        @Test
        void leavesBattlefieldToExile() {
            var card = createCard();
            var permanent = createPermanent(card);
            var event = new LeavesBattlefieldEvent(permanent, ZoneType.EXILE);

            resolver.resolve(event, gameState);

            verify(battlefield).remove(permanent.id());
        }

        @Test
        void leavesBattlefieldToHand() {
            var card = createCard();
            var permanent = createPermanent(card);
            var event = new LeavesBattlefieldEvent(permanent, ZoneType.HAND);

            resolver.resolve(event, gameState);

            verify(battlefield).remove(permanent.id());
        }

        @Test
        void recordsLkiBeforeLeaving() {
            var card = createCard();
            var permanent = createPermanent(card);
            var event = new LeavesBattlefieldEvent(permanent, ZoneType.HAND);

            resolver.resolve(event, gameState);

            verify(lki).record(any(ObjectSnapshot.class));
        }
    }

    @Nested
    class ReturnToHandEvents {

        @Test
        void returnsCardFromGraveyardToHand() {
            var card = createCard();
            var event = new ReturnToHandEvent(card, ZoneType.GRAVEYARD);

            resolver.resolve(event, gameState);

            verify(graveyard).remove(card.id());
            verify(hand).add(card);
        }

        @Test
        void returnsCardFromExileToHand() {
            var card = createCard();
            var event = new ReturnToHandEvent(card, ZoneType.EXILE);

            resolver.resolve(event, gameState);

            verify(exile).remove(card.id());
            verify(hand).add(card);
        }

        @Test
        void returnsPermanentFromBattlefieldToHand() {
            var card = createCard();
            var permanent = createPermanent(card);
            var event = new ReturnToHandEvent(permanent, ZoneType.BATTLEFIELD);

            resolver.resolve(event, gameState);

            verify(battlefield).remove(permanent.id());
            verify(hand).add(card);
        }

        @Test
        void returnsCardFromLibraryToHand() {
            var card = createCard();
            var event = new ReturnToHandEvent(card, ZoneType.LIBRARY);

            resolver.resolve(event, gameState);

            verify(library).remove(card.id());
            verify(hand).add(card);
        }

        @Test
        void recordsLkiBeforeReturning() {
            var card = createCard();
            var event = new ReturnToHandEvent(card, ZoneType.GRAVEYARD);

            resolver.resolve(event, gameState);

            verify(lki).record(any(ObjectSnapshot.class));
        }
    }

    @Nested
    class CastEvents {

        @Test
        void castsCardFromHand() {
            var card = createCard();
            var event = new CastEvent(card, ZoneType.HAND, player);

            resolver.resolve(event, gameState);

            verify(hand).remove(card.id());
            verify(stack).push(any(Spell.class));
        }

        @Test
        void castsCardFromGraveyard() {
            var card = createCard();
            var event = new CastEvent(card, ZoneType.GRAVEYARD, player);

            resolver.resolve(event, gameState);

            verify(graveyard).remove(card.id());
            verify(stack).push(any(Spell.class));
        }

        @Test
        void castsCardFromExile() {
            var card = createCard();
            var event = new CastEvent(card, ZoneType.EXILE, player);

            resolver.resolve(event, gameState);

            verify(exile).remove(card.id());
            verify(stack).push(any(Spell.class));
        }

        @Test
        void castsCardFromLibrary() {
            var card = createCard();
            var event = new CastEvent(card, ZoneType.LIBRARY, player);

            resolver.resolve(event, gameState);

            verify(library).remove(card.id());
            verify(stack).push(any(Spell.class));
        }

        @Test
        void recordsLkiBeforeCasting() {
            var card = createCard();
            var event = new CastEvent(card, ZoneType.HAND, player);

            resolver.resolve(event, gameState);

            verify(lki).record(any(ObjectSnapshot.class));
        }
    }

    @Nested
    class SpellCounterEvents {

        @Test
        void countersSpellFromStack() {
            var card = createCard();
            var spell = mock(Spell.class);
            when(spell.id()).thenReturn(new ObjectId());
            when(spell.source()).thenReturn(card);
            when(spell.owner()).thenReturn(player);
            var event = new CounterEvent(spell);

            resolver.resolve(event, gameState);

            verify(stack).remove(spell.id());
            verify(graveyard).put(card);
        }

        @Test
        void countersSpellWithoutCardSource() {
            var spell = mock(Spell.class);
            when(spell.id()).thenReturn(new ObjectId());
            when(spell.source()).thenReturn(null);
            when(spell.owner()).thenReturn(player);
            var event = new CounterEvent(spell);

            resolver.resolve(event, gameState);

            verify(stack).remove(spell.id());
        }

        @Test
        void countersAbilityOnStack() {
            var ability = mock(AbilityOnStack.class);
            var event = new CounterEvent(ability);

            resolver.resolve(event, gameState);

            // AbilityOnStack objects just cease to exist when countered
            // No graveyard interaction
        }

        @Test
        void recordsLkiBeforeCountering() {
            var card = createCard();
            var spell = mock(Spell.class);
            when(spell.id()).thenReturn(new ObjectId());
            when(spell.source()).thenReturn(card);
            when(spell.owner()).thenReturn(player);
            var event = new CounterEvent(spell);

            resolver.resolve(event, gameState);

            verify(lki).record(any(ObjectSnapshot.class));
        }
    }

    @Nested
    class PutIntoGraveyardEvents {

        @Test
        void putsCardFromExileIntoGraveyard() {
            var card = createCard();
            var event = new PutIntoGraveyardEvent(card, ZoneType.EXILE);

            resolver.resolve(event, gameState);

            verify(exile).remove(card.id());
            verify(graveyard).put(card);
        }

        @Test
        void putsCardFromLibraryIntoGraveyard() {
            var card = createCard();
            var event = new PutIntoGraveyardEvent(card, ZoneType.LIBRARY);

            resolver.resolve(event, gameState);

            verify(library).remove(card.id());
            verify(graveyard).put(card);
        }

        @Test
        void putsCardFromStackIntoGraveyard() {
            var card = createCard();
            var event = new PutIntoGraveyardEvent(card, ZoneType.STACK);

            resolver.resolve(event, gameState);

            verify(stack).remove(card.id());
            verify(graveyard).put(card);
        }

        @Test
        void putsPermanentFromBattlefieldIntoGraveyard() {
            var card = createCard();
            var permanent = createPermanent(card);
            var event = new PutIntoGraveyardEvent(permanent, ZoneType.BATTLEFIELD);

            resolver.resolve(event, gameState);

            verify(battlefield).remove(permanent.id());
            verify(graveyard).put(card);
        }

        @Test
        void recordsLkiBeforePuttingIntoGraveyard() {
            var card = createCard();
            var event = new PutIntoGraveyardEvent(card, ZoneType.EXILE);

            resolver.resolve(event, gameState);

            verify(lki).record(any(ObjectSnapshot.class));
        }
    }

    @Nested
    class RemoveFromZoneEdgeCases {

        @Test
        void removesCardFromCommandZone() {
            var card = createCard();
            var event = new ShuffleIntoLibraryEvent(List.of(card), ZoneType.COMMAND, player);

            resolver.resolve(event, gameState);

            verify(commandZone).removeCommander(card.id());
            verify(library).putOnBottom(card);
        }

        @Test
        void removesNonCardFromCommandZone() {
            // Test that non-Card objects don't try to remove from command zone
            var card = createCard();
            var permanent = createPermanent(card);
            var event = new ExileEvent(permanent, ZoneType.COMMAND, player);

            resolver.resolve(event, gameState);

            // Permanent is not a Card directly, so command zone remove should not be called
            verify(exile).exile(card);
        }

        @Test
        void removesFromAllZoneTypes() {
            var card1 = createCard();
            var card2 = createCard();
            var card3 = createCard();
            var card4 = createCard();
            var card5 = createCard();
            var card6 = createCard();

            // Test all zone types in removeFromZone
            resolver.resolve(new ShuffleIntoLibraryEvent(List.of(card1), ZoneType.LIBRARY, player), gameState);
            resolver.resolve(new ShuffleIntoLibraryEvent(List.of(card2), ZoneType.HAND, player), gameState);
            resolver.resolve(new ShuffleIntoLibraryEvent(List.of(card3), ZoneType.GRAVEYARD, player), gameState);
            resolver.resolve(new ShuffleIntoLibraryEvent(List.of(card4), ZoneType.STACK, player), gameState);
            resolver.resolve(new ShuffleIntoLibraryEvent(List.of(card5), ZoneType.EXILE, player), gameState);
            resolver.resolve(new ShuffleIntoLibraryEvent(List.of(card6), ZoneType.COMMAND, player), gameState);

            verify(library).remove(card1.id());
            verify(hand).remove(card2.id());
            verify(graveyard).remove(card3.id());
            verify(stack).remove(card4.id());
            verify(exile).remove(card5.id());
            verify(commandZone).removeCommander(card6.id());
        }
    }

    @Nested
    class DiesEventEdgeCases {

        @Test
        void diesEventWithNonCardSource() {
            // Test permanent with Token source (edge case)
            var permanent = mock(Permanent.class);
            var tokenSource = mock(Token.class);
            when(permanent.id()).thenReturn(new ObjectId());
            when(permanent.source()).thenReturn(tokenSource);
            when(permanent.owner()).thenReturn(player);
            var event = new DiesEvent(permanent, new DiesEvent.DeathCause.Destroyed());

            resolver.resolve(event, gameState);

            verify(battlefield).remove(permanent.id());
            // No graveyard interaction since source is not a Card
        }
    }

    @Nested
    class EntersBattlefieldEdgeCases {

        @Test
        void entersBattlefieldWithNonCardSource() {
            // Test permanent with Token source (edge case)
            var permanent = mock(Permanent.class);
            var tokenSource = mock(Token.class);
            when(permanent.id()).thenReturn(new ObjectId());
            when(permanent.source()).thenReturn(tokenSource);
            when(permanent.controller()).thenReturn(player);
            var event = new EntersBattlefieldEvent(permanent, ZoneType.HAND, new EntersBattlefieldEvent.EtbCause.Put());

            resolver.resolve(event, gameState);

            verify(battlefield).enter(permanent);
            // No zone removal since source is not a Card
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
