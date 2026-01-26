package be.imgn.mtg.engine.ability.internal.parser.selector;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.Permanent;

@DisplayName("ObjectSelector unsupported operations")
class ObjectSelectorUnsupportedTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
    }

    private ObjectSelector selector(List<Qualifier> qualifiers) {
        return new ObjectSelector(
                new Quantifier.One(), qualifiers, new TypeMatcher.Permanent(), List.of(), Optional.empty());
    }

    private ObjectSelector selector(WithClause... clauses) {
        return new ObjectSelector(
                new Quantifier.One(), List.of(), new TypeMatcher.Permanent(), List.of(clauses), Optional.empty());
    }

    private Permanent permanent() {
        return Permanent.fromCard(
                        Card.builder()
                                .owner(player)
                                .controller(player)
                                .name("Test")
                                .type(Type.CREATURE)
                                .build(),
                        player)
                .build();
    }

    @Nested
    @DisplayName("Status qualifiers")
    class StatusQualifierTests {

        @Test
        @DisplayName("attacking status throws UnsupportedOperationException")
        void attackingThrows() {
            var selector = selector(List.of(new Qualifier.Status(StatusType.ATTACKING)));

            assertThatThrownBy(() -> selector.matches(permanent()))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Status ATTACKING not yet supported");
        }

        @Test
        @DisplayName("blocking status throws UnsupportedOperationException")
        void blockingThrows() {
            var selector = selector(List.of(new Qualifier.Status(StatusType.BLOCKING)));

            assertThatThrownBy(() -> selector.matches(permanent()))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Status BLOCKING not yet supported");
        }

        @Test
        @DisplayName("equipped status throws UnsupportedOperationException")
        void equippedThrows() {
            var selector = selector(List.of(new Qualifier.Status(StatusType.EQUIPPED)));

            assertThatThrownBy(() -> selector.matches(permanent()))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Status EQUIPPED not yet supported");
        }

        @Test
        @DisplayName("enchanted status throws UnsupportedOperationException")
        void enchantedThrows() {
            var selector = selector(List.of(new Qualifier.Status(StatusType.ENCHANTED)));

            assertThatThrownBy(() -> selector.matches(permanent()))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Status ENCHANTED not yet supported");
        }
    }

    @Nested
    @DisplayName("With clauses")
    class WithClauseTests {

        @Test
        @DisplayName("ability with-clause throws UnsupportedOperationException")
        void abilityThrows() {
            var selector = selector(new WithClause.Ability("flying"));

            assertThatThrownBy(() -> selector.matches(permanent()))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Ability with-clause not yet supported");
        }

        @Test
        @DisplayName("counter with-clause throws UnsupportedOperationException")
        void counterThrows() {
            var selector = selector(new WithClause.Counter("+1/+1"));

            assertThatThrownBy(() -> selector.matches(permanent()))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Counter with-clause not yet supported");
        }
    }
}
