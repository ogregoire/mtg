package be.imgn.mtg.parse;

import static be.imgn.mtg.parse.Parser.string;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

/// Provides a fluent API for building an operator precedence grammar.
///
/// For example, to build a simple calculator parser:
///
/// ```java
/// Parser<Integer> calculator = new OperatorTable<Integer>()
///     .leftAssociative("+", (l, r) -> l + r, 10)
///     .leftAssociative("-", (l, r) -> l - r, 10)
///     .leftAssociative("*", (l, r) -> l * r, 20)
///     .rightAssociative("^", (l, r) -> pow(l, r), 30)
///     .prefix("-", n -> -n, 40)
///     .postfix("++", n -> n + 1, 40)
///     .postfix("--", n -> n - 1, 40)
///     .build(consecutive(DIGIT, "number").map(Integer::parseInt));
///
/// calculator.parse("1+2*3^2") // 19
/// calculator.parseSkipping(whitespace(), "1 + 2") // 3
/// ```
///
/// @param <T> the type of the operands and result
public final class OperatorTable<T> {
    private final Map<Integer, Prefix<T>> prefixTable = new HashMap<>();
    private final Map<Integer, Postfix<T>> postfixTable = new HashMap<>();
    private final Map<Integer, Infixl<T>> infixlTable = new HashMap<>();
    private final Map<Integer, Infixn<T>> infixnTable = new HashMap<>();
    private final Map<Integer, Infixr<T>> infixrTable = new HashMap<>();

    public OperatorTable() {}

    /// Adds a prefix operator with the given precedence to the table. The higher `precedence` value the higher
    /// precedence it is.
    public OperatorTable<T> prefix(String op, UnaryOperator<T> operator, int precedence) {
        return prefix(string(op).thenReturn(requireNonNull(operator)), precedence);
    }

    /// Adds a prefix operator with the given precedence to the table. The higher `precedence` value the higher
    /// precedence it is.
    public OperatorTable<T> prefix(Parser<? extends UnaryOperator<T>> operator, int precedence) {
        prefixTable.computeIfAbsent(precedence, k -> new Prefix<T>()).add(requireNonNull(operator));
        return this;
    }

    /// Adds a postfix operator with the given precedence to the table. The higher `precedence` value the higher
    /// precedence it is.
    public OperatorTable<T> postfix(String op, UnaryOperator<T> operator, int precedence) {
        return postfix(string(op).thenReturn(requireNonNull(operator)), precedence);
    }

    /// Adds a postfix operator with the given precedence to the table. This binary operator will call
    /// `postfixFunction.apply(operand, postfixValue)`, where `postfixValue` is the result of the postfix
    /// `operator` parser. The higher `precedence` value the higher precedence it is.
    public <S> OperatorTable<T> postfix(
            Parser<S> operator, BiFunction<? super T, ? super S, ? extends T> postfixFunction, int precedence) {
        requireNonNull(postfixFunction);
        return postfix(operator.map(suffix -> operand -> postfixFunction.apply(operand, suffix)), precedence);
    }

    /// Adds a postfix operator with the given precedence to the table. The higher `precedence` value the higher
    /// precedence it is.
    public OperatorTable<T> postfix(Parser<? extends UnaryOperator<T>> operator, int precedence) {
        postfixTable.computeIfAbsent(precedence, k -> new Postfix<T>()).add(requireNonNull(operator));
        return this;
    }

    /// Adds a left-associative infix operator with the given precedence to the table. The higher `precedence`
    /// value the higher precedence it is.
    public OperatorTable<T> leftAssociative(String op, BinaryOperator<T> operator, int precedence) {
        return leftAssociative(string(op).thenReturn(requireNonNull(operator)), precedence);
    }

    /// Adds a left-associative infix operator with the given precedence to the table. The higher `precedence`
    /// value the higher precedence it is.
    public OperatorTable<T> leftAssociative(Parser<? extends BinaryOperator<T>> operator, int precedence) {
        infixlTable.computeIfAbsent(precedence, k -> new Infixl<T>()).add(requireNonNull(operator));
        return this;
    }

    /// Adds a non-associative infix operator (e.g. a comparison operator like '>'), with the given precedence to the
    /// table. The higher `precedence` value the higher precedence it is.
    public OperatorTable<T> nonAssociative(String op, BinaryOperator<T> operator, int precedence) {
        requireNonNull(operator);
        return nonAssociative(string(op).thenReturn(operator), precedence);
    }

    /// Adds a non-associative infix operator (e.g. a comparison operator like '>'), with the given precedence to the
    /// table. The higher `precedence` value the higher precedence it is.
    public OperatorTable<T> nonAssociative(Parser<? extends BinaryOperator<T>> operator, int precedence) {
        infixnTable.computeIfAbsent(precedence, k -> new Infixn<T>()).add(requireNonNull(operator));
        return this;
    }

    /// Adds a right-associative infix operator with the given precedence to the table. The higher `precedence`
    /// value the higher precedence it is.
    public OperatorTable<T> rightAssociative(String op, BinaryOperator<T> operator, int precedence) {
        return rightAssociative(string(op).thenReturn(requireNonNull(operator)), precedence);
    }

    /// Adds a right-associative infix operator with the given precedence to the table. The higher `precedence`
    /// value the higher precedence it is.
    public OperatorTable<T> rightAssociative(Parser<? extends BinaryOperator<T>> operator, int precedence) {
        infixrTable.computeIfAbsent(precedence, k -> new Infixr<T>()).add(requireNonNull(operator));
        return this;
    }

    /// Builds a parser with the configured operators applied to the given operand.
    public Parser<T> build(Parser<? extends T> operand) {
        @SuppressWarnings("unchecked")
        var result = (Parser<T>) operand;
        // higher precedence first and then stable order by
        // prefix -> postfix -> infixl -> infixn -> infixr
        var tables = List.<Map<Integer, ? extends OperatorGroup<T>>>of(
                prefixTable, postfixTable, infixlTable, infixnTable, infixrTable);
        var entries = new ArrayList<Map.Entry<Integer, ? extends OperatorGroup<T>>>();
        for (var table : tables) {
            entries.addAll(table.entrySet());
        }
        entries.sort((a, b) -> Integer.compare(b.getKey(), a.getKey())); // descending
        for (var entry : entries) {
            result = entry.getValue().makeExpressionParser(result);
        }
        return result;
    }

    private interface OperatorGroup<T> {
        Parser<T> makeExpressionParser(Parser<T> operand);
    }

    private static final class Prefix<T> extends Unary<T> {
        @Override
        public Parser<T> makeExpressionParser(Parser<T> operand) {
            return operand.prefix(opParser());
        }
    }

    private static final class Postfix<T> extends Unary<T> {
        @Override
        public Parser<T> makeExpressionParser(Parser<T> operand) {
            return operand.postfix(opParser());
        }
    }

    private static final class Infixl<T> extends Binary<T> {
        @Override
        public Parser<T> makeExpressionParser(Parser<T> operand) {
            return operand.postfix(opWithRightHandSide(operand));
        }
    }

    private static final class Infixn<T> extends Binary<T> {
        @Override
        public Parser<T> makeExpressionParser(Parser<T> operand) {
            return operand.optionalPostfix(opWithRightHandSide(operand));
        }
    }

    private static final class Infixr<T> extends Binary<T> {
        @Override
        public Parser<T> makeExpressionParser(Parser<T> operand) {
            return Parser.sequence(
                    operand, Parser.sequence(opParser(), operand, Rhs::new).zeroOrMore(), (left, rights) -> {
                        if (rights.isEmpty()) {
                            return left;
                        }
                        var rhsResult = rights.getLast().value();
                        for (var i = rights.size() - 1; i > 0; i--) {
                            rhsResult =
                                    rights.get(i).op().apply(rights.get(i - 1).value(), rhsResult);
                        }
                        return rights.getFirst().op().apply(left, rhsResult);
                    });
        }

        private record Rhs<T>(BinaryOperator<T> op, T value) {}
    }

    private abstract static class Unary<T> implements OperatorGroup<T> {
        private final List<Parser<? extends UnaryOperator<T>>> peers = new ArrayList<>();

        final void add(Parser<? extends UnaryOperator<T>> op) {
            peers.add(op);
        }

        final Parser<UnaryOperator<T>> opParser() {
            return peers.stream().collect(Parser.or());
        }
    }

    private abstract static class Binary<T> implements OperatorGroup<T> {
        private final List<Parser<? extends BinaryOperator<T>>> peers = new ArrayList<>();

        final void add(Parser<? extends BinaryOperator<T>> op) {
            peers.add(op);
        }

        final Parser<UnaryOperator<T>> opWithRightHandSide(Parser<T> operand) {
            return Parser.sequence(opParser(), operand, (op, right) -> left -> op.apply(left, right));
        }

        final Parser<BinaryOperator<T>> opParser() {
            return peers.stream().collect(Parser.or());
        }
    }
}
