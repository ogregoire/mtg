package be.imgn.mtg.engine.oracle;

import static com.google.common.labs.parse.Parser.digits;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.word;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WordsTest {

    private static final CharPredicate SPACE = CharPredicate.is(' ');

    @Nested
    class SequenceWithOptionalPrefix {

        record Eat(String who, String what) {}

        private static final Parser<Eat> EAT =
                Words.sequence(word().orElse("you"), word("eat").then(word()), Eat::new);

        @Test
        void matchesWithPrefix() {
            var result = EAT.parseSkipping(SPACE, "Alice eat brunch");
            assertThat(result).isEqualTo(new Eat("Alice", "brunch"));
        }

        @Test
        void matchesWithoutPrefixUsingDefault() {
            var result = EAT.parseSkipping(SPACE, "eat brunch");
            assertThat(result).isEqualTo(new Eat("you", "brunch"));
        }

        @Test
        void failsWhenRequiredPartMissing() {
            assertThatThrownBy(() -> EAT.parseSkipping(SPACE, "Alice")).isInstanceOf(Exception.class);
        }

        @Test
        void failsOnEmptyInput() {
            assertThatThrownBy(() -> EAT.parseSkipping(SPACE, "")).isInstanceOf(Exception.class);
        }

        @Test
        void differentDefaultValue() {
            Parser<Eat> parser =
                    Words.sequence(word().orElse("nobody"), word("eat").then(word()), Eat::new);
            var result = parser.parseSkipping(SPACE, "eat cake");
            assertThat(result).isEqualTo(new Eat("nobody", "cake"));
        }

        @Test
        void prefixMatchDoesNotConsumeRequiredInput() {
            // "eat" could match the prefix (word()), but then the required part
            // ("eat" + word) would fail. Should fall back to default prefix.
            var result = EAT.parseSkipping(SPACE, "eat lunch");
            assertThat(result).isEqualTo(new Eat("you", "lunch"));
        }

        @Test
        void worksWithThreeArgSequenceOnRight() {
            record Pay(String who, String whom, int amount) {}

            Parser<Pay> pay = Words.sequence(
                    word().orElse("you"),
                    sequence(word("pay").then(word()), digits().map(Integer::parseInt), Map::entry),
                    (who, entry) -> new Pay(who, entry.getKey(), entry.getValue()));

            assertThat(pay.parseSkipping(SPACE, "Bob pay Alice 50")).isEqualTo(new Pay("Bob", "Alice", 50));
            assertThat(pay.parseSkipping(SPACE, "pay Alice 50")).isEqualTo(new Pay("you", "Alice", 50));
        }
    }
}
