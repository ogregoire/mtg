package be.imgn.mtg.engine.characteristics.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.BasicLandType;
import be.imgn.mtg.engine.characteristics.CreatureType;
import be.imgn.mtg.engine.characteristics.PlaneswalkerType;
import be.imgn.mtg.engine.characteristics.Supertype;
import be.imgn.mtg.engine.characteristics.Type;

@DisplayName("TypeLineParser")
class TypeLineParserTest {

    @Nested
    @DisplayName("Simple type lines")
    class SimpleTypeLines {

        @Test
        @DisplayName("\"Instant\" → Types: [INSTANT]")
        void instant() {
            var result = TypeLineParser.parse("Instant");
            assertThat(result.types()).contains(Type.INSTANT);
            assertThat(result.supertypes().isEmpty()).isTrue();
            assertThat(result.subtypes().isEmpty()).isTrue();
        }

        @Test
        @DisplayName("\"Sorcery\" → Types: [SORCERY]")
        void sorcery() {
            var result = TypeLineParser.parse("Sorcery");
            assertThat(result.types()).contains(Type.SORCERY);
        }
    }

    @Nested
    @DisplayName("Types with subtypes")
    class TypesWithSubtypes {

        @Test
        @DisplayName("\"Creature — Human Soldier\"")
        void creatureSubtypes() {
            var result = TypeLineParser.parse("Creature — Human Soldier");
            assertThat(result.types()).contains(Type.CREATURE);
            assertThat(result.subtypes()).contains(CreatureType.HUMAN, CreatureType.SOLDIER);
        }

        @Test
        @DisplayName("\"Land — Forest\"")
        void basicLand() {
            var result = TypeLineParser.parse("Land — Forest");
            assertThat(result.types()).contains(Type.LAND);
            assertThat(result.subtypes()).contains(BasicLandType.FOREST);
        }
    }

    @Nested
    @DisplayName("Supertypes")
    class SupertypeTests {

        @Test
        @DisplayName("\"Legendary Creature — Human Soldier\"")
        void legendary() {
            var result = TypeLineParser.parse("Legendary Creature — Human Soldier");
            assertThat(result.supertypes()).contains(Supertype.LEGENDARY);
            assertThat(result.types()).contains(Type.CREATURE);
            assertThat(result.subtypes()).contains(CreatureType.HUMAN, CreatureType.SOLDIER);
        }

        @Test
        @DisplayName("\"Basic Land — Forest\"")
        void basicLand() {
            var result = TypeLineParser.parse("Basic Land — Forest");
            assertThat(result.supertypes()).contains(Supertype.BASIC);
            assertThat(result.types()).contains(Type.LAND);
            assertThat(result.subtypes()).contains(BasicLandType.FOREST);
        }
    }

    @Nested
    @DisplayName("Multiple types")
    class MultipleTypes {

        @Test
        @DisplayName("\"Artifact Creature — Golem\"")
        void artifactCreature() {
            var result = TypeLineParser.parse("Artifact Creature — Golem");
            assertThat(result.types()).contains(Type.ARTIFACT, Type.CREATURE);
            assertThat(result.subtypes()).contains(CreatureType.GOLEM);
        }

        @Test
        @DisplayName("\"Legendary Artifact Creature — Golem\"")
        void legendaryArtifactCreature() {
            var result = TypeLineParser.parse("Legendary Artifact Creature — Golem");
            assertThat(result.supertypes()).contains(Supertype.LEGENDARY);
            assertThat(result.types()).contains(Type.ARTIFACT, Type.CREATURE);
            assertThat(result.subtypes()).contains(CreatureType.GOLEM);
        }
    }

    @Nested
    @DisplayName("Planeswalker")
    class PlaneswalkerTests {

        @Test
        @DisplayName("\"Legendary Planeswalker — Jace\"")
        void planeswalker() {
            var result = TypeLineParser.parse("Legendary Planeswalker — Jace");
            assertThat(result.supertypes()).contains(Supertype.LEGENDARY);
            assertThat(result.types()).contains(Type.PLANESWALKER);
            assertThat(result.subtypes()).contains(PlaneswalkerType.JACE);
        }
    }
}
