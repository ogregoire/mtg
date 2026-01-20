package be.imgn.mtg.engine.turn.internal;

import be.imgn.mtg.engine.turn.OccurrenceTracker;
import be.imgn.mtg.engine.turn.PhaseType;
import be.imgn.mtg.engine.turn.StepType;
import be.imgn.mtg.engine.util.Multiset;

/// Default implementation of [OccurrenceTracker] using EnumMultiset.
///
/// Tracks how many times each phase and step has occurred in the current turn.
final class DefaultOccurrenceTracker implements OccurrenceTracker {

    private final Multiset<PhaseType> phaseCounts;
    private final Multiset<StepType> stepCounts;

    DefaultOccurrenceTracker() {
        this.phaseCounts = Multiset.newEnumMultiset(PhaseType.class);
        this.stepCounts = Multiset.newEnumMultiset(StepType.class);
    }

    @Override
    public int increment(PhaseType phase) {
        phaseCounts.add(phase);
        return phaseCounts.count(phase);
    }

    @Override
    public int increment(StepType step) {
        stepCounts.add(step);
        return stepCounts.count(step);
    }

    @Override
    public int count(PhaseType phase) {
        return phaseCounts.count(phase);
    }

    @Override
    public int count(StepType step) {
        return stepCounts.count(step);
    }

    @Override
    public void reset() {
        phaseCounts.clear();
        stepCounts.clear();
    }
}
