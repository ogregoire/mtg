package be.imgn.mtg.engine.zone.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.state.internal.ObjectStore;

class DefaultHandTest {

    Player owner;
    DefaultHand hand;

    @BeforeEach
    void setUp() {
        owner = mock(Player.class);
        hand = new DefaultHand(new ObjectStore(), owner);
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

            assertThat(hand.contains(card)).isTrue();
        }

        @Test
        void containsReturnsFalseForAbsentCard() {
            var card = createCard("Not Added");
            assertThat(hand.contains(card)).isFalse();
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
            assertThat(hand.contains(card1)).isTrue();
            assertThat(hand.contains(card2)).isTrue();
            assertThat(hand.contains(card3)).isTrue();
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
            var card = createCard("Not Added");
            var removed = hand.remove(card);

            assertThat(removed).isFalse();
        }

        @Test
        void removeExisting() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            hand.add(card1);
            hand.add(card2);

            var removed = hand.remove(card1);

            assertThat(removed).isTrue();
            assertThat(hand.size()).isEqualTo(1);
            assertThat(hand.contains(card1)).isFalse();
            assertThat(hand.contains(card2)).isTrue();
        }

        @Test
        void removeFromEmptyHand() {
            var card = createCard("Not Added");
            var removed = hand.remove(card);

            assertThat(removed).isFalse();
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
