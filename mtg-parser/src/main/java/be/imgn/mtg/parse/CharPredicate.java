package be.imgn.mtg.parse;

import java.util.function.IntPredicate;

/// A predicate for matching characters. Provides common character patterns and combinators for building character
/// matchers.
@FunctionalInterface
public interface CharPredicate {

    /// A predicate that matches no characters.
    CharPredicate NONE = c -> false;

    /// A predicate that matches any character.
    CharPredicate ANY = c -> true;

    /// Matches word characters: `[a-zA-Z0-9_]`.
    CharPredicate WORD = range('a', 'z').orRange('A', 'Z').orRange('0', '9').or('_');

    /// Matches ASCII letters: `[a-zA-Z]`.
    CharPredicate ASCII_LETTER = range('a', 'z').orRange('A', 'Z');

    /// Matches ASCII digits: `[0-9]`.
    CharPredicate ASCII_DIGIT = range('0', '9');

    /// Matches whitespace characters.
    CharPredicate WHITESPACE = Character::isWhitespace;

    /// Tests if the given character matches this predicate.
    ///
    /// @param c the character to test
    /// @return true if the character matches
    boolean test(char c);

    /// Returns a predicate that matches a single character.
    ///
    /// @param c the character to match
    /// @return a predicate matching exactly that character
    static CharPredicate is(char c) {
        return ch -> ch == c;
    }

    /// Returns a predicate that matches a range of characters (inclusive).
    ///
    /// @param from the start of the range (inclusive)
    /// @param to the end of the range (inclusive)
    /// @return a predicate matching characters in the range
    static CharPredicate range(char from, char to) {
        return c -> c >= from && c <= to;
    }

    /// Returns a predicate that matches any character in the given string.
    ///
    /// @param chars the characters to match
    /// @return a predicate matching any of the characters
    static CharPredicate anyOf(String chars) {
        return c -> chars.indexOf(c) >= 0;
    }

    /// Returns a predicate that matches any character not equal to the given character.
    ///
    /// @param c the character to exclude
    /// @return a predicate matching any character except c
    static CharPredicate isNot(char c) {
        return ch -> ch != c;
    }

    /// Returns a predicate that matches any character not in the given string.
    ///
    /// @param chars the characters to exclude
    /// @return a predicate matching any character not in chars
    static CharPredicate noneOf(String chars) {
        return c -> chars.indexOf(c) < 0;
    }

    /// Returns the negation of this predicate.
    ///
    /// @return a predicate that matches if this predicate does not match
    default CharPredicate not() {
        return c -> !test(c);
    }

    /// Returns a predicate that matches if this predicate or the given character matches.
    ///
    /// @param c the additional character to match
    /// @return a predicate matching this or the character
    default CharPredicate or(char c) {
        return ch -> test(ch) || ch == c;
    }

    /// Returns a predicate that matches if this predicate or the given predicate matches.
    ///
    /// @param other the other predicate
    /// @return a predicate matching this or other
    default CharPredicate or(CharPredicate other) {
        return c -> test(c) || other.test(c);
    }

    /// Returns a predicate that matches if this predicate matches or the character is in the range.
    ///
    /// @param from the start of the range (inclusive)
    /// @param to the end of the range (inclusive)
    /// @return a predicate matching this or the range
    default CharPredicate orRange(char from, char to) {
        return or(range(from, to));
    }

    /// Returns a predicate that matches if both this and the other predicate match.
    ///
    /// @param other the other predicate
    /// @return a predicate matching both this and other
    default CharPredicate and(CharPredicate other) {
        return c -> test(c) && other.test(c);
    }

    /// Returns true if all characters in the string match this predicate.
    ///
    /// @param str the string to test
    /// @return true if all characters match
    default boolean matchesAllOf(CharSequence str) {
        for (var i = 0; i < str.length(); i++) {
            if (!test(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /// Returns true if any character in the string matches this predicate.
    ///
    /// @param str the string to test
    /// @return true if any character matches
    default boolean matchesAnyOf(CharSequence str) {
        for (var i = 0; i < str.length(); i++) {
            if (test(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /// Returns true if no character in the string matches this predicate.
    ///
    /// @param str the string to test
    /// @return true if no character matches
    default boolean matchesNoneOf(CharSequence str) {
        return !matchesAnyOf(str);
    }

    /// Converts this CharPredicate to an IntPredicate.
    ///
    /// @return an IntPredicate that tests characters
    default IntPredicate asIntPredicate() {
        return c -> test((char) c);
    }
}
