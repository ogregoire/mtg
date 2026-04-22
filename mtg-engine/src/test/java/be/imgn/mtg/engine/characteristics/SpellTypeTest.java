package be.imgn.mtg.engine.characteristics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("SpellType")
class SpellTypeTest {

    @Nested
    @DisplayName("enum values")
    class EnumValues {

        @Test
        void hasAdventureType() {
            assertThat(SpellType.valueOf("ADVENTURE")).isEqualTo(SpellType.ADVENTURE);
        }

        @Test
        void hasArcaneType() {
            assertThat(SpellType.valueOf("ARCANE")).isEqualTo(SpellType.ARCANE);
        }

        @Test
        void hasChorusType() {
            assertThat(SpellType.valueOf("CHORUS")).isEqualTo(SpellType.CHORUS);
        }

        @Test
        void hasLessonType() {
            assertThat(SpellType.valueOf("LESSON")).isEqualTo(SpellType.LESSON);
        }

        @Test
        void hasTrapType() {
            assertThat(SpellType.valueOf("TRAP")).isEqualTo(SpellType.TRAP);
        }

        @Test
        void hasExactlySixValues() {
            assertThat(SpellType.values()).hasSize(6);
        }
    }

    @Nested
    @DisplayName("implements Subtype")
    class ImplementsSubtype {

        @ParameterizedTest
        @EnumSource(SpellType.class)
        void isInstanceOfSubtype(SpellType spellType) {
            Subtype subtype = spellType;

            assertThat(subtype).isInstanceOf(Subtype.class);
        }

        @Test
        void canBeUsedAsSubtype() {
            Subtype subtype = SpellType.ADVENTURE;

            assertThat(subtype).isEqualTo(SpellType.ADVENTURE);
        }
    }

    @Nested
    @DisplayName("valueOf")
    class ValueOf {

        @ParameterizedTest
        @EnumSource(SpellType.class)
        void returnsCorrectEnumForName(SpellType spellType) {
            var result = SpellType.valueOf(spellType.name());

            assertThat(result).isEqualTo(spellType);
        }

        @Test
        void isCaseSensitive() {
            assertThat(SpellType.valueOf("ADVENTURE")).isEqualTo(SpellType.ADVENTURE);
        }
    }

    @Nested
    @DisplayName("values")
    class Values {

        @Test
        void returnsAllSpellTypes() {
            var values = SpellType.values();

            assertThat(values)
                    .containsExactlyInAnyOrder(
                            SpellType.ADVENTURE,
                            SpellType.ARCANE,
                            SpellType.CHORUS,
                            SpellType.LESSON,
                            SpellType.OMEN,
                            SpellType.TRAP);
        }

        @Test
        void returnsNewArrayEachTime() {
            var values1 = SpellType.values();
            var values2 = SpellType.values();

            assertThat(values1).isNotSameAs(values2);
        }
    }

    @Nested
    @DisplayName("name")
    class Name {

        @Test
        void adventureNameIsADVENTURE() {
            assertThat(SpellType.ADVENTURE.name()).isEqualTo("ADVENTURE");
        }

        @Test
        void arcaneNameIsARCANE() {
            assertThat(SpellType.ARCANE.name()).isEqualTo("ARCANE");
        }

        @Test
        void chorusNameIsCHORUS() {
            assertThat(SpellType.CHORUS.name()).isEqualTo("CHORUS");
        }

        @Test
        void lessonNameIsLESSON() {
            assertThat(SpellType.LESSON.name()).isEqualTo("LESSON");
        }

        @Test
        void trapNameIsTRAP() {
            assertThat(SpellType.TRAP.name()).isEqualTo("TRAP");
        }
    }

    @Nested
    @DisplayName("equality")
    class Equality {

        @Test
        void differentEnumValuesAreNotEqual() {
            assertThat(SpellType.ADVENTURE).isNotEqualTo(SpellType.TRAP);
        }
    }

    @Nested
    @DisplayName("hashCode")
    class HashCode {

        @ParameterizedTest
        @EnumSource(SpellType.class)
        void isConsistent(SpellType spellType) {
            var hashCode1 = spellType.hashCode();
            var hashCode2 = spellType.hashCode();

            assertThat(hashCode1).isEqualTo(hashCode2);
        }

        @Test
        void differentValuesHaveDifferentHashCodes() {
            assertThat(SpellType.ADVENTURE.hashCode()).isNotEqualTo(SpellType.TRAP.hashCode());
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @ParameterizedTest
        @EnumSource(SpellType.class)
        void returnsEnumName(SpellType spellType) {
            assertThat(spellType.toString()).isEqualTo(spellType.name());
        }
    }
}
