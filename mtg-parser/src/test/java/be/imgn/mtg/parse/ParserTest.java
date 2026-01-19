package be.imgn.mtg.parse;

import static be.imgn.mtg.parse.CharacterSet.charsIn;
import static be.imgn.mtg.parse.Parser.anyOf;
import static be.imgn.mtg.parse.Parser.chars;
import static be.imgn.mtg.parse.Parser.consecutive;
import static be.imgn.mtg.parse.Parser.digits;
import static be.imgn.mtg.parse.Parser.first;
import static be.imgn.mtg.parse.Parser.quotedBy;
import static be.imgn.mtg.parse.Parser.quotedByWithEscapes;
import static be.imgn.mtg.parse.Parser.sequence;
import static be.imgn.mtg.parse.Parser.string;
import static be.imgn.mtg.parse.Parser.word;
import static be.imgn.mtg.parse.Parser.zeroOrMore;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.StringReader;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ParserTest {

    // ===== String Parser Tests =====

    @Test
    void string_success() {
        Parser<String> parser = string("foo");
        assertThat(parser.parse("foo")).isEqualTo("foo");
        assertThat(parser.parseToStream("foo").toList()).containsExactly("foo");
        assertThat(parser.parseToStream("").toList()).isEmpty();
    }

    @Test
    void string_success_source() {
        Parser<String> parser = string("foo");
        assertThat(parser.source().parse("foo")).isEqualTo("foo");
        assertThat(parser.source().parseToStream("foo").toList()).containsExactly("foo");
    }

    @Test
    void string_failure_withLeftover() {
        assertThatThrownBy(() -> string("foo").parse("fooa")).isInstanceOf(Parser.ParseException.class);
        assertThatThrownBy(() -> string("foo").parseToStream("fooa").toList())
                .isInstanceOf(Parser.ParseException.class);
    }

    @Test
    void string_failure() {
        assertThatThrownBy(() -> string("foo").parse("fo")).isInstanceOf(Parser.ParseException.class);
        assertThatThrownBy(() -> string("foo").parse("food")).isInstanceOf(Parser.ParseException.class);
        assertThatThrownBy(() -> string("foo").parse("bar")).isInstanceOf(Parser.ParseException.class);
    }

    @Test
    void string_cannotBeEmpty() {
        assertThatThrownBy(() -> string("")).isInstanceOf(IllegalArgumentException.class);
    }

    // ===== Word Parser Tests =====

    @Test
    void word_success() {
        assertThat(word("foo").parse("foo")).isEqualTo("foo");
    }

    @Test
    void word_failIfFollowedByWordChar() {
        assertThatThrownBy(() -> word("foo").parse("foobar")).isInstanceOf(Parser.ParseException.class);
        assertThatThrownBy(() -> word("foo").parse("foo_bar")).isInstanceOf(Parser.ParseException.class);
        assertThatThrownBy(() -> word("foo").parse("foo1")).isInstanceOf(Parser.ParseException.class);
    }

    @Test
    void word_successIfNotFollowedByWordChar() {
        assertThat(word("foo").probe("foo?").toList()).containsExactly("foo");
        assertThat(word("foo").probe("foo-").toList()).containsExactly("foo");
    }

    @Test
    void word_skipping_success() {
        assertThat(word("foo")
                        .skipping(Character::isWhitespace)
                        .parseToStream("foo")
                        .toList())
                .containsExactly("foo");
        assertThat(word("foo")
                        .skipping(Character::isWhitespace)
                        .parseToStream("foo foo")
                        .toList())
                .containsExactly("foo", "foo");
        assertThat(word("foo")
                        .skipping(Character::isWhitespace)
                        .probe(" foo-foo")
                        .toList())
                .containsExactly("foo");
    }

    // ===== First Parser Tests =====

    @Test
    void first_atBeginning_followedBy() {
        assertThat(first("foo").followedBy(string("bar")).parse("foobar")).isEqualTo("foo");
    }

    @Test
    void first_severalCharsIn_followedBy() {
        assertThat(first("foo").followedBy(string("bar")).parse("skip foobar")).isEqualTo("foo");
    }

    @Test
    void first_notFound() {
        assertThatThrownBy(() -> first("skip").then(first("foo")).parse("skip fobar"))
                .isInstanceOf(Parser.ParseException.class)
                .hasMessageContaining("1:5")
                .hasMessageContaining("expecting <foo>");
    }

    @Test
    void first_emptyString_throws() {
        assertThatThrownBy(() -> first("")).isInstanceOf(IllegalArgumentException.class);
    }

    // ===== Digits Parser Tests =====

    @Test
    void digits_success() {
        assertThat(digits().parse("123")).isEqualTo("123");
        assertThat(digits().parseToStream("123").toList()).containsExactly("123");
    }

    @Test
    void digits_failure() {
        assertThatThrownBy(() -> digits().parse("abc")).isInstanceOf(Parser.ParseException.class);
    }

    // ===== Chars Parser Tests =====

    @Test
    void chars_success() {
        assertThat(chars(3).parse("abc")).isEqualTo("abc");
        assertThat(chars(5).parse("hello")).isEqualTo("hello");
    }

    @Test
    void chars_failure_notEnoughChars() {
        assertThatThrownBy(() -> chars(5).parse("abc")).isInstanceOf(Parser.ParseException.class);
    }

    @Test
    void chars_mustBePositive() {
        assertThatThrownBy(() -> chars(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> chars(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    // ===== Map Tests =====

    @Test
    void map_success() {
        Parser<Integer> parser = string("123").map(Integer::parseInt);
        assertThat(parser.parse("123")).isEqualTo(123);
        assertThat(parser.parseToStream("123").toList()).containsExactly(123);
    }

    @Test
    void map_success_source() {
        Parser<Integer> parser = string("123").map(Integer::parseInt);
        assertThat(parser.source().parse("123")).isEqualTo("123");
    }

    @Test
    void map_failure() {
        assertThatThrownBy(() -> string("abc").map(Integer::parseInt).parse("def"))
                .isInstanceOf(Parser.ParseException.class);
    }

    // ===== FlatMap Tests =====

    @Test
    void flatMap_success() {
        Parser<String> parser = digits().flatMap(number -> string("=" + number));
        assertThat(parser.parse("123=123")).isEqualTo("=123");
    }

    @Test
    void flatMap_success_source() {
        Parser<String> parser = digits().flatMap(number -> string("=" + number));
        assertThat(parser.source().parse("123=123")).isEqualTo("123=123");
    }

    @Test
    void flatMap_failure() {
        Parser<String> parser = digits().flatMap(number -> string("=" + number));
        assertThatThrownBy(() -> parser.parse("=123")).isInstanceOf(Parser.ParseException.class);
        assertThatThrownBy(() -> parser.parse("123=124")).isInstanceOf(Parser.ParseException.class);
    }

    // ===== ThenReturn Tests =====

    @Test
    void thenReturn_success() {
        Parser<Integer> parser = string("one").thenReturn(1);
        assertThat(parser.parse("one")).isEqualTo(1);
        assertThat(parser.parseToStream("one").toList()).containsExactly(1);
    }

    @Test
    void thenReturn_failure() {
        assertThatThrownBy(() -> string("one").thenReturn(1).parse("two")).isInstanceOf(Parser.ParseException.class);
    }

    // ===== Then Tests =====

    @Test
    void then_success() {
        Parser<Integer> parser = string("value:").then(string("123").map(Integer::parseInt));
        assertThat(parser.parse("value:123")).isEqualTo(123);
    }

    @Test
    void then_success_source() {
        Parser<Integer> parser = string("value:").then(string("123").map(Integer::parseInt));
        assertThat(parser.source().parse("value:123")).isEqualTo("value:123");
    }

    @Test
    void then_failure() {
        Parser<Integer> parser = string("value:").then(string("123").map(Integer::parseInt));
        assertThatThrownBy(() -> parser.parse("value:abc")).isInstanceOf(Parser.ParseException.class);
        assertThatThrownBy(() -> parser.parse("val:123")).isInstanceOf(Parser.ParseException.class);
    }

    @Test
    void then_orEmpty_p2MatchesZeroTimes() {
        Parser<List<String>> parser = string("a").then(string("b").zeroOrMore());
        assertThat(parser.parse("a")).isEmpty();
    }

    // ===== SuchThat Tests =====

    @Test
    void suchThat_conditionSucceeds() {
        Set<String> magicNumbers = Set.of("888", "911");
        Parser<String> parser = digits().suchThat(magicNumbers::contains, "magic");
        assertThat(parser.parse("888")).isEqualTo("888");
        assertThat(parser.skipping(Character::isWhitespace)
                        .parseToStream("911 888")
                        .toList())
                .containsExactly("911", "888");
    }

    @Test
    void suchThat_conditionFails() {
        Parser<Integer> parser = string("23").map(Integer::parseInt).suchThat(i -> i > 100, "larger than 100");
        assertThatThrownBy(() -> parser.parse("23"))
                .isInstanceOf(Parser.ParseException.class)
                .hasMessageContaining("expecting <larger than 100>");
    }

    @Test
    void suchThat_parserFails() {
        Set<String> keywords = Set.of("if", "else");
        Parser<String> parser = word().suchThat(keywords::contains, "keyword");
        assertThatThrownBy(() -> parser.parse("b"))
                .isInstanceOf(Parser.ParseException.class)
                .hasMessageContaining("expecting <keyword>");
    }

    // ===== FollowedBy Tests =====

    @Test
    void followedBy_success() {
        assertThat(string("foo").followedBy("bar").parse("foobar")).isEqualTo("foo");
    }

    @Test
    void followedBy_failure() {
        assertThatThrownBy(() -> string("foo").followedBy("bar").parse("foobaz"))
                .isInstanceOf(Parser.ParseException.class);
    }

    @Test
    void optionallyFollowedBy_withSuffix() {
        assertThat(string("foo").optionallyFollowedBy("bar").parse("foobar")).isEqualTo("foo");
    }

    @Test
    void optionallyFollowedBy_withoutSuffix() {
        assertThat(string("foo").optionallyFollowedBy("bar").parse("foo")).isEqualTo("foo");
    }

    // ===== NotFollowedBy Tests =====

    @Test
    void notFollowedBy_success() {
        assertThat(string("foo").notFollowedBy("bar").probe("foobaz").toList()).containsExactly("foo");
    }

    @Test
    void notFollowedBy_failure() {
        assertThat(string("foo").notFollowedBy("bar").probe("foobar").toList()).isEmpty();
    }

    // ===== Sequence Tests =====

    @Test
    void sequence_twoArgs_success() {
        Parser<String> parser = sequence(string("a"), string("b"), (a, b) -> a + b);
        assertThat(parser.parse("ab")).isEqualTo("ab");
    }

    @Test
    void sequence_threeArgs_success() {
        Parser<String> parser = sequence(string("a"), string("b"), string("c"), (a, b, c) -> a + b + c);
        assertThat(parser.parse("abc")).isEqualTo("abc");
    }

    @Test
    void sequence_fourArgs_success() {
        Parser<String> parser =
                sequence(string("a"), string("b"), string("c"), string("d"), (a, b, c, d) -> a + b + c + d);
        assertThat(parser.parse("abcd")).isEqualTo("abcd");
    }

    // ===== Or Tests =====

    @Test
    void or_firstMatches() {
        Parser<String> parser = string("foo").or(string("bar"));
        assertThat(parser.parse("foo")).isEqualTo("foo");
    }

    @Test
    void or_secondMatches() {
        Parser<String> parser = string("foo").or(string("bar"));
        assertThat(parser.parse("bar")).isEqualTo("bar");
    }

    @Test
    void or_neitherMatches() {
        Parser<String> parser = string("foo").or(string("bar"));
        assertThatThrownBy(() -> parser.parse("baz")).isInstanceOf(Parser.ParseException.class);
    }

    // ===== AnyOf Tests =====

    @Test
    void anyOf_success() {
        Parser<String> parser = anyOf(string("a"), string("b"), string("c"));
        assertThat(parser.parse("a")).isEqualTo("a");
        assertThat(parser.parse("b")).isEqualTo("b");
        assertThat(parser.parse("c")).isEqualTo("c");
    }

    @Test
    void anyOf_failure() {
        Parser<String> parser = anyOf(string("a"), string("b"), string("c"));
        assertThatThrownBy(() -> parser.parse("d")).isInstanceOf(Parser.ParseException.class);
    }

    // ===== AtLeastOnce Tests =====

    @Test
    void atLeastOnce_success() {
        Parser<List<String>> parser = string("a").atLeastOnce();
        assertThat(parser.parse("a")).containsExactly("a");
        assertThat(parser.parse("aaa")).containsExactly("a", "a", "a");
    }

    @Test
    void atLeastOnce_failure() {
        Parser<List<String>> parser = string("a").atLeastOnce();
        assertThatThrownBy(() -> parser.parse("b")).isInstanceOf(Parser.ParseException.class);
    }

    @Test
    void atLeastOnce_withReducer() {
        Parser<Integer> parser = digits().map(Integer::parseInt).atLeastOnce(Integer::sum);
        assertThat(parser.parseSkipping(Character::isWhitespace, "1 2 3")).isEqualTo(6);
    }

    // ===== AtLeastOnceDelimitedBy Tests =====

    @Test
    void atLeastOnceDelimitedBy_success() {
        Parser<List<String>> parser = word().atLeastOnceDelimitedBy(",");
        assertThat(parser.parse("a,b,c")).containsExactly("a", "b", "c");
    }

    @Test
    void atLeastOnceDelimitedBy_singleElement() {
        Parser<List<String>> parser = word().atLeastOnceDelimitedBy(",");
        assertThat(parser.parse("a")).containsExactly("a");
    }

    // ===== ZeroOrMore Tests =====

    @Test
    void zeroOrMore_success() {
        Parser<List<String>>.OrEmpty parser = string("a").zeroOrMore();
        assertThat(parser.parse("")).isEmpty();
        assertThat(parser.parse("a")).containsExactly("a");
        assertThat(parser.parse("aaa")).containsExactly("a", "a", "a");
    }

    @Test
    void zeroOrMore_charPredicate() {
        Parser<String>.OrEmpty parser = zeroOrMore(CharPredicate.ASCII_LETTER, "letters");
        assertThat(parser.parse("")).isEmpty();
        assertThat(parser.parse("abc")).isEqualTo("abc");
    }

    // ===== ZeroOrMoreDelimitedBy Tests =====

    @Test
    void zeroOrMoreDelimitedBy_success() {
        Parser<List<String>>.OrEmpty parser = word().zeroOrMoreDelimitedBy(",");
        assertThat(parser.parse("")).isEmpty();
        assertThat(parser.parse("a")).containsExactly("a");
        assertThat(parser.parse("a,b,c")).containsExactly("a", "b", "c");
    }

    // ===== OrElse Tests =====

    @Test
    void orElse_matches() {
        Parser<String>.OrEmpty parser = string("foo").orElse("default");
        assertThat(parser.parse("foo")).isEqualTo("foo");
    }

    @Test
    void orElse_default() {
        Parser<String>.OrEmpty parser = string("foo").orElse("default");
        assertThat(parser.parse("")).isEqualTo("default");
    }

    // ===== Optional Tests =====

    @Test
    void optional_present() {
        Parser<Optional<String>>.OrEmpty parser = string("foo").optional();
        assertThat(parser.parse("foo")).contains("foo");
    }

    @Test
    void optional_absent() {
        Parser<Optional<String>>.OrEmpty parser = string("foo").optional();
        assertThat(parser.parse("")).isEmpty();
    }

    // ===== Between Tests =====

    @Test
    void between_success() {
        Parser<String> parser = word().between("(", ")");
        assertThat(parser.parse("(foo)")).isEqualTo("foo");
    }

    @Test
    void between_failure_noParen() {
        Parser<String> parser = word().between("(", ")");
        assertThatThrownBy(() -> parser.parse("foo")).isInstanceOf(Parser.ParseException.class);
    }

    @Test
    void between_failure_unclosed() {
        Parser<String> parser = word().between("(", ")");
        assertThatThrownBy(() -> parser.parse("(foo")).isInstanceOf(Parser.ParseException.class);
    }

    @Test
    void immediatelyBetween_success() {
        Parser<String> parser = word().immediatelyBetween("<", ">");
        assertThat(parser.parse("<foo>")).isEqualTo("foo");
    }

    // ===== Prefix/Postfix Tests =====

    @Test
    void prefix_success() {
        Parser<Integer> parser =
                digits().map(Integer::parseInt).prefix(string("-").thenReturn((Integer i) -> -i));
        assertThat(parser.parse("-123")).isEqualTo(-123);
        assertThat(parser.parse("--123")).isEqualTo(123);
        assertThat(parser.parse("123")).isEqualTo(123);
    }

    @Test
    void postfix_success() {
        Parser<Integer> parser =
                digits().map(Integer::parseInt).postfix(string("++").thenReturn((Integer i) -> i + 1));
        assertThat(parser.parse("123")).isEqualTo(123);
        assertThat(parser.parse("123++")).isEqualTo(124);
        assertThat(parser.parse("123++++")).isEqualTo(125);
    }

    // ===== Consecutive Tests =====

    @Test
    void consecutive_success() {
        Parser<String> parser = consecutive(CharPredicate.ASCII_LETTER, "letters");
        assertThat(parser.parse("abc")).isEqualTo("abc");
        assertThat(parser.parse("HelloWorld")).isEqualTo("HelloWorld");
    }

    @Test
    void consecutive_failure() {
        Parser<String> parser = consecutive(CharPredicate.ASCII_LETTER, "letters");
        assertThatThrownBy(() -> parser.parse("123")).isInstanceOf(Parser.ParseException.class);
    }

    @Test
    void consecutive_characterSet() {
        Parser<String> parser = consecutive(charsIn("[a-z]"));
        assertThat(parser.parse("abc")).isEqualTo("abc");
    }

    // ===== QuotedBy Tests =====

    @Test
    void quotedBy_success() {
        Parser<String> parser = quotedBy('"', '"');
        assertThat(parser.parse("\"hello\"")).isEqualTo("hello");
    }

    @Test
    void quotedBy_empty() {
        Parser<String> parser = quotedBy('"', '"');
        assertThat(parser.parse("\"\"")).isEqualTo("");
    }

    @Test
    void quotedBy_differentDelimiters() {
        Parser<String> parser = quotedBy('<', '>');
        assertThat(parser.parse("<content>")).isEqualTo("content");
    }

    @Test
    void quotedByWithEscapes_success() {
        Parser<String> parser = quotedByWithEscapes('"', '"', chars(1));
        assertThat(parser.parse("\"hello\"")).isEqualTo("hello");
        // The escape just removes the backslash, doesn't interpret sequences like \n
        assertThat(parser.parse("\"hello\\nworld\"")).isEqualTo("hellonworld");
        assertThat(parser.parse("\"\\\\\"")).isEqualTo("\\");
    }

    // ===== Skipping Tests =====

    @Test
    void skipping_whitespace_success() {
        Parser<String> parser = word();
        assertThat(parser.skipping(Character::isWhitespace).parse("  foo  ")).isEqualTo("foo");
    }

    @Test
    void skipping_pattern_success() {
        Parser<String> parser = word();
        assertThat(parser.skipping(string(" ")).parseToStream("foo bar baz").toList())
                .containsExactly("foo", "bar", "baz");
    }

    @Test
    void parseSkipping_whitespace() {
        Parser<String> parser = word();
        assertThat(parser.parseSkipping(Character::isWhitespace, "  foo  ")).isEqualTo("foo");
    }

    // ===== Recursive Grammar Tests =====

    @Test
    void define_recursiveGrammar() {
        Parser<Integer> expr = Parser.define(self -> anyOf(self.between("(", ")"), digits().map(Integer::parseInt)));
        assertThat(expr.parse("123")).isEqualTo(123);
        assertThat(expr.parse("(123)")).isEqualTo(123);
        assertThat(expr.parse("((123))")).isEqualTo(123);
    }

    @Test
    void rule_recursiveGrammar() {
        var rule = new Parser.Rule<Integer>();
        Parser<Integer> atomic = anyOf(rule.between("(", ")"), digits().map(Integer::parseInt));
        Parser<Integer> expr = rule.definedAs(atomic);
        assertThat(expr.parse("123")).isEqualTo(123);
        assertThat(expr.parse("(123)")).isEqualTo(123);
        assertThat(expr.parse("((123))")).isEqualTo(123);
    }

    // ===== ParseToStream Tests =====

    @Test
    void parseToStream_success() {
        Parser<String> parser = word();
        assertThat(parser.skipping(Character::isWhitespace)
                        .parseToStream("foo bar baz")
                        .toList())
                .containsExactly("foo", "bar", "baz");
    }

    @Test
    void parseToStream_empty() {
        Parser<String> parser = word();
        assertThat(parser.parseToStream("").toList()).isEmpty();
    }

    @Test
    void parseToStream_reader() {
        Parser<String> parser = word();
        assertThat(parser.skipping(Character::isWhitespace)
                        .parseToStream(new StringReader("foo bar"))
                        .toList())
                .containsExactly("foo", "bar");
    }

    // ===== Probe Tests =====

    @Test
    void probe_success() {
        // word() matches [a-zA-Z0-9_]+, so "foo123" is a single word
        Parser<String> parser = word();
        assertThat(parser.probe("foo123").toList()).containsExactly("foo123");
    }

    @Test
    void probe_terminatesOnFailure() {
        // probe terminates without exception when parser can't match
        Parser<String> parser = word();
        assertThat(parser.probe("foo 123").toList()).containsExactly("foo");
    }

    @Test
    void probe_empty() {
        // Letters only (not word chars which include digits)
        Parser<String> parser = consecutive(CharPredicate.ASCII_LETTER, "letters");
        assertThat(parser.probe("123").toList()).isEmpty();
    }

    // ===== Error Message Tests =====

    @Test
    void parseException_containsPosition() {
        assertThatThrownBy(() -> string("foo").parse("bar"))
                .isInstanceOf(Parser.ParseException.class)
                .hasMessageContaining("1:1");
    }

    @Test
    void parseException_containsExpected() {
        assertThatThrownBy(() -> string("foo").parse("bar"))
                .isInstanceOf(Parser.ParseException.class)
                .hasMessageContaining("expecting <foo>");
    }

    @Test
    void parseException_sourceIndex() {
        try {
            string("foo").then(string("bar")).parse("fooXXX");
        } catch (Parser.ParseException e) {
            assertThat(e.getSourceIndex()).isEqualTo(3);
        }
    }

    // ===== Complex Examples =====

    @Test
    void jsonLikeArray() {
        Parser<List<Integer>> parser =
                digits().map(Integer::parseInt).zeroOrMoreDelimitedBy(",").between("[", "]");
        assertThat(parser.parse("[]")).isEmpty();
        assertThat(parser.parse("[1]")).containsExactly(1);
        assertThat(parser.parse("[1,2,3]")).containsExactly(1, 2, 3);
    }

    @Test
    void keyValuePairs() {
        Parser<Both<String, String>> pair = sequence(word().followedBy("="), word(), Both::of);
        Parser<List<Both<String, String>>> parser =
                pair.zeroOrMoreDelimitedBy(",").between("{", "}");
        var result = parser.parse("{a=1,b=2}");
        assertThat(result).hasSize(2);
        assertThat(result.get(0).first()).isEqualTo("a");
        assertThat(result.get(0).second()).isEqualTo("1");
    }

    @Test
    void doubleParser_withValidation() {
        Parser<Double> parser = consecutive(charsIn("[0-9.]"))
                .map(s -> {
                    try {
                        return Double.parseDouble(s);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .suchThat(Objects::nonNull, "double number");
        assertThat(parser.probe("1.23.4").toList()).isEmpty();
        assertThat(parser.parse("1.23")).isEqualTo(1.23);
    }

    @Test
    void csvLine() {
        Parser<String> field = consecutive(CharPredicate.noneOf(",\n"), "field");
        Parser<List<String>> parser = field.orElse("").delimitedBy(",").notEmpty();
        assertThat(parser.parse("a,b,c")).containsExactly("a", "b", "c");
        assertThat(parser.parse(",a,")).containsExactly("", "a", "");
    }
}
