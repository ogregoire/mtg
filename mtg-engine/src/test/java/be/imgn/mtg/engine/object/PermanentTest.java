package be.imgn.mtg.engine.object;

import static be.imgn.mtg.engine.assertions.MTGAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.CreatureType;
import be.imgn.mtg.engine.characteristics.PlaneswalkerType;
import be.imgn.mtg.engine.characteristics.StandardCounterType;
import be.imgn.mtg.engine.characteristics.Supertype;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.game.Player;

class PermanentTest {

    @Test
    void permanentFromCard() {
        var player = mock(Player.class);

        var card = Card.builder()
                .owner(player)
                .controller(player)
                .name("Grizzly Bears")
                .color(Color.GREEN)
                .type(Type.CREATURE)
                .subtype(CreatureType.BEAR)
                .power(Value.of(2))
                .toughness(Value.of(2))
                .build();

        var permanent = Permanent.fromCard(card, player).build();

        assertThat(permanent)
                .hasName("Grizzly Bears")
                .hasController(player)
                .isGreen()
                .isCreature()
                .hasPower(2)
                .hasToughness(2)
                .isUntapped()
                .isUnflipped()
                .isFaceUp()
                .isPhasedIn()
                .hasNoCounters();
    }

    @Test
    void permanentFromToken() {
        var player = mock(Player.class);

        var token = Token.builder()
                .owner(player)
                .controller(player)
                .name("Soldier Token")
                .color(Color.WHITE)
                .type(Type.CREATURE)
                .subtype(CreatureType.SOLDIER)
                .power(Value.of(1))
                .toughness(Value.of(1))
                .build();

        var permanent = Permanent.fromToken(token, player).build();

        assertThat(permanent)
                .hasName("Soldier Token")
                .isWhite()
                .isCreature()
                .hasPower(1)
                .hasToughness(1)
                .isUntapped();
    }

    @Test
    void tapAndUntap() {
        var player = mock(Player.class);

        var card = Card.builder()
                .owner(player)
                .controller(player)
                .name("Forest")
                .type(Type.LAND)
                .build();

        var permanent = Permanent.fromCard(card, player).build();

        assertThat(permanent).isUntapped();

        permanent.tap();

        assertThat(permanent).isTapped();

        permanent.untap();

        assertThat(permanent).isUntapped();
    }

    @Test
    void counters() {
        var player = mock(Player.class);

        var card = Card.builder()
                .owner(player)
                .controller(player)
                .name("Creature")
                .color(Color.GREEN)
                .type(Type.CREATURE)
                .power(Value.of(1))
                .toughness(Value.of(1))
                .build();

        var permanent = Permanent.fromCard(card, player).build();

        assertThat(permanent).hasNoCounters();

        permanent.counters().add(StandardCounterType.PLUS_ONE_PLUS_ONE, 2);

        assertThat(permanent).hasCounters(StandardCounterType.PLUS_ONE_PLUS_ONE, 2);

        permanent.counters().remove(StandardCounterType.PLUS_ONE_PLUS_ONE, 1);

        assertThat(permanent).hasCounters(StandardCounterType.PLUS_ONE_PLUS_ONE, 1);
    }

    @Test
    void phaseInAndOut() {
        var player = mock(Player.class);

        var card = Card.builder()
                .owner(player)
                .controller(player)
                .name("Creature")
                .type(Type.CREATURE)
                .power(Value.of(2))
                .toughness(Value.of(2))
                .build();

        var permanent = Permanent.fromCard(card, player).build();

        assertThat(permanent).isPhasedIn();

        permanent.phaseOut();

        assertThat(permanent).isPhasedOut();

        permanent.phaseIn();

        assertThat(permanent).isPhasedIn();
    }

    @Test
    void planeswalkerWithLoyalty() {
        var player = mock(Player.class);

        var card = Card.builder()
                .owner(player)
                .controller(player)
                .name("Jace, the Mind Sculptor")
                .color(Color.BLUE)
                .type(Type.PLANESWALKER)
                .supertype(Supertype.LEGENDARY)
                .subtype(PlaneswalkerType.JACE)
                .loyalty(Value.of(3))
                .build();

        var permanent = Permanent.fromCard(card, player).build();

        assertThat(permanent)
                .hasName("Jace, the Mind Sculptor")
                .isBlue()
                .isPlaneswalker()
                .isLegendary()
                .hasLoyalty(3)
                .hasCounters(StandardCounterType.LOYALTY, 3);

        permanent.counters().add(StandardCounterType.LOYALTY, 2);

        assertThat(permanent).hasLoyalty(5);

        permanent.counters().remove(StandardCounterType.LOYALTY, 1);

        assertThat(permanent).hasLoyalty(4);
    }

    @Nested
    class FlipStatus {

        @Test
        void permanentStartsUnflipped() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Card")
                    .type(Type.CREATURE)
                    .build();

            var permanent = Permanent.fromCard(card, player).build();

            assertThat(permanent).isUnflipped();
            assertThat(permanent.isFlipped()).isFalse();
        }

        @Test
        void canFlipAndUnflip() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Card")
                    .type(Type.CREATURE)
                    .build();

            var permanent = Permanent.fromCard(card, player).build();

            assertThat(permanent.canFlip()).isTrue();

            permanent.flip();

            assertThat(permanent.isFlipped()).isTrue();
            assertThat(permanent.isUnflipped()).isFalse();
            assertThat(permanent.canUnflip()).isTrue();

            permanent.unflip();

            assertThat(permanent).isUnflipped();
        }
    }

    @Nested
    class FaceStatus {

        @Test
        void permanentStartsFaceUp() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Card")
                    .type(Type.CREATURE)
                    .build();

            var permanent = Permanent.fromCard(card, player).build();

            assertThat(permanent).isFaceUp();
            assertThat(permanent.isFaceDown()).isFalse();
        }

        @Test
        void canTurnFaceDownAndUp() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Morph Creature")
                    .type(Type.CREATURE)
                    .build();

            var permanent = Permanent.fromCard(card, player).build();

            assertThat(permanent.canTurnFaceDown()).isTrue();

            permanent.turnFaceDown();

            assertThat(permanent.isFaceDown()).isTrue();
            assertThat(permanent.isFaceUp()).isFalse();
            assertThat(permanent.canTurnFaceUp()).isTrue();

            permanent.turnFaceUp();

            assertThat(permanent).isFaceUp();
        }
    }

    @Nested
    class CanActions {

        @Test
        void untappedPermanentCanTap() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Card")
                    .type(Type.ARTIFACT)
                    .build();

            var permanent = Permanent.fromCard(card, player).build();

            assertThat(permanent.canTap()).isTrue();
            assertThat(permanent.canUntap()).isFalse();
        }

        @Test
        void tappedPermanentCanUntap() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Card")
                    .type(Type.ARTIFACT)
                    .build();

            var permanent = Permanent.fromCard(card, player).build();
            permanent.tap();

            assertThat(permanent.canUntap()).isTrue();
            assertThat(permanent.canTap()).isFalse();
        }

        @Test
        void phasedInPermanentCanPhaseOut() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Card")
                    .type(Type.CREATURE)
                    .build();

            var permanent = Permanent.fromCard(card, player).build();

            assertThat(permanent.canPhaseOut()).isTrue();
            assertThat(permanent.canPhaseIn()).isFalse();
        }

        @Test
        void phasedOutPermanentCanPhaseIn() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Card")
                    .type(Type.CREATURE)
                    .build();

            var permanent = Permanent.fromCard(card, player).build();
            permanent.phaseOut();

            assertThat(permanent.canPhaseIn()).isTrue();
            assertThat(permanent.canPhaseOut()).isFalse();
        }
    }

    @Nested
    class PermanentSource {

        @Test
        void permanentFromCardRetainsCardAsSource() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Card")
                    .type(Type.CREATURE)
                    .build();

            var permanent = Permanent.fromCard(card, player).build();

            assertThat(permanent.source()).isSameAs(card);
            assertThat(permanent.source()).isInstanceOf(Card.class);
        }

        @Test
        void permanentFromTokenRetainsTokenAsSource() {
            var player = mock(Player.class);

            var token = Token.builder()
                    .owner(player)
                    .controller(player)
                    .name("Token")
                    .type(Type.CREATURE)
                    .build();

            var permanent = Permanent.fromToken(token, player).build();

            assertThat(permanent.source()).isSameAs(token);
            assertThat(permanent.source()).isInstanceOf(Token.class);
        }
    }

    @Nested
    class PermanentIdentity {

        @Test
        void eachPermanentGetsUniqueId() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Card")
                    .type(Type.CREATURE)
                    .build();

            var permanent1 = Permanent.fromCard(card, player).build();
            var permanent2 = Permanent.fromCard(card, player).build();

            assertThat(permanent1.id()).isNotEqualTo(permanent2.id());
        }

        @Test
        void permanentIdDiffersFromSourceId() {
            var player = mock(Player.class);

            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Card")
                    .type(Type.CREATURE)
                    .build();

            var permanent = Permanent.fromCard(card, player).build();

            assertThat(permanent.id()).isNotEqualTo(card.id());
        }
    }

    @Nested
    class PermanentController {

        @Test
        void permanentControllerCanDifferFromOwner() {
            var owner = mock(Player.class);
            var controller = mock(Player.class);

            var card = Card.builder()
                    .owner(owner)
                    .controller(owner)
                    .name("Stolen Permanent")
                    .type(Type.CREATURE)
                    .build();

            var permanent = Permanent.fromCard(card, controller).build();

            assertThat(permanent.owner()).isSameAs(owner);
            assertThat(permanent.controller()).isSameAs(controller);
        }
    }
}
