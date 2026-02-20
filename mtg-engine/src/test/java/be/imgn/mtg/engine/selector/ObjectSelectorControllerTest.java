package be.imgn.mtg.engine.selector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.Permanent;

@DisplayName("ObjectSelector controller matching")
class ObjectSelectorControllerTest {

    private Player you;
    private Player opponent;

    @BeforeEach
    void setUp() {
        you = mock(Player.class);
        opponent = mock(Player.class);
    }

    private Permanent creatureControlledBy(Player controller) {
        return Permanent.fromCard(
                        Card.builder()
                                .owner(controller)
                                .controller(controller)
                                .name("Bear")
                                .type(Type.CREATURE)
                                .build(),
                        controller)
                .build();
    }

    @Nested
    @DisplayName("'you control'")
    class YouControl {

        @Test
        @DisplayName("matches creature you control")
        void matchesYourCreature() {
            var selector = new ObjectSelector(
                    new Quantifier.One(),
                    List.of(),
                    new TypeMatcher.Single(Type.CREATURE),
                    List.of(),
                    new ControllerClause(PlayerReference.YOU));

            assertThat(selector.matches(creatureControlledBy(you), you)).isTrue();
        }

        @Test
        @DisplayName("does not match creature opponent controls")
        void doesNotMatchOpponentCreature() {
            var selector = new ObjectSelector(
                    new Quantifier.One(),
                    List.of(),
                    new TypeMatcher.Single(Type.CREATURE),
                    List.of(),
                    new ControllerClause(PlayerReference.YOU));

            assertThat(selector.matches(creatureControlledBy(opponent), you)).isFalse();
        }
    }

    @Nested
    @DisplayName("'an opponent controls'")
    class OpponentControls {

        @Test
        @DisplayName("matches creature opponent controls")
        void matchesOpponentCreature() {
            var selector = new ObjectSelector(
                    new Quantifier.One(),
                    List.of(),
                    new TypeMatcher.Single(Type.CREATURE),
                    List.of(),
                    new ControllerClause(PlayerReference.OPPONENT));

            assertThat(selector.matches(creatureControlledBy(opponent), you)).isTrue();
        }

        @Test
        @DisplayName("does not match creature you control")
        void doesNotMatchYourCreature() {
            var selector = new ObjectSelector(
                    new Quantifier.One(),
                    List.of(),
                    new TypeMatcher.Single(Type.CREATURE),
                    List.of(),
                    new ControllerClause(PlayerReference.OPPONENT));

            assertThat(selector.matches(creatureControlledBy(you), you)).isFalse();
        }
    }

    @Nested
    @DisplayName("no controller clause")
    class NoControllerClause {

        @Test
        @DisplayName("matches any creature regardless of controller")
        void matchesAnyCreature() {
            var selector = new ObjectSelector(
                    new Quantifier.One(), List.of(), new TypeMatcher.Single(Type.CREATURE), List.of(), null);

            assertThat(selector.matches(creatureControlledBy(opponent), you)).isTrue();
        }
    }
}
