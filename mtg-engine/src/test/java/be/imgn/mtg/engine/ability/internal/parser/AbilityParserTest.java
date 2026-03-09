package be.imgn.mtg.engine.ability.internal.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.ability.SpellAbility;

@DisplayName("AbilityParser")
class AbilityParserTest {

    @Nested
    @DisplayName("Single line")
    class SingleLine {

        @Test
        @DisplayName("activated ability line → ActivatedAbility")
        void activatedAbility() {
            var abilities = AbilityParser.parse("{T}: Add {G}.");
            assertThat(abilities).hasSize(1);
            assertThat(abilities.getFirst()).isInstanceOf(ActivatedAbility.class);
        }

        @Test
        @DisplayName("spell effect line → SpellAbility")
        void spellAbility() {
            var abilities = AbilityParser.parse("Deal 3 damage to any target.");
            assertThat(abilities).hasSize(1);
            assertThat(abilities.getFirst()).isInstanceOf(SpellAbility.class);
        }
    }

    @Nested
    @DisplayName("Multiple lines")
    class MultipleLines {

        @Test
        @DisplayName("two activated abilities on separate lines")
        void twoActivated() {
            var abilities = AbilityParser.parse("{T}: Add {G}.\n{T}: Add {R}.");
            assertThat(abilities).hasSize(2);
            assertThat(abilities).allSatisfy(a -> assertThat(a).isInstanceOf(ActivatedAbility.class));
        }
    }

    @Nested
    @DisplayName("Card name self-reference")
    class CardNameSelfReference {

        @Test
        @DisplayName("card name in oracle text replaced with ~ before parsing")
        void cardNameReplaced() {
            var abilities = AbilityParser.parse("Lightning Bolt", "Lightning Bolt deals 3 damage to any target.");
            assertThat(abilities).hasSize(1);
            assertThat(abilities.getFirst()).isInstanceOf(SpellAbility.class);
        }

        @Test
        @DisplayName("null card name — no replacement, text parsed as-is")
        void nullCardName() {
            var abilities = AbilityParser.parse(null, "Deal 3 damage to any target.");
            assertThat(abilities).hasSize(1);
            assertThat(abilities.getFirst()).isInstanceOf(SpellAbility.class);
        }
    }

    @Nested
    @DisplayName("Unparseable lines")
    class UnparseableLines {

        @Test
        @DisplayName("keyword ability line is silently skipped")
        void keywordSkipped() {
            var abilities = AbilityParser.parse("Flying");
            assertThat(abilities).isEmpty();
        }

        @Test
        @DisplayName("parseable + unparseable lines → only parseable returned")
        void mixedLines() {
            var abilities = AbilityParser.parse("Flying\n{T}: Add {G}.");
            assertThat(abilities).hasSize(1);
            assertThat(abilities.getFirst()).isInstanceOf(ActivatedAbility.class);
        }

        @Test
        @DisplayName("empty oracle text → empty list")
        void emptyText() {
            var abilities = AbilityParser.parse("");
            assertThat(abilities).isEmpty();
        }

        @Test
        @DisplayName("null oracle text → empty list")
        void nullText() {
            var abilities = AbilityParser.parse(null);
            assertThat(abilities).isEmpty();
        }
    }
}
