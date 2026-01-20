package be.imgn.mtg.engine.turn.internal.steps;

import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.turn.Step;
import be.imgn.mtg.engine.turn.StepType;

/// Base implementation for steps with common functionality.
abstract class AbstractStep implements Step {

    private final StepType type;
    private final int occurrence;

    protected AbstractStep(StepType type, int occurrence) {
        this.type = type;
        this.occurrence = occurrence;
    }

    @Override
    public final StepType type() {
        return type;
    }

    @Override
    public final int occurrence() {
        return occurrence;
    }

    @Override
    public boolean hasPriority() {
        return type.hasPriority();
    }

    @Override
    public void performEndActions(GameState gameState) {
        // Default: no end actions
    }
}
