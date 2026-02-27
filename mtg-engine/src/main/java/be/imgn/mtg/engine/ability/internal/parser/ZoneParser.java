package be.imgn.mtg.engine.ability.internal.parser;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.zone.ZoneType;

/// Parser for game zones in oracle text.
public final class ZoneParser {

    private ZoneParser() {}

    /// Parses a zone name.
    public static final Parser<ZoneType> ZONE = anyOf(
            word("battlefield").thenReturn(ZoneType.BATTLEFIELD),
            word("hand").thenReturn(ZoneType.HAND),
            word("graveyard").thenReturn(ZoneType.GRAVEYARD),
            word("library").thenReturn(ZoneType.LIBRARY),
            word("exile").thenReturn(ZoneType.EXILE),
            word("stack").thenReturn(ZoneType.STACK),
            string("command zone").thenReturn(ZoneType.COMMAND));
}
