package be.imgn.mtg.engine.ability.internal.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.internal.parser.effect.DiscardEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.DrawEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.ScryEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.SearchLibraryEffect;
import be.imgn.mtg.engine.ability.internal.parser.reference.PlayerReference;
import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.ability.internal.parser.selector.TypeMatcher;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.parse.CharPredicate;

@DisplayName("DrawParser")
class DrawParserTest {

    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    private DrawEffect parseDraw(String text) {
        return DrawParser.DRAW_EFFECT.parseSkipping(WHITESPACE, text);
    }

    private DiscardEffect parseDiscard(String text) {
        return DrawParser.DISCARD_EFFECT.parseSkipping(WHITESPACE, text);
    }

    private ScryEffect parseScry(String text) {
        return DrawParser.SCRY_EFFECT.parseSkipping(WHITESPACE, text);
    }

    private SearchLibraryEffect parseSearchLibrary(String text) {
        return DrawParser.SEARCH_LIBRARY_EFFECT.parseSkipping(WHITESPACE, text);
    }

    @Nested
    @DisplayName("Draw")
    class Draw {

        @Test
        @DisplayName("Draw two cards.")
        void drawTwoCards() {
            var effect = parseDraw("Draw two cards.");

            assertThat(effect.player()).isEmpty();
            assertThat(effect.amount()).isEqualTo(new Amount.Exact(2));
        }

        @Test
        @DisplayName("Draw a card.")
        void drawACard() {
            var effect = parseDraw("Draw a card.");

            assertThat(effect.player()).isEmpty();
            assertThat(effect.amount()).isEqualTo(new Amount.Exact(1));
        }

        @Test
        @DisplayName("Target player draws three cards.")
        void targetPlayerDrawsThreeCards() {
            var effect = parseDraw("Target player draws three cards.");

            assertThat(effect.player()).isEqualTo(Optional.of(PlayerReference.TARGET_PLAYER));
            assertThat(effect.amount()).isEqualTo(new Amount.Exact(3));
        }

        @Test
        @DisplayName("Draw X cards.")
        void drawXCards() {
            var effect = parseDraw("Draw X cards.");

            assertThat(effect.player()).isEmpty();
            assertThat(effect.amount()).isEqualTo(new Amount.XValue());
        }
    }

    @Nested
    @DisplayName("Discard")
    class Discard {

        @Test
        @DisplayName("Discard a card.")
        void discardACard() {
            var effect = parseDiscard("Discard a card.");

            assertThat(effect.player()).isEmpty();
            assertThat(effect.amount()).isEqualTo(new Amount.Exact(1));
        }

        @Test
        @DisplayName("Target player discards two cards.")
        void targetPlayerDiscardsTwoCards() {
            var effect = parseDiscard("Target player discards two cards.");

            assertThat(effect.player()).isEqualTo(Optional.of(PlayerReference.TARGET_PLAYER));
            assertThat(effect.amount()).isEqualTo(new Amount.Exact(2));
        }

        @Test
        @DisplayName("Discard three cards.")
        void discardThreeCards() {
            var effect = parseDiscard("Discard three cards.");

            assertThat(effect.player()).isEmpty();
            assertThat(effect.amount()).isEqualTo(new Amount.Exact(3));
        }
    }

    @Nested
    @DisplayName("Scry")
    class Scry {

        @Test
        @DisplayName("Scry 2.")
        void scryTwo() {
            var effect = parseScry("Scry 2.");

            assertThat(effect.amount()).isEqualTo(new Amount.Exact(2));
        }

        @Test
        @DisplayName("Scry X.")
        void scryX() {
            var effect = parseScry("Scry X.");

            assertThat(effect.amount()).isEqualTo(new Amount.XValue());
        }
    }

    @Nested
    @DisplayName("Search library")
    class SearchLibrary {

        @Test
        @DisplayName("Search your library for a card.")
        void searchLibraryForACard() {
            var effect = parseSearchLibrary("Search your library for a card.");

            assertThat(effect.cardType()).isEmpty();
        }

        @Test
        @DisplayName("Search your library for a creature card.")
        void searchLibraryForACreatureCard() {
            var effect = parseSearchLibrary("Search your library for a creature card.");

            assertThat(effect.cardType()).isEqualTo(Optional.of(new TypeMatcher.Single(Type.CREATURE)));
        }

        @Test
        @DisplayName("Search your library for an artifact card.")
        void searchLibraryForAnArtifactCard() {
            var effect = parseSearchLibrary("Search your library for an artifact card.");

            assertThat(effect.cardType()).isEqualTo(Optional.of(new TypeMatcher.Single(Type.ARTIFACT)));
        }
    }
}
