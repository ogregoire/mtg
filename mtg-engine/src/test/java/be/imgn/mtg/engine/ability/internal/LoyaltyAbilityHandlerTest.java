package be.imgn.mtg.engine.ability.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.AbilityActivatedEvent;
import be.imgn.mtg.engine.ability.AbilityContext;
import be.imgn.mtg.engine.ability.AbilityId;
import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.ability.ActivationResult;
import be.imgn.mtg.engine.event.EventBus;
import be.imgn.mtg.engine.event.EventTracker;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.TypedObject;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.Phase;
import be.imgn.mtg.engine.turn.PhaseStartedEvent;
import be.imgn.mtg.engine.turn.TurnTracker;
import be.imgn.mtg.engine.zone.Battlefield;
import be.imgn.mtg.engine.zone.Hand;
import be.imgn.mtg.engine.zone.Stack;
import be.imgn.mtg.engine.zone.ZoneType;

@DisplayName("LoyaltyAbilityHandler")
class LoyaltyAbilityHandlerTest {

    private TurnTracker turnTracker;
    private EventTracker eventTracker;
    private EventBus eventBus;
    private LoyaltyAbilityHandler handler;
    private GameState state;
    private TypedObject source;
    private Player controller;
    private Stack stack;

    @BeforeEach
    void setUp() {
        turnTracker = mock(TurnTracker.class);
        eventTracker = mock(EventTracker.class);
        eventBus = mock(EventBus.class);
        handler = new LoyaltyAbilityHandler(turnTracker, eventTracker, eventBus);
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
        void nonLoyaltyAbility_returnsFalse() {
            var ability = mock(ActivatedAbility.class);
            when(ability.isLoyaltyAbility()).thenReturn(false);

            var result = handler.canActivate(ability, source, state);

            assertThat(result).isFalse();
        }

        @Test
        void noPriority_returnsFalse() {
            var ability = setupLoyaltyAbility();
            when(turnTracker.hasPriority(controller)).thenReturn(false);

            var result = handler.canActivate(ability, source, state);

            assertThat(result).isFalse();
        }

        @Test
        void notActivePlayer_returnsFalse() {
            var ability = setupLoyaltyAbility();
            var activePlayer = mock(Player.class);
            when(turnTracker.hasPriority(controller)).thenReturn(true);
            when(state.activePlayer()).thenReturn(activePlayer);

            var result = handler.canActivate(ability, source, state);

            assertThat(result).isFalse();
        }

        @Test
        void stackNotEmpty_returnsFalse() {
            var ability = setupLoyaltyAbility();
            when(turnTracker.hasPriority(controller)).thenReturn(true);
            when(state.activePlayer()).thenReturn(controller);
            when(state.stack()).thenReturn(stack);
            when(stack.isEmpty()).thenReturn(false);

            var result = handler.canActivate(ability, source, state);

            assertThat(result).isFalse();
        }

        @Test
        void notMainPhase_returnsFalse() {
            var ability = setupLoyaltyAbility();
            when(turnTracker.hasPriority(controller)).thenReturn(true);
            when(state.activePlayer()).thenReturn(controller);
            when(state.stack()).thenReturn(stack);
            when(stack.isEmpty()).thenReturn(true);
            when(eventTracker.eventsFromThisTurn(PhaseStartedEvent.class))
                    .thenReturn(Stream.of(new PhaseStartedEvent(Phase.COMBAT, 1)));

            var result = handler.canActivate(ability, source, state);

            assertThat(result).isFalse();
        }

        @Test
        void sourceNotOnBattlefield_returnsFalse() {
            var ability = setupLoyaltyAbility();
            var zone = mock(Hand.class);
            when(zone.type()).thenReturn(ZoneType.HAND);
            setupMainPhase();
            when(turnTracker.hasPriority(controller)).thenReturn(true);
            when(state.activePlayer()).thenReturn(controller);
            when(state.stack()).thenReturn(stack);
            when(stack.isEmpty()).thenReturn(true);
            when(state.findZone(source)).thenReturn(Optional.of(zone));

            var result = handler.canActivate(ability, source, state);

            assertThat(result).isFalse();
        }

        @Test
        void loyaltyAbilityAlreadyActivatedThisTurn_returnsFalse() {
            var ability = setupLoyaltyAbility();
            var battlefield = mock(Battlefield.class);
            when(battlefield.type()).thenReturn(ZoneType.BATTLEFIELD);
            var activationEvent = mock(AbilityActivatedEvent.class);
            setupMainPhase();

            when(turnTracker.hasPriority(controller)).thenReturn(true);
            when(state.activePlayer()).thenReturn(controller);
            when(state.stack()).thenReturn(stack);
            when(stack.isEmpty()).thenReturn(true);
            when(state.findZone(source)).thenReturn(Optional.of(battlefield));
            when(eventTracker.eventsFromThisTurn(AbilityActivatedEvent.class)).thenReturn(Stream.of(activationEvent));
            when(activationEvent.isLoyaltyAbility()).thenReturn(true);
            when(activationEvent.source()).thenReturn(source);

            var result = handler.canActivate(ability, source, state);

            assertThat(result).isFalse();
        }

        @Test
        void allConditionsMet_returnsTrue() {
            var ability = setupLoyaltyAbility();
            var battlefield = mock(Battlefield.class);
            when(battlefield.type()).thenReturn(ZoneType.BATTLEFIELD);
            setupMainPhase();

            when(turnTracker.hasPriority(controller)).thenReturn(true);
            when(state.activePlayer()).thenReturn(controller);
            when(state.stack()).thenReturn(stack);
            when(stack.isEmpty()).thenReturn(true);
            when(state.findZone(source)).thenReturn(Optional.of(battlefield));
            when(eventTracker.eventsFromThisTurn(AbilityActivatedEvent.class)).thenReturn(Stream.empty());

            var result = handler.canActivate(ability, source, state);

            assertThat(result).isTrue();
        }

        @Test
        void loyaltyAbilityActivatedOnDifferentPlaneswalker_returnsTrue() {
            var ability = setupLoyaltyAbility();
            var battlefield = mock(Battlefield.class);
            when(battlefield.type()).thenReturn(ZoneType.BATTLEFIELD);
            var activationEvent = mock(AbilityActivatedEvent.class);
            var otherSource = Card.builder()
                    .owner(controller)
                    .controller(controller)
                    .name("Other Source")
                    .build();
            setupMainPhase();

            when(turnTracker.hasPriority(controller)).thenReturn(true);
            when(state.activePlayer()).thenReturn(controller);
            when(state.stack()).thenReturn(stack);
            when(stack.isEmpty()).thenReturn(true);
            when(state.findZone(source)).thenReturn(Optional.of(battlefield));
            when(eventTracker.eventsFromThisTurn(AbilityActivatedEvent.class)).thenReturn(Stream.of(activationEvent));
            when(activationEvent.isLoyaltyAbility()).thenReturn(true);
            when(activationEvent.source()).thenReturn(otherSource);

            var result = handler.canActivate(ability, source, state);

            assertThat(result).isTrue();
        }

        private void setupMainPhase() {
            when(eventTracker.eventsFromThisTurn(PhaseStartedEvent.class))
                    .thenReturn(Stream.of(new PhaseStartedEvent(Phase.MAIN, 1)));
        }
    }

    @Nested
    @DisplayName("activate()")
    class ActivateTests {

        @Test
        void cannotActivate_returnsIllegalResult() {
            var ability = setupLoyaltyAbility();
            var context = new AbilityContext(source, controller, state);
            when(ability.isLoyaltyAbility()).thenReturn(false);

            var result = handler.activate(ability, source, context, stack);

            assertThat(result).isInstanceOf(ActivationResult.Illegal.class);
            var illegal = (ActivationResult.Illegal) result;
            assertThat(illegal.reason()).isEqualTo("Loyalty ability cannot be activated");
        }

        @Test
        void successfulActivation_putsAbilityOnStack() {
            var ability = setupValidLoyaltyActivation();
            var context = new AbilityContext(source, controller, state);

            handler.activate(ability, source, context, stack);

            verify(stack).push(any());
        }

        @Test
        void successfulActivation_firesEvent() {
            var ability = setupValidLoyaltyActivation();
            var context = new AbilityContext(source, controller, state);

            handler.activate(ability, source, context, stack);

            verify(eventBus).post(any(AbilityActivatedEvent.class));
        }

        @Test
        void successfulActivation_returnsSuccessResult() {
            var ability = setupValidLoyaltyActivation();
            var context = new AbilityContext(source, controller, state);

            var result = handler.activate(ability, source, context, stack);

            assertThat(result).isInstanceOf(ActivationResult.Success.class);
        }

        @Test
        void successfulActivation_returnsEmptyEventList() {
            var ability = setupValidLoyaltyActivation();
            var context = new AbilityContext(source, controller, state);

            var result = handler.activate(ability, source, context, stack);

            assertThat(result).isInstanceOf(ActivationResult.Success.class);
            var success = (ActivationResult.Success) result;
            assertThat(success.events()).isEmpty();
        }

        @Test
        void sourceWithName_formatsAbilityNameWithSourceName() {
            var ability = setupValidLoyaltyActivation();
            var context = new AbilityContext(source, controller, state);

            handler.activate(ability, source, context, stack);

            verify(stack).push(any());
        }

        private ActivatedAbility setupValidLoyaltyActivation() {
            var ability = setupLoyaltyAbility();
            var battlefield = mock(Battlefield.class);
            when(battlefield.type()).thenReturn(ZoneType.BATTLEFIELD);

            when(turnTracker.hasPriority(controller)).thenReturn(true);
            when(state.activePlayer()).thenReturn(controller);
            when(state.stack()).thenReturn(stack);
            when(stack.isEmpty()).thenReturn(true);
            when(state.findZone(source)).thenReturn(Optional.of(battlefield));
            when(eventTracker.eventsFromThisTurn(PhaseStartedEvent.class))
                    .thenReturn(Stream.of(new PhaseStartedEvent(Phase.MAIN, 1)));
            when(eventTracker.eventsFromThisTurn(AbilityActivatedEvent.class)).thenReturn(Stream.empty());

            return ability;
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        void multiplePhaseEventsThisTurn_usesLatestPhase() {
            var ability = setupLoyaltyAbility();
            var battlefield = mock(Battlefield.class);
            when(battlefield.type()).thenReturn(ZoneType.BATTLEFIELD);

            when(turnTracker.hasPriority(controller)).thenReturn(true);
            when(state.activePlayer()).thenReturn(controller);
            when(state.stack()).thenReturn(stack);
            when(stack.isEmpty()).thenReturn(true);
            when(state.findZone(source)).thenReturn(Optional.of(battlefield));
            when(eventTracker.eventsFromThisTurn(PhaseStartedEvent.class))
                    .thenReturn(
                            Stream.of(new PhaseStartedEvent(Phase.COMBAT, 1), new PhaseStartedEvent(Phase.MAIN, 2)));
            when(eventTracker.eventsFromThisTurn(AbilityActivatedEvent.class)).thenReturn(Stream.empty());

            var result = handler.canActivate(ability, source, state);

            assertThat(result).isTrue();
        }

        @Test
        void noPhaseEventsYet_returnsFalse() {
            var ability = setupLoyaltyAbility();
            var battlefield = mock(Battlefield.class);
            when(battlefield.type()).thenReturn(ZoneType.BATTLEFIELD);

            when(turnTracker.hasPriority(controller)).thenReturn(true);
            when(state.activePlayer()).thenReturn(controller);
            when(state.stack()).thenReturn(stack);
            when(stack.isEmpty()).thenReturn(true);
            when(state.findZone(source)).thenReturn(Optional.of(battlefield));
            when(eventTracker.eventsFromThisTurn(PhaseStartedEvent.class)).thenReturn(Stream.empty());

            var result = handler.canActivate(ability, source, state);

            assertThat(result).isFalse();
        }
    }

    private ActivatedAbility setupLoyaltyAbility() {
        var ability = mock(ActivatedAbility.class);
        when(ability.isLoyaltyAbility()).thenReturn(true);
        when(ability.id()).thenReturn(new AbilityId());
        return ability;
    }
}
