package be.imgn.mtg.parse;

import static be.imgn.mtg.parse.Parser.anyOf;
import static be.imgn.mtg.parse.Parser.digits;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OperatorTableTest {

    @Test
    void simpleAddition() {
        var calculator = new OperatorTable<Integer>()
                .leftAssociative("+", Integer::sum, 10)
                .build(digits().map(Integer::parseInt));

        assertThat(calculator.parse("1")).isEqualTo(1);
        assertThat(calculator.parseSkipping(Character::isWhitespace, "1 + 2")).isEqualTo(3);
        assertThat(calculator.parseSkipping(Character::isWhitespace, "1 + 2 + 3"))
                .isEqualTo(6);
    }

    @Test
    void simpleSubtraction() {
        var calculator = new OperatorTable<Integer>()
                .leftAssociative("-", (a, b) -> a - b, 10)
                .build(digits().map(Integer::parseInt));

        assertThat(calculator.parseSkipping(Character::isWhitespace, "5 - 3")).isEqualTo(2);
        assertThat(calculator.parseSkipping(Character::isWhitespace, "10 - 3 - 2"))
                .isEqualTo(5);
    }

    @Test
    void mixedPrecedence() {
        var calculator = new OperatorTable<Integer>()
                .leftAssociative("+", Integer::sum, 10)
                .leftAssociative("*", (a, b) -> a * b, 20)
                .build(digits().map(Integer::parseInt));

        // 1 + 2 * 3 = 1 + 6 = 7 (not (1 + 2) * 3 = 9)
        assertThat(calculator.parseSkipping(Character::isWhitespace, "1 + 2 * 3"))
                .isEqualTo(7);
    }

    @Test
    void prefixOperator() {
        var calculator = new OperatorTable<Integer>()
                .prefix("-", n -> -n, 30)
                .leftAssociative("+", Integer::sum, 10)
                .build(digits().map(Integer::parseInt));

        assertThat(calculator.parseSkipping(Character::isWhitespace, "-5")).isEqualTo(-5);
        assertThat(calculator.parseSkipping(Character::isWhitespace, "--5")).isEqualTo(5);
        assertThat(calculator.parseSkipping(Character::isWhitespace, "3 + -5")).isEqualTo(-2);
    }

    @Test
    void postfixOperator() {
        var calculator = new OperatorTable<Integer>()
                .postfix("++", n -> n + 1, 30)
                .leftAssociative("+", Integer::sum, 10)
                .build(digits().map(Integer::parseInt));

        assertThat(calculator.parse("5++")).isEqualTo(6);
        assertThat(calculator.parse("5++++")).isEqualTo(7);
    }

    @Test
    void rightAssociative() {
        var calculator = new OperatorTable<Integer>()
                .rightAssociative("^", (a, b) -> (int) Math.pow(a, b), 30)
                .build(digits().map(Integer::parseInt));

        // 2 ^ 3 ^ 2 = 2 ^ 9 = 512 (right associative)
        assertThat(calculator.parseSkipping(Character::isWhitespace, "2 ^ 3 ^ 2"))
                .isEqualTo(512);
    }

    @Test
    void nonAssociative() {
        var calculator = new OperatorTable<Integer>()
                .nonAssociative("=", (a, b) -> a.equals(b) ? 1 : 0, 10)
                .build(digits().map(Integer::parseInt));

        assertThat(calculator.parseSkipping(Character::isWhitespace, "5 = 5")).isEqualTo(1);
        assertThat(calculator.parseSkipping(Character::isWhitespace, "5 = 3")).isEqualTo(0);
    }

    @Test
    void fullCalculator() {
        var rule = new Parser.Rule<Integer>();
        var atom = anyOf(rule.between("(", ")"), digits().map(Integer::parseInt));

        var calculator = new OperatorTable<Integer>()
                .prefix("-", n -> -n, 40)
                .rightAssociative("^", (a, b) -> (int) Math.pow(a, b), 30)
                .leftAssociative("*", (a, b) -> a * b, 20)
                .leftAssociative("/", (a, b) -> a / b, 20)
                .leftAssociative("+", Integer::sum, 10)
                .leftAssociative("-", (a, b) -> a - b, 10)
                .build(atom);

        rule.definedAs(calculator);

        // Tests
        assertThat(calculator.parseSkipping(Character::isWhitespace, "1 + 2 * 3"))
                .isEqualTo(7);
        assertThat(calculator.parseSkipping(Character::isWhitespace, "(1 + 2) * 3"))
                .isEqualTo(9);
        assertThat(calculator.parseSkipping(Character::isWhitespace, "2 ^ 3 ^ 2"))
                .isEqualTo(512);
        assertThat(calculator.parseSkipping(Character::isWhitespace, "10 / 2 * 5"))
                .isEqualTo(25);
        assertThat(calculator.parseSkipping(Character::isWhitespace, "-5 + 3")).isEqualTo(-2);
        assertThat(calculator.parseSkipping(Character::isWhitespace, "-(5 + 3)"))
                .isEqualTo(-8);
    }

    @Test
    void sameOperatorMultipleTimes() {
        var calculator = new OperatorTable<Integer>()
                .leftAssociative("+", Integer::sum, 10)
                .leftAssociative("-", (a, b) -> a - b, 10)
                .build(digits().map(Integer::parseInt));

        // Left associative: 10 - 3 + 2 = (10 - 3) + 2 = 9
        assertThat(calculator.parseSkipping(Character::isWhitespace, "10 - 3 + 2"))
                .isEqualTo(9);
    }

    @Test
    void operatorParsers() {
        var calculator = new OperatorTable<Integer>()
                .leftAssociative(Parser.string("plus").thenReturn(Integer::sum), 10)
                .leftAssociative(Parser.string("times").thenReturn((a, b) -> a * b), 20)
                .build(digits().map(Integer::parseInt));

        assertThat(calculator.parseSkipping(Character::isWhitespace, "2 plus 3 times 4"))
                .isEqualTo(14);
    }

    @Test
    void postfixWithBiFunction() {
        var calculator = new OperatorTable<Integer>()
                .postfix(
                        Parser.string("[").then(digits().map(Integer::parseInt)).followedBy("]"),
                        (base, index) -> base * 10 + index,
                        30)
                .build(digits().map(Integer::parseInt));

        assertThat(calculator.parse("5[3]")).isEqualTo(53);
    }
}
