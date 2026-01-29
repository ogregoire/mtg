package be.imgn.mtg.engine.state;

import static be.imgn.mtg.engine.assertions.MTGAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.Abilities;
import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.CreatureType;
import be.imgn.mtg.engine.characteristics.Supertype;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.cost.Costs;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.zone.ZoneType;

class ObjectSnapshotTest {

    private Player player;
    private Player opponent;

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
        opponent = mock(Player.class);
    }

    @Nested
    class SnapshotCreation {

        @Test
        void createsSnapshotFromCard() {
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Lightning Bolt")
                    .color(Color.RED)
                    .type(Type.INSTANT)
                    .build();

            var snapshot = ObjectSnapshot.of(card, ZoneType.GRAVEYARD);

            assertThat(snapshot.name()).isEqualTo("Lightning Bolt");
            assertThat(snapshot.zone()).isEqualTo(ZoneType.GRAVEYARD);
            assertThat(snapshot.owner()).isSameAs(player);
            assertThat(snapshot.controller()).isSameAs(player);
        }

        @Test
        void capturesColors() {
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test")
                    .colors(Color.WHITE, Color.BLUE)
                    .build();

            var snapshot = ObjectSnapshot.of(card, ZoneType.HAND);

            assertThat(snapshot.colors()).isWhite().isBlue();
        }

        @Test
        void capturesTypes() {
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test")
                    .types(Type.CREATURE, Type.ARTIFACT)
                    .build();

            var snapshot = ObjectSnapshot.of(card, ZoneType.BATTLEFIELD);

            assertThat(snapshot.types()).isCreature().isArtifact();
        }

        @Test
        void capturesSupertypes() {
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test")
                    .supertype(Supertype.LEGENDARY)
                    .build();

            var snapshot = ObjectSnapshot.of(card, ZoneType.BATTLEFIELD);

            assertThat(snapshot.supertypes()).contains(Supertype.LEGENDARY);
        }

        @Test
        void capturesSubtypes() {
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test")
                    .subtypes(CreatureType.GOBLIN, CreatureType.WARRIOR)
                    .build();

            var snapshot = ObjectSnapshot.of(card, ZoneType.GRAVEYARD);

            assertThat(snapshot.subtypes()).contains(CreatureType.GOBLIN).contains(CreatureType.WARRIOR);
        }

        @Test
        void capturesAbilities() {
            var abilities = Abilities.empty();
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test")
                    .abilities(abilities)
                    .build();

            var snapshot = ObjectSnapshot.of(card, ZoneType.EXILE);

            assertThat(snapshot.abilities()).isSameAs(abilities);
        }

        @Test
        void capturesCosts() {
            var costs = Costs.empty();
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test")
                    .costs(costs)
                    .build();

            var snapshot = ObjectSnapshot.of(card, ZoneType.HAND);

            assertThat(snapshot.costs()).isSameAs(costs);
        }
    }

    @Nested
    class CreaturePowerToughness {

        @Test
        void capturesCreaturePowerAndToughness() {
            var creature = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Grizzly Bears")
                    .type(Type.CREATURE)
                    .power(Value.of(2))
                    .toughness(Value.of(2))
                    .build();

            var snapshot = ObjectSnapshot.of(creature, ZoneType.GRAVEYARD);

            assertThat(snapshot.power()).isEqualTo(Value.of(2));
            assertThat(snapshot.toughness()).isEqualTo(Value.of(2));
        }

        @Test
        void defaultsToZeroWhenPowerIsNull() {
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Non-Creature")
                    .build();

            var snapshot = ObjectSnapshot.of(card, ZoneType.BATTLEFIELD);

            assertThat(snapshot.power()).isEqualTo(Value.of(0));
        }

        @Test
        void defaultsToZeroWhenToughnessIsNull() {
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Non-Creature")
                    .build();

            var snapshot = ObjectSnapshot.of(card, ZoneType.BATTLEFIELD);

            assertThat(snapshot.toughness()).isEqualTo(Value.of(0));
        }
    }

    @Nested
    class PlaneswalkerLoyalty {

        @Test
        void capturesLoyalty() {
            var planeswalker = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Jace Beleren")
                    .type(Type.PLANESWALKER)
                    .loyalty(Value.of(3))
                    .build();

            var snapshot = ObjectSnapshot.of(planeswalker, ZoneType.GRAVEYARD);

            assertThat(snapshot.loyalty()).isEqualTo(Value.of(3));
        }

        @Test
        void defaultsToZeroWhenLoyaltyIsNull() {
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Non-Planeswalker")
                    .build();

            var snapshot = ObjectSnapshot.of(card, ZoneType.BATTLEFIELD);

            assertThat(snapshot.loyalty()).isEqualTo(Value.of(0));
        }
    }

    @Nested
    class ControllerAndOwner {

        @Test
        void capturesDifferentControllerAndOwner() {
            var card = Card.builder()
                    .owner(player)
                    .controller(opponent)
                    .name("Control Magic Target")
                    .build();

            var snapshot = ObjectSnapshot.of(card, ZoneType.BATTLEFIELD);

            assertThat(snapshot.owner()).isSameAs(player);
            assertThat(snapshot.controller()).isSameAs(opponent);
        }

        @Test
        void preservesOwnerInGraveyard() {
            var card = Card.builder()
                    .owner(player)
                    .controller(opponent)
                    .name("Test")
                    .build();

            var snapshot = ObjectSnapshot.of(card, ZoneType.GRAVEYARD);

            // In graveyard, controller resets to owner, but snapshot captures state before
            assertThat(snapshot.owner()).isSameAs(player);
            assertThat(snapshot.controller()).isSameAs(opponent);
        }
    }

    @Nested
    class DifferentZoneTypes {

        @Test
        void snapshotInBattlefield() {
            var card =
                    Card.builder().owner(player).controller(player).name("Test").build();

            var snapshot = ObjectSnapshot.of(card, ZoneType.BATTLEFIELD);

            assertThat(snapshot.zone()).isEqualTo(ZoneType.BATTLEFIELD);
        }

        @Test
        void snapshotInGraveyard() {
            var card =
                    Card.builder().owner(player).controller(player).name("Test").build();

            var snapshot = ObjectSnapshot.of(card, ZoneType.GRAVEYARD);

            assertThat(snapshot.zone()).isEqualTo(ZoneType.GRAVEYARD);
        }

        @Test
        void snapshotInExile() {
            var card =
                    Card.builder().owner(player).controller(player).name("Test").build();

            var snapshot = ObjectSnapshot.of(card, ZoneType.EXILE);

            assertThat(snapshot.zone()).isEqualTo(ZoneType.EXILE);
        }

        @Test
        void snapshotOnStack() {
            var card =
                    Card.builder().owner(player).controller(player).name("Test").build();

            var snapshot = ObjectSnapshot.of(card, ZoneType.STACK);

            assertThat(snapshot.zone()).isEqualTo(ZoneType.STACK);
        }

        @Test
        void snapshotInHand() {
            var card =
                    Card.builder().owner(player).controller(player).name("Test").build();

            var snapshot = ObjectSnapshot.of(card, ZoneType.HAND);

            assertThat(snapshot.zone()).isEqualTo(ZoneType.HAND);
        }

        @Test
        void snapshotInLibrary() {
            var card =
                    Card.builder().owner(player).controller(player).name("Test").build();

            var snapshot = ObjectSnapshot.of(card, ZoneType.LIBRARY);

            assertThat(snapshot.zone()).isEqualTo(ZoneType.LIBRARY);
        }

        @Test
        void snapshotInCommandZone() {
            var card =
                    Card.builder().owner(player).controller(player).name("Test").build();

            var snapshot = ObjectSnapshot.of(card, ZoneType.COMMAND);

            assertThat(snapshot.zone()).isEqualTo(ZoneType.COMMAND);
        }
    }

    @Nested
    class Permanents {

        @Test
        void createsSnapshotFromPermanent() {
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test Permanent")
                    .color(Color.GREEN)
                    .type(Type.CREATURE)
                    .subtype(CreatureType.ELF)
                    .power(Value.of(1))
                    .toughness(Value.of(1))
                    .build();
            var permanent = Permanent.fromCard(card, player).build();

            var snapshot = ObjectSnapshot.of(permanent, ZoneType.BATTLEFIELD);

            assertThat(snapshot.name()).isEqualTo("Test Permanent");
            assertThat(snapshot.colors()).isGreen();
            assertThat(snapshot.types()).isCreature();
            assertThat(snapshot.subtypes()).contains(CreatureType.ELF);
            assertThat(snapshot.power()).isEqualTo(Value.of(1));
            assertThat(snapshot.toughness()).isEqualTo(Value.of(1));
        }
    }

    @Nested
    class RecordEquality {

        @Test
        void snapshotsWithSameDataAreEqual() {
            var card =
                    Card.builder().owner(player).controller(player).name("Test").build();

            var snapshot1 = ObjectSnapshot.of(card, ZoneType.HAND);
            var snapshot2 = ObjectSnapshot.of(card, ZoneType.HAND);

            assertThat(snapshot1).isEqualTo(snapshot2);
        }

        @Test
        void snapshotsWithDifferentZonesAreNotEqual() {
            var card =
                    Card.builder().owner(player).controller(player).name("Test").build();

            var snapshot1 = ObjectSnapshot.of(card, ZoneType.HAND);
            var snapshot2 = ObjectSnapshot.of(card, ZoneType.GRAVEYARD);

            assertThat(snapshot1).isNotEqualTo(snapshot2);
        }
    }
}
