package be.imgn.mtg.engine.characteristics;

import static be.imgn.mtg.engine.assertions.MTGAssertions.assertThat;

import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SubtypesTest {

    @Test
    void emptySubtypes() {
        var subtypes = Subtypes.empty();

        assertThat(subtypes).isEmpty().hasCount(0);
    }

    @Test
    void singleSubtype() {
        var subtypes = Subtypes.of(CreatureType.HUMAN);

        assertThat(subtypes).isNotEmpty().hasCount(1).contains(CreatureType.HUMAN);
    }

    @Test
    void multipleSubtypes() {
        var subtypes = Subtypes.of(CreatureType.HUMAN, CreatureType.WIZARD);

        assertThat(subtypes).hasCount(2).contains(CreatureType.HUMAN).contains(CreatureType.WIZARD);
    }

    @Test
    void mixedSubtypes() {
        var subtypes = Subtypes.of(CreatureType.HUMAN, BasicLandType.FOREST);

        assertThat(subtypes).hasCount(2).contains(CreatureType.HUMAN).contains(BasicLandType.FOREST);
    }

    @Test
    void hasBasicLandType() {
        var withBasic = Subtypes.of(BasicLandType.PLAINS, CreatureType.SOLDIER);
        var withoutBasic = Subtypes.of(CreatureType.SOLDIER);

        assertThat(withBasic).hasBasicLandType();
        Assertions.assertThat(withoutBasic.hasBasicLandType()).isFalse();
    }

    @Test
    void builderAddsSubtypes() {
        var subtypes = Subtypes.builder()
                .add(CreatureType.ELF)
                .add(CreatureType.WARRIOR)
                .build();

        assertThat(subtypes).hasCount(2).containsExactly(CreatureType.ELF, CreatureType.WARRIOR);
    }

    @Test
    void toBuilderCopiesSubtypes() {
        var original = Subtypes.of(CreatureType.GOBLIN);
        var modified = original.toBuilder().add(CreatureType.SHAMAN).build();

        assertThat(original).hasCount(1);
        assertThat(modified).hasCount(2);
    }

    @Nested
    @DisplayName("toSubtypes() collector")
    class ToSubtypesCollector {

        @Test
        void collectsEmptyStream() {
            var subtypes = Stream.<Subtype>empty().collect(Subtypes.toSubtypes());

            assertThat(subtypes).isEmpty();
        }

        @Test
        void collectsSingleElement() {
            var subtypes = Stream.<Subtype>of(CreatureType.HUMAN).collect(Subtypes.toSubtypes());

            assertThat(subtypes).hasCount(1).contains(CreatureType.HUMAN);
        }

        @Test
        void collectsMultipleElements() {
            var subtypes = Stream.<Subtype>of(CreatureType.HUMAN, CreatureType.WIZARD, BasicLandType.ISLAND)
                    .collect(Subtypes.toSubtypes());

            assertThat(subtypes)
                    .hasCount(3)
                    .contains(CreatureType.HUMAN)
                    .contains(CreatureType.WIZARD)
                    .contains(BasicLandType.ISLAND);
        }

        @Test
        void deduplicatesDuplicates() {
            var subtypes = Stream.<Subtype>of(CreatureType.HUMAN, CreatureType.HUMAN, CreatureType.WIZARD)
                    .collect(Subtypes.toSubtypes());

            assertThat(subtypes).hasCount(2).contains(CreatureType.HUMAN).contains(CreatureType.WIZARD);
        }

        @Test
        void worksWithParallelStream() {
            var subtypes = Stream.<Subtype>of(
                            CreatureType.HUMAN,
                            CreatureType.WIZARD,
                            CreatureType.SOLDIER,
                            BasicLandType.PLAINS,
                            BasicLandType.ISLAND)
                    .parallel()
                    .collect(Subtypes.toSubtypes());

            assertThat(subtypes).hasCount(5);
        }
    }
}
