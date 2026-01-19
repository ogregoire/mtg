package be.imgn.mtg.engine.ability.internal.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.internal.parser.effect.AddCountersEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.RemoveCountersEffect;
import be.imgn.mtg.engine.ability.internal.parser.reference.PronounType;
import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.ability.internal.parser.selector.Qualifier;
import be.imgn.mtg.engine.ability.internal.parser.selector.TypeMatcher;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.parse.CharPredicate;

@DisplayName("CounterParser")
class CounterParserTest {

    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    private AddCountersEffect parseAddCounters(String text) {
        return CounterParser.ADD_COUNTERS_EFFECT.parseSkipping(WHITESPACE, text);
    }

    private RemoveCountersEffect parseRemoveCounters(String text) {
        return CounterParser.REMOVE_COUNTERS_EFFECT.parseSkipping(WHITESPACE, text);
    }

    @Nested
    @DisplayName("Add counters")
    class AddCounters {

        @Test
        @DisplayName("Put a +1/+1 counter on target creature.")
        void putPlusOneCounterOnTargetCreature() {
            var effect = parseAddCounters("Put a +1/+1 counter on target creature.");

            assertThat(effect.amount()).isEqualTo(new Amount.Exact(1));
            assertThat(effect.counterType()).isEqualTo("+1/+1");
            var select = (Subject.Select) effect.subject();
            assertThat(select.selector().qualifiers()).containsExactly(new Qualifier.Target());
            assertThat(select.selector().typeMatcher()).isEqualTo(new TypeMatcher.Single(Type.CREATURE));
        }

        @Test
        @DisplayName("Put two +1/+1 counters on target creature.")
        void putTwoPlusOneCountersOnTargetCreature() {
            var effect = parseAddCounters("Put two +1/+1 counters on target creature.");

            assertThat(effect.amount()).isEqualTo(new Amount.Exact(2));
            assertThat(effect.counterType()).isEqualTo("+1/+1");
        }

        @Test
        @DisplayName("Put X +1/+1 counters on it.")
        void putXCountersOnIt() {
            var effect = parseAddCounters("Put X +1/+1 counters on it.");

            assertThat(effect.amount()).isEqualTo(new Amount.XValue());
            assertThat(effect.subject()).isEqualTo(new Subject.Pronoun(PronounType.IT));
        }

        @Test
        @DisplayName("Put a loyalty counter on target permanent.")
        void putLoyaltyCounterOnPermanent() {
            var effect = parseAddCounters("Put a loyalty counter on target permanent.");

            assertThat(effect.amount()).isEqualTo(new Amount.Exact(1));
            assertThat(effect.counterType()).isEqualTo("loyalty");
        }
    }

    @Nested
    @DisplayName("Remove counters")
    class RemoveCounters {

        @Test
        @DisplayName("Remove a +1/+1 counter from target creature.")
        void removeCounterFromCreature() {
            var effect = parseRemoveCounters("Remove a +1/+1 counter from target creature.");

            assertThat(effect.amount()).isEqualTo(new Amount.Exact(1));
            assertThat(effect.counterType()).isEqualTo("+1/+1");
            var select = (Subject.Select) effect.subject();
            assertThat(select.selector().qualifiers()).containsExactly(new Qualifier.Target());
        }

        @Test
        @DisplayName("Remove two -1/-1 counters from it.")
        void removeTwoMinusCountersFromIt() {
            var effect = parseRemoveCounters("Remove two -1/-1 counters from it.");

            assertThat(effect.amount()).isEqualTo(new Amount.Exact(2));
            assertThat(effect.counterType()).isEqualTo("-1/-1");
            assertThat(effect.subject()).isEqualTo(new Subject.Pronoun(PronounType.IT));
        }
    }
}
