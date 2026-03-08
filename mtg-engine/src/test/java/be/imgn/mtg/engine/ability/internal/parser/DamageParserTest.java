package be.imgn.mtg.engine.ability.internal.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.effect.DealDamageEffect;
import be.imgn.mtg.engine.effect.GainLifeEffect;
import be.imgn.mtg.engine.effect.LoseLifeEffect;
import be.imgn.mtg.engine.selector.CompositeSelector;
import be.imgn.mtg.engine.selector.ObjectSelector;
import be.imgn.mtg.engine.selector.PlayerReference;
import be.imgn.mtg.engine.selector.Qualifier;
import be.imgn.mtg.engine.selector.Quantifier;
import be.imgn.mtg.engine.selector.TypeMatcher;

@DisplayName("DamageParser")
class DamageParserTest {

    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    private DealDamageEffect parseDamage(String text) {
        return DamageParser.DEAL_DAMAGE_EFFECT.parseSkipping(WHITESPACE, text);
    }

    private GainLifeEffect parseGainLife(String text) {
        return DamageParser.GAIN_LIFE_EFFECT.parseSkipping(WHITESPACE, text);
    }

    private LoseLifeEffect parseLoseLife(String text) {
        return DamageParser.LOSE_LIFE_EFFECT.parseSkipping(WHITESPACE, text);
    }

    @Nested
    @DisplayName("Deal damage")
    class DealDamage {

        @Test
        @DisplayName("Deal 3 damage to target creature.")
        void dealThreeDamageToTargetCreature() {
            var effect = parseDamage("Deal 3 damage to target creature.");

            assertThat(effect.amount()).isEqualTo(new Amount.Exact(3));
            var select = (Subject.Select) effect.target();
            assertThat(((ObjectSelector) select.selector()).qualifiers()).containsExactly(new Qualifier.Target());
            assertThat(((ObjectSelector) select.selector()).typeMatcher())
                    .isEqualTo(new TypeMatcher.Single(Type.CREATURE));
        }

        @Test
        @DisplayName("Deal X damage to any target.")
        void dealXDamageToAnyTarget() {
            var effect = parseDamage("Deal X damage to any target.");

            assertThat(effect.amount()).isEqualTo(Amount.X);
            var select = (Subject.Select) effect.target();
            assertThat(select.selector()).isInstanceOf(CompositeSelector.class);
            assertThat(select.selector().quantifier()).isEqualTo(new Quantifier.One());
        }
    }

    @Nested
    @DisplayName("Gain life")
    class GainLife {

        @Test
        @DisplayName("You gain 3 life.")
        void youGainThreeLife() {
            var effect = parseGainLife("You gain 3 life.");

            assertThat(effect.player()).isEqualTo(Optional.of(PlayerReference.YOU));
            assertThat(effect.amount()).isEqualTo(new Amount.Exact(3));
        }

        @Test
        @DisplayName("Gain 5 life.")
        void gainFiveLife() {
            var effect = parseGainLife("Gain 5 life.");

            assertThat(effect.player()).isEmpty();
            assertThat(effect.amount()).isEqualTo(new Amount.Exact(5));
        }

        @Test
        @DisplayName("Target player gains 2 life.")
        void targetPlayerGainsTwoLife() {
            var effect = parseGainLife("Target player gains 2 life.");

            assertThat(effect.player()).isEqualTo(Optional.of(PlayerReference.TARGET_PLAYER));
            assertThat(effect.amount()).isEqualTo(new Amount.Exact(2));
        }
    }

    @Nested
    @DisplayName("Lose life")
    class LoseLife {

        @Test
        @DisplayName("Target player loses 3 life.")
        void targetPlayerLosesThreeLife() {
            var effect = parseLoseLife("Target player loses 3 life.");

            assertThat(effect.player()).isEqualTo(Optional.of(PlayerReference.TARGET_PLAYER));
            assertThat(effect.amount()).isEqualTo(new Amount.Exact(3));
        }

        @Test
        @DisplayName("You lose 2 life.")
        void youLoseTwoLife() {
            var effect = parseLoseLife("You lose 2 life.");

            assertThat(effect.player()).isEqualTo(Optional.of(PlayerReference.YOU));
            assertThat(effect.amount()).isEqualTo(new Amount.Exact(2));
        }

        @Test
        @DisplayName("Each opponent loses 1 life.")
        void eachOpponentLosesOneLife() {
            var effect = parseLoseLife("Each opponent loses 1 life.");

            assertThat(effect.player()).isEqualTo(Optional.of(PlayerReference.EACH_OPPONENT));
            assertThat(effect.amount()).isEqualTo(new Amount.Exact(1));
        }
    }
}
