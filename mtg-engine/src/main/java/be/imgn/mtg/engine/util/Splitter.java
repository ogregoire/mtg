package be.imgn.mtg.engine.util;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/// A lightweight string splitter that returns a {@link Stream} of parts.
///
/// For literal separators ({@link #on(char)}, {@link #on(String)}), splitting uses
/// {@code String.indexOf} with no regex overhead. For regex separators
/// ({@link #onPattern(String)}, {@link #onPattern(Pattern)}), splitting uses a compiled
/// {@link Pattern}.
///
/// Example usage:
/// ```java
/// Splitter.on(',').trimResults().split("a, b, c")  // Stream of "a", "b", "c"
/// Splitter.on(',').omitEmptyStrings().split("a,,b")  // Stream of "a", "b"
/// Splitter.on(',').limit(2).split("a,b,c")  // Stream of "a", "b,c"
/// Splitter.onPattern("\\s+").split("one two  three")  // Stream of "one", "two", "three"
/// ```
public final class Splitter {

    private final Strategy strategy;
    private final UnaryOperator<String> transform;
    private final boolean omitEmpty;
    private final int limit;

    private Splitter(Strategy strategy, UnaryOperator<String> transform, boolean omitEmpty, int limit) {
        this.strategy = strategy;
        this.transform = transform;
        this.omitEmpty = omitEmpty;
        this.limit = limit;
    }

    /// Creates a splitter that splits on the given literal string.
    public static Splitter on(String separator) {
        if (separator.isEmpty()) {
            throw new IllegalArgumentException("separator cannot be empty");
        }
        return new Splitter(new LiteralStrategy(separator), UnaryOperator.identity(), false, 0);
    }

    /// Creates a splitter that splits on the given character.
    public static Splitter on(char separator) {
        return new Splitter(new CharStrategy(separator), UnaryOperator.identity(), false, 0);
    }

    /// Creates a splitter that splits on the given regex pattern string.
    public static Splitter onPattern(String regex) {
        return onPattern(Pattern.compile(regex));
    }

    /// Creates a splitter that splits on the given compiled pattern.
    public static Splitter onPattern(Pattern pattern) {
        return new Splitter(new PatternStrategy(pattern), UnaryOperator.identity(), false, 0);
    }

    /// Returns a splitter that trims whitespace from the resulting parts.
    public Splitter trimResults() {
        return new Splitter(strategy, String::strip, omitEmpty, limit);
    }

    /// Returns a splitter that omits empty strings from the results.
    /// Empty strings are checked after trimming (if {@link #trimResults()} is also used).
    public Splitter omitEmptyStrings() {
        return new Splitter(strategy, transform, true, limit);
    }

    /// Returns a splitter that stops splitting after the given number of parts.
    /// The last part contains the unsplit remainder of the input.
    ///
    /// @param maxParts the maximum number of parts to produce (must be positive)
    public Splitter limit(int maxParts) {
        if (maxParts <= 0) {
            throw new IllegalArgumentException("limit must be positive: " + maxParts);
        }
        return new Splitter(strategy, transform, omitEmpty, maxParts);
    }

    /// Splits the given input and returns a stream of parts.
    public Stream<String> split(CharSequence input) {
        var str = input.toString();
        var spliterator = strategy.spliterator(str, transform, omitEmpty, limit);
        return StreamSupport.stream(spliterator, false);
    }

    /// Strategy for finding the next separator in the input.
    private sealed interface Strategy {
        Spliterator<String> spliterator(String input, UnaryOperator<String> transform, boolean omitEmpty, int limit);
    }

    private record CharStrategy(char separator) implements Strategy {
        @Override
        public Spliterator<String> spliterator(
                String input, UnaryOperator<String> transform, boolean omitEmpty, int limit) {
            return new SplitSpliterator(input, transform, omitEmpty, limit) {
                @Override
                int nextSeparator(int start) {
                    return input.indexOf(separator, start);
                }

                @Override
                int separatorEnd(int separatorStart) {
                    return separatorStart + 1;
                }
            };
        }
    }

    private record LiteralStrategy(String separator) implements Strategy {
        @Override
        public Spliterator<String> spliterator(
                String input, UnaryOperator<String> transform, boolean omitEmpty, int limit) {
            return new SplitSpliterator(input, transform, omitEmpty, limit) {
                @Override
                int nextSeparator(int start) {
                    return input.indexOf(separator, start);
                }

                @Override
                int separatorEnd(int separatorStart) {
                    return separatorStart + separator.length();
                }
            };
        }
    }

    private record PatternStrategy(Pattern pattern) implements Strategy {
        @Override
        public Spliterator<String> spliterator(
                String input, UnaryOperator<String> transform, boolean omitEmpty, int limit) {
            var matcher = pattern.matcher(input);
            return new SplitSpliterator(input, transform, omitEmpty, limit) {
                @Override
                int nextSeparator(int start) {
                    return matcher.find(start) ? matcher.start() : -1;
                }

                @Override
                int separatorEnd(int separatorStart) {
                    return matcher.end();
                }
            };
        }
    }

    private abstract static class SplitSpliterator extends Spliterators.AbstractSpliterator<String> {

        private final String input;
        private final UnaryOperator<String> transform;
        private final boolean omitEmpty;
        private final int limit;
        private int offset;
        private int emitted;
        private boolean done;

        SplitSpliterator(String input, UnaryOperator<String> transform, boolean omitEmpty, int limit) {
            super(
                    limit > 0 ? limit : Long.MAX_VALUE,
                    Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.IMMUTABLE);
            this.input = input;
            this.transform = transform;
            this.omitEmpty = omitEmpty;
            this.limit = limit;
        }

        abstract int nextSeparator(int start);

        abstract int separatorEnd(int separatorStart);

        @Override
        public boolean tryAdvance(Consumer<? super String> action) {
            while (!done) {
                var lastPart = limit > 0 && emitted == limit - 1;
                if (lastPart) {
                    var result = transform.apply(input.substring(offset));
                    done = true;
                    if (omitEmpty && result.isEmpty()) {
                        return false;
                    }
                    action.accept(result);
                    emitted++;
                    return true;
                }
                var sepIndex = nextSeparator(offset);
                if (sepIndex == -1) {
                    var result = transform.apply(input.substring(offset));
                    done = true;
                    if (omitEmpty && result.isEmpty()) {
                        return false;
                    }
                    action.accept(result);
                    emitted++;
                    return true;
                }
                var result = transform.apply(input.substring(offset, sepIndex));
                offset = separatorEnd(sepIndex);
                if (omitEmpty && result.isEmpty()) {
                    continue;
                }
                action.accept(result);
                emitted++;
                return true;
            }
            return false;
        }
    }
}
