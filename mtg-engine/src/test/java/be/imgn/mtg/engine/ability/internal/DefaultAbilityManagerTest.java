package be.imgn.mtg.engine.ability.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.AbilityContext;
import be.imgn.mtg.engine.ability.AbilityId;
import be.imgn.mtg.engine.ability.AbilityManager;
import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.ability.ActivationResult;
import be.imgn.mtg.engine.ability.StaticAbility;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.trigger.TriggeredAbility;
import be.imgn.mtg.engine.zone.Stack;

@DisplayName("DefaultAbilityManager")
class DefaultAbilityManagerTest {

    private ActivatedAbilityHandler activatedHandler;
    private ManaAbilityHandler manaHandler;
    private LoyaltyAbilityHandler loyaltyHandler;
    private StaticAbilityScanner scanner;
    private Stack stack;
    private AbilityManager manager;
    private GameState state;
    private GameObject source;
    private Player controller;

    @BeforeEach
    void setUp() {
        activatedHandler = mock(ActivatedAbilityHandler.class);
        manaHandler = mock(ManaAbilityHandler.class);
        loyaltyHandler = mock(LoyaltyAbilityHandler.class);
        scanner = mock(StaticAbilityScanner.class);
        stack = mock(Stack.class);
        manager = new DefaultAbilityManager(activatedHandler, manaHandler, loyaltyHandler, scanner, stack);
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
        void manaAbility_delegatesToManaHandler() {
            var ability = mock(ActivatedAbility.class);
            when(ability.isManaAbility()).thenReturn(true);
            when(manaHandler.canActivate(ability, source, state)).thenReturn(true);

            var result = manager.canActivate(ability, source, state);

            verify(manaHandler).canActivate(ability, source, state);
            assertThat(result).isTrue();
        }

        @Test
        void loyaltyAbility_delegatesToLoyaltyHandler() {
            var ability = mock(ActivatedAbility.class);
            when(ability.isManaAbility()).thenReturn(false);
            when(ability.isLoyaltyAbility()).thenReturn(true);
            when(loyaltyHandler.canActivate(ability, source, state)).thenReturn(true);

            var result = manager.canActivate(ability, source, state);

            verify(loyaltyHandler).canActivate(ability, source, state);
            assertThat(result).isTrue();
        }

        @Test
        void regularActivatedAbility_delegatesToActivatedHandler() {
            var ability = mock(ActivatedAbility.class);
            when(ability.isManaAbility()).thenReturn(false);
            when(ability.isLoyaltyAbility()).thenReturn(false);
            when(activatedHandler.canActivate(ability, source, state)).thenReturn(true);

            var result = manager.canActivate(ability, source, state);

            verify(activatedHandler).canActivate(ability, source, state);
            assertThat(result).isTrue();
        }

        @Test
        void handlerReturnsFalse_returnsFalse() {
            var ability = mock(ActivatedAbility.class);
            when(ability.isManaAbility()).thenReturn(false);
            when(ability.isLoyaltyAbility()).thenReturn(false);
            when(activatedHandler.canActivate(ability, source, state)).thenReturn(false);

            var result = manager.canActivate(ability, source, state);

            assertThat(result).isFalse();
        }

        @Test
        void manaAbilityTakesPrecedenceOverLoyalty() {
            // A mana ability should never be a loyalty ability, but test routing priority
            var ability = mock(ActivatedAbility.class);
            when(ability.isManaAbility()).thenReturn(true);
            when(ability.isLoyaltyAbility()).thenReturn(true);
            when(manaHandler.canActivate(ability, source, state)).thenReturn(true);

            var result = manager.canActivate(ability, source, state);

            verify(manaHandler).canActivate(ability, source, state);
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("activate()")
    class ActivateTests {

        @Test
        void manaAbility_delegatesToManaHandler() {
            var ability = mock(ActivatedAbility.class);
            var context = new AbilityContext(source, controller, state);
            var expectedResult = new ActivationResult.ManaAbilitySuccess(List.of());
            when(ability.isManaAbility()).thenReturn(true);
            when(manaHandler.activate(ability, source, context)).thenReturn(expectedResult);

            var result = manager.activate(ability, source, context);

            verify(manaHandler).activate(ability, source, context);
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        void loyaltyAbility_delegatesToLoyaltyHandler() {
            var ability = mock(ActivatedAbility.class);
            var context = new AbilityContext(source, controller, state);
            var expectedResult = new ActivationResult.Illegal("Test");
            when(ability.isManaAbility()).thenReturn(false);
            when(ability.isLoyaltyAbility()).thenReturn(true);
            when(loyaltyHandler.activate(ability, source, context, stack)).thenReturn(expectedResult);

            var result = manager.activate(ability, source, context);

            verify(loyaltyHandler).activate(ability, source, context, stack);
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        void regularActivatedAbility_delegatesToActivatedHandler() {
            var ability = mock(ActivatedAbility.class);
            var context = new AbilityContext(source, controller, state);
            var expectedResult = new ActivationResult.Illegal("Test");
            when(ability.isManaAbility()).thenReturn(false);
            when(ability.isLoyaltyAbility()).thenReturn(false);
            when(activatedHandler.activate(ability, source, context, stack)).thenReturn(expectedResult);

            var result = manager.activate(ability, source, context);

            verify(activatedHandler).activate(ability, source, context, stack);
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        void loyaltyHandlerReceivesStack() {
            var ability = mock(ActivatedAbility.class);
            var context = new AbilityContext(source, controller, state);
            when(ability.isManaAbility()).thenReturn(false);
            when(ability.isLoyaltyAbility()).thenReturn(true);
            when(loyaltyHandler.activate(ability, source, context, stack))
                    .thenReturn(new ActivationResult.Illegal("Test"));

            manager.activate(ability, source, context);

            verify(loyaltyHandler).activate(ability, source, context, stack);
        }

        @Test
        void activatedHandlerReceivesStack() {
            var ability = mock(ActivatedAbility.class);
            var context = new AbilityContext(source, controller, state);
            when(ability.isManaAbility()).thenReturn(false);
            when(ability.isLoyaltyAbility()).thenReturn(false);
            when(activatedHandler.activate(ability, source, context, stack))
                    .thenReturn(new ActivationResult.Illegal("Test"));

            manager.activate(ability, source, context);

            verify(activatedHandler).activate(ability, source, context, stack);
        }
    }

    @Nested
    @DisplayName("getActivatableAbilities()")
    class GetActivatableAbilitiesTests {

        @Test
        void noAbilities_returnsEmptyList() {
            var testSource = Card.builder()
                    .owner(controller)
                    .controller(controller)
                    .name("Test Source")
                    .build();

            var result = manager.getActivatableAbilities(testSource, state);

            assertThat(result).isEmpty();
        }

        @Test
        void onlyNonActivatedAbilities_returnsEmptyList() {
            var staticAbility = mock(StaticAbility.class);
            var triggeredAbility = mock(TriggeredAbility.class);
            when(staticAbility.id()).thenReturn(new AbilityId());
            when(triggeredAbility.id()).thenReturn(new AbilityId());
            var testSource = Card.builder()
                    .owner(controller)
                    .controller(controller)
                    .name("Test Source")
                    .abilities(staticAbility, triggeredAbility)
                    .build();

            var result = manager.getActivatableAbilities(testSource, state);

            assertThat(result).isEmpty();
        }

        @Test
        void singleActivatableAbility_returnsThatAbility() {
            var ability = mock(ActivatedAbility.class);
            when(ability.id()).thenReturn(new AbilityId());
            when(ability.isManaAbility()).thenReturn(false);
            when(ability.isLoyaltyAbility()).thenReturn(false);
            var testSource = Card.builder()
                    .owner(controller)
                    .controller(controller)
                    .name("Test Source")
                    .ability(ability)
                    .build();
            when(activatedHandler.canActivate(ability, testSource, state)).thenReturn(true);

            var result = manager.getActivatableAbilities(testSource, state);

            assertThat(result).containsExactly(ability);
        }

        @Test
        void multipleActivatableAbilities_returnsAll() {
            var ability1 = mock(ActivatedAbility.class);
            var ability2 = mock(ActivatedAbility.class);
            when(ability1.id()).thenReturn(new AbilityId());
            when(ability2.id()).thenReturn(new AbilityId());
            when(ability1.isManaAbility()).thenReturn(false);
            when(ability2.isManaAbility()).thenReturn(true);
            when(ability1.isLoyaltyAbility()).thenReturn(false);
            var testSource = Card.builder()
                    .owner(controller)
                    .controller(controller)
                    .name("Test Source")
                    .abilities(ability1, ability2)
                    .build();
            when(activatedHandler.canActivate(ability1, testSource, state)).thenReturn(true);
            when(manaHandler.canActivate(ability2, testSource, state)).thenReturn(true);

            var result = manager.getActivatableAbilities(testSource, state);

            assertThat(result).containsExactly(ability1, ability2);
        }

        @Test
        void mixedActivatableAndNonActivatable_returnsOnlyActivatable() {
            var ability1 = mock(ActivatedAbility.class);
            var ability2 = mock(ActivatedAbility.class);
            when(ability1.id()).thenReturn(new AbilityId());
            when(ability2.id()).thenReturn(new AbilityId());
            when(ability1.isManaAbility()).thenReturn(false);
            when(ability2.isManaAbility()).thenReturn(false);
            when(ability1.isLoyaltyAbility()).thenReturn(false);
            when(ability2.isLoyaltyAbility()).thenReturn(false);
            var testSource = Card.builder()
                    .owner(controller)
                    .controller(controller)
                    .name("Test Source")
                    .abilities(ability1, ability2)
                    .build();
            when(activatedHandler.canActivate(ability1, testSource, state)).thenReturn(true);
            when(activatedHandler.canActivate(ability2, testSource, state)).thenReturn(false);

            var result = manager.getActivatableAbilities(testSource, state);

            assertThat(result).containsExactly(ability1);
        }

        @Test
        void noneCanActivate_returnsEmptyList() {
            var ability = mock(ActivatedAbility.class);
            when(ability.id()).thenReturn(new AbilityId());
            when(ability.isManaAbility()).thenReturn(false);
            when(ability.isLoyaltyAbility()).thenReturn(false);
            var testSource = Card.builder()
                    .owner(controller)
                    .controller(controller)
                    .name("Test Source")
                    .ability(ability)
                    .build();
            when(activatedHandler.canActivate(ability, testSource, state)).thenReturn(false);

            var result = manager.getActivatableAbilities(testSource, state);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("registerAbilities()")
    class RegisterAbilitiesTests {

        @Test
        void delegatesToScanner() {
            var permanent = mock(Permanent.class);

            manager.registerAbilities(permanent, state);

            verify(scanner).registerAbilities(permanent, state);
        }
    }

    @Nested
    @DisplayName("unregisterAbilities()")
    class UnregisterAbilitiesTests {

        @Test
        void delegatesToScanner() {
            var permanent = mock(Permanent.class);

            manager.unregisterAbilities(permanent, state);

            verify(scanner).unregisterAbilities(permanent, state);
        }
    }

    @Nested
    @DisplayName("Integration scenarios")
    class IntegrationTests {

        @Test
        void permanentWithMixedAbilities_correctlyRoutes() {
            var manaAbility = mock(ActivatedAbility.class);
            var loyaltyAbility = mock(ActivatedAbility.class);
            var activatedAbility = mock(ActivatedAbility.class);

            when(manaAbility.id()).thenReturn(new AbilityId());
            when(loyaltyAbility.id()).thenReturn(new AbilityId());
            when(activatedAbility.id()).thenReturn(new AbilityId());

            when(manaAbility.isManaAbility()).thenReturn(true);
            when(loyaltyAbility.isManaAbility()).thenReturn(false);
            when(loyaltyAbility.isLoyaltyAbility()).thenReturn(true);
            when(activatedAbility.isManaAbility()).thenReturn(false);
            when(activatedAbility.isLoyaltyAbility()).thenReturn(false);

            var testSource = Card.builder()
                    .owner(controller)
                    .controller(controller)
                    .name("Test Source")
                    .abilities(manaAbility, loyaltyAbility, activatedAbility)
                    .build();
            when(manaHandler.canActivate(manaAbility, testSource, state)).thenReturn(true);
            when(loyaltyHandler.canActivate(loyaltyAbility, testSource, state)).thenReturn(true);
            when(activatedHandler.canActivate(activatedAbility, testSource, state))
                    .thenReturn(true);

            var result = manager.getActivatableAbilities(testSource, state);

            assertThat(result).containsExactly(manaAbility, loyaltyAbility, activatedAbility);
        }
    }
}
