package be.imgn.mtg.engine.action;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.action.ValidationResult.ValidationError;

@DisplayName("ValidationResult")
class ValidationResultTest {

    @Nested
    @DisplayName("Legal")
    class LegalTests {

        @Test
        void canBeCreated() {
            var result = new ValidationResult.Legal();
            assertThat(result).isNotNull();
        }

        @Test
        void isInstanceOfValidationResult() {
            ValidationResult result = new ValidationResult.Legal();
            assertThat(result).isInstanceOf(ValidationResult.class);
        }

        @Test
        void equalsAnotherLegalResult() {
            var result1 = new ValidationResult.Legal();
            var result2 = new ValidationResult.Legal();
            assertThat(result1).isEqualTo(result2);
        }

        @Test
        void hasSameHashCodeAsAnotherLegalResult() {
            var result1 = new ValidationResult.Legal();
            var result2 = new ValidationResult.Legal();
            assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        }
    }

    @Nested
    @DisplayName("Illegal")
    class IllegalTests {

        @Test
        void canBeCreated() {
            var result = new ValidationResult.Illegal(
                    List.of(new ValidationError("Test reason", IllegalActionType.NO_PRIORITY)));
            assertThat(result).isNotNull();
        }

        @Test
        void storesErrors() {
            var errors = List.of(new ValidationError("Cannot cast spell", IllegalActionType.WRONG_TIMING));
            var result = new ValidationResult.Illegal(errors);
            assertThat(result.errors())
                    .containsExactly(new ValidationError("Cannot cast spell", IllegalActionType.WRONG_TIMING));
        }

        @Test
        void storesMultipleErrors() {
            var errors = List.of(
                    new ValidationError("No priority", IllegalActionType.NO_PRIORITY),
                    new ValidationError("Wrong phase", IllegalActionType.WRONG_TIMING));
            var result = new ValidationResult.Illegal(errors);
            assertThat(result.errors()).hasSize(2);
        }

        @Test
        void isInstanceOfValidationResult() {
            ValidationResult result =
                    new ValidationResult.Illegal(List.of(new ValidationError("reason", IllegalActionType.NO_PRIORITY)));
            assertThat(result).isInstanceOf(ValidationResult.class);
        }

        @Test
        void equalsAnotherIllegalResultWithSameValues() {
            var errors = List.of(new ValidationError("reason", IllegalActionType.NO_PRIORITY));
            var result1 = new ValidationResult.Illegal(errors);
            var result2 = new ValidationResult.Illegal(errors);
            assertThat(result1).isEqualTo(result2);
        }

        @Test
        void doesNotEqualIllegalResultWithDifferentErrors() {
            var result1 = new ValidationResult.Illegal(
                    List.of(new ValidationError("reason1", IllegalActionType.NO_PRIORITY)));
            var result2 = new ValidationResult.Illegal(
                    List.of(new ValidationError("reason2", IllegalActionType.NO_PRIORITY)));
            assertThat(result1).isNotEqualTo(result2);
        }
    }

    @Nested
    @DisplayName("merge")
    class MergeTests {

        @Test
        void returnsLegalWhenAllResultsAreLegal() {
            var result = ValidationResult.merge(new ValidationResult.Legal(), new ValidationResult.Legal());
            assertThat(result).isEqualTo(new ValidationResult.Legal());
        }

        @Test
        void returnsIllegalWhenAnyResultIsIllegal() {
            var result = ValidationResult.merge(
                    new ValidationResult.Legal(),
                    new ValidationResult.Illegal(List.of(new ValidationError("error", IllegalActionType.NO_PRIORITY))));
            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.errors()).containsExactly(new ValidationError("error", IllegalActionType.NO_PRIORITY));
        }

        @Test
        void combinesErrorsFromMultipleIllegalResults() {
            var result = ValidationResult.merge(
                    new ValidationResult.Illegal(List.of(new ValidationError("error1", IllegalActionType.NO_PRIORITY))),
                    new ValidationResult.Legal(),
                    new ValidationResult.Illegal(
                            List.of(new ValidationError("error2", IllegalActionType.WRONG_TIMING))));
            assertThat(result).isInstanceOf(ValidationResult.Illegal.class);
            var illegal = (ValidationResult.Illegal) result;
            assertThat(illegal.errors())
                    .containsExactly(
                            new ValidationError("error1", IllegalActionType.NO_PRIORITY),
                            new ValidationError("error2", IllegalActionType.WRONG_TIMING));
        }

        @Test
        void returnsLegalWhenNoResultsProvided() {
            var result = ValidationResult.merge();
            assertThat(result).isEqualTo(new ValidationResult.Legal());
        }
    }

    @Test
    void legalAndIllegalAreNotEqual() {
        ValidationResult legal = new ValidationResult.Legal();
        ValidationResult illegal =
                new ValidationResult.Illegal(List.of(new ValidationError("reason", IllegalActionType.NO_PRIORITY)));
        assertThat(legal).isNotEqualTo(illegal);
    }
}
