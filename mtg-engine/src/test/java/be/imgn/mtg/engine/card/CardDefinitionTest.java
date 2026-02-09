package be.imgn.mtg.engine.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CardDefinitionTest {

    private static final UUID ORACLE_ID = UUID.fromString("a0b1c2d3-e4f5-6789-abcd-ef0123456789");
    private static final UUID OTHER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Nested
    class NormalCardBuild {

        @Test
        void buildMinimalNormalCard() {
            var card = CardDefinition.builder(ORACLE_ID, "Lightning Bolt", CardLayout.NORMAL)
                    .build();

            assertThat(card.oracleId()).isEqualTo(ORACLE_ID);
            assertThat(card.name()).isEqualTo("Lightning Bolt");
            assertThat(card.layout()).isEqualTo(CardLayout.NORMAL);
        }

        @Test
        void buildFullNormalCard() {
            var card = CardDefinition.builder(ORACLE_ID, "Lightning Bolt", CardLayout.NORMAL)
                    .manaCost("{R}")
                    .oracleText("Lightning Bolt deals 3 damage to any target.")
                    .typeLine("Instant")
                    .manaValue(1)
                    .colors(Set.of("R"))
                    .colorIdentity(Set.of("R"))
                    .build();

            assertThat(card.manaCost()).hasValue("{R}");
            assertThat(card.oracleText()).hasValue("Lightning Bolt deals 3 damage to any target.");
            assertThat(card.typeLine()).hasValue("Instant");
            assertThat(card.manaValue()).isEqualTo(1);
            assertThat(card.colors()).containsExactly("R");
            assertThat(card.colorIdentity()).containsExactly("R");
        }

        @Test
        void optionalFieldsDefaultToEmpty() {
            var card = CardDefinition.builder(ORACLE_ID, "Lightning Bolt", CardLayout.NORMAL)
                    .build();

            assertThat(card.manaCost()).isEmpty();
            assertThat(card.oracleText()).isEmpty();
            assertThat(card.typeLine()).isEmpty();
            assertThat(card.power()).isEmpty();
            assertThat(card.toughness()).isEmpty();
            assertThat(card.loyalty()).isEmpty();
            assertThat(card.defense()).isEmpty();
            assertThat(card.primaryFace()).isEmpty();
            assertThat(card.secondaryFace()).isEmpty();
        }

        @Test
        void setFieldsDefaultToEmpty() {
            var card = CardDefinition.builder(ORACLE_ID, "Lightning Bolt", CardLayout.NORMAL)
                    .build();

            assertThat(card.colors()).isEmpty();
            assertThat(card.colorIdentity()).isEmpty();
            assertThat(card.colorIndicator()).isEmpty();
        }

        @Test
        void manaValueDefaultsToZero() {
            var card = CardDefinition.builder(ORACLE_ID, "Lightning Bolt", CardLayout.NORMAL)
                    .build();

            assertThat(card.manaValue()).isZero();
        }

        @Test
        void creatureFieldsAreSet() {
            var card = CardDefinition.builder(ORACLE_ID, "Grizzly Bears", CardLayout.NORMAL)
                    .power("2")
                    .toughness("2")
                    .build();

            assertThat(card.power()).hasValue("2");
            assertThat(card.toughness()).hasValue("2");
        }

        @Test
        void planeswalkerFieldsAreSet() {
            var card = CardDefinition.builder(ORACLE_ID, "Jace", CardLayout.NORMAL)
                    .loyalty("3")
                    .build();

            assertThat(card.loyalty()).hasValue("3");
        }

        @Test
        void battleFieldsAreSet() {
            var card = CardDefinition.builder(ORACLE_ID, "Invasion of Gobakhan", CardLayout.NORMAL)
                    .defense("3")
                    .build();

            assertThat(card.defense()).hasValue("3");
        }
    }

    @Nested
    class DoubleFacedCardBuild {

        @Test
        void buildTransformCard() {
            var card = CardDefinition.builder(
                            ORACLE_ID, "Delver of Secrets // Insectile Aberration", CardLayout.TRANSFORM)
                    .primaryFace("Delver of Secrets")
                    .manaCost("{U}")
                    .typeLine("Creature — Human Wizard")
                    .oracleText("At the beginning of your upkeep...")
                    .power("1")
                    .toughness("1")
                    .endFace()
                    .secondaryFace("Insectile Aberration")
                    .typeLine("Creature — Human Insect")
                    .oracleText("Flying")
                    .power("3")
                    .toughness("2")
                    .colorIndicator(Set.of("U"))
                    .endFace()
                    .build();

            assertThat(card.primaryFace()).isPresent();
            assertThat(card.secondaryFace()).isPresent();

            var front = card.primaryFace().orElseThrow();
            assertThat(front.name()).isEqualTo("Delver of Secrets");
            assertThat(front.manaCost()).hasValue("{U}");
            assertThat(front.power()).hasValue("1");
            assertThat(front.toughness()).hasValue("1");

            var back = card.secondaryFace().orElseThrow();
            assertThat(back.name()).isEqualTo("Insectile Aberration");
            assertThat(back.oracleText()).hasValue("Flying");
            assertThat(back.power()).hasValue("3");
            assertThat(back.colorIndicator()).containsExactly("U");
        }

        @Test
        void buildModalDfcCard() {
            var card = CardDefinition.builder(
                            ORACLE_ID, "Barkchannel Pathway // Tidechannel Pathway", CardLayout.MODAL_DFC)
                    .primaryFace("Barkchannel Pathway")
                    .typeLine("Land")
                    .endFace()
                    .secondaryFace("Tidechannel Pathway")
                    .typeLine("Land")
                    .endFace()
                    .build();

            assertThat(card.primaryFace()).isPresent();
            assertThat(card.secondaryFace()).isPresent();
        }
    }

    @Nested
    class BuilderValidation {

        @Test
        void primaryFaceThrowsOnNonDfc() {
            var builder = CardDefinition.builder(ORACLE_ID, "Lightning Bolt", CardLayout.NORMAL);

            assertThatThrownBy(() -> builder.primaryFace("Front"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("NORMAL");
        }

        @Test
        void secondaryFaceThrowsOnNonDfc() {
            var builder = CardDefinition.builder(ORACLE_ID, "Lightning Bolt", CardLayout.NORMAL);

            assertThatThrownBy(() -> builder.secondaryFace("Back"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("NORMAL");
        }

        @Test
        void dfcWithoutFacesThrows() {
            var builder = CardDefinition.builder(ORACLE_ID, "Card", CardLayout.TRANSFORM);

            assertThatThrownBy(builder::build)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("requires both");
        }

        @Test
        void dfcWithOnlyPrimaryFaceThrows() {
            var builder = CardDefinition.builder(ORACLE_ID, "Card", CardLayout.TRANSFORM)
                    .primaryFace("Front")
                    .endFace();

            assertThatThrownBy(builder::build)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("requires both");
        }

        @Test
        void dfcWithOnlySecondaryFaceThrows() {
            var builder = CardDefinition.builder(ORACLE_ID, "Card", CardLayout.TRANSFORM)
                    .secondaryFace("Back")
                    .endFace();

            assertThatThrownBy(builder::build)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("requires both");
        }
    }

    @Nested
    class Equality {

        @Test
        void sameOracleIdAreEqual() {
            var card1 = CardDefinition.builder(ORACLE_ID, "Lightning Bolt", CardLayout.NORMAL)
                    .build();
            var card2 = CardDefinition.builder(ORACLE_ID, "Lightning Bolt", CardLayout.NORMAL)
                    .build();

            assertThat(card1).isEqualTo(card2);
        }

        @Test
        void differentOracleIdAreNotEqual() {
            var card1 = CardDefinition.builder(ORACLE_ID, "Lightning Bolt", CardLayout.NORMAL)
                    .build();
            var card2 = CardDefinition.builder(OTHER_ID, "Lightning Bolt", CardLayout.NORMAL)
                    .build();

            assertThat(card1).isNotEqualTo(card2);
        }

        @Test
        void sameOracleIdHaveSameHashCode() {
            var card1 = CardDefinition.builder(ORACLE_ID, "Lightning Bolt", CardLayout.NORMAL)
                    .build();
            var card2 = CardDefinition.builder(ORACLE_ID, "Lightning Bolt", CardLayout.NORMAL)
                    .build();

            assertThat(card1.hashCode()).isEqualTo(card2.hashCode());
        }

        @Test
        void equalityIgnoresOtherFields() {
            var card1 = CardDefinition.builder(ORACLE_ID, "Lightning Bolt", CardLayout.NORMAL)
                    .manaCost("{R}")
                    .build();
            var card2 = CardDefinition.builder(ORACLE_ID, "Chain Lightning", CardLayout.NORMAL)
                    .manaCost("{R}")
                    .build();

            assertThat(card1).isEqualTo(card2);
        }

        @Test
        void notEqualToNull() {
            var card = CardDefinition.builder(ORACLE_ID, "Lightning Bolt", CardLayout.NORMAL)
                    .build();

            assertThat(card).isNotEqualTo(null);
        }

        @Test
        void notEqualToOtherType() {
            var card = CardDefinition.builder(ORACLE_ID, "Lightning Bolt", CardLayout.NORMAL)
                    .build();

            assertThat(card).isNotEqualTo("not a card");
        }
    }

    @Nested
    class Immutability {

        @Test
        void colorsSetIsImmutable() {
            var card = CardDefinition.builder(ORACLE_ID, "Bolt", CardLayout.NORMAL)
                    .colors(Set.of("R"))
                    .build();

            assertThat(card.colors()).isUnmodifiable();
        }

        @Test
        void colorIdentitySetIsImmutable() {
            var card = CardDefinition.builder(ORACLE_ID, "Bolt", CardLayout.NORMAL)
                    .colorIdentity(Set.of("R"))
                    .build();

            assertThat(card.colorIdentity()).isUnmodifiable();
        }

        @Test
        void colorIndicatorSetIsImmutable() {
            var card = CardDefinition.builder(ORACLE_ID, "Bolt", CardLayout.NORMAL)
                    .colorIndicator(Set.of("R"))
                    .build();

            assertThat(card.colorIndicator()).isUnmodifiable();
        }
    }
}
