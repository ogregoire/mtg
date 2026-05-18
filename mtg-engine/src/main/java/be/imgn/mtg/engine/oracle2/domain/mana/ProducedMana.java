package be.imgn.mtg.engine.oracle2.domain.mana;

import java.util.List;

/// The mana an [be.imgn.mtg.engine.oracle2.domain.effect.AddManaEffect]
/// produces in one resolution — an ordered list of individual [Mana]
/// instances. Single-symbol "Add `{G}`." carries one element;
/// multi-symbol "Add `{G}{G}`." carries two; etc.
///
/// Spend restrictions ({@mtg.rule 106.6}) live on each [Mana] —
/// [#withRestriction(Restriction)] distributes a single restriction
/// across every instance so the AST faithfully captures that a
/// "Spend this mana only …" sentence binds to *every* mana the
/// effect produces, not the bundle.
public record ProducedMana(List<Mana> instances) {

    public ProducedMana {
        instances = List.copyOf(instances);
        if (instances.isEmpty()) {
            throw new IllegalArgumentException("ProducedMana must contain at least one Mana instance");
        }
    }

    /// Distribute `restriction` across every [Mana] instance —
    /// returns a new [ProducedMana] in which each element carries
    /// the restriction.
    public ProducedMana withRestriction(Restriction restriction) {
        return new ProducedMana(
                instances.stream().map(m -> m.withRestriction(restriction)).toList());
    }
}
