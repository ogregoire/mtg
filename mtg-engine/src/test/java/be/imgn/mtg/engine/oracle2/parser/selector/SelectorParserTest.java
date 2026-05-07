package be.imgn.mtg.engine.oracle2.parser.selector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Locale;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.oracle2.domain.Ability;
import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.AmountMatcher;
import be.imgn.mtg.engine.oracle2.domain.CardType;
import be.imgn.mtg.engine.oracle2.domain.Color;
import be.imgn.mtg.engine.oracle2.domain.CombatRole;
import be.imgn.mtg.engine.oracle2.domain.CombatStatus;
import be.imgn.mtg.engine.oracle2.domain.CounterType;
import be.imgn.mtg.engine.oracle2.domain.ObjectStatus;
import be.imgn.mtg.engine.oracle2.domain.PlayerDesignation;
import be.imgn.mtg.engine.oracle2.domain.PlayerRelation;
import be.imgn.mtg.engine.oracle2.domain.PlayerTurnRole;
import be.imgn.mtg.engine.oracle2.domain.StandardQuantifier;
import be.imgn.mtg.engine.oracle2.domain.Supertype;
import be.imgn.mtg.engine.oracle2.domain.selector.AbilitySelector;
import be.imgn.mtg.engine.oracle2.domain.selector.CardTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ColorSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.CombatRoleSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.CombatStatusSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ControlledBySelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ManaCostSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.NameSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectCounterSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectPropertySelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerCounterSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerDesignationSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerRelationSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerTurnRoleSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PowerSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.QuantifierSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.domain.selector.SelfSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.StatusSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.StickerSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.SupertypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ToughnessSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ZoneSelector;

class SelectorParserTest {

    private static Selector parse(String input) {
        return SelectorParser.SELECTOR.parseSkipping(CharPredicate.is(' '), input);
    }

    /// Wrap `inner` in `Quantifier(Exact(1), …)` — the implicit
    /// count for any singular noun phrase with no explicit
    /// quantifier word ("target creature", "you", "tapped creature").
    /// The parser always emits this wrapper so the AST carries the
    /// count explicitly; tests use this helper to express that.
    private static Selector one(Selector inner) {
        return new QuantifierSelector(new Amount.Exact(1), inner);
    }

    @Nested
    class PlayerForms {
        @Test
        void you() {
            assertThat(parse("you")).isEqualTo(one(new PlayerRelationSelector(PlayerRelation.YOU)));
        }

        /// "an opponent" → "exactly one opponent, chosen freely" —
        /// the `an` is a count, not part of the noun atom. Mirrors
        /// "a creature" → `QuantifierSelector(Exact(1), …)` on the
        /// object side.
        @Test
        void anOpponent() {
            assertThat(parse("an opponent"))
                    .isEqualTo(new QuantifierSelector(
                            new Amount.Exact(1), new PlayerRelationSelector(PlayerRelation.OPPONENT)));
        }

        /// "Each X" means "all X" — must wrap in [QuantifierSelector] with
        /// [StandardQuantifier#ALL]. Bare `OPPONENT` would lose the
        /// "all of them" semantics.
        @Test
        void eachOpponent() {
            assertThat(parse("each opponent"))
                    .isEqualTo(new QuantifierSelector(
                            StandardQuantifier.ALL, new PlayerRelationSelector(PlayerRelation.OPPONENT)));
        }

        @Test
        void eachPlayer() {
            assertThat(parse("each player"))
                    .isEqualTo(new QuantifierSelector(StandardQuantifier.ALL, PlayerSelector.Anyone.ANYONE));
        }

        @Test
        void yourTeam() {
            assertThat(parse("your team")).isEqualTo(one(new PlayerRelationSelector(PlayerRelation.TEAM)));
        }

        @Test
        void theActivePlayer() {
            assertThat(parse("the active player")).isEqualTo(one(new PlayerTurnRoleSelector(PlayerTurnRole.ACTIVE)));
        }

        @Test
        void theMonarch() {
            assertThat(parse("the monarch")).isEqualTo(one(new PlayerDesignationSelector(PlayerDesignation.MONARCH)));
        }

        @Test
        void theAttackingPlayer() {
            assertThat(parse("the attacking player")).isEqualTo(one(new CombatRoleSelector(CombatRole.ATTACKING)));
        }

        @Test
        void anyPlayer() {
            assertThat(parse("any player")).isEqualTo(one(PlayerSelector.Anyone.ANYONE));
        }
    }

    @Nested
    class ObjectForms {
        @Test
        void self() {
            assertThat(parse("~")).isEqualTo(one(SelfSelector.SELF));
        }

        @Test
        void bareCreatureBecomesBattlefieldPermanent() {
            assertThat(parse("creature"))
                    .isEqualTo(one(new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(new CardTypeSelector.Is(CardType.CREATURE)))));
        }

        @Test
        void legendaryCreature() {
            var expected = new ZoneSelector.Battlefield(
                    new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                            new SupertypeSelector.Is(Supertype.LEGENDARY),
                            new CardTypeSelector.Is(CardType.CREATURE)))));
            assertThat(parse("legendary creature")).isEqualTo(one(expected));
        }

        @Test
        void blueCreature() {
            var expected =
                    new ZoneSelector.Battlefield(new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(
                            List.of(new ColorSelector.Is(Color.BLUE), new CardTypeSelector.Is(CardType.CREATURE)))));
            assertThat(parse("blue creature")).isEqualTo(one(expected));
        }

        @Test
        void noncreatureArtifact() {
            var expected = new ZoneSelector.Battlefield(
                    new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                            new CardTypeSelector.IsNot(CardType.CREATURE),
                            new CardTypeSelector.Is(CardType.ARTIFACT)))));
            assertThat(parse("noncreature artifact")).isEqualTo(one(expected));
        }

        @Test
        void creatureYouControl() {
            var expected = new ZoneSelector.Battlefield(
                    new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                            new CardTypeSelector.Is(CardType.CREATURE),
                            new ControlledBySelector(new PlayerRelationSelector(PlayerRelation.YOU))))));
            assertThat(parse("creature you control")).isEqualTo(one(expected));
        }

        @Test
        void creatureOrPlaneswalker() {
            var expected = new ZoneSelector.Battlefield(
                    new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AnyOf(List.of(
                            new CardTypeSelector.Is(CardType.CREATURE),
                            new CardTypeSelector.Is(CardType.PLANESWALKER)))));
            assertThat(parse("creature or planeswalker")).isEqualTo(one(expected));
        }

        @Test
        void creatureCardInYourGraveyard() {
            assertThat(parse("creature card in your graveyard"))
                    .isEqualTo(one(new ZoneSelector.Graveyard(
                            new PlayerRelationSelector(PlayerRelation.YOU),
                            new ObjectTypeSelector.Card(new CardTypeSelector.Is(CardType.CREATURE)))));
        }

        @Test
        void cardInExile() {
            assertThat(parse("card in exile"))
                    .isEqualTo(one(new ZoneSelector.Exile(
                            new ObjectTypeSelector.Card(ObjectPropertySelector.Anything.ANYTHING))));
        }

        @Test
        void attackingCreature() {
            var expected = new ZoneSelector.Battlefield(
                    new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                            new CombatStatusSelector(CombatStatus.ATTACKING),
                            new CardTypeSelector.Is(CardType.CREATURE)))));
            assertThat(parse("attacking creature")).isEqualTo(one(expected));
        }
    }

    @Nested
    class TargetForms {
        @Test
        void targetCreature() {
            assertThat(parse("target creature"))
                    .isEqualTo(one(new ObjectSelector.Target(new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(new CardTypeSelector.Is(CardType.CREATURE))))));
        }

        @Test
        void targetPlayer() {
            assertThat(parse("target player")).isEqualTo(one(new PlayerSelector.Target(PlayerSelector.Anyone.ANYONE)));
        }

        /// Cross-axis target union — Firesong and Sunspeaker:
        /// "target creature or player". The lone vintage card with
        /// this phrasing. `target` distributes across each
        /// alternative and the union folds into [Selector.AnyOf]
        /// because the alternatives mix axes (object vs player) —
        /// distinct from [ObjectPropertySelector.AnyOf] which only
        /// composes single-axis property selectors.
        @Test
        void targetCreatureOrPlayer() {
            var creature = new ObjectSelector.Target(new ZoneSelector.Battlefield(
                    new ObjectTypeSelector.Permanent(new CardTypeSelector.Is(CardType.CREATURE))));
            var player = new PlayerSelector.Target(PlayerSelector.Anyone.ANYONE);
            assertThat(parse("target creature or player"))
                    .isEqualTo(one(new Selector.AnyOf(List.of(creature, player))));
        }
    }

    @Nested
    class QuantifiedForms {
        @Test
        void twoCreatures() {
            var inner = new ZoneSelector.Battlefield(
                    new ObjectTypeSelector.Permanent(new CardTypeSelector.Is(CardType.CREATURE)));
            assertThat(parse("two creatures")).isEqualTo(new QuantifierSelector(new Amount.Exact(2), inner));
        }

        @Test
        void allCreaturesYouControl() {
            var inner = new ZoneSelector.Battlefield(
                    new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                            new CardTypeSelector.Is(CardType.CREATURE),
                            new ControlledBySelector(new PlayerRelationSelector(PlayerRelation.YOU))))));
            assertThat(parse("all creatures you control"))
                    .isEqualTo(new QuantifierSelector(StandardQuantifier.ALL, inner));
        }

        @Test
        void twoTargetCreatures() {
            var creature = new ZoneSelector.Battlefield(
                    new ObjectTypeSelector.Permanent(new CardTypeSelector.Is(CardType.CREATURE)));
            assertThat(parse("two target creatures"))
                    .isEqualTo(new QuantifierSelector(new Amount.Exact(2), new ObjectSelector.Target(creature)));
        }

        @Test
        void anyNumberOfCreatures() {
            var inner = new ZoneSelector.Battlefield(
                    new ObjectTypeSelector.Permanent(new CardTypeSelector.Is(CardType.CREATURE)));
            assertThat(parse("any number of creatures"))
                    .isEqualTo(new QuantifierSelector(StandardQuantifier.ANY_NUMBER, inner));
        }
    }

    @Nested
    class AbilityAxis {
        @Test
        void creatureWithFlying() {
            var expected = new ZoneSelector.Battlefield(
                    new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                            new CardTypeSelector.Is(CardType.CREATURE),
                            new AbilitySelector.Has(Ability.StaticKeyword.FLYING)))));
            assertThat(parse("creature with flying")).isEqualTo(one(expected));
        }

        /// Multi-word keyword phrase — `first strike` is two tokens but
        /// one keyword.
        @Test
        void creatureWithFirstStrike() {
            var expected = new ZoneSelector.Battlefield(
                    new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                            new CardTypeSelector.Is(CardType.CREATURE),
                            new AbilitySelector.Has(Ability.StaticKeyword.FIRST_STRIKE)))));
            assertThat(parse("creature with first strike")).isEqualTo(one(expected));
        }

        /// Disjunction at the keyword list — `with` distributes,
        /// each keyword becomes its own [AbilitySelector.Has], and
        /// they combine via [ObjectPropertySelector.AnyOf].
        @Test
        void creatureWithFlyingOrReach() {
            var either = new ObjectPropertySelector.AnyOf(List.of(
                    new AbilitySelector.Has(Ability.StaticKeyword.FLYING),
                    new AbilitySelector.Has(Ability.StaticKeyword.REACH)));
            var expected = new ZoneSelector.Battlefield(new ObjectTypeSelector.Permanent(
                    new ObjectPropertySelector.AllOf(List.of(new CardTypeSelector.Is(CardType.CREATURE), either))));
            assertThat(parse("creature with flying or reach")).isEqualTo(one(expected));
        }

        /// Conjunction — same shape as the disjunction but combined
        /// via [ObjectPropertySelector.AllOf].
        @Test
        void creatureWithFlyingAndVigilance() {
            var both = new ObjectPropertySelector.AllOf(List.of(
                    new AbilitySelector.Has(Ability.StaticKeyword.FLYING),
                    new AbilitySelector.Has(Ability.StaticKeyword.VIGILANCE)));
            var expected = new ZoneSelector.Battlefield(new ObjectTypeSelector.Permanent(
                    new ObjectPropertySelector.AllOf(List.of(new CardTypeSelector.Is(CardType.CREATURE), both))));
            assertThat(parse("creature with flying and vigilance")).isEqualTo(one(expected));
        }

        /// Triggered keyword — same parser arm, different enum.
        @Test
        void creatureWithProwess() {
            var expected = new ZoneSelector.Battlefield(
                    new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                            new CardTypeSelector.Is(CardType.CREATURE),
                            new AbilitySelector.Has(Ability.TriggeredKeyword.PROWESS)))));
            assertThat(parse("creature with prowess")).isEqualTo(one(expected));
        }

        /// Distinct shape — [AbilitySelector.HasNoAbilities] rather
        /// than `Has(...)`.
        @Test
        void creatureWithNoAbilities() {
            var expected = new ZoneSelector.Battlefield(
                    new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                            new CardTypeSelector.Is(CardType.CREATURE), new AbilitySelector.HasNoAbilities()))));
            assertThat(parse("creature with no abilities")).isEqualTo(one(expected));
        }

        /// Composes with other property arms — `you control` follows
        /// `with flying` via [PropertyParser]'s `atLeastOnce` AND
        /// chain.
        @Test
        void creatureWithFlyingYouControl() {
            var expected = new ZoneSelector.Battlefield(
                    new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                            new CardTypeSelector.Is(CardType.CREATURE),
                            new AbilitySelector.Has(Ability.StaticKeyword.FLYING),
                            new ControlledBySelector(new PlayerRelationSelector(PlayerRelation.YOU))))));
            assertThat(parse("creature with flying you control")).isEqualTo(one(expected));
        }

        /// Earthquake / Flamebreak / Thunder of Hooves family:
        /// "deal N damage to each creature without flying and each
        /// player". The "and" here joins two top-level selectors,
        /// not two keywords inside a `without` clause — so the
        /// without-clause must stop at `flying` and the top-level
        /// [SelectorParser#SELECTOR] folds the two selectors into a
        /// [Selector.AllOf]. Regression test for the deliberate
        /// absence of a `without X and Y` parser arm: if we ever
        /// re-add one, the without-clause would greedily consume
        /// "flying and each player" and this composition would
        /// break.
        @Test
        void eachCreatureWithoutFlyingAndEachPlayer() {
            var creatureWithoutFlying = new QuantifierSelector(
                    StandardQuantifier.ALL,
                    new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                                    new CardTypeSelector.Is(CardType.CREATURE),
                                    new AbilitySelector.HasNot(Ability.StaticKeyword.FLYING))))));
            var eachPlayer = new QuantifierSelector(StandardQuantifier.ALL, PlayerSelector.Anyone.ANYONE);
            assertThat(parse("each creature without flying and each player"))
                    .isEqualTo(new Selector.AllOf(List.of(creatureWithoutFlying, eachPlayer)));
        }
    }

    @Nested
    class NameAxis {
        @Test
        void cardNamedForestInYourGraveyard() {
            assertThat(parse("card named Forest in your graveyard"))
                    .isEqualTo(one(new ZoneSelector.Graveyard(
                            new PlayerRelationSelector(PlayerRelation.YOU),
                            new ObjectTypeSelector.Card(new NameSelector.Is("Forest")))));
        }

        @Test
        void creatureNamedSquee() {
            assertThat(parse("creature named Squee"))
                    .isEqualTo(one(new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                                    new CardTypeSelector.Is(CardType.CREATURE), new NameSelector.Is("Squee")))))));
        }

        @Test
        void creatureNamedSqueeTheImmortal() {
            assertThat(parse("creature named Squee, the Immortal"))
                    .isEqualTo(one(new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                                    new CardTypeSelector.Is(CardType.CREATURE),
                                    new NameSelector.Is("Squee, the Immortal")))))));
        }

        @Test
        void permanentWithTheChosenName() {
            assertThat(parse("permanent with the chosen name"))
                    .isEqualTo(one(new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(NameSelector.Standard.CHOSEN))));
        }

        /// Pompous Gadabout: "creatures that don't have a name" — the
        /// only vintage-legal namelessness phrasing.
        @Test
        void creaturesThatDontHaveAName() {
            assertThat(parse("creatures that don't have a name"))
                    .isEqualTo(one(new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                                    new CardTypeSelector.Is(CardType.CREATURE), NameSelector.Standard.HAS_NO_NAME))))));
        }
    }

    @Nested
    class ManaCostAxis {
        /// Extinction Event / Mutinous Massacre: "creature with mana
        /// value of the chosen quality".
        @Test
        void creatureWithManaValueOfTheChosenQuality() {
            assertThat(parse("creature with mana value of the chosen quality"))
                    .isEqualTo(one(new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                                    new CardTypeSelector.Is(CardType.CREATURE), ManaCostSelector.Standard.CHOSEN))))));
        }

        /// Ashling's Prerogative: "without mana value of the chosen
        /// quality" — handled by the property-level `Not` arm.
        @Test
        void creatureWithoutManaValueOfTheChosenQualityIsBlockedToday() {
            // "without" isn't yet wired in PropertyParser as a generic
            // negation prefix — documented as a future extension.
            assertThatThrownBy(() -> parse("creature without mana value of the chosen quality"))
                    .isInstanceOf(Exception.class);
        }

        /// Abrupt Decay: "Destroy target nonland permanent with mana
        /// value 3 or less."
        @Test
        void permanentWithManaValue3OrLess() {
            assertThat(parse("permanent with mana value 3 or less"))
                    .isEqualTo(one(new ZoneSelector.Battlefield(new ObjectTypeSelector.Permanent(
                            new ManaCostSelector.HasManaValue(new AmountMatcher.AtMost(new Amount.Exact(3)))))));
        }

        /// Angry Rabble: "Whenever you cast a spell with mana value 4
        /// or greater…".
        @Test
        void spellWithManaValue4OrGreater() {
            assertThat(parse("spell with mana value 4 or greater"))
                    .isEqualTo(one(new ZoneSelector.Stack(new ObjectTypeSelector.Spell(
                            new ManaCostSelector.HasManaValue(new AmountMatcher.AtLeast(new Amount.Exact(4)))))));
        }

        /// As Foretold: "spell you cast with mana value X or less".
        /// (We test the simpler "spell with mana value X or less" —
        /// "you cast" is a controller-relation that the property
        /// parser doesn't yet model.)
        @Test
        void spellWithManaValueXOrLess() {
            assertThat(parse("spell with mana value X or less"))
                    .isEqualTo(one(new ZoneSelector.Stack(new ObjectTypeSelector.Spell(
                            new ManaCostSelector.HasManaValue(new AmountMatcher.AtMost(Amount.Standard.X))))));
        }

        @Test
        void creatureWithTheSameManaValueAsThatCreatureRecursive() {
            // SharesManaValueWith with `~` self-reference (smallest
            // recursive case the current ObjectSelector supports).
            assertThat(parse("creature with the same mana value as ~"))
                    .isEqualTo(one(new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                                    new CardTypeSelector.Is(CardType.CREATURE),
                                    new ManaCostSelector.SharesManaValueWith(SelfSelector.SELF)))))));
        }
    }

    @Nested
    class CardTypeAxis {
        @Test
        void allCardTypesParse() {
            for (CardType t : CardType.values()) {
                String singular = t.text().replaceAll("\\(.*\\)", "").replaceAll("\\[(.*)\\|.*]", "$1");
                assertThat(parse(singular.toLowerCase(Locale.ROOT)))
                        .as("card type: %s", t)
                        .isEqualTo(one(new ZoneSelector.Battlefield(
                                new ObjectTypeSelector.Permanent(new CardTypeSelector.Is(t)))));
            }
        }
    }

    @Nested
    class SupertypeAxis {
        @Test
        void allSupertypesAsModifiersParse() {
            for (Supertype s : Supertype.values()) {
                String text = s.text().toLowerCase(Locale.ROOT);
                var actual = parse(text + " creature");
                var expected =
                        new ZoneSelector.Battlefield(new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(
                                List.of(new SupertypeSelector.Is(s), new CardTypeSelector.Is(CardType.CREATURE)))));
                assertThat(actual).as("supertype: %s", s).isEqualTo(one(expected));
            }
        }
    }

    @Nested
    class PowerAxis {
        /// Eternal Isolation: "target creature with power 4 or greater".
        @Test
        void creatureWithPower4OrGreater() {
            assertThat(parse("creature with power 4 or greater"))
                    .isEqualTo(one(new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                                    new CardTypeSelector.Is(CardType.CREATURE),
                                    new PowerSelector.HasPower(new AmountMatcher.AtLeast(new Amount.Exact(4)))))))));
        }

        @Test
        void creatureWithPower2OrLess() {
            assertThat(parse("creature with power 2 or less"))
                    .isEqualTo(one(new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                                    new CardTypeSelector.Is(CardType.CREATURE),
                                    new PowerSelector.HasPower(new AmountMatcher.AtMost(new Amount.Exact(2)))))))));
        }

        @Test
        void creatureWithPowerX() {
            assertThat(parse("creature with power X"))
                    .isEqualTo(one(new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                                    new CardTypeSelector.Is(CardType.CREATURE),
                                    new PowerSelector.HasPower(new AmountMatcher.Exactly(Amount.Standard.X))))))));
        }

        /// Shared-matcher disjunction: "with power or toughness 1 or
        /// less" distributes the matcher across both aspects.
        @Test
        void creatureYouControlWithPowerOrToughness1OrLess() {
            var matcher = new AmountMatcher.AtMost(new Amount.Exact(1));
            var inner = new ZoneSelector.Battlefield(
                    new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                            new CardTypeSelector.Is(CardType.CREATURE),
                            new ControlledBySelector(new PlayerRelationSelector(PlayerRelation.YOU)),
                            new ObjectPropertySelector.AnyOf(List.of(
                                    new PowerSelector.HasPower(matcher),
                                    new ToughnessSelector.HasToughness(matcher)))))));
            assertThat(parse("a creature you control with power or toughness 1 or less"))
                    .isEqualTo(new QuantifierSelector(new Amount.Exact(1), inner));
        }

        @Test
        void creatureWithTheSamePowerAsSelf() {
            assertThat(parse("creature with the same power as ~"))
                    .isEqualTo(one(new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                                    new CardTypeSelector.Is(CardType.CREATURE),
                                    new PowerSelector.SharesPowerWith(SelfSelector.SELF)))))));
        }
    }

    @Nested
    class ToughnessAxis {
        @Test
        void creatureWithToughness3OrLess() {
            assertThat(parse("creature with toughness 3 or less"))
                    .isEqualTo(one(new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                                    new CardTypeSelector.Is(CardType.CREATURE),
                                    new ToughnessSelector.HasToughness(
                                            new AmountMatcher.AtMost(new Amount.Exact(3)))))))));
        }
    }

    @Nested
    class StatusAxis {
        @Test
        void tappedCreature() {
            assertThat(parse("tapped creature"))
                    .isEqualTo(one(new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                                    new StatusSelector.HasStatus(ObjectStatus.TAPPED),
                                    new CardTypeSelector.Is(CardType.CREATURE)))))));
        }

        @Test
        void untappedCreature() {
            assertThat(parse("untapped creature"))
                    .isEqualTo(one(new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                                    new StatusSelector.HasStatus(ObjectStatus.UNTAPPED),
                                    new CardTypeSelector.Is(CardType.CREATURE)))))));
        }

        @Test
        void faceDownCreatureHyphenated() {
            assertThat(parse("face-down creature"))
                    .isEqualTo(one(new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                                    new StatusSelector.HasStatus(ObjectStatus.FACE_DOWN),
                                    new CardTypeSelector.Is(CardType.CREATURE)))))));
        }

        @Test
        void phasedOutPermanent() {
            assertThat(parse("phased out permanent"))
                    .isEqualTo(one(new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(new StatusSelector.HasStatus(ObjectStatus.PHASED_OUT)))));
        }
    }

    @Nested
    class ObjectCounterAxis {
        @Test
        void creatureWithAPlus1Plus1Counter() {
            assertThat(parse("creature with a +1/+1 counter"))
                    .isEqualTo(one(new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                                    new CardTypeSelector.Is(CardType.CREATURE),
                                    new ObjectCounterSelector.HasCounters(
                                            new CounterType.PtCounter(1, 1),
                                            new AmountMatcher.AtLeast(new Amount.Exact(1)))))))));
        }

        @Test
        void creatureWithThreeOrMorePlus1Plus1Counters() {
            assertThat(parse("creature with three or more +1/+1 counters"))
                    .isEqualTo(one(new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                                    new CardTypeSelector.Is(CardType.CREATURE),
                                    new ObjectCounterSelector.HasCounters(
                                            new CounterType.PtCounter(1, 1),
                                            new AmountMatcher.AtLeast(new Amount.Exact(3)))))))));
        }

        @Test
        void creatureWithALoyaltyCounter() {
            assertThat(parse("creature with a loyalty counter"))
                    .isEqualTo(one(new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                                    new CardTypeSelector.Is(CardType.CREATURE),
                                    new ObjectCounterSelector.HasCounters(
                                            CounterType.Named.LOYALTY,
                                            new AmountMatcher.AtLeast(new Amount.Exact(1)))))))));
        }

        @Test
        void creatureWithNoCounters() {
            assertThat(parse("creature with no counters"))
                    .isEqualTo(one(new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                                    new CardTypeSelector.Is(CardType.CREATURE),
                                    new ObjectCounterSelector.HasCounters(
                                            CounterType.Any.ANY, new AmountMatcher.Exactly(new Amount.Exact(0)))))))));
        }
    }

    @Nested
    class StickerAxis {
        /// Scared Stiff / Big Winner / etc.: "stickered permanent".
        @Test
        void stickeredPermanent() {
            assertThat(parse("stickered permanent"))
                    .isEqualTo(one(new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(new StickerSelector.IsStickered()))));
        }
    }

    @Nested
    class PlayerCounterAxis {
        /// `an` is treated as a quantifier (count of 1), so the
        /// selector wraps the inner [PlayerCounterSelector.HasCounters]
        /// in a [QuantifierSelector].
        @Test
        void opponentWithThreeOrMorePoisonCounters() {
            assertThat(parse("an opponent with three or more poison counters"))
                    .isEqualTo(new QuantifierSelector(
                            new Amount.Exact(1),
                            new PlayerCounterSelector.HasCounters(
                                    new PlayerRelationSelector(PlayerRelation.OPPONENT),
                                    CounterType.Named.POISON,
                                    new AmountMatcher.AtLeast(new Amount.Exact(3)))));
        }

        @Test
        void playerWithAPoisonCounter() {
            assertThat(parse("a player with a poison counter"))
                    .isEqualTo(new QuantifierSelector(
                            new Amount.Exact(1),
                            new PlayerCounterSelector.HasCounters(
                                    PlayerSelector.Anyone.ANYONE,
                                    CounterType.Named.POISON,
                                    new AmountMatcher.AtLeast(new Amount.Exact(1)))));
        }
    }
}
