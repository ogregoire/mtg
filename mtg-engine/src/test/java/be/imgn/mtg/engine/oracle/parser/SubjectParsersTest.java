package be.imgn.mtg.engine.oracle.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.oracle.domain.GameObjectType;
import be.imgn.mtg.engine.oracle.domain.PronounType;
import be.imgn.mtg.engine.oracle.domain.Selector;
import be.imgn.mtg.engine.oracle.domain.Subject;

class SubjectParsersTest {

    private static final CharPredicate SPACE = CharPredicate.is(' ');

    // ── Player references ──────────────────────────────────────────────────

    @Nested
    class PlayerRefParser {

        @Test
        void parsesYou() {
            var result = SubjectParsers.PLAYER_REF.parseSkipping(SPACE, "you");
            assertThat(result).isEqualTo(Subject.PlayerRef.YOU);
        }

        @Test
        void parsesYouTitleCase() {
            var result = SubjectParsers.PLAYER_REF.parseSkipping(SPACE, "You");
            assertThat(result).isEqualTo(Subject.PlayerRef.YOU);
        }

        @Test
        void parsesTargetPlayer() {
            var result = SubjectParsers.PLAYER_REF.parseSkipping(SPACE, "target player");
            assertThat(result).isEqualTo(Subject.PlayerRef.TARGET_PLAYER);
        }

        @Test
        void parsesTargetOpponent() {
            var result = SubjectParsers.PLAYER_REF.parseSkipping(SPACE, "target opponent");
            assertThat(result).isEqualTo(Subject.PlayerRef.TARGET_OPPONENT);
        }

        @Test
        void parsesEachOpponent() {
            var result = SubjectParsers.PLAYER_REF.parseSkipping(SPACE, "each opponent");
            assertThat(result).isEqualTo(Subject.PlayerRef.EACH_OPPONENT);
        }

        @Test
        void parsesEachPlayer() {
            var result = SubjectParsers.PLAYER_REF.parseSkipping(SPACE, "each player");
            assertThat(result).isEqualTo(Subject.PlayerRef.EACH_PLAYER);
        }

        @Test
        void parsesThatPlayer() {
            var result = SubjectParsers.PLAYER_REF.parseSkipping(SPACE, "that player");
            assertThat(result).isEqualTo(Subject.PlayerRef.THAT_PLAYER);
        }

        @Test
        void parsesDefendingPlayer() {
            var result = SubjectParsers.PLAYER_REF.parseSkipping(SPACE, "defending player");
            assertThat(result).isEqualTo(Subject.PlayerRef.DEFENDING_PLAYER);
        }

        @Test
        void parsesThey() {
            var result = SubjectParsers.PLAYER_REF.parseSkipping(SPACE, "they");
            assertThat(result).isEqualTo(Subject.PlayerRef.THEY);
        }
    }

    // ── Subject ────────────────────────────────────────────────────────────

    @Nested
    class SubjectParser {

        @Test
        void parsesTildeAsSelfRef() {
            var result = SubjectParsers.SUBJECT.parseSkipping(SPACE, "~");
            assertThat(result).isInstanceOf(Subject.SelfRef.class);
            var selfRef = (Subject.SelfRef) result;
            assertThat(selfRef.type()).isNull();
        }

        @Test
        void parsesThisCreatureAsSelfRef() {
            var result = SubjectParsers.SUBJECT.parseSkipping(SPACE, "this creature");
            assertThat(result).isInstanceOf(Subject.SelfRef.class);
            var selfRef = (Subject.SelfRef) result;
            assertThat(selfRef.type()).isEqualTo("creature");
        }

        @Test
        void parsesItAsPronoun() {
            var result = SubjectParsers.SUBJECT.parseSkipping(SPACE, "it");
            assertThat(result).isEqualTo(new Subject.Pronoun(PronounType.IT));
        }

        @Test
        void parsesThemAsPronoun() {
            var result = SubjectParsers.SUBJECT.parseSkipping(SPACE, "them");
            assertThat(result).isEqualTo(new Subject.Pronoun(PronounType.THEM));
        }

        @Test
        void parsesAnyTarget() {
            var result = SubjectParsers.SUBJECT.parseSkipping(SPACE, "any target");
            assertThat(result).isInstanceOf(Subject.AnyTarget.class);
        }

        @Test
        void parsesYouAsPlayerSubject() {
            var result = SubjectParsers.SUBJECT.parseSkipping(SPACE, "you");
            assertThat(result).isInstanceOf(Subject.Player.class);
            var player = (Subject.Player) result;
            assertThat(player.ref()).isEqualTo(Subject.PlayerRef.YOU);
        }

        @Test
        void parsesTargetPlayerAsPlayerSubject() {
            var result = SubjectParsers.SUBJECT.parseSkipping(SPACE, "target player");
            assertThat(result).isInstanceOf(Subject.Player.class);
            var player = (Subject.Player) result;
            assertThat(player.ref()).isEqualTo(Subject.PlayerRef.TARGET_PLAYER);
        }

        @Test
        void parsesTargetCreatureAsSelectSubject() {
            var result = SubjectParsers.SUBJECT.parseSkipping(SPACE, "target creature");
            assertThat(result).isInstanceOf(Subject.Select.class);
            var select = (Subject.Select) result;
            assertThat(select.selector().head()).isEqualTo(GameObjectType.PERMANENT);
            assertThat(select.selector().qualifiers()).contains(Selector.Qualifier.TARGET);
        }

        @Test
        void parsesItsController() {
            var result = SubjectParsers.SUBJECT.parseSkipping(SPACE, "its controller");
            assertThat(result).isInstanceOf(Subject.PossessiveSubject.class);
            var poss = (Subject.PossessiveSubject) result;
            assertThat(poss.possessive()).isEqualTo("its");
            assertThat(poss.role()).isEqualTo("controller");
        }

        @Test
        void parsesItsOwner() {
            var result = SubjectParsers.SUBJECT.parseSkipping(SPACE, "its owner");
            assertThat(result).isInstanceOf(Subject.PossessiveSubject.class);
            var poss = (Subject.PossessiveSubject) result;
            assertThat(poss.possessive()).isEqualTo("its");
            assertThat(poss.role()).isEqualTo("owner");
        }

        @Test
        void parsesThatCreatureAsDemonstrative() {
            var result = SubjectParsers.SUBJECT.parseSkipping(SPACE, "that creature");
            assertThat(result).isInstanceOf(Subject.Demonstrative.class);
            var dem = (Subject.Demonstrative) result;
            assertThat(dem.determiner()).isEqualTo("that");
        }
    }
}
