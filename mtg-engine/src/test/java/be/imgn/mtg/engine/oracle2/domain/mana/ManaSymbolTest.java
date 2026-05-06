package be.imgn.mtg.engine.oracle2.domain.mana;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Colored;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Colorless;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.ColorlessHybrid;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Generic;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Hybrid;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.HybridPhyrexian;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.MonoColorHybrid;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Phyrexian;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Snow;
import be.imgn.mtg.engine.oracle2.domain.mana.ManaSymbol.Variable;

class ManaSymbolTest {

    @Nested
    class Notation {
        @Test
        void colored() {
            assertThat(Colored.WHITE.notation()).isEqualTo("{W}");
            assertThat(Colored.BLUE.notation()).isEqualTo("{U}");
            assertThat(Colored.BLACK.notation()).isEqualTo("{B}");
            assertThat(Colored.RED.notation()).isEqualTo("{R}");
            assertThat(Colored.GREEN.notation()).isEqualTo("{G}");
        }

        @Test
        void colorless() {
            assertThat(Colorless.COLORLESS.notation()).isEqualTo("{C}");
        }

        @Test
        void variable() {
            assertThat(Variable.X.notation()).isEqualTo("{X}");
        }

        @Test
        void snow() {
            assertThat(Snow.SNOW.notation()).isEqualTo("{S}");
        }

        @Test
        void generic() {
            assertThat(new Generic(0).notation()).isEqualTo("{0}");
            assertThat(new Generic(1).notation()).isEqualTo("{1}");
            assertThat(new Generic(15).notation()).isEqualTo("{15}");
        }

        @Test
        void hybrid() {
            assertThat(Hybrid.WHITE_BLUE.notation()).isEqualTo("{W/U}");
            assertThat(Hybrid.GREEN_BLUE.notation()).isEqualTo("{G/U}");
        }

        @Test
        void monoColorHybrid() {
            assertThat(MonoColorHybrid.TWO_WHITE.notation()).isEqualTo("{2/W}");
            assertThat(MonoColorHybrid.TWO_GREEN.notation()).isEqualTo("{2/G}");
        }

        @Test
        void colorlessHybrid() {
            assertThat(ColorlessHybrid.COLORLESS_WHITE.notation()).isEqualTo("{C/W}");
            assertThat(ColorlessHybrid.COLORLESS_GREEN.notation()).isEqualTo("{C/G}");
        }

        @Test
        void phyrexian() {
            assertThat(Phyrexian.WHITE.notation()).isEqualTo("{W/P}");
            assertThat(Phyrexian.GREEN.notation()).isEqualTo("{G/P}");
        }

        @Test
        void hybridPhyrexian() {
            assertThat(HybridPhyrexian.WHITE_BLUE.notation()).isEqualTo("{W/U/P}");
            assertThat(HybridPhyrexian.GREEN_BLUE.notation()).isEqualTo("{G/U/P}");
        }
    }

    @Nested
    class Accepts {
        @Test
        void coloredAcceptsOnlyItsType() {
            assertThat(Colored.WHITE.accepts()).containsExactly(ManaType.WHITE);
            assertThat(Colored.RED.accepts()).containsExactly(ManaType.RED);
        }

        @Test
        void colorlessAcceptsOnlyColorless() {
            assertThat(Colorless.COLORLESS.accepts()).containsExactly(ManaType.COLORLESS);
        }

        @Test
        void genericAcceptsAllTypes() {
            assertThat(new Generic(3).accepts()).containsExactlyInAnyOrder(ManaType.values());
        }

        @Test
        void variableAcceptsAllTypes() {
            assertThat(Variable.X.accepts()).containsExactlyInAnyOrder(ManaType.values());
        }

        @Test
        void snowAcceptsAllTypes() {
            // Snow is orthogonal to type — any colored or colorless mana
            // produced by a snow source pays {S}.
            assertThat(Snow.SNOW.accepts()).containsExactlyInAnyOrder(ManaType.values());
        }

        @Test
        void hybridAcceptsBothColors() {
            assertThat(Hybrid.WHITE_BLUE.accepts()).containsExactlyInAnyOrder(ManaType.WHITE, ManaType.BLUE);
            assertThat(Hybrid.BLACK_GREEN.accepts()).containsExactlyInAnyOrder(ManaType.BLACK, ManaType.GREEN);
        }

        @Test
        void monoColorHybridAcceptsOnlyTheColor() {
            // The "two mana of any type" option is documented but not encoded.
            assertThat(MonoColorHybrid.TWO_WHITE.accepts()).containsExactly(ManaType.WHITE);
        }

        @Test
        void colorlessHybridAcceptsColoredAndColorless() {
            assertThat(ColorlessHybrid.COLORLESS_WHITE.accepts())
                    .containsExactlyInAnyOrder(ManaType.WHITE, ManaType.COLORLESS);
        }

        @Test
        void phyrexianAcceptsOnlyTheColor() {
            // The "pay 2 life" option is documented but not encoded.
            assertThat(Phyrexian.BLACK.accepts()).containsExactly(ManaType.BLACK);
        }

        @Test
        void hybridPhyrexianAcceptsBothColors() {
            assertThat(HybridPhyrexian.WHITE_BLUE.accepts()).containsExactlyInAnyOrder(ManaType.WHITE, ManaType.BLUE);
        }
    }

    @Nested
    class Lookups {
        @Test
        void coloredOf() {
            assertThat(Colored.of(ManaType.WHITE)).isEqualTo(Colored.WHITE);
            assertThat(Colored.of(ManaType.GREEN)).isEqualTo(Colored.GREEN);
        }

        @Test
        void coloredOfRejectsColorless() {
            assertThatThrownBy(() -> Colored.of(ManaType.COLORLESS)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void hybridOfFindsCanonicalPair() {
            assertThat(Hybrid.of(ManaType.WHITE, ManaType.BLUE)).isEqualTo(Hybrid.WHITE_BLUE);
            assertThat(Hybrid.of(ManaType.GREEN, ManaType.BLUE)).isEqualTo(Hybrid.GREEN_BLUE);
        }

        @Test
        void hybridOfRejectsInvalidPair() {
            // (WHITE, RED) is not in rule 107.4 — {W/R} doesn't exist.
            assertThatThrownBy(() -> Hybrid.of(ManaType.WHITE, ManaType.RED))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void hybridOfRejectsReversedPair() {
            // {U/W} is not the canonical form — only {W/U}.
            assertThatThrownBy(() -> Hybrid.of(ManaType.BLUE, ManaType.WHITE))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void hybridPhyrexianOf() {
            assertThat(HybridPhyrexian.of(ManaType.RED, ManaType.WHITE)).isEqualTo(HybridPhyrexian.RED_WHITE);
        }

        @Test
        void monoColorHybridOf() {
            assertThat(MonoColorHybrid.of(ManaType.WHITE)).isEqualTo(MonoColorHybrid.TWO_WHITE);
        }

        @Test
        void colorlessHybridOf() {
            assertThat(ColorlessHybrid.of(ManaType.WHITE)).isEqualTo(ColorlessHybrid.COLORLESS_WHITE);
        }

        @Test
        void phyrexianOf() {
            assertThat(Phyrexian.of(ManaType.BLACK)).isEqualTo(Phyrexian.BLACK);
        }
    }

    @Nested
    class GenericValidation {
        @Test
        void zeroAllowed() {
            assertThat(new Generic(0).amount()).isEqualTo(0);
        }

        @Test
        void negativeRejected() {
            assertThatThrownBy(() -> new Generic(-1)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
