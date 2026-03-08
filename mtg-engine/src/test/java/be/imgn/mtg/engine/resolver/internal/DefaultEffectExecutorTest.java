package be.imgn.mtg.engine.resolver.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.internal.parser.reference.PronounType;
import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.combat.DamageEvent;
import be.imgn.mtg.engine.effect.CompoundEffect;
import be.imgn.mtg.engine.effect.DealDamageEffect;
import be.imgn.mtg.engine.effect.DestroyEffect;
import be.imgn.mtg.engine.effect.DiscardEffect;
import be.imgn.mtg.engine.effect.DrawEffect;
import be.imgn.mtg.engine.effect.GainLifeEffect;
import be.imgn.mtg.engine.effect.TapEffect;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.object.Spell;
import be.imgn.mtg.engine.resolver.ResolutionContext;
import be.imgn.mtg.engine.selector.ObjectSelector;
import be.imgn.mtg.engine.selector.PlayerReference;
import be.imgn.mtg.engine.selector.Quantifier;
import be.imgn.mtg.engine.selector.Selectable;
import be.imgn.mtg.engine.selector.TypeMatcher;
import be.imgn.mtg.engine.spell.SpellContext;
import be.imgn.mtg.engine.spell.TargetChoice;
import be.imgn.mtg.engine.spell.TargetChoices;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.zone.DiesEvent;
import be.imgn.mtg.engine.zone.DiscardEvent;
import be.imgn.mtg.engine.zone.DrawEvent;
import be.imgn.mtg.engine.zone.Hand;
import be.imgn.mtg.engine.zone.Library;

class DefaultEffectExecutorTest {

    private DefaultEffectExecutor executor;
    private GameEventProcessor eventProcessor;
    private GameState gameState;
    private Player controller;
    private Spell spell;
    private ResolutionContext context;

    @BeforeEach
    void setUp() {
        executor = new DefaultEffectExecutor();
        eventProcessor = mock(GameEventProcessor.class);
        gameState = mock(GameState.class);
        controller = mock(Player.class);
        spell = mock(Spell.class);
        context = new ResolutionContext(gameState, spell, controller, SpellContext.empty(), eventProcessor);
    }

    @Nested
    class UnsupportedEffects {

        @Test
        void throwsForUnimplementedEffect() {
            var effect = new TapEffect(new Subject.Pronoun(PronounType.IT));

            assertThatThrownBy(() -> executor.execute(effect, context))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class GainLifeEffectTests {

        @Test
        void controllerGainsLife() {
            var effect = new GainLifeEffect(Optional.empty(), new Amount.Exact(3));
            executor.execute(effect, context);
            verify(controller).gainLife(3);
        }

        @Test
        void specifiedPlayerGainsLife() {
            var effect = new GainLifeEffect(Optional.of(PlayerReference.YOU), new Amount.Exact(5));
            executor.execute(effect, context);
            verify(controller).gainLife(5);
        }
    }

    @Nested
    class DrawEffectTests {

        @Test
        void controllerDrawsCards() {
            var library = mock(Library.class);
            var card1 = mock(Card.class);
            var card2 = mock(Card.class);
            when(gameState.library(controller)).thenReturn(library);
            when(library.peekTop()).thenReturn(Optional.of(card1), Optional.of(card2));

            var effect = new DrawEffect(Optional.empty(), new Amount.Exact(2));
            executor.execute(effect, context);

            verify(eventProcessor, times(2)).process(any(DrawEvent.class));
        }
    }

    @Nested
    class DealDamageEffectTests {

        @Test
        void dealsNonCombatDamageToTargetPlayer() {
            var targetPlayer = mock(Player.class);
            var subject = subjectTargeting(targetPlayer);
            var ctx = contextWithTarget(subject, targetPlayer);

            var effect = new DealDamageEffect(new Amount.Exact(3), subject);
            executor.execute(effect, ctx);

            verify(eventProcessor).process(any(DamageEvent.class));
        }
    }

    @Nested
    class DestroyEffectTests {

        @Test
        void destroysTargetPermanent() {
            var permanent = mock(Permanent.class);
            when(permanent.controller()).thenReturn(controller);
            var subject = subjectTargeting(permanent);
            var ctx = contextWithTarget(subject, permanent);

            var effect = new DestroyEffect(subject, true);
            executor.execute(effect, ctx);

            verify(eventProcessor).process(any(DiesEvent.class));
        }
    }

    @Nested
    class DiscardEffectTests {

        @Test
        void controllerDiscardsCard() {
            var hand = mock(Hand.class);
            var card = mock(Card.class);
            when(gameState.hand(controller)).thenReturn(hand);
            when(hand.stream()).thenReturn(Stream.of(card));
            when(controller.choose(any())).thenReturn(List.of(card));

            var effect = new DiscardEffect(Optional.empty(), new Amount.Exact(1));
            executor.execute(effect, context);

            verify(eventProcessor).process(any(DiscardEvent.class));
        }
    }

    @Nested
    class CompoundEffectTests {

        @Test
        void executesAllSubEffects() {
            var effect = new CompoundEffect(List.of(
                    new GainLifeEffect(Optional.empty(), new Amount.Exact(3)),
                    new GainLifeEffect(Optional.empty(), new Amount.Exact(2))));
            executor.execute(effect, context);
            verify(controller).gainLife(3);
            verify(controller).gainLife(2);
        }
    }

    private Subject.Select subjectTargeting(Selectable target) {
        var selector = new ObjectSelector(
                new Quantifier.One(), List.of(), new TypeMatcher.Single(Type.CREATURE), List.of(), null);
        return new Subject.Select(selector);
    }

    private ResolutionContext contextWithTarget(Subject subject, Selectable target) {
        var targets = new TargetChoices(List.of(new TargetChoice(subject, target)));
        return new ResolutionContext(gameState, spell, controller, new SpellContext(targets), eventProcessor);
    }
}
