package be.imgn.mtg.engine.turn.internal;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.turn.PhaseType;
import be.imgn.mtg.engine.turn.SkipTracker;
import be.imgn.mtg.engine.turn.StepType;

/// Default implementation of [SkipTracker].
///
/// Tracks which phases, steps, and turns should be skipped.
/// Uses EnumSets for efficient phase/step tracking.
///
/// @see SkipTracker
public final class DefaultSkipTracker implements SkipTracker {

    private final Set<PhaseType> skippedPhases = EnumSet.noneOf(PhaseType.class);
    private final Set<StepType> skippedSteps = EnumSet.noneOf(StepType.class);
    private final Set<Player> playersWithSkippedTurn = new HashSet<>();

    @Override
    public boolean isSkipped(PhaseType phaseType) {
        return skippedPhases.contains(phaseType);
    }

    @Override
    public boolean isSkipped(StepType stepType) {
        return skippedSteps.contains(stepType);
    }

    @Override
    public boolean shouldSkipNextTurn(Player player) {
        return playersWithSkippedTurn.contains(player);
    }

    @Override
    public void skipPhase(PhaseType phaseType) {
        skippedPhases.add(phaseType);
    }

    @Override
    public void skipStep(StepType stepType) {
        skippedSteps.add(stepType);
    }

    @Override
    public void skipNextTurn(Player player) {
        playersWithSkippedTurn.add(player);
    }

    @Override
    public void clearTurnSkip(Player player) {
        playersWithSkippedTurn.remove(player);
    }

    @Override
    public void resetForNewTurn() {
        skippedPhases.clear();
        skippedSteps.clear();
    }
}
