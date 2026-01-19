package be.imgn.mtg.parse;

import static be.imgn.mtg.parse.CharPredicate.isNot;
import static java.util.stream.Collectors.reducing;

/// Represents a set of characters specified by a regex-like character set string.
///
/// For example `charsIn("[a-zA-Z-_]")` is a shorthand of `CharPredicate.range('a', 'z').orRange('A',
/// 'Z').or('-').or('_')`.
///
/// You can also use `'^'` to get negative character set like: `charsIn("[^a-zA-Z]")`, which is any
/// non-alphabet character.
///
/// Note that it's different from `CharPredicate.anyOf(string)`, which treats the string as a list of literal
/// characters, not a regex-like character set.
public final class CharacterSet implements CharPredicate {
    private final String string;
    private final CharPredicate predicate;

    private CharacterSet(String string, CharPredicate predicate) {
        this.string = string;
        this.predicate = predicate;
    }

    /// Returns a {@link CharacterSet} instance compiled from the given `characterSet` specifier.
    ///
    /// @param characterSet A regex-like character set string (e.g. `"[a-zA-Z0-9-_]"`), but disallows backslash so
    ///     doesn't support escaping.
    /// @throws IllegalArgumentException if `characterSet` includes backslash or the right bracket (except the
    ///     outmost pairs of `[]`).
    public static CharacterSet charsIn(String characterSet) {
        return new CharacterSet(characterSet, compileCharacterSet(characterSet));
    }

    /// Returns true if this set contains the character `ch`.
    @Override
    public boolean test(char ch) {
        return predicate.test(ch);
    }

    /// Returns true if this set contains the character `ch`.
    public boolean contains(char ch) {
        return predicate.test(ch);
    }

    @Override
    public CharacterSet not() {
        String newString;
        if (string.startsWith("[^")) {
            newString = "[" + string.substring(2);
        } else if (string.startsWith("[")) {
            newString = "[^" + string.substring(1);
        } else {
            newString = string;
        }
        return new CharacterSet(newString, predicate.not());
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof CharacterSet that) && string.equals(that.string);
    }

    @Override
    public int hashCode() {
        return string.hashCode();
    }

    /// Returns the character set string representation. For example `"[a-zA-Z0-9-_]"`.
    @Override
    public String toString() {
        return string;
    }

    private static CharPredicate compileCharacterSet(String characterSet) {
        checkArgument(
                characterSet.startsWith("[") && characterSet.endsWith("]"),
                "Character set must be in square brackets. Use [%s] instead.",
                characterSet);
        checkArgument(
                !characterSet.contains("\\"),
                "Escaping (%s) not supported in a character set. Please use CharPredicate instead.",
                characterSet);
        var validChar = Parser.single(isNot(']'), "character");
        var range = Parser.sequence(validChar.followedBy("-"), validChar, CharPredicate::range);
        var positiveSet = Parser.anyOf(range, validChar.map(CharPredicate::is))
                .zeroOrMore(reducing(CharPredicate.NONE, CharPredicate::or));
        var negativeSet = Parser.string("^").then(positiveSet).map(CharPredicate::not);
        return negativeSet.or(positiveSet).between("[", "]").parse(characterSet);
    }

    private static void checkArgument(boolean condition, String message, Object... args) {
        if (!condition) {
            throw new IllegalArgumentException(String.format(message, args));
        }
    }
}
