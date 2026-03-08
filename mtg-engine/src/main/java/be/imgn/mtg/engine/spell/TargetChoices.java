package be.imgn.mtg.engine.spell;

import java.util.List;
import java.util.Optional;

import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.selector.Selectable;

/// The target choices made when casting a spell.
///
/// @param choices the ordered list of target choices
public record TargetChoices(List<TargetChoice> choices) {

    /// Returns an empty target choices instance.
    public static TargetChoices empty() {
        return new TargetChoices(List.of());
    }

    /// Finds the chosen target for a given subject, if any.
    public Optional<Selectable> findTarget(Subject subject) {
        return choices.stream()
                .filter(c -> c.subject().equals(subject))
                .map(TargetChoice::target)
                .findFirst();
    }
}
