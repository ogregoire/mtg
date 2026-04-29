package be.imgn.mtg.engine.oracle.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.oracle.domain.*;

class OracleParserTest {

    // ── Empty / null input ────────────────────────────────────────────────

    @Nested
    class EmptyInput {

        @Test
        void parsesEmptyStringAsEmpty() {
            var result = OracleParser.parse("Some Card", "");
            assertThat(result).isEmpty();
        }

        @Test
        void parsesBlankAsEmpty() {
            var result = OracleParser.parse("Some Card", "   ");
            assertThat(result).isEmpty();
        }
    }

    // ── Activated abilities ───────────────────────────────────────────────

    @Nested
    class ActivatedAbilities {

        @Test
        void parsesTapAddGreen() {
            var result = OracleParser.parse("Llanowar Elves", "{T}: Add {G}.");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isInstanceOf(Ability.ActivatedAbility.class);
            var activated = (Ability.ActivatedAbility) result.getFirst();
            assertThat(activated.cost()).isInstanceOf(Cost.TapSelf.class);
            assertThat(activated.effects()).hasSize(1);
            assertThat(activated.effects().getFirst()).isInstanceOf(Effect.AddMana.class);
        }

        @Test
        void parsesTwoWhiteTapEffect() {
            var result = OracleParser.parse("Test Card", "{2}{W}, {T}: Draw a card.");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isInstanceOf(Ability.ActivatedAbility.class);
            var activated = (Ability.ActivatedAbility) result.getFirst();
            assertThat(activated.cost()).isInstanceOf(Cost.AllOf.class);
        }

        @Test
        void absorbsTrailingSpendRestrictionIntoMana() {
            // Adarkar Unicorn — the "Spend this mana only …" sentence
            // following an AddMana folds into the AddMana's payload as
            // Mana.Restricted (rule 106.6).
            var result = OracleParser.parse(
                    "Adarkar Unicorn", "{T}: Add {U} or {C}{U}. Spend this mana only to pay cumulative upkeep costs.");
            assertThat(result).hasSize(1);
            var activated = (Ability.ActivatedAbility) result.getFirst();
            assertThat(activated.effects()).hasSize(1);
            var addMana = (Effect.AddMana) activated.effects().getFirst();
            assertThat(addMana.mana())
                    .isEqualTo(new Mana.Restricted(
                            new Mana.AnyOf(List.of(
                                    new Mana.Exact(List.of(new ManaSymbol("{U}"))),
                                    new Mana.Exact(List.of(new ManaSymbol("{C}"), new ManaSymbol("{U}"))))),
                            new Restriction.SpendOnly("pay cumulative upkeep costs")));
        }

        @Test
        void leavesOrphanSpendRestrictionAsSiblingEffect() {
            // Piracy — the "Spend this mana only …" follows TapForMana,
            // not AddMana, so it stays as a sibling SpendThisManaOnly
            // effect (no AddMana to fold into).
            var result = OracleParser.parse(
                    "Piracy",
                    "Until end of turn, you may tap lands you don't control for mana."
                            + " Spend this mana only to cast spells.");
            assertThat(result).hasSize(1);
            var spell = (Ability.SpellAbility) result.getFirst();
            assertThat(spell.effects()).hasSize(2);
            assertThat(spell.effects().get(1)).isInstanceOf(Effect.SpendThisManaOnly.class);
            var stmo = (Effect.SpendThisManaOnly) spell.effects().get(1);
            assertThat(stmo.restriction()).isEqualTo(new Restriction.SpendOnly("cast spells"));
        }
    }

    // ── Spell abilities ───────────────────────────────────────────────────

    @Nested
    class SpellAbilities {

        @Test
        void parsesDealDamageAnyTarget() {
            var result = OracleParser.parse("Dummy", "Deal 3 damage to any target.");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isInstanceOf(Ability.SpellAbility.class);
            var spell = (Ability.SpellAbility) result.getFirst();
            assertThat(spell.effects()).hasSize(1);
            assertThat(spell.effects().getFirst()).isInstanceOf(Effect.DealDamage.class);
        }

        @Test
        void parsesDestroyTargetCreature() {
            var result = OracleParser.parse("Dummy", "Destroy target creature.");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isInstanceOf(Ability.SpellAbility.class);
            var spell = (Ability.SpellAbility) result.getFirst();
            assertThat(spell.effects().getFirst()).isInstanceOf(Effect.Destroy.class);
        }

        @Test
        void parsesExileTargetCreature() {
            var result = OracleParser.parse("Dummy", "Exile target creature.");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isInstanceOf(Ability.SpellAbility.class);
            var spell = (Ability.SpellAbility) result.getFirst();
            assertThat(spell.effects().getFirst()).isInstanceOf(Effect.Exile.class);
        }

        /// Regression for the OR_TYPE refactor: "enchanted creature or
        /// enchantment creature" must resolve as an Or with two
        /// Alternatives — the first carrying its own {@code enchanted}
        /// qualifier on a Single creature, the second a bare compound —
        /// while the outer Selector's shared qualifiers contain only
        /// {@code target}.
        @Test
        void parsesFeastOfDreamsOrOfCompound() {
            var result =
                    OracleParser.parse("Feast of Dreams", "Destroy target enchanted creature or enchantment creature.");
            // Multi-axis Or with per-branch qualifiers ("enchanted"
            // only on the first branch) can't be represented as a
            // single [Selector] in the new matcher-qualifier model;
            // the parse instead succeeds at the [Subject.OneOf]
            // level, with one [Subject.Select] per disjunct.
            assertThat(result).hasSize(1);
            var spell = (Ability.SpellAbility) result.getFirst();
            var destroy = (Effect.Destroy) spell.effects().getFirst();
            assertThat(destroy.target()).isInstanceOf(Subject.OneOf.class);
            var oneOf = (Subject.OneOf) destroy.target();
            assertThat(oneOf.alternatives()).hasSize(2);
            var first = (Subject.Select) oneOf.alternatives().get(0);
            assertThat(first.selector().objectType()).isEqualTo(GameObjectType.PERMANENT);
            assertThat(first.selector().qualifiers())
                    .containsExactly(
                            Selector.Qualifier.TARGET,
                            Selector.Qualifier.Enchanted.ENCHANTED,
                            new Selector.Qualifier.Types(TypeMatcher.CREATURE));
            var second = (Subject.Select) oneOf.alternatives().get(1);
            assertThat(second.selector().objectType()).isEqualTo(GameObjectType.PERMANENT);
            assertThat(second.selector().qualifiers())
                    .containsExactly(new Selector.Qualifier.Types(
                            new TypeMatcher.All(List.of(TypeMatcher.ENCHANTMENT, TypeMatcher.CREATURE))));
        }

        /// Regression: a plain Oxford-comma or-list of singles folds
        /// into a single Selector with one matcher-`Any` qualifier.
        @Test
        void parsesSimpleOrListAsOrOfSingles() {
            var result = OracleParser.parse("Test", "Destroy target artifact, enchantment, or land.");
            assertThat(result).hasSize(1);
            var spell = (Ability.SpellAbility) result.getFirst();
            var destroy = (Effect.Destroy) spell.effects().getFirst();
            var select = (Subject.Select) destroy.target();
            assertThat(select.selector().qualifiers())
                    .containsExactly(
                            Selector.Qualifier.TARGET,
                            new Selector.Qualifier.Types(new TypeMatcher.Any(
                                    List.of(TypeMatcher.ARTIFACT, TypeMatcher.ENCHANTMENT, TypeMatcher.LAND))));
            assertThat(select.selector().objectType()).isEqualTo(GameObjectType.PERMANENT);
        }

        /// Regression: plain compound types (no "or") fold into a
        /// single `CardTypes(All[...])` qualifier through the merge
        /// step.
        @Test
        void parsesCompoundOnlyStaysCompound() {
            var result = OracleParser.parse("Test", "Destroy target artifact creature.");
            assertThat(result).hasSize(1);
            var spell = (Ability.SpellAbility) result.getFirst();
            var destroy = (Effect.Destroy) spell.effects().getFirst();
            var select = (Subject.Select) destroy.target();
            assertThat(select.selector().objectType()).isEqualTo(GameObjectType.PERMANENT);
            assertThat(select.selector().qualifiers())
                    .containsExactly(
                            Selector.Qualifier.TARGET,
                            new Selector.Qualifier.Types(
                                    new TypeMatcher.All(List.of(TypeMatcher.ARTIFACT, TypeMatcher.CREATURE))));
        }
    }

    // ── Triggered abilities ───────────────────────────────────────────────

    @Nested
    class TriggeredAbilities {

        @Test
        void parsesWhenEntersDrawACard() {
            var result = OracleParser.parse("Test Card", "When ~ enters, draw a card.");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isInstanceOf(Ability.TriggeredAbility.class);
            var triggered = (Ability.TriggeredAbility) result.getFirst();
            assertThat(triggered.triggerWord()).isEqualToIgnoringCase("when");
            assertThat(triggered.effects()).hasSize(1);
            assertThat(triggered.effects().getFirst()).isInstanceOf(Effect.Draw.class);
        }

        @Test
        void parsesWheneverCreatureEnters() {
            var result = OracleParser.parse("Test Card", "Whenever a creature enters, draw a card.");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isInstanceOf(Ability.TriggeredAbility.class);
            var triggered = (Ability.TriggeredAbility) result.getFirst();
            assertThat(triggered.triggerWord()).isEqualToIgnoringCase("whenever");
        }
    }

    // ── Self-reference replacement ─────────────────────────────────────────

    @Nested
    class SelfReference {

        @Test
        void replacesCardNameWithTilde() {
            var result = OracleParser.parse("Lightning Bolt", "Lightning Bolt deals 3 damage to any target.");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isInstanceOf(Ability.SpellAbility.class);
            var spell = (Ability.SpellAbility) result.getFirst();
            assertThat(spell.effects().getFirst()).isInstanceOf(Effect.DealDamage.class);
            var dd = (Effect.DealDamage) spell.effects().getFirst();
            assertThat(dd.source()).isInstanceOf(Subject.SelfRef.class);
        }
    }

    // ── Multi-paragraph ───────────────────────────────────────────────────

    @Nested
    class MultiParagraph {

        @Test
        void parsesTwoParagraphs() {
            var text = "Destroy target creature.\nWhen ~ enters, draw a card.";
            var result = OracleParser.parse("Test Card", text);
            assertThat(result).hasSize(2);
            assertThat(result.get(0)).isInstanceOf(Ability.SpellAbility.class);
            assertThat(result.get(1)).isInstanceOf(Ability.TriggeredAbility.class);
        }

        @Test
        void skipsEmptyLines() {
            var text = "Destroy target creature.\n\nWhen ~ enters, draw a card.";
            var result = OracleParser.parse("Test Card", text);
            assertThat(result).hasSize(2);
        }
    }

    // ── Keyword abilities ─────────────────────────────────────────────────

    @Nested
    class KeywordAbilities {

        @Test
        void parsesFlyingAsSingleton() {
            var result = OracleParser.parse("Test Card", "Flying");
            assertThat(result).containsExactly(Ability.StaticKeyword.FLYING);
        }

        @Test
        void parsesMultiWordKeyword() {
            var result = OracleParser.parse("Test Card", "First strike");
            assertThat(result).containsExactly(Ability.StaticKeyword.FIRST_STRIKE);
        }

        @Test
        void parsesTriggeredKeyword() {
            var result = OracleParser.parse("Test Card", "Prowess");
            assertThat(result).containsExactly(Ability.TriggeredKeyword.PROWESS);
            assertThat(result.getFirst()).isInstanceOf(Ability.Triggered.class);
        }

        @Test
        void parsesKeywordList() {
            var result = OracleParser.parse("Test Card", "Flying, trample, haste");
            assertThat(result)
                    .containsExactly(
                            Ability.StaticKeyword.FLYING, Ability.StaticKeyword.TRAMPLE, Ability.StaticKeyword.HASTE);
        }

        @Test
        void parsesBaneslayerAngelLine() {
            var result = OracleParser.parse(
                    "Baneslayer Angel", "Flying, first strike, lifelink, protection from Demons and from Dragons");
            assertThat(result).hasSize(4);
            assertThat(result.get(0)).isEqualTo(Ability.StaticKeyword.FLYING);
            assertThat(result.get(1)).isEqualTo(Ability.StaticKeyword.FIRST_STRIKE);
            assertThat(result.get(2)).isEqualTo(Ability.StaticKeyword.LIFELINK);
            var protection = (Ability.Protection) result.get(3);
            assertThat(protection.qualities())
                    .containsExactly(
                            new ProtectionQuality.OfSubtype(CreatureType.DEMON),
                            new ProtectionQuality.OfSubtype(CreatureType.DRAGON));
        }

        @Test
        void parsesWardMana() {
            var result = OracleParser.parse("Test Card", "Ward {2}");
            assertThat(result).hasSize(1);
            var ward = (Ability.Ward) result.getFirst();
            assertThat(ward.cost()).isEqualTo(new Cost.Mana(List.of(new ManaSymbol("{2}"))));
        }

        @Test
        void parsesEquipCost() {
            var result = OracleParser.parse("Test Card", "Equip {3}");
            assertThat(result).hasSize(1);
            var equip = (Ability.Equip) result.getFirst();
            assertThat(equip.cost()).isEqualTo(new Cost.Mana(List.of(new ManaSymbol("{3}"))));
            assertThat(equip).isInstanceOf(Ability.Activated.class);
        }

        @Test
        void parsesEnchant() {
            var result = OracleParser.parse("Test Card", "Enchant creature");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isInstanceOf(Ability.Enchant.class);
            var enchant = (Ability.Enchant) result.getFirst();
            assertThat(enchant.target().objectType()).isEqualTo(GameObjectType.PERMANENT);
            assertThat(enchant.target().qualifiers())
                    .containsExactly(new Selector.Qualifier.Types(TypeMatcher.CREATURE));
        }

        @Test
        void parsesBasicLandwalk() {
            var result = OracleParser.parse("Test", "Forestwalk");
            assertThat(result).hasSize(1);
            var lw = (Ability.Landwalk) result.getFirst();
            assertThat(lw.selector()).isEqualTo(LandSelector.ofSubtype(LandType.FOREST));
        }

        @Test
        void parsesQualifiedLandwalk() {
            var result = OracleParser.parse("Test", "Nonbasic landwalk");
            assertThat(result).hasSize(1);
            var lw = (Ability.Landwalk) result.getFirst();
            assertThat(lw.selector()).isEqualTo(new LandSelector(null, true, null, null));
        }

        @Test
        void parsesSnowBasicwalk() {
            var result = OracleParser.parse("Test", "Snow swampwalk");
            assertThat(result).hasSize(1);
            var lw = (Ability.Landwalk) result.getFirst();
            assertThat(lw.selector()).isEqualTo(new LandSelector(Supertype.SNOW, false, null, LandType.SWAMP));
        }

        @Test
        void parsesProtectionFromColor() {
            var result = OracleParser.parse("Test Card", "Protection from red");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst())
                    .isEqualTo(new Ability.Protection(List.of(new ProtectionQuality.OfColor(Color.RED))));
        }

        @Test
        void parsesProtectionFromCardType() {
            var result = OracleParser.parse("Test Card", "Protection from artifacts");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst())
                    .isEqualTo(new Ability.Protection(List.of(new ProtectionQuality.OfCardType(CardType.ARTIFACT))));
        }
    }

    // ── Reminder text ─────────────────────────────────────────────────────

    @Nested
    class ReminderText {

        @Test
        void ignoresTrailingReminderOnKeyword() {
            var result = OracleParser.parse("Test Card", "Vigilance (Attacking doesn't cause this creature to tap.)");
            assertThat(result).containsExactly(Ability.StaticKeyword.VIGILANCE);
        }

        @Test
        void ignoresReminderPerKeywordInList() {
            var result = OracleParser.parse(
                    "Test Card", "Flying (can only be blocked by creatures with flying or reach.), trample");
            assertThat(result).containsExactly(Ability.StaticKeyword.FLYING, Ability.StaticKeyword.TRAMPLE);
        }

        @Test
        void pureReminderParagraphProducesNoAbilities() {
            var result = OracleParser.parse("Bayou", "({T}: Add {B} or {G}.)");
            assertThat(result).isEmpty();
        }

        @Test
        void ignoresReminderOnTriggeredAbility() {
            var result = OracleParser.parse("Test Card", "When ~ enters, draw a card. (Reminder text here.)");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isInstanceOf(Ability.TriggeredAbility.class);
        }
    }

    // ── Unknown text fails ────────────────────────────────────────────────

    @Nested
    class UnknownText {

        @Test
        void failsOnUnknownText() {
            assertThatThrownBy(() -> OracleParser.parse("Test Card", "Xyzzy fnord"))
                    .isInstanceOf(Exception.class);
        }
    }
}
