package be.imgn.mtg.engine.oracle2.domain.effect;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// "Regenerate X." ({@mtg.rule 701.15}). Creates a regeneration
/// replacement effect on `selector` — the next time it would be
/// destroyed this turn, instead remove all damage, tap it, and if
/// it's an attacker or blocker, remove it from combat. Typical
/// surface forms target a single permanent ("Regenerate target
/// creature.", "Regenerate this creature.").
public record RegenerateEffect(Selector selector) implements Effect {
    public RegenerateEffect {
        requireNonNull(selector);
    }
}
