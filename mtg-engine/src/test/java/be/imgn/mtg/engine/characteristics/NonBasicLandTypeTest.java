package be.imgn.mtg.engine.characteristics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("NonBasicLandType")
class NonBasicLandTypeTest {

    @Nested
    @DisplayName("enum values")
    class EnumValues {

        @Test
        void hasCaveType() {
            assertThat(NonBasicLandType.valueOf("CAVE")).isEqualTo(NonBasicLandType.CAVE);
        }

        @Test
        void hasDesertType() {
            assertThat(NonBasicLandType.valueOf("DESERT")).isEqualTo(NonBasicLandType.DESERT);
        }

        @Test
        void hasGateType() {
            assertThat(NonBasicLandType.valueOf("GATE")).isEqualTo(NonBasicLandType.GATE);
        }

        @Test
        void hasLairType() {
            assertThat(NonBasicLandType.valueOf("LAIR")).isEqualTo(NonBasicLandType.LAIR);
        }

        @Test
        void hasLocusType() {
            assertThat(NonBasicLandType.valueOf("LOCUS")).isEqualTo(NonBasicLandType.LOCUS);
        }

        @Test
        void hasMineType() {
            assertThat(NonBasicLandType.valueOf("MINE")).isEqualTo(NonBasicLandType.MINE);
        }

        @Test
        void hasPowerPlantType() {
            assertThat(NonBasicLandType.valueOf("POWER_PLANT")).isEqualTo(NonBasicLandType.POWER_PLANT);
        }

        @Test
        void hasSphereType() {
            assertThat(NonBasicLandType.valueOf("SPHERE")).isEqualTo(NonBasicLandType.SPHERE);
        }

        @Test
        void hasTowerType() {
            assertThat(NonBasicLandType.valueOf("TOWER")).isEqualTo(NonBasicLandType.TOWER);
        }

        @Test
        void hasUrzasType() {
            assertThat(NonBasicLandType.valueOf("URZAS")).isEqualTo(NonBasicLandType.URZAS);
        }

        @Test
        void hasExactlyTwelveValues() {
            assertThat(NonBasicLandType.values()).hasSize(12);
        }
    }

    @Nested
    @DisplayName("implements LandType")
    class ImplementsLandType {

        @ParameterizedTest
        @EnumSource(NonBasicLandType.class)
        void isInstanceOfLandType(NonBasicLandType landType) {
            LandType type = landType;

            assertThat(type).isInstanceOf(LandType.class);
        }

        @Test
        void canBeUsedAsLandType() {
            LandType landType = NonBasicLandType.GATE;

            assertThat(landType).isEqualTo(NonBasicLandType.GATE);
        }
    }

    @Nested
    @DisplayName("implements Subtype")
    class ImplementsSubtype {

        @ParameterizedTest
        @EnumSource(NonBasicLandType.class)
        void isInstanceOfSubtype(NonBasicLandType landType) {
            Subtype subtype = landType;

            assertThat(subtype).isInstanceOf(Subtype.class);
        }

        @Test
        void canBeUsedAsSubtype() {
            Subtype subtype = NonBasicLandType.DESERT;

            assertThat(subtype).isEqualTo(NonBasicLandType.DESERT);
        }
    }

    @Nested
    @DisplayName("valueOf")
    class ValueOf {

        @ParameterizedTest
        @EnumSource(NonBasicLandType.class)
        void returnsCorrectEnumForName(NonBasicLandType landType) {
            var result = NonBasicLandType.valueOf(landType.name());

            assertThat(result).isEqualTo(landType);
        }

        @Test
        void isCaseSensitive() {
            assertThat(NonBasicLandType.valueOf("CAVE")).isEqualTo(NonBasicLandType.CAVE);
        }
    }

    @Nested
    @DisplayName("values")
    class Values {

        @Test
        void returnsAllNonBasicLandTypes() {
            var values = NonBasicLandType.values();

            assertThat(values)
                    .containsExactlyInAnyOrder(
                            NonBasicLandType.CAVE,
                            NonBasicLandType.DESERT,
                            NonBasicLandType.GATE,
                            NonBasicLandType.LAIR,
                            NonBasicLandType.LOCUS,
                            NonBasicLandType.MINE,
                            NonBasicLandType.PLANET,
                            NonBasicLandType.POWER_PLANT,
                            NonBasicLandType.SPHERE,
                            NonBasicLandType.TOWER,
                            NonBasicLandType.TOWN,
                            NonBasicLandType.URZAS);
        }

        @Test
        void returnsNewArrayEachTime() {
            var values1 = NonBasicLandType.values();
            var values2 = NonBasicLandType.values();

            assertThat(values1).isNotSameAs(values2);
        }
    }

    @Nested
    @DisplayName("name")
    class Name {

        @Test
        void caveNameIsCAVE() {
            assertThat(NonBasicLandType.CAVE.name()).isEqualTo("CAVE");
        }

        @Test
        void desertNameIsDESERT() {
            assertThat(NonBasicLandType.DESERT.name()).isEqualTo("DESERT");
        }

        @Test
        void gateNameIsGATE() {
            assertThat(NonBasicLandType.GATE.name()).isEqualTo("GATE");
        }

        @Test
        void lairNameIsLAIR() {
            assertThat(NonBasicLandType.LAIR.name()).isEqualTo("LAIR");
        }

        @Test
        void locusNameIsLOCUS() {
            assertThat(NonBasicLandType.LOCUS.name()).isEqualTo("LOCUS");
        }

        @Test
        void mineNameIsMINE() {
            assertThat(NonBasicLandType.MINE.name()).isEqualTo("MINE");
        }

        @Test
        void powerPlantNameIsPOWER_PLANT() {
            assertThat(NonBasicLandType.POWER_PLANT.name()).isEqualTo("POWER_PLANT");
        }

        @Test
        void sphereNameIsSPHERE() {
            assertThat(NonBasicLandType.SPHERE.name()).isEqualTo("SPHERE");
        }

        @Test
        void towerNameIsTOWER() {
            assertThat(NonBasicLandType.TOWER.name()).isEqualTo("TOWER");
        }

        @Test
        void urzasNameIsURZAS() {
            assertThat(NonBasicLandType.URZAS.name()).isEqualTo("URZAS");
        }
    }

    @Nested
    @DisplayName("equality")
    class Equality {

        @Test
        void differentEnumValuesAreNotEqual() {
            assertThat(NonBasicLandType.GATE).isNotEqualTo(NonBasicLandType.DESERT);
        }
    }

    @Nested
    @DisplayName("hashCode")
    class HashCode {

        @ParameterizedTest
        @EnumSource(NonBasicLandType.class)
        void isConsistent(NonBasicLandType landType) {
            var hashCode1 = landType.hashCode();
            var hashCode2 = landType.hashCode();

            assertThat(hashCode1).isEqualTo(hashCode2);
        }

        @Test
        void differentValuesHaveDifferentHashCodes() {
            assertThat(NonBasicLandType.MINE.hashCode()).isNotEqualTo(NonBasicLandType.TOWER.hashCode());
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @ParameterizedTest
        @EnumSource(NonBasicLandType.class)
        void returnsEnumName(NonBasicLandType landType) {
            assertThat(landType.toString()).isEqualTo(landType.name());
        }
    }

    @Nested
    @DisplayName("Urza's lands")
    class UrzasLands {

        @Test
        void containsAllUrzasLandTypes() {
            var values = NonBasicLandType.values();

            assertThat(values)
                    .contains(
                            NonBasicLandType.MINE,
                            NonBasicLandType.POWER_PLANT,
                            NonBasicLandType.TOWER,
                            NonBasicLandType.URZAS);
        }

        @Test
        void urzasSphereIsUrzasType() {
            // Urza's can be combined with other types like "Urza's Sphere"
            assertThat(NonBasicLandType.URZAS).isInstanceOf(LandType.class);
            assertThat(NonBasicLandType.SPHERE).isInstanceOf(LandType.class);
        }
    }
}
