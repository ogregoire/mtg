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
import be.imgn.mtg.engine.oracle2.domain.StandardQuantifier;
import be.imgn.mtg.engine.oracle2.domain.TypeMatcher;
import be.imgn.mtg.engine.oracle2.domain.effect.AddManaEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.AddManaEffect.Replacement;
import be.imgn.mtg.engine.oracle2.domain.mana.Mana;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Colored;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Colorless;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Variable;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaType;
import be.imgn.mtg.engine.oracle2.domain.mana.Palette;
import be.imgn.mtg.engine.oracle2.domain.mana.ProducedMana;
import be.imgn.mtg.engine.oracle2.domain.mana.Restriction;
import be.imgn.mtg.engine.oracle2.domain.selector.CardTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ControlledBySelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectPropertySelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerRelationSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.QuantifierSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.domain.selector.SelfSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ZoneSelector;

class AddManaEffectParserTest {

    private static AddManaEffect parse(String input) {
        return AddManaEffectParser.ADD_MANA.parseSkipping(CharPredicate.is(' '), input);
    }

    private static ProducedMana.Exact payload(ManaSymbol... symbols) {
        return new ProducedMana.Exact(Arrays.stream(symbols).map(Mana::new).toList());
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

        /// "Add {R} or {G}." (Birds of Paradise variants — the
        /// controller chooses one of the alternatives at resolution).
        @Test
        void anyOfAlternatives() {
            var r = Colored.of(ManaType.RED);
            var anyOf = new ProducedMana.OneOf(List.of(payload(r), payload(G)));
            assertThat(parse("Add {R} or {G}.")).isEqualTo(new AddManaEffect(anyOf));
        }

        /// "Add {U} or {C}{U}." (Adarkar Unicorn) — alternatives may
        /// have different lengths.
        @Test
        void anyOfAsymmetricAlternatives() {
            var u = Colored.of(ManaType.BLUE);
            var anyOf = new ProducedMana.OneOf(List.of(payload(u), payload(C, u)));
            assertThat(parse("Add {U} or {C}{U}.")).isEqualTo(new AddManaEffect(anyOf));
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
                    new Restriction.ToCast(new TypeMatcher.OneOf(List.of(elementalSpell, chandraPlaneswalker)));
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
            var restriction = new Restriction.OneOf(
                    List.of(new Restriction.ToCast(spell), new Restriction.ToActivateAbility(source)));
            assertThat(parse("Add {C}. Spend this mana only to cast colorless Eldrazi spells "
                            + "or activate abilities of colorless Eldrazi."))
                    .isEqualTo(new AddManaEffect(payload(C)).withRestriction(restriction));
        }

        @Test
        void activateOrCastReversedOrder() {
            var artifactSpell = new TypeMatcher.IsCardType(CardType.ARTIFACT);
            var restriction = new Restriction.OneOf(
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

    @Nested
    class ActorForm {
        @Test
        void targetPlayerAddsG() {
            var targetPlayer = new QuantifierSelector(
                    new Amount.Exact(1), new PlayerSelector.Target(PlayerSelector.Anyone.ANYONE));
            assertThat(parse("Target player adds {G}."))
                    .isEqualTo(new AddManaEffect(payload(G)).withPlayer(targetPlayer));
        }

        @Test
        void eachOpponentAddsC() {
            var eachOpponent =
                    new QuantifierSelector(StandardQuantifier.ALL, new PlayerRelationSelector(PlayerRelation.OPPONENT));
            assertThat(parse("Each opponent adds {C}."))
                    .isEqualTo(new AddManaEffect(payload(C)).withPlayer(eachOpponent));
        }
    }

    @Nested
    class TrailingFlavour {
        /// "where X is the number of \<selector\>" — parses
        /// structurally to an [Amount.CountOf] bound on the
        /// [AddManaEffect#xDefinition] slot.
        @Test
        void whereXIsTheNumberOfCreaturesYouControl() {
            var parsed = parse("Add X mana of any one color, where X is the number of creatures you control.");
            var payload = new ProducedMana.OfOneColor(
                    Amount.Standard.X,
                    ((ProducedMana.OfOneColor)
                                    parse("Add X mana of any one color.").payload())
                            .palette());
            // The parsed X-definition should be a CountOf — check structure
            // without re-asserting the full ObjectSelector tree (covered by
            // selector parser tests).
            assertThat(parsed.payload()).isEqualTo(payload);
            assertThat(parsed.xDefinition()).isInstanceOf(Amount.CountOf.class);
        }
    }

    @Nested
    class DynamicPalette {
        /// "a land you control" parsed via `ObjectSelectorParser.OBJECT_SELECTOR`
        /// (with the "a" determiner stripped) — `Battlefield(Permanent(AllOf([land, controlled-by-you])))`.
        /// No `QuantifierSelector` wrap: the palette slot is
        /// `ObjectSelector` (narrow), the count is implicit in the
        /// palette's role.
        private final ObjectSelector landYouControl =
                new ZoneSelector.Battlefield(new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                        new CardTypeSelector.Is(CardType.LAND),
                        new ControlledBySelector(new PlayerRelationSelector(PlayerRelation.YOU))))));

        @Test
        void anyColorALandYouControlCouldProduce() {
            var palette = new Palette.CouldProduce(landYouControl, Palette.Filter.COLOR);
            var payload = new ProducedMana.OfOneColor(new Amount.Exact(1), palette);
            assertThat(parse("Add one mana of any color that a land you control could produce."))
                    .isEqualTo(new AddManaEffect(payload));
        }

        @Test
        void anyTypeALandYouControlProduced() {
            var palette = new Palette.Produced(landYouControl, Palette.Filter.TYPE);
            var payload = new ProducedMana.OfOneColor(new Amount.Exact(1), palette);
            assertThat(parse("Add one mana of any type that a land you control produced."))
                    .isEqualTo(new AddManaEffect(payload));
        }
    }

    @Nested
    class RepeatedShapes {
        @Test
        void manaForEachSelector() {
            // "for each creature you control" → CountOf via the
            // full ObjectSelector parser (includes ControlledBy).
            // The inner selector shape is well-tested elsewhere; here
            // we just check the outer Repeated/CountOf structure.
            var addMana = parse("Add {C}{C} for each creature you control.");
            assertThat(addMana.payload()).isInstanceOf(ProducedMana.Repeated.class);
            var rep = (ProducedMana.Repeated) addMana.payload();
            assertThat(rep.count()).isInstanceOf(Amount.CountOf.class);
            assertThat(rep.symbols()).hasSize(2);
        }

        @Test
        void amountOfSymbolEqualToThisCreaturesPower() {
            var power = new Amount.PowerOf(SelfSelector.SELF);
            var expected = new ProducedMana.Repeated(power, List.of(G));
            assertThat(parse("Add an amount of {G} equal to this creature's power."))
                    .isEqualTo(new AddManaEffect(expected));
        }
    }
}
