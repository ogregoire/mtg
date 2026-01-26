package be.imgn.mtg.parse;

import static be.imgn.mtg.parse.CharacterSet.charsIn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CharacterSetTest {

    @Test
    void singleChar() {
        var set = charsIn("[a]");
        assertThat(set.test('a')).isTrue();
        assertThat(set.test('b')).isFalse();
    }

    @Test
    void range() {
        var set = charsIn("[a-z]");
        assertThat(set.test('a')).isTrue();
        assertThat(set.test('m')).isTrue();
        assertThat(set.test('z')).isTrue();
        assertThat(set.test('A')).isFalse();
        assertThat(set.test('0')).isFalse();
    }

    @Test
    void multipleRanges() {
        var set = charsIn("[a-zA-Z]");
        assertThat(set.test('a')).isTrue();
        assertThat(set.test('Z')).isTrue();
        assertThat(set.test('0')).isFalse();
    }

    @Test
    void alphanumeric() {
        var set = charsIn("[a-zA-Z0-9]");
        assertThat(set.test('a')).isTrue();
        assertThat(set.test('Z')).isTrue();
        assertThat(set.test('5')).isTrue();
        assertThat(set.test('_')).isFalse();
    }

    @Test
    void withSpecialChars() {
        var set = charsIn("[a-z_-]");
        assertThat(set.test('a')).isTrue();
        assertThat(set.test('_')).isTrue();
        assertThat(set.test('-')).isTrue();
        assertThat(set.test('0')).isFalse();
    }

    @Test
    void negated() {
        var set = charsIn("[^a-z]");
        assertThat(set.test('a')).isFalse();
        assertThat(set.test('A')).isTrue();
        assertThat(set.test('0')).isTrue();
    }

    @Test
    void not_method() {
        var set = charsIn("[a-z]");
        var notSet = set.not();
        assertThat(notSet.test('a')).isFalse();
        assertThat(notSet.test('A')).isTrue();
    }

    @Test
    void contains() {
        var set = charsIn("[0-9]");
        assertThat(set.contains('0')).isTrue();
        assertThat(set.contains('9')).isTrue();
        assertThat(set.contains('a')).isFalse();
    }

    @Test
    void matchesAllOf() {
        var set = charsIn("[a-z]");
        assertThat(set.matchesAllOf("abc")).isTrue();
        assertThat(set.matchesAllOf("abc1")).isFalse();
        assertThat(set.matchesAllOf("")).isTrue();
    }

    @Test
    void matchesAnyOf() {
        var set = charsIn("[0-9]");
        assertThat(set.matchesAnyOf("abc123")).isTrue();
        assertThat(set.matchesAnyOf("abc")).isFalse();
    }

    @Test
    void matchesNoneOf() {
        var set = charsIn("[0-9]");
        assertThat(set.matchesNoneOf("abc")).isTrue();
        assertThat(set.matchesNoneOf("abc123")).isFalse();
    }

    @Test
    void mustBeInBrackets() {
        assertThatThrownBy(() -> charsIn("abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("square brackets");
    }

    @Test
    void backslashNotSupported() {
        assertThatThrownBy(() -> charsIn("[\\n]"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Escaping");
    }

    @Test
    void toString_returnsOriginalString() {
        assertThat(charsIn("[a-z]").toString()).isEqualTo("[a-z]");
        assertThat(charsIn("[^0-9]").toString()).isEqualTo("[^0-9]");
    }

    @Test
    void equals_sameString() {
        assertThat(charsIn("[a-z]")).isEqualTo(charsIn("[a-z]"));
    }

    @Test
    void equals_differentString() {
        assertThat(charsIn("[a-z]")).isNotEqualTo(charsIn("[A-Z]"));
    }

    @Test
    void hashCode_sameString() {
        assertThat(charsIn("[a-z]").hashCode()).isEqualTo(charsIn("[a-z]").hashCode());
    }
}
