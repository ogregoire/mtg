package be.imgn.mtg.engine.game.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.game.Choice;
import be.imgn.mtg.engine.game.Option;
import be.imgn.mtg.engine.game.SelectionCount;

class FirstOptionChoiceHandlerTest {

    private FirstOptionChoiceHandler handler;

    @BeforeEach
    void setUp() {
        handler = FirstOptionChoiceHandler.INSTANCE;
    }

    @Nested
    class Singleton {

        @Test
        void instanceIsNotNull() {
            assertThat(FirstOptionChoiceHandler.INSTANCE).isNotNull();
        }

        @Test
        void instanceIsSingleton() {
            assertThat(handler).isSameAs(FirstOptionChoiceHandler.INSTANCE);
        }
    }

    @Nested
    class ExactlyOne {

        @Test
        void selectsFirstOption() {
            var choice = Choice.oneOf(List.of("A", "B", "C"), "Pick one");

            var result = handler.choose(choice).join();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).value()).isEqualTo("A");
        }

        @Test
        void selectsOnlyAvailableOption() {
            var choice = Choice.oneOf(List.of("Only"), "Single choice");

            var result = handler.choose(choice).join();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).value()).isEqualTo("Only");
        }
    }

    @Nested
    class ExactlyN {

        @Test
        void selectsFirstNOptions() {
            var choice = Choice.nOf(List.of("A", "B", "C", "D"), 3, "Pick three");

            var result = handler.choose(choice).join();

            assertThat(result).hasSize(3);
            assertThat(result).extracting(Option::value).containsExactly("A", "B", "C");
        }

        @Test
        void selectsAllWhenNEqualsAvailable() {
            var choice = Choice.nOf(List.of("X", "Y"), 2, "Both");

            var result = handler.choose(choice).join();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Option::value).containsExactly("X", "Y");
        }

        @Test
        void selectsAllAvailableWhenNExceedsAvailable() {
            var choice = Choice.nOf(List.of("A", "B"), 5, "Pick five");

            var result = handler.choose(choice).join();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Option::value).containsExactly("A", "B");
        }
    }

    @Nested
    class UpTo {

        @Test
        void selectsOneOptionForUpTo() {
            var choice = Choice.upTo(List.of("A", "B", "C"), 2, "Up to two");

            var result = handler.choose(choice).join();

            // FirstOptionChoiceHandler selects 1 for "up to" rather than 0
            assertThat(result).hasSize(1);
            assertThat(result.get(0).value()).isEqualTo("A");
        }

        @Test
        void selectsFirstWhenUpToOne() {
            var choice = Choice.upTo(List.of("X", "Y"), 1, "Maybe one");

            var result = handler.choose(choice).join();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).value()).isEqualTo("X");
        }
    }

    @Nested
    class AtLeast {

        @Test
        void selectsMinimumRequired() {
            var choice = Choice.atLeast(List.of("A", "B", "C", "D"), 2, "At least two");

            var result = handler.choose(choice).join();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Option::value).containsExactly("A", "B");
        }

        @Test
        void selectsAllWhenMinEqualsAvailable() {
            var choice = Choice.atLeast(List.of("X", "Y"), 2, "At least two");

            var result = handler.choose(choice).join();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Option::value).containsExactly("X", "Y");
        }

        @Test
        void selectsAllAvailableWhenMinExceedsAvailable() {
            var choice = Choice.atLeast(List.of("A"), 3, "At least three");

            var result = handler.choose(choice).join();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).value()).isEqualTo("A");
        }
    }

    @Nested
    class Between {

        @Test
        void selectsMinimumForBetween() {
            var choice = Choice.withDescriptions(
                    List.of(
                            new Option<>("A", "First"),
                            new Option<>("B", "Second"),
                            new Option<>("C", "Third"),
                            new Option<>("D", "Fourth")),
                    SelectionCount.between(2, 3),
                    "Pick 2-3");

            var result = handler.choose(choice).join();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Option::value).containsExactly("A", "B");
        }

        @Test
        void selectsAllWhenBetweenRangeExceedsAvailable() {
            var choice = Choice.withDescriptions(
                    List.of(new Option<>("X", "Only one")), SelectionCount.between(2, 5), "Pick 2-5");

            var result = handler.choose(choice).join();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).value()).isEqualTo("X");
        }
    }

    @Nested
    class CompletesImmediately {

        @Test
        void returnsCompletedFuture() {
            var choice = Choice.oneOf(List.of("A"), "Choose");

            var future = handler.choose(choice);

            assertThat(future).isDone();
            assertThat(future).isNotCancelled();
        }
    }
}
