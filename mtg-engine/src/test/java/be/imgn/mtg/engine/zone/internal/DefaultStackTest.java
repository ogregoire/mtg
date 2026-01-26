package be.imgn.mtg.engine.zone.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.AbilityOnStack;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.ObjectId;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.object.Spell;

class DefaultStackTest {

    Player player;
    DefaultStack stack;

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
        stack = new DefaultStack();
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

            assertThat(stack.contains(spell.id())).isTrue();
        }

        @Test
        void containsReturnsFalseForAbsentObject() {
            assertThat(stack.contains(new ObjectId())).isFalse();
        }

        @Test
        void pushAbility() {
            var ability = createAbility();

            stack.push(ability);

            assertThat(stack.size()).isEqualTo(1);
            assertThat(stack.contains(ability.id())).isTrue();
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

            assertThat(stack.contains(spell.id())).isFalse();
        }
    }

    @Nested
    class RemoveOperations {

        @Test
        void removeNonExistent() {
            var removed = stack.remove(new ObjectId());

            assertThat(removed).isEmpty();
        }

        @Test
        void removeFromMiddle() {
            var spell1 = createSpell("Spell 1");
            var spell2 = createSpell("Spell 2");
            var spell3 = createSpell("Spell 3");
            stack.push(spell1);
            stack.push(spell2);
            stack.push(spell3);

            var removed = stack.remove(spell2.id());

            assertThat(removed).contains(spell2);
            assertThat(stack.size()).isEqualTo(2);
            assertThat(stack.contains(spell2.id())).isFalse();
        }

        @Test
        void removeFromTop() {
            var spell1 = createSpell("Spell 1");
            var spell2 = createSpell("Spell 2");
            stack.push(spell1);
            stack.push(spell2);

            var removed = stack.remove(spell2.id());

            assertThat(removed).contains(spell2);
            assertThat(stack.peek()).contains(spell1);
        }
    }

    @Nested
    class FindByIdOperations {

        @Test
        void findByIdReturnsEmptyForAbsentObject() {
            assertThat(stack.findById(new ObjectId())).isEmpty();
        }

        @Test
        void findByIdReturnsPresentObject() {
            var spell = createSpell("Lightning Bolt");
            stack.push(spell);

            assertThat(stack.findById(spell.id())).contains(spell);
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
            assertThat(stack.contains(spell.id())).isTrue();
            assertThat(stack.contains(ability.id())).isTrue();
        }
    }
}
