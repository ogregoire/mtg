package be.imgn.mtg.engine.util;

import java.security.SecureRandom;
import java.util.UUID;

/// UUID version 7 generator (RFC 9562).
///
/// UUID v7 is time-ordered using Unix timestamp in milliseconds,
/// making it suitable for database primary keys and sorted collections.
///
/// Format (128 bits total):
/// - 48 bits: Unix timestamp in milliseconds
/// - 4 bits: version (0111 = 7)
/// - 12 bits: random
/// - 2 bits: variant (10)
/// - 62 bits: random
public final class UUIDv7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UUIDv7() {}

    /// Creates a new UUID v7.
    public static UUID randomUUID() {
        var timestamp = System.currentTimeMillis();
        var randomBytes = new byte[10];
        RANDOM.nextBytes(randomBytes);

        // Most significant 64 bits:
        // - 48 bits timestamp
        // - 4 bits version (7)
        // - 12 bits random
        var msb = (timestamp << 16) | (7L << 12) | ((randomBytes[0] & 0xFFL) << 4) | ((randomBytes[1] & 0xF0L) >> 4);

        // Least significant 64 bits:
        // - 2 bits variant (10)
        // - 62 bits random
        var lsb = ((randomBytes[1] & 0x0FL) << 60)
                | ((randomBytes[2] & 0xFFL) << 52)
                | ((randomBytes[3] & 0xFFL) << 44)
                | ((randomBytes[4] & 0xFFL) << 36)
                | ((randomBytes[5] & 0xFFL) << 28)
                | ((randomBytes[6] & 0xFFL) << 20)
                | ((randomBytes[7] & 0xFFL) << 12)
                | ((randomBytes[8] & 0xFFL) << 4)
                | ((randomBytes[9] & 0xF0L) >> 4);
        lsb = (lsb & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;

        return new UUID(msb, lsb);
    }
}
