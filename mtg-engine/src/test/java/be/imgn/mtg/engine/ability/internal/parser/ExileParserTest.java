package be.imgn.mtg.engine.ability.internal.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.internal.parser.effect.ExileEffect;
import be.imgn.mtg.engine.ability.internal.parser.reference.PronounType;
import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.ability.internal.parser.selector.NegationType;
import be.imgn.mtg.engine.ability.internal.parser.selector.Qualifier;
import be.imgn.mtg.engine.ability.internal.parser.selector.Quantifier;
import be.imgn.mtg.engine.ability.internal.parser.selector.TypeMatcher;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.parse.CharPredicate;

@DisplayName("ExileParser")
class ExileParserTest {

    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    private ExileEffect parse(String text) {
        return ExileParser.EXILE_EFFECT.parseSkipping(WHITESPACE, text);
    }

    @Nested
    @DisplayName("Basic patterns")
    class BasicPatterns {

        @Test
        @DisplayName("Exile target creature.")
        void exileTargetCreature() {
            var effect = parse("Exile target creature.");

            assertThat(effect.subject()).isInstanceOf(Subject.Select.class);
            var select = (Subject.Select) effect.subject();
            var selector = select.selector();

            assertThat(selector.quantifier()).isEqualTo(new Quantifier.One());
            assertThat(selector.qualifiers()).containsExactly(new Qualifier.Target());
            assertThat(selector.typeMatcher()).isEqualTo(new TypeMatcher.Single(Type.CREATURE));
        }

        @Test
        @DisplayName("Exile all creatures.")
        void exileAllCreatures() {
            var effect = parse("Exile all creatures.");

            assertThat(effect.subject()).isInstanceOf(Subject.Select.class);
            var select = (Subject.Select) effect.subject();
            var selector = select.selector();

            assertThat(selector.quantifier()).isEqualTo(new Quantifier.All());
            assertThat(selector.typeMatcher()).isEqualTo(new TypeMatcher.Single(Type.CREATURE));
        }

        @Test
        @DisplayName("Exile it.")
        void exileIt() {
            var effect = parse("Exile it.");

            assertThat(effect.subject()).isEqualTo(new Subject.Pronoun(PronounType.IT));
        }
    }

    @Nested
    @DisplayName("With qualifiers")
    class WithQualifiers {

        @Test
        @DisplayName("Exile target nonblack creature.")
        void exileTargetNonblackCreature() {
            var effect = parse("Exile target nonblack creature.");

            var select = (Subject.Select) effect.subject();
            var selector = select.selector();

            assertThat(selector.qualifiers())
                    .containsExactly(new Qualifier.Target(), new Qualifier.Negation(NegationType.BLACK));
            assertThat(selector.typeMatcher()).isEqualTo(new TypeMatcher.Single(Type.CREATURE));
        }
    }
}
