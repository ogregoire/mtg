package be.imgn.mtg.engine.state.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.ObjectId;
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
            lki.record(snapshot);

            var retrieved = lki.get(card.id());

            assertThat(retrieved).isPresent();
            assertThat(retrieved.get()).isEqualTo(snapshot);
        }

        @Test
        void overwritesExistingSnapshot() {
            lki.record(snapshot);

            var newSnapshot = ObjectSnapshot.of(card, ZoneType.GRAVEYARD);
            lki.record(newSnapshot);

            var retrieved = lki.get(card.id());

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

            lki.record(snapshot);
            lki.record(snapshot2);

            assertThat(lki.get(card.id())).isPresent();
            assertThat(lki.get(card2.id())).isPresent();
        }
    }

    @Nested
    class GetSnapshot {

        @Test
        void returnsEmptyWhenNotRecorded() {
            var result = lki.get(card.id());

            assertThat(result).isEmpty();
        }

        @Test
        void returnsEmptyForUnknownId() {
            lki.record(snapshot);

            var unknownId = new ObjectId();
            var result = lki.get(unknownId);

            assertThat(result).isEmpty();
        }

        @Test
        void returnsRecordedSnapshot() {
            lki.record(snapshot);

            var result = lki.get(card.id());

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(card.id());
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

            lki.record(snapshot);
            lki.record(snapshot2);

            var result = lki.get(card2.id());

            assertThat(result).isPresent();
            assertThat(result.get().name()).isEqualTo("Card 2");
            assertThat(result.get().zone()).isEqualTo(ZoneType.EXILE);
        }
    }

    @Nested
    class ClearAll {

        @Test
        void clearsAllSnapshots() {
            lki.record(snapshot);

            lki.clear();

            assertThat(lki.get(card.id())).isEmpty();
        }

        @Test
        void clearingEmptyLkiDoesNothing() {
            lki.clear();

            assertThat(lki.get(card.id())).isEmpty();
        }

        @Test
        void clearsMultipleSnapshots() {
            var card2 = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Card 2")
                    .build();
            var snapshot2 = ObjectSnapshot.of(card2, ZoneType.HAND);

            lki.record(snapshot);
            lki.record(snapshot2);

            lki.clear();

            assertThat(lki.get(card.id())).isEmpty();
            assertThat(lki.get(card2.id())).isEmpty();
        }

        @Test
        void canRecordAfterClearing() {
            lki.record(snapshot);
            lki.clear();

            lki.record(snapshot);

            assertThat(lki.get(card.id())).isPresent();
        }
    }

    @Nested
    class ClearSpecific {

        @Test
        void clearsSpecificSnapshot() {
            lki.record(snapshot);

            lki.clear(card.id());

            assertThat(lki.get(card.id())).isEmpty();
        }

        @Test
        void clearingNonExistentIdDoesNothing() {
            var unknownId = new ObjectId();

            lki.clear(unknownId);

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

            lki.record(snapshot);
            lki.record(snapshot2);

            lki.clear(card.id());

            assertThat(lki.get(card.id())).isEmpty();
            assertThat(lki.get(card2.id())).isPresent();
        }

        @Test
        void canRecordAfterClearingSpecific() {
            lki.record(snapshot);
            lki.clear(card.id());

            lki.record(snapshot);

            assertThat(lki.get(card.id())).isPresent();
        }
    }
}
