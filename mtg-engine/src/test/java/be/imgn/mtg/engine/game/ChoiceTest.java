package be.imgn.mtg.engine.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ChoiceTest {

    @Nested
    class Construction {

        @Test
        void createsChoiceWithOptions() {
            var options = List.of(new Option<>("A", "Option A"), new Option<>("B", "Option B"));
            var choice = new Choice<>(options, SelectionCount.exactly(1), "Pick one");

            assertThat(choice.options()).hasSize(2);
            assertThat(choice.count()).isInstanceOf(SelectionCount.Exactly.class);
            assertThat(choice.description()).isEqualTo("Pick one");
        }

        @Test
        void throwsWhenOptionsAreEmpty() {
            assertThatThrownBy(() -> new Choice<>(List.of(), SelectionCount.exactly(1), "Description"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one option");
        }

        @Test
        void makesDefensiveCopyOfOptions() {
            var option1 = new Option<>("A", "A");
            var option2 = new Option<>("B", "B");
            var options = new ArrayList<>(List.of(option1, option2));
            var choice = new Choice<>(options, SelectionCount.exactly(1), "Choose");

            // Choice makes a defensive copy, so modifying the original shouldn't affect the choice
            options.clear();
            assertThat(choice.options()).hasSize(2);
            assertThat(choice.options()).containsExactly(option1, option2);
        }
    }

    @Nested
    class OneOf {

        @Test
        void createsChoiceWithExactlyOne() {
            var choice = Choice.oneOf(List.of("A", "B", "C"), "Choose one");

            assertThat(choice.count()).isEqualTo(SelectionCount.exactly(1));
            assertThat(choice.description()).isEqualTo("Choose one");
            assertThat(choice.options()).hasSize(3);
        }

        @Test
        void wrapsValuesInOptions() {
            var choice = Choice.oneOf(List.of("Apple", "Banana"), "Pick fruit");

            assertThat(choice.options()).extracting(Option::value).containsExactly("Apple", "Banana");
        }

        @Test
        void usesToStringForDescription() {
            var choice = Choice.oneOf(List.of("Alpha", "Beta"), "Choose");

            assertThat(choice.options()).extracting(Option::description).containsExactly("Alpha", "Beta");
        }
    }

    @Nested
    class NOf {

        @Test
        void createsChoiceWithExactlyN() {
            var choice = Choice.nOf(List.of(1, 2, 3, 4), 3, "Pick three");

            assertThat(choice.count()).isEqualTo(SelectionCount.exactly(3));
            assertThat(choice.description()).isEqualTo("Pick three");
            assertThat(choice.options()).hasSize(4);
        }

        @Test
        void wrapsValuesInOptions() {
            var choice = Choice.nOf(List.of(10, 20, 30), 2, "Choose two");

            assertThat(choice.options()).extracting(Option::value).containsExactly(10, 20, 30);
        }
    }

    @Nested
    class UpTo {

        @Test
        void createsChoiceWithUpToMax() {
            var choice = Choice.upTo(List.of("X", "Y", "Z"), 2, "Up to two");

            assertThat(choice.count()).isEqualTo(SelectionCount.upTo(2));
            assertThat(choice.description()).isEqualTo("Up to two");
            assertThat(choice.options()).hasSize(3);
        }

        @Test
        void wrapsValuesInOptions() {
            var choice = Choice.upTo(List.of(1.0, 2.0), 1, "Maybe one");

            assertThat(choice.options()).extracting(Option::value).containsExactly(1.0, 2.0);
        }
    }

    @Nested
    class AtLeast {

        @Test
        void createsChoiceWithAtLeastMin() {
            var choice = Choice.atLeast(List.of("A", "B", "C", "D"), 2, "At least two");

            assertThat(choice.count()).isEqualTo(SelectionCount.atLeast(2));
            assertThat(choice.description()).isEqualTo("At least two");
            assertThat(choice.options()).hasSize(4);
        }

        @Test
        void wrapsValuesInOptions() {
            var choice = Choice.atLeast(List.of(true, false), 1, "At least one");

            assertThat(choice.options()).extracting(Option::value).containsExactly(true, false);
        }
    }

    @Nested
    class WithDescriptions {

        @Test
        void createsChoiceFromExplicitOptions() {
            var options = List.of(new Option<>(1, "First option"), new Option<>(2, "Second option"));
            var choice = Choice.withDescriptions(options, "Choose");

            assertThat(choice.options()).isEqualTo(options);
            assertThat(choice.count()).isEqualTo(SelectionCount.exactly(1));
            assertThat(choice.description()).isEqualTo("Choose");
        }

        @Test
        void preservesCustomDescriptions() {
            var options = List.of(
                    new Option<>("short", "A much longer description"),
                    new Option<>("tiny", "Another verbose description"));
            var choice = Choice.withDescriptions(options, "Pick");

            assertThat(choice.options().get(0).description()).isEqualTo("A much longer description");
            assertThat(choice.options().get(1).description()).isEqualTo("Another verbose description");
        }

        @Test
        void supportsCustomSelectionCount() {
            var options = List.of(
                    new Option<>("A", "Option A"), new Option<>("B", "Option B"), new Option<>("C", "Option C"));
            var choice = Choice.withDescriptions(options, SelectionCount.between(1, 2), "Choose 1-2");

            assertThat(choice.count()).isEqualTo(SelectionCount.between(1, 2));
            assertThat(choice.description()).isEqualTo("Choose 1-2");
        }
    }
}
