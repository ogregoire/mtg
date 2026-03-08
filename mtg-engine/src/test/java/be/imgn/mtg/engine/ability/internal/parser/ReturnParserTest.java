package be.imgn.mtg.engine.ability.internal.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.internal.parser.reference.PronounType;
import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.ability.internal.parser.selector.LibraryPosition;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.effect.PutOnLibraryEffect;
import be.imgn.mtg.engine.effect.ReturnToHandEffect;
import be.imgn.mtg.engine.selector.ObjectSelector;
import be.imgn.mtg.engine.selector.Qualifier;
import be.imgn.mtg.engine.selector.Quantifier;
import be.imgn.mtg.engine.selector.TypeMatcher;

@DisplayName("ReturnParser")
class ReturnParserTest {

    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    private ReturnToHandEffect parseReturnToHand(String text) {
        return ReturnParser.RETURN_TO_HAND_EFFECT.parseSkipping(WHITESPACE, text);
    }

    private PutOnLibraryEffect parsePutOnLibrary(String text) {
        return ReturnParser.PUT_ON_LIBRARY_EFFECT.parseSkipping(WHITESPACE, text);
    }

    @Nested
    @DisplayName("Return to hand")
    class ReturnToHand {

        @Test
        @DisplayName("Return target creature to its owner's hand.")
        void returnTargetCreatureToItsOwnersHand() {
            var effect = parseReturnToHand("Return target creature to its owner's hand.");

            assertThat(effect.subject()).isInstanceOf(Subject.Select.class);
            var select = (Subject.Select) effect.subject();
            var selector = (ObjectSelector) select.selector();

            assertThat(selector.quantifier()).isEqualTo(new Quantifier.One());
            assertThat(selector.qualifiers()).containsExactly(new Qualifier.Target());
            assertThat(selector.typeMatcher()).isEqualTo(new TypeMatcher.Single(Type.CREATURE));
        }

        @Test
        @DisplayName("Return it to its owner's hand.")
        void returnItToItsOwnersHand() {
            var effect = parseReturnToHand("Return it to its owner's hand.");

            assertThat(effect.subject()).isEqualTo(new Subject.Pronoun(PronounType.IT));
        }

        @Test
        @DisplayName("Return all creatures to their owners' hands.")
        void returnAllCreaturesToTheirOwnersHands() {
            var effect = parseReturnToHand("Return all creatures to their owners' hands.");

            var select = (Subject.Select) effect.subject();
            var selector = (ObjectSelector) select.selector();

            assertThat(selector.quantifier()).isEqualTo(new Quantifier.All());
            assertThat(selector.typeMatcher()).isEqualTo(new TypeMatcher.Single(Type.CREATURE));
        }
    }

    @Nested
    @DisplayName("Put on library")
    class PutOnLibrary {

        @Test
        @DisplayName("Put target creature on the bottom of its owner's library.")
        void putTargetCreatureOnBottomOfLibrary() {
            var effect = parsePutOnLibrary("Put target creature on the bottom of its owner's library.");

            assertThat(effect.position()).isEqualTo(LibraryPosition.BOTTOM);
            var select = (Subject.Select) effect.subject();
            var selector = (ObjectSelector) select.selector();

            assertThat(selector.qualifiers()).containsExactly(new Qualifier.Target());
            assertThat(selector.typeMatcher()).isEqualTo(new TypeMatcher.Single(Type.CREATURE));
        }

        @Test
        @DisplayName("Put target creature on the top of its owner's library.")
        void putTargetCreatureOnTopOfLibrary() {
            var effect = parsePutOnLibrary("Put target creature on the top of its owner's library.");

            assertThat(effect.position()).isEqualTo(LibraryPosition.TOP);
        }

        @Test
        @DisplayName("Put it on the bottom of its owner's library.")
        void putItOnBottomOfLibrary() {
            var effect = parsePutOnLibrary("Put it on the bottom of its owner's library.");

            assertThat(effect.subject()).isEqualTo(new Subject.Pronoun(PronounType.IT));
            assertThat(effect.position()).isEqualTo(LibraryPosition.BOTTOM);
        }
    }
}
