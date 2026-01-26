package be.imgn.mtg.engine.mana;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.characteristics.Color;

class ManaTypeTest {

    @Nested
    class ColoredManaTypes {

        @Test
        void whiteType() {
            assertThat(ManaType.WHITE.name()).isEqualTo("WHITE");
            assertThat(ManaType.WHITE.notation()).isEqualTo("{W}");
            assertThat(ManaType.WHITE.color()).isEqualTo(Color.WHITE);
        }

        @Test
        void blueType() {
            assertThat(ManaType.BLUE.name()).isEqualTo("BLUE");
            assertThat(ManaType.BLUE.notation()).isEqualTo("{U}");
            assertThat(ManaType.BLUE.color()).isEqualTo(Color.BLUE);
        }

        @Test
        void blackType() {
            assertThat(ManaType.BLACK.name()).isEqualTo("BLACK");
            assertThat(ManaType.BLACK.notation()).isEqualTo("{B}");
            assertThat(ManaType.BLACK.color()).isEqualTo(Color.BLACK);
        }

        @Test
        void redType() {
            assertThat(ManaType.RED.name()).isEqualTo("RED");
            assertThat(ManaType.RED.notation()).isEqualTo("{R}");
            assertThat(ManaType.RED.color()).isEqualTo(Color.RED);
        }

        @Test
        void greenType() {
            assertThat(ManaType.GREEN.name()).isEqualTo("GREEN");
            assertThat(ManaType.GREEN.notation()).isEqualTo("{G}");
            assertThat(ManaType.GREEN.color()).isEqualTo(Color.GREEN);
        }

        @Test
        void fromColorWhite() {
            assertThat(ManaType.fromColor(Color.WHITE)).isEqualTo(ManaType.WHITE);
        }

        @Test
        void fromColorBlue() {
            assertThat(ManaType.fromColor(Color.BLUE)).isEqualTo(ManaType.BLUE);
        }

        @Test
        void fromColorBlack() {
            assertThat(ManaType.fromColor(Color.BLACK)).isEqualTo(ManaType.BLACK);
        }

        @Test
        void fromColorRed() {
            assertThat(ManaType.fromColor(Color.RED)).isEqualTo(ManaType.RED);
        }

        @Test
        void fromColorGreen() {
            assertThat(ManaType.fromColor(Color.GREEN)).isEqualTo(ManaType.GREEN);
        }
    }

    @Nested
    class ColorlessType {

        @Test
        void colorlessType() {
            assertThat(ManaType.COLORLESS.name()).isEqualTo("COLORLESS");
            assertThat(ManaType.COLORLESS.notation()).isEqualTo("{C}");
        }
    }

    @Nested
    class AllTypes {

        @Test
        void valuesReturnsAllTypes() {
            var values = ManaType.values();
            assertThat(values).hasSize(6);
            assertThat(values)
                    .containsExactly(
                            ManaType.WHITE,
                            ManaType.BLUE,
                            ManaType.BLACK,
                            ManaType.RED,
                            ManaType.GREEN,
                            ManaType.COLORLESS);
        }

        @Test
        void allConstantMatchesValues() {
            assertThat(ManaType.ALL).isEqualTo(ManaType.values());
        }
    }
}
