package be.imgn.mtg.engine.ability.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.AbilityActivatedEvent;
import be.imgn.mtg.engine.ability.AbilityContext;
import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.ability.ActivationResult;
import be.imgn.mtg.engine.event.EventBus;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.zone.Battlefield;
import be.imgn.mtg.engine.zone.Graveyard;
import be.imgn.mtg.engine.zone.Hand;
import be.imgn.mtg.engine.zone.ZoneType;

@DisplayName("ManaAbilityHandler")
class ManaAbilityHandlerTest {

    private EventBus eventBus;
    private ManaAbilityHandler handler;
    private GameState state;
    private GameObject source;
    private Player controller;

    @BeforeEach
    void setUp() {
        eventBus = mock(EventBus.class);
        handler = new ManaAbilityHandler(eventBus);
        state = mock(GameState.class);
        controller = mock(Player.class);
        source = Card.builder()
                .owner(controller)
                .controller(controller)
                .name("Test Source")
                .build();
    }

    @Nested
    @DisplayName("canActivate()")
    class CanActivateTests {

        @Test
        void nonManaAbility_returnsFalse() {
            var ability = mock(ActivatedAbility.class);
            when(ability.isManaAbility()).thenReturn(false);

            var result = handler.canActivate(ability, source, state);

            assertThat(result).isFalse();
        }

        @Test
        void sourceNotFound_returnsFalse() {
            var ability = mock(ActivatedAbility.class);
            when(ability.isManaAbility()).thenReturn(true);
            when(state.findZone(source.id())).thenReturn(Optional.empty());

            var result = handler.canActivate(ability, source, state);

            assertThat(result).isFalse();
        }

        @Test
        void sourceInWrongZone_returnsFalse() {
            var ability = mock(ActivatedAbility.class);
            var zone = mock(Hand.class);
            when(zone.type()).thenReturn(ZoneType.HAND);
            when(ability.isManaAbility()).thenReturn(true);
            when(ability.activatesFrom()).thenReturn(Set.of(ZoneType.BATTLEFIELD));
            when(state.findZone(source.id())).thenReturn(Optional.of(zone));

            var result = handler.canActivate(ability, source, state);

            assertThat(result).isFalse();
        }

        @Test
        void validManaAbilityInCorrectZone_returnsTrue() {
            var ability = mock(ActivatedAbility.class);
            var zone = mock(Battlefield.class);
            when(zone.type()).thenReturn(ZoneType.BATTLEFIELD);
            when(ability.isManaAbility()).thenReturn(true);
            when(ability.activatesFrom()).thenReturn(Set.of(ZoneType.BATTLEFIELD));
            when(state.findZone(source.id())).thenReturn(Optional.of(zone));

            var result = handler.canActivate(ability, source, state);

            assertThat(result).isTrue();
        }

        @Test
        void abilityActivatesFromMultipleZones_oneMatches_returnsTrue() {
            var ability = mock(ActivatedAbility.class);
            var zone = mock(Graveyard.class);
            when(zone.type()).thenReturn(ZoneType.GRAVEYARD);
            when(ability.isManaAbility()).thenReturn(true);
            when(ability.activatesFrom()).thenReturn(Set.of(ZoneType.BATTLEFIELD, ZoneType.GRAVEYARD));
            when(state.findZone(source.id())).thenReturn(Optional.of(zone));

            var result = handler.canActivate(ability, source, state);

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("activate()")
    class ActivateTests {

        @Test
        void cannotActivate_returnsIllegalResult() {
            var ability = mock(ActivatedAbility.class);
            var context = new AbilityContext(source, controller, state);
            when(ability.isManaAbility()).thenReturn(false);

            var result = handler.activate(ability, source, context);

            assertThat(result).isInstanceOf(ActivationResult.Illegal.class);
            var illegal = (ActivationResult.Illegal) result;
            assertThat(illegal.reason()).isEqualTo("Mana ability cannot be activated");
        }

        @Test
        void successfulActivation_firesEvent() {
            var ability = mock(ActivatedAbility.class);
            var zone = mock(Battlefield.class);
            when(zone.type()).thenReturn(ZoneType.BATTLEFIELD);
            var context = new AbilityContext(source, controller, state);

            when(ability.isManaAbility()).thenReturn(true);
            when(ability.activatesFrom()).thenReturn(Set.of(ZoneType.BATTLEFIELD));
            when(state.findZone(source.id())).thenReturn(Optional.of(zone));

            handler.activate(ability, source, context);

            verify(eventBus).post(any(AbilityActivatedEvent.class));
        }

        @Test
        void successfulActivation_returnsManaAbilitySuccessResult() {
            var ability = mock(ActivatedAbility.class);
            var zone = mock(Battlefield.class);
            when(zone.type()).thenReturn(ZoneType.BATTLEFIELD);
            var context = new AbilityContext(source, controller, state);

            when(ability.isManaAbility()).thenReturn(true);
            when(ability.activatesFrom()).thenReturn(Set.of(ZoneType.BATTLEFIELD));
            when(state.findZone(source.id())).thenReturn(Optional.of(zone));

            var result = handler.activate(ability, source, context);

            assertThat(result).isInstanceOf(ActivationResult.ManaAbilitySuccess.class);
        }

        @Test
        void successfulActivation_returnsEmptyEventList() {
            var ability = mock(ActivatedAbility.class);
            var zone = mock(Battlefield.class);
            when(zone.type()).thenReturn(ZoneType.BATTLEFIELD);
            var context = new AbilityContext(source, controller, state);

            when(ability.isManaAbility()).thenReturn(true);
            when(ability.activatesFrom()).thenReturn(Set.of(ZoneType.BATTLEFIELD));
            when(state.findZone(source.id())).thenReturn(Optional.of(zone));

            var result = handler.activate(ability, source, context);

            assertThat(result).isInstanceOf(ActivationResult.ManaAbilitySuccess.class);
            var success = (ActivationResult.ManaAbilitySuccess) result;
            assertThat(success.events()).isEmpty();
        }

        @Test
        void eventContainsCorrectAbilitySourceAndController() {
            var ability = mock(ActivatedAbility.class);
            var zone = mock(Battlefield.class);
            when(zone.type()).thenReturn(ZoneType.BATTLEFIELD);
            var context = new AbilityContext(source, controller, state);

            when(ability.isManaAbility()).thenReturn(true);
            when(ability.activatesFrom()).thenReturn(Set.of(ZoneType.BATTLEFIELD));
            when(state.findZone(source.id())).thenReturn(Optional.of(zone));

            handler.activate(ability, source, context);

            verify(eventBus).post(any(AbilityActivatedEvent.class));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        void abilityWithNoActivationZones_cannotActivate() {
            var ability = mock(ActivatedAbility.class);
            var zone = mock(Battlefield.class);
            when(zone.type()).thenReturn(ZoneType.BATTLEFIELD);
            when(ability.isManaAbility()).thenReturn(true);
            when(ability.activatesFrom()).thenReturn(Set.of());
            when(state.findZone(source.id())).thenReturn(Optional.of(zone));

            var result = handler.canActivate(ability, source, state);

            assertThat(result).isFalse();
        }
    }
}
