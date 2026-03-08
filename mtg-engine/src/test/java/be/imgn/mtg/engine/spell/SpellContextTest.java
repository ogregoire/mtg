package be.imgn.mtg.engine.spell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.Spell;
import be.imgn.mtg.engine.selector.PlayerCriterion;
import be.imgn.mtg.engine.selector.PlayerSelector;
import be.imgn.mtg.engine.selector.Quantifier;

class SpellContextTest {

    @Test
    void emptyContextHasNoTargets() {
        var context = SpellContext.empty();

        assertThat(context.targets().choices()).isEmpty();
    }

    @Test
    void contextStoresTargetChoices() {
        var selector = new PlayerSelector(new Quantifier.One(), PlayerCriterion.ANY);
        var subject = new Subject.Select(selector);
        var target = mock(Player.class);
        var choices = new TargetChoices(List.of(new TargetChoice(subject, target)));
        var context = new SpellContext(choices);

        assertThat(context.targets().choices()).hasSize(1);
        assertThat(context.targets().choices().getFirst().subject()).isEqualTo(subject);
        assertThat(context.targets().choices().getFirst().target()).isSameAs(target);
    }

    @Nested
    class FindTarget {

        @Test
        void findTargetReturnsMatchingChoice() {
            var selector = new PlayerSelector(new Quantifier.One(), PlayerCriterion.ANY);
            var subject = new Subject.Select(selector);
            var target = mock(Player.class);
            var choices = new TargetChoices(List.of(new TargetChoice(subject, target)));

            var result = choices.findTarget(subject);

            assertThat(result).contains(target);
        }

        @Test
        void findTargetReturnsEmptyWhenNoMatch() {
            var subject1 = new Subject.Select(new PlayerSelector(new Quantifier.One(), PlayerCriterion.ANY));
            var subject2 = new Subject.Select(new PlayerSelector(new Quantifier.One(), PlayerCriterion.OPPONENT));
            var target = mock(Player.class);
            var choices = new TargetChoices(List.of(new TargetChoice(subject1, target)));

            var result = choices.findTarget(subject2);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class SpellIntegration {

        @Test
        void spellFromCardHasEmptyContext() {
            var player = mock(Player.class);
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Test Card")
                    .type(Type.INSTANT)
                    .build();

            var spell = Spell.fromCard(card, player).build();

            assertThat(spell.context()).isNotNull();
            assertThat(spell.context().targets().choices()).isEmpty();
        }

        @Test
        void spellBuilderAcceptsContext() {
            var player = mock(Player.class);
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Lightning Bolt")
                    .color(Color.RED)
                    .type(Type.INSTANT)
                    .build();

            var selector = new PlayerSelector(new Quantifier.One(), PlayerCriterion.ANY);
            var subject = new Subject.Select(selector);
            var target = mock(Player.class);
            var ctx = new SpellContext(new TargetChoices(List.of(new TargetChoice(subject, target))));

            var spell = Spell.fromCard(card, player).context(ctx).build();

            assertThat(spell.context()).isSameAs(ctx);
            assertThat(spell.context().targets().findTarget(subject)).contains(target);
        }
    }
}
