package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.parse.Parser.anyOf;
import static be.imgn.mtg.parse.Parser.sequence;
import static be.imgn.mtg.parse.Parser.string;
import static be.imgn.mtg.parse.Parser.word;

import be.imgn.mtg.engine.ability.internal.parser.effect.PutOnLibraryEffect;
import be.imgn.mtg.engine.ability.internal.parser.effect.ReturnToHandEffect;
import be.imgn.mtg.engine.ability.internal.parser.selector.LibraryPosition;
import be.imgn.mtg.parse.Parser;

/// Parser for return effects in oracle text.
public final class ReturnParser {

    private ReturnParser() {}

    /// Parses the suffix "to its owner's hand" or "to their owner's hand".
    private static final Parser<String> TO_OWNERS_HAND =
            anyOf(string("to its owner's hand"), string("to their owner's hand"), string("to their owners' hands"));

    /// Parses "Return target creature to its owner's hand."
    ///
    /// Pattern: "Return" subject "to its/their owner's hand" ["."]
    public static final Parser<ReturnToHandEffect> RETURN_TO_HAND_EFFECT = word("Return")
            .then(ReferenceParser.SUBJECT)
            .followedBy(TO_OWNERS_HAND)
            .map(ReturnToHandEffect::new)
            .optionallyFollowedBy(".");

    /// Parses library position (top or bottom).
    private static final Parser<LibraryPosition> LIBRARY_POSITION =
            anyOf(word("top").thenReturn(LibraryPosition.TOP), word("bottom").thenReturn(LibraryPosition.BOTTOM));

    /// Parses the suffix for library placement.
    private static final Parser<LibraryPosition> ON_LIBRARY_SUFFIX = string("on the")
            .then(LIBRARY_POSITION)
            .followedBy(anyOf(
                    string("of its owner's library"),
                    string("of their owner's library"),
                    string("of their owners' libraries")));

    /// Parses "Put target creature on the bottom of its owner's library."
    ///
    /// Pattern: "Put" subject "on the top/bottom of its/their owner's library" ["."]
    public static final Parser<PutOnLibraryEffect> PUT_ON_LIBRARY_EFFECT = word("Put")
            .then(sequence(ReferenceParser.SUBJECT, ON_LIBRARY_SUFFIX, PutOnLibraryEffect::new))
            .optionallyFollowedBy(".");
}
