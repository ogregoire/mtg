package be.imgn.mtg.engine.object;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.AbilityId;
import be.imgn.mtg.engine.ability.StaticAbility;
import be.imgn.mtg.engine.game.Player;

class EmblemTest {

    @Nested
    class Builder {

        @Test
        void buildsEmblemWithOwnerAndController() {
            var player = mock(Player.class);

            var emblem = Emblem.builder().owner(player).controller(player).build();

            assertThat(emblem.owner()).isSameAs(player);
            assertThat(emblem.controller()).isSameAs(player);
        }

        @Test
        void buildsEmblemWithDifferentOwnerAndController() {
            var owner = mock(Player.class);
            var controller = mock(Player.class);

            var emblem = Emblem.builder().owner(owner).controller(controller).build();

            assertThat(emblem.owner()).isSameAs(owner);
            assertThat(emblem.controller()).isSameAs(controller);
        }

        @Test
        void buildsEmblemWithAbility() {
            var player = mock(Player.class);
            var ability = mock(StaticAbility.class);
            var abilityId = new AbilityId();
            when(ability.id()).thenReturn(abilityId);

            var emblem = Emblem.builder()
                    .owner(player)
                    .controller(player)
                    .addAbility(ability)
                    .build();

            assertThat(emblem.abilities()).isNotNull();
            assertThat(emblem.abilities().stream()).containsExactly(ability);
        }

        @Test
        void buildsEmblemWithEmptyAbilities() {
            var player = mock(Player.class);

            var emblem = Emblem.builder().owner(player).controller(player).build();

            assertThat(emblem.abilities()).isNotNull();
            assertThat(emblem.abilities().stream()).isEmpty();
        }

        @Test
        void throwsWhenOwnerIsNull() {
            var player = mock(Player.class);

            assertThatThrownBy(() -> Emblem.builder().controller(player).build())
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void throwsWhenControllerIsNull() {
            var player = mock(Player.class);

            assertThatThrownBy(() -> Emblem.builder().owner(player).build()).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Hierarchy {

        @Test
        void emblemIsGameObject() {
            var player = mock(Player.class);
            var emblem = Emblem.builder().owner(player).controller(player).build();

            assertThat(emblem).isInstanceOf(GameObject.class);
        }

        @Test
        void emblemIsNotTypedObject() {
            var player = mock(Player.class);
            var emblem = Emblem.builder().owner(player).controller(player).build();

            assertThat(emblem).isNotInstanceOf(TypedObject.class);
        }
    }

    @Nested
    class Abilities {

        @Test
        void multipleAbilities() {
            var player = mock(Player.class);
            var ability1 = mock(StaticAbility.class);
            var ability2 = mock(StaticAbility.class);
            when(ability1.id()).thenReturn(new AbilityId());
            when(ability2.id()).thenReturn(new AbilityId());

            var emblem = Emblem.builder()
                    .owner(player)
                    .controller(player)
                    .addAbility(ability1)
                    .addAbility(ability2)
                    .build();

            assertThat(emblem.abilities().stream()).containsExactly(ability1, ability2);
        }
    }
}
