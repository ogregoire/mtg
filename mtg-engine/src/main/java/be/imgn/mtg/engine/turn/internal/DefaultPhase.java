package be.imgn.mtg.engine.turn.internal;

import java.util.List;

import be.imgn.mtg.engine.turn.Phase;
import be.imgn.mtg.engine.turn.PhaseType;
import be.imgn.mtg.engine.turn.Step;

/// Default implementation of [Phase].
record DefaultPhase(PhaseType type, int occurrence, List<Step> steps) implements Phase {

    DefaultPhase {
        steps = List.copyOf(steps);
    }
}
