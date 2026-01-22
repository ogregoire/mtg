package be.imgn.mtg.engine.mana;

import static be.imgn.mtg.engine.ability.internal.parser.assertions.EffectAssertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.internal.parser.effect.Effect;
import be.imgn.mtg.engine.mana.internal.ManaParser;
import be.imgn.mtg.parse.CharPredicate;

@DisplayName("ManaParser")
class ManaParserTest {

    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    private Effect parse(String text) {
        return ManaParser.ADD_MANA.parseSkipping(WHITESPACE, text);
    }

    @Nested
    @DisplayName("Add exact mana")
    class AddExactMana {

        @Test
        @DisplayName("Add {G}.")
        void addSingleGreen() {
            var effect = parse("Add {G}.");

            assertThat(effect).isAddExactManaEffect().hasMana("{G}");
        }

        @Test
        @DisplayName("Add {G}{G}.")
        void addTwoGreen() {
            var effect = parse("Add {G}{G}.");

            assertThat(effect).isAddExactManaEffect().hasMana("{G}{G}");
        }

        @Test
        @DisplayName("Add {R}")
        void addRedWithoutPeriod() {
            var effect = parse("Add {R}");

            assertThat(effect).isAddExactManaEffect().hasMana("{R}");
        }

        @Test
        @DisplayName("Add {W}{U}{B}{R}{G}.")
        void addAllColors() {
            var effect = parse("Add {W}{U}{B}{R}{G}.");

            assertThat(effect).isAddExactManaEffect().hasMana("{W}{U}{B}{R}{G}");
        }

        @Test
        @DisplayName("Add {C}.")
        void addColorless() {
            var effect = parse("Add {C}.");

            assertThat(effect).isAddExactManaEffect().hasMana("{C}");
        }

        @Test
        @DisplayName("Add {1}.")
        void addGenericOne() {
            var effect = parse("Add {1}.");

            assertThat(effect).isAddExactManaEffect().hasMana("{1}");
        }

        @Test
        @DisplayName("Add {16}.")
        void addGenericSixteen() {
            var effect = parse("Add {16}.");

            assertThat(effect).isAddExactManaEffect().hasMana("{16}");
        }
    }

    @Nested
    @DisplayName("Add mana of any color")
    class AnyColor {

        @Test
        @DisplayName("Add one mana of any color.")
        void addOneManaOfAnyColor() {
            var effect = parse("Add one mana of any color.");

            assertThat(effect).isAddManaOfAnyCombinationEffect().hasAmount(1).allowsAllColors();
        }

        @Test
        @DisplayName("Add two mana of any color.")
        void addTwoManaOfAnyColor() {
            var effect = parse("Add two mana of any color.");

            assertThat(effect).isAddManaOfAnyCombinationEffect().hasAmount(2).allowsAllColors();
        }

        @Test
        @DisplayName("Add three mana in any combination of colors.")
        void addThreeManaInAnyCombination() {
            var effect = parse("Add three mana in any combination of colors.");

            assertThat(effect).isAddManaOfAnyCombinationEffect().hasAmount(3).allowsAllColors();
        }

        @Test
        @DisplayName("Add five mana of any color")
        void addFiveManaWithoutPeriod() {
            var effect = parse("Add five mana of any color");

            assertThat(effect).isAddManaOfAnyCombinationEffect().hasAmount(5).allowsAllColors();
        }
    }

    @Nested
    @DisplayName("Add mana in any combination of specific colors")
    class AnyCombinationOfSpecificColors {

        @Test
        @DisplayName("Add three mana in any combination of {R} and/or {G}.")
        void addThreeManaRedOrGreen() {
            var effect = parse("Add three mana in any combination of {R} and/or {G}.");

            assertThat(effect)
                    .isAddManaOfAnyCombinationEffect()
                    .hasAmount(3)
                    .hasAllowedTypes(Set.of(ManaType.RED, ManaType.GREEN));
        }

        @Test
        @DisplayName("Add two mana in any combination of {W} and/or {U}.")
        void addTwoManaWhiteOrBlue() {
            var effect = parse("Add two mana in any combination of {W} and/or {U}.");

            assertThat(effect)
                    .isAddManaOfAnyCombinationEffect()
                    .hasAmount(2)
                    .hasAllowedTypes(Set.of(ManaType.WHITE, ManaType.BLUE));
        }

        @Test
        @DisplayName("Add four mana in any combination of {W}, {U}, and/or {B}.")
        void addFourManaThreeColors() {
            var effect = parse("Add four mana in any combination of {W}, {U}, and/or {B}.");

            assertThat(effect)
                    .isAddManaOfAnyCombinationEffect()
                    .hasAmount(4)
                    .hasAllowedTypes(Set.of(ManaType.WHITE, ManaType.BLUE, ManaType.BLACK));
        }

        @Test
        @DisplayName("Add five mana in any combination of {W}, {U}, {B}, and/or {R}.")
        void addFiveManaFourColors() {
            var effect = parse("Add five mana in any combination of {W}, {U}, {B}, and/or {R}.");

            assertThat(effect)
                    .isAddManaOfAnyCombinationEffect()
                    .hasAmount(5)
                    .hasAllowedTypes(Set.of(ManaType.WHITE, ManaType.BLUE, ManaType.BLACK, ManaType.RED));
        }
    }

    @Nested
    @DisplayName("Add mana of any one color")
    class AnyOneColor {

        @Test
        @DisplayName("Add two mana of any one color.")
        void addTwoManaOfAnyOneColor() {
            var effect = parse("Add two mana of any one color.");

            assertThat(effect)
                    .isAddManaFromSelectionEffect()
                    .hasAmount(2)
                    .hasOptionCount(5)
                    .hasOptions(List.of(
                            List.of(ManaType.WHITE),
                            List.of(ManaType.BLUE),
                            List.of(ManaType.BLACK),
                            List.of(ManaType.RED),
                            List.of(ManaType.GREEN)));
        }

        @Test
        @DisplayName("Add four mana of any one color.")
        void addFourManaOfAnyOneColor() {
            var effect = parse("Add four mana of any one color.");

            assertThat(effect)
                    .isAddManaFromSelectionEffect()
                    .hasAmount(4)
                    .hasOptionCount(5)
                    .hasOptionAt(0, List.of(ManaType.WHITE))
                    .hasOptionAt(4, List.of(ManaType.GREEN));
        }

        @Test
        @DisplayName("Add one mana of any one color.")
        void addOneManaOfAnyOneColor() {
            var effect = parse("Add one mana of any one color.");

            assertThat(effect)
                    .isAddManaFromSelectionEffect()
                    .hasAmount(1)
                    .hasOptionCount(5)
                    .hasOptions(List.of(
                            List.of(ManaType.WHITE),
                            List.of(ManaType.BLUE),
                            List.of(ManaType.BLACK),
                            List.of(ManaType.RED),
                            List.of(ManaType.GREEN)));
        }
    }

    @Nested
    @DisplayName("Add mana from selection")
    class ManaSelection {

        @Test
        @DisplayName("Add {U} or {B}.")
        void addBlueOrBlack() {
            var effect = parse("Add {U} or {B}.");

            assertThat(effect)
                    .isAddManaFromSelectionEffect()
                    .hasOptionCount(2)
                    .hasOptions(List.of(List.of(ManaType.BLUE), List.of(ManaType.BLACK)));
        }

        @Test
        @DisplayName("Add {R} or {G}.")
        void addRedOrGreen() {
            var effect = parse("Add {R} or {G}.");

            assertThat(effect)
                    .isAddManaFromSelectionEffect()
                    .hasOptionCount(2)
                    .hasOptions(List.of(List.of(ManaType.RED), List.of(ManaType.GREEN)));
        }

        @Test
        @DisplayName("Add {W}, {U}, or {B}.")
        void addWhiteBlueOrBlack() {
            var effect = parse("Add {W}, {U}, or {B}.");

            assertThat(effect)
                    .isAddManaFromSelectionEffect()
                    .hasOptionCount(3)
                    .hasOptions(List.of(List.of(ManaType.WHITE), List.of(ManaType.BLUE), List.of(ManaType.BLACK)));
        }

        @Test
        @DisplayName("Add {R}{R}, {R}{G}, or {G}{G}.")
        void addDoubleRedMixedOrDoubleGreen() {
            var effect = parse("Add {R}{R}, {R}{G}, or {G}{G}.");

            assertThat(effect)
                    .isAddManaFromSelectionEffect()
                    .hasOptionCount(3)
                    .hasOptions(List.of(
                            List.of(ManaType.RED, ManaType.RED),
                            List.of(ManaType.RED, ManaType.GREEN),
                            List.of(ManaType.GREEN, ManaType.GREEN)));
        }

        @Test
        @DisplayName("Add {G}{G} or {G}{U}.")
        void addDoubleGreenOrGreenBlue() {
            var effect = parse("Add {G}{G} or {G}{U}.");

            assertThat(effect)
                    .isAddManaFromSelectionEffect()
                    .hasOptionCount(2)
                    .hasOptions(
                            List.of(List.of(ManaType.GREEN, ManaType.GREEN), List.of(ManaType.GREEN, ManaType.BLUE)));
        }

        @Test
        @DisplayName("Add {W}{W}{W}, {U}{U}{U}, {B}{B}{B}, {R}{R}{R}, or {G}{G}{G}.")
        void addTripleManaOptions() {
            var effect = parse("Add {W}{W}{W}, {U}{U}{U}, {B}{B}{B}, {R}{R}{R}, or {G}{G}{G}.");

            assertThat(effect)
                    .isAddManaFromSelectionEffect()
                    .hasOptionCount(5)
                    .hasOptionAt(0, List.of(ManaType.WHITE, ManaType.WHITE, ManaType.WHITE))
                    .hasOptionAt(1, List.of(ManaType.BLUE, ManaType.BLUE, ManaType.BLUE))
                    .hasOptionAt(2, List.of(ManaType.BLACK, ManaType.BLACK, ManaType.BLACK))
                    .hasOptionAt(3, List.of(ManaType.RED, ManaType.RED, ManaType.RED))
                    .hasOptionAt(4, List.of(ManaType.GREEN, ManaType.GREEN, ManaType.GREEN));
        }

        @Test
        @DisplayName("Add {B} or {R} without period")
        void addBlackOrRedWithoutPeriod() {
            var effect = parse("Add {B} or {R}");

            assertThat(effect)
                    .isAddManaFromSelectionEffect()
                    .hasOptionCount(2)
                    .hasOptions(List.of(List.of(ManaType.BLACK), List.of(ManaType.RED)));
        }

        @Test
        @DisplayName("Add {W}, {U}, {B}, or {C}{C}.")
        void addFourOptionsWithDoubleColorlessLast() {
            var effect = parse("Add {W}, {U}, {B}, or {C}{C}.");

            assertThat(effect)
                    .isAddManaFromSelectionEffect()
                    .hasOptionCount(4)
                    .hasOptions(List.of(
                            List.of(ManaType.WHITE),
                            List.of(ManaType.BLUE),
                            List.of(ManaType.BLACK),
                            List.of(ManaType.COLORLESS, ManaType.COLORLESS)));
        }
    }
}
