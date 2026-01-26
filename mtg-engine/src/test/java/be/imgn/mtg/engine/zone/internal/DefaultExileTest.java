package be.imgn.mtg.engine.zone.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.ObjectId;

class DefaultExileTest {

    Player owner;
    DefaultExile exile;

    @BeforeEach
    void setUp() {
        owner = mock(Player.class);
        exile = new DefaultExile();
    }

    private Card createCard(String name) {
        return Card.builder().owner(owner).controller(owner).name(name).build();
    }

    @Nested
    class BasicOperations {

        @Test
        void emptyExile() {
            assertThat(exile.isEmpty()).isTrue();
            assertThat(exile.size()).isZero();
        }

        @Test
        void exileIncreasesSize() {
            var card = createCard("Card 1");

            exile.exile(card);

            assertThat(exile.size()).isEqualTo(1);
            assertThat(exile.isEmpty()).isFalse();
        }

        @Test
        void containsReturnsTrueForExiledCard() {
            var card = createCard("Card 1");
            exile.exile(card);

            assertThat(exile.contains(card.id())).isTrue();
        }

        @Test
        void containsReturnsFalseForAbsentCard() {
            assertThat(exile.contains(new ObjectId())).isFalse();
        }
    }

    @Nested
    class FaceDownOperations {

        @Test
        void exileFaceDownIncreasesSize() {
            var card = createCard("Card 1");

            exile.exileFaceDown(card);

            assertThat(exile.size()).isEqualTo(1);
        }

        @Test
        void isFaceDownReturnsTrueForFaceDownCard() {
            var card = createCard("Card 1");
            exile.exileFaceDown(card);

            assertThat(exile.isFaceDown(card.id())).isTrue();
        }

        @Test
        void isFaceDownReturnsFalseForFaceUpCard() {
            var card = createCard("Card 1");
            exile.exile(card);

            assertThat(exile.isFaceDown(card.id())).isFalse();
        }

        @Test
        void isFaceDownReturnsFalseForAbsentCard() {
            assertThat(exile.isFaceDown(new ObjectId())).isFalse();
        }

        @Test
        void faceUpExcludesFaceDownCards() {
            var faceUp1 = createCard("Face Up 1");
            var faceDown = createCard("Face Down");
            var faceUp2 = createCard("Face Up 2");

            exile.exile(faceUp1);
            exile.exileFaceDown(faceDown);
            exile.exile(faceUp2);

            assertThat(exile.faceUp()).containsExactlyInAnyOrder(faceUp1, faceUp2);
        }

        @Test
        void faceUpReturnsEmptyWhenAllFaceDown() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            exile.exileFaceDown(card1);
            exile.exileFaceDown(card2);

            assertThat(exile.faceUp()).isEmpty();
        }
    }

    @Nested
    class RemoveOperations {

        @Test
        void removeNonExistent() {
            var removed = exile.remove(new ObjectId());

            assertThat(removed).isEmpty();
        }

        @Test
        void removeExisting() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            exile.exile(card1);
            exile.exile(card2);

            var removed = exile.remove(card1.id());

            assertThat(removed).contains(card1);
            assertThat(exile.size()).isEqualTo(1);
        }

        @Test
        void removeFaceDownCardClearsFaceDownStatus() {
            var card = createCard("Card 1");
            exile.exileFaceDown(card);

            exile.remove(card.id());

            // If re-exiled (hypothetically), it should not be face down
            assertThat(exile.isFaceDown(card.id())).isFalse();
        }
    }

    @Nested
    class FindByIdOperations {

        @Test
        void findByIdReturnsEmptyForAbsentCard() {
            assertThat(exile.findById(new ObjectId())).isEmpty();
        }

        @Test
        void findByIdReturnsPresentCard() {
            var card = createCard("Card 1");
            exile.exile(card);

            assertThat(exile.findById(card.id())).contains(card);
        }
    }

    @Nested
    class AllOperations {

        @Test
        void allReturnsEmptyForEmptyExile() {
            assertThat(exile.all()).isEmpty();
        }

        @Test
        void allReturnsAllCards() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            exile.exile(card1);
            exile.exileFaceDown(card2);

            assertThat(exile.all()).containsExactlyInAnyOrder(card1, card2);
        }

        @Test
        void allReturnsCopy() {
            var card = createCard("Card 1");
            exile.exile(card);

            var all = exile.all();

            try {
                all.clear();
            } catch (UnsupportedOperationException e) {
                // Expected for immutable copy
            }
            assertThat(exile.size()).isEqualTo(1);
        }
    }

    @Nested
    class StreamOperations {

        @Test
        void streamReturnsAllCards() {
            var card1 = createCard("Card 1");
            var card2 = createCard("Card 2");
            exile.exile(card1);
            exile.exileFaceDown(card2);

            var cards = exile.stream().toList();

            assertThat(cards).containsExactlyInAnyOrder(card1, card2);
        }

        @Test
        void streamOnEmptyExile() {
            assertThat(exile.stream().toList()).isEmpty();
        }
    }
}
