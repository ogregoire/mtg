package be.imgn.mtg.engine.assertions;

import be.imgn.mtg.engine.object.Spell;

/// Assertion class for Spell.
public class SpellAssert extends AbstractGameObjectAssert<SpellAssert, Spell> {

    protected SpellAssert(Spell actual) {
        super(actual, SpellAssert.class);
    }

    public static SpellAssert assertThat(Spell actual) {
        return new SpellAssert(actual);
    }
}
