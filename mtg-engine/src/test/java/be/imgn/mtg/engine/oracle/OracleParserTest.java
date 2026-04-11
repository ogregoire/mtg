package be.imgn.mtg.engine.oracle;

import static org.assertj.core.api.Assertions.assertThat;

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

    // ── Keyword abilities ─────────────────────────────────────────────────

    @Nested
    class KeywordAbilities {

        @Test
        void parsesFlying() {
            var result = OracleParser.parse("Test Card", "Flying");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isInstanceOf(Ability.Keyword.class);
            var keyword = (Ability.Keyword) result.getFirst();
            assertThat(keyword.name()).isEqualTo("Flying");
        }

        @Test
        void parsesTrample() {
            var result = OracleParser.parse("Test Card", "Trample");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isInstanceOf(Ability.Keyword.class);
        }
    }

    // ── Activated abilities ───────────────────────────────────────────────

    @Nested
    class ActivatedAbilities {

        @Test
        void parsesTapAddGreen() {
            var result = OracleParser.parse("Llanowar Elves", "{T}: Add {G}.");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isInstanceOf(Ability.Activated.class);
            var activated = (Ability.Activated) result.getFirst();
            assertThat(activated.cost()).isInstanceOf(Cost.TapSelf.class);
            assertThat(activated.effects()).hasSize(1);
            assertThat(activated.effects().getFirst()).isInstanceOf(Effect.AddMana.class);
        }

        @Test
        void parsesTwoWhiteTapEffect() {
            var result = OracleParser.parse("Test Card", "{2}{W}, {T}: Draw a card.");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isInstanceOf(Ability.Activated.class);
            var activated = (Ability.Activated) result.getFirst();
            assertThat(activated.cost()).isInstanceOf(Cost.Compound.class);
        }
    }

    // ── Spell abilities ───────────────────────────────────────────────────

    @Nested
    class SpellAbilities {

        @Test
        void parsesDealDamageAnyTarget() {
            // Using a dummy name so ~ replacement doesn't apply
            var result = OracleParser.parse("Dummy", "Deal 3 damage to any target.");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isInstanceOf(Ability.Spell.class);
            var spell = (Ability.Spell) result.getFirst();
            assertThat(spell.effects()).hasSize(1);
            assertThat(spell.effects().getFirst()).isInstanceOf(Effect.DealDamage.class);
        }

        @Test
        void parsesDestroyTargetCreature() {
            var result = OracleParser.parse("Dummy", "Destroy target creature.");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isInstanceOf(Ability.Spell.class);
            var spell = (Ability.Spell) result.getFirst();
            assertThat(spell.effects().getFirst()).isInstanceOf(Effect.Destroy.class);
        }

        @Test
        void parsesExileTargetCreature() {
            var result = OracleParser.parse("Dummy", "Exile target creature.");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isInstanceOf(Ability.Spell.class);
            var spell = (Ability.Spell) result.getFirst();
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
            assertThat(result.getFirst()).isInstanceOf(Ability.Triggered.class);
            var triggered = (Ability.Triggered) result.getFirst();
            assertThat(triggered.triggerWord()).isEqualToIgnoringCase("when");
            assertThat(triggered.effects()).hasSize(1);
            assertThat(triggered.effects().getFirst()).isInstanceOf(Effect.Draw.class);
        }

        @Test
        void parsesWheneverCreatureEnters() {
            var result = OracleParser.parse("Test Card", "Whenever a creature enters, draw a card.");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isInstanceOf(Ability.Triggered.class);
            var triggered = (Ability.Triggered) result.getFirst();
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
            assertThat(result.getFirst()).isInstanceOf(Ability.Spell.class);
            var spell = (Ability.Spell) result.getFirst();
            assertThat(spell.effects().getFirst()).isInstanceOf(Effect.DealDamage.class);
            var dd = (Effect.DealDamage) spell.effects().getFirst();
            assertThat(dd.source()).isInstanceOf(Subject.SelfRef.class);
        }
    }

    // ── Multi-paragraph ───────────────────────────────────────────────────

    @Nested
    class MultiParagraph {

        @Test
        void parsesTwoParagraphsAsTwo() {
            var text = "Flying\nWhen ~ enters, draw a card.";
            var result = OracleParser.parse("Test Card", text);
            assertThat(result).hasSize(2);
            assertThat(result.get(0)).isInstanceOf(Ability.Keyword.class);
            assertThat(result.get(1)).isInstanceOf(Ability.Triggered.class);
        }

        @Test
        void parsesThreeParagraphs() {
            var text = "Flying\nTrample\nWhen ~ enters, draw a card.";
            var result = OracleParser.parse("Test Card", text);
            assertThat(result).hasSize(3);
        }

        @Test
        void skipsEmptyLines() {
            var text = "Flying\n\nWhen ~ enters, draw a card.";
            var result = OracleParser.parse("Test Card", text);
            assertThat(result).hasSize(2);
        }
    }

    // ── Fallback to keyword for unparseable ───────────────────────────────

    @Nested
    class FallbackKeyword {

        @Test
        void fallsBackToKeywordForUnknownText() {
            var result = OracleParser.parse("Test Card", "Undying");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isInstanceOf(Ability.Keyword.class);
            var keyword = (Ability.Keyword) result.getFirst();
            assertThat(keyword.name()).isEqualTo("Undying");
        }
    }
}
