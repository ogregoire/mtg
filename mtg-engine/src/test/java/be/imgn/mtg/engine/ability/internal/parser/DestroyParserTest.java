package be.imgn.mtg.engine.ability.internal.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.internal.parser.reference.ControllerClause;
import be.imgn.mtg.engine.ability.internal.parser.reference.PlayerReference;
import be.imgn.mtg.engine.ability.internal.parser.reference.PronounType;
import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.ability.internal.parser.selector.Comparison;
import be.imgn.mtg.engine.ability.internal.parser.selector.NegationType;
import be.imgn.mtg.engine.ability.internal.parser.selector.Qualifier;
import be.imgn.mtg.engine.ability.internal.parser.selector.Quantifier;
import be.imgn.mtg.engine.ability.internal.parser.selector.TypeMatcher;
import be.imgn.mtg.engine.ability.internal.parser.selector.WithClause;
import be.imgn.mtg.engine.characteristics.Type;

@DisplayName("DestroyParser")
class DestroyParserTest {

    @Nested
    @DisplayName("Basic patterns")
    class BasicPatterns {

        @Test
        @DisplayName("Destroy target creature.")
        void destroyTargetCreature() {
            var effect = DestroyParser.parse("Destroy target creature.");

            assertThat(effect.subject()).isInstanceOf(Subject.Select.class);
            var select = (Subject.Select) effect.subject();
            var selector = select.selector();

            assertThat(selector.quantifier()).isEqualTo(new Quantifier.One());
            assertThat(selector.qualifiers()).containsExactly(new Qualifier.Target());
            assertThat(selector.typeMatcher()).isEqualTo(new TypeMatcher.Single(Type.CREATURE));
            assertThat(selector.withClauses()).isEmpty();
            assertThat(selector.controller()).isEmpty();
        }

        @Test
        @DisplayName("Destroy all creatures.")
        void destroyAllCreatures() {
            var effect = DestroyParser.parse("Destroy all creatures.");

            assertThat(effect.subject()).isInstanceOf(Subject.Select.class);
            var select = (Subject.Select) effect.subject();
            var selector = select.selector();

            assertThat(selector.quantifier()).isEqualTo(new Quantifier.All());
            assertThat(selector.qualifiers()).isEmpty();
            assertThat(selector.typeMatcher()).isEqualTo(new TypeMatcher.Single(Type.CREATURE));
        }

        @Test
        @DisplayName("Destroy target artifact or enchantment.")
        void destroyTargetArtifactOrEnchantment() {
            var effect = DestroyParser.parse("Destroy target artifact or enchantment.");

            assertThat(effect.subject()).isInstanceOf(Subject.Select.class);
            var select = (Subject.Select) effect.subject();
            var selector = select.selector();

            assertThat(selector.quantifier()).isEqualTo(new Quantifier.One());
            assertThat(selector.qualifiers()).containsExactly(new Qualifier.Target());
            assertThat(selector.typeMatcher())
                    .isEqualTo(new TypeMatcher.Or(
                            List.of(new TypeMatcher.Single(Type.ARTIFACT), new TypeMatcher.Single(Type.ENCHANTMENT))));
        }
    }

    @Nested
    @DisplayName("With clauses")
    class WithClauses {

        @Test
        @DisplayName("Destroy target nonland permanent with mana value 3 or less.")
        void destroyTargetNonlandPermanentWithManaValue() {
            var effect = DestroyParser.parse("Destroy target nonland permanent with mana value 3 or less.");

            assertThat(effect.subject()).isInstanceOf(Subject.Select.class);
            var select = (Subject.Select) effect.subject();
            var selector = select.selector();

            assertThat(selector.quantifier()).isEqualTo(new Quantifier.One());
            assertThat(selector.qualifiers())
                    .containsExactly(new Qualifier.Target(), new Qualifier.Negation(NegationType.LAND));
            assertThat(selector.typeMatcher()).isEqualTo(new TypeMatcher.AnyPermanent());
            assertThat(selector.withClauses()).containsExactly(new WithClause.ManaValue(Comparison.LESS_OR_EQUAL, 3));
        }
    }

    @Nested
    @DisplayName("Quantifiers")
    class Quantifiers {

        @Test
        @DisplayName("Destroy up to one target artifact.")
        void destroyUpToOneTargetArtifact() {
            var effect = DestroyParser.parse("Destroy up to one target artifact.");

            assertThat(effect.subject()).isInstanceOf(Subject.Select.class);
            var select = (Subject.Select) effect.subject();
            var selector = select.selector();

            assertThat(selector.quantifier()).isEqualTo(new Quantifier.UpTo(1));
            assertThat(selector.qualifiers()).containsExactly(new Qualifier.Target());
            assertThat(selector.typeMatcher()).isEqualTo(new TypeMatcher.Single(Type.ARTIFACT));
        }

        @Test
        @DisplayName("Destroy each creature.")
        void destroyEachCreature() {
            var effect = DestroyParser.parse("Destroy each creature.");

            assertThat(effect.subject()).isInstanceOf(Subject.Select.class);
            var select = (Subject.Select) effect.subject();
            var selector = select.selector();

            assertThat(selector.quantifier()).isEqualTo(new Quantifier.Each());
            assertThat(selector.typeMatcher()).isEqualTo(new TypeMatcher.Single(Type.CREATURE));
        }
    }

    @Nested
    @DisplayName("Controller clauses")
    class ControllerClauses {

        @Test
        @DisplayName("Destroy target creature that player controls.")
        void destroyTargetCreatureThatPlayerControls() {
            var effect = DestroyParser.parse("Destroy target creature that player controls.");

            assertThat(effect.subject()).isInstanceOf(Subject.Select.class);
            var select = (Subject.Select) effect.subject();
            var selector = select.selector();

            assertThat(selector.quantifier()).isEqualTo(new Quantifier.One());
            assertThat(selector.qualifiers()).containsExactly(new Qualifier.Target());
            assertThat(selector.typeMatcher()).isEqualTo(new TypeMatcher.Single(Type.CREATURE));
            assertThat(selector.controller()).isEqualTo(Optional.of(new ControllerClause(PlayerReference.THAT_PLAYER)));
        }

        @Test
        @DisplayName("Destroy all creatures you control.")
        void destroyAllCreaturesYouControl() {
            var effect = DestroyParser.parse("Destroy all creatures you control.");

            var select = (Subject.Select) effect.subject();
            var selector = select.selector();

            assertThat(selector.quantifier()).isEqualTo(new Quantifier.All());
            assertThat(selector.typeMatcher()).isEqualTo(new TypeMatcher.Single(Type.CREATURE));
            assertThat(selector.controller()).isEqualTo(Optional.of(new ControllerClause(PlayerReference.YOU)));
        }
    }

    @Nested
    @DisplayName("Pronoun references")
    class PronounReferences {

        @Test
        @DisplayName("Destroy it.")
        void destroyIt() {
            var effect = DestroyParser.parse("Destroy it.");

            assertThat(effect.subject()).isEqualTo(new Subject.Pronoun(PronounType.IT));
        }

        @Test
        @DisplayName("Destroy them.")
        void destroyThem() {
            var effect = DestroyParser.parse("Destroy them.");

            assertThat(effect.subject()).isEqualTo(new Subject.Pronoun(PronounType.THEM));
        }
    }

    @Nested
    @DisplayName("That object references")
    class ThatObjectReferences {

        @Test
        @DisplayName("Destroy that creature.")
        void destroyThatCreature() {
            var effect = DestroyParser.parse("Destroy that creature.");

            assertThat(effect.subject())
                    .isEqualTo(new Subject.ThatObject(Optional.of(new TypeMatcher.Single(Type.CREATURE))));
        }

        @Test
        @DisplayName("Destroy that permanent.")
        void destroyThatPermanent() {
            var effect = DestroyParser.parse("Destroy that permanent.");

            assertThat(effect.subject()).isEqualTo(new Subject.ThatObject(Optional.of(new TypeMatcher.AnyPermanent())));
        }
    }

    @Nested
    @DisplayName("Without trailing period")
    class WithoutTrailingPeriod {

        @Test
        @DisplayName("Destroy target creature (without period)")
        void destroyTargetCreatureNoPeriod() {
            var effect = DestroyParser.parse("Destroy target creature");

            assertThat(effect.subject()).isInstanceOf(Subject.Select.class);
            var select = (Subject.Select) effect.subject();
            assertThat(select.selector().typeMatcher()).isEqualTo(new TypeMatcher.Single(Type.CREATURE));
        }
    }
}
