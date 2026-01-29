package be.imgn.mtg.engine.object;

import static be.imgn.mtg.engine.assertions.MTGAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.StaticAbility;
import be.imgn.mtg.engine.characteristics.ArtifactType;
import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.CreatureType;
import be.imgn.mtg.engine.characteristics.Supertype;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.game.Player;

class TokenTest {

    @Test
    void basicCreatureToken() {
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

        assertThat(token)
                .hasName("Soldier Token")
                .hasOwner(player)
                .hasController(player)
                .isWhite()
                .isCreature()
                .hasPower(1)
                .hasToughness(1);
    }

    @Test
    void colorlessArtifactToken() {
        var player = mock(Player.class);

        var token = Token.builder()
                .owner(player)
                .controller(player)
                .name("Treasure")
                .type(Type.ARTIFACT)
                .subtype(ArtifactType.TREASURE)
                .build();

        assertThat(token).hasName("Treasure").isArtifact().isColorless();
    }

    @Test
    void multiColoredToken() {
        var player = mock(Player.class);

        var token = Token.builder()
                .owner(player)
                .controller(player)
                .name("Elemental Token")
                .colors(Color.RED, Color.GREEN)
                .type(Type.CREATURE)
                .subtype(CreatureType.ELEMENTAL)
                .power(Value.of(5))
                .toughness(Value.of(5))
                .build();

        assertThat(token).isMultiColored().isRed().isGreen().hasPower(5).hasToughness(5);
    }

    @Nested
    class TokenOwnership {

        @Test
        void ownerAndControllerAreSameByDefault() {
            var player = mock(Player.class);

            var token = Token.builder()
                    .owner(player)
                    .controller(player)
                    .name("Token")
                    .type(Type.CREATURE)
                    .build();

            assertThat(token.owner()).isSameAs(player);
            assertThat(token.controller()).isSameAs(player);
        }

        @Test
        void ownerAndControllerCanBeDifferent() {
            var owner = mock(Player.class);
            var controller = mock(Player.class);

            var token = Token.builder()
                    .owner(owner)
                    .controller(controller)
                    .name("Token")
                    .type(Type.CREATURE)
                    .build();

            assertThat(token.owner()).isSameAs(owner);
            assertThat(token.controller()).isSameAs(controller);
        }
    }

    @Nested
    class TokenIdentity {

        @Test
        void eachTokenGetsUniqueId() {
            var player = mock(Player.class);

            var token1 = Token.builder()
                    .owner(player)
                    .controller(player)
                    .name("Soldier")
                    .type(Type.CREATURE)
                    .build();

            var token2 = Token.builder()
                    .owner(player)
                    .controller(player)
                    .name("Soldier")
                    .type(Type.CREATURE)
                    .build();

            assertThat(token1).isNotSameAs(token2);
        }

        @Test
        void tokenHasName() {
            var player = mock(Player.class);

            var token = Token.builder()
                    .owner(player)
                    .controller(player)
                    .name("Dragon Token")
                    .type(Type.CREATURE)
                    .build();

            assertThat(token.name()).isEqualTo("Dragon Token");
        }
    }

    @Nested
    class TokenAbilities {

        @Test
        void tokenWithAbility() {
            var player = mock(Player.class);
            var ability = mock(StaticAbility.class);

            var token = Token.builder()
                    .owner(player)
                    .controller(player)
                    .name("Token")
                    .type(Type.CREATURE)
                    .ability(ability)
                    .build();

            assertThat(token.abilities()).hasCount(1).contains(ability);
        }

        @Test
        void tokenWithMultipleAbilities() {
            var player = mock(Player.class);
            var ability1 = mock(StaticAbility.class);
            var ability2 = mock(StaticAbility.class);

            var token = Token.builder()
                    .owner(player)
                    .controller(player)
                    .name("Token")
                    .type(Type.CREATURE)
                    .addAbility(ability1)
                    .addAbility(ability2)
                    .build();

            assertThat(token.abilities()).hasCount(2);
        }
    }

    @Nested
    class SpecialTokenTypes {

        @Test
        void legendaryToken() {
            var player = mock(Player.class);

            var token = Token.builder()
                    .owner(player)
                    .controller(player)
                    .name("Legendary Token")
                    .type(Type.CREATURE)
                    .supertype(Supertype.LEGENDARY)
                    .power(Value.of(5))
                    .toughness(Value.of(5))
                    .build();

            assertThat(token).isLegendary();
        }

        @Test
        void artifactCreatureToken() {
            var player = mock(Player.class);

            var token = Token.builder()
                    .owner(player)
                    .controller(player)
                    .name("Servo")
                    .types(Type.ARTIFACT, Type.CREATURE)
                    .subtype(CreatureType.SERVO)
                    .power(Value.of(1))
                    .toughness(Value.of(1))
                    .build();

            assertThat(token).isArtifact().isCreature();
        }

        @Test
        void enchantmentCreatureToken() {
            var player = mock(Player.class);

            var token = Token.builder()
                    .owner(player)
                    .controller(player)
                    .name("Nymph")
                    .types(Type.ENCHANTMENT, Type.CREATURE)
                    .color(Color.GREEN)
                    .power(Value.of(2))
                    .toughness(Value.of(2))
                    .build();

            assertThat(token).isEnchantment().isCreature().isGreen();
        }
    }

    @Nested
    class TokenWithoutPowerToughness {

        @Test
        void nonCreatureTokenHasNoPowerToughness() {
            var player = mock(Player.class);

            var token = Token.builder()
                    .owner(player)
                    .controller(player)
                    .name("Artifact Token")
                    .type(Type.ARTIFACT)
                    .build();

            assertThat(token.power()).isNull();
            assertThat(token.toughness()).isNull();
        }
    }
}
