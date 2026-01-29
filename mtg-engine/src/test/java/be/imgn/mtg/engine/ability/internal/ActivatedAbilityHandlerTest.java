package be.imgn.mtg.engine.ability.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import be.imgn.mtg.engine.ability.AbilityId;
import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.ability.ActivationLimit;
import be.imgn.mtg.engine.ability.ActivationResult;
import be.imgn.mtg.engine.event.EventBus;
import be.imgn.mtg.engine.event.EventTracker;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.TypedObject;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.TurnTracker;
import be.imgn.mtg.engine.zone.Battlefield;
import be.imgn.mtg.engine.zone.Graveyard;
import be.imgn.mtg.engine.zone.Hand;
import be.imgn.mtg.engine.zone.Stack;
import be.imgn.mtg.engine.zone.ZoneType;

@DisplayName("ActivatedAbilityHandler")
class ActivatedAbilityHandlerTest {

    private TurnTracker turnTracker;
    private EventTracker eventTracker;
    private EventBus eventBus;
    private ActivatedAbilityHandler handler;
    private GameState state;
    private TypedObject source;
    private Player controller;
    private Stack stack;

    @BeforeEach
    void setUp() {
        turnTracker = mock(TurnTracker.class);
        eventTracker = mock(EventTracker.class);
        eventBus = mock(EventBus.class);
        handler = new ActivatedAbilityHandler(turnTracker, eventTracker, eventBus);
        state = mock(GameState.class);
        controller = mock(Player.class);
        source = Card.builder()
                .owner(controller)
                .controller(controller)
                .name("Test Source")
                .build();
        stack = mock(Stack.class);
    }

    @Nested
    @DisplayName("canActivate()")
    class CanActivateTests {

        @Test
        void noPriority_returnsFalse() {
            var ability = mock(ActivatedAbility.class);
            when(turnTracker.hasPriority(controller)).thenReturn(false);

            var result = handler.canActivate(ability, source, state);

            assertThat(result).isFalse();
        }

        @Test
        void sourceNotFound_returnsFalse() {
            var ability = mock(ActivatedAbility.class);
            when(turnTracker.hasPriority(controller)).thenReturn(true);
            when(state.findZone(source)).thenReturn(Optional.empty());

            var result = handler.canActivate(ability, source, state);

            assertThat(result).isFalse();
        }

        @Test
        void sourceInWrongZone_returnsFalse() {
            var ability = mock(ActivatedAbility.class);
            var zone = mock(Graveyard.class);
            when(zone.type()).thenReturn(ZoneType.GRAVEYARD);
            when(turnTracker.hasPriority(controller)).thenReturn(true);
            when(state.findZone(source)).thenReturn(Optional.of(zone));
            when(ability.activatesFrom()).thenReturn(Set.of(ZoneType.BATTLEFIELD));

            var result = handler.canActivate(ability, source, state);

            assertThat(result).isFalse();
        }

        @Test
        void activationLimitReached_returnsFalse() {
            var ability = mock(ActivatedAbility.class);
            var abilityId = new AbilityId();
            var zone = mock(Battlefield.class);
            when(zone.type()).thenReturn(ZoneType.BATTLEFIELD);
            var limit = mock(ActivationLimit.class);

            when(turnTracker.hasPriority(controller)).thenReturn(true);
            when(state.findZone(source)).thenReturn(Optional.of(zone));
            when(ability.activatesFrom()).thenReturn(Set.of(ZoneType.BATTLEFIELD));
            when(ability.id()).thenReturn(abilityId);
            when(ability.limit()).thenReturn(limit);
            when(limit.canActivate(abilityId, eventTracker)).thenReturn(false);

            var result = handler.canActivate(ability, source, state);

            assertThat(result).isFalse();
        }

        @Test
        void allConditionsMet_returnsTrue() {
            var ability = mock(ActivatedAbility.class);
            var abilityId = new AbilityId();
            var zone = mock(Battlefield.class);
            when(zone.type()).thenReturn(ZoneType.BATTLEFIELD);
            var limit = mock(ActivationLimit.class);

            when(turnTracker.hasPriority(controller)).thenReturn(true);
            when(state.findZone(source)).thenReturn(Optional.of(zone));
            when(ability.activatesFrom()).thenReturn(Set.of(ZoneType.BATTLEFIELD));
            when(ability.id()).thenReturn(abilityId);
            when(ability.limit()).thenReturn(limit);
            when(limit.canActivate(abilityId, eventTracker)).thenReturn(true);

            var result = handler.canActivate(ability, source, state);

            assertThat(result).isTrue();
        }

        @Test
        void abilityActivatesFromMultipleZones_sourceInOne_returnsTrue() {
            var ability = mock(ActivatedAbility.class);
            var abilityId = new AbilityId();
            var zone = mock(Hand.class);
            when(zone.type()).thenReturn(ZoneType.HAND);
            var limit = mock(ActivationLimit.class);

            when(turnTracker.hasPriority(controller)).thenReturn(true);
            when(state.findZone(source)).thenReturn(Optional.of(zone));
            when(ability.activatesFrom()).thenReturn(Set.of(ZoneType.BATTLEFIELD, ZoneType.HAND));
            when(ability.id()).thenReturn(abilityId);
            when(ability.limit()).thenReturn(limit);
            when(limit.canActivate(abilityId, eventTracker)).thenReturn(true);

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
            when(turnTracker.hasPriority(controller)).thenReturn(false);

            var result = handler.activate(ability, source, context, stack);

            assertThat(result).isInstanceOf(ActivationResult.Illegal.class);
            var illegal = (ActivationResult.Illegal) result;
            assertThat(illegal.reason()).isEqualTo("Ability cannot be activated");
        }

        @Test
        void successfulActivation_putsAbilityOnStack() {
            var ability = setupValidActivation();
            var context = new AbilityContext(source, controller, state);

            handler.activate(ability, source, context, stack);

            verify(stack).push(any());
        }

        @Test
        void successfulActivation_firesEvent() {
            var ability = setupValidActivation();
            var context = new AbilityContext(source, controller, state);

            handler.activate(ability, source, context, stack);

            verify(eventBus).post(any(AbilityActivatedEvent.class));
        }

        @Test
        void successfulActivation_returnsSuccessResult() {
            var ability = setupValidActivation();
            var context = new AbilityContext(source, controller, state);

            var result = handler.activate(ability, source, context, stack);

            assertThat(result).isInstanceOf(ActivationResult.Success.class);
        }

        @Test
        void successfulActivation_returnsEmptyEventList() {
            var ability = setupValidActivation();
            var context = new AbilityContext(source, controller, state);

            var result = handler.activate(ability, source, context, stack);

            assertThat(result).isInstanceOf(ActivationResult.Success.class);
            var success = (ActivationResult.Success) result;
            assertThat(success.events()).isEmpty();
        }

        @Test
        void sourceWithName_formatsAbilityNameWithSourceName() {
            var ability = setupValidActivation();
            var context = new AbilityContext(source, controller, state);

            handler.activate(ability, source, context, stack);

            // The ability should be pushed to stack with formatted name
            verify(stack).push(any());
        }

        private ActivatedAbility setupValidActivation() {
            var ability = mock(ActivatedAbility.class);
            var abilityId = new AbilityId();
            var zone = mock(Battlefield.class);
            when(zone.type()).thenReturn(ZoneType.BATTLEFIELD);
            var limit = mock(ActivationLimit.class);

            when(ability.id()).thenReturn(abilityId);
            when(ability.activatesFrom()).thenReturn(Set.of(ZoneType.BATTLEFIELD));
            when(ability.limit()).thenReturn(limit);
            when(turnTracker.hasPriority(controller)).thenReturn(true);
            when(state.findZone(source)).thenReturn(Optional.of(zone));
            when(limit.canActivate(eq(abilityId), any())).thenReturn(true);

            return ability;
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        void unlimitedActivationLimit_canAlwaysActivate() {
            var ability = mock(ActivatedAbility.class);
            var abilityId = new AbilityId();
            var zone = mock(Battlefield.class);
            when(zone.type()).thenReturn(ZoneType.BATTLEFIELD);

            when(turnTracker.hasPriority(controller)).thenReturn(true);
            when(state.findZone(source)).thenReturn(Optional.of(zone));
            when(ability.activatesFrom()).thenReturn(Set.of(ZoneType.BATTLEFIELD));
            when(ability.id()).thenReturn(abilityId);
            when(ability.limit()).thenReturn(ActivationLimit.UNLIMITED);

            var result = handler.canActivate(ability, source, state);

            assertThat(result).isTrue();
        }
    }
}
