package be.imgn.mtg.engine.oracle2.parser.selector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.oracle2.domain.Ability;
import be.imgn.mtg.engine.oracle2.domain.selector.AbilitySelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectPropertySelector;

/// Direct tests for [AbilitySelectorParser#ABILITY_SELECTOR].
/// Complements the end-to-end coverage in
/// `SelectorParserTest$AbilityAxis` with sharper failure messages
/// and explicit edge cases (Oxford-comma 3+ lists, unknown
/// keywords, mixed casing) that don't ride the full SELECTOR
/// pipeline.
class AbilitySelectorParserTest {

    private static ObjectPropertySelector parse(String input) {
        return AbilitySelectorParser.ABILITY_SELECTOR.parseSkipping(CharPredicate.is(' '), input);
    }

    @Nested
    class Singleton {
        @Test
        void flying() {
            assertThat(parse("with flying")).isEqualTo(new AbilitySelector.Has(Ability.StaticKeyword.FLYING));
        }

        /// Multi-word keyword: `first strike` is two tokens but one
        /// keyword — the [AbilitySelectorParser#ABILITY_KEYWORD]
        /// `phrase("first strike")` arm consumes both.
        @Test
        void firstStrike() {
            assertThat(parse("with first strike"))
                    .isEqualTo(new AbilitySelector.Has(Ability.StaticKeyword.FIRST_STRIKE));
        }

        /// Triggered keyword goes through the same arm as static —
        /// the parser doesn't care which sub-interface the enum
        /// implements.
        @Test
        void prowess() {
            assertThat(parse("with prowess")).isEqualTo(new AbilitySelector.Has(Ability.TriggeredKeyword.PROWESS));
        }
    }

    @Nested
    class Compound {
        @Test
        void twoKeywordsOr() {
            assertThat(parse("with flying or reach"))
                    .isEqualTo(new ObjectPropertySelector.AnyOf(List.of(
                            new AbilitySelector.Has(Ability.StaticKeyword.FLYING),
                            new AbilitySelector.Has(Ability.StaticKeyword.REACH))));
        }

        @Test
        void twoKeywordsAnd() {
            assertThat(parse("with flying and vigilance"))
                    .isEqualTo(new ObjectPropertySelector.AllOf(List.of(
                            new AbilitySelector.Has(Ability.StaticKeyword.FLYING),
                            new AbilitySelector.Has(Ability.StaticKeyword.VIGILANCE))));
        }

        /// Three-element Oxford-comma `or` list — exercises the
        /// `andList` / `orList` Oxford-comma path, not just the
        /// two-element pair shape.
        @Test
        void threeKeywordsOxfordOr() {
            assertThat(parse("with flying, trample, or haste"))
                    .isEqualTo(new ObjectPropertySelector.AnyOf(List.of(
                            new AbilitySelector.Has(Ability.StaticKeyword.FLYING),
                            new AbilitySelector.Has(Ability.StaticKeyword.TRAMPLE),
                            new AbilitySelector.Has(Ability.StaticKeyword.HASTE))));
        }

        @Test
        void threeKeywordsOxfordAnd() {
            assertThat(parse("with trample, flying, and haste"))
                    .isEqualTo(new ObjectPropertySelector.AllOf(List.of(
                            new AbilitySelector.Has(Ability.StaticKeyword.TRAMPLE),
                            new AbilitySelector.Has(Ability.StaticKeyword.FLYING),
                            new AbilitySelector.Has(Ability.StaticKeyword.HASTE))));
        }
    }

    @Nested
    class Negation {
        @Test
        void withoutFlying() {
            assertThat(parse("without flying")).isEqualTo(new AbilitySelector.HasNot(Ability.StaticKeyword.FLYING));
        }

        /// Multi-word keyword behind `without`.
        @Test
        void withoutFirstStrike() {
            assertThat(parse("without first strike"))
                    .isEqualTo(new AbilitySelector.HasNot(Ability.StaticKeyword.FIRST_STRIKE));
        }

        /// `without X or Y` — De Morgan: NOT(X OR Y) = NOT X AND
        /// NOT Y. Combiner is [AllOf] (lacks both), not [AnyOf].
        /// Oracle uses this form to mean "lacks both keywords"
        /// (Stormtide Leviathan).
        @Test
        void withoutFlyingOrReach() {
            assertThat(parse("without flying or reach"))
                    .isEqualTo(new ObjectPropertySelector.AllOf(List.of(
                            new AbilitySelector.HasNot(Ability.StaticKeyword.FLYING),
                            new AbilitySelector.HasNot(Ability.StaticKeyword.REACH))));
        }

        /// Oracle never coordinates `without` with `and`; the
        /// parser deliberately doesn't accept that form so a
        /// hypothetical occurrence surfaces as a failure rather
        /// than silently parsing as if it were `or`.
        @Test
        void withoutAndIsRejected() {
            assertThatThrownBy(() -> parse("without flying and vigilance")).isInstanceOf(Exception.class);
        }
    }

    @Nested
    class Special {
        @Test
        void noAbilities() {
            assertThat(parse("with no abilities")).isEqualTo(new AbilitySelector.HasNoAbilities());
        }
    }

    @Nested
    class Failure {
        /// `with quux` doesn't match any keyword in the table — the
        /// singleton arm has nothing to map.
        @Test
        void unknownKeywordFails() {
            assertThatThrownBy(() -> parse("with quux")).isInstanceOf(Exception.class);
        }

        /// Bare keyword without `with` doesn't match — every arm of
        /// `ABILITY_SELECTOR` is anchored to `with`.
        @Test
        void bareKeywordFails() {
            assertThatThrownBy(() -> parse("flying")).isInstanceOf(Exception.class);
        }
    }
}
