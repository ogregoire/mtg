package be.imgn.mtg.engine.object;

import static be.imgn.mtg.engine.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.CounterType;
import be.imgn.mtg.engine.characteristics.CreatureType;
import be.imgn.mtg.engine.characteristics.PlaneswalkerType;
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

        permanent.counters().add(CounterType.PLUS_ONE_PLUS_ONE, 2);

        assertThat(permanent).hasCounters(CounterType.PLUS_ONE_PLUS_ONE, 2);

        permanent.counters().remove(CounterType.PLUS_ONE_PLUS_ONE, 1);

        assertThat(permanent).hasCounters(CounterType.PLUS_ONE_PLUS_ONE, 1);
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
                .hasCounters(CounterType.LOYALTY, 3);

        permanent.counters().add(CounterType.LOYALTY, 2);

        assertThat(permanent).hasLoyalty(5);

        permanent.counters().remove(CounterType.LOYALTY, 1);

        assertThat(permanent).hasLoyalty(4);
    }
}
