package be.imgn.mtg.engine.oracle2.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.TriggerEvent;
import be.imgn.mtg.engine.oracle2.domain.selector.QuantifierSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.domain.selector.SelfSelector;
import be.imgn.mtg.engine.turn.Step;

class TriggerEventParserTest {

    private static TriggerEvent parse(String input) {
        return TriggerEventParser.TRIGGER_EVENT.parseSkipping(CharPredicate.is(' '), input);
    }

    /// `~` after card-name substitution. Always wrapped in a
    /// [QuantifierSelector] with implicit count 1 by
    /// [be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser].
    private static final Selector SELF = new QuantifierSelector(new Amount.Exact(1), SelfSelector.SELF);

    @Nested
    class Enters {
        @Test
        void selfEnters() {
            assertThat(parse("~ enters")).isEqualTo(new TriggerEvent.Enters(SELF));
        }

        @Test
        void selfEntersTheBattlefield() {
            assertThat(parse("~ enters the battlefield")).isEqualTo(new TriggerEvent.Enters(SELF));
        }
    }

    @Nested
    class Dies {
        @Test
        void selfDies() {
            assertThat(parse("~ dies")).isEqualTo(new TriggerEvent.Dies(SELF));
        }
    }

    @Nested
    class Attacks {
        @Test
        void selfAttacks() {
            assertThat(parse("~ attacks")).isEqualTo(new TriggerEvent.Attacks(SELF));
        }
    }

    @Nested
    class AtBeginningOf {
        @Test
        void yourUpkeep() {
            assertThat(parse("the beginning of your upkeep")).isEqualTo(new TriggerEvent.AtBeginningOf(Step.UPKEEP));
        }

        @Test
        void yourEndStep() {
            assertThat(parse("the beginning of your end step")).isEqualTo(new TriggerEvent.AtBeginningOf(Step.END));
        }
    }
}
