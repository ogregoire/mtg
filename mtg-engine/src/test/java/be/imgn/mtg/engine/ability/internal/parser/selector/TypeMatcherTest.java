package be.imgn.mtg.engine.ability.internal.parser.selector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.object.Spell;
import be.imgn.mtg.engine.object.Token;

@DisplayName("TypeMatcher.matches")
class TypeMatcherTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
    }

    private Card creatureCard() {
        return Card.builder()
                .owner(player)
                .controller(player)
                .name("Grizzly Bears")
                .type(Type.CREATURE)
                .build();
    }

    private Permanent creaturePermanent() {
        return Permanent.fromCard(creatureCard(), player).build();
    }

    private Permanent planeswalkerPermanent() {
        return Permanent.fromCard(
                        Card.builder()
                                .owner(player)
                                .controller(player)
                                .name("Jace")
                                .type(Type.PLANESWALKER)
                                .build(),
                        player)
                .build();
    }

    private Permanent artifactPermanent() {
        return Permanent.fromCard(
                        Card.builder()
                                .owner(player)
                                .controller(player)
                                .name("Sol Ring")
                                .type(Type.ARTIFACT)
                                .build(),
                        player)
                .build();
    }

    private Card instantCard() {
        return Card.builder()
                .owner(player)
                .controller(player)
                .name("Lightning Bolt")
                .type(Type.INSTANT)
                .build();
    }

    private Card sorceryCard() {
        return Card.builder()
                .owner(player)
                .controller(player)
                .name("Divination")
                .type(Type.SORCERY)
                .build();
    }

    private Spell creatureSpell() {
        return Spell.fromCard(creatureCard(), player).build();
    }

    private Spell instantSpell() {
        return Spell.fromCard(instantCard(), player).build();
    }

    private Permanent tokenPermanent() {
        var token = Token.builder()
                .owner(player)
                .controller(player)
                .name("Soldier")
                .type(Type.CREATURE)
                .build();
        return Permanent.fromToken(token, player).build();
    }

    @Nested
    @DisplayName("Single")
    class SingleTests {

        private final TypeMatcher matcher = new TypeMatcher.Single(Type.CREATURE);

        @Test
        @DisplayName("matches a creature permanent")
        void matchesCreaturePermanent() {
            assertThat(matcher.matches(creaturePermanent())).isTrue();
        }

        @Test
        @DisplayName("does not match a creature card")
        void doesNotMatchCreatureCard() {
            assertThat(matcher.matches(creatureCard())).isFalse();
        }

        @Test
        @DisplayName("does not match a creature spell")
        void doesNotMatchCreatureSpell() {
            assertThat(matcher.matches(creatureSpell())).isFalse();
        }

        @Test
        @DisplayName("does not match an artifact permanent")
        void doesNotMatchArtifactPermanent() {
            assertThat(matcher.matches(artifactPermanent())).isFalse();
        }
    }

    @Nested
    @DisplayName("Permanent")
    class PermanentTests {

        private final TypeMatcher matcher = new TypeMatcher.Permanent();

        @Test
        @DisplayName("matches any permanent")
        void matchesPermanent() {
            assertThat(matcher.matches(creaturePermanent())).isTrue();
            assertThat(matcher.matches(artifactPermanent())).isTrue();
        }

        @Test
        @DisplayName("does not match a card")
        void doesNotMatchCard() {
            assertThat(matcher.matches(creatureCard())).isFalse();
        }

        @Test
        @DisplayName("does not match a spell")
        void doesNotMatchSpell() {
            assertThat(matcher.matches(creatureSpell())).isFalse();
        }
    }

    @Nested
    @DisplayName("Spell")
    class SpellTests {

        private final TypeMatcher matcher = new TypeMatcher.Spell();

        @Test
        @DisplayName("matches a spell")
        void matchesSpell() {
            assertThat(matcher.matches(creatureSpell())).isTrue();
        }

        @Test
        @DisplayName("does not match a permanent")
        void doesNotMatchPermanent() {
            assertThat(matcher.matches(creaturePermanent())).isFalse();
        }

        @Test
        @DisplayName("does not match a card")
        void doesNotMatchCard() {
            assertThat(matcher.matches(creatureCard())).isFalse();
        }
    }

    @Nested
    @DisplayName("Card")
    class CardTests {

        private final TypeMatcher matcher = new TypeMatcher.Card();

        @Test
        @DisplayName("matches a card")
        void matchesCard() {
            assertThat(matcher.matches(creatureCard())).isTrue();
        }

        @Test
        @DisplayName("does not match a permanent")
        void doesNotMatchPermanent() {
            assertThat(matcher.matches(creaturePermanent())).isFalse();
        }

        @Test
        @DisplayName("does not match a spell")
        void doesNotMatchSpell() {
            assertThat(matcher.matches(creatureSpell())).isFalse();
        }
    }

    @Nested
    @DisplayName("Target")
    class TargetTests {

        private final TypeMatcher matcher = new TypeMatcher.Target();

        @Test
        @DisplayName("matches a creature permanent")
        void matchesCreature() {
            assertThat(matcher.matches(creaturePermanent())).isTrue();
        }

        @Test
        @DisplayName("matches a planeswalker permanent")
        void matchesPlaneswalker() {
            assertThat(matcher.matches(planeswalkerPermanent())).isTrue();
        }

        @Test
        @DisplayName("does not match an artifact permanent")
        void doesNotMatchArtifact() {
            assertThat(matcher.matches(artifactPermanent())).isFalse();
        }

        @Test
        @DisplayName("does not match a card")
        void doesNotMatchCard() {
            assertThat(matcher.matches(creatureCard())).isFalse();
        }
    }

    @Nested
    @DisplayName("Or")
    class OrTests {

        private final TypeMatcher matcher = new TypeMatcher.Or(
                List.of(new TypeMatcher.Single(Type.ARTIFACT), new TypeMatcher.Single(Type.ENCHANTMENT)));

        @Test
        @DisplayName("matches an artifact permanent")
        void matchesArtifact() {
            assertThat(matcher.matches(artifactPermanent())).isTrue();
        }

        @Test
        @DisplayName("does not match a creature permanent")
        void doesNotMatchCreature() {
            assertThat(matcher.matches(creaturePermanent())).isFalse();
        }

        @Test
        @DisplayName("does not match an artifact card")
        void doesNotMatchArtifactCard() {
            var card = Card.builder()
                    .owner(player)
                    .controller(player)
                    .name("Sol Ring")
                    .type(Type.ARTIFACT)
                    .build();
            assertThat(matcher.matches(card)).isFalse();
        }
    }

    @Nested
    @DisplayName("Token")
    class TokenTests {

        private final TypeMatcher matcher = new TypeMatcher.Token();

        @Test
        @DisplayName("matches a token permanent")
        void matchesTokenPermanent() {
            assertThat(matcher.matches(tokenPermanent())).isTrue();
        }

        @Test
        @DisplayName("does not match a card-based permanent")
        void doesNotMatchCardPermanent() {
            assertThat(matcher.matches(creaturePermanent())).isFalse();
        }

        @Test
        @DisplayName("does not match a card")
        void doesNotMatchCard() {
            assertThat(matcher.matches(creatureCard())).isFalse();
        }
    }

    @Nested
    @DisplayName("CardWithType")
    class CardWithTypeTests {

        private final TypeMatcher matcher = new TypeMatcher.CardWithType(List.of(Type.CREATURE));

        @Test
        @DisplayName("matches a creature card")
        void matchesCreatureCard() {
            assertThat(matcher.matches(creatureCard())).isTrue();
        }

        @Test
        @DisplayName("does not match an instant card")
        void doesNotMatchInstantCard() {
            assertThat(matcher.matches(instantCard())).isFalse();
        }

        @Test
        @DisplayName("does not match a creature permanent")
        void doesNotMatchPermanent() {
            assertThat(matcher.matches(creaturePermanent())).isFalse();
        }

        @Test
        @DisplayName("does not match a creature spell")
        void doesNotMatchSpell() {
            assertThat(matcher.matches(creatureSpell())).isFalse();
        }

        @Test
        @DisplayName("matches when any listed type matches")
        void matchesAnyListedType() {
            var multiType = new TypeMatcher.CardWithType(List.of(Type.INSTANT, Type.SORCERY));
            assertThat(multiType.matches(instantCard())).isTrue();
            assertThat(multiType.matches(sorceryCard())).isTrue();
            assertThat(multiType.matches(creatureCard())).isFalse();
        }
    }

    @Nested
    @DisplayName("CardWithPermanentType")
    class CardWithPermanentTypeTests {

        private final TypeMatcher matcher = new TypeMatcher.CardWithPermanentType();

        @Test
        @DisplayName("matches a creature card")
        void matchesCreatureCard() {
            assertThat(matcher.matches(creatureCard())).isTrue();
        }

        @Test
        @DisplayName("does not match an instant card")
        void doesNotMatchInstantCard() {
            assertThat(matcher.matches(instantCard())).isFalse();
        }

        @Test
        @DisplayName("does not match a sorcery card")
        void doesNotMatchSorceryCard() {
            assertThat(matcher.matches(sorceryCard())).isFalse();
        }

        @Test
        @DisplayName("does not match a permanent")
        void doesNotMatchPermanent() {
            assertThat(matcher.matches(creaturePermanent())).isFalse();
        }

        @Test
        @DisplayName("does not match a spell")
        void doesNotMatchSpell() {
            assertThat(matcher.matches(creatureSpell())).isFalse();
        }
    }

    @Nested
    @DisplayName("CardWithSpellType")
    class CardWithSpellTypeTests {

        private final TypeMatcher matcher = new TypeMatcher.CardWithSpellType();

        @Test
        @DisplayName("matches an instant card")
        void matchesInstantCard() {
            assertThat(matcher.matches(instantCard())).isTrue();
        }

        @Test
        @DisplayName("matches a sorcery card")
        void matchesSorceryCard() {
            assertThat(matcher.matches(sorceryCard())).isTrue();
        }

        @Test
        @DisplayName("does not match a creature card")
        void doesNotMatchCreatureCard() {
            assertThat(matcher.matches(creatureCard())).isFalse();
        }

        @Test
        @DisplayName("does not match a permanent")
        void doesNotMatchPermanent() {
            assertThat(matcher.matches(creaturePermanent())).isFalse();
        }

        @Test
        @DisplayName("does not match a spell")
        void doesNotMatchSpell() {
            assertThat(matcher.matches(instantSpell())).isFalse();
        }
    }

    @Nested
    @DisplayName("SpellWithType")
    class SpellWithTypeTests {

        private final TypeMatcher matcher = new TypeMatcher.SpellWithType(List.of(Type.CREATURE));

        @Test
        @DisplayName("matches a creature spell")
        void matchesCreatureSpell() {
            assertThat(matcher.matches(creatureSpell())).isTrue();
        }

        @Test
        @DisplayName("does not match an instant spell")
        void doesNotMatchInstantSpell() {
            assertThat(matcher.matches(instantSpell())).isFalse();
        }

        @Test
        @DisplayName("does not match a creature permanent")
        void doesNotMatchPermanent() {
            assertThat(matcher.matches(creaturePermanent())).isFalse();
        }

        @Test
        @DisplayName("does not match a creature card")
        void doesNotMatchCard() {
            assertThat(matcher.matches(creatureCard())).isFalse();
        }

        @Test
        @DisplayName("matches when any listed type matches")
        void matchesAnyListedType() {
            var multiType = new TypeMatcher.SpellWithType(List.of(Type.INSTANT, Type.SORCERY));
            assertThat(multiType.matches(instantSpell())).isTrue();
            assertThat(multiType.matches(creatureSpell())).isFalse();
        }
    }

    @Nested
    @DisplayName("SpellWithPermanentType")
    class SpellWithPermanentTypeTests {

        private final TypeMatcher matcher = new TypeMatcher.SpellWithPermanentType();

        @Test
        @DisplayName("matches a creature spell")
        void matchesCreatureSpell() {
            assertThat(matcher.matches(creatureSpell())).isTrue();
        }

        @Test
        @DisplayName("does not match an instant spell")
        void doesNotMatchInstantSpell() {
            assertThat(matcher.matches(instantSpell())).isFalse();
        }

        @Test
        @DisplayName("does not match a permanent")
        void doesNotMatchPermanent() {
            assertThat(matcher.matches(creaturePermanent())).isFalse();
        }

        @Test
        @DisplayName("does not match a card")
        void doesNotMatchCard() {
            assertThat(matcher.matches(creatureCard())).isFalse();
        }
    }

    @Nested
    @DisplayName("Or with Empty Matchers")
    class OrEmptyTests {

        @Test
        @DisplayName("does not match when no matchers match")
        void doesNotMatchWhenEmpty() {
            var matcher = new TypeMatcher.Or(List.of());
            assertThat(matcher.matches(artifactPermanent())).isFalse();
        }
    }

    @Nested
    @DisplayName("Target Edge Cases")
    class TargetEdgeCaseTests {

        @Test
        @DisplayName("does not match a spell")
        void doesNotMatchSpell() {
            var matcher = new TypeMatcher.Target();
            assertThat(matcher.matches(creatureSpell())).isFalse();
        }

        @Test
        @DisplayName("matches a token creature")
        void matchesTokenCreature() {
            // Token creatures are still creatures and can be targeted by damage effects
            var matcher = new TypeMatcher.Target();
            assertThat(matcher.matches(tokenPermanent())).isTrue();
        }
    }
}
