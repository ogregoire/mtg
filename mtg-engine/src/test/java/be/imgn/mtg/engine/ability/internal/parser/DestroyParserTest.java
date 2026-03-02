package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.engine.ability.internal.parser.assertions.DestroyEffectAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.internal.parser.reference.PronounType;
import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.selector.Comparison;
import be.imgn.mtg.engine.selector.ControllerClause;
import be.imgn.mtg.engine.selector.ObjectSelector;
import be.imgn.mtg.engine.selector.PlayerReference;
import be.imgn.mtg.engine.selector.Qualifier;
import be.imgn.mtg.engine.selector.Quantifier;
import be.imgn.mtg.engine.selector.Trait;
import be.imgn.mtg.engine.selector.TypeMatcher;
import be.imgn.mtg.engine.selector.WithClause;

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
            var selector = (ObjectSelector) select.selector();

            assertThat(selector.quantifier()).isEqualTo(new Quantifier.One());
            assertThat(selector.qualifiers()).containsExactly(new Qualifier.Target());
            assertThat(selector.typeMatcher()).isEqualTo(new TypeMatcher.Single(Type.CREATURE));
            assertThat(selector.withClauses()).isEmpty();
            assertThat(selector.controller()).isNull();
        }

        @Test
        @DisplayName("Destroy all creatures.")
        void destroyAllCreatures() {
            var effect = DestroyParser.parse("Destroy all creatures.");

            assertThat(effect.subject()).isInstanceOf(Subject.Select.class);
            var select = (Subject.Select) effect.subject();
            var selector = (ObjectSelector) select.selector();

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
            var selector = (ObjectSelector) select.selector();

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
            var selector = (ObjectSelector) select.selector();

            assertThat(selector.quantifier()).isEqualTo(new Quantifier.One());
            assertThat(selector.qualifiers())
                    .containsExactly(new Qualifier.Target(), new Qualifier.Not(new Trait.CardType(Type.LAND)));
            assertThat(selector.typeMatcher()).isEqualTo(new TypeMatcher.Permanent());
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
            var selector = (ObjectSelector) select.selector();

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
            var selector = (ObjectSelector) select.selector();

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
            var selector = (ObjectSelector) select.selector();

            assertThat(selector.quantifier()).isEqualTo(new Quantifier.One());
            assertThat(selector.qualifiers()).containsExactly(new Qualifier.Target());
            assertThat(selector.typeMatcher()).isEqualTo(new TypeMatcher.Single(Type.CREATURE));
            assertThat(selector.controller()).isEqualTo(new ControllerClause(PlayerReference.THAT_PLAYER));
        }

        @Test
        @DisplayName("Destroy all creatures you control.")
        void destroyAllCreaturesYouControl() {
            var effect = DestroyParser.parse("Destroy all creatures you control.");

            var select = (Subject.Select) effect.subject();
            var selector = (ObjectSelector) select.selector();

            assertThat(selector.quantifier()).isEqualTo(new Quantifier.All());
            assertThat(selector.typeMatcher()).isEqualTo(new TypeMatcher.Single(Type.CREATURE));
            assertThat(selector.controller()).isEqualTo(new ControllerClause(PlayerReference.YOU));
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

            assertThat(effect.subject()).isEqualTo(new Subject.ThatObject(Optional.of(new TypeMatcher.Permanent())));
        }
    }

    @Nested
    @DisplayName("Can't be regenerated")
    class CantBeRegenerated {

        @Test
        @DisplayName("Can be regenerated by default")
        void canBeRegeneratedByDefault() {
            var effect = DestroyParser.parse("Destroy target creature.");

            assertThat(effect).canBeRegenerated();
        }

        @Test
        @DisplayName("Destroy target creature. It can't be regenerated.")
        void itCantBeRegenerated() {
            var effect = DestroyParser.parse("Destroy target creature. It can't be regenerated.");

            assertThat(effect).cannotBeRegenerated();
        }

        @Test
        @DisplayName("Destroy all creatures. They can't be regenerated.")
        void theyCantBeRegenerated() {
            var effect = DestroyParser.parse("Destroy all creatures. They can't be regenerated.");

            assertThat(effect).cannotBeRegenerated();
        }

        @Test
        @DisplayName("Destroy target creature. That creature can't be regenerated.")
        void thatCreatureCantBeRegenerated() {
            var effect = DestroyParser.parse("Destroy target creature. That creature can't be regenerated.");

            assertThat(effect).cannotBeRegenerated();
        }

        @Test
        @DisplayName("Destroy target creature. The creature can't be regenerated.")
        void theCreatureCantBeRegenerated() {
            var effect = DestroyParser.parse("Destroy target creature. The creature can't be regenerated.");

            assertThat(effect).cannotBeRegenerated();
        }

        @Test
        @DisplayName("Destroy all creatures. Those creatures can't be regenerated.")
        void thoseCreaturesCantBeRegenerated() {
            var effect = DestroyParser.parse("Destroy all creatures. Those creatures can't be regenerated.");

            assertThat(effect).cannotBeRegenerated();
        }

        @Test
        @DisplayName("Destroy target creature. A creature destroyed this way can't be regenerated.")
        void aCreatureDestroyedThisWayCantBeRegenerated() {
            var effect =
                    DestroyParser.parse("Destroy target creature. A creature destroyed this way can't be regenerated.");

            assertThat(effect).cannotBeRegenerated();
        }

        @Test
        @DisplayName("Destroy all artifacts. Artifacts destroyed this way can't be regenerated.")
        void artifactsDestroyedThisWayCantBeRegenerated() {
            var effect =
                    DestroyParser.parse("Destroy all artifacts. Artifacts destroyed this way can't be regenerated.");

            assertThat(effect).cannotBeRegenerated();
        }

        @Test
        @DisplayName("Destroy all creatures. Creatures destroyed this way can't be regenerated.")
        void creaturesDestroyedThisWayCantBeRegenerated() {
            var effect =
                    DestroyParser.parse("Destroy all creatures. Creatures destroyed this way can't be regenerated.");

            assertThat(effect).cannotBeRegenerated();
        }
    }
}
