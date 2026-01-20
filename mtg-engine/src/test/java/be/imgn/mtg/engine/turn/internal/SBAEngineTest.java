package be.imgn.mtg.engine.turn.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.StateBasedAction;

class SBAEngineTest {

    private GameState gameState;

    @BeforeEach
    void setUp() {
        gameState = mock(GameState.class);
    }

    @Nested
    class CheckAndApply {

        @Test
        void returnsFalseWhenNoSBAsApply() {
            var sba = mockSBA(false);
            var engine = new DefaultSBAEngine(List.of(sba));

            var result = engine.checkAndApply(gameState);

            assertThat(result).isFalse();
            verify(sba, never()).apply(any());
        }

        @Test
        void returnsTrueWhenSBAApplies() {
            var sba = mock(StateBasedAction.class);
            // Returns true once, then false (SBA applies once, then stops)
            when(sba.appliesTo(gameState)).thenReturn(true, false);
            var engine = new DefaultSBAEngine(List.of(sba));

            var result = engine.checkAndApply(gameState);

            assertThat(result).isTrue();
            verify(sba).apply(gameState);
        }

        @Test
        void appliesMultipleSBAsInOnePass() {
            var sba1 = mock(StateBasedAction.class);
            var sba2 = mock(StateBasedAction.class);
            // Both apply once, then stop
            when(sba1.appliesTo(gameState)).thenReturn(true, false);
            when(sba2.appliesTo(gameState)).thenReturn(true, false);
            var engine = new DefaultSBAEngine(List.of(sba1, sba2));

            engine.checkAndApply(gameState);

            verify(sba1).apply(gameState);
            verify(sba2).apply(gameState);
        }

        @Test
        void loopsUntilNoSBAsApply() {
            var sba = mock(StateBasedAction.class);
            // First pass: applies, second pass: doesn't apply
            when(sba.appliesTo(gameState)).thenReturn(true, true, false);

            var engine = new DefaultSBAEngine(List.of(sba));
            engine.checkAndApply(gameState);

            // Should have been applied twice (two passes where it applied)
            verify(sba, times(2)).apply(gameState);
        }
    }

    @Nested
    class WouldPerformActions {

        @Test
        void returnsTrueWhenAnySBAWouldApply() {
            var sba1 = mockSBA(false);
            var sba2 = mockSBA(true);
            var engine = new DefaultSBAEngine(List.of(sba1, sba2));

            assertThat(engine.wouldPerformActions(gameState)).isTrue();
        }

        @Test
        void returnsFalseWhenNoSBAWouldApply() {
            var sba1 = mockSBA(false);
            var sba2 = mockSBA(false);
            var engine = new DefaultSBAEngine(List.of(sba1, sba2));

            assertThat(engine.wouldPerformActions(gameState)).isFalse();
        }

        @Test
        void doesNotApplyWhenChecking() {
            var sba = mockSBA(true);
            var engine = new DefaultSBAEngine(List.of(sba));

            engine.wouldPerformActions(gameState);

            verify(sba, never()).apply(any());
        }
    }

    private StateBasedAction mockSBA(boolean applies) {
        var sba = mock(StateBasedAction.class);
        when(sba.appliesTo(gameState)).thenReturn(applies);
        return sba;
    }
}
