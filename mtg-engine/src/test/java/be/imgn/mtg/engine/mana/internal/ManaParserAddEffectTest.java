package be.imgn.mtg.engine.mana.internal;

import static be.imgn.mtg.engine.ability.internal.parser.assertions.EffectAssertions.assertThat;

import java.util.List;
import java.util.Set;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.mana.ManaType;

/// Additional tests for ManaParser.ADD_MANA_EFFECT to improve branch coverage.
class ManaParserAddEffectTest {

    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    @Nested
    class VariableMana {

        @Test
        void addXWhite() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add X {W}.");

            assertThat(effect).isAddVariableManaEffect().hasMana(ManaType.WHITE);
        }

        @Test
        void addXBlue() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add X {U}.");

            assertThat(effect).isAddVariableManaEffect().hasMana(ManaType.BLUE);
        }

        @Test
        void addXBlack() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add X {B}.");

            assertThat(effect).isAddVariableManaEffect().hasMana(ManaType.BLACK);
        }

        @Test
        void addXRed() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add X {R}.");

            assertThat(effect).isAddVariableManaEffect().hasMana(ManaType.RED);
        }
    }

    @Nested
    class ExactManaAllColors {

        @Test
        void addWhite() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add {W}.");

            assertThat(effect).isAddExactManaEffect().hasMana(ManaType.WHITE);
        }

        @Test
        void addBlue() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add {U}.");

            assertThat(effect).isAddExactManaEffect().hasMana(ManaType.BLUE);
        }

        @Test
        void addBlack() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add {B}.");

            assertThat(effect).isAddExactManaEffect().hasMana(ManaType.BLACK);
        }

        @Test
        void addRed() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add {R}.");

            assertThat(effect).isAddExactManaEffect().hasMana(ManaType.RED);
        }

        @Test
        void addColorless() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add {C}.");

            assertThat(effect).isAddExactManaEffect().hasMana(ManaType.COLORLESS);
        }

        @Test
        void addMultipleColorless() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add {C}{C}.");

            assertThat(effect).isAddExactManaEffect().hasMana(ManaType.COLORLESS, ManaType.COLORLESS);
        }

        @Test
        void addMixedTypes() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add {W}{C}.");

            assertThat(effect).isAddExactManaEffect().hasMana(ManaType.WHITE, ManaType.COLORLESS);
        }
    }

    @Nested
    class SelectionTwoColors {

        @Test
        void addWhiteOrBlue() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add {W} or {U}.");

            assertThat(effect)
                    .isAddManaFromSelectionEffect()
                    .hasOptionCount(2)
                    .hasOptions(List.of(List.of(ManaType.WHITE), List.of(ManaType.BLUE)));
        }

        @Test
        void addWhiteOrBlack() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add {W} or {B}.");

            assertThat(effect)
                    .isAddManaFromSelectionEffect()
                    .hasOptionCount(2)
                    .hasOptions(List.of(List.of(ManaType.WHITE), List.of(ManaType.BLACK)));
        }

        @Test
        void addRedOrWhite() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add {R} or {W}.");

            assertThat(effect)
                    .isAddManaFromSelectionEffect()
                    .hasOptionCount(2)
                    .hasOptions(List.of(List.of(ManaType.RED), List.of(ManaType.WHITE)));
        }

        @Test
        void addGreenOrWhite() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add {G} or {W}.");

            assertThat(effect)
                    .isAddManaFromSelectionEffect()
                    .hasOptionCount(2)
                    .hasOptions(List.of(List.of(ManaType.GREEN), List.of(ManaType.WHITE)));
        }

        @Test
        void addColorlessOrGreen() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add {C} or {G}.");

            assertThat(effect)
                    .isAddManaFromSelectionEffect()
                    .hasOptionCount(2)
                    .hasOptions(List.of(List.of(ManaType.COLORLESS), List.of(ManaType.GREEN)));
        }
    }

    @Nested
    class SelectionManyColors {

        @Test
        void addFourOptions() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add {W}, {U}, {B}, or {R}.");

            assertThat(effect)
                    .isAddManaFromSelectionEffect()
                    .hasOptionCount(4)
                    .hasOptions(List.of(
                            List.of(ManaType.WHITE),
                            List.of(ManaType.BLUE),
                            List.of(ManaType.BLACK),
                            List.of(ManaType.RED)));
        }

        @Test
        void addDoubleManaCombinations() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add {W}{W}, {U}{U}, or {B}{B}.");

            assertThat(effect)
                    .isAddManaFromSelectionEffect()
                    .hasOptionCount(3)
                    .hasOptions(List.of(
                            List.of(ManaType.WHITE, ManaType.WHITE),
                            List.of(ManaType.BLUE, ManaType.BLUE),
                            List.of(ManaType.BLACK, ManaType.BLACK)));
        }

        @Test
        void addSixOptions() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add {W}, {U}, {B}, {R}, {G}, or {C}.");

            assertThat(effect)
                    .isAddManaFromSelectionEffect()
                    .hasOptionCount(6)
                    .hasOptionAt(5, List.of(ManaType.COLORLESS));
        }
    }

    @Nested
    class AnyColorWordNumbers {

        @Test
        void addOneMana() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add one mana of any color.");

            assertThat(effect).isAddManaOfAnyCombinationEffect().hasAmount(1).allowsAllColors();
        }

        @Test
        void addThreeMana() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add three mana of any color.");

            assertThat(effect).isAddManaOfAnyCombinationEffect().hasAmount(3).allowsAllColors();
        }

        @Test
        void addFourMana() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add four mana of any color.");

            assertThat(effect).isAddManaOfAnyCombinationEffect().hasAmount(4).allowsAllColors();
        }

        @Test
        void addSixMana() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add six mana of any color.");

            assertThat(effect).isAddManaOfAnyCombinationEffect().hasAmount(6).allowsAllColors();
        }

        @Test
        void addSevenManaInAnyCombination() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(
                    WHITESPACE, "Add seven mana in any combination of colors.");

            assertThat(effect).isAddManaOfAnyCombinationEffect().hasAmount(7).allowsAllColors();
        }

        @Test
        void addEightMana() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add eight mana of any color.");

            assertThat(effect).isAddManaOfAnyCombinationEffect().hasAmount(8).allowsAllColors();
        }

        @Test
        void addNineMana() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add nine mana of any color.");

            assertThat(effect).isAddManaOfAnyCombinationEffect().hasAmount(9).allowsAllColors();
        }

        @Test
        void addTenMana() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add ten mana of any color.");

            assertThat(effect).isAddManaOfAnyCombinationEffect().hasAmount(10).allowsAllColors();
        }
    }

    @Nested
    class AnyOneColorWordNumbers {

        @Test
        void addThreeManaOfAnyOneColor() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add three mana of any one color.");

            assertThat(effect).isAddManaFromSelectionEffect().hasAmount(3).hasOptionCount(5);
        }

        @Test
        void addFiveManaOfAnyOneColor() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add five mana of any one color.");

            assertThat(effect).isAddManaFromSelectionEffect().hasAmount(5).hasOptionCount(5);
        }

        @Test
        void addSixManaOfAnyOneColor() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add six mana of any one color.");

            assertThat(effect).isAddManaFromSelectionEffect().hasAmount(6).hasOptionCount(5);
        }
    }

    @Nested
    class CombinationOfSpecificColorsTwoColors {

        @Test
        void addOneInCombinationWhiteBlue() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(
                    WHITESPACE, "Add one mana in any combination of {W} and/or {U}.");

            assertThat(effect)
                    .isAddManaOfAnyCombinationEffect()
                    .hasAmount(1)
                    .hasAllowedTypes(Set.of(ManaType.WHITE, ManaType.BLUE));
        }

        @Test
        void addFourInCombinationBlackRed() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(
                    WHITESPACE, "Add four mana in any combination of {B} and/or {R}.");

            assertThat(effect)
                    .isAddManaOfAnyCombinationEffect()
                    .hasAmount(4)
                    .hasAllowedTypes(Set.of(ManaType.BLACK, ManaType.RED));
        }

        @Test
        void addFiveInCombinationBlueRed() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(
                    WHITESPACE, "Add five mana in any combination of {U} and/or {R}.");

            assertThat(effect)
                    .isAddManaOfAnyCombinationEffect()
                    .hasAmount(5)
                    .hasAllowedTypes(Set.of(ManaType.BLUE, ManaType.RED));
        }

        @Test
        void addSixInCombinationWhiteBlack() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(
                    WHITESPACE, "Add six mana in any combination of {W} and/or {B}.");

            assertThat(effect)
                    .isAddManaOfAnyCombinationEffect()
                    .hasAmount(6)
                    .hasAllowedTypes(Set.of(ManaType.WHITE, ManaType.BLACK));
        }

        @Test
        void addSevenInCombinationGreenWhite() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(
                    WHITESPACE, "Add seven mana in any combination of {G} and/or {W}.");

            assertThat(effect)
                    .isAddManaOfAnyCombinationEffect()
                    .hasAmount(7)
                    .hasAllowedTypes(Set.of(ManaType.GREEN, ManaType.WHITE));
        }
    }

    @Nested
    class CombinationOfSpecificColorsMoreColors {

        @Test
        void addTwoInCombinationWhiteBlueBlack() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(
                    WHITESPACE, "Add two mana in any combination of {W}, {U}, and/or {B}.");

            assertThat(effect)
                    .isAddManaOfAnyCombinationEffect()
                    .hasAmount(2)
                    .hasAllowedTypes(Set.of(ManaType.WHITE, ManaType.BLUE, ManaType.BLACK));
        }

        @Test
        void addThreeInCombinationBlackRedGreen() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(
                    WHITESPACE, "Add three mana in any combination of {B}, {R}, and/or {G}.");

            assertThat(effect)
                    .isAddManaOfAnyCombinationEffect()
                    .hasAmount(3)
                    .hasAllowedTypes(Set.of(ManaType.BLACK, ManaType.RED, ManaType.GREEN));
        }

        @Test
        void addSixInCombinationFourColors() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(
                    WHITESPACE, "Add six mana in any combination of {W}, {U}, {B}, and/or {R}.");

            assertThat(effect)
                    .isAddManaOfAnyCombinationEffect()
                    .hasAmount(6)
                    .hasAllowedTypes(Set.of(ManaType.WHITE, ManaType.BLUE, ManaType.BLACK, ManaType.RED));
        }

        @Test
        void addEightInCombinationFiveColors() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(
                    WHITESPACE, "Add eight mana in any combination of {W}, {U}, {B}, {R}, and/or {G}.");

            assertThat(effect)
                    .isAddManaOfAnyCombinationEffect()
                    .hasAmount(8)
                    .hasAllowedTypes(
                            Set.of(ManaType.WHITE, ManaType.BLUE, ManaType.BLACK, ManaType.RED, ManaType.GREEN));
        }
    }

    @Nested
    class WithoutPeriod {

        @Test
        void addGreenWithoutPeriod() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add {G}");

            assertThat(effect).isAddExactManaEffect().hasMana(ManaType.GREEN);
        }

        @Test
        void addXRedWithoutPeriod() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add X {R}");

            assertThat(effect).isAddVariableManaEffect().hasMana(ManaType.RED);
        }

        @Test
        void addSelectionWithoutPeriod() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add {W} or {U}");

            assertThat(effect).isAddManaFromSelectionEffect().hasOptionCount(2);
        }

        @Test
        void addAnyColorWithoutPeriod() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add one mana of any color");

            assertThat(effect).isAddManaOfAnyCombinationEffect().hasAmount(1).allowsAllColors();
        }

        @Test
        void addCombinationWithoutPeriod() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(
                    WHITESPACE, "Add two mana in any combination of {R} and/or {G}");

            assertThat(effect)
                    .isAddManaOfAnyCombinationEffect()
                    .hasAmount(2)
                    .hasAllowedTypes(Set.of(ManaType.RED, ManaType.GREEN));
        }

        @Test
        void addAnyOneColorWithoutPeriod() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add three mana of any one color");

            assertThat(effect).isAddManaFromSelectionEffect().hasAmount(3).hasOptionCount(5);
        }
    }

    @Nested
    class RealCards {

        @Test
        void llanowarElves() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add {G}.");

            assertThat(effect).isAddExactManaEffect().hasMana(ManaType.GREEN);
        }

        @Test
        void solRing() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add {C}{C}.");

            assertThat(effect).isAddExactManaEffect().hasMana(ManaType.COLORLESS, ManaType.COLORLESS);
        }

        @Test
        void birdOfParadise() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add one mana of any color.");

            assertThat(effect).isAddManaOfAnyCombinationEffect().hasAmount(1).allowsAllColors();
        }

        @Test
        void commandTower() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add one mana of any color.");

            assertThat(effect).isAddManaOfAnyCombinationEffect().hasAmount(1).allowsAllColors();
        }

        @Test
        void thranDynamo() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add {C}{C}{C}.");

            assertThat(effect)
                    .isAddExactManaEffect()
                    .hasMana(ManaType.COLORLESS, ManaType.COLORLESS, ManaType.COLORLESS);
        }

        @Test
        void nyxbloom_ancient() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add {G}{G}{G}.");

            assertThat(effect).isAddExactManaEffect().hasMana(ManaType.GREEN, ManaType.GREEN, ManaType.GREEN);
        }

        @Test
        void chromeMox() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add one mana of any color.");

            assertThat(effect).isAddManaOfAnyCombinationEffect().hasAmount(1).allowsAllColors();
        }

        @Test
        void cityOfBrass() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add one mana of any color.");

            assertThat(effect).isAddManaOfAnyCombinationEffect().hasAmount(1).allowsAllColors();
        }

        @Test
        void manaConfluence() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add one mana of any color.");

            assertThat(effect).isAddManaOfAnyCombinationEffect().hasAmount(1).allowsAllColors();
        }

        @Test
        void springleafDrum() {
            var effect = ManaParser.ADD_MANA_EFFECT.parseSkipping(WHITESPACE, "Add one mana of any color.");

            assertThat(effect).isAddManaOfAnyCombinationEffect().hasAmount(1).allowsAllColors();
        }
    }
}
