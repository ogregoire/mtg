package be.imgn.mtg.engine.card;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CardLayoutTest {

    private static final Set<CardLayout> DOUBLE_FACED_LAYOUTS = EnumSet.of(
            CardLayout.TRANSFORM,
            CardLayout.MODAL_DFC,
            CardLayout.DOUBLE_FACED_TOKEN,
            CardLayout.ART_SERIES,
            CardLayout.REVERSIBLE_CARD);

    @Nested
    class IsDoubleFaced {

        @ParameterizedTest
        @EnumSource(CardLayout.class)
        void returnsCorrectValueForAllLayouts(CardLayout layout) {
            assertThat(layout.isDoubleFaced()).isEqualTo(DOUBLE_FACED_LAYOUTS.contains(layout));
        }

        @Test
        void exactlyFiveDoubleFacedLayouts() {
            var count = 0;
            for (var layout : CardLayout.values()) {
                if (layout.isDoubleFaced()) {
                    count++;
                }
            }
            assertThat(count).isEqualTo(5);
        }

        @Test
        void meldIsNotDoubleFaced() {
            assertThat(CardLayout.MELD.isDoubleFaced()).isFalse();
        }
    }
}
