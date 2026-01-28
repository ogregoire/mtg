package be.imgn.mtg.engine.characteristics;

import static be.imgn.mtg.engine.assertions.MTGAssertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SupertypesTest {

    @Test
    void emptySupertypes() {
        var supertypes = Supertypes.empty();

        assertThat(supertypes).isEmpty().hasCount(0);
    }

    @Test
    void singleSupertype() {
        var supertypes = Supertypes.of(Supertype.LEGENDARY);

        assertThat(supertypes)
                .isNotEmpty()
                .hasCount(1)
                .contains(Supertype.LEGENDARY)
                .isLegendary();
    }

    @Test
    void multipleSupertypes() {
        var supertypes = Supertypes.of(Supertype.LEGENDARY, Supertype.SNOW);

        assertThat(supertypes).hasCount(2).isLegendary().isSnow();
    }

    @Test
    void builderAddsSupertypes() {
        var supertypes =
                Supertypes.builder().add(Supertype.BASIC).add(Supertype.SNOW).build();

        assertThat(supertypes).hasCount(2).containsExactly(Supertype.BASIC, Supertype.SNOW);
    }

    @Test
    void toBuilderCopiesSupertypes() {
        var original = Supertypes.of(Supertype.LEGENDARY);
        var modified = original.toBuilder().add(Supertype.SNOW).build();

        assertThat(original).hasCount(1);
        assertThat(modified).hasCount(2);
    }

    @Nested
    @DisplayName("toSupertypes() collector")
    class ToSupertypesCollector {

        @Test
        void collectsEmptyStream() {
            var supertypes = Stream.<Supertype>empty().collect(Supertypes.toSupertypes());

            assertThat(supertypes).isEmpty();
        }

        @Test
        void collectsSingleElement() {
            var supertypes = Stream.of(Supertype.LEGENDARY).collect(Supertypes.toSupertypes());

            assertThat(supertypes).hasCount(1).contains(Supertype.LEGENDARY);
        }

        @Test
        void collectsMultipleElements() {
            var supertypes = Stream.of(Supertype.LEGENDARY, Supertype.SNOW, Supertype.BASIC)
                    .collect(Supertypes.toSupertypes());

            assertThat(supertypes)
                    .hasCount(3)
                    .contains(Supertype.LEGENDARY)
                    .contains(Supertype.SNOW)
                    .contains(Supertype.BASIC);
        }

        @Test
        void deduplicatesDuplicates() {
            var supertypes = Stream.of(Supertype.LEGENDARY, Supertype.LEGENDARY, Supertype.SNOW)
                    .collect(Supertypes.toSupertypes());

            assertThat(supertypes).hasCount(2).contains(Supertype.LEGENDARY).contains(Supertype.SNOW);
        }

        @Test
        void worksWithParallelStream() {
            var supertypes = Stream.of(Supertype.BASIC, Supertype.LEGENDARY, Supertype.SNOW, Supertype.WORLD)
                    .parallel()
                    .collect(Supertypes.toSupertypes());

            assertThat(supertypes).hasCount(4);
        }
    }
}
