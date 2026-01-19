package be.imgn.mtg.parse;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;

/// An abstraction over sequentially read characters. Supports both in-memory strings and lazily-loaded reader input.
abstract class CharInput {

    /// Reads the character at `index`.
    abstract char charAt(int index);

    /// Returns the index of `str` starting from `fromIndex`, or -1 if not found.
    abstract int indexOf(String str, int fromIndex);

    /// Do the characters starting from `index` start with `prefix`?
    abstract boolean startsWith(String prefix, int index);

    /// Is `index` the end of the input?
    abstract boolean isEof(int index);

    final boolean isInRange(int index) {
        return !isEof(index);
    }

    /// Returns a snippet of string starting from `index` with at most `maxChars`.
    abstract String snippet(int index, int maxChars);

    /// Characters before `checkpointIndex` are no longer needed.
    void markCheckpoint(int checkpointIndex) {}

    /// Returns the source position of the character at `at`. It's assumed that the index `at` has been read.
    abstract String sourcePosition(int at);

    /// An input backed by in-memory string.
    static CharInput from(String text) {
        requireNonNull(text);
        return new CharInput() {
            @Override
            char charAt(int index) {
                return text.charAt(index);
            }

            @Override
            int indexOf(String str, int fromIndex) {
                return text.indexOf(str, fromIndex);
            }

            @Override
            boolean startsWith(String prefix, int index) {
                return text.startsWith(prefix, index);
            }

            @Override
            boolean isEof(int index) {
                return index >= text.length();
            }

            @Override
            String snippet(int index, int maxLength) {
                return text.substring(index, Math.min(text.length(), index + maxLength));
            }

            @Override
            String sourcePosition(int at) {
                var line = 1;
                var lineStartIndex = 0;
                for (var i = 0; i < at && i < text.length(); i++) {
                    if (text.charAt(i) == '\n') {
                        lineStartIndex = i + 1;
                        line++;
                    }
                }
                return line + ":" + (at - lineStartIndex + 1);
            }
        };
    }

    /// A lazily-loaded input from `reader`.
    static CharInput from(Reader reader) {
        return from(reader, /* bufferSize= */ 8192, /* compactionThreshold= */ 128 * 1024);
    }

    /// A lazily-loaded input from `reader`.
    ///
    /// @param compactionThreshold compact the buffer if we have this number of chars no longer needed.
    static CharInput from(Reader reader, int bufferSize, int compactionThreshold) {
        requireNonNull(reader);
        return new CharInput() {
            private final char[] temp = new char[bufferSize];
            private final StringBuilder chars = new StringBuilder();
            private int garbageCharCount = 0;

            @Override
            char charAt(int index) {
                ensureCharCount(index + 1);
                return chars.charAt(toPhysicalIndex(index));
            }

            @Override
            int indexOf(String str, int fromIndex) {
                checkArgument(fromIndex >= garbageCharCount, "fromIndex < %s", garbageCharCount);
                for (var i = fromIndex; ; ) {
                    ensureCharCount(i + str.length());
                    var fromPhysicalIndex = toPhysicalIndex(i);
                    // If after expansion, we don't have enough chars, we've reached the end.
                    if (fromPhysicalIndex + str.length() > chars.length()) {
                        return -1;
                    }
                    var foundPhysicalIndex = chars.indexOf(str, fromPhysicalIndex);
                    // if String.indexOf() has found it, translate the physical index back to logical.
                    if (foundPhysicalIndex >= fromPhysicalIndex) {
                        return toLogicalIndex(foundPhysicalIndex);
                    }
                    // Assuming `str` is 5 chars, when we load the next page of characters, we can resume the
                    // scan with the last 4 chars in the current page, just in case. All other chars are
                    // provably useless.
                    i = toLogicalIndex(chars.length() - str.length() + 1);
                }
            }

            @Override
            boolean startsWith(String prefix, int index) {
                ensureCharCount(index + prefix.length());
                index = toPhysicalIndex(index);
                if (chars.length() < index + prefix.length()) {
                    return false;
                }
                for (var i = 0; i < prefix.length(); i++) {
                    if (prefix.charAt(i) != chars.charAt(index + i)) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            boolean isEof(int index) {
                ensureCharCount(index + 1);
                return toPhysicalIndex(index) >= chars.length();
            }

            @Override
            String snippet(int index, int maxLength) {
                ensureCharCount(index + maxLength);
                index = toPhysicalIndex(index);
                return chars.substring(index, Math.min(chars.length(), index + maxLength));
            }

            @Override
            void markCheckpoint(int checkpointIndex) {
                var unused = checkpointIndex - garbageCharCount;
                if (unused > compactionThreshold) {
                    chars.delete(0, unused);
                    garbageCharCount += unused;
                }
            }

            @Override
            String sourcePosition(int at) {
                return garbageCharCount > 0
                        ? Integer.toString(at)
                        : from(chars.toString()).sourcePosition(at);
            }

            private int toPhysicalIndex(int index) {
                index -= garbageCharCount;
                if (index < 0) {
                    throw new IllegalArgumentException("index must be at least " + garbageCharCount);
                }
                return index;
            }

            private int toLogicalIndex(int physical) {
                return garbageCharCount + physical;
            }

            private void ensureCharCount(int charCount) {
                for (var missing = charCount - garbageCharCount - chars.length(); missing > 0; ) {
                    try {
                        var loaded = reader.read(temp);
                        if (loaded <= 0) { // no more to load
                            break;
                        }
                        chars.append(temp, 0, loaded);
                        missing -= loaded;
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }
        };
    }

    private static void checkArgument(boolean condition, String message, Object... args) {
        if (!condition) {
            throw new IllegalArgumentException(String.format(message, args));
        }
    }
}
