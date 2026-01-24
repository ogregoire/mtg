package be.imgn.mtg.engine.ability.internal.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.internal.parser.effect.TapEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.UntapEffect;
import be.imgn.mtg.engine.ability.internal.parser.reference.PronounType;
import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.ability.internal.parser.selector.Qualifier;
import be.imgn.mtg.engine.ability.internal.parser.selector.Quantifier;
import be.imgn.mtg.engine.ability.internal.parser.selector.TypeMatcher;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.parse.CharPredicate;

@DisplayName("TapParser")
class TapParserTest {

    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    private TapEffect parseTap(String text) {
        return TapParser.TAP_EFFECT.parseSkipping(WHITESPACE, text);
    }

    private UntapEffect parseUntap(String text) {
        return TapParser.UNTAP_EFFECT.parseSkipping(WHITESPACE, text);
    }

    @Nested
    @DisplayName("Tap")
    class Tap {

        @Test
        @DisplayName("Tap target creature.")
        void tapTargetCreature() {
            var effect = parseTap("Tap target creature.");

            var select = (Subject.Select) effect.subject();
            var selector = select.selector();

            assertThat(selector.qualifiers()).containsExactly(new Qualifier.Target());
            assertThat(selector.typeMatcher()).isEqualTo(new TypeMatcher.Single(Type.CREATURE));
        }

        @Test
        @DisplayName("Tap all creatures.")
        void tapAllCreatures() {
            var effect = parseTap("Tap all creatures.");

            var select = (Subject.Select) effect.subject();
            assertThat(select.selector().quantifier()).isEqualTo(new Quantifier.All());
        }

        @Test
        @DisplayName("Tap it.")
        void tapIt() {
            var effect = parseTap("Tap it.");

            assertThat(effect.subject()).isEqualTo(new Subject.Pronoun(PronounType.IT));
        }
    }

    @Nested
    @DisplayName("Untap")
    class Untap {

        @Test
        @DisplayName("Untap target creature.")
        void untapTargetCreature() {
            var effect = parseUntap("Untap target creature.");

            var select = (Subject.Select) effect.subject();
            var selector = select.selector();

            assertThat(selector.qualifiers()).containsExactly(new Qualifier.Target());
            assertThat(selector.typeMatcher()).isEqualTo(new TypeMatcher.Single(Type.CREATURE));
        }

        @Test
        @DisplayName("Untap target permanent.")
        void untapTargetPermanent() {
            var effect = parseUntap("Untap target permanent.");

            var select = (Subject.Select) effect.subject();
            assertThat(select.selector().typeMatcher()).isEqualTo(new TypeMatcher.Permanent());
        }

        @Test
        @DisplayName("Untap it.")
        void untapIt() {
            var effect = parseUntap("Untap it.");

            assertThat(effect.subject()).isEqualTo(new Subject.Pronoun(PronounType.IT));
        }
    }
}
