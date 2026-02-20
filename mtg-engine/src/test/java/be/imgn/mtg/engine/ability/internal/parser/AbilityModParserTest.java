package be.imgn.mtg.engine.ability.internal.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.internal.parser.effect.GainAbilityEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.ModifyPowerToughnessEffect;
import be.imgn.mtg.engine.ability.internal.parser.reference.PronounType;
import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.ability.internal.parser.selector.Duration;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.selector.ObjectSelector;
import be.imgn.mtg.engine.selector.Qualifier;
import be.imgn.mtg.engine.selector.TypeMatcher;
import be.imgn.mtg.parse.CharPredicate;

@DisplayName("AbilityModParser")
class AbilityModParserTest {

    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    private GainAbilityEffect parseGainAbility(String text) {
        return AbilityModParser.GAIN_ABILITY_EFFECT.parseSkipping(WHITESPACE, text);
    }

    private ModifyPowerToughnessEffect parseModifyPT(String text) {
        return AbilityModParser.MODIFY_PT_EFFECT.parseSkipping(WHITESPACE, text);
    }

    @Nested
    @DisplayName("Gain ability")
    class GainAbility {

        @Test
        @DisplayName("target creature gains flying until end of turn.")
        void targetCreatureGainsFlying() {
            var effect = parseGainAbility("target creature gains flying until end of turn.");

            var select = (Subject.Select) effect.subject();
            assertThat(((ObjectSelector) select.selector()).qualifiers()).containsExactly(new Qualifier.Target());
            assertThat(((ObjectSelector) select.selector()).typeMatcher())
                    .isEqualTo(new TypeMatcher.Single(Type.CREATURE));
            assertThat(effect.ability()).isEqualTo("flying");
            assertThat(effect.duration()).isEqualTo(Optional.of(new Duration.UntilEndOfTurn()));
        }

        @Test
        @DisplayName("it gains haste.")
        void itGainsHaste() {
            var effect = parseGainAbility("it gains haste.");

            assertThat(effect.subject()).isEqualTo(new Subject.Pronoun(PronounType.IT));
            assertThat(effect.ability()).isEqualTo("haste");
            assertThat(effect.duration()).isEmpty();
        }

        @Test
        @DisplayName("target creature gains hexproof until your next turn.")
        void targetCreatureGainsHexproof() {
            var effect = parseGainAbility("target creature gains hexproof until your next turn.");

            assertThat(effect.ability()).isEqualTo("hexproof");
            assertThat(effect.duration()).isEqualTo(Optional.of(new Duration.UntilYourNextTurn()));
        }

        @Test
        @DisplayName("target creature gains indestructible.")
        void gainsIndestructible() {
            var effect = parseGainAbility("target creature gains indestructible.");

            assertThat(effect.ability()).isEqualTo("indestructible");
            assertThat(effect.duration()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Modify power/toughness")
    class ModifyPT {

        @Test
        @DisplayName("target creature gets +2/+2 until end of turn.")
        void targetCreatureGetsPlusTwoPlusTwo() {
            var effect = parseModifyPT("target creature gets +2/+2 until end of turn.");

            var select = (Subject.Select) effect.subject();
            assertThat(((ObjectSelector) select.selector()).qualifiers()).containsExactly(new Qualifier.Target());
            assertThat(effect.powerMod()).isEqualTo(2);
            assertThat(effect.toughnessMod()).isEqualTo(2);
            assertThat(effect.duration()).isEqualTo(Optional.of(new Duration.UntilEndOfTurn()));
        }

        @Test
        @DisplayName("target creature gets -3/-3 until end of turn.")
        void targetCreatureGetsMinusThree() {
            var effect = parseModifyPT("target creature gets -3/-3 until end of turn.");

            assertThat(effect.powerMod()).isEqualTo(-3);
            assertThat(effect.toughnessMod()).isEqualTo(-3);
        }

        @Test
        @DisplayName("it gets +3/+0 until end of turn.")
        void itGetsPlusThreePlusZero() {
            var effect = parseModifyPT("it gets +3/+0 until end of turn.");

            assertThat(effect.subject()).isEqualTo(new Subject.Pronoun(PronounType.IT));
            assertThat(effect.powerMod()).isEqualTo(3);
            assertThat(effect.toughnessMod()).isEqualTo(0);
        }

        @Test
        @DisplayName("target creature gets +1/+1.")
        void targetCreatureGetsPermanentBoost() {
            var effect = parseModifyPT("target creature gets +1/+1.");

            assertThat(effect.powerMod()).isEqualTo(1);
            assertThat(effect.toughnessMod()).isEqualTo(1);
            assertThat(effect.duration()).isEmpty();
        }
    }
}
