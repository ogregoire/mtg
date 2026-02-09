package be.imgn.mtg.engine.card;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FaceDefinitionTest {

    private static FaceDefinition buildMinimalFace(String name) {
        return CardDefinition.builder(UUID.randomUUID(), name + " // Back", CardLayout.TRANSFORM)
                .primaryFace(name)
                .endFace()
                .secondaryFace("Back")
                .endFace()
                .build()
                .primaryFace()
                .orElseThrow();
    }

    private static FaceDefinition buildFullFace() {
        return CardDefinition.builder(UUID.randomUUID(), "Front // Back", CardLayout.TRANSFORM)
                .primaryFace("Front")
                .manaCost("{1}{U}")
                .oracleText("Draw a card.")
                .typeLine("Creature — Human Wizard")
                .power("2")
                .toughness("3")
                .loyalty("4")
                .defense("5")
                .colors(Set.of("U"))
                .colorIndicator(Set.of("U", "R"))
                .manaValue(2)
                .endFace()
                .secondaryFace("Back")
                .endFace()
                .build()
                .primaryFace()
                .orElseThrow();
    }

    @Nested
    class MinimalBuild {

        @Test
        void hasName() {
            var face = buildMinimalFace("Delver of Secrets");

            assertThat(face.name()).isEqualTo("Delver of Secrets");
        }

        @Test
        void optionalFieldsAreEmpty() {
            var face = buildMinimalFace("Delver of Secrets");

            assertThat(face.manaCost()).isEmpty();
            assertThat(face.oracleText()).isEmpty();
            assertThat(face.typeLine()).isEmpty();
            assertThat(face.power()).isEmpty();
            assertThat(face.toughness()).isEmpty();
            assertThat(face.loyalty()).isEmpty();
            assertThat(face.defense()).isEmpty();
        }

        @Test
        void setsDefaultToEmpty() {
            var face = buildMinimalFace("Delver of Secrets");

            assertThat(face.colors()).isEmpty();
            assertThat(face.colorIndicator()).isEmpty();
        }

        @Test
        void manaValueDefaultsToZero() {
            var face = buildMinimalFace("Delver of Secrets");

            assertThat(face.manaValue()).isZero();
        }
    }

    @Nested
    class FullBuild {

        @Test
        void allFieldsPopulated() {
            var face = buildFullFace();

            assertThat(face.name()).isEqualTo("Front");
            assertThat(face.manaCost()).hasValue("{1}{U}");
            assertThat(face.oracleText()).hasValue("Draw a card.");
            assertThat(face.typeLine()).hasValue("Creature — Human Wizard");
            assertThat(face.power()).hasValue("2");
            assertThat(face.toughness()).hasValue("3");
            assertThat(face.loyalty()).hasValue("4");
            assertThat(face.defense()).hasValue("5");
            assertThat(face.colors()).containsExactly("U");
            assertThat(face.colorIndicator()).containsExactlyInAnyOrder("U", "R");
            assertThat(face.manaValue()).isEqualTo(2);
        }
    }

    @Nested
    class Immutability {

        @Test
        void colorsSetIsImmutable() {
            var face = buildFullFace();

            assertThat(face.colors()).isUnmodifiable();
        }

        @Test
        void colorIndicatorSetIsImmutable() {
            var face = buildFullFace();

            assertThat(face.colorIndicator()).isUnmodifiable();
        }
    }
}
