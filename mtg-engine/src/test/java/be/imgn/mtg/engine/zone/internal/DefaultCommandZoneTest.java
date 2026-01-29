package be.imgn.mtg.engine.zone.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.Supertype;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;

class DefaultCommandZoneTest {

    Player player1;
    Player player2;
    DefaultCommandZone commandZone;

    @BeforeEach
    void setUp() {
        player1 = mock(Player.class);
        player2 = mock(Player.class);
        commandZone = new DefaultCommandZone();
    }

    private Card createCommander(Player owner, String name) {
        return Card.builder()
                .owner(owner)
                .controller(owner)
                .name(name)
                .supertype(Supertype.LEGENDARY)
                .type(Type.CREATURE)
                .build();
    }

    @Nested
    class BasicOperations {

        @Test
        void emptyCommandZone() {
            assertThat(commandZone.isEmpty()).isTrue();
            assertThat(commandZone.size()).isZero();
        }

        @Test
        void addCommanderIncreasesSize() {
            var commander = createCommander(player1, "Kenrith");

            commandZone.addCommander(commander, player1);

            assertThat(commandZone.size()).isEqualTo(1);
            assertThat(commandZone.isEmpty()).isFalse();
        }

        @Test
        void containsReturnsTrueForAddedCommander() {
            var commander = createCommander(player1, "Kenrith");
            commandZone.addCommander(commander, player1);

            assertThat(commandZone.contains(commander)).isTrue();
        }

        @Test
        void containsReturnsFalseForAbsentCommander() {
            var commander = createCommander(player1, "Not Added");
            assertThat(commandZone.contains(commander)).isFalse();
        }
    }

    @Nested
    class CommandersOperations {

        @Test
        void commandersReturnsEmptyForPlayerWithNoCommanders() {
            assertThat(commandZone.commanders(player1)).isEmpty();
        }

        @Test
        void commandersReturnsCommandersForPlayer() {
            var commander1 = createCommander(player1, "P1 Commander");
            var commander2 = createCommander(player2, "P2 Commander");
            commandZone.addCommander(commander1, player1);
            commandZone.addCommander(commander2, player2);

            assertThat(commandZone.commanders(player1)).containsExactly(commander1);
        }

        @Test
        void commandersReturnsMultipleCommanders() {
            var commander1 = createCommander(player1, "Partner 1");
            var commander2 = createCommander(player1, "Partner 2");
            commandZone.addCommander(commander1, player1);
            commandZone.addCommander(commander2, player1);

            assertThat(commandZone.commanders(player1)).containsExactly(commander1, commander2);
        }
    }

    @Nested
    class RemoveCommanderOperations {

        @Test
        void removeCommanderNonExistent() {
            var commander = createCommander(player1, "Not Added");
            var removed = commandZone.removeCommander(commander);

            assertThat(removed).isFalse();
        }

        @Test
        void removeCommanderExisting() {
            var commander1 = createCommander(player1, "Commander 1");
            var commander2 = createCommander(player1, "Commander 2");
            commandZone.addCommander(commander1, player1);
            commandZone.addCommander(commander2, player1);

            var removed = commandZone.removeCommander(commander1);

            assertThat(removed).isTrue();
            assertThat(commandZone.size()).isEqualTo(1);
            assertThat(commandZone.commanders(player1)).containsExactly(commander2);
        }

        @Test
        void removeCommanderUpdatesContains() {
            var commander = createCommander(player1, "Kenrith");
            commandZone.addCommander(commander, player1);

            commandZone.removeCommander(commander);

            assertThat(commandZone.contains(commander)).isFalse();
        }
    }

    @Nested
    class AllCommandersOperations {

        @Test
        void allCommandersReturnsEmptyForEmptyZone() {
            assertThat(commandZone.allCommanders()).isEmpty();
        }

        @Test
        void allCommandersReturnsAllCommanders() {
            var commander1 = createCommander(player1, "P1 Commander");
            var commander2 = createCommander(player2, "P2 Commander");
            commandZone.addCommander(commander1, player1);
            commandZone.addCommander(commander2, player2);

            assertThat(commandZone.allCommanders()).containsExactlyInAnyOrder(commander1, commander2);
        }

        @Test
        void allCommandersReturnsCopy() {
            var commander = createCommander(player1, "Kenrith");
            commandZone.addCommander(commander, player1);

            var all = commandZone.allCommanders();

            try {
                all.clear();
            } catch (UnsupportedOperationException e) {
                // Expected for immutable copy
            }
            assertThat(commandZone.size()).isEqualTo(1);
        }
    }

    @Nested
    class AllOperations {

        @Test
        void allReturnsEmptyForEmptyZone() {
            assertThat(commandZone.all()).isEmpty();
        }

        @Test
        void allReturnsAllGameObjects() {
            var commander1 = createCommander(player1, "P1 Commander");
            var commander2 = createCommander(player2, "P2 Commander");
            commandZone.addCommander(commander1, player1);
            commandZone.addCommander(commander2, player2);

            assertThat(commandZone.all()).containsExactlyInAnyOrder(commander1, commander2);
        }
    }

    @Nested
    class StreamOperations {

        @Test
        void streamReturnsAllCommanders() {
            var commander1 = createCommander(player1, "P1 Commander");
            var commander2 = createCommander(player2, "P2 Commander");
            commandZone.addCommander(commander1, player1);
            commandZone.addCommander(commander2, player2);

            var commanders = commandZone.stream().toList();

            assertThat(commanders).containsExactlyInAnyOrder(commander1, commander2);
        }

        @Test
        void streamOnEmptyZone() {
            assertThat(commandZone.stream().toList()).isEmpty();
        }
    }
}
