package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.parse.Parser.anyOf;
import static be.imgn.mtg.parse.Parser.word;

import be.imgn.mtg.engine.ability.internal.parser.selector.Zone;
import be.imgn.mtg.parse.Parser;

/// Parser for game zones in oracle text.
public final class ZoneParser {

    private ZoneParser() {}

    /// Parses a zone name.
    public static final Parser<Zone> ZONE = anyOf(
            word("battlefield").thenReturn(Zone.BATTLEFIELD),
            word("hand").thenReturn(Zone.HAND),
            word("graveyard").thenReturn(Zone.GRAVEYARD),
            word("library").thenReturn(Zone.LIBRARY),
            word("exile").thenReturn(Zone.EXILE),
            word("stack").thenReturn(Zone.STACK),
            word("command zone").thenReturn(Zone.COMMAND));
}
