package be.imgn.mtg.engine.trigger.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.AbilityId;
import be.imgn.mtg.engine.effect.Effect;
import be.imgn.mtg.engine.event.GameEvent;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.trigger.TriggerCondition;
import be.imgn.mtg.engine.trigger.TriggeredAbility;
import be.imgn.mtg.engine.zone.Battlefield;
import be.imgn.mtg.engine.zone.DrawEvent;
import be.imgn.mtg.engine.zone.Graveyard;
import be.imgn.mtg.engine.zone.ZoneType;

class DefaultTriggerDetectorTest {

    DefaultTriggerDetector detector;
    GameState gameState;
    Player player;
    Card source;

    @BeforeEach
    void setUp() {
        detector = new DefaultTriggerDetector();
        gameState = mock(GameState.class);
        player = mock(Player.class);
        source = mock(Card.class);
    }

    private GameEvent createEvent() {
        var card = Card.builder().owner(player).controller(player).name("Test").build();
        return new DrawEvent(card, player);
    }

    private Battlefield createBattlefield() {
        return mock(Battlefield.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
    }

    private Graveyard createGraveyard() {
        return mock(Graveyard.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
    }

    @Nested
    class Registration {

        @Test
        void registerAddsAbility() {
            var ability = new TestTriggeredAbility();

            detector.register(ability, source, player);

            // Verify by detecting - needs gameState setup
            when(gameState.findZone(source)).thenReturn(Optional.of(createBattlefield()));

            var triggered = detector.detect(createEvent(), gameState);
            assertThat(triggered).hasSize(1);
        }

        @Test
        void registerMultipleAbilitiesForSameSource() {
            var ability1 = new TestTriggeredAbility();
            var ability2 = new TestTriggeredAbility();

            detector.register(ability1, source, player);
            detector.register(ability2, source, player);

            when(gameState.findZone(source)).thenReturn(Optional.of(createBattlefield()));

            var triggered = detector.detect(createEvent(), gameState);
            assertThat(triggered).hasSize(2);
        }

        @Test
        void registerAbilitiesForDifferentSources() {
            var ability1 = new TestTriggeredAbility();
            var ability2 = new TestTriggeredAbility();
            var source2 = mock(Card.class);

            detector.register(ability1, source, player);
            detector.register(ability2, source2, player);

            when(gameState.findZone(source)).thenReturn(Optional.of(createBattlefield()));
            when(gameState.findZone(source2)).thenReturn(Optional.of(createBattlefield()));

            var triggered = detector.detect(createEvent(), gameState);
            assertThat(triggered).hasSize(2);
        }
    }

    @Nested
    class Unregistration {

        @Test
        void unregisterRemovesAbilities() {
            var ability = new TestTriggeredAbility();
            detector.register(ability, source, player);

            detector.unregister(source);

            when(gameState.findZone(source)).thenReturn(Optional.of(createBattlefield()));
            var triggered = detector.detect(createEvent(), gameState);
            assertThat(triggered).isEmpty();
        }

        @Test
        void unregisterNonexistentSourceDoesNothing() {
            var ability = new TestTriggeredAbility();
            detector.register(ability, source, player);

            detector.unregister(mock(Card.class)); // Different source

            when(gameState.findZone(source)).thenReturn(Optional.of(createBattlefield()));
            var triggered = detector.detect(createEvent(), gameState);
            assertThat(triggered).hasSize(1);
        }
    }

    @Nested
    class Detection {

        @Test
        void detectReturnsEmptyWhenNoAbilitiesRegistered() {
            var triggered = detector.detect(createEvent(), gameState);
            assertThat(triggered).isEmpty();
        }

        @Test
        void detectReturnsEmptyWhenSourceNotInZone() {
            var ability = new TestTriggeredAbility();
            detector.register(ability, source, player);

            when(gameState.findZone(source)).thenReturn(Optional.empty());

            var triggered = detector.detect(createEvent(), gameState);
            assertThat(triggered).isEmpty();
        }

        @Test
        void detectReturnsEmptyWhenSourceInWrongZone() {
            var ability = new TestTriggeredAbility(Set.of(ZoneType.BATTLEFIELD));
            detector.register(ability, source, player);

            // Source is in graveyard but ability only triggers from battlefield
            when(gameState.findZone(source)).thenReturn(Optional.of(createGraveyard()));

            var triggered = detector.detect(createEvent(), gameState);
            assertThat(triggered).isEmpty();
        }

        @Test
        void detectReturnsAbilityWhenConditionMatches() {
            var ability = new TestTriggeredAbility();
            detector.register(ability, source, player);

            when(gameState.findZone(source)).thenReturn(Optional.of(createBattlefield()));

            var triggered = detector.detect(createEvent(), gameState);
            assertThat(triggered).hasSize(1);
            assertThat(triggered.get(0).ability()).isSameAs(ability);
            assertThat(triggered.get(0).source()).isSameAs(source);
            assertThat(triggered.get(0).controller()).isSameAs(player);
        }

        @Test
        void detectReturnsEmptyWhenConditionDoesNotMatch() {
            var ability = new TestTriggeredAbility((event, state) -> false);
            detector.register(ability, source, player);

            when(gameState.findZone(source)).thenReturn(Optional.of(createBattlefield()));

            var triggered = detector.detect(createEvent(), gameState);
            assertThat(triggered).isEmpty();
        }

        @Test
        void detectReturnsEmptyWhenInterveningIfFails() {
            var ability = new TestTriggeredAbility(
                    (event, state) -> true, // condition matches
                    Optional.of(state -> false) // but intervening-if fails
                    );
            detector.register(ability, source, player);

            when(gameState.findZone(source)).thenReturn(Optional.of(createBattlefield()));

            var triggered = detector.detect(createEvent(), gameState);
            assertThat(triggered).isEmpty();
        }

        @Test
        void detectReturnsAbilityWhenInterveningIfPasses() {
            var ability = new TestTriggeredAbility((event, state) -> true, Optional.of(state -> true));
            detector.register(ability, source, player);

            when(gameState.findZone(source)).thenReturn(Optional.of(createBattlefield()));

            var triggered = detector.detect(createEvent(), gameState);
            assertThat(triggered).hasSize(1);
        }

        @Test
        void detectIncludesTriggeredEventInInstance() {
            var ability = new TestTriggeredAbility();
            detector.register(ability, source, player);

            when(gameState.findZone(source)).thenReturn(Optional.of(createBattlefield()));

            var event = createEvent();
            var triggered = detector.detect(event, gameState);

            assertThat(triggered.get(0).triggeringEvent()).isSameAs(event);
        }
    }

    static class TestTriggeredAbility implements TriggeredAbility {
        private final AbilityId id = new AbilityId();
        private final TriggerCondition condition;
        private final Optional<Predicate<GameState>> interveningIf;
        private final Set<ZoneType> triggersFrom;

        TestTriggeredAbility() {
            this((event, state) -> true, Optional.empty(), Set.of(ZoneType.BATTLEFIELD));
        }

        TestTriggeredAbility(TriggerCondition condition) {
            this(condition, Optional.empty(), Set.of(ZoneType.BATTLEFIELD));
        }

        TestTriggeredAbility(Set<ZoneType> triggersFrom) {
            this((event, state) -> true, Optional.empty(), triggersFrom);
        }

        TestTriggeredAbility(TriggerCondition condition, Optional<Predicate<GameState>> interveningIf) {
            this(condition, interveningIf, Set.of(ZoneType.BATTLEFIELD));
        }

        TestTriggeredAbility(
                TriggerCondition condition, Optional<Predicate<GameState>> interveningIf, Set<ZoneType> triggersFrom) {
            this.condition = condition;
            this.interveningIf = interveningIf;
            this.triggersFrom = triggersFrom;
        }

        @Override
        public AbilityId id() {
            return id;
        }

        @Override
        public String oracleText() {
            return "Test triggered ability";
        }

        @Override
        public TriggerCondition condition() {
            return condition;
        }

        @Override
        public Optional<Predicate<GameState>> interveningIf() {
            return interveningIf;
        }

        @Override
        public Set<ZoneType> triggersFrom() {
            return triggersFrom;
        }

        @Override
        public List<Effect> effects() {
            return List.of();
        }

        @Override
        public boolean isManaAbility() {
            return false;
        }
    }
}
