package be.imgn.mtg.engine.characteristics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CustomCounterType")
class CustomCounterTypeTest {

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        void createsCounterTypeWithText() {
            var counterType = new CustomCounterType("hourglass");

            assertThat(counterType.text()).isEqualTo("hourglass");
        }

        @Test
        void normalizesTextToLowercase() {
            var counterType = new CustomCounterType("FILIBUSTER");

            assertThat(counterType.text()).isEqualTo("filibuster");
        }

        @Test
        void preservesAlreadyLowercaseText() {
            var counterType = new CustomCounterType("age");

            assertThat(counterType.text()).isEqualTo("age");
        }

        @Test
        void normalizesMixedCaseText() {
            var counterType = new CustomCounterType("DeVoTiOn");

            assertThat(counterType.text()).isEqualTo("devotion");
        }

        @Test
        void preservesHyphenatedNames() {
            var counterType = new CustomCounterType("Ki-Stone");

            assertThat(counterType.text()).isEqualTo("ki-stone");
        }

        @Test
        void preservesMultiWordNames() {
            var counterType = new CustomCounterType("Lore Counter");

            assertThat(counterType.text()).isEqualTo("lore counter");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t", "\n", "   \t\n  "})
        void throwsExceptionForBlankText(String blankText) {
            assertThatThrownBy(() -> new CustomCounterType(blankText))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Counter type text cannot be null or blank");
        }
    }

    @Nested
    @DisplayName("text")
    class Text {

        @Test
        void returnsNormalizedText() {
            var counterType = new CustomCounterType("BRICK");

            assertThat(counterType.text()).isEqualTo("brick");
        }
    }

    @Nested
    @DisplayName("equality and hashCode")
    class EqualityAndHashCode {

        @Test
        void sameTextCreatesEqualCounterTypes() {
            var counterType1 = new CustomCounterType("verse");
            var counterType2 = new CustomCounterType("verse");

            assertThat(counterType1).isEqualTo(counterType2);
            assertThat(counterType1.hashCode()).isEqualTo(counterType2.hashCode());
        }

        @Test
        void normalizationMakesCounterTypesEqual() {
            var counterType1 = new CustomCounterType("verse");
            var counterType2 = new CustomCounterType("VERSE");

            assertThat(counterType1).isEqualTo(counterType2);
            assertThat(counterType1.hashCode()).isEqualTo(counterType2.hashCode());
        }

        @Test
        void differentTextCreatesUnequalCounterTypes() {
            var counterType1 = new CustomCounterType("verse");
            var counterType2 = new CustomCounterType("chorus");

            assertThat(counterType1).isNotEqualTo(counterType2);
        }

        @Test
        void isNotEqualToNull() {
            var counterType = new CustomCounterType("time");

            assertThat(counterType).isNotEqualTo(null);
        }
    }

    @Nested
    @DisplayName("implements CounterType")
    class ImplementsCounterType {

        @Test
        void isInstanceOfCounterType() {
            CounterType counterType = new CustomCounterType("quest");

            assertThat(counterType).isInstanceOf(CounterType.class);
        }

        @Test
        void canBeUsedAsCounterType() {
            CounterType counterType = new CustomCounterType("experience");

            assertThat(counterType.text()).isEqualTo("experience");
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        void containsCounterTypeName() {
            var counterType = new CustomCounterType("doom");

            assertThat(counterType.toString()).contains("doom");
        }
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCases {

        @Test
        void handlesUnicodeCharacters() {
            var counterType = new CustomCounterType("Müller");

            assertThat(counterType.text()).isEqualTo("müller");
        }

        @Test
        void handlesNumbersInText() {
            var counterType = new CustomCounterType("Stage3");

            assertThat(counterType.text()).isEqualTo("stage3");
        }

        @Test
        void handlesSpecialCharactersInText() {
            var counterType = new CustomCounterType("Soul's");

            assertThat(counterType.text()).isEqualTo("soul's");
        }

        @Test
        void handlesVeryLongText() {
            var longText = "a".repeat(1000);
            var counterType = new CustomCounterType(longText);

            assertThat(counterType.text()).hasSize(1000);
        }
    }
}
