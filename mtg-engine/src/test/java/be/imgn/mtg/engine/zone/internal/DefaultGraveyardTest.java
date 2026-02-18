package be.imgn.mtg.engine.zone.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.state.internal.ObjectStore;

class DefaultGraveyardTest {

    Player owner;
    DefaultGraveyard graveyard;

    @BeforeEach
    void setUp() {
        owner = mock(Player.class);
        graveyard = new DefaultGraveyard(new ObjectStore(), owner);
    }

    private Card createCard(String name) {
        return Card.builder().owner(owner).controller(owner).name(name).build();
    }

    @Nested
    class BasicOperations {

        @Test
        void ownerIsSet() {
            assertThat(graveyard.owner()).isSameAs(owner);
        }

        @Test
        void emptyGraveyard() {
            assertThat(graveyard.isEmpty()).isTrue();
            assertThat(graveyard.size()).isZero();
        }

        @Test
        void putIncreasesSize() {
            var card = createCard("Card 1");

            graveyard.put(card);

            assertThat(graveyard.size()).isEqualTo(1);
            assertThat(graveyard.isEmpty()).isFalse();
        }

        @Test
        void containsReturnsTrueForPresentCard() {
            var card = createCard("Card 1");
            graveyard.put(card);

            assertThat(graveyard.contains(card)).isTrue();
        }

        @Test
        void containsReturnsFalseForAbsentCard() {
            var card = createCard("Not Added");
            assertThat(graveyard.contains(card)).isFalse();
        }
    }

    @Nested
    class PeekOperations {

        @Test
        void peekTopOnEmpty() {
            assertThat(graveyard.peekTop()).isEmpty();
        }

        @Test
        void peekTopReturnsMostRecentCard() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            graveyard.put(card1);
            graveyard.put(card2);

            assertThat(graveyard.peekTop()).contains(card2);
        }

        @Test
        void peekTopDoesNotRemove() {
            var card = createCard("Card 1");
            graveyard.put(card);

            graveyard.peekTop();

            assertThat(graveyard.size()).isEqualTo(1);
        }
    }

    @Nested
    class RemoveOperations {

        @Test
        void removeNonExistent() {
            var card = createCard("Not Added");
            var removed = graveyard.remove(card);

            assertThat(removed).isFalse();
        }

        @Test
        void removeExisting() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            graveyard.put(card1);
            graveyard.put(card2);

            var removed = graveyard.remove(card1);

            assertThat(removed).isTrue();
            assertThat(graveyard.size()).isEqualTo(1);
        }

        @Test
        void removeUpdatesTopCard() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            graveyard.put(card1);
            graveyard.put(card2);

            graveyard.remove(card2);

            assertThat(graveyard.peekTop()).contains(card1);
        }
    }

    @Nested
    class CardsOperations {

        @Test
        void cardsReturnsEmptyListForEmptyGraveyard() {
            assertThat(graveyard.cards()).isEmpty();
        }

        @Test
        void cardsReturnsAllCardsInOrder() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            var card3 = createCard("Card 3");
            graveyard.put(card1);
            graveyard.put(card2);
            graveyard.put(card3);

            // Most recent (card3) should be first
            assertThat(graveyard.cards()).containsExactly(card3, card2, card1);
        }

        @Test
        void cardsReturnsCopy() {
            var card = createCard("Card 1");
            graveyard.put(card);

            var cards = graveyard.cards();

            // Original graveyard unchanged by modification attempt
            try {
                cards.clear();
            } catch (UnsupportedOperationException e) {
                // Expected for immutable copy
            }
            assertThat(graveyard.size()).isEqualTo(1);
        }
    }

    @Nested
    class StreamOperations {

        @Test
        void streamReturnsAllCards() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            graveyard.put(card1);
            graveyard.put(card2);

            var cards = graveyard.stream().toList();

            assertThat(cards).containsExactlyInAnyOrder(card1, card2);
        }

        @Test
        void streamOnEmptyGraveyard() {
            assertThat(graveyard.stream().toList()).isEmpty();
        }
    }
}
