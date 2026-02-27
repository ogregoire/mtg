package be.imgn.mtg.engine.state.internal;

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
import be.imgn.mtg.engine.selector.CompositeSelector;
import be.imgn.mtg.engine.selector.ObjectSelector;
import be.imgn.mtg.engine.selector.PlayerCriterion;
import be.imgn.mtg.engine.selector.PlayerSelector;
import be.imgn.mtg.engine.selector.Quantifier;
import be.imgn.mtg.engine.selector.TypeMatcher;
import be.imgn.mtg.engine.state.LastKnownInformation;
import be.imgn.mtg.engine.zone.Battlefield;
import be.imgn.mtg.engine.zone.CommandZone;
import be.imgn.mtg.engine.zone.Exile;
import be.imgn.mtg.engine.zone.Stack;

@DisplayName("GameState.select")
class GameStateSelectTest {

    private ObjectStore store;
    private Player you;
    private Player opponent;
    private DefaultGameState state;
    private Battlefield battlefield;

    @BeforeEach
    void setUp() {
        store = new ObjectStore();
        you = mock(Player.class);
        opponent = mock(Player.class);
        battlefield = mock(Battlefield.class);
        state = new DefaultGameState(
                store,
                battlefield,
                mock(Stack.class),
                mock(Exile.class),
                mock(CommandZone.class),
                mock(LastKnownInformation.class),
                List.of(you, opponent));
    }

    @Test
    @DisplayName("selects matching objects from store")
    void selectsMatchingObjects() {
        var creature = Permanent.fromCard(
                        Card.builder()
                                .owner(you)
                                .controller(you)
                                .name("Bear")
                                .type(Type.CREATURE)
                                .build(),
                        you)
                .build();
        var artifact = Permanent.fromCard(
                        Card.builder()
                                .owner(you)
                                .controller(you)
                                .name("Sol Ring")
                                .type(Type.ARTIFACT)
                                .build(),
                        you)
                .build();
        store.add(creature, battlefield);
        store.add(artifact, battlefield);

        var selector = new ObjectSelector(
                new Quantifier.All(), List.of(), new TypeMatcher.Single(Type.CREATURE), List.of(), null);

        var results = state.select(selector, you).toList();

        assertThat(results).containsExactly(creature);
    }

    @Test
    @DisplayName("selects matching players")
    void selectsMatchingPlayers() {
        var selector = new PlayerSelector(new Quantifier.One(), PlayerCriterion.OPPONENT);

        var results = state.select(selector, you).toList();

        assertThat(results).containsExactly(opponent);
    }

    @Test
    @DisplayName("composite selector matches both objects and players")
    void compositeSelectorMatchesBoth() {
        var creature = Permanent.fromCard(
                        Card.builder()
                                .owner(you)
                                .controller(you)
                                .name("Bear")
                                .type(Type.CREATURE)
                                .build(),
                        you)
                .build();
        store.add(creature, battlefield);

        var selector = new CompositeSelector(
                new Quantifier.One(),
                new ObjectSelector(
                        new Quantifier.One(),
                        List.of(),
                        new TypeMatcher.Or(List.of(
                                new TypeMatcher.Single(Type.CREATURE),
                                new TypeMatcher.Single(Type.PLANESWALKER),
                                new TypeMatcher.Single(Type.BATTLE))),
                        List.of(),
                        null),
                new PlayerSelector(new Quantifier.One(), PlayerCriterion.ANY));

        var results = state.select(selector, you).toList();

        assertThat(results)
                .hasSize(3)
                .contains(creature)
                .satisfiesOnlyOnce(s -> assertThat(s).isInstanceOf(Player.class).isEqualTo(you))
                .satisfiesOnlyOnce(s -> assertThat(s).isInstanceOf(Player.class).isEqualTo(opponent));
    }
}
