package be.imgn.mtg.engine.oracle.domain;

/// Direction of a cost modification — a spell / ability costs more or less
/// than its printed cost. Used by [Effect.ModifyCost].
public enum CostDelta {
    MORE,
    LESS
}
