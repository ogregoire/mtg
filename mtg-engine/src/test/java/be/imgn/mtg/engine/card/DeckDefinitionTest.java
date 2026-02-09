package be.imgn.mtg.engine.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DeckDefinitionTest {

    private static CardDefinition card(String name) {
        return CardDefinition.builder(UUID.randomUUID(), name, CardLayout.NORMAL)
                .build();
    }

    @Nested
    class MainDeck {

        @Test
        void emptyDeckHasSizeZero() {
            var deck = DeckDefinition.builder().build();

            assertThat(deck.size()).isZero();
        }

        @Test
        void addSingleCard() {
            var bolt = card("Lightning Bolt");
            var deck = DeckDefinition.builder().add(bolt).build();

            assertThat(deck.size()).isEqualTo(1);
            assertThat(deck.count(bolt)).isEqualTo(1);
        }

        @Test
        void addMultipleCopies() {
            var bolt = card("Lightning Bolt");
            var deck = DeckDefinition.builder().add(bolt, 4).build();

            assertThat(deck.size()).isEqualTo(4);
            assertThat(deck.count(bolt)).isEqualTo(4);
        }

        @Test
        void addDifferentCards() {
            var bolt = card("Lightning Bolt");
            var island = card("Island");
            var deck = DeckDefinition.builder().add(bolt, 4).add(island, 20).build();

            assertThat(deck.size()).isEqualTo(24);
            assertThat(deck.count(bolt)).isEqualTo(4);
            assertThat(deck.count(island)).isEqualTo(20);
        }

        @Test
        void countReturnsZeroForAbsentCard() {
            var bolt = card("Lightning Bolt");
            var island = card("Island");
            var deck = DeckDefinition.builder().add(bolt).build();

            assertThat(deck.count(island)).isZero();
        }
    }

    @Nested
    class Sideboard {

        @Test
        void emptySideboardByDefault() {
            var deck = DeckDefinition.builder().add(card("Lightning Bolt")).build();

            assertThat(deck.sideboard()).isEmpty();
        }

        @Test
        void addSingleSideboardCard() {
            var bolt = card("Lightning Bolt");
            var deck = DeckDefinition.builder().addSideboard(bolt).build();

            assertThat(deck.sideboard().count(bolt)).isEqualTo(1);
        }

        @Test
        void addMultipleSideboardCopies() {
            var bolt = card("Lightning Bolt");
            var deck = DeckDefinition.builder().addSideboard(bolt, 3).build();

            assertThat(deck.sideboard().count(bolt)).isEqualTo(3);
        }

        @Test
        void sideboardIsSeparateFromMainDeck() {
            var bolt = card("Lightning Bolt");
            var island = card("Island");
            var deck = DeckDefinition.builder()
                    .add(bolt, 4)
                    .addSideboard(island, 2)
                    .build();

            assertThat(deck.count(bolt)).isEqualTo(4);
            assertThat(deck.count(island)).isZero();
            assertThat(deck.sideboard().count(island)).isEqualTo(2);
            assertThat(deck.sideboard().count(bolt)).isZero();
        }
    }

    @Nested
    class CountWithSideboard {

        @Test
        void returnsZeroForAbsentCard() {
            var bolt = card("Lightning Bolt");
            var deck = DeckDefinition.builder().build();

            assertThat(deck.countWithSideboard(bolt)).isZero();
        }

        @Test
        void sumsMainDeckAndSideboard() {
            var bolt = card("Lightning Bolt");
            var deck =
                    DeckDefinition.builder().add(bolt, 4).addSideboard(bolt, 3).build();

            assertThat(deck.countWithSideboard(bolt)).isEqualTo(7);
        }

        @Test
        void countsOnlyMainDeckWhenNotInSideboard() {
            var bolt = card("Lightning Bolt");
            var deck = DeckDefinition.builder().add(bolt, 4).build();

            assertThat(deck.countWithSideboard(bolt)).isEqualTo(4);
        }

        @Test
        void countsOnlySideboardWhenNotInMainDeck() {
            var bolt = card("Lightning Bolt");
            var deck = DeckDefinition.builder().addSideboard(bolt, 2).build();

            assertThat(deck.countWithSideboard(bolt)).isEqualTo(2);
        }
    }

    @Nested
    class UniqueCards {

        @Test
        void emptyDeckHasNoUniqueCards() {
            var deck = DeckDefinition.builder().build();

            assertThat(deck.uniqueCards()).isEmpty();
        }

        @Test
        void returnsDistinctCardsFromMainDeck() {
            var bolt = card("Lightning Bolt");
            var island = card("Island");
            var deck = DeckDefinition.builder().add(bolt, 4).add(island, 20).build();

            assertThat(deck.uniqueCards()).containsExactlyInAnyOrder(bolt, island);
        }

        @Test
        void includesCardsFromSideboard() {
            var bolt = card("Lightning Bolt");
            var island = card("Island");
            var deck = DeckDefinition.builder()
                    .add(bolt, 4)
                    .addSideboard(island, 2)
                    .build();

            assertThat(deck.uniqueCards()).containsExactlyInAnyOrder(bolt, island);
        }

        @Test
        void deduplicatesCardsInBothMainAndSideboard() {
            var bolt = card("Lightning Bolt");
            var deck =
                    DeckDefinition.builder().add(bolt, 4).addSideboard(bolt, 1).build();

            assertThat(deck.uniqueCards()).containsExactly(bolt);
        }

        @Test
        void uniqueCardsSetIsImmutable() {
            var deck = DeckDefinition.builder().add(card("Lightning Bolt")).build();

            assertThat(deck.uniqueCards()).isUnmodifiable();
        }
    }

    @Nested
    class Immutability {

        @Test
        void cardsMultisetIsImmutable() {
            var deck = DeckDefinition.builder().add(card("Lightning Bolt")).build();

            assertThatThrownBy(() -> deck.cards().add(card("Island")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void sideboardMultisetIsImmutable() {
            var deck = DeckDefinition.builder()
                    .addSideboard(card("Lightning Bolt"))
                    .build();

            assertThatThrownBy(() -> deck.sideboard().add(card("Island")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
