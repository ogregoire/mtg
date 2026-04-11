package be.imgn.mtg.engine.oracle;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;

/// Case-insensitive word matching utilities for oracle text parsing.
final class Words {
    private Words() {}

    /// Match a word case-insensitively (both title-case and lowercase).
    static Parser<String> w(String text) {
        var lower = text.toLowerCase();
        var title = Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
        if (lower.equals(title)) return word(lower);
        return anyOf(word(title), word(lower));
    }
}
