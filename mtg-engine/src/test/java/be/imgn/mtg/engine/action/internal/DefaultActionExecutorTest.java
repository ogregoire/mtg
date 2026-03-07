package be.imgn.mtg.engine.action.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import be.imgn.mtg.engine.ability.Abilities;
import be.imgn.mtg.engine.ability.AbilityContext;
import be.imgn.mtg.engine.ability.AbilityManager;
import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.ability.ActivationResult;
import be.imgn.mtg.engine.ability.StaticAbility;
import be.imgn.mtg.engine.action.ActionExecutor;
import be.imgn.mtg.engine.action.ExecutionResult;
import be.imgn.mtg.engine.action.PlayerAction;
import be.imgn.mtg.engine.action.SpecialActionHandler;
import be.imgn.mtg.engine.action.SpecialActionType;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.cost.CostContext;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.mana.ManaCost;
import be.imgn.mtg.engine.object.AbilityOnStack;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.object.TapEvent;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.TurnTracker;
import be.imgn.mtg.engine.zone.CastEvent;
import be.imgn.mtg.engine.zone.ZoneType;

@DisplayName("DefaultActionExecutor")
class DefaultActionExecutorTest {

    private TurnTracker turnTracker;
    private SpecialActionHandler specialActionHandler;
    private AbilityManager abilityManager;
    private GameEventProcessor eventProcessor;
    private GameState gameState;
    private ActionExecutor executor;
    private Player player;

    @BeforeEach
    void setUp() {
        turnTracker = mock(TurnTracker.class);
        specialActionHandler = mock(SpecialActionHandler.class);
        abilityManager = mock(AbilityManager.class);
        eventProcessor = mock(GameEventProcessor.class);
        gameState = mock(GameState.class);
        executor = new DefaultActionExecutor(turnTracker, specialActionHandler, abilityManager, eventProcessor);
        player = mock(Player.class);
    }

    @Nested
    @DisplayName("Pass execution")
    class PassExecutionTests {

        @Test
        void callsPassPriorityOnTurnTracker() {
            var action = new PlayerAction.Pass(player);

            executor.execute(action, gameState);

            verify(turnTracker).passPriority(player);
        }

        @Test
        void returnsSuccessWithEmptyEvents() {
            var action = new PlayerAction.Pass(player);

            var result = executor.execute(action, gameState);

            assertThat(result).isInstanceOf(ExecutionResult.Success.class);
            assertThat(((ExecutionResult.Success) result).events()).isEmpty();
        }
    }

    @Nested
    @DisplayName("PlayLand execution")
    class PlayLandExecutionTests {

        @Test
        void delegatesToSpecialActionHandler() {
            var land = mock(Card.class);
            var action = new PlayerAction.PlayLand(player, land);
            when(specialActionHandler.playLand(action, gameState)).thenReturn(new ExecutionResult.Success(List.of()));

            executor.execute(action, gameState);

            verify(specialActionHandler).playLand(action, gameState);
        }

        @Test
        void returnsResultFromHandler() {
            var land = mock(Card.class);
            var action = new PlayerAction.PlayLand(player, land);
            var expectedResult = new ExecutionResult.Success(List.of());
            when(specialActionHandler.playLand(action, gameState)).thenReturn(expectedResult);

            var result = executor.execute(action, gameState);

            assertThat(result).isEqualTo(expectedResult);
        }
    }

    @Nested
    @DisplayName("SpecialAction execution")
    class SpecialActionExecutionTests {

        @Test
        void delegatesToSpecialActionHandler() {
            var target = mock(Card.class);
            var action = new PlayerAction.SpecialAction(player, SpecialActionType.SUSPEND, target);
            when(specialActionHandler.execute(action, gameState)).thenReturn(new ExecutionResult.Success(List.of()));

            executor.execute(action, gameState);

            verify(specialActionHandler).execute(action, gameState);
        }
    }

    @Nested
    @DisplayName("CastSpell execution")
    class CastSpellExecutionTests {

        @Test
        void paysManaCostAndProcessesCastEvent() {
            var manaCost = ManaCost.parse("{1}{R}");
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Lightning Bolt")
                    .type(Type.INSTANT)
                    .manaCost(manaCost)
                    .build();
            var action = new PlayerAction.CastSpell(player, card);

            executor.execute(action, gameState);

            verify(player).pay(eq(manaCost), any(CostContext.class));
            verify(eventProcessor).process(any(CastEvent.class));
        }

        @Test
        void returnsSuccessWithCastEvent() {
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Lightning Bolt")
                    .type(Type.INSTANT)
                    .manaCost(ManaCost.parse("{R}"))
                    .build();
            var action = new PlayerAction.CastSpell(player, card);

            var result = executor.execute(action, gameState);

            assertThat(result).isInstanceOf(ExecutionResult.Success.class);
            var success = (ExecutionResult.Success) result;
            assertThat(success.events()).hasSize(1);
            assertThat(success.events().getFirst()).isInstanceOf(CastEvent.class);
        }

        @Test
        void castEventHasCorrectFields() {
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Grizzly Bears")
                    .type(Type.CREATURE)
                    .manaCost(ManaCost.parse("{1}{G}"))
                    .power(Value.of(2))
                    .toughness(Value.of(2))
                    .build();
            var action = new PlayerAction.CastSpell(player, card);

            executor.execute(action, gameState);

            var captor = ArgumentCaptor.forClass(CastEvent.class);
            verify(eventProcessor).process(captor.capture());
            var event = captor.getValue();
            assertThat(event.card()).isEqualTo(card);
            assertThat(event.from()).isEqualTo(ZoneType.HAND);
            assertThat(event.caster()).isEqualTo(player);
        }

        @Test
        void skipsPaymentForZeroManaCost() {
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Ornithopter")
                    .type(Type.CREATURE)
                    .type(Type.ARTIFACT)
                    .manaCost(ManaCost.empty())
                    .power(Value.of(0))
                    .toughness(Value.of(2))
                    .build();
            var action = new PlayerAction.CastSpell(player, card);

            var result = executor.execute(action, gameState);

            assertThat(result).isInstanceOf(ExecutionResult.Success.class);
            verify(player, never()).pay(any(ManaCost.class), any(CostContext.class));
            verify(eventProcessor).process(any(CastEvent.class));
        }

        @Test
        void skipsPaymentForNullManaCost() {
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Ancestral Vision")
                    .type(Type.SORCERY)
                    .build();
            var action = new PlayerAction.CastSpell(player, card);

            var result = executor.execute(action, gameState);

            assertThat(result).isInstanceOf(ExecutionResult.Success.class);
            verify(player, never()).pay(any(ManaCost.class), any(CostContext.class));
            verify(eventProcessor).process(any(CastEvent.class));
        }
    }

    @Nested
    @DisplayName("ActivateAbility execution")
    class ActivateAbilityExecutionTests {

        @Test
        void returnsIllegalWhenAbilityIndexNegative() {
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test")
                    .abilities(Abilities.of())
                    .build();
            var source = Permanent.fromCard(card, player).build();

            var action = new PlayerAction.ActivateAbility(player, source, -1);
            var result = executor.execute(action, gameState);

            assertThat(result).isInstanceOf(ExecutionResult.Illegal.class);
            var illegal = (ExecutionResult.Illegal) result;
            assertThat(illegal.reason()).isEqualTo("Invalid ability index");
        }

        @Test
        void returnsIllegalWhenAbilityIndexTooLarge() {
            var ability = mock(ActivatedAbility.class);
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test")
                    .abilities(Abilities.of(ability))
                    .build();
            var source = Permanent.fromCard(card, player).build();

            var action = new PlayerAction.ActivateAbility(player, source, 5);
            var result = executor.execute(action, gameState);

            assertThat(result).isInstanceOf(ExecutionResult.Illegal.class);
            var illegal = (ExecutionResult.Illegal) result;
            assertThat(illegal.reason()).isEqualTo("Invalid ability index");
        }

        @Test
        void returnsIllegalWhenAbilityIsNotActivated() {
            var staticAbility = mock(StaticAbility.class);
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test")
                    .abilities(Abilities.of(staticAbility))
                    .build();
            var source = Permanent.fromCard(card, player).build();

            var action = new PlayerAction.ActivateAbility(player, source, 0);
            var result = executor.execute(action, gameState);

            assertThat(result).isInstanceOf(ExecutionResult.Illegal.class);
            var illegal = (ExecutionResult.Illegal) result;
            assertThat(illegal.reason()).isEqualTo("Not an activated ability");
        }

        @Test
        void returnsSuccessForActivationSuccess() {
            var ability = mock(ActivatedAbility.class);
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test")
                    .abilities(Abilities.of(ability))
                    .build();
            var source = Permanent.fromCard(card, player).build();

            var event = new TapEvent(source, true);
            var abilityOnStack = mock(AbilityOnStack.class);
            when(abilityManager.activate(eq(ability), eq(source), any(AbilityContext.class)))
                    .thenReturn(new ActivationResult.Success(abilityOnStack, List.of(event)));

            var action = new PlayerAction.ActivateAbility(player, source, 0);
            var result = executor.execute(action, gameState);

            assertThat(result).isInstanceOf(ExecutionResult.Success.class);
            var success = (ExecutionResult.Success) result;
            assertThat(success.events()).containsExactly(event);
        }

        @Test
        void returnsSuccessForManaAbilitySuccess() {
            var ability = mock(ActivatedAbility.class);
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test")
                    .abilities(Abilities.of(ability))
                    .build();
            var source = Permanent.fromCard(card, player).build();

            var event = new TapEvent(source, true);
            when(abilityManager.activate(eq(ability), eq(source), any(AbilityContext.class)))
                    .thenReturn(new ActivationResult.ManaAbilitySuccess(List.of(event)));

            var action = new PlayerAction.ActivateAbility(player, source, 0);
            var result = executor.execute(action, gameState);

            assertThat(result).isInstanceOf(ExecutionResult.Success.class);
            var success = (ExecutionResult.Success) result;
            assertThat(success.events()).containsExactly(event);
        }

        @Test
        void returnsIllegalForActivationIllegal() {
            var ability = mock(ActivatedAbility.class);
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test")
                    .abilities(Abilities.of(ability))
                    .build();
            var source = Permanent.fromCard(card, player).build();

            when(abilityManager.activate(eq(ability), eq(source), any(AbilityContext.class)))
                    .thenReturn(new ActivationResult.Illegal("Cannot activate"));

            var action = new PlayerAction.ActivateAbility(player, source, 0);
            var result = executor.execute(action, gameState);

            assertThat(result).isInstanceOf(ExecutionResult.Illegal.class);
            var illegal = (ExecutionResult.Illegal) result;
            assertThat(illegal.reason()).isEqualTo("Cannot activate");
        }

        @Test
        void delegatesToAbilityManager() {
            var ability = mock(ActivatedAbility.class);
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test")
                    .abilities(Abilities.of(ability))
                    .build();
            var source = Permanent.fromCard(card, player).build();

            when(abilityManager.activate(eq(ability), eq(source), any(AbilityContext.class)))
                    .thenReturn(new ActivationResult.ManaAbilitySuccess(List.of()));

            var action = new PlayerAction.ActivateAbility(player, source, 0);
            var result = executor.execute(action, gameState);

            assertThat(result).isInstanceOf(ExecutionResult.Success.class);
        }
    }
}
