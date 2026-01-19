package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.parse.Parser.anyOf;
import static be.imgn.mtg.parse.Parser.word;

import be.imgn.mtg.engine.zone.ZoneType;
import be.imgn.mtg.parse.Parser;

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
            word("command zone").thenReturn(ZoneType.COMMAND));
}
