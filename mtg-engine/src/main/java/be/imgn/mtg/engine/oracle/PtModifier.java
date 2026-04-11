package be.imgn.mtg.engine.oracle;

/// A power/toughness modification (e.g., +2/+1, -1/-1).
public record PtModifier(int powerMod, int toughnessMod) {}
