package be.imgn.mtg.engine.ability.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.StaticAbility;
import be.imgn.mtg.engine.ability.ZoneFunctionality;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.zone.ZoneType;

@DisplayName("DefaultZoneFunctionality")
class DefaultZoneFunctionalityTest {

    private ZoneFunctionality zoneFunctionality;
    private GameObject source;
    private Player controller;

    @BeforeEach
    void setUp() {
        zoneFunctionality = new DefaultZoneFunctionality();
        controller = mock(Player.class);
        source = Card.builder()
                .owner(controller)
                .controller(controller)
                .name("Test Source")
                .build();
    }

    @Nested
    @DisplayName("functionalZones()")
    class FunctionalZonesTests {

        @Test
        void characteristicDefiningAbility_returnsAllZones() {
            var ability = mock(StaticAbility.class);
            when(ability.isCharacteristicDefining()).thenReturn(true);

            var zones = zoneFunctionality.functionalZones(ability, source);

            assertThat(zones).containsExactlyInAnyOrder(ZoneType.values());
        }

        @Test
        void abilityWithSpecifiedZones_returnsThoseZones() {
            var specifiedZones = Set.of(ZoneType.HAND, ZoneType.GRAVEYARD);
            var ability = mock(StaticAbility.class);
            when(ability.isCharacteristicDefining()).thenReturn(false);
            when(ability.functionalZones()).thenReturn(specifiedZones);

            var zones = zoneFunctionality.functionalZones(ability, source);

            assertThat(zones).isEqualTo(specifiedZones);
        }

        @Test
        void abilityWithEmptySpecifiedZones_returnsBattlefieldOnly() {
            var ability = mock(StaticAbility.class);
            when(ability.isCharacteristicDefining()).thenReturn(false);
            when(ability.functionalZones()).thenReturn(Set.of());

            var zones = zoneFunctionality.functionalZones(ability, source);

            assertThat(zones).containsExactly(ZoneType.BATTLEFIELD);
        }

        @Test
        void normalStaticAbility_returnsBattlefieldOnly() {
            var ability = mock(StaticAbility.class);
            when(ability.isCharacteristicDefining()).thenReturn(false);
            when(ability.functionalZones()).thenReturn(Set.of());

            var zones = zoneFunctionality.functionalZones(ability, source);

            assertThat(zones).containsExactly(ZoneType.BATTLEFIELD);
            assertThat(zones).hasSize(1);
        }

        @Test
        void characteristicDefiningAbilityTakesPrecedenceOverSpecifiedZones() {
            var specifiedZones = Set.of(ZoneType.HAND);
            var ability = mock(StaticAbility.class);
            when(ability.isCharacteristicDefining()).thenReturn(true);
            when(ability.functionalZones()).thenReturn(specifiedZones);

            var zones = zoneFunctionality.functionalZones(ability, source);

            // CDA should return all zones, ignoring the specified zones
            assertThat(zones).containsExactlyInAnyOrder(ZoneType.values());
            assertThat(zones).hasSize(ZoneType.values().length);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        void multipleZonesSpecified_returnsAllSpecifiedZones() {
            var specifiedZones = Set.of(ZoneType.HAND, ZoneType.GRAVEYARD, ZoneType.EXILE, ZoneType.COMMAND);
            var ability = mock(StaticAbility.class);
            when(ability.isCharacteristicDefining()).thenReturn(false);
            when(ability.functionalZones()).thenReturn(specifiedZones);

            var zones = zoneFunctionality.functionalZones(ability, source);

            assertThat(zones).isEqualTo(specifiedZones);
        }

        @Test
        void battlefieldOnlySpecified_returnsBattlefield() {
            var specifiedZones = Set.of(ZoneType.BATTLEFIELD);
            var ability = mock(StaticAbility.class);
            when(ability.isCharacteristicDefining()).thenReturn(false);
            when(ability.functionalZones()).thenReturn(specifiedZones);

            var zones = zoneFunctionality.functionalZones(ability, source);

            assertThat(zones).containsExactly(ZoneType.BATTLEFIELD);
        }
    }
}
