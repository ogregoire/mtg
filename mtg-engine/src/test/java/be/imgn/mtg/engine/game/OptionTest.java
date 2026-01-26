package be.imgn.mtg.engine.game;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OptionTest {

    @Nested
    class Construction {

        @Test
        void createsOptionWithValueAndDescription() {
            var option = new Option<>("value", "A description");

            assertThat(option.value()).isEqualTo("value");
            assertThat(option.description()).isEqualTo("A description");
        }

        @Test
        void allowsDifferentTypes() {
            var intOption = new Option<>(42, "The answer");
            var boolOption = new Option<>(true, "Yes");

            assertThat(intOption.value()).isEqualTo(42);
            assertThat(boolOption.value()).isTrue();
        }
    }

    @Nested
    class ToStringMethod {

        @Test
        void returnsDescription() {
            var option = new Option<>("underlying", "Display text");

            assertThat(option.toString()).isEqualTo("Display text");
        }

        @Test
        void usesDescriptionNotValue() {
            var option = new Option<>(123, "Number one-two-three");

            assertThat(option.toString()).isEqualTo("Number one-two-three");
            assertThat(option.toString()).isNotEqualTo("123");
        }
    }

    @Nested
    class RecordEquality {

        @Test
        void equalOptionsAreEqual() {
            var option1 = new Option<>("A", "Option A");
            var option2 = new Option<>("A", "Option A");

            assertThat(option1).isEqualTo(option2);
        }

        @Test
        void differentValuesAreNotEqual() {
            var option1 = new Option<>("A", "Same description");
            var option2 = new Option<>("B", "Same description");

            assertThat(option1).isNotEqualTo(option2);
        }

        @Test
        void differentDescriptionsAreNotEqual() {
            var option1 = new Option<>("Same", "Description 1");
            var option2 = new Option<>("Same", "Description 2");

            assertThat(option1).isNotEqualTo(option2);
        }

        @Test
        void hashCodeIsConsistent() {
            var option1 = new Option<>(42, "Number");
            var option2 = new Option<>(42, "Number");

            assertThat(option1.hashCode()).isEqualTo(option2.hashCode());
        }
    }
}
