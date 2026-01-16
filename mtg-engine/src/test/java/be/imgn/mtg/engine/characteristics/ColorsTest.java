package be.imgn.mtg.engine.characteristics;

import static be.imgn.mtg.engine.assertions.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ColorsTest {

    @Test
    void emptyColors() {
        var colors = Colors.empty();

        assertThat(colors).isEmpty().isColorless().hasCount(0);
    }

    @Test
    void singleColor() {
        var colors = Colors.of(Color.RED);

        assertThat(colors)
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

        assertThat(colors)
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

        assertThat(colors).hasCount(5).isWhite().isBlue().isBlack().isRed().isGreen();
    }

    @Test
    void builderAddsColors() {
        var colors = Colors.builder().add(Color.WHITE).add(Color.BLACK).build();

        assertThat(colors).hasCount(2).containsExactly(Color.WHITE, Color.BLACK);
    }

    @Test
    void builderClearsColors() {
        var colors = Colors.builder().add(Color.RED).clear().add(Color.GREEN).build();

        assertThat(colors).hasCount(1).contains(Color.GREEN).doesNotContain(Color.RED);
    }

    @Test
    void toBuilderCopiesColors() {
        var original = Colors.of(Color.BLUE, Color.BLACK);
        var modified = original.toBuilder().add(Color.RED).build();

        assertThat(original).hasCount(2);
        assertThat(modified)
                .hasCount(3)
                .contains(Color.BLUE)
                .contains(Color.BLACK)
                .contains(Color.RED);
    }

    @Test
    void duplicateColorsAreIgnored() {
        var colors = Colors.of(Color.RED, Color.RED, Color.RED);

        assertThat(colors).hasCount(1).contains(Color.RED);
    }
}
