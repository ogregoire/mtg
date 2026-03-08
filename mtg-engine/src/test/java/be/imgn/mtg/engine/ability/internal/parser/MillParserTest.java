package be.imgn.mtg.engine.ability.internal.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.effect.MillEffect;
import be.imgn.mtg.engine.selector.PlayerReference;

@DisplayName("MillParser")
class MillParserTest {

    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    private MillEffect parse(String text) {
        return MillParser.MILL_EFFECT.parseSkipping(WHITESPACE, text);
    }

    @Nested
    @DisplayName("Basic patterns")
    class BasicPatterns {

        @Test
        @DisplayName("Mill three cards.")
        void millThreeCards() {
            var effect = parse("Mill three cards.");

            assertThat(effect.player()).isEmpty();
            assertThat(effect.amount()).isEqualTo(new Amount.Exact(3));
        }

        @Test
        @DisplayName("Mill 5 cards.")
        void millFiveCardsDigit() {
            var effect = parse("Mill 5 cards.");

            assertThat(effect.player()).isEmpty();
            assertThat(effect.amount()).isEqualTo(new Amount.Exact(5));
        }

        @Test
        @DisplayName("Mill X cards.")
        void millXCards() {
            var effect = parse("Mill X cards.");

            assertThat(effect.player()).isEmpty();
            assertThat(effect.amount()).isEqualTo(Amount.X);
        }
    }

    @Nested
    @DisplayName("With player reference")
    class WithPlayerReference {

        @Test
        @DisplayName("Target player mills five cards.")
        void targetPlayerMillsFiveCards() {
            var effect = parse("Target player mills five cards.");

            assertThat(effect.player()).isEqualTo(Optional.of(PlayerReference.TARGET_PLAYER));
            assertThat(effect.amount()).isEqualTo(new Amount.Exact(5));
        }

        @Test
        @DisplayName("Each player mills three cards.")
        void eachPlayerMillsThreeCards() {
            var effect = parse("Each player mills three cards.");

            assertThat(effect.player()).isEqualTo(Optional.of(PlayerReference.EACH_PLAYER));
            assertThat(effect.amount()).isEqualTo(new Amount.Exact(3));
        }
    }
}
