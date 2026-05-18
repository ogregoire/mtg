package be.imgn.mtg.engine.oracle2.parser.effect;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.AmountMatcher;
import be.imgn.mtg.engine.oracle2.domain.CardType;
import be.imgn.mtg.engine.oracle2.domain.Condition;
import be.imgn.mtg.engine.oracle2.domain.CreatureType;
import be.imgn.mtg.engine.oracle2.domain.PlaneswalkerType;
import be.imgn.mtg.engine.oracle2.domain.PlayerRelation;
import be.imgn.mtg.engine.oracle2.domain.TypeMatcher;
import be.imgn.mtg.engine.oracle2.domain.effect.AddManaEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.AddManaEffect.Replacement;
import be.imgn.mtg.engine.oracle2.domain.mana.Mana;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Colored;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Colorless;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Variable;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaType;
import be.imgn.mtg.engine.oracle2.domain.mana.ProducedMana;
import be.imgn.mtg.engine.oracle2.domain.mana.Restriction;
import be.imgn.mtg.engine.oracle2.domain.selector.CardTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerRelationSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.QuantifierSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.domain.selector.ZoneSelector;

class AddManaEffectParserTest {

    private static AddManaEffect parse(String input) {
        return AddManaEffectParser.ADD_MANA.parseSkipping(CharPredicate.is(' '), input);
    }

    private static ProducedMana payload(ManaSymbol... symbols) {
        return new ProducedMana(Arrays.stream(symbols).map(Mana::new).toList());
    }

    private static final ManaSymbol G = Colored.of(ManaType.GREEN);
    private static final ManaSymbol C = Colorless.COLORLESS;

    @Nested
    class Bare {
        @Test
        void single() {
            assertThat(parse("Add {G}.")).isEqualTo(new AddManaEffect(payload(G)));
        }

        @Test
        void multiSymbol() {
            assertThat(parse("Add {G}{G}.")).isEqualTo(new AddManaEffect(payload(G, G)));
        }
    }

    @Nested
    class ToCastRestriction {
        @Test
        void artifactSpells() {
            var restriction = new Restriction.ToCast(new TypeMatcher.IsCardType(CardType.ARTIFACT));
            assertThat(parse("Add {C}. Spend this mana only to cast artifact spells."))
                    .isEqualTo(new AddManaEffect(payload(C)).withRestriction(restriction));
        }

        @Test
        void colorlessEldraziSpells() {
            var restriction = new Restriction.ToCast(new TypeMatcher.AllOf(
                    List.of(TypeMatcher.Standard.COLORLESS, new TypeMatcher.IsSubtype(CreatureType.ELDRAZI))));
            assertThat(parse("Add {C}. Spend this mana only to cast colorless Eldrazi spells."))
                    .isEqualTo(new AddManaEffect(payload(C)).withRestriction(restriction));
        }

        @Test
        void spellsWithManaValueFourOrGreater() {
            var restriction = new Restriction.ToCast(
                    new TypeMatcher.HasManaValue(new AmountMatcher.AtLeast(new Amount.Exact(4))));
            assertThat(parse("Add {C}{C}{C}. Spend this mana only to cast spells with mana value 4 or greater."))
                    .isEqualTo(new AddManaEffect(payload(C, C, C)).withRestriction(restriction));
        }

        @Test
        void elementalOrChandraPlaneswalker() {
            var elementalSpell = new TypeMatcher.IsSubtype(CreatureType.ELEMENTAL);
            var chandraPlaneswalker = new TypeMatcher.AllOf(List.of(
                    new TypeMatcher.IsSubtype(PlaneswalkerType.CHANDRA),
                    new TypeMatcher.IsCardType(CardType.PLANESWALKER)));
            var restriction =
                    new Restriction.ToCast(new TypeMatcher.AnyOf(List.of(elementalSpell, chandraPlaneswalker)));
            assertThat(
                            parse(
                                    "Add {R}. Spend this mana only to cast an Elemental spell or a Chandra planeswalker spell."))
                    .isEqualTo(new AddManaEffect(payload(Colored.of(ManaType.RED))).withRestriction(restriction));
        }
    }

    @Nested
    class ToActivateAbilityRestriction {
        @Test
        void bareAbilities() {
            var restriction = new Restriction.ToActivateAbility(null);
            assertThat(parse("Add {C}{C}. Spend this mana only to activate abilities."))
                    .isEqualTo(new AddManaEffect(payload(C, C)).withRestriction(restriction));
        }

        @Test
        void abilitiesOfColorlessEldrazi() {
            var source = new TypeMatcher.AllOf(
                    List.of(TypeMatcher.Standard.COLORLESS, new TypeMatcher.IsSubtype(CreatureType.ELDRAZI)));
            var restriction = new Restriction.ToActivateAbility(source);
            assertThat(parse("Add {C}. Spend this mana only to activate abilities of colorless Eldrazi."))
                    .isEqualTo(new AddManaEffect(payload(C)).withRestriction(restriction));
        }
    }

    @Nested
    class CompositeRestriction {
        @Test
        void castOrActivateAbilitiesOf() {
            var spell = new TypeMatcher.AllOf(
                    List.of(TypeMatcher.Standard.COLORLESS, new TypeMatcher.IsSubtype(CreatureType.ELDRAZI)));
            var source = new TypeMatcher.AllOf(
                    List.of(TypeMatcher.Standard.COLORLESS, new TypeMatcher.IsSubtype(CreatureType.ELDRAZI)));
            var restriction = new Restriction.AnyOf(
                    List.of(new Restriction.ToCast(spell), new Restriction.ToActivateAbility(source)));
            assertThat(parse("Add {C}. Spend this mana only to cast colorless Eldrazi spells "
                            + "or activate abilities of colorless Eldrazi."))
                    .isEqualTo(new AddManaEffect(payload(C)).withRestriction(restriction));
        }

        @Test
        void activateOrCastReversedOrder() {
            var artifactSpell = new TypeMatcher.IsCardType(CardType.ARTIFACT);
            var restriction = new Restriction.AnyOf(
                    List.of(new Restriction.ToActivateAbility(null), new Restriction.ToCast(artifactSpell)));
            assertThat(parse("Add {C}. Spend this mana only to activate an ability or cast an artifact spell."))
                    .isEqualTo(new AddManaEffect(payload(C)).withRestriction(restriction));
        }
    }

    @Nested
    class OnCostsContainingRestriction {
        @Test
        void onCostsContainingX() {
            var restriction = new Restriction.OnCostsContaining(Variable.X);
            assertThat(parse("Add {C}. Spend this mana only on costs that contain {X}."))
                    .isEqualTo(new AddManaEffect(payload(C)).withRestriction(restriction));
        }
    }

    @Nested
    class ConditionalReplacement {
        @Test
        void ifYouControlFourOrMoreCreatures() {
            Selector you = new QuantifierSelector(new Amount.Exact(1), new PlayerRelationSelector(PlayerRelation.YOU));
            Selector creatures = new QuantifierSelector(
                    new Amount.Exact(1),
                    new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(new CardTypeSelector.Is(CardType.CREATURE))));
            var condition = new Condition.Controls(you, new AmountMatcher.AtLeast(new Amount.Exact(4)), creatures);
            var replacement = new Replacement(condition, payload(G, G));
            assertThat(parse("Add {G}. If you control four or more creatures, add {G}{G} instead."))
                    .isEqualTo(new AddManaEffect(payload(G)).withReplacement(replacement));
        }
    }
}
