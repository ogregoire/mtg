package be.imgn.mtg.engine.ability.internal.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.effect.DealDamageEffect;
import be.imgn.mtg.engine.effect.DestroyEffect;
import be.imgn.mtg.engine.effect.DrawEffect;

@DisplayName("SpellAbilityParser")
class SpellAbilityParserTest {

    @Nested
    @DisplayName("Single effect spells")
    class SingleEffect {

        @Test
        @DisplayName("\"Deal 3 damage to any target.\" → DealDamageEffect")
        void dealDamage() {
            var ability = SpellAbilityParser.parse("Deal 3 damage to any target.");
            assertThat(ability.id()).isNotNull();
            assertThat(ability.effects()).hasSize(1);
            assertThat(ability.effects().getFirst()).isInstanceOf(DealDamageEffect.class);
        }

        @Test
        @DisplayName("\"Destroy target creature.\" → DestroyEffect")
        void destroy() {
            var ability = SpellAbilityParser.parse("Destroy target creature.");
            assertThat(ability.effects()).hasSize(1);
            assertThat(ability.effects().getFirst()).isInstanceOf(DestroyEffect.class);
        }

        @Test
        @DisplayName("\"Draw two cards.\" → DrawEffect")
        void draw() {
            var ability = SpellAbilityParser.parse("Draw two cards.");
            assertThat(ability.effects()).hasSize(1);
            assertThat(ability.effects().getFirst()).isInstanceOf(DrawEffect.class);
        }
    }

    @Nested
    @DisplayName("Ability properties")
    class Properties {

        @Test
        @DisplayName("parsed spell ability stores oracle text")
        void storesOracleText() {
            var ability = SpellAbilityParser.parse("Deal 3 damage to any target.");
            assertThat(ability.oracleText()).isEqualTo("Deal 3 damage to any target.");
        }

        @Test
        @DisplayName("different parsed abilities have different IDs")
        void uniqueIds() {
            var a1 = SpellAbilityParser.parse("Deal 3 damage to any target.");
            var a2 = SpellAbilityParser.parse("Deal 3 damage to any target.");
            assertThat(a1.id()).isNotEqualTo(a2.id());
        }
    }
}
