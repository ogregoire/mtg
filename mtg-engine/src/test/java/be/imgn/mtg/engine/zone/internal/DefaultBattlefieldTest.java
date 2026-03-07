package be.imgn.mtg.engine.zone.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.CreatureType;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.object.Token;
import be.imgn.mtg.engine.state.internal.ObjectStore;
import be.imgn.mtg.engine.zone.EntersBattlefieldEvent;
import be.imgn.mtg.engine.zone.ZoneType;

class DefaultBattlefieldTest {

    Player player1;
    Player player2;
    GameEventProcessor eventProcessor;
    DefaultBattlefield battlefield;

    @BeforeEach
    void setUp() {
        player1 = mock(Player.class);
        player2 = mock(Player.class);
        eventProcessor = mock(GameEventProcessor.class);
        battlefield = new DefaultBattlefield(new ObjectStore(), eventProcessor);

        // Simulate the event processor resolving ETB by adding the permanent to the battlefield
        doAnswer(invocation -> {
                    var event = invocation.getArgument(0);
                    if (event instanceof EntersBattlefieldEvent etb) {
                        battlefield.enter(etb.permanent());
                    }
                    return null;
                })
                .when(eventProcessor)
                .process(any());
    }

    private Card createCreatureCard(Player owner, String name) {
        return Card.builder()
                .owner(owner)
                .controller(owner)
                .name(name)
                .type(Type.CREATURE)
                .subtype(CreatureType.HUMAN)
                .power(Value.of(2))
                .toughness(Value.of(2))
                .build();
    }

    private Card createLandCard(Player owner, String name) {
        return Card.builder()
                .owner(owner)
                .controller(owner)
                .name(name)
                .type(Type.LAND)
                .build();
    }

    private Token createToken(Player owner, String name) {
        return Token.builder()
                .owner(owner)
                .controller(owner)
                .name(name)
                .color(Color.WHITE)
                .type(Type.CREATURE)
                .subtype(CreatureType.SOLDIER)
                .power(Value.of(1))
                .toughness(Value.of(1))
                .build();
    }

    @Nested
    class BasicOperations {

        @Test
        void emptyBattlefield() {
            assertThat(battlefield.isEmpty()).isTrue();
            assertThat(battlefield.size()).isZero();
        }

        @Test
        void enterCardIncreasesSize() {
            var card = createCreatureCard(player1, "Creature");

            battlefield.enter(card, player1);

            assertThat(battlefield.size()).isEqualTo(1);
            assertThat(battlefield.isEmpty()).isFalse();
        }

        @Test
        void enterTokenIncreasesSize() {
            var token = createToken(player1, "Soldier");

            battlefield.enter(token, player1);

            assertThat(battlefield.size()).isEqualTo(1);
        }

        @Test
        void enterPermanentDirectly() {
            var card = createCreatureCard(player1, "Creature");
            var permanent = Permanent.fromCard(card, player1).build();

            battlefield.enter(permanent);

            assertThat(battlefield.size()).isEqualTo(1);
            assertThat(battlefield.contains(permanent)).isTrue();
        }

        @Test
        void containsReturnsTrueForPresentPermanent() {
            var card = createCreatureCard(player1, "Creature");
            var permanent = battlefield.enter(card, player1);

            assertThat(battlefield.contains(permanent)).isTrue();
        }

        @Test
        void containsReturnsFalseForAbsentPermanent() {
            var card = createCreatureCard(player1, "Not Added");
            var permanent = Permanent.fromCard(card, player1).build();
            assertThat(battlefield.contains(permanent)).isFalse();
        }
    }

    @Nested
    class RemoveOperations {

        @Test
        void removeNonExistent() {
            var card = createCreatureCard(player1, "Not Added");
            var permanent = Permanent.fromCard(card, player1).build();
            var removed = battlefield.remove(permanent);

            assertThat(removed).isFalse();
        }

        @Test
        void removeExisting() {
            var card1 = createCreatureCard(player1, "Creature 1");
            var card2 = createCreatureCard(player1, "Creature 2");
            var permanent1 = battlefield.enter(card1, player1);
            battlefield.enter(card2, player1);

            var removed = battlefield.remove(permanent1);

            assertThat(removed).isTrue();
            assertThat(battlefield.size()).isEqualTo(1);
        }

        @Test
        void removeUpdatesControllerList() {
            var card = createCreatureCard(player1, "Creature");
            var permanent = battlefield.enter(card, player1);

            battlefield.remove(permanent);

            assertThat(battlefield.controlledBy(player1)).isEmpty();
        }
    }

    @Nested
    class ControlledByOperations {

        @Test
        void controlledByReturnsEmptyForNoController() {
            assertThat(battlefield.controlledBy(player1)).isEmpty();
        }

        @Test
        void controlledByReturnsPermanentsForController() {
            var card1 = createCreatureCard(player1, "P1 Creature");
            var card2 = createCreatureCard(player2, "P2 Creature");
            var permanent1 = battlefield.enter(card1, player1);
            battlefield.enter(card2, player2);

            assertThat(battlefield.controlledBy(player1)).containsExactly(permanent1);
        }

        @Test
        void controlledByReturnsMultiplePermanents() {
            var card1 = createCreatureCard(player1, "Creature 1");
            var card2 = createCreatureCard(player1, "Creature 2");
            var permanent1 = battlefield.enter(card1, player1);
            var permanent2 = battlefield.enter(card2, player1);

            assertThat(battlefield.controlledBy(player1)).containsExactly(permanent1, permanent2);
        }
    }

    @Nested
    class OfTypeOperations {

        @Test
        void ofTypeReturnsEmptyWhenNoMatch() {
            var creature = createCreatureCard(player1, "Creature");
            battlefield.enter(creature, player1);

            assertThat(battlefield.ofType(Type.LAND)).isEmpty();
        }

        @Test
        void ofTypeReturnsMatchingPermanents() {
            var creature = createCreatureCard(player1, "Creature");
            var land = createLandCard(player1, "Land");
            var creaturePermanent = battlefield.enter(creature, player1);
            battlefield.enter(land, player1);

            assertThat(battlefield.ofType(Type.CREATURE)).containsExactly(creaturePermanent);
        }
    }

    @Nested
    class ControlledByOfTypeOperations {

        @Test
        void controlledByOfTypeReturnsEmptyWhenNoController() {
            assertThat(battlefield.controlledByOfType(player1, Type.CREATURE)).isEmpty();
        }

        @Test
        void controlledByOfTypeReturnsEmptyWhenNoTypeMatch() {
            var creature = createCreatureCard(player1, "Creature");
            battlefield.enter(creature, player1);

            assertThat(battlefield.controlledByOfType(player1, Type.LAND)).isEmpty();
        }

        @Test
        void controlledByOfTypeReturnsMatching() {
            var p1Creature = createCreatureCard(player1, "P1 Creature");
            var p1Land = createLandCard(player1, "P1 Land");
            var p2Creature = createCreatureCard(player2, "P2 Creature");

            var p1CreaturePerm = battlefield.enter(p1Creature, player1);
            battlefield.enter(p1Land, player1);
            battlefield.enter(p2Creature, player2);

            assertThat(battlefield.controlledByOfType(player1, Type.CREATURE)).containsExactly(p1CreaturePerm);
        }
    }

    @Nested
    class AllOperations {

        @Test
        void allReturnsEmptyForEmptyBattlefield() {
            assertThat(battlefield.all()).isEmpty();
        }

        @Test
        void allReturnsAllPermanents() {
            var card1 = createCreatureCard(player1, "Creature 1");
            var card2 = createCreatureCard(player2, "Creature 2");
            var permanent1 = battlefield.enter(card1, player1);
            var permanent2 = battlefield.enter(card2, player2);

            assertThat(battlefield.all()).containsExactlyInAnyOrder(permanent1, permanent2);
        }

        @Test
        void allReturnsCopy() {
            var card = createCreatureCard(player1, "Creature");
            battlefield.enter(card, player1);

            var all = battlefield.all();

            try {
                all.clear();
            } catch (UnsupportedOperationException e) {
                // Expected for immutable copy
            }
            assertThat(battlefield.size()).isEqualTo(1);
        }
    }

    @Nested
    class StreamOperations {

        @Test
        void streamReturnsAllPermanents() {
            var card1 = createCreatureCard(player1, "Creature 1");
            var card2 = createCreatureCard(player2, "Creature 2");
            var permanent1 = battlefield.enter(card1, player1);
            var permanent2 = battlefield.enter(card2, player2);

            var permanents = battlefield.stream().toList();

            assertThat(permanents).containsExactlyInAnyOrder(permanent1, permanent2);
        }

        @Test
        void streamOnEmptyBattlefield() {
            assertThat(battlefield.stream().toList()).isEmpty();
        }
    }

    @Nested
    class EnterFromCard {

        @Test
        void processesEntersBattlefieldEvent() {
            var card = createCreatureCard(player1, "Bear");

            battlefield.enter(card, player1);

            verify(eventProcessor).process(any(EntersBattlefieldEvent.class));
        }

        @Test
        void eventContainsCorrectPermanent() {
            var card = createCreatureCard(player1, "Bear");

            var permanent = battlefield.enter(card, player1);

            var captor = ArgumentCaptor.forClass(EntersBattlefieldEvent.class);
            verify(eventProcessor).process(captor.capture());
            var event = captor.getValue();
            assertThat(event.permanent()).isEqualTo(permanent);
        }

        @Test
        void eventHasCorrectFromZone() {
            var card = createCreatureCard(player1, "Bear");

            battlefield.enter(card, player1);

            var captor = ArgumentCaptor.forClass(EntersBattlefieldEvent.class);
            verify(eventProcessor).process(captor.capture());
            var event = captor.getValue();
            assertThat(event.from()).isEqualTo(ZoneType.HAND);
        }

        @Test
        void enterWithFromZoneUsesSpecifiedZone() {
            var card = createCreatureCard(player1, "Bear");

            battlefield.enter(card, player1, ZoneType.STACK);

            var captor = ArgumentCaptor.forClass(EntersBattlefieldEvent.class);
            verify(eventProcessor).process(captor.capture());
            var event = captor.getValue();
            assertThat(event.from()).isEqualTo(ZoneType.STACK);
        }
    }
}
