package be.imgn.mtg.engine.characteristics.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.assertions.MTGAssertions;
import be.imgn.mtg.engine.characteristics.Color;

class DefaultColorsTest {

    @Test
    void emptyArrayCreatesEmpty() {
        var colors = DefaultColors.of();

        MTGAssertions.assertThat(colors).isEmpty();
        MTGAssertions.assertThat(colors).isSameAs(DefaultColors.empty());
    }

    @Test
    void singleColorNotEmpty() {
        var colors = DefaultColors.of(Color.RED);

        MTGAssertions.assertThat(colors).isNotEmpty();
        MTGAssertions.assertThat(colors).isNotSameAs(DefaultColors.empty());
    }

    @Test
    void equalsWithDifferentType() {
        var colors = DefaultColors.of(Color.GREEN);
        var notColors = "not a Colors object";

        MTGAssertions.assertThat(colors).isNotEqualTo(notColors);
    }

    @Test
    void equalsWithNull() {
        var colors = DefaultColors.of(Color.WHITE);

        MTGAssertions.assertThat(colors).isNotEqualTo(null);
    }

    @Test
    void equalColorsAreEqual() {
        var colors1 = DefaultColors.of(Color.RED, Color.GREEN);
        var colors2 = DefaultColors.of(Color.RED, Color.GREEN);

        assertThat(colors1).isEqualTo(colors2);
        assertThat(colors1.hashCode()).isEqualTo(colors2.hashCode());
    }

    @Test
    void differentColorsNotEqual() {
        var colors1 = DefaultColors.of(Color.RED);
        var colors2 = DefaultColors.of(Color.BLUE);

        assertThat(colors1).isNotEqualTo(colors2);
    }

    @Test
    void builderWithEmptyReturnsEmpty() {
        var colors = DefaultColors.builder().build();

        MTGAssertions.assertThat(colors).isEmpty();
        MTGAssertions.assertThat(colors).isSameAs(DefaultColors.empty());
    }

    @Test
    void builderWithElementsNotEmpty() {
        var colors = DefaultColors.builder().add(Color.BLACK).build();

        MTGAssertions.assertThat(colors).isNotEmpty();
        MTGAssertions.assertThat(colors).isNotSameAs(DefaultColors.empty());
    }

    @Test
    void builderClearReturnsEmpty() {
        var colors = DefaultColors.builder().add(Color.RED).clear().build();

        MTGAssertions.assertThat(colors).isEmpty();
        MTGAssertions.assertThat(colors).isSameAs(DefaultColors.empty());
    }

    @Test
    void builderAddAllWithVarargs() {
        var colors = DefaultColors.builder()
                .addAll(Color.WHITE, Color.BLUE, Color.BLACK)
                .build();

        MTGAssertions.assertThat(colors).hasCount(3);
    }

    @Test
    void builderAddAllWithColors() {
        var initial = DefaultColors.of(Color.RED, Color.GREEN);
        var colors = DefaultColors.builder().addAll(initial).build();

        MTGAssertions.assertThat(colors).hasCount(2);
    }

    @Test
    void builderSetSingleElement() {
        var colors = DefaultColors.builder().add(Color.RED).set(Color.BLUE).build();

        MTGAssertions.assertThat(colors).hasCount(1).contains(Color.BLUE);
    }

    @Test
    void builderSetMultipleElements() {
        var colors = DefaultColors.builder()
                .add(Color.RED)
                .set(Color.BLUE, Color.BLACK)
                .build();

        MTGAssertions.assertThat(colors).hasCount(2).contains(Color.BLUE).contains(Color.BLACK);
    }

    @Test
    void toStringReturnsElementsString() {
        var colors = DefaultColors.of(Color.RED);

        assertThat(colors.toString()).contains("RED");
    }

    @Test
    void iteratorReturnsElements() {
        var colors = DefaultColors.of(Color.WHITE, Color.BLUE);
        var iterator = colors.iterator();

        assertThat(iterator).hasNext();
    }

    @Test
    void streamReturnsElements() {
        var colors = DefaultColors.of(Color.GREEN, Color.RED);
        var stream = colors.stream();

        assertThat(stream).hasSize(2);
    }
}
