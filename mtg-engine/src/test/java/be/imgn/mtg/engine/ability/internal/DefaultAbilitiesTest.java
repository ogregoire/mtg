package be.imgn.mtg.engine.ability.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.Ability;
import be.imgn.mtg.engine.ability.AbilityId;
import be.imgn.mtg.engine.ability.StaticAbility;

@DisplayName("DefaultAbilities")
class DefaultAbilitiesTest {

    @Nested
    @DisplayName("empty()")
    class EmptyTests {

        @Test
        void returnsEmptyAbilities() {
            var abilities = DefaultAbilities.empty();

            assertThat(abilities).isEmpty();
        }

        @Test
        void returnsSameInstance() {
            var first = DefaultAbilities.empty();
            var second = DefaultAbilities.empty();

            assertThat(first).isSameAs(second);
        }
    }

    @Nested
    @DisplayName("of()")
    class OfTests {

        @Test
        void withNoArguments_returnsEmpty() {
            var abilities = DefaultAbilities.of();

            assertThat(abilities).isEmpty();
        }

        @Test
        void withNoArguments_returnsSameInstanceAsEmpty() {
            var abilities = DefaultAbilities.of();
            var empty = DefaultAbilities.empty();

            assertThat(abilities).isSameAs(empty);
        }

        @Test
        void withOneAbility_containsThatAbility() {
            var ability = testAbility("Ability 1");

            var abilities = DefaultAbilities.of(ability);

            assertThat(abilities).containsExactly(ability);
            assertThat(abilities.count()).isEqualTo(1);
        }

        @Test
        void withMultipleAbilities_containsAllAbilities() {
            var ability1 = testAbility("Ability 1");
            var ability2 = testAbility("Ability 2");
            var ability3 = testAbility("Ability 3");

            var abilities = DefaultAbilities.of(ability1, ability2, ability3);

            assertThat(abilities).containsExactly(ability1, ability2, ability3);
            assertThat(abilities.count()).isEqualTo(3);
        }

        @Test
        void withDuplicateAbilities_removesduplicates() {
            var ability = testAbility("Ability 1");

            var abilities = DefaultAbilities.of(ability, ability, ability);

            assertThat(abilities).containsExactly(ability);
            assertThat(abilities.count()).isEqualTo(1);
        }

        @Test
        void preservesInsertionOrder() {
            var ability1 = testAbility("Ability 1");
            var ability2 = testAbility("Ability 2");
            var ability3 = testAbility("Ability 3");

            var abilities = DefaultAbilities.of(ability3, ability1, ability2);

            assertThat(abilities).containsExactly(ability3, ability1, ability2);
        }
    }

    @Nested
    @DisplayName("builder()")
    class BuilderTests {

        @Test
        void buildEmpty_returnsEmpty() {
            var abilities = DefaultAbilities.builder().build();

            assertThat(abilities).isEmpty();
        }

        @Test
        void addSingle_containsAbility() {
            var ability = testAbility("Ability 1");

            var abilities = DefaultAbilities.builder().add(ability).build();

            assertThat(abilities).containsExactly(ability);
        }

        @Test
        void addMultiple_containsAllAbilities() {
            var ability1 = testAbility("Ability 1");
            var ability2 = testAbility("Ability 2");

            var abilities =
                    DefaultAbilities.builder().add(ability1).add(ability2).build();

            assertThat(abilities).containsExactly(ability1, ability2);
        }

        @Test
        void addVarargs_containsAllAbilities() {
            var ability1 = testAbility("Ability 1");
            var ability2 = testAbility("Ability 2");
            var ability3 = testAbility("Ability 3");

            var abilities = DefaultAbilities.builder()
                    .addAll(ability1, ability2, ability3)
                    .build();

            assertThat(abilities).containsExactly(ability1, ability2, ability3);
        }

        @Test
        void addDuplicates_removesduplicates() {
            var ability = testAbility("Ability 1");

            var abilities = DefaultAbilities.builder().add(ability).add(ability).build();

            assertThat(abilities).containsExactly(ability);
            assertThat(abilities.count()).isEqualTo(1);
        }

        @Test
        void clear_removesAllAbilities() {
            var ability1 = testAbility("Ability 1");
            var ability2 = testAbility("Ability 2");

            var abilities = DefaultAbilities.builder()
                    .add(ability1)
                    .add(ability2)
                    .clear()
                    .build();

            assertThat(abilities).isEmpty();
        }
    }

    @Nested
    @DisplayName("toBuilder()")
    class ToBuilderTests {

        @Test
        void fromEmpty_createsBuilderThatBuildsEmpty() {
            var abilities = DefaultAbilities.empty().toBuilder().build();

            assertThat(abilities).isEmpty();
        }

        @Test
        void preservesExistingAbilities() {
            var ability1 = testAbility("Ability 1");
            var ability2 = testAbility("Ability 2");
            var original = DefaultAbilities.of(ability1, ability2);

            var rebuilt = original.toBuilder().build();

            assertThat(rebuilt).containsExactly(ability1, ability2);
        }

        @Test
        void allowsModification() {
            var ability1 = testAbility("Ability 1");
            var ability2 = testAbility("Ability 2");
            var ability3 = testAbility("Ability 3");
            var original = DefaultAbilities.of(ability1, ability2);

            var modified = original.toBuilder().add(ability3).build();

            assertThat(modified).containsExactly(ability1, ability2, ability3);
            assertThat(original).containsExactly(ability1, ability2); // Original unchanged
        }
    }

    @Nested
    @DisplayName("contains()")
    class ContainsTests {

        @Test
        void returnsTrueForPresentAbility() {
            var ability = testAbility("Ability 1");
            var abilities = DefaultAbilities.of(ability);

            assertThat(abilities.contains(ability)).isTrue();
        }

        @Test
        void returnsFalseForAbsentAbility() {
            var ability1 = testAbility("Ability 1");
            var ability2 = testAbility("Ability 2");
            var abilities = DefaultAbilities.of(ability1);

            assertThat(abilities.contains(ability2)).isFalse();
        }

        @Test
        void returnsFalseForEmptyAbilities() {
            var ability = testAbility("Ability 1");
            var abilities = DefaultAbilities.empty();

            assertThat(abilities.contains(ability)).isFalse();
        }
    }

    @Nested
    @DisplayName("isEmpty()")
    class IsEmptyTests {

        @Test
        void returnsTrueForEmpty() {
            var abilities = DefaultAbilities.empty();

            assertThat(abilities.isEmpty()).isTrue();
        }

        @Test
        void returnsFalseForNonEmpty() {
            var ability = testAbility("Ability 1");
            var abilities = DefaultAbilities.of(ability);

            assertThat(abilities.isEmpty()).isFalse();
        }
    }

    @Nested
    @DisplayName("stream()")
    class StreamTests {

        @Test
        void emptyAbilities_producesEmptyStream() {
            var abilities = DefaultAbilities.empty();

            assertThat(abilities.stream()).isEmpty();
        }

        @Test
        void nonEmptyAbilities_producesStream() {
            var ability1 = testAbility("Ability 1");
            var ability2 = testAbility("Ability 2");
            var abilities = DefaultAbilities.of(ability1, ability2);

            assertThat(abilities.stream()).containsExactly(ability1, ability2);
        }
    }

    @Nested
    @DisplayName("iterator()")
    class IteratorTests {

        @Test
        void emptyAbilities_producesEmptyIterator() {
            var abilities = DefaultAbilities.empty();

            assertThat(abilities).isEmpty();
        }

        @Test
        void nonEmptyAbilities_producesIterator() {
            var ability1 = testAbility("Ability 1");
            var ability2 = testAbility("Ability 2");
            var abilities = DefaultAbilities.of(ability1, ability2);

            assertThat(abilities).containsExactly(ability1, ability2);
        }

        @Test
        void iteratorPreservesOrder() {
            var ability1 = testAbility("Ability 1");
            var ability2 = testAbility("Ability 2");
            var ability3 = testAbility("Ability 3");
            var abilities = DefaultAbilities.of(ability1, ability2, ability3);

            assertThat(abilities).containsExactly(ability1, ability2, ability3);
        }
    }

    // Helper method to create test abilities
    private Ability testAbility(String text) {
        var ability = mock(StaticAbility.class);
        when(ability.id()).thenReturn(new AbilityId());
        when(ability.oracleText()).thenReturn(text);
        return ability;
    }
}
