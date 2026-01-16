package be.imgn.mtg.engine.characteristics;

import static be.imgn.mtg.engine.assertions.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TypesTest {

    @Test
    void emptyTypes() {
        var types = Types.empty();

        assertThat(types).isEmpty().hasCount(0);
    }

    @Test
    void singleType() {
        var types = Types.of(Type.CREATURE);

        assertThat(types)
                .isNotEmpty()
                .hasCount(1)
                .contains(Type.CREATURE)
                .isCreature()
                .isPermanentType();
    }

    @Test
    void multipleTypes() {
        var types = Types.of(Type.ARTIFACT, Type.CREATURE);

        assertThat(types).hasCount(2).isArtifact().isCreature().isPermanentType();
    }

    @Test
    void spellTypes() {
        var instant = Types.of(Type.INSTANT);
        var sorcery = Types.of(Type.SORCERY);

        assertThat(instant).isInstant().isSpellType();
        assertThat(sorcery).isSorcery().isSpellType();
    }

    @Test
    void permanentTypes() {
        assertThat(Types.of(Type.ARTIFACT)).isPermanentType();
        assertThat(Types.of(Type.CREATURE)).isPermanentType();
        assertThat(Types.of(Type.ENCHANTMENT)).isPermanentType();
        assertThat(Types.of(Type.LAND)).isPermanentType();
        assertThat(Types.of(Type.PLANESWALKER)).isPermanentType();
    }

    @Test
    void builderAddsTypes() {
        var types = Types.builder().add(Type.CREATURE).add(Type.ENCHANTMENT).build();

        assertThat(types).hasCount(2).containsExactly(Type.CREATURE, Type.ENCHANTMENT);
    }

    @Test
    void toBuilderCopiesTypes() {
        var original = Types.of(Type.ARTIFACT);
        var modified = original.toBuilder().add(Type.CREATURE).build();

        assertThat(original).hasCount(1);
        assertThat(modified).hasCount(2);
    }
}
