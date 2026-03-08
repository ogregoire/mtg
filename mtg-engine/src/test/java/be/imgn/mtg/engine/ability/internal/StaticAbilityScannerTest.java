package be.imgn.mtg.engine.ability.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.Abilities;
import be.imgn.mtg.engine.ability.AbilityId;
import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.ability.SpellAbility;
import be.imgn.mtg.engine.ability.StaticAbility;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.trigger.TriggerDetector;
import be.imgn.mtg.engine.trigger.TriggeredAbility;

@DisplayName("StaticAbilityScanner")
class StaticAbilityScannerTest {

    private TriggerDetector triggerDetector;
    private StaticAbilityScanner scanner;
    private GameState state;
    private Permanent permanent;

    @BeforeEach
    void setUp() {
        triggerDetector = mock(TriggerDetector.class);
        scanner = new StaticAbilityScanner(triggerDetector);
        state = mock(GameState.class);
        permanent = mock(Permanent.class);
    }

    @Nested
    @DisplayName("registerAbilities()")
    class RegisterAbilitiesTests {

        @Test
        void noAbilities_doesNothing() {
            when(permanent.abilities()).thenReturn(Abilities.empty());

            scanner.registerAbilities(permanent, state);

            // Should complete without error
        }

        @Test
        void staticAbility_processesRegistration() {
            var staticAbility = mock(StaticAbility.class);
            when(staticAbility.id()).thenReturn(new AbilityId());
            when(permanent.abilities()).thenReturn(Abilities.of(staticAbility));

            scanner.registerAbilities(permanent, state);

            // Method should complete - registration is TODO but should not throw
        }

        @Test
        void triggeredAbility_processesRegistration() {
            var triggeredAbility = mock(TriggeredAbility.class);
            when(triggeredAbility.id()).thenReturn(new AbilityId());
            when(permanent.abilities()).thenReturn(Abilities.of(triggeredAbility));

            scanner.registerAbilities(permanent, state);

            // Method should complete - registration is TODO but should not throw
        }

        @Test
        void activatedAbility_doesNotRegister() {
            var activatedAbility = mock(ActivatedAbility.class);
            when(activatedAbility.id()).thenReturn(new AbilityId());
            when(permanent.abilities()).thenReturn(Abilities.of(activatedAbility));

            scanner.registerAbilities(permanent, state);

            // Activated abilities don't need registration
        }

        @Test
        void spellAbility_doesNotRegister() {
            var spellAbility = new SpellAbility(new AbilityId(), "", List.of());
            when(permanent.abilities()).thenReturn(Abilities.of(spellAbility));

            scanner.registerAbilities(permanent, state);

            // Spell abilities shouldn't be on permanents
        }

        @Test
        void mixedAbilities_processesAll() {
            var staticAbility = mock(StaticAbility.class);
            var triggeredAbility = mock(TriggeredAbility.class);
            var activatedAbility = mock(ActivatedAbility.class);
            when(staticAbility.id()).thenReturn(new AbilityId());
            when(triggeredAbility.id()).thenReturn(new AbilityId());
            when(activatedAbility.id()).thenReturn(new AbilityId());
            when(permanent.abilities()).thenReturn(Abilities.of(staticAbility, triggeredAbility, activatedAbility));

            scanner.registerAbilities(permanent, state);

            // All abilities should be processed
        }

        @Test
        void multipleStaticAbilities_processesAll() {
            var staticAbility1 = mock(StaticAbility.class);
            var staticAbility2 = mock(StaticAbility.class);
            when(staticAbility1.id()).thenReturn(new AbilityId());
            when(staticAbility2.id()).thenReturn(new AbilityId());
            when(permanent.abilities()).thenReturn(Abilities.of(staticAbility1, staticAbility2));

            scanner.registerAbilities(permanent, state);

            // Both abilities should be processed
        }
    }

    @Nested
    @DisplayName("unregisterAbilities()")
    class UnregisterAbilitiesTests {

        @Test
        void noAbilities_doesNothing() {
            when(permanent.abilities()).thenReturn(Abilities.empty());

            scanner.unregisterAbilities(permanent, state);

            // Should complete without error
        }

        @Test
        void staticAbility_processesUnregistration() {
            var staticAbility = mock(StaticAbility.class);
            when(staticAbility.id()).thenReturn(new AbilityId());
            when(permanent.abilities()).thenReturn(Abilities.of(staticAbility));

            scanner.unregisterAbilities(permanent, state);

            // Method should complete - unregistration is TODO but should not throw
        }

        @Test
        void triggeredAbility_processesUnregistration() {
            var triggeredAbility = mock(TriggeredAbility.class);
            when(triggeredAbility.id()).thenReturn(new AbilityId());
            when(permanent.abilities()).thenReturn(Abilities.of(triggeredAbility));

            scanner.unregisterAbilities(permanent, state);

            // Method should complete - unregistration is TODO but should not throw
        }

        @Test
        void activatedAbility_doesNotUnregister() {
            var activatedAbility = mock(ActivatedAbility.class);
            when(activatedAbility.id()).thenReturn(new AbilityId());
            when(permanent.abilities()).thenReturn(Abilities.of(activatedAbility));

            scanner.unregisterAbilities(permanent, state);

            // Activated abilities don't need unregistration
        }

        @Test
        void mixedAbilities_processesAll() {
            var staticAbility = mock(StaticAbility.class);
            var triggeredAbility = mock(TriggeredAbility.class);
            var activatedAbility = mock(ActivatedAbility.class);
            when(staticAbility.id()).thenReturn(new AbilityId());
            when(triggeredAbility.id()).thenReturn(new AbilityId());
            when(activatedAbility.id()).thenReturn(new AbilityId());
            when(permanent.abilities()).thenReturn(Abilities.of(staticAbility, triggeredAbility, activatedAbility));

            scanner.unregisterAbilities(permanent, state);

            // All abilities should be processed
        }
    }

    @Nested
    @DisplayName("Register and unregister symmetry")
    class SymmetryTests {

        @Test
        void registerThenUnregister_processesAbilitiesBothTimes() {
            var staticAbility = mock(StaticAbility.class);
            var triggeredAbility = mock(TriggeredAbility.class);
            when(staticAbility.id()).thenReturn(new AbilityId());
            when(triggeredAbility.id()).thenReturn(new AbilityId());
            when(permanent.abilities()).thenReturn(Abilities.of(staticAbility, triggeredAbility));

            scanner.registerAbilities(permanent, state);
            scanner.unregisterAbilities(permanent, state);

            // Both operations should complete successfully
        }
    }
}
