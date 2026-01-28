package be.imgn.mtg.engine.ability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AbilitiesTest {

    @Test
    void emptyAbilities() {
        var abilities = Abilities.empty();

        assertThat(abilities.isEmpty()).isTrue();
        assertThat(abilities.count()).isEqualTo(0);
    }

    @Test
    void singleAbility() {
        var ability = mock(StaticAbility.class);
        var abilities = Abilities.of(ability);

        assertThat(abilities.isEmpty()).isFalse();
        assertThat(abilities.count()).isEqualTo(1);
        assertThat(abilities.contains(ability)).isTrue();
    }

    @Test
    void multipleAbilities() {
        var ability1 = mock(StaticAbility.class);
        var ability2 = mock(StaticAbility.class);
        var abilities = Abilities.of(ability1, ability2);

        assertThat(abilities.count()).isEqualTo(2);
        assertThat(abilities.contains(ability1)).isTrue();
        assertThat(abilities.contains(ability2)).isTrue();
    }

    @Test
    void builderAddsAbilities() {
        var ability1 = mock(StaticAbility.class);
        var ability2 = mock(StaticAbility.class);
        var abilities = Abilities.builder().add(ability1).add(ability2).build();

        assertThat(abilities.count()).isEqualTo(2);
    }

    @Test
    void toBuilderCopiesAbilities() {
        var ability1 = mock(StaticAbility.class);
        var ability2 = mock(StaticAbility.class);
        var original = Abilities.of(ability1);
        var modified = original.toBuilder().add(ability2).build();

        assertThat(original.count()).isEqualTo(1);
        assertThat(modified.count()).isEqualTo(2);
    }

    @Nested
    @DisplayName("toAbilities() collector")
    class ToAbilitiesCollector {

        @Test
        void collectsEmptyStream() {
            var abilities = Stream.<Ability>empty().collect(Abilities.toAbilities());

            assertThat(abilities.isEmpty()).isTrue();
        }

        @Test
        void collectsSingleElement() {
            var ability = mock(StaticAbility.class);
            var abilities = Stream.of(ability).collect(Abilities.toAbilities());

            assertThat(abilities.count()).isEqualTo(1);
            assertThat(abilities.contains(ability)).isTrue();
        }

        @Test
        void collectsMultipleElements() {
            var ability1 = mock(StaticAbility.class);
            var ability2 = mock(StaticAbility.class);
            var ability3 = mock(StaticAbility.class);
            var abilities = Stream.of(ability1, ability2, ability3).collect(Abilities.toAbilities());

            assertThat(abilities.count()).isEqualTo(3);
            assertThat(abilities.contains(ability1)).isTrue();
            assertThat(abilities.contains(ability2)).isTrue();
            assertThat(abilities.contains(ability3)).isTrue();
        }

        @Test
        void deduplicatesDuplicates() {
            var ability = mock(StaticAbility.class);
            var abilities = Stream.of(ability, ability, ability).collect(Abilities.toAbilities());

            assertThat(abilities.count()).isEqualTo(1);
        }

        @Test
        void worksWithParallelStream() {
            var ability1 = mock(StaticAbility.class);
            var ability2 = mock(StaticAbility.class);
            var ability3 = mock(StaticAbility.class);
            var ability4 = mock(StaticAbility.class);
            var ability5 = mock(StaticAbility.class);
            var abilities = Stream.of(ability1, ability2, ability3, ability4, ability5)
                    .parallel()
                    .collect(Abilities.toAbilities());

            assertThat(abilities.count()).isEqualTo(5);
        }
    }
}
