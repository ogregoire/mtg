package be.imgn.mtg.engine.selector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.Permanent;

@DisplayName("CompositeSelector.matches")
class CompositeSelectorTest {

    private Player you;
    private Player opponent;

    @BeforeEach
    void setUp() {
        you = mock(Player.class);
        opponent = mock(Player.class);
    }

    private CompositeSelector anyTarget() {
        return new CompositeSelector(
                new Quantifier.One(),
                new ObjectSelector(new Quantifier.One(), List.of(), new TypeMatcher.Target(), List.of(), null),
                new PlayerSelector(new Quantifier.One(), PlayerCriterion.ANY));
    }

    @Test
    @DisplayName("matches a creature permanent")
    void matchesCreature() {
        var selector = anyTarget();
        var permanent = Permanent.fromCard(
                        Card.builder()
                                .owner(you)
                                .controller(you)
                                .name("Bear")
                                .type(Type.CREATURE)
                                .build(),
                        you)
                .build();

        assertThat(selector.matches(permanent, you)).isTrue();
    }

    @Test
    @DisplayName("matches a player")
    void matchesPlayer() {
        var selector = anyTarget();
        assertThat(selector.matches(opponent, you)).isTrue();
    }

    @Test
    @DisplayName("does not match an artifact permanent")
    void doesNotMatchArtifact() {
        var selector = anyTarget();
        var permanent = Permanent.fromCard(
                        Card.builder()
                                .owner(you)
                                .controller(you)
                                .name("Sol Ring")
                                .type(Type.ARTIFACT)
                                .build(),
                        you)
                .build();

        assertThat(selector.matches(permanent, you)).isFalse();
    }
}
