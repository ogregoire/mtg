package be.imgn.mtg.engine.mana;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.Supertype;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;

class ManaTest {

    @Nested
    class StandardMana {

        @Test
        void createWithType() {
            var player = mock(Player.class);
            var source =
                    Card.builder().owner(player).controller(player).name("Test").build();
            var mana = Mana.of(ManaType.GREEN, source);

            assertThat(mana).isInstanceOf(Mana.Standard.class);
            assertThat(mana.type()).isEqualTo(ManaType.GREEN);
            assertThat(mana.source()).isSameAs(source);
        }

        @Test
        @SuppressWarnings("NullAway")
        void nullTypeThrows() {
            var player = mock(Player.class);
            var source =
                    Card.builder().owner(player).controller(player).name("Test").build();

            assertThatThrownBy(() -> Mana.of(null, source))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("type");
        }

        @Test
        @SuppressWarnings("NullAway")
        void nullSourceThrows() {
            assertThatThrownBy(() -> Mana.of(ManaType.RED, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("source");
        }

        @Test
        void isSnowWithSnowSource() {
            var player = mock(Player.class);
            var snowSource = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Snow Permanent")
                    .supertype(Supertype.SNOW)
                    .build();
            var mana = Mana.of(ManaType.BLUE, snowSource);

            assertThat(mana.isSnow()).isTrue();
        }

        @Test
        void isNotSnowWithNonSnowSource() {
            var player = mock(Player.class);
            var source = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Regular Permanent")
                    .build();
            var mana = Mana.of(ManaType.WHITE, source);

            assertThat(mana.isSnow()).isFalse();
        }

        @Test
        void toStringWithColoredMana() {
            var player = mock(Player.class);
            var source =
                    Card.builder().owner(player).controller(player).name("Test").build();
            var mana = Mana.of(ManaType.GREEN, source);

            assertThat(mana.toString()).isEqualTo("green mana");
        }

        @Test
        void toStringWithColorlessMana() {
            var player = mock(Player.class);
            var source =
                    Card.builder().owner(player).controller(player).name("Test").build();
            var mana = Mana.of(ManaType.COLORLESS, source);

            assertThat(mana.toString()).isEqualTo("colorless mana");
        }

        @Test
        void toStringWithSnowMana() {
            var player = mock(Player.class);
            var snowSource = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Snow Permanent")
                    .supertype(Supertype.SNOW)
                    .build();
            var mana = Mana.of(ManaType.RED, snowSource);

            assertThat(mana.toString()).isEqualTo("snow red mana");
        }

        @Test
        void allColoredTypes() {
            var player = mock(Player.class);
            var source =
                    Card.builder().owner(player).controller(player).name("Test").build();

            assertThat(Mana.of(ManaType.WHITE, source).toString()).isEqualTo("white mana");
            assertThat(Mana.of(ManaType.BLUE, source).toString()).isEqualTo("blue mana");
            assertThat(Mana.of(ManaType.BLACK, source).toString()).isEqualTo("black mana");
            assertThat(Mana.of(ManaType.RED, source).toString()).isEqualTo("red mana");
            assertThat(Mana.of(ManaType.GREEN, source).toString()).isEqualTo("green mana");
        }
    }

    @Nested
    class RestrictedMana {

        @Test
        void createWithRestriction() {
            var player = mock(Player.class);
            var source =
                    Card.builder().owner(player).controller(player).name("Test").build();
            var restriction = new ManaRestriction.TypeRestriction(Type.CREATURE);
            var mana = Mana.restricted(ManaType.GREEN, source, restriction);

            assertThat(mana).isInstanceOf(Mana.Restricted.class);
            assertThat(mana.type()).isEqualTo(ManaType.GREEN);
            assertThat(mana.source()).isSameAs(source);

            var restricted = (Mana.Restricted) mana;
            assertThat(restricted.restriction()).isSameAs(restriction);
        }

        @Test
        @SuppressWarnings("NullAway")
        void nullTypeThrows() {
            var player = mock(Player.class);
            var source =
                    Card.builder().owner(player).controller(player).name("Test").build();
            var restriction = new ManaRestriction.TypeRestriction(Type.CREATURE);

            assertThatThrownBy(() -> Mana.restricted(null, source, restriction))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("type");
        }

        @Test
        @SuppressWarnings("NullAway")
        void nullSourceThrows() {
            var restriction = new ManaRestriction.TypeRestriction(Type.CREATURE);

            assertThatThrownBy(() -> Mana.restricted(ManaType.RED, null, restriction))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("source");
        }

        @Test
        @SuppressWarnings("NullAway")
        void nullRestrictionThrows() {
            var player = mock(Player.class);
            var source =
                    Card.builder().owner(player).controller(player).name("Test").build();

            assertThatThrownBy(() -> Mana.restricted(ManaType.BLUE, source, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("restriction");
        }

        @Test
        void isSnowWithSnowSource() {
            var player = mock(Player.class);
            var snowSource = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Snow Permanent")
                    .supertype(Supertype.SNOW)
                    .build();
            var restriction = new ManaRestriction.TypeRestriction(Type.ARTIFACT);
            var mana = Mana.restricted(ManaType.WHITE, snowSource, restriction);

            assertThat(mana.isSnow()).isTrue();
        }

        @Test
        void isNotSnowWithNonSnowSource() {
            var player = mock(Player.class);
            var source = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Regular Permanent")
                    .build();
            var restriction = new ManaRestriction.TypeRestriction(Type.ARTIFACT);
            var mana = Mana.restricted(ManaType.BLACK, source, restriction);

            assertThat(mana.isSnow()).isFalse();
        }

        @Test
        void toStringWithColoredMana() {
            var player = mock(Player.class);
            var source =
                    Card.builder().owner(player).controller(player).name("Test").build();
            var restriction = new ManaRestriction.TypeRestriction(Type.CREATURE);
            var mana = Mana.restricted(ManaType.GREEN, source, restriction);

            assertThat(mana.toString()).isEqualTo("green mana (restricted)");
        }

        @Test
        void toStringWithColorlessMana() {
            var player = mock(Player.class);
            var source =
                    Card.builder().owner(player).controller(player).name("Test").build();
            var restriction = new ManaRestriction.TypeRestriction(Type.ARTIFACT);
            var mana = Mana.restricted(ManaType.COLORLESS, source, restriction);

            assertThat(mana.toString()).isEqualTo("colorless mana (restricted)");
        }

        @Test
        void toStringWithSnowMana() {
            var player = mock(Player.class);
            var snowSource = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Snow Permanent")
                    .supertype(Supertype.SNOW)
                    .build();
            var restriction = new ManaRestriction.TypeRestriction(Type.INSTANT);
            var mana = Mana.restricted(ManaType.RED, snowSource, restriction);

            assertThat(mana.toString()).isEqualTo("snow red mana (restricted)");
        }

        @Test
        void allColoredTypes() {
            var player = mock(Player.class);
            var source =
                    Card.builder().owner(player).controller(player).name("Test").build();
            var restriction = new ManaRestriction.TypeRestriction(Type.SORCERY);

            assertThat(Mana.restricted(ManaType.WHITE, source, restriction).toString())
                    .isEqualTo("white mana (restricted)");
            assertThat(Mana.restricted(ManaType.BLUE, source, restriction).toString())
                    .isEqualTo("blue mana (restricted)");
            assertThat(Mana.restricted(ManaType.BLACK, source, restriction).toString())
                    .isEqualTo("black mana (restricted)");
            assertThat(Mana.restricted(ManaType.RED, source, restriction).toString())
                    .isEqualTo("red mana (restricted)");
            assertThat(Mana.restricted(ManaType.GREEN, source, restriction).toString())
                    .isEqualTo("green mana (restricted)");
        }
    }

    @Nested
    class PatternMatching {

        @Test
        void standardManaDoesNotMatchRestrictedPattern() {
            var player = mock(Player.class);
            var source =
                    Card.builder().owner(player).controller(player).name("Test").build();
            var mana = Mana.of(ManaType.GREEN, source);

            assertThat(mana).isNotInstanceOf(Mana.Restricted.class);
        }

        @Test
        void restrictedManaMatchesRestrictedPattern() {
            var player = mock(Player.class);
            var source =
                    Card.builder().owner(player).controller(player).name("Test").build();
            var restriction = new ManaRestriction.TypeRestriction(Type.CREATURE);
            var mana = Mana.restricted(ManaType.GREEN, source, restriction);

            assertThat(mana).isInstanceOf(Mana.Restricted.class);
            if (mana instanceof Mana.Restricted(var type, var src, var restr)) {
                assertThat(type).isEqualTo(ManaType.GREEN);
                assertThat(src).isSameAs(source);
                assertThat(restr).isSameAs(restriction);
            }
        }
    }
}
