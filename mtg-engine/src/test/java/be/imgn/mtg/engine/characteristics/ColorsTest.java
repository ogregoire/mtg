package be.imgn.mtg.engine.characteristics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.assertions.MTGAssertions;

class ColorsTest {

    @Test
    void emptyColors() {
        var colors = Colors.empty();

        MTGAssertions.assertThat(colors).isEmpty().isColorless().hasCount(0);
    }

    @Test
    void singleColor() {
        var colors = Colors.of(Color.RED);

        MTGAssertions.assertThat(colors)
                .isNotEmpty()
                .isColored()
                .isMonoColored()
                .hasCount(1)
                .contains(Color.RED)
                .isRed()
                .doesNotContain(Color.WHITE);
    }

    @Test
    void multipleColors() {
        var colors = Colors.of(Color.WHITE, Color.BLUE);

        MTGAssertions.assertThat(colors)
                .isNotEmpty()
                .isColored()
                .isMultiColored()
                .hasCount(2)
                .contains(Color.WHITE)
                .contains(Color.BLUE)
                .isWhite()
                .isBlue();
    }

    @Test
    void allColors() {
        var colors = Colors.of(Color.WHITE, Color.BLUE, Color.BLACK, Color.RED, Color.GREEN);

        MTGAssertions.assertThat(colors)
                .hasCount(5)
                .isWhite()
                .isBlue()
                .isBlack()
                .isRed()
                .isGreen();
    }

    @Test
    void builderAddsColors() {
        var colors = Colors.builder().add(Color.WHITE).add(Color.BLACK).build();

        MTGAssertions.assertThat(colors).hasCount(2).containsExactly(Color.WHITE, Color.BLACK);
    }

    @Test
    void builderClearsColors() {
        var colors = Colors.builder().add(Color.RED).clear().add(Color.GREEN).build();

        MTGAssertions.assertThat(colors).hasCount(1).contains(Color.GREEN).doesNotContain(Color.RED);
    }

    @Test
    void toBuilderCopiesColors() {
        var original = Colors.of(Color.BLUE, Color.BLACK);
        var modified = original.toBuilder().add(Color.RED).build();

        MTGAssertions.assertThat(original).hasCount(2);
        MTGAssertions.assertThat(modified)
                .hasCount(3)
                .contains(Color.BLUE)
                .contains(Color.BLACK)
                .contains(Color.RED);
    }

    @Test
    void duplicateColorsAreIgnored() {
        var colors = Colors.of(Color.RED, Color.RED, Color.RED);

        MTGAssertions.assertThat(colors).hasCount(1).contains(Color.RED);
    }

    @Test
    void isMonoColoredReturnsFalseForEmpty() {
        var colors = Colors.empty();

        assertThat(colors.isMonoColored()).isFalse();
    }

    @Test
    void isMonoColoredReturnsFalseForMultiple() {
        var colors = Colors.of(Color.RED, Color.GREEN);

        assertThat(colors.isMonoColored()).isFalse();
    }

    @Test
    void isMultiColoredReturnsFalseForEmpty() {
        var colors = Colors.empty();

        assertThat(colors.isMultiColored()).isFalse();
    }

    @Test
    void isMultiColoredReturnsFalseForSingle() {
        var colors = Colors.of(Color.BLUE);

        assertThat(colors.isMultiColored()).isFalse();
    }

    @Test
    void isAllColorsReturnsTrueForFiveColors() {
        var colors = Colors.of(Color.WHITE, Color.BLUE, Color.BLACK, Color.RED, Color.GREEN);

        assertThat(colors.isAllColors()).isTrue();
    }

    @Test
    void isAllColorsReturnsFalseForFewerColors() {
        var colors = Colors.of(Color.RED, Color.GREEN);

        assertThat(colors.isAllColors()).isFalse();
    }

    @Test
    void isAllColorsReturnsFalseForEmpty() {
        var colors = Colors.empty();

        assertThat(colors.isAllColors()).isFalse();
    }

    @Nested
    @DisplayName("toColors() collector")
    class ToColorsCollector {

        @Test
        void collectsEmptyStream() {
            var colors = Stream.<Color>empty().collect(Colors.toColors());

            MTGAssertions.assertThat(colors).isEmpty();
        }

        @Test
        void collectsSingleElement() {
            var colors = Stream.of(Color.RED).collect(Colors.toColors());

            MTGAssertions.assertThat(colors).hasCount(1).contains(Color.RED);
        }

        @Test
        void collectsMultipleElements() {
            var colors = Stream.of(Color.WHITE, Color.BLUE, Color.BLACK).collect(Colors.toColors());

            MTGAssertions.assertThat(colors)
                    .hasCount(3)
                    .contains(Color.WHITE)
                    .contains(Color.BLUE)
                    .contains(Color.BLACK);
        }

        @Test
        void deduplicatesDuplicates() {
            var colors = Stream.of(Color.RED, Color.RED, Color.GREEN, Color.RED).collect(Colors.toColors());

            MTGAssertions.assertThat(colors).hasCount(2).contains(Color.RED).contains(Color.GREEN);
        }

        @Test
        void worksWithParallelStream() {
            var colors = Stream.of(Color.WHITE, Color.BLUE, Color.BLACK, Color.RED, Color.GREEN)
                    .parallel()
                    .collect(Colors.toColors());

            MTGAssertions.assertThat(colors).hasCount(5);
        }
    }
}
