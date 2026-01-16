package be.imgn.mtg.engine.characteristics.internal;

import be.imgn.mtg.engine.characteristics.Value;

/// A fixed integer value.
public record FixedValue(int value) implements Value {}
