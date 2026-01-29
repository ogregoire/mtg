package be.imgn.mtg.engine.state.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.state.LastKnownInformation;
import be.imgn.mtg.engine.state.ObjectSnapshot;
import be.imgn.mtg.engine.zone.ZoneType;

class DefaultLastKnownInformationTest {

    private LastKnownInformation lki;
    private Player player;
    private Card card;
    private ObjectSnapshot snapshot;

    @BeforeEach
    void setUp() {
        lki = new DefaultLastKnownInformation();
        player = mock(Player.class);
        card = Card.builder().owner(player).controller(player).name("Test Card").build();
        snapshot = ObjectSnapshot.of(card, ZoneType.BATTLEFIELD);
    }

    @Nested
    class RecordSnapshot {

        @Test
        void recordsSnapshot() {
            lki.record(card, snapshot);

            var retrieved = lki.get(card);

            assertThat(retrieved).isPresent();
            assertThat(retrieved.get()).isEqualTo(snapshot);
        }

        @Test
        void overwritesExistingSnapshot() {
            lki.record(card, snapshot);

            var newSnapshot = ObjectSnapshot.of(card, ZoneType.GRAVEYARD);
            lki.record(card, newSnapshot);

            var retrieved = lki.get(card);

            assertThat(retrieved).isPresent();
            assertThat(retrieved.get()).isEqualTo(newSnapshot);
            assertThat(retrieved.get().zone()).isEqualTo(ZoneType.GRAVEYARD);
        }

        @Test
        void recordsMultipleSnapshots() {
            var card2 = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Card 2")
                    .build();
            var snapshot2 = ObjectSnapshot.of(card2, ZoneType.HAND);

            lki.record(card, snapshot);
            lki.record(card2, snapshot2);

            assertThat(lki.get(card)).isPresent();
            assertThat(lki.get(card2)).isPresent();
        }
    }

    @Nested
    class GetSnapshot {

        @Test
        void returnsEmptyWhenNotRecorded() {
            var result = lki.get(card);

            assertThat(result).isEmpty();
        }

        @Test
        void returnsEmptyForUnknownObject() {
            lki.record(card, snapshot);

            var unknownObject = mock(Card.class);
            var result = lki.get(unknownObject);

            assertThat(result).isEmpty();
        }

        @Test
        void returnsRecordedSnapshot() {
            lki.record(card, snapshot);

            var result = lki.get(card);

            assertThat(result).isPresent();
            assertThat(result.get().name()).isEqualTo("Test Card");
        }

        @Test
        void returnsCorrectSnapshotAmongMultiple() {
            var card2 = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Card 2")
                    .build();
            var snapshot2 = ObjectSnapshot.of(card2, ZoneType.EXILE);

            lki.record(card, snapshot);
            lki.record(card2, snapshot2);

            var result = lki.get(card2);

            assertThat(result).isPresent();
            assertThat(result.get().name()).isEqualTo("Card 2");
            assertThat(result.get().zone()).isEqualTo(ZoneType.EXILE);
        }
    }

    @Nested
    class ClearAll {

        @Test
        void clearsAllSnapshots() {
            lki.record(card, snapshot);

            lki.clear();

            assertThat(lki.get(card)).isEmpty();
        }

        @Test
        void clearingEmptyLkiDoesNothing() {
            lki.clear();

            assertThat(lki.get(card)).isEmpty();
        }

        @Test
        void clearsMultipleSnapshots() {
            var card2 = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Card 2")
                    .build();
            var snapshot2 = ObjectSnapshot.of(card2, ZoneType.HAND);

            lki.record(card, snapshot);
            lki.record(card2, snapshot2);

            lki.clear();

            assertThat(lki.get(card)).isEmpty();
            assertThat(lki.get(card2)).isEmpty();
        }

        @Test
        void canRecordAfterClearing() {
            lki.record(card, snapshot);
            lki.clear();

            lki.record(card, snapshot);

            assertThat(lki.get(card)).isPresent();
        }
    }

    @Nested
    class ClearSpecific {

        @Test
        void clearsSpecificSnapshot() {
            lki.record(card, snapshot);

            lki.clear(card);

            assertThat(lki.get(card)).isEmpty();
        }

        @Test
        void clearingNonExistentObjectDoesNothing() {
            var unknownObject = mock(Card.class);

            lki.clear(unknownObject);

            // Should not throw
        }

        @Test
        void clearsOnlySpecifiedSnapshot() {
            var card2 = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Card 2")
                    .build();
            var snapshot2 = ObjectSnapshot.of(card2, ZoneType.LIBRARY);

            lki.record(card, snapshot);
            lki.record(card2, snapshot2);

            lki.clear(card);

            assertThat(lki.get(card)).isEmpty();
            assertThat(lki.get(card2)).isPresent();
        }

        @Test
        void canRecordAfterClearingSpecific() {
            lki.record(card, snapshot);
            lki.clear(card);

            lki.record(card, snapshot);

            assertThat(lki.get(card)).isPresent();
        }
    }
}
