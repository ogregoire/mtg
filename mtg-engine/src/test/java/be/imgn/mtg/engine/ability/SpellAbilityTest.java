package be.imgn.mtg.engine.ability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.effect.DrawEffect;

class SpellAbilityTest {

    @Test
    void storesEffectsAndOracleText() {
        var effect = new DrawEffect(Optional.empty(), new Amount.Exact(2));
        var ability = new SpellAbility(new AbilityId(), "Draw two cards.", List.of(effect));

        assertThat(ability.oracleText()).isEqualTo("Draw two cards.");
        assertThat(ability.effects()).containsExactly(effect);
        assertThat(ability.id()).isNotNull();
    }

    @Test
    void implementsAbility() {
        var ability = new SpellAbility(new AbilityId(), "Gain 3 life.", List.of());

        assertThat(ability).isInstanceOf(Ability.class);
    }
}
