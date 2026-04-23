package be.imgn.mtg.engine.oracle.domain;

/// Keywords whose printed form carries a cost and which can be referenced
/// collectively by oracle text ("Buyback costs cost {2} less").
///
/// Rule 702.1a: "If an effect refers to a '[keyword ability] cost,' it refers
/// only to the variable costs for that keyword."
public enum CostKeyword {
    BUYBACK, // 702.27
    KICKER, // 702.33
    MULTIKICKER,
    FLASHBACK, // 702.34
    MADNESS, // 702.35
    ECHO, // 702.30
    CYCLING, // 702.29
    EQUIP, // 702.6
    FORTIFY, // 702.67
    WARD, // 702.21
    BESTOW, // 702.103
    DASH, // 702.109
    ENTWINE, // 702.42
    SPLICE, // 702.47
    REPLICATE, // 702.56
    SUSPEND, // 702.62
    TRANSMUTE, // 702.53
    TRANSFIGURE, // 702.71
    RECOVER, // 702.59
    AURA_SWAP, // 702.65
    NINJUTSU, // 702.49
    OUTLAST, // 702.107
    SCAVENGE, // 702.97
    UNEARTH, // 702.84
    LEVEL_UP, // 702.87
    REINFORCE, // 702.77
    AWAKEN, // 702.113
    EMERGE, // 702.119
    ESCAPE, // 702.138
    EMBALM, // 702.128
    ETERNALIZE // 702.129
}
