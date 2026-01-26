package be.imgn.mtg.engine.object;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ObjectIdTest {

    @Nested
    class Constructor {

        @Test
        void generatesUniqueIdWhenNoParameterProvided() {
            var id1 = new ObjectId();
            var id2 = new ObjectId();

            assertThat(id1).isNotEqualTo(id2);
            assertThat(id1.value()).isNotEqualTo(id2.value());
        }

        @Test
        void acceptsProvidedUUID() {
            var uuid = UUID.randomUUID();
            var objectId = new ObjectId(uuid);

            assertThat(objectId.value()).isEqualTo(uuid);
        }

        @Test
        void multipleInstancesGenerateUniqueIds() {
            var ids = new HashSet<ObjectId>();
            for (int i = 0; i < 100; i++) {
                ids.add(new ObjectId());
            }

            assertThat(ids).hasSize(100);
        }
    }

    @Nested
    class Equality {

        @Test
        void equalWhenSameUUID() {
            var uuid = UUID.randomUUID();
            var id1 = new ObjectId(uuid);
            var id2 = new ObjectId(uuid);

            assertThat(id1).isEqualTo(id2);
            assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }

        @Test
        void notEqualWhenDifferentUUID() {
            var id1 = new ObjectId(UUID.randomUUID());
            var id2 = new ObjectId(UUID.randomUUID());

            assertThat(id1).isNotEqualTo(id2);
        }

        @Test
        void canBeUsedInHashSet() {
            var uuid = UUID.randomUUID();
            var id1 = new ObjectId(uuid);
            var id2 = new ObjectId(uuid);

            var set = new HashSet<ObjectId>();
            set.add(id1);
            set.add(id2);

            assertThat(set).hasSize(1).contains(id1);
        }
    }

    @Nested
    class ToStringMethod {

        @Test
        void returnsUUIDString() {
            var uuid = UUID.randomUUID();
            var objectId = new ObjectId(uuid);

            assertThat(objectId.toString()).isEqualTo(uuid.toString());
        }

        @Test
        void producesValidUUIDFormat() {
            var objectId = new ObjectId();

            assertThat(objectId.toString()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }
    }

    @Nested
    class UUIDv7Properties {

        @Test
        void usesUUIDv7ForTimeOrdering() {
            var id1 = new ObjectId();
            var id2 = new ObjectId();

            // UUIDv7 should have version bits set to 0111 (7)
            var version1 = (id1.value().getMostSignificantBits() >> 12) & 0xF;
            var version2 = (id2.value().getMostSignificantBits() >> 12) & 0xF;

            assertThat(version1).isEqualTo(7);
            assertThat(version2).isEqualTo(7);
        }
    }
}
