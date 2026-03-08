package be.imgn.mtg.engine.zone.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.AbilityId;
import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.ability.SpellAbility;
import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.effect.DestroyEffect;
import be.imgn.mtg.engine.effect.DrawEffect;
import be.imgn.mtg.engine.effect.Effect;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.AbilityOnStack;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.object.Spell;
import be.imgn.mtg.engine.resolver.EffectExecutor;
import be.imgn.mtg.engine.resolver.ResolutionContext;
import be.imgn.mtg.engine.selector.ObjectSelector;
import be.imgn.mtg.engine.selector.Selectable;
import be.imgn.mtg.engine.spell.SpellContext;
import be.imgn.mtg.engine.spell.TargetChoice;
import be.imgn.mtg.engine.spell.TargetChoices;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.state.internal.ObjectStore;
import be.imgn.mtg.engine.zone.Battlefield;
import be.imgn.mtg.engine.zone.PutIntoGraveyardEvent;
import be.imgn.mtg.engine.zone.ZoneType;

class DefaultStackTest {

    Player player;
    GameEventProcessor eventProcessor;
    GameState gameState;
    Battlefield battlefield;
    EffectExecutor effectExecutor;
    DefaultStack stack;

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
        eventProcessor = mock(GameEventProcessor.class);
        gameState = mock(GameState.class);
        battlefield = mock(Battlefield.class);
        effectExecutor = mock(EffectExecutor.class);
        when(gameState.battlefield()).thenReturn(battlefield);
        stack = new DefaultStack(new ObjectStore(), eventProcessor, gameState, effectExecutor);
    }

    private Spell createSpell(String name) {
        var card = Card.builder()
                .owner(player)
                .controller(player)
                .name(name)
                .type(Type.INSTANT)
                .build();
        return Spell.fromCard(card, player).build();
    }

    private AbilityOnStack createAbility() {
        var ability = mock(ActivatedAbility.class);
        var card = Card.builder()
                .owner(player)
                .controller(player)
                .name("Source")
                .type(Type.CREATURE)
                .power(Value.of(1))
                .toughness(Value.of(1))
                .build();
        var permanent = Permanent.fromCard(card, player).build();
        return AbilityOnStack.from(ability, permanent).build();
    }

    @Nested
    class BasicOperations {

        @Test
        void emptyStack() {
            assertThat(stack.isEmpty()).isTrue();
            assertThat(stack.size()).isZero();
        }

        @Test
        void pushIncreasesSize() {
            var spell = createSpell("Lightning Bolt");

            stack.push(spell);

            assertThat(stack.size()).isEqualTo(1);
            assertThat(stack.isEmpty()).isFalse();
        }

        @Test
        void containsReturnsTrueForPushedObject() {
            var spell = createSpell("Lightning Bolt");
            stack.push(spell);

            assertThat(stack.contains(spell)).isTrue();
        }

        @Test
        void containsReturnsFalseForAbsentObject() {
            var spell = createSpell("Not Pushed");
            assertThat(stack.contains(spell)).isFalse();
        }

        @Test
        void pushAbility() {
            var ability = createAbility();

            stack.push(ability);

            assertThat(stack.size()).isEqualTo(1);
            assertThat(stack.contains(ability)).isTrue();
        }
    }

    @Nested
    class PeekOperations {

        @Test
        void peekOnEmpty() {
            assertThat(stack.peek()).isEmpty();
        }

        @Test
        void peekReturnsTopObject() {
            var spell1 = createSpell("Spell 1");
            var spell2 = createSpell("Spell 2");
            stack.push(spell1);
            stack.push(spell2);

            assertThat(stack.peek()).contains(spell2);
        }

        @Test
        void peekDoesNotRemove() {
            var spell = createSpell("Lightning Bolt");
            stack.push(spell);

            stack.peek();

            assertThat(stack.size()).isEqualTo(1);
        }
    }

    @Nested
    class PopOperations {

        @Test
        void popOnEmpty() {
            assertThat(stack.pop()).isEmpty();
        }

        @Test
        void popRemovesAndReturnsTopObject() {
            var spell1 = createSpell("Spell 1");
            var spell2 = createSpell("Spell 2");
            stack.push(spell1);
            stack.push(spell2);

            var popped = stack.pop();

            assertThat(popped).contains(spell2);
            assertThat(stack.size()).isEqualTo(1);
            assertThat(stack.peek()).contains(spell1);
        }

        @Test
        void popUpdatesContains() {
            var spell = createSpell("Lightning Bolt");
            stack.push(spell);

            stack.pop();

            assertThat(stack.contains(spell)).isFalse();
        }
    }

    @Nested
    class RemoveOperations {

        @Test
        void removeNonExistent() {
            var spell = createSpell("Not Pushed");
            var removed = stack.remove(spell);

            assertThat(removed).isFalse();
        }

        @Test
        void removeFromMiddle() {
            var spell1 = createSpell("Spell 1");
            var spell2 = createSpell("Spell 2");
            var spell3 = createSpell("Spell 3");
            stack.push(spell1);
            stack.push(spell2);
            stack.push(spell3);

            var removed = stack.remove(spell2);

            assertThat(removed).isTrue();
            assertThat(stack.size()).isEqualTo(2);
            assertThat(stack.contains(spell2)).isFalse();
        }

        @Test
        void removeFromTop() {
            var spell1 = createSpell("Spell 1");
            var spell2 = createSpell("Spell 2");
            stack.push(spell1);
            stack.push(spell2);

            var removed = stack.remove(spell2);

            assertThat(removed).isTrue();
            assertThat(stack.peek()).contains(spell1);
        }
    }

    @Nested
    class AllOperations {

        @Test
        void allReturnsEmptyForEmptyStack() {
            assertThat(stack.all()).isEmpty();
        }

        @Test
        void allReturnsObjectsInOrder() {
            var spell1 = createSpell("Spell 1");
            var spell2 = createSpell("Spell 2");
            var spell3 = createSpell("Spell 3");
            stack.push(spell1);
            stack.push(spell2);
            stack.push(spell3);

            // Top of stack should be first
            assertThat(stack.all()).containsExactly(spell3, spell2, spell1);
        }

        @Test
        void allReturnsCopy() {
            var spell = createSpell("Lightning Bolt");
            stack.push(spell);

            var all = stack.all();

            try {
                all.clear();
            } catch (UnsupportedOperationException e) {
                // Expected for immutable copy
            }
            assertThat(stack.size()).isEqualTo(1);
        }
    }

    @Nested
    class StreamOperations {

        @Test
        void streamReturnsAllObjects() {
            var spell1 = createSpell("Spell 1");
            var spell2 = createSpell("Spell 2");
            stack.push(spell1);
            stack.push(spell2);

            var objects = stack.stream().toList();

            assertThat(objects).containsExactlyInAnyOrder(spell1, spell2);
        }

        @Test
        void streamOnEmptyStack() {
            assertThat(stack.stream().toList()).isEmpty();
        }
    }

    @Nested
    class MixedObjectTypes {

        @Test
        void stackHandlesSpellsAndAbilities() {
            var spell = createSpell("Lightning Bolt");
            var ability = createAbility();

            stack.push(spell);
            stack.push(ability);

            assertThat(stack.size()).isEqualTo(2);
            assertThat(stack.peek()).contains(ability);
            assertThat(stack.contains(spell)).isTrue();
            assertThat(stack.contains(ability)).isTrue();
        }
    }

    @Nested
    class ResolveOperations {

        @Test
        void resolveOnEmptyStackDoesNothing() {
            stack.resolve();

            assertThat(stack.isEmpty()).isTrue();
        }

        @Test
        void resolvePermanentSpellEntersBattlefield() {
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Grizzly Bears")
                    .type(Type.CREATURE)
                    .power(Value.of(2))
                    .toughness(Value.of(2))
                    .build();
            var spell = Spell.fromCard(card, player).build();
            stack.push(spell);

            stack.resolve();

            verify(battlefield).enter(card, player, ZoneType.STACK);
            assertThat(stack.isEmpty()).isTrue();
        }

        @Test
        void resolveInstantSpellGoesToGraveyard() {
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Lightning Bolt")
                    .type(Type.INSTANT)
                    .build();
            var spell = Spell.fromCard(card, player).build();
            stack.push(spell);

            stack.resolve();

            verify(eventProcessor).process(any(PutIntoGraveyardEvent.class));
            assertThat(stack.isEmpty()).isTrue();
        }

        @Test
        void resolveSorcerySpellGoesToGraveyard() {
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Divination")
                    .type(Type.SORCERY)
                    .build();
            var spell = Spell.fromCard(card, player).build();
            stack.push(spell);

            stack.resolve();

            verify(eventProcessor).process(any(PutIntoGraveyardEvent.class));
            assertThat(stack.isEmpty()).isTrue();
        }

        @Test
        void resolveOnlyResolvesTopItem() {
            var card1 = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Spell 1")
                    .type(Type.INSTANT)
                    .build();
            var card2 = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Spell 2")
                    .type(Type.INSTANT)
                    .build();
            var spell1 = Spell.fromCard(card1, player).build();
            var spell2 = Spell.fromCard(card2, player).build();
            stack.push(spell1);
            stack.push(spell2);

            stack.resolve();

            assertThat(stack.size()).isEqualTo(1);
            assertThat(stack.peek()).contains(spell1);
        }

        @Test
        void resolveAbilityOnStackJustRemovesIt() {
            var ability = createAbility();
            stack.push(ability);

            stack.resolve();

            assertThat(stack.isEmpty()).isTrue();
            verify(eventProcessor, never()).process(any());
            verify(battlefield, never()).enter(any(Card.class), any(), any());
        }
    }

    @Nested
    class SpellEffectResolution {

        @Test
        void executesSpellAbilityEffectsBeforeGraveyard() {
            var drawEffect = new DrawEffect(Optional.empty(), new Amount.Exact(1));
            var spellAbility = new SpellAbility(new AbilityId(), "Draw a card.", List.of(drawEffect));
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Divination")
                    .type(Type.SORCERY)
                    .addAbility(spellAbility)
                    .build();
            var spell = Spell.fromCard(card, player).build();
            stack.push(spell);

            stack.resolve();

            var order = inOrder(effectExecutor, eventProcessor);
            order.verify(effectExecutor).execute(any(Effect.class), any(ResolutionContext.class));
            order.verify(eventProcessor).process(any(PutIntoGraveyardEvent.class));
        }

        @Test
        void spellWithoutSpellAbilityJustGoesToGraveyard() {
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Blank Sorcery")
                    .type(Type.SORCERY)
                    .build();
            var spell = Spell.fromCard(card, player).build();
            stack.push(spell);

            stack.resolve();

            verify(eventProcessor).process(any(PutIntoGraveyardEvent.class));
            verify(effectExecutor, never()).execute(any(), any());
        }

        @Test
        void fizzlesWhenAllTargetsIllegal() {
            var selector = mock(ObjectSelector.class);
            when(selector.matches(any(Selectable.class), any(Player.class))).thenReturn(false);

            var subject = new Subject.Select(selector);
            var target = mock(Permanent.class);
            var targets = new TargetChoices(List.of(new TargetChoice(subject, target)));
            var context = new SpellContext(targets);

            var destroyEffect = new DestroyEffect(subject, true);
            var spellAbility = new SpellAbility(new AbilityId(), "Destroy target creature.", List.of(destroyEffect));
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Murder")
                    .type(Type.INSTANT)
                    .addAbility(spellAbility)
                    .build();
            var spell = Spell.fromCard(card, player).context(context).build();
            stack.push(spell);

            stack.resolve();

            verify(eventProcessor).process(any(PutIntoGraveyardEvent.class));
            verify(effectExecutor, never()).execute(any(), any());
        }
    }
}
