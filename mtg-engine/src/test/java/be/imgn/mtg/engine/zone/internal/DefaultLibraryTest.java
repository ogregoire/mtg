package be.imgn.mtg.engine.zone.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;

class DefaultLibraryTest {

    Player owner;
    DefaultLibrary library;

    @BeforeEach
    void setUp() {
        owner = mock(Player.class);
        library = new DefaultLibrary(owner);
    }

    private Card createCard(String name) {
        return Card.builder().owner(owner).controller(owner).name(name).build();
    }

    @Nested
    class BasicOperations {

        @Test
        void ownerIsSet() {
            assertThat(library.owner()).isSameAs(owner);
        }

        @Test
        void emptyLibrary() {
            assertThat(library.isEmpty()).isTrue();
            assertThat(library.size()).isZero();
        }

        @Test
        void putOnTopIncreasesSize() {
            var card = createCard("Card 1");

            library.putOnTop(card);

            assertThat(library.size()).isEqualTo(1);
            assertThat(library.isEmpty()).isFalse();
        }

        @Test
        void putOnBottomIncreasesSize() {
            var card = createCard("Card 1");

            library.putOnBottom(card);

            assertThat(library.size()).isEqualTo(1);
        }
    }

    @Nested
    class PeekOperations {

        @Test
        void peekTopOnEmpty() {
            assertThat(library.peekTop()).isEmpty();
        }

        @Test
        void peekTopReturnsTopCard() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            library.putOnTop(card1);
            library.putOnTop(card2);

            assertThat(library.peekTop()).contains(card2);
        }

        @Test
        void peekTopDoesNotRemove() {
            var card = createCard("Card 1");
            library.putOnTop(card);

            library.peekTop();

            assertThat(library.size()).isEqualTo(1);
        }

        @Test
        void peekTopCountZero() {
            var card = createCard("Card 1");
            library.putOnTop(card);

            assertThat(library.peekTop(0)).isEmpty();
        }

        @Test
        void peekTopCountNegative() {
            var card = createCard("Card 1");
            library.putOnTop(card);

            assertThat(library.peekTop(-1)).isEmpty();
        }

        @Test
        void peekTopCountMoreThanAvailable() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            library.putOnTop(card1);
            library.putOnTop(card2);

            var result = library.peekTop(5);

            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(card2, card1);
        }

        @Test
        void peekTopCountExact() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            var card3 = createCard("Card 3");
            library.putOnTop(card1);
            library.putOnTop(card2);
            library.putOnTop(card3);

            var result = library.peekTop(2);

            assertThat(result).containsExactly(card3, card2);
        }

        @Test
        void peekBottomOnEmpty() {
            assertThat(library.peekBottom()).isEmpty();
        }

        @Test
        void peekBottomReturnsBottomCard() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            library.putOnTop(card1);
            library.putOnTop(card2);

            assertThat(library.peekBottom()).contains(card1);
        }
    }

    @Nested
    class DrawOperations {

        @Test
        void drawTopOnEmpty() {
            assertThat(library.drawTop()).isEmpty();
        }

        @Test
        void drawTopRemovesCard() {
            var card = createCard("Card 1");
            library.putOnTop(card);

            var drawn = library.drawTop();

            assertThat(drawn).contains(card);
            assertThat(library.isEmpty()).isTrue();
        }

        @Test
        void drawTopReturnsTopCard() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            library.putOnTop(card1);
            library.putOnTop(card2);

            var drawn = library.drawTop();

            assertThat(drawn).contains(card2);
            assertThat(library.peekTop()).contains(card1);
        }

        @Test
        void drawTopCountZero() {
            var card = createCard("Card 1");
            library.putOnTop(card);

            assertThat(library.drawTop(0)).isEmpty();
            assertThat(library.size()).isEqualTo(1);
        }

        @Test
        void drawTopCountOnEmpty() {
            assertThat(library.drawTop(3)).isEmpty();
        }

        @Test
        void drawTopCountMoreThanAvailable() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            library.putOnTop(card1);
            library.putOnTop(card2);

            var drawn = library.drawTop(5);

            assertThat(drawn).hasSize(2);
            assertThat(library.isEmpty()).isTrue();
        }

        @Test
        void drawTopCountExact() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            var card3 = createCard("Card 3");
            library.putOnTop(card1);
            library.putOnTop(card2);
            library.putOnTop(card3);

            var drawn = library.drawTop(2);

            assertThat(drawn).containsExactly(card3, card2);
            assertThat(library.size()).isEqualTo(1);
        }
    }

    @Nested
    class PutOperations {

        @Test
        void putOnTopList() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");

            library.putOnTop(List.of(card1, card2));

            // First card in list should be on top
            assertThat(library.peekTop()).contains(card1);
        }

        @Test
        void putOnBottomList() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            library.putOnTop(createCard("Existing"));

            library.putOnBottom(List.of(card1, card2));

            // First card added should be closer to bottom
            assertThat(library.peekBottom()).contains(card2);
        }
    }

    @Nested
    class RemoveOperations {

        @Test
        void removeNonExistent() {
            var card = createCard("Card 1");

            var removed = library.remove(card);

            assertThat(removed).isFalse();
        }

        @Test
        void removeExisting() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            library.putOnTop(card1);
            library.putOnTop(card2);

            var removed = library.remove(card1);

            assertThat(removed).isTrue();
            assertThat(library.size()).isEqualTo(1);
        }
    }

    @Nested
    class ShuffleOperations {

        @Test
        void shuffleDoesNotChangeSize() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            var card3 = createCard("Card 3");
            library.putOnTop(card1);
            library.putOnTop(card2);
            library.putOnTop(card3);

            library.shuffle();

            assertThat(library.size()).isEqualTo(3);
        }
    }

    @Nested
    class SearchOperations {

        @Test
        void searchFindsMatchingCards() {
            var card1 = createCard("Lightning Bolt");
            var card2 = createCard("Mountain");
            var card3 = createCard("Lightning Strike");
            library.putOnTop(card1);
            library.putOnTop(card2);
            library.putOnTop(card3);

            var found = library.search(c -> c.name().startsWith("Lightning"));

            assertThat(found).containsExactlyInAnyOrder(card1, card3);
        }

        @Test
        void searchReturnsEmptyWhenNoMatch() {
            var card = createCard("Mountain");
            library.putOnTop(card);

            var found = library.search(c -> c.name().startsWith("Island"));

            assertThat(found).isEmpty();
        }
    }

    @Nested
    class StreamOperations {

        @Test
        void streamReturnsAllCards() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            library.putOnTop(card1);
            library.putOnTop(card2);

            var cards = library.stream().toList();

            assertThat(cards).containsExactlyInAnyOrder(card1, card2);
        }
    }
}
