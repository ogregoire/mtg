package be.imgn.mtg.engine.oracle2.domain;

/// A permanent's status ({@mtg.rule 110.5}) — the four binary axes
/// every permanent carries alongside its characteristics:
/// tapped/untapped, flipped/unflipped, face up/face down, phased
/// in/phased out. Status isn't a characteristic ({@mtg.rule 110.5a}).
///
/// Each value's [#text()] is the canonical oracle-text adjective
/// usable as a `phrase()` template. The Face values use bracket
/// alternation so both "face-down" (hyphenated) and "face down"
/// (spaced) spellings match.
///
/// Order matters: `UNTAPPED` precedes `TAPPED` and `UNFLIPPED`
/// precedes `FLIPPED` so the longer prefix wins when the parser
/// iterates `values()`.
public enum ObjectStatus implements Parseable {
    UNTAPPED("Untapped"),
    TAPPED("Tapped"),
    UNFLIPPED("Unflipped"),
    FLIPPED("Flipped"),
    FACE_UP("[Face-up|Face up]"),
    FACE_DOWN("[Face-down|Face down]"),
    PHASED_IN("Phased in"),
    PHASED_OUT("Phased out");

    private final String text;

    ObjectStatus(String text) {
        this.text = text;
    }

    @Override
    public String text() {
        return text;
    }
}
