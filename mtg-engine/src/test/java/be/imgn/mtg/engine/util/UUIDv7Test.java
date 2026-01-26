package be.imgn.mtg.engine.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UUIDv7Test {

    @Nested
    class RandomUUID {

        @Test
        void generatesValidUUID() {
            var uuid = UUIDv7.randomUUID();

            assertThat(uuid).isNotNull();
        }

        @Test
        void hasVersion7() {
            var uuid = UUIDv7.randomUUID();

            // Version is stored in bits 12-15 of the msb (4 bits after 48-bit timestamp)
            assertThat(uuid.version()).isEqualTo(7);
        }

        @Test
        void hasVariant2() {
            var uuid = UUIDv7.randomUUID();

            // RFC 4122 variant (bits 64-65 = 10)
            assertThat(uuid.variant()).isEqualTo(2);
        }

        @Test
        void generatesUniqueUUIDs() {
            var uuids = new HashSet<UUID>();
            for (int i = 0; i < 1000; i++) {
                uuids.add(UUIDv7.randomUUID());
            }

            assertThat(uuids).hasSize(1000);
        }

        @Test
        void generatesTimeOrderedUUIDs() {
            var uuid1 = UUIDv7.randomUUID();

            // Small delay to ensure different timestamp
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            var uuid2 = UUIDv7.randomUUID();

            // UUIDs should be time-ordered (uuid1 < uuid2 when comparing as strings)
            // The first 48 bits are the timestamp, so comparing MSB works
            assertThat(uuid1.getMostSignificantBits()).isLessThanOrEqualTo(uuid2.getMostSignificantBits());
        }

        @Test
        void timestampIsRecent() {
            var before = System.currentTimeMillis();
            var uuid = UUIDv7.randomUUID();
            var after = System.currentTimeMillis();

            // Extract timestamp from MSB (first 48 bits)
            var timestamp = uuid.getMostSignificantBits() >>> 16;

            assertThat(timestamp).isBetween(before, after);
        }

        @Test
        void hasCorrectStringFormat() {
            var uuid = UUIDv7.randomUUID();
            var str = uuid.toString();

            // UUID string format: xxxxxxxx-xxxx-7xxx-yxxx-xxxxxxxxxxxx
            // where 7 is the version and y is 8, 9, a, or b (variant)
            assertThat(str).matches("[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
        }

        @Test
        void canBeConvertedToAndFromString() {
            var original = UUIDv7.randomUUID();
            var str = original.toString();
            var parsed = UUID.fromString(str);

            assertThat(parsed).isEqualTo(original);
        }
    }
}
