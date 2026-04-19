package be.imgn.mtg.engine.oracle;

/// Direction of a cost modification — a spell / ability costs more or less
/// than its printed cost. Used by {@link Effect.ModifyCost} and
/// {@link Effect.ModifyKeywordCost}.
public enum CostDelta {
    MORE,
    LESS
}
