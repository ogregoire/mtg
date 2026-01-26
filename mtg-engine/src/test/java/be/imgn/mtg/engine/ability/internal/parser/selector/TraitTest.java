package be.imgn.mtg.engine.ability.internal.parser.selector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.Supertype;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.object.Token;

@DisplayName("Trait")
class TraitTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
    }

    @Nested
    @DisplayName("CardType")
    class CardTypeTests {

        @Test
        @DisplayName("matches when object has the type")
        void matchesWithType() {
            var trait = new Trait.CardType(Type.CREATURE);
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Bear")
                                    .type(Type.CREATURE)
                                    .build(),
                            player)
                    .build();

            assertThat(trait.test(permanent)).isTrue();
        }

        @Test
        @DisplayName("does not match when object lacks the type")
        void doesNotMatchWithoutType() {
            var trait = new Trait.CardType(Type.CREATURE);
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Sol Ring")
                                    .type(Type.ARTIFACT)
                                    .build(),
                            player)
                    .build();

            assertThat(trait.test(permanent)).isFalse();
        }
    }

    @Nested
    @DisplayName("ObjectColor")
    class ObjectColorTests {

        @Test
        @DisplayName("matches when object has the color")
        void matchesWithColor() {
            var trait = new Trait.ObjectColor(Color.RED);
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Lightning Bolt")
                                    .type(Type.INSTANT)
                                    .color(Color.RED)
                                    .build(),
                            player)
                    .build();

            assertThat(trait.test(permanent)).isTrue();
        }

        @Test
        @DisplayName("does not match when object lacks the color")
        void doesNotMatchWithoutColor() {
            var trait = new Trait.ObjectColor(Color.RED);
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Bear")
                                    .type(Type.CREATURE)
                                    .color(Color.GREEN)
                                    .build(),
                            player)
                    .build();

            assertThat(trait.test(permanent)).isFalse();
        }
    }

    @Nested
    @DisplayName("ObjectSupertype")
    class ObjectSupertypeTests {

        @Test
        @DisplayName("matches when object has the supertype")
        void matchesWithSupertype() {
            var trait = new Trait.ObjectSupertype(Supertype.LEGENDARY);
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Ragavan")
                                    .type(Type.CREATURE)
                                    .supertype(Supertype.LEGENDARY)
                                    .build(),
                            player)
                    .build();

            assertThat(trait.test(permanent)).isTrue();
        }

        @Test
        @DisplayName("does not match when object lacks the supertype")
        void doesNotMatchWithoutSupertype() {
            var trait = new Trait.ObjectSupertype(Supertype.LEGENDARY);
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Bear")
                                    .type(Type.CREATURE)
                                    .build(),
                            player)
                    .build();

            assertThat(trait.test(permanent)).isFalse();
        }

        @Test
        @DisplayName("matches basic lands")
        void matchesBasic() {
            var trait = new Trait.ObjectSupertype(Supertype.BASIC);
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Forest")
                                    .type(Type.LAND)
                                    .supertype(Supertype.BASIC)
                                    .build(),
                            player)
                    .build();

            assertThat(trait.test(permanent)).isTrue();
        }
    }

    @Nested
    @DisplayName("TokenSource")
    class TokenSourceTests {

        @Test
        @DisplayName("matches when object is a token permanent")
        void matchesTokenPermanent() {
            var trait = new Trait.TokenSource();
            var token = Token.builder()
                    .owner(player)
                    .controller(player)
                    .name("Soldier")
                    .type(Type.CREATURE)
                    .build();
            var permanent = Permanent.fromToken(token, player).build();

            assertThat(trait.test(permanent)).isTrue();
        }

        @Test
        @DisplayName("does not match when object is a card-based permanent")
        void doesNotMatchCardPermanent() {
            var trait = new Trait.TokenSource();
            var permanent = Permanent.fromCard(
                            Card.builder()
                                    .owner(player)
                                    .controller(player)
                                    .name("Bear")
                                    .type(Type.CREATURE)
                                    .build(),
                            player)
                    .build();

            assertThat(trait.test(permanent)).isFalse();
        }

        @Test
        @DisplayName("does not match when object is a card")
        void doesNotMatchCard() {
            var trait = new Trait.TokenSource();
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Bear")
                    .type(Type.CREATURE)
                    .build();

            assertThat(trait.test(card)).isFalse();
        }
    }
}
