package be.imgn.mtg.engine.oracle.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.oracle.domain.Amount;
import be.imgn.mtg.engine.oracle.domain.Effect;
import be.imgn.mtg.engine.oracle.domain.Mana;
import be.imgn.mtg.engine.oracle.domain.ManaSymbol;

class ManaParsersTest {

    private static final CharPredicate SPACE = CharPredicate.is(' ');

    private static final List<ManaSymbol> BASIC_COLORS = List.of(
            new ManaSymbol("{W}"),
            new ManaSymbol("{U}"),
            new ManaSymbol("{B}"),
            new ManaSymbol("{R}"),
            new ManaSymbol("{G}"));

    @Test
    void addsGreenMana() {
        var result = ManaParsers.ADD_MANA.parseSkipping(SPACE, "Add {G}");
        assertThat(result).isInstanceOf(Effect.AddMana.class);
        assertThat(result.mana()).isEqualTo(new Mana.Exact(List.of(new ManaSymbol("{G}"))));
    }

    @Test
    void addsWhiteWhiteMana() {
        var result = ManaParsers.ADD_MANA.parseSkipping(SPACE, "Add {W}{W}");
        assertThat(result).isInstanceOf(Effect.AddMana.class);
        assertThat(result.mana()).isEqualTo(new Mana.Exact(List.of(new ManaSymbol("{W}"), new ManaSymbol("{W}"))));
    }

    @Test
    void addsTwoBlackMana() {
        var result = ManaParsers.ADD_MANA.parseSkipping(SPACE, "Add {2}{B}");
        assertThat(result).isInstanceOf(Effect.AddMana.class);
        assertThat(result.mana()).isEqualTo(new Mana.Exact(List.of(new ManaSymbol("{2}"), new ManaSymbol("{B}"))));
    }

    @Test
    void addsThreeManaOfAnyOneColor() {
        var result = ManaParsers.ADD_MANA.parseSkipping(SPACE, "Add three mana of any one color");
        assertThat(result).isInstanceOf(Effect.AddMana.class);
        assertThat(result.mana())
                .isEqualTo(new Mana.OfOneColor(new Amount.Exact(3), new Mana.Palette.Explicit(BASIC_COLORS)));
    }

    @Test
    void addsXManaOfAnyOneColor() {
        var result = ManaParsers.ADD_MANA.parseSkipping(SPACE, "Add X mana of any one color");
        assertThat(result).isInstanceOf(Effect.AddMana.class);
        assertThat(result.mana())
                .isEqualTo(new Mana.OfOneColor(Amount.Variable.VARIABLE, new Mana.Palette.Explicit(BASIC_COLORS)));
    }

    @Test
    void addsAlternativeMana() {
        var result = ManaParsers.ADD_MANA.parseSkipping(SPACE, "Add {B} or {R}");
        assertThat(result).isInstanceOf(Effect.AddMana.class);
        assertThat(result.mana())
                .isEqualTo(new Mana.AnyOf(List.of(
                        new Mana.Exact(List.of(new ManaSymbol("{B}"))),
                        new Mana.Exact(List.of(new ManaSymbol("{R}"))))));
    }

    @Test
    void addsThreeManaOfDifferentColors() {
        var result = ManaParsers.ADD_MANA.parseSkipping(SPACE, "Add three mana of different colors");
        assertThat(result).isInstanceOf(Effect.AddMana.class);
        assertThat(result.mana())
                .isEqualTo(new Mana.OfDistinctColors(new Amount.Exact(3), new Mana.Palette.Explicit(BASIC_COLORS)));
    }
}
