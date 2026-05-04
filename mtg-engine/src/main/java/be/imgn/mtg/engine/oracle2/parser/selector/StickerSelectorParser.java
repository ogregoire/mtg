package be.imgn.mtg.engine.oracle2.parser.selector;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.selector.StickerSelector;

/// Parser for [StickerSelector]. Single arm matching the participle
/// "stickered" — used as an adjective filter on permanents in
/// vintage oracle text.
public final class StickerSelectorParser {
    private StickerSelectorParser() {}

    /// "stickered" — [StickerSelector.IsStickered] presence check.
    public static final Parser<StickerSelector> STICKER_SELECTOR =
            phrase("Stickered").thenReturn(new StickerSelector.IsStickered());
}
