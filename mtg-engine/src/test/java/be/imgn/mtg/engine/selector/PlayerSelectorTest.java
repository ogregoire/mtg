package be.imgn.mtg.engine.selector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;

@DisplayName("PlayerSelector.matches")
class PlayerSelectorTest {

    private Player you;
    private Player opponent;

    @BeforeEach
    void setUp() {
        you = mock(Player.class);
        opponent = mock(Player.class);
    }

    @Nested
    @DisplayName("ANY criterion")
    class AnyCriterion {

        @Test
        @DisplayName("matches the perspective player")
        void matchesYou() {
            var selector = new PlayerSelector(new Quantifier.One(), PlayerCriterion.ANY);
            assertThat(selector.matches(you, you)).isTrue();
        }

        @Test
        @DisplayName("matches an opponent")
        void matchesOpponent() {
            var selector = new PlayerSelector(new Quantifier.One(), PlayerCriterion.ANY);
            assertThat(selector.matches(opponent, you)).isTrue();
        }

        @Test
        @DisplayName("does not match a game object")
        void doesNotMatchGameObject() {
            var selector = new PlayerSelector(new Quantifier.One(), PlayerCriterion.ANY);
            var card = Card.builder()
                    .owner(you)
                    .controller(you)
                    .name("Test")
                    .type(Type.CREATURE)
                    .build();
            assertThat(selector.matches(card, you)).isFalse();
        }
    }

    @Nested
    @DisplayName("YOU criterion")
    class YouCriterion {

        @Test
        @DisplayName("matches the perspective player")
        void matchesYou() {
            var selector = new PlayerSelector(new Quantifier.One(), PlayerCriterion.YOU);
            assertThat(selector.matches(you, you)).isTrue();
        }

        @Test
        @DisplayName("does not match an opponent")
        void doesNotMatchOpponent() {
            var selector = new PlayerSelector(new Quantifier.One(), PlayerCriterion.YOU);
            assertThat(selector.matches(opponent, you)).isFalse();
        }
    }

    @Nested
    @DisplayName("OPPONENT criterion")
    class OpponentCriterion {

        @Test
        @DisplayName("matches an opponent")
        void matchesOpponent() {
            var selector = new PlayerSelector(new Quantifier.One(), PlayerCriterion.OPPONENT);
            assertThat(selector.matches(opponent, you)).isTrue();
        }

        @Test
        @DisplayName("does not match the perspective player")
        void doesNotMatchYou() {
            var selector = new PlayerSelector(new Quantifier.One(), PlayerCriterion.OPPONENT);
            assertThat(selector.matches(you, you)).isFalse();
        }
    }
}
