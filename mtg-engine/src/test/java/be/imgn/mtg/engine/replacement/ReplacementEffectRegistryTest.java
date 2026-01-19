package be.imgn.mtg.engine.replacement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.event.GameEvent;
import be.imgn.mtg.engine.event.ReplacementResult;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.ObjectId;
import be.imgn.mtg.engine.replacement.internal.DefaultReplacementEffectRegistry;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.zone.DrawEvent;

class ReplacementEffectRegistryTest {

    private ReplacementEffectRegistry registry;
    private Player player;
    private Card card;
    private ObjectId sourceId;
    private GameState gameState;

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
        card = mock(Card.class);
        sourceId = ObjectId.create();
        gameState = mock(GameState.class);
        registry = new DefaultReplacementEffectRegistry(gameState);
    }

    @Nested
    class Registration {

        @Test
        void registerAddsEffect() {
            var effect = new AlwaysAppliesEffect();
            registry.register(effect, sourceId, player);

            var event = new DrawEvent(card, player);
            var applicable = registry.findApplicable(event);

            assertThat(applicable).hasSize(1);
            assertThat(applicable.getFirst().effect()).isSameAs(effect);
        }

        @Test
        void registerMultipleEffectsFromSameSource() {
            var effect1 = new AlwaysAppliesEffect();
            var effect2 = new AlwaysAppliesEffect();
            registry.register(effect1, sourceId, player);
            registry.register(effect2, sourceId, player);

            var event = new DrawEvent(card, player);
            var applicable = registry.findApplicable(event);

            assertThat(applicable).hasSize(2);
        }

        @Test
        void registerEffectsFromDifferentSources() {
            var effect1 = new AlwaysAppliesEffect();
            var effect2 = new AlwaysAppliesEffect();
            var sourceId2 = ObjectId.create();

            registry.register(effect1, sourceId, player);
            registry.register(effect2, sourceId2, player);

            var event = new DrawEvent(card, player);
            var applicable = registry.findApplicable(event);

            assertThat(applicable).hasSize(2);
        }
    }

    @Nested
    class Unregistration {

        @Test
        void unregisterRemovesAllEffectsFromSource() {
            var effect1 = new AlwaysAppliesEffect();
            var effect2 = new AlwaysAppliesEffect();
            registry.register(effect1, sourceId, player);
            registry.register(effect2, sourceId, player);

            registry.unregister(sourceId);

            var event = new DrawEvent(card, player);
            var applicable = registry.findApplicable(event);

            assertThat(applicable).isEmpty();
        }

        @Test
        void unregisterDoesNotAffectOtherSources() {
            var effect1 = new AlwaysAppliesEffect();
            var effect2 = new AlwaysAppliesEffect();
            var sourceId2 = ObjectId.create();

            registry.register(effect1, sourceId, player);
            registry.register(effect2, sourceId2, player);

            registry.unregister(sourceId);

            var event = new DrawEvent(card, player);
            var applicable = registry.findApplicable(event);

            assertThat(applicable).hasSize(1);
            assertThat(applicable.getFirst().effect()).isSameAs(effect2);
        }

        @Test
        void unregisterNonExistentSourceDoesNotThrow() {
            registry.unregister(ObjectId.create());
            // Should not throw
        }
    }

    @Nested
    class FindApplicable {

        @Test
        void findsOnlyApplicableEffects() {
            var applies = new AlwaysAppliesEffect();
            var doesNotApply = new NeverAppliesEffect();

            registry.register(applies, sourceId, player);
            registry.register(doesNotApply, ObjectId.create(), player);

            var event = new DrawEvent(card, player);
            var applicable = registry.findApplicable(event);

            assertThat(applicable).hasSize(1);
            assertThat(applicable.getFirst().effect()).isSameAs(applies);
        }

        @Test
        void returnsEmptyWhenNoEffectsApply() {
            var doesNotApply = new NeverAppliesEffect();
            registry.register(doesNotApply, sourceId, player);

            var event = new DrawEvent(card, player);
            var applicable = registry.findApplicable(event);

            assertThat(applicable).isEmpty();
        }

        @Test
        void returnsEmptyWhenNoEffectsRegistered() {
            var event = new DrawEvent(card, player);
            var applicable = registry.findApplicable(event);

            assertThat(applicable).isEmpty();
        }

        @Test
        void includesSourceAndControllerInfo() {
            var effect = new AlwaysAppliesEffect();
            registry.register(effect, sourceId, player);

            var event = new DrawEvent(card, player);
            var applicable = registry.findApplicable(event);

            assertThat(applicable).hasSize(1);
            assertThat(applicable.getFirst().source()).isEqualTo(sourceId);
            assertThat(applicable.getFirst().controller()).isSameAs(player);
        }
    }

    @Nested
    class Rule616Ordering {

        @Test
        void selfReplacementEffectsFirst() {
            var normal = new AlwaysAppliesEffect();
            var selfReplacement = new SelfReplacementEffect();

            registry.register(normal, ObjectId.create(), player);
            registry.register(selfReplacement, ObjectId.create(), player);

            var event = new DrawEvent(card, player);
            var applicable = registry.findApplicable(event);

            assertThat(applicable).hasSize(2);
            assertThat(applicable.get(0).effect()).isSameAs(selfReplacement);
            assertThat(applicable.get(1).effect()).isSameAs(normal);
        }

        @Test
        void controlChangingBeforeCopyEffects() {
            var copyEffect = new CopyEffect();
            var controlChanging = new ControlChangingEffect();
            var normal = new AlwaysAppliesEffect();

            registry.register(normal, ObjectId.create(), player);
            registry.register(copyEffect, ObjectId.create(), player);
            registry.register(controlChanging, ObjectId.create(), player);

            var event = new DrawEvent(card, player);
            var applicable = registry.findApplicable(event);

            assertThat(applicable).hasSize(3);
            // Order: control-changing, copy, normal
            assertThat(applicable.get(0).effect()).isSameAs(controlChanging);
            assertThat(applicable.get(1).effect()).isSameAs(copyEffect);
            assertThat(applicable.get(2).effect()).isSameAs(normal);
        }

        @Test
        void fullRule616Order() {
            var normal = new AlwaysAppliesEffect();
            var copyEffect = new CopyEffect();
            var controlChanging = new ControlChangingEffect();
            var selfReplacement = new SelfReplacementEffect();

            registry.register(normal, ObjectId.create(), player);
            registry.register(copyEffect, ObjectId.create(), player);
            registry.register(controlChanging, ObjectId.create(), player);
            registry.register(selfReplacement, ObjectId.create(), player);

            var event = new DrawEvent(card, player);
            var applicable = registry.findApplicable(event);

            assertThat(applicable).hasSize(4);
            // Order: self-replacement, control-changing, copy, normal
            assertThat(applicable.get(0).effect().isSelfReplacement()).isTrue();
            assertThat(applicable.get(1).effect().isControlChanging()).isTrue();
            assertThat(applicable.get(2).effect().isCopyEffect()).isTrue();
            assertThat(applicable.get(3).effect()).isSameAs(normal);
        }
    }

    // Test implementations

    static class AlwaysAppliesEffect implements ReplacementEffect {
        @Override
        public boolean appliesTo(GameEvent event, GameState state) {
            return true;
        }

        @Override
        public ReplacementResult replace(GameEvent event, GameState state) {
            return new ReplacementResult.Modified(event);
        }
    }

    static class NeverAppliesEffect implements ReplacementEffect {
        @Override
        public boolean appliesTo(GameEvent event, GameState state) {
            return false;
        }

        @Override
        public ReplacementResult replace(GameEvent event, GameState state) {
            return new ReplacementResult.Modified(event);
        }
    }

    static class SelfReplacementEffect implements ReplacementEffect {
        @Override
        public boolean appliesTo(GameEvent event, GameState state) {
            return true;
        }

        @Override
        public ReplacementResult replace(GameEvent event, GameState state) {
            return new ReplacementResult.Modified(event);
        }

        @Override
        public boolean isSelfReplacement() {
            return true;
        }
    }

    static class ControlChangingEffect implements ReplacementEffect {
        @Override
        public boolean appliesTo(GameEvent event, GameState state) {
            return true;
        }

        @Override
        public ReplacementResult replace(GameEvent event, GameState state) {
            return new ReplacementResult.Modified(event);
        }

        @Override
        public boolean isControlChanging() {
            return true;
        }
    }

    static class CopyEffect implements ReplacementEffect {
        @Override
        public boolean appliesTo(GameEvent event, GameState state) {
            return true;
        }

        @Override
        public ReplacementResult replace(GameEvent event, GameState state) {
            return new ReplacementResult.Modified(event);
        }

        @Override
        public boolean isCopyEffect() {
            return true;
        }
    }
}
