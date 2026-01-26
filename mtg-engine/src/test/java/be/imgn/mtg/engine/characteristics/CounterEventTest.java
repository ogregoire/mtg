package be.imgn.mtg.engine.characteristics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.ObjectId;

class CounterEventTest {

    @Test
    void isAddingReturnsTrueForPositiveAmount() {
        var player = mock(Player.class);
        var objectId = mock(ObjectId.class);
        var event = new CounterEvent(objectId, player, StandardCounterType.LOYALTY, 3);

        assertThat(event.isAdding()).isTrue();
    }

    @Test
    void isAddingReturnsFalseForNegativeAmount() {
        var player = mock(Player.class);
        var objectId = mock(ObjectId.class);
        var event = new CounterEvent(objectId, player, StandardCounterType.LOYALTY, -2);

        assertThat(event.isAdding()).isFalse();
    }

    @Test
    void isAddingReturnsFalseForZeroAmount() {
        var player = mock(Player.class);
        var objectId = mock(ObjectId.class);
        var event = new CounterEvent(objectId, player, StandardCounterType.LOYALTY, 0);

        assertThat(event.isAdding()).isFalse();
    }

    @Test
    void isRemovingReturnsTrueForNegativeAmount() {
        var player = mock(Player.class);
        var objectId = mock(ObjectId.class);
        var event = new CounterEvent(objectId, player, StandardCounterType.PLUS_ONE_PLUS_ONE, -1);

        assertThat(event.isRemoving()).isTrue();
    }

    @Test
    void isRemovingReturnsFalseForPositiveAmount() {
        var player = mock(Player.class);
        var objectId = mock(ObjectId.class);
        var event = new CounterEvent(objectId, player, StandardCounterType.PLUS_ONE_PLUS_ONE, 5);

        assertThat(event.isRemoving()).isFalse();
    }

    @Test
    void isRemovingReturnsFalseForZeroAmount() {
        var player = mock(Player.class);
        var objectId = mock(ObjectId.class);
        var event = new CounterEvent(objectId, player, StandardCounterType.PLUS_ONE_PLUS_ONE, 0);

        assertThat(event.isRemoving()).isFalse();
    }

    @Test
    void affectedPlayerReturnsController() {
        var player = mock(Player.class);
        var objectId = mock(ObjectId.class);
        var event = new CounterEvent(objectId, player, StandardCounterType.LOYALTY, 2);

        assertThat(event.affectedPlayer()).isSameAs(player);
    }
}
