package be.imgn.mtg.engine.ability.internal.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.internal.parser.effect.SacrificeEffect;
import be.imgn.mtg.engine.ability.internal.parser.reference.PronounType;
import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.selector.ObjectSelector;
import be.imgn.mtg.engine.selector.Qualifier;
import be.imgn.mtg.engine.selector.Quantifier;
import be.imgn.mtg.engine.selector.TypeMatcher;

@DisplayName("SacrificeParser")
class SacrificeParserTest {

    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    private SacrificeEffect parse(String text) {
        return SacrificeParser.SACRIFICE_EFFECT.parseSkipping(WHITESPACE, text);
    }

    @Nested
    @DisplayName("Basic patterns")
    class BasicPatterns {

        @Test
        @DisplayName("Sacrifice a creature.")
        void sacrificeACreature() {
            var effect = parse("Sacrifice a creature.");

            assertThat(effect.subject()).isInstanceOf(Subject.Select.class);
            var select = (Subject.Select) effect.subject();
            var selector = (ObjectSelector) select.selector();

            assertThat(selector.quantifier()).isEqualTo(new Quantifier.One());
            assertThat(selector.qualifiers()).isEmpty();
            assertThat(selector.typeMatcher()).isEqualTo(new TypeMatcher.Single(Type.CREATURE));
        }

        @Test
        @DisplayName("Sacrifice target permanent.")
        void sacrificeTargetPermanent() {
            var effect = parse("Sacrifice target permanent.");

            var select = (Subject.Select) effect.subject();
            var selector = (ObjectSelector) select.selector();

            assertThat(selector.qualifiers()).containsExactly(new Qualifier.Target());
            assertThat(selector.typeMatcher()).isEqualTo(new TypeMatcher.Permanent());
        }

        @Test
        @DisplayName("Sacrifice it.")
        void sacrificeIt() {
            var effect = parse("Sacrifice it.");

            assertThat(effect.subject()).isEqualTo(new Subject.Pronoun(PronounType.IT));
        }
    }
}
