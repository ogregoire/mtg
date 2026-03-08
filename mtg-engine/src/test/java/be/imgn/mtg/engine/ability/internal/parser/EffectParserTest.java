package be.imgn.mtg.engine.ability.internal.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.effect.CounterSpellEffect;
import be.imgn.mtg.engine.effect.CreateTokenEffect;
import be.imgn.mtg.engine.effect.DealDamageEffect;
import be.imgn.mtg.engine.effect.DestroyEffect;
import be.imgn.mtg.engine.effect.DiscardEffect;
import be.imgn.mtg.engine.effect.DrawEffect;
import be.imgn.mtg.engine.effect.ExileEffect;
import be.imgn.mtg.engine.effect.FightEffect;
import be.imgn.mtg.engine.effect.GainAbilityEffect;
import be.imgn.mtg.engine.effect.GainControlEffect;
import be.imgn.mtg.engine.effect.GainLifeEffect;
import be.imgn.mtg.engine.effect.MillEffect;
import be.imgn.mtg.engine.effect.ScryEffect;
import be.imgn.mtg.engine.effect.TapEffect;
import be.imgn.mtg.engine.mana.AddManaEffect;
import be.imgn.mtg.engine.mana.ManaType;
import be.imgn.mtg.engine.selector.ObjectSelector;
import be.imgn.mtg.engine.selector.Qualifier;
import be.imgn.mtg.engine.selector.TypeMatcher;

@DisplayName("EffectParser")
class EffectParserTest {

    @Nested
    @DisplayName("Master parser handles all effect types")
    class MasterParser {

        @Test
        @DisplayName("Parses destroy effects")
        void parsesDestroyEffects() {
            var effect = EffectParser.parse("Destroy target creature.");

            assertThat(effect).isInstanceOf(DestroyEffect.class);
        }

        @Test
        @DisplayName("Parses exile effects")
        void parsesExileEffects() {
            var effect = EffectParser.parse("Exile target creature.");

            assertThat(effect).isInstanceOf(ExileEffect.class);
        }

        @Test
        @DisplayName("Parses damage effects")
        void parsesDamageEffects() {
            var effect = EffectParser.parse("Deal 3 damage to target creature.");

            assertThat(effect).isInstanceOf(DealDamageEffect.class);
        }

        @Test
        @DisplayName("Parses gain life effects")
        void parsesGainLifeEffects() {
            var effect = EffectParser.parse("You gain 3 life.");

            assertThat(effect).isInstanceOf(GainLifeEffect.class);
        }

        @Test
        @DisplayName("Parses draw effects")
        void parsesDrawEffects() {
            var effect = EffectParser.parse("Draw two cards.");

            assertThat(effect).isInstanceOf(DrawEffect.class);
        }

        @Test
        @DisplayName("Parses discard effects")
        void parsesDiscardEffects() {
            var effect = EffectParser.parse("Discard a card.");

            assertThat(effect).isInstanceOf(DiscardEffect.class);
        }

        @Test
        @DisplayName("Parses scry effects")
        void parsesScryEffects() {
            var effect = EffectParser.parse("Scry 2.");

            assertThat(effect).isInstanceOf(ScryEffect.class);
        }

        @Test
        @DisplayName("Parses tap effects")
        void parsesTapEffects() {
            var effect = EffectParser.parse("Tap target creature.");

            assertThat(effect).isInstanceOf(TapEffect.class);
        }

        @Test
        @DisplayName("Parses mill effects")
        void parsesMillEffects() {
            var effect = EffectParser.parse("Mill three cards.");

            assertThat(effect).isInstanceOf(MillEffect.class);
        }

        @Test
        @DisplayName("Parses gain control effects")
        void parsesGainControlEffects() {
            var effect = EffectParser.parse("Gain control of target creature.");

            assertThat(effect).isInstanceOf(GainControlEffect.class);
            var gainControl = (GainControlEffect) effect;
            var select = (Subject.Select) gainControl.subject();
            assertThat(((ObjectSelector) select.selector()).qualifiers()).containsExactly(new Qualifier.Target());
            assertThat(((ObjectSelector) select.selector()).typeMatcher())
                    .isEqualTo(new TypeMatcher.Single(Type.CREATURE));
        }

        @Test
        @DisplayName("Parses counter spell effects")
        void parsesCounterSpellEffects() {
            var effect = EffectParser.parse("Counter target spell.");

            assertThat(effect).isInstanceOf(CounterSpellEffect.class);
        }

        @Test
        @DisplayName("Parses fight effects")
        void parsesFightEffects() {
            var effect = EffectParser.parse("target creature you control fights target creature.");

            assertThat(effect).isInstanceOf(FightEffect.class);
        }

        @Test
        @DisplayName("Parses add mana effects")
        void parsesAddManaEffects() {
            var effect = EffectParser.parse("Add {G}{G}.");

            assertThat(effect).isInstanceOf(AddManaEffect.Exact.class);
            var addMana = (AddManaEffect.Exact) effect;
            assertThat(addMana.mana()).containsExactly(ManaType.GREEN, ManaType.GREEN);
        }

        @Test
        @DisplayName("Parses create token effects")
        void parsesCreateTokenEffects() {
            var effect = EffectParser.parse("Create a 1/1 white Soldier creature token.");

            assertThat(effect).isInstanceOf(CreateTokenEffect.class);
        }

        @Test
        @DisplayName("Parses gain ability effects")
        void parsesGainAbilityEffects() {
            var effect = EffectParser.parse("target creature gains flying until end of turn.");

            assertThat(effect).isInstanceOf(GainAbilityEffect.class);
        }
    }
}
