package be.imgn.mtg.engine.action;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
            var result = new ValidationResult.Illegal("Test reason", IllegalActionType.NO_PRIORITY);
            assertThat(result).isNotNull();
        }

        @Test
        void storesReason() {
            var result = new ValidationResult.Illegal("Cannot cast spell", IllegalActionType.WRONG_TIMING);
            assertThat(result.reason()).isEqualTo("Cannot cast spell");
        }

        @Test
        void storesType() {
            var result = new ValidationResult.Illegal("No mana", IllegalActionType.CANNOT_PAY_COST);
            assertThat(result.type()).isEqualTo(IllegalActionType.CANNOT_PAY_COST);
        }

        @Test
        void isInstanceOfValidationResult() {
            ValidationResult result = new ValidationResult.Illegal("reason", IllegalActionType.NO_PRIORITY);
            assertThat(result).isInstanceOf(ValidationResult.class);
        }

        @Test
        void equalsAnotherIllegalResultWithSameValues() {
            var result1 = new ValidationResult.Illegal("reason", IllegalActionType.NO_PRIORITY);
            var result2 = new ValidationResult.Illegal("reason", IllegalActionType.NO_PRIORITY);
            assertThat(result1).isEqualTo(result2);
        }

        @Test
        void doesNotEqualIllegalResultWithDifferentReason() {
            var result1 = new ValidationResult.Illegal("reason1", IllegalActionType.NO_PRIORITY);
            var result2 = new ValidationResult.Illegal("reason2", IllegalActionType.NO_PRIORITY);
            assertThat(result1).isNotEqualTo(result2);
        }

        @Test
        void doesNotEqualIllegalResultWithDifferentType() {
            var result1 = new ValidationResult.Illegal("reason", IllegalActionType.NO_PRIORITY);
            var result2 = new ValidationResult.Illegal("reason", IllegalActionType.WRONG_TIMING);
            assertThat(result1).isNotEqualTo(result2);
        }
    }

    @Test
    void legalAndIllegalAreNotEqual() {
        ValidationResult legal = new ValidationResult.Legal();
        ValidationResult illegal = new ValidationResult.Illegal("reason", IllegalActionType.NO_PRIORITY);
        assertThat(legal).isNotEqualTo(illegal);
    }
}
