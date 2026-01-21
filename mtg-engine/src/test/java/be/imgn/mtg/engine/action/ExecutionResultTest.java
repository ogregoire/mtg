package be.imgn.mtg.engine.action;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.event.GameEvent;

@DisplayName("ExecutionResult")
class ExecutionResultTest {

    @Nested
    @DisplayName("Success")
    class SuccessTests {

        @Test
        void canBeCreatedWithEmptyEvents() {
            var result = new ExecutionResult.Success(List.of());
            assertThat(result).isNotNull();
            assertThat(result.events()).isEmpty();
        }

        @Test
        void storesEvents() {
            // We use an empty list for now as we don't have concrete GameEvent implementations
            var events = List.<GameEvent>of();
            var result = new ExecutionResult.Success(events);
            assertThat(result.events()).isEqualTo(events);
        }

        @Test
        void isInstanceOfExecutionResult() {
            ExecutionResult result = new ExecutionResult.Success(List.of());
            assertThat(result).isInstanceOf(ExecutionResult.class);
        }

        @Test
        void equalsAnotherSuccessWithSameEvents() {
            var result1 = new ExecutionResult.Success(List.of());
            var result2 = new ExecutionResult.Success(List.of());
            assertThat(result1).isEqualTo(result2);
        }
    }

    @Nested
    @DisplayName("Aborted")
    class AbortedTests {

        @Test
        void canBeCreated() {
            var result = new ExecutionResult.Aborted("Player cancelled");
            assertThat(result).isNotNull();
        }

        @Test
        void storesReason() {
            var result = new ExecutionResult.Aborted("Target became invalid");
            assertThat(result.reason()).isEqualTo("Target became invalid");
        }

        @Test
        void isInstanceOfExecutionResult() {
            ExecutionResult result = new ExecutionResult.Aborted("reason");
            assertThat(result).isInstanceOf(ExecutionResult.class);
        }

        @Test
        void equalsAnotherAbortedWithSameReason() {
            var result1 = new ExecutionResult.Aborted("reason");
            var result2 = new ExecutionResult.Aborted("reason");
            assertThat(result1).isEqualTo(result2);
        }

        @Test
        void doesNotEqualAbortedWithDifferentReason() {
            var result1 = new ExecutionResult.Aborted("reason1");
            var result2 = new ExecutionResult.Aborted("reason2");
            assertThat(result1).isNotEqualTo(result2);
        }
    }

    @Nested
    @DisplayName("Illegal")
    class IllegalTests {

        @Test
        void canBeCreated() {
            var result = new ExecutionResult.Illegal("Action no longer valid");
            assertThat(result).isNotNull();
        }

        @Test
        void storesReason() {
            var result = new ExecutionResult.Illegal("State changed");
            assertThat(result.reason()).isEqualTo("State changed");
        }

        @Test
        void isInstanceOfExecutionResult() {
            ExecutionResult result = new ExecutionResult.Illegal("reason");
            assertThat(result).isInstanceOf(ExecutionResult.class);
        }

        @Test
        void equalsAnotherIllegalWithSameReason() {
            var result1 = new ExecutionResult.Illegal("reason");
            var result2 = new ExecutionResult.Illegal("reason");
            assertThat(result1).isEqualTo(result2);
        }

        @Test
        void doesNotEqualIllegalWithDifferentReason() {
            var result1 = new ExecutionResult.Illegal("reason1");
            var result2 = new ExecutionResult.Illegal("reason2");
            assertThat(result1).isNotEqualTo(result2);
        }
    }

    @Test
    void successAndAbortedAreNotEqual() {
        ExecutionResult success = new ExecutionResult.Success(List.of());
        ExecutionResult aborted = new ExecutionResult.Aborted("reason");
        assertThat(success).isNotEqualTo(aborted);
    }

    @Test
    void successAndIllegalAreNotEqual() {
        ExecutionResult success = new ExecutionResult.Success(List.of());
        ExecutionResult illegal = new ExecutionResult.Illegal("reason");
        assertThat(success).isNotEqualTo(illegal);
    }

    @Test
    void abortedAndIllegalAreNotEqual() {
        ExecutionResult aborted = new ExecutionResult.Aborted("reason");
        ExecutionResult illegal = new ExecutionResult.Illegal("reason");
        assertThat(aborted).isNotEqualTo(illegal);
    }
}
