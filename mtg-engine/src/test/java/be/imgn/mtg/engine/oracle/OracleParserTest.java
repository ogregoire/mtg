package be.imgn.mtg.engine.oracle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
            assertThat(activated.cost()).isInstanceOf(Cost.Compound.class);
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
            assertThat(result).containsExactly(Ability.Flying.FLYING);
        }

        @Test
        void parsesMultiWordKeyword() {
            var result = OracleParser.parse("Test Card", "First strike");
            assertThat(result).containsExactly(Ability.FirstStrike.FIRST_STRIKE);
        }

        @Test
        void parsesTriggeredKeyword() {
            var result = OracleParser.parse("Test Card", "Prowess");
            assertThat(result).containsExactly(Ability.Prowess.PROWESS);
            assertThat(result.getFirst()).isInstanceOf(Ability.Triggered.class);
        }

        @Test
        void parsesKeywordList() {
            var result = OracleParser.parse("Test Card", "Flying, trample, haste");
            assertThat(result).containsExactly(Ability.Flying.FLYING, Ability.Trample.TRAMPLE, Ability.Haste.HASTE);
        }

        @Test
        void parsesBaneslayerAngelLine() {
            var result = OracleParser.parse(
                    "Baneslayer Angel", "Flying, first strike, lifelink, protection from Demons and from Dragons");
            assertThat(result).hasSize(4);
            assertThat(result.get(0)).isEqualTo(Ability.Flying.FLYING);
            assertThat(result.get(1)).isEqualTo(Ability.FirstStrike.FIRST_STRIKE);
            assertThat(result.get(2)).isEqualTo(Ability.Lifelink.LIFELINK);
            var protection = (Ability.Protection) result.get(3);
            assertThat(protection.qualities())
                    .containsExactly(
                            new ProtectionQuality.OfSubtype("Demons"), new ProtectionQuality.OfSubtype("Dragons"));
        }

        @Test
        void parsesWardMana() {
            var result = OracleParser.parse("Test Card", "Ward {2}");
            assertThat(result).hasSize(1);
            var ward = (Ability.Ward) result.getFirst();
            assertThat(ward.cost()).containsExactly(new ManaSymbol("{2}"));
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
            assertThat(result.getFirst()).isEqualTo(new Ability.Enchant("creature"));
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
            assertThat(result).containsExactly(Ability.Vigilance.VIGILANCE);
        }

        @Test
        void ignoresReminderPerKeywordInList() {
            var result = OracleParser.parse(
                    "Test Card", "Flying (can only be blocked by creatures with flying or reach.), trample");
            assertThat(result).containsExactly(Ability.Flying.FLYING, Ability.Trample.TRAMPLE);
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
