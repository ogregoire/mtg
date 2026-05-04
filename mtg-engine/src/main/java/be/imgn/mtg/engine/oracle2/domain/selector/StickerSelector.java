package be.imgn.mtg.engine.oracle2.domain.selector;

/// Selects an object by stickers on it ({@mtg.rule 123}). Sole
/// vintage-legal use is the participle "stickered permanent" — a
/// presence check (the permanent has at least one sticker of any
/// kind on it). Used by ~6 cards (Scared Stiff, Grabby Tabby, Big
/// Winner, etc., all in "as long as you control a stickered
/// permanent" conditions).
///
/// Sticker types (name, ability, P/T, art) aren't filtered by
/// vintage oracle text; if the engine ever needs to distinguish
/// them at the selector level, add a parameterized arm later.
public sealed interface StickerSelector extends ObjectPropertySelector permits StickerSelector.IsStickered {

    /// "stickered permanent" — has at least one sticker on it.
    record IsStickered() implements StickerSelector {}
}
