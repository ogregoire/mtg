package be.imgn.mtg.engine.zone.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.ObjectId;

class DefaultHandTest {

    Player owner;
    DefaultHand hand;

    @BeforeEach
    void setUp() {
        owner = mock(Player.class);
        hand = new DefaultHand(owner);
    }

    private Card createCard(String name) {
        return Card.builder().owner(owner).controller(owner).name(name).build();
    }

    @Nested
    class BasicOperations {

        @Test
        void ownerIsSet() {
            assertThat(hand.owner()).isSameAs(owner);
        }

        @Test
        void emptyHand() {
            assertThat(hand.isEmpty()).isTrue();
            assertThat(hand.size()).isZero();
        }

        @Test
        void addIncreasesSize() {
            var card = createCard("Card 1");

            hand.add(card);

            assertThat(hand.size()).isEqualTo(1);
            assertThat(hand.isEmpty()).isFalse();
        }

        @Test
        void containsReturnsTrueForPresentCard() {
            var card = createCard("Card 1");
            hand.add(card);

            assertThat(hand.contains(card.id())).isTrue();
        }

        @Test
        void containsReturnsFalseForAbsentCard() {
            assertThat(hand.contains(new ObjectId())).isFalse();
        }
    }

    @Nested
    class AddAllOperations {

        @Test
        void addAllEmptyList() {
            hand.addAll(List.of());

            assertThat(hand.isEmpty()).isTrue();
        }

        @Test
        void addAllMultipleCards() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            var card3 = createCard("Card 3");

            hand.addAll(List.of(card1, card2, card3));

            assertThat(hand.size()).isEqualTo(3);
            assertThat(hand.contains(card1.id())).isTrue();
            assertThat(hand.contains(card2.id())).isTrue();
            assertThat(hand.contains(card3.id())).isTrue();
        }

        @Test
        void addAllToNonEmptyHand() {
            var existing = createCard("Existing");
            hand.add(existing);

            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            hand.addAll(List.of(card1, card2));

            assertThat(hand.size()).isEqualTo(3);
        }
    }

    @Nested
    class RemoveOperations {

        @Test
        void removeNonExistent() {
            var removed = hand.remove(new ObjectId());

            assertThat(removed).isEmpty();
        }

        @Test
        void removeExisting() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            hand.add(card1);
            hand.add(card2);

            var removed = hand.remove(card1.id());

            assertThat(removed).contains(card1);
            assertThat(hand.size()).isEqualTo(1);
            assertThat(hand.contains(card1.id())).isFalse();
            assertThat(hand.contains(card2.id())).isTrue();
        }

        @Test
        void removeFromEmptyHand() {
            var removed = hand.remove(new ObjectId());

            assertThat(removed).isEmpty();
        }
    }

    @Nested
    class FindByIdOperations {

        @Test
        void findByIdReturnsEmptyForAbsentCard() {
            assertThat(hand.findById(new ObjectId())).isEmpty();
        }

        @Test
        void findByIdReturnsPresentCard() {
            var card = createCard("Card 1");
            hand.add(card);

            assertThat(hand.findById(card.id())).contains(card);
        }

        @Test
        void findByIdAfterRemove() {
            var card = createCard("Card 1");
            hand.add(card);
            hand.remove(card.id());

            assertThat(hand.findById(card.id())).isEmpty();
        }
    }

    @Nested
    class CardsOperations {

        @Test
        void cardsReturnsEmptyListForEmptyHand() {
            assertThat(hand.cards()).isEmpty();
        }

        @Test
        void cardsReturnsAllCards() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            hand.add(card1);
            hand.add(card2);

            assertThat(hand.cards()).containsExactlyInAnyOrder(card1, card2);
        }

        @Test
        void cardsReturnsCopy() {
            var card = createCard("Card 1");
            hand.add(card);

            var cards = hand.cards();

            // Original hand unchanged by modification attempt
            try {
                cards.clear();
            } catch (UnsupportedOperationException e) {
                // Expected for immutable copy
            }
            assertThat(hand.size()).isEqualTo(1);
        }
    }

    @Nested
    class StreamOperations {

        @Test
        void streamReturnsAllCards() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            hand.add(card1);
            hand.add(card2);

            var cards = hand.stream().toList();

            assertThat(cards).containsExactlyInAnyOrder(card1, card2);
        }

        @Test
        void streamOnEmptyHand() {
            assertThat(hand.stream().toList()).isEmpty();
        }
    }
}
