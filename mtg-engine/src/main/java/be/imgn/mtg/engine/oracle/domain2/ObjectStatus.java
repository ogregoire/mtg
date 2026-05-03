package be.imgn.mtg.engine.oracle.domain2;

/// A permanent's status ({@mtg.rule 110.5}) — the four binary axes
/// every permanent carries alongside its characteristics:
/// tapped/untapped, flipped/unflipped, face up/face down, phased
/// in/phased out. Status isn't a characteristic ({@mtg.rule 110.5a}).
public enum ObjectStatus {
    TAPPED,
    UNTAPPED,
    FLIPPED,
    UNFLIPPED,
    FACE_UP,
    FACE_DOWN,
    PHASED_IN,
    PHASED_OUT
}
