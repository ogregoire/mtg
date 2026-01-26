package be.imgn.mtg.engine.characteristics.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.assertions.MTGAssertions;
import be.imgn.mtg.engine.characteristics.Type;

class DefaultTypesTest {

    @Test
    void emptyArrayCreatesEmpty() {
        var types = DefaultTypes.of();

        MTGAssertions.assertThat(types).isEmpty();
        MTGAssertions.assertThat(types).isSameAs(DefaultTypes.empty());
    }

    @Test
    void singleTypeNotEmpty() {
        var types = DefaultTypes.of(Type.CREATURE);

        MTGAssertions.assertThat(types).isNotEmpty();
        MTGAssertions.assertThat(types).isNotSameAs(DefaultTypes.empty());
    }

    @Test
    void equalsWithDifferentType() {
        var types = DefaultTypes.of(Type.ENCHANTMENT);
        var notTypes = "not a Types object";

        MTGAssertions.assertThat(types).isNotEqualTo(notTypes);
    }

    @Test
    void equalsWithNull() {
        var types = DefaultTypes.of(Type.LAND);

        MTGAssertions.assertThat(types).isNotEqualTo(null);
    }

    @Test
    void equalTypesAreEqual() {
        var types1 = DefaultTypes.of(Type.INSTANT, Type.SORCERY);
        var types2 = DefaultTypes.of(Type.INSTANT, Type.SORCERY);

        assertThat(types1).isEqualTo(types2);
        assertThat(types1.hashCode()).isEqualTo(types2.hashCode());
    }

    @Test
    void differentTypesNotEqual() {
        var types1 = DefaultTypes.of(Type.CREATURE);
        var types2 = DefaultTypes.of(Type.ARTIFACT);

        assertThat(types1).isNotEqualTo(types2);
    }

    @Test
    void builderWithEmptyReturnsEmpty() {
        var types = DefaultTypes.builder().build();

        MTGAssertions.assertThat(types).isEmpty();
        MTGAssertions.assertThat(types).isSameAs(DefaultTypes.empty());
    }

    @Test
    void builderWithElementsNotEmpty() {
        var types = DefaultTypes.builder().add(Type.PLANESWALKER).build();

        MTGAssertions.assertThat(types).isNotEmpty();
        MTGAssertions.assertThat(types).isNotSameAs(DefaultTypes.empty());
    }

    @Test
    void builderClearReturnsEmpty() {
        var types = DefaultTypes.builder().add(Type.CREATURE).clear().build();

        MTGAssertions.assertThat(types).isEmpty();
        MTGAssertions.assertThat(types).isSameAs(DefaultTypes.empty());
    }

    @Test
    void builderAddAllWithVarargs() {
        var types = DefaultTypes.builder()
                .addAll(Type.ARTIFACT, Type.CREATURE, Type.ENCHANTMENT)
                .build();

        MTGAssertions.assertThat(types).hasCount(3);
    }

    @Test
    void builderAddAllWithTypes() {
        var initial = DefaultTypes.of(Type.LAND, Type.CREATURE);
        var types = DefaultTypes.builder().addAll(initial).build();

        MTGAssertions.assertThat(types).hasCount(2);
    }

    @Test
    void builderSetSingleElement() {
        var types = DefaultTypes.builder().add(Type.CREATURE).set(Type.ARTIFACT).build();

        MTGAssertions.assertThat(types).hasCount(1).contains(Type.ARTIFACT);
    }

    @Test
    void builderSetMultipleElements() {
        var types = DefaultTypes.builder()
                .add(Type.CREATURE)
                .set(Type.INSTANT, Type.SORCERY)
                .build();

        MTGAssertions.assertThat(types).hasCount(2).contains(Type.INSTANT).contains(Type.SORCERY);
    }

    @Test
    void toStringReturnsElementsString() {
        var types = DefaultTypes.of(Type.CREATURE);

        assertThat(types.toString()).contains("CREATURE");
    }

    @Test
    void iteratorReturnsElements() {
        var types = DefaultTypes.of(Type.ARTIFACT, Type.CREATURE);
        var iterator = types.iterator();

        assertThat(iterator).hasNext();
    }

    @Test
    void streamReturnsElements() {
        var types = DefaultTypes.of(Type.LAND, Type.CREATURE);
        var stream = types.stream();

        assertThat(stream).hasSize(2);
    }
}
