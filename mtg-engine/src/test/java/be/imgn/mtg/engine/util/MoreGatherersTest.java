package be.imgn.mtg.engine.util;

import static be.imgn.mtg.engine.util.MoreGatherers.instanceOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MoreGatherersTest {

    @Nested
    class InstanceOf {

        @Test
        void filtersAndCastsByType() {
            var result = Stream.of("a", 1, "b", 2, "c")
                    .gather(instanceOf(String.class))
                    .toList();

            assertThat(result).containsExactly("a", "b", "c");
        }

        @Test
        void returnsEmptyWhenNoMatches() {
            var result = Stream.of(1, 2, 3).gather(instanceOf(String.class)).toList();

            assertThat(result).isEmpty();
        }

        @Test
        void filtersSubtypes() {
            var result =
                    Stream.of(1, 2.5, 3, 4.5).gather(instanceOf(Double.class)).toList();

            assertThat(result).containsExactly(2.5, 4.5);
        }

        @Test
        void acceptsAllWhenMatchingBaseType() {
            var result = Stream.of(1, 2.5, 3L).gather(instanceOf(Number.class)).toList();

            assertThat(result).containsExactly(1, 2.5, 3L);
        }

        @Test
        void handlesMixedNullsAndValues() {
            // null is not an instance of any class
            var result =
                    Stream.of("a", null, "b").gather(instanceOf(String.class)).toList();

            assertThat(result).containsExactly("a", "b");
        }

        @Test
        void worksWithRecordTypes() {
            record A(int value) {}
            record B(String text) {}

            var result = Stream.of(new A(1), new B("x"), new A(2))
                    .gather(instanceOf(A.class))
                    .toList();

            assertThat(result).containsExactly(new A(1), new A(2));
        }

        @Test
        void preservesOrder() {
            var result = Stream.of("first", 1, "second", 2, "third")
                    .gather(instanceOf(String.class))
                    .toList();

            assertThat(result).containsExactly("first", "second", "third");
        }

        @Test
        void worksWithEmptyStream() {
            var result = Stream.<Object>empty().gather(instanceOf(String.class)).toList();

            assertThat(result).isEmpty();
        }

        @Test
        void supportsIntermediateStreamOperations() {
            var result = Stream.of("a", 1, "bb", 2, "ccc")
                    .gather(instanceOf(String.class))
                    .filter(s -> s.length() > 1)
                    .map(String::toUpperCase)
                    .toList();

            assertThat(result).containsExactly("BB", "CCC");
        }
    }
}
