package be.imgn.mtg.engine.oracle;

import java.util.List;

import org.jspecify.annotations.Nullable;

/// A parsed ability from oracle text.
public sealed interface Ability {
    record Keyword(String name, @Nullable String parameter) implements Ability {}

    record Activated(Cost cost, List<Effect> effects) implements Ability {}

    record Triggered(
            String triggerWord, String event, @Nullable Condition interveningIf, List<Effect> effects)
            implements Ability {}

    record Static(String text) implements Ability {}

    record Spell(List<Effect> effects) implements Ability {}

    record Modal(String quantity, List<Mode> modes) implements Ability {}

    record Chapter(String numerals, List<Effect> effects) implements Ability {}

    record CastingModifier(String text) implements Ability {}

    record Mode(@Nullable Cost cost, List<Effect> effects) {}
}
