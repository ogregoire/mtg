package be.imgn.mtg.parse;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.io.Reader;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jspecify.annotations.Nullable;

/// A simple recursive descent parser combinator for parsing simple grammars such as regex, csv, format string patterns,
/// and MTG oracle text.
///
/// Different from most parser combinators (such as Haskell Parsec), a common source of bugs (infinite loop or
/// StackOverflowError caused by accidental zero-consumption rule in the context of many() or recursive grammar) is made
/// impossible by requiring all parsers to consume at least one character. Optional suffix is achieved through using the
/// built-in combinators such as {@link #optionallyFollowedBy optionallyFollowedBy()} and {@link #postfix postfix()}; or
/// you can use the {@link #zeroOrMore zeroOrMore()}, {@link #zeroOrMoreDelimitedBy zeroOrMoreDelimitedBy()},
/// {@link #orElse orElse()} and {@link #optional optional()} fluent chains.
///
/// For simplicity, {@link #or or()} and {@link #anyOf anyOf()} will always backtrack upon failure. But it's more
/// efficient to factor out common left prefix. For example instead of `anyOf(expr.followedBy(";"), expr)`, use
/// `expr.optionallyFollowedBy(";")` instead.
///
/// WARNING: A poorly-written grammar with long common prefixes may incur expensive backtracking overhead. And if you
/// define recursive grammars using {@link #define define()} or {@link Parser.Rule}, maliciously crafted input (think of
/// 10K left parens in an expression parser) can cause StackOverflowError.
///
/// @param <T> the type of the parsed result
public abstract class Parser<T> {

    /// Only use in context where input consumption is guaranteed. Do not use within a loop, like atLeastOnce(),
    /// zeroOrMore()!
    private static final Parser<Void> UNSAFE_EOF = new Parser<>() {
        @Override
        MatchResult<Void> skipAndMatch(@Nullable Parser<?> skip, CharInput input, int start, ErrorContext context) {
            start = skipIfAny(skip, input, start);
            return input.isEof(start) ? new MatchResult.Success<>(start, start, null) : context.expecting("EOF", start);
        }
    };

    Parser() {}

    /// Matches a character as specified by `matcher`.
    public static Parser<Character> single(CharPredicate matcher, String name) {
        requireNonNull(matcher);
        requireNonNull(name);
        return new Parser<>() {
            @Override
            MatchResult<Character> skipAndMatch(
                    @Nullable Parser<?> skip, CharInput input, int start, ErrorContext context) {
                start = skipIfAny(skip, input, start);
                if (input.isInRange(start) && matcher.test(input.charAt(start))) {
                    return new MatchResult.Success<>(start, start + 1, input.charAt(start));
                }
                return context.expecting(name, start);
            }
        };
    }

    /// Matches one or more consecutive characters as specified by `matcher`.
    public static Parser<String> consecutive(CharPredicate matcher, String name) {
        return skipConsecutive(matcher, name).source();
    }

    /// Matches one or more consecutive characters contained in `characterSet`.
    ///
    /// For example:
    ///
    /// ```java
    /// import static be.imgn.mtg.parse.CharacterSet.charsIn;
    ///
    /// Parser<Integer> hexNumber = consecutive(charsIn("[0-9A-Fa-f]"))
    ///     .map(hex -> Integer.parseInt(hex, 16));
    /// ```
    public static Parser<String> consecutive(CharacterSet characterSet) {
        return consecutive(characterSet, "one or more " + characterSet);
    }

    private static Parser<Void> skipConsecutive(CharPredicate matcher, String name) {
        requireNonNull(matcher);
        requireNonNull(name);
        return new Parser<>() {
            @Override
            MatchResult<Void> skipAndMatch(@Nullable Parser<?> skip, CharInput input, int start, ErrorContext context) {
                start = skipIfAny(skip, input, start);
                var end = start;
                for (; input.isInRange(end) && matcher.test(input.charAt(end)); end++) {}
                return end > start ? new MatchResult.Success<>(start, end, null) : context.expecting(name, end);
            }
        };
    }

    /// Consumes exactly `n` consecutive characters. `n` must be positive.
    public static Parser<String> chars(int n) {
        checkArgument(n > 0, "chars count (%s) must be positive", n);
        var name = n + " char(s)";
        return new Parser<>() {
            @Override
            MatchResult<String> skipAndMatch(
                    @Nullable Parser<?> skip, CharInput input, int start, ErrorContext context) {
                start = skipIfAny(skip, input, start);
                return input.isInRange(start + n - 1)
                        ? new MatchResult.Success<>(start, start + n, input.snippet(start, n))
                        : context.expecting(name, start);
            }
        };
    }

    /// `word("or")` matches "or" but not "orange".
    public static Parser<String> word(String word) {
        return string(word).notImmediatelyFollowedBy(CharPredicate.WORD, "[a-zA-Z0-9_]");
    }

    /// One or more regex `\w+` characters.
    public static Parser<String> word() {
        return consecutive(CharPredicate.WORD, "word");
    }

    /// One or more regex `\d+` characters.
    public static Parser<String> digits() {
        return consecutive(CharPredicate.range('0', '9'), "digits");
    }

    /// Returns a parser that finds the first literal `string` that may start from the current position or after
    /// any number of characters.
    ///
    /// Useful when you need to skip characters until a particular anchor point.
    public static Parser<String> first(String target) {
        checkArgument(target.length() > 0, "target cannot be empty");
        return new Parser<>() {
            @Override
            MatchResult<String> skipAndMatch(
                    @Nullable Parser<?> skip, CharInput input, int start, ErrorContext context) {
                // Unlike other parsers, first() doesn't apply the skip parser first.
                var found = input.indexOf(target, start);
                if (found >= 0) {
                    return new MatchResult.Success<>(found, found + target.length(), target);
                }
                return context.expecting(target, skipIfAny(skip, input, start));
            }
        };
    }

    /// Matches a literal `string`.
    public static Parser<String> string(String value) {
        checkArgument(value.length() > 0, "value cannot be empty");
        return new Parser<>() {
            @Override
            MatchResult<String> skipAndMatch(
                    @Nullable Parser<?> skip, CharInput input, int start, ErrorContext context) {
                start = skipIfAny(skip, input, start);
                if (input.startsWith(value, start)) {
                    return new MatchResult.Success<>(start, start + value.length(), value);
                }
                return context.expecting(value, start);
            }
        };
    }

    /// Matches the characters quoted by `before` and `after`, and returns the string in between.
    public static Parser<String> quotedBy(char before, char after) {
        return quotedBy(Character.toString(before), Character.toString(after));
    }

    /// Matches the characters quoted by `before` and `after`, and returns the string in between.
    public static Parser<String> quotedBy(String before, String after) {
        return quotedBy(string(before), first(after));
    }

    private static Parser<String> quotedBy(Parser<?> before, Parser<?> after) {
        requireNonNull(after);
        return before.then(new Parser<>() {
            @Override
            MatchResult<String> skipAndMatch(
                    @Nullable Parser<?> skip, CharInput input, int start, ErrorContext context) {
                return switch (after.skipAndMatch(skip, input, start, context)) {
                    case MatchResult.Success<?> success ->
                        new MatchResult.Success<>(start, success.tail(), input.snippet(start, success.head() - start));
                    case MatchResult.Failure<?> failure -> failure.safeCast();
                };
            }
        });
    }

    /// String literal quoted by `quoteChar` with backslash escapes.
    ///
    /// When a backslash is encountered, the `escaped` parser is used to parse the escaped character(s).
    public static Parser<String> quotedByWithEscapes(char before, char after, Parser<? extends CharSequence> escaped) {
        var escape = string("\\").then(escaped);
        checkArgument(before != '\\', "quoteChar cannot be '\\'");
        checkArgument(after != '\\', "quoteChar cannot be '\\'");
        checkArgument(!Character.isISOControl(before), "quoteChar cannot be a control character");
        checkArgument(!Character.isISOControl(after), "quoteChar cannot be a control character");
        return anyOf(consecutive(CharPredicate.isNot(after).and(CharPredicate.isNot('\\')), "quoted chars"), escape)
                .zeroOrMore(Collectors.joining())
                .immediatelyBetween(Character.toString(before), Character.toString(after));
    }

    /// Parses a 4-digit hex BMP code unit.
    public static Parser<Integer> bmpCodeUnit() {
        return chars(4).suchThat(
                        CharPredicate.range('0', '9').orRange('A', 'F').orRange('a', 'f')::matchesAllOf,
                        "4 hex digits UTF-16 code unit")
                .map(digits -> Integer.parseInt(digits, 16));
    }

    /// Sequentially matches `left` then `right`, and then combines the results using the `combiner`
    /// function.
    public static <A, B, C> Parser<C> sequence(
            Parser<A> left, Parser<B> right, BiFunction<? super A, ? super B, ? extends C> combiner) {
        requireNonNull(right);
        requireNonNull(combiner);
        return left.flatMap(v1 -> right.map(v2 -> combiner.apply(v1, v2)));
    }

    /// Sequentially matches `left` then `right` (which is allowed to be optional), and then combines the
    /// results using the `combiner` function.
    public static <A, B, C> Parser<C> sequence(
            Parser<A> left, Parser<B>.OrEmpty right, BiFunction<? super A, ? super B, ? extends C> combiner) {
        return sequence(left, right.asUnsafeZeroWidthParser(), combiner);
    }

    /// Sequentially matches `left` then `right`, with both allowed to be optional, and then combines the
    /// results using the `combiner` function.
    public static <A, B, C> Parser<C>.OrEmpty sequence(
            Parser<A>.OrEmpty left, Parser<B>.OrEmpty right, BiFunction<? super A, ? super B, ? extends C> combiner) {
        return anyOf(
                sequence(left.notEmpty(), right, combiner),
                right.notEmpty().map(v2 -> combiner.apply(left.computeDefaultValue(), v2)))
        .new OrEmpty(() -> combiner.apply(left.computeDefaultValue(), right.computeDefaultValue()));
    }

    /// Sequentially matches `left` (which is allowed to be optional), then `right`, and then combines the
    /// results using the `combiner` function.
    static <A, B, C> Parser<C> sequence(
            Parser<A>.OrEmpty left, Parser<B> right, BiFunction<? super A, ? super B, ? extends C> combiner) {
        return sequence(left.asUnsafeZeroWidthParser(), right, combiner);
    }

    /// Sequentially matches `a`, `b` and `c`, and then combines the results using the `combiner`
    /// function.
    public static <A, B, C, T> Parser<T> sequence(
            Parser<A> a, Parser<B> b, Parser<C> c, TriFunction<? super A, ? super B, ? super C, ? extends T> combiner) {
        requireNonNull(combiner);
        return sequence(
                a,
                sequence(b, c, AbstractMap.SimpleImmutableEntry<B, C>::new),
                (v1, bc) -> combiner.apply(v1, bc.getKey(), bc.getValue()));
    }

    /// Sequentially matches `a`, `b`, `c` and `d`, and then combines the results using the
    /// `combiner` function.
    public static <A, B, C, D, T> Parser<T> sequence(
            Parser<A> a,
            Parser<B> b,
            Parser<C> c,
            Parser<D> d,
            Function4<? super A, ? super B, ? super C, ? super D, ? extends T> combiner) {
        requireNonNull(combiner);
        return sequence(
                sequence(a, b, AbstractMap.SimpleImmutableEntry<A, B>::new),
                sequence(c, d, AbstractMap.SimpleImmutableEntry<C, D>::new),
                (ab, cd) -> combiner.apply(ab.getKey(), ab.getValue(), cd.getKey(), cd.getValue()));
    }

    /// Matches if any of the given `parsers` match.
    @SafeVarargs
    public static <T> Parser<T> anyOf(Parser<? extends T>... parsers) {
        return stream(parsers).collect(or());
    }

    /// Returns a collector that results in a parser that matches if any of the input `parsers` match.
    public static <T> Collector<Parser<? extends T>, ?, Parser<T>> or() {
        return collectingAndThen(toUnmodifiableList(), parsers -> {
            checkArgument(parsers.size() > 0, "parsers cannot be empty");
            if (parsers.size() == 1) {
                @SuppressWarnings("unchecked")
                var parser = (Parser<T>) parsers.get(0);
                return parser;
            }
            return new Parser<T>() {
                @Override
                MatchResult<T> skipAndMatch(
                        @Nullable Parser<?> skip, CharInput input, int start, ErrorContext context) {
                    MatchResult.Failure<?> farthestFailure = null;
                    for (var parser : parsers) {
                        switch (parser.skipAndMatch(skip, input, start, context)) {
                            case MatchResult.Success(int head, int tail, T value) -> {
                                return new MatchResult.Success<>(head, tail, value);
                            }
                            case MatchResult.Failure<?> failure -> {
                                if (farthestFailure == null || farthestFailure.at() < failure.at()) {
                                    farthestFailure = failure;
                                }
                            }
                        }
                    }
                    // farthestFailure cannot be null here: parsers is non-empty, so at least one failure must have been
                    // recorded
                    return requireNonNull(farthestFailure).safeCast();
                }
            };
        });
    }

    /// Matches if `this` or `that` matches.
    public final Parser<T> or(Parser<? extends T> that) {
        return anyOf(this, that);
    }

    /// Matches if `this` or `that` matches. If both failed to match, use the default result specified in
    /// `that`.
    public final Parser<T>.OrEmpty or(Parser<? extends T>.OrEmpty that) {
        return or(that.notEmpty()).new OrEmpty(that.defaultSupplier);
    }

    /// Returns a parser that applies this parser at least once, greedily.
    public final Parser<List<T>> atLeastOnce() {
        return atLeastOnce(toUnmodifiableList());
    }

    /// Returns a parser that applies this parser at least once, greedily, and reduces the results using the
    /// `reducer` function.
    public final Parser<T> atLeastOnce(BinaryOperator<T> reducer) {
        return atLeastOnce(reducing(requireNonNull(reducer))).map(Optional::get);
    }

    /// Returns a parser that applies this parser at least once, greedily, and collects the return values using
    /// `collector`.
    public final <A, R> Parser<R> atLeastOnce(Collector<? super T, A, ? extends R> collector) {
        requireNonNull(collector);
        var self = this;
        return new Parser<>() {
            @Override
            MatchResult<R> skipAndMatch(@Nullable Parser<?> skip, CharInput input, int start, ErrorContext context) {
                var buffer = collector.supplier().get();
                var accumulator = collector.accumulator();
                switch (self.skipAndMatch(skip, input, start, context)) {
                    case MatchResult.Success(int head, int tail, T value) -> {
                        accumulator.accept(buffer, value);
                        for (var from = tail; ; ) {
                            switch (self.skipAndMatch(skip, input, from, context)) {
                                case MatchResult.Success(int head2, int tail2, T value2) -> {
                                    accumulator.accept(buffer, value2);
                                    from = tail2;
                                }
                                case MatchResult.Failure<?> failure -> {
                                    return new MatchResult.Success<>(
                                            head, from, collector.finisher().apply(buffer));
                                }
                            }
                        }
                    }
                    case MatchResult.Failure<?> failure -> {
                        return failure.safeCast();
                    }
                }
            }
        };
    }

    /// Returns a parser that matches `this` pattern at least once, delimited by the given delimiter.
    public final Parser<List<T>> atLeastOnceDelimitedBy(String delimiter) {
        return atLeastOnceDelimitedBy(delimiter, toUnmodifiableList());
    }

    /// Returns a parser that matches `this` pattern at least once, delimited by the given delimiter, using the
    /// given `reducer` function to reduce the results.
    public final Parser<T> atLeastOnceDelimitedBy(String delimiter, BinaryOperator<T> reducer) {
        return atLeastOnceDelimitedBy(delimiter, reducing(requireNonNull(reducer)))
                .map(Optional::get);
    }

    /// Returns a parser that matches `this` pattern at least once, delimited by the given delimiter.
    public final <A, R> Parser<R> atLeastOnceDelimitedBy(
            String delimiter, Collector<? super T, A, ? extends R> collector) {
        requireNonNull(collector);
        return sequence(
                this, string(delimiter).then(this).zeroOrMore(toCollection(ArrayDeque::new)), (first, deque) -> {
                    deque.addFirst(first);
                    return deque.stream().collect(collector);
                });
    }

    /// Starts a fluent chain for matching consecutive `charsToMatch` zero or more times.
    public static Parser<String>.OrEmpty zeroOrMore(CharPredicate charsToMatch, String name) {
        return consecutive(charsToMatch, name).orElse("");
    }

    /// Starts a fluent chain for matching the current parser zero or more times.
    public final Parser<List<T>>.OrEmpty zeroOrMore() {
        return zeroOrMore(toUnmodifiableList());
    }

    /// Starts a fluent chain for matching the current parser zero or more times.
    public final <A, R> Parser<R>.OrEmpty zeroOrMore(Collector<? super T, A, ? extends R> collector) {
        return this.<A, R>atLeastOnce(collector).new OrEmpty(emptyValueSupplier(collector));
    }

    /// Starts a fluent chain for matching the current parser zero or more times, delimited by `delimiter`.
    public final Parser<List<T>>.OrEmpty zeroOrMoreDelimitedBy(String delimiter) {
        return zeroOrMoreDelimitedBy(delimiter, toUnmodifiableList());
    }

    /// Starts a fluent chain for matching the current parser zero or more times, delimited by `delimiter`.
    public final <A, R> Parser<R>.OrEmpty zeroOrMoreDelimitedBy(
            String delimiter, Collector<? super T, A, ? extends R> collector) {
        return this.<A, R>atLeastOnceDelimitedBy(delimiter, collector).new OrEmpty(emptyValueSupplier(collector));
    }

    /// Applies `first` and `second` patterns in order, for zero or more times, collecting the results using
    /// the provided {@link BiCollector}.
    public static <A, B, R> Parser<R>.OrEmpty zeroOrMoreDelimited(
            Parser<A> first, Parser<B> second, String delimiter, BiCollector<? super A, ? super B, R> collector) {
        return sequence(first, second, Both::of)
                .zeroOrMoreDelimitedBy(delimiter, BiCollector.toBothCollector(collector));
    }

    /// Returns a parser that applies the `operator` parser zero or more times before `this` and applies the
    /// result unary operator functions iteratively.
    public final Parser<T> prefix(Parser<? extends UnaryOperator<T>> operator) {
        return sequence(operator.zeroOrMore(), this, (ops, operand) -> applyOperators(ops.reversed(), operand));
    }

    /// Returns a parser that after this parser succeeds, applies the `operator` parser zero or more times and
    /// applies the result unary operator function iteratively.
    public final Parser<T> postfix(Parser<? extends UnaryOperator<T>> operator) {
        return sequence(this, operator.zeroOrMore(), (operand, ops) -> applyOperators(ops, operand));
    }

    /// Returns a parser that after this parser succeeds, applies the `operator` parser zero or more times and
    /// applies the result unary operator function iteratively.
    public final <S> Parser<T> postfix(
            Parser<S> operator, BiFunction<? super T, ? super S, ? extends T> postfixFunction) {
        requireNonNull(postfixFunction);
        return postfix(operator.map(postfixValue -> operand -> postfixFunction.apply(operand, postfixValue)));
    }

    /// Returns a parser that matches `this` pattern enclosed between `prefix` and `suffix`, which are
    /// non-empty string delimiters.
    public final Parser<T> between(String prefix, String suffix) {
        return between(string(prefix), string(suffix));
    }

    /// Returns a parser that matches `this` pattern enclosed between `prefix` and `suffix`.
    public final Parser<T> between(Parser<?> prefix, Parser<?> suffix) {
        return prefix.then(this).followedBy(suffix);
    }

    /// Returns a parser that matches `this` pattern enclosed between `prefix` and `suffix`, both
    /// allowed to be empty.
    public final Parser<T> between(Parser<?>.OrEmpty prefix, Parser<?>.OrEmpty suffix) {
        return prefix.then(this).followedBy(suffix);
    }

    /// Returns a parser that matches `this` pattern *immediately* enclosed between `prefix` and
    /// `suffix` (no skippable characters in between).
    public final Parser<T> immediatelyBetween(String prefix, String suffix) {
        return string(prefix).then(literally(followedBy(suffix)));
    }

    /// If this parser matches, returns the result of applying the given function to the match.
    public final <R> Parser<R> map(Function<? super T, ? extends R> f) {
        requireNonNull(f);
        var self = this;
        return new Parser<>() {
            @Override
            MatchResult<R> skipAndMatch(@Nullable Parser<?> skip, CharInput input, int start, ErrorContext context) {
                return switch (self.skipAndMatch(skip, input, start, context)) {
                    case MatchResult.Success(int head, int tail, T value) ->
                        new MatchResult.Success<>(head, tail, f.apply(value));
                    case MatchResult.Failure<?> failure -> failure.safeCast();
                };
            }
        };
    }

    /// If this parser matches, applies function `f` to get the next parser to match in sequence.
    public final <R> Parser<R> flatMap(Function<? super T, Parser<R>> f) {
        requireNonNull(f);
        var self = this;
        return new Parser<>() {
            @Override
            MatchResult<R> skipAndMatch(@Nullable Parser<?> skip, CharInput input, int start, ErrorContext context) {
                return switch (self.skipAndMatch(skip, input, start, context)) {
                    case MatchResult.Success(int head, int tail, T value) ->
                        switch (f.apply(value).skipAndMatch(skip, input, tail, context)) {
                            case MatchResult.Success(int head2, int tail2, R value2) ->
                                new MatchResult.Success<>(head, tail2, value2);
                            case MatchResult.Failure<?> failure -> failure.safeCast();
                        };
                    case MatchResult.Failure<?> failure -> failure.safeCast();
                };
            }
        };
    }

    /// If this parser matches, returns the given result.
    public final <R> Parser<R> thenReturn(R result) {
        return map(unused -> result);
    }

    /// If this parser matches, applies the given parser on the remaining input.
    public final <R> Parser<R> then(Parser<R> next) {
        requireNonNull(next);
        return flatMap(unused -> next);
    }

    /// If this parser matches, applies the given optional (or zero-or-more) parser on the remaining input.
    public final <R> Parser<R> then(Parser<R>.OrEmpty next) {
        return sequence(this, next, (unused, value) -> value);
    }

    /// If this parser matches, applies the given `condition` and disqualifies the match if the condition is false.
    public final Parser<T> suchThat(Predicate<? super T> condition, String name) {
        requireNonNull(condition);
        requireNonNull(name);
        var self = this;
        return new Parser<>() {
            @Override
            MatchResult<T> skipAndMatch(@Nullable Parser<?> skip, CharInput input, int start, ErrorContext context) {
                var result = self.skipAndMatch(skip, input, start, context);
                return result instanceof MatchResult.Success<T> success && !condition.test(success.value())
                        ? context.expecting(name, success.head())
                        : result;
            }
        };
    }

    /// If this parser matches, continue to match `suffix`.
    public final Parser<T> followedBy(String suffix) {
        return followedBy(string(suffix));
    }

    /// If this parser matches, continue to match `suffix`.
    public final Parser<T> followedBy(Parser<?> suffix) {
        return sequence(this, suffix, (value, unused) -> value);
    }

    /// If this parser matches, continue to match the optional `suffix`.
    public final <X> Parser<T> followedBy(Parser<X>.OrEmpty suffix) {
        return sequence(this, suffix, (value, unused) -> value);
    }

    /// Specifies that the matched pattern must be either followed by `suffix` or EOF.
    public final Parser<T> followedByOrEof(Parser<?> suffix) {
        return followedBy(anyOf(suffix, UNSAFE_EOF));
    }

    final Parser<T> followedByEof() {
        return followedBy(UNSAFE_EOF);
    }

    /// Returns an equivalent parser except it allows `suffix` if present.
    public final Parser<T> optionallyFollowedBy(String suffix) {
        return followedBy(string(suffix).orElse(null));
    }

    /// If this parser matches, optionally applies the `op` function if the pattern is followed by `suffix`.
    public final Parser<T> optionallyFollowedBy(String suffix, Function<? super T, ? extends T> op) {
        return optionalPostfix(string(suffix).thenReturn(op::apply));
    }

    /// If this parser matches, optionally matches `suffix` with the `op` BiFunction to transform the current
    /// parser's result.
    public final <S> Parser<T> optionallyFollowedBy(
            Parser<S> suffix, BiFunction<? super T, ? super S, ? extends T> op) {
        requireNonNull(op);
        return optionalPostfix(suffix.map(s -> p -> op.apply(p, s)));
    }

    final Parser<T> optionalPostfix(Parser<UnaryOperator<T>> suffix) {
        return sequence(this, suffix.orElse(identity()), (operand, op) -> op.apply(operand));
    }

    /// A form of negative lookahead such that the match is rejected if followed by `suffix`.
    public final Parser<T> notFollowedBy(String suffix) {
        return notFollowedBy(string(suffix), suffix);
    }

    /// A form of negative lookahead such that the match is rejected if followed by `suffix`.
    public final Parser<T> notFollowedBy(Parser<?> suffix, String name) {
        requireNonNull(suffix);
        requireNonNull(name);
        var self = this;
        return new Parser<>() {
            @Override
            MatchResult<T> skipAndMatch(@Nullable Parser<?> skip, CharInput input, int start, ErrorContext context) {
                return switch (self.skipAndMatch(skip, input, start, context)) {
                    case MatchResult.Success<T> success -> {
                        var lookaheadContext = new ErrorContext(input);
                        yield switch (suffix.skipAndMatch(skip, input, success.tail(), lookaheadContext)) {
                            case MatchResult.Success<?> followed ->
                                lookaheadContext.failAt(
                                        success.tail(),
                                        "unexpected `%s` – %s.",
                                        name,
                                        new Snippet(input, success.tail()));
                            default -> success;
                        };
                    }
                    case MatchResult.Failure<T> failure -> failure;
                };
            }
        };
    }

    /// A form of negative lookahead such that the match is rejected if *immediately* followed by a character that
    /// matches `predicate`.
    public final Parser<T> notImmediatelyFollowedBy(CharPredicate predicate, String name) {
        return notFollowedBy(literally(single(predicate, name)), name);
    }

    /// Starts a fluent chain for matching the current parser optionally. `defaultValue` will be the result in case
    /// the current parser doesn't match.
    public final OrEmpty orElse(@Nullable T defaultValue) {
        return new OrEmpty(() -> defaultValue);
    }

    /// Starts a fluent chain for matching the current parser optionally. `Optional.empty()` will be the result in
    /// case the current parser doesn't match.
    public final Parser<Optional<T>>.OrEmpty optional() {
        return map(Optional::ofNullable).new OrEmpty(Optional::empty);
    }

    /// Returns a parser that matches `this` pattern and returns the matched string.
    public final Parser<String> source() {
        var self = this;
        return new Parser<String>() {
            @Override
            MatchResult<String> skipAndMatch(
                    @Nullable Parser<?> skip, CharInput input, int start, ErrorContext context) {
                return switch (self.skipAndMatch(skip, input, start, context)) {
                    case MatchResult.Success<T>(int head, int tail, T value) ->
                        new MatchResult.Success<>(head, tail, input.snippet(head, tail - head));
                    case MatchResult.Failure<T> failure -> failure.safeCast();
                };
            }
        };
    }

    /// Returns an equivalent parser that suppresses character skipping.
    public static <T> Parser<T> literally(Parser<T> parser) {
        requireNonNull(parser);
        return new Parser<T>() {
            @Override
            MatchResult<T> skipAndMatch(@Nullable Parser<?> ignored, CharInput input, int start, ErrorContext context) {
                return parser.skipAndMatch(null, input, start, context);
            }
        };
    }

    /// Specifies that the optional (or zero-or-more) `rule` should be matched literally.
    public static <T> Parser<T>.OrEmpty literally(Parser<T>.OrEmpty rule) {
        return literally(rule.notEmpty()).new OrEmpty(rule::computeDefaultValue);
    }

    /// Starts a fluent chain for parsing inputs while skipping patterns matched by `skip`.
    public final Lexical skipping(Parser<?> skip) {
        return new Lexical(skip.atLeastOnce(counting()));
    }

    /// Starts a fluent chain for parsing inputs while skipping `charsToSkip`.
    public final Lexical skipping(CharPredicate charsToSkip) {
        return new Lexical(skipConsecutive(charsToSkip, "skipped"));
    }

    /// Parses `input` while skipping patterns matched by `skip` around atomic matches.
    public final T parseSkipping(Parser<?> skip, String input) {
        return skipping(skip).parse(input);
    }

    /// Parses `input` while `charsToSkip` around atomic matches.
    public final T parseSkipping(CharPredicate charsToSkip, String input) {
        return skipping(charsToSkip).parse(input);
    }

    /// Parses the entire input string and returns the result.
    ///
    /// @throws ParseException if the input cannot be parsed.
    public final T parse(String input) {
        return parse(CharInput.from(input), 0);
    }

    /// Parses the input string starting from `fromIndex` and returns the result.
    ///
    /// @throws ParseException if the input cannot be parsed.
    public final T parse(String input, int fromIndex) {
        checkPositionIndex(fromIndex, input.length(), "fromIndex");
        return parse(CharInput.from(input), fromIndex);
    }

    private T parse(CharInput input, int fromIndex) {
        var context = new ErrorContext(input);
        var result = match(input, fromIndex, context);
        switch (result) {
            case MatchResult.Success(int head, int tail, T value) -> {
                if (!input.isEof(tail)) {
                    throw context.report(context.expecting("EOF", tail));
                }
                return value;
            }
            case MatchResult.Failure<?> failure -> {
                throw context.report(failure);
            }
        }
    }

    /// Parses the entire input string lazily by applying this parser repeatedly until the end of input.
    public final Stream<T> parseToStream(String input) {
        return parseToStream(input, 0);
    }

    /// Parses `input` starting from `fromIndex` to a lazy stream.
    public final Stream<T> parseToStream(String input, int fromIndex) {
        checkPositionIndex(fromIndex, input.length(), "fromIndex");
        return parseToStream(CharInput.from(input), fromIndex);
    }

    /// Parses the input reader lazily by applying this parser repeatedly until the end of input.
    ///
    /// @throws UncheckedIOException if the underlying reader throws
    public final Stream<T> parseToStream(Reader input) {
        return parseToStream(CharInput.from(input), 0);
    }

    final Stream<T> parseToStream(CharInput input, int fromIndex) {
        class Cursor implements Iterator<MatchResult.Success<T>> {
            private int index = fromIndex;

            @Override
            public boolean hasNext() {
                return !input.isEof(index);
            }

            @Override
            public MatchResult.Success<T> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                var context = new ErrorContext(input);
                return switch (match(input, index, context)) {
                    case MatchResult.Success<T> success -> {
                        index = success.tail();
                        input.markCheckpoint(index);
                        yield success;
                    }
                    case MatchResult.Failure<?> failure -> {
                        throw context.report(failure);
                    }
                };
            }
        }
        return StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(new Cursor(), Spliterator.ORDERED | Spliterator.NONNULL),
                        false)
                .map(MatchResult.Success::value);
    }

    /// Lazily and iteratively matches `input`, until the input is exhausted or matching failed.
    ///
    /// Note that unlike {@link #parseToStream(String) parseToStream()}, a matching failure terminates the stream
    /// without throwing exception.
    public final Stream<T> probe(String input) {
        return probe(input, 0);
    }

    /// Lazily and iteratively matches `input` starting from `fromIndex`.
    public final Stream<T> probe(String input, int fromIndex) {
        checkPositionIndex(fromIndex, input.length(), "fromIndex");
        return probe(CharInput.from(input), fromIndex);
    }

    /// Lazily and iteratively matches `input` reader.
    ///
    /// @throws UncheckedIOException if the underlying reader throws
    public final Stream<T> probe(Reader input) {
        return probe(CharInput.from(input), 0);
    }

    final Stream<T> probe(CharInput input, int fromIndex) {
        class Cursor implements Iterator<MatchResult.Success<T>> {
            private int index = fromIndex;
            private MatchResult.@Nullable Success<T> next = computeNext();

            private MatchResult.@Nullable Success<T> computeNext() {
                return switch (match(input, index, new ErrorContext(input))) {
                    case MatchResult.Success<T> success -> {
                        index = success.tail();
                        input.markCheckpoint(index);
                        yield success;
                    }
                    case MatchResult.Failure<?> failure -> null;
                };
            }

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public MatchResult.Success<T> next() {
                if (next == null) {
                    throw new NoSuchElementException();
                }
                var result = next;
                next = computeNext();
                return result;
            }
        }
        return StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(new Cursor(), Spliterator.ORDERED | Spliterator.NONNULL),
                        false)
                .map(MatchResult.Success::value);
    }

    /// Fluent API for matching the current parser optionally. This is needed because we require all parsers to match at
    /// least one character.
    public final class OrEmpty {
        private final Supplier<? extends T> defaultSupplier;

        private OrEmpty(Supplier<? extends T> defaultSupplier) {
            this.defaultSupplier = defaultSupplier;
        }

        /// The current optional (or zero-or-more) parser must be enclosed between non-empty `prefix` and
        /// `suffix`.
        public Parser<T> between(String prefix, String suffix) {
            return between(string(prefix), string(suffix));
        }

        /// The current optional (or zero-or-more) parser must be enclosed between non-empty `prefix` and
        /// `suffix`.
        public Parser<T> between(Parser<?> prefix, Parser<?> suffix) {
            return prefix.then(this).followedBy(suffix);
        }

        /// The current parser enclosed between `prefix` and `suffix`, both allowed to be empty.
        public final Parser<T>.OrEmpty between(Parser<?>.OrEmpty prefix, Parser<?>.OrEmpty suffix) {
            return prefix.then(this).followedBy(suffix);
        }

        /// The current optional (or zero-or-more) parser must be *immediately* enclosed between non-empty
        /// `prefix` and `suffix`.
        public final Parser<T> immediatelyBetween(String prefix, String suffix) {
            return string(prefix).then(literally(followedBy(suffix)));
        }

        /// The current optional parser repeated and delimited by `delimiter`.
        public <R> Parser<R>.OrEmpty delimitedBy(String delimiter, Collector<? super T, ?, R> collector) {
            return sequence(
                    this, string(delimiter).then(this).zeroOrMore(toCollection(ArrayDeque::new)), (first, deque) -> {
                        deque.addFirst(first);
                        return deque.stream().collect(collector);
                    });
        }

        /// The current optional parser repeated and delimited by `delimiter`.
        public Parser<List<T>>.OrEmpty delimitedBy(String delimiter) {
            return delimitedBy(delimiter, toUnmodifiableList());
        }

        /// After matching the current optional (or zero-or-more) parser, proceed to match `suffix`.
        public <S> Parser<S>.OrEmpty then(Parser<S>.OrEmpty suffix) {
            return sequence(this, suffix, (a, b) -> b);
        }

        /// After matching the current optional (or zero-or-more) parser, proceed to match `suffix`.
        <S> Parser<S> then(Parser<S> suffix) {
            return sequence(this, suffix, (a, b) -> b);
        }

        /// The current optional (or zero-or-more) parser must be followed by non-empty `suffix`.
        public Parser<T> followedBy(String suffix) {
            return followedBy(string(suffix));
        }

        /// The current optional (or zero-or-more) parser may optionally be followed by `suffix`.
        public <S> Parser<T>.OrEmpty followedBy(Parser<S>.OrEmpty suffix) {
            return sequence(this, suffix, (a, b) -> a);
        }

        /// The current optional (or zero-or-more) parser must be followed by non-empty `suffix`.
        Parser<T> followedBy(Parser<?> suffix) {
            return sequence(this, suffix, (a, b) -> a);
        }

        /// The current optional (or zero-or-more) parser may optionally be followed by `suffix`.
        public Parser<T>.OrEmpty optionallyFollowedBy(String suffix) {
            return followedBy(string(suffix).orElse(null));
        }

        /// Returns the otherwise equivalent `Parser` that will fail instead of returning the default value if
        /// empty.
        public Parser<T> notEmpty() {
            return Parser.this;
        }

        /// Parses the entire input string and returns the result; if input is empty, returns the default empty value.
        public T parse(String input) {
            return asUnsafeZeroWidthParser().parse(input);
        }

        /// Parses the entire input string, ignoring patterns matched by `skip`.
        public T parseSkipping(Parser<?> skip, String input) {
            return asUnsafeZeroWidthParser().parseSkipping(skip, input);
        }

        /// Parses the entire input string, ignoring `charsToSkip`.
        public T parseSkipping(CharPredicate charsToSkip, String input) {
            return parseSkipping(skipConsecutive(charsToSkip, "skipped"), input);
        }

        T computeDefaultValue() {
            return defaultSupplier.get();
        }

        /// Temporarily creates a zero-width success parser. It's a crippled parser, not safe to be used in a loop and
        /// must be carefully composed with a parser that does consume!
        private Parser<T> asUnsafeZeroWidthParser() {
            return new Parser<T>() {
                @Override
                MatchResult<T> skipAndMatch(
                        @Nullable Parser<?> skip, CharInput input, int start, ErrorContext context) {
                    return switch (notEmpty().skipAndMatch(skip, input, start, context)) {
                        case MatchResult.Success<T> success -> success;
                        default -> new MatchResult.Success<>(start, start, computeDefaultValue());
                    };
                }
            };
        }
    }

    /// Fluent API for parsing while skipping patterns around lexical tokens.
    public final class Lexical {
        private final Parser<?> toSkip;

        private Lexical(Parser<?> toSkip) {
            this.toSkip = toSkip;
        }

        /// Parses `input` while skipping the skippable patterns around lexical tokens.
        public T parse(String input) {
            return forTokens().parse(input);
        }

        /// Parses `input` starting from `fromIndex` while skipping patterns around lexical tokens.
        public T parse(String input, int fromIndex) {
            return forTokens().parse(input, fromIndex);
        }

        /// Parses `input` to a lazy stream while skipping the skippable patterns around lexical tokens.
        public Stream<T> parseToStream(String input) {
            return parseToStream(input, 0);
        }

        /// Parses `input` starting from `fromIndex` to a lazy stream.
        public Stream<T> parseToStream(String input, int fromIndex) {
            checkPositionIndex(fromIndex, input.length(), "fromIndex");
            return parseToStream(CharInput.from(input), fromIndex);
        }

        /// Parses `input` reader to a lazy stream.
        ///
        /// @throws UncheckedIOException if the underlying reader throws
        public Stream<T> parseToStream(Reader input) {
            return parseToStream(CharInput.from(input), 0);
        }

        Stream<T> parseToStream(CharInput input, int fromIndex) {
            var context = new ErrorContext(input);
            if (toSkip.match(input, fromIndex, context) instanceof MatchResult.Success<?> success
                    && input.isEof(success.tail())) {
                return Stream.empty();
            }
            return forTokens().parseToStream(input, fromIndex);
        }

        /// Lazily and iteratively matches `input`, skipping the skippable patterns.
        public Stream<T> probe(String input) {
            return forTokens().probe(input);
        }

        /// Lazily and iteratively matches `input` starting from `fromIndex`.
        public Stream<T> probe(String input, int fromIndex) {
            return forTokens().probe(input, fromIndex);
        }

        /// Lazily and iteratively matches `input` reader.
        ///
        /// @throws UncheckedIOException if the underlying reader throws
        public Stream<T> probe(Reader input) {
            return forTokens().probe(input);
        }

        private Parser<T> forTokens() {
            var self = Parser.this;
            return new Parser<T>() {
                @Override
                MatchResult<T> skipAndMatch(
                        @Nullable Parser<?> ignored, CharInput input, int start, ErrorContext context) {
                    return self.skipAndMatch(toSkip, input, start, context);
                }

                @Override
                MatchResult<T> match(CharInput input, int start, ErrorContext context) {
                    return switch (super.match(input, start, context)) {
                        case MatchResult.Success(int head, int tail, T value) ->
                            new MatchResult.Success<>(head, skipIfAny(toSkip, input, tail), value);
                        case MatchResult.Failure<T> failure -> failure;
                    };
                }
            };
        }
    }

    /// Defines a simple recursive grammar without needing to explicitly forward-declare a {@link Rule}.
    public static <T> Parser<T> define(Function<? super Parser<T>, ? extends Parser<? extends T>> definition) {
        var rule = new Rule<T>();
        @SuppressWarnings("unchecked")
        var parser = (Parser<T>) rule.definedAs(definition.apply(rule));
        return parser;
    }

    /// A forward-declared grammar rule, to be used for recursive grammars.
    public static final class Rule<T> extends Parser<T> {
        private final AtomicReference<Parser<T>> ref = new AtomicReference<>();

        @Override
        MatchResult<T> skipAndMatch(@Nullable Parser<?> skip, CharInput input, int start, ErrorContext context) {
            var p = requireNonNull(ref.get(), "definedAs() should have been called before parse()");
            return p.skipAndMatch(skip, input, start, context);
        }

        /// Define this rule as `parser` and returns it.
        @SuppressWarnings("unchecked")
        public <S extends T> Parser<S> definedAs(Parser<S> parser) {
            requireNonNull(parser);
            checkArgument(!(parser instanceof Rule), "Do not delegate to a Rule parser");
            checkState(ref.compareAndSet(null, (Parser<T>) parser), "definedAs() already called");
            return parser;
        }
    }

    /// Thrown if parsing failed.
    public static class ParseException extends IllegalArgumentException {
        private final int index;

        ParseException(int index, String message) {
            super(message);
            this.index = index;
        }

        /// Returns the index in the source where this error was detected.
        public int getSourceIndex() {
            return index;
        }
    }

    /// Matches the input string starting at the given position.
    ///
    /// @return a MatchResult containing the parsed value and the [start, end) range of the match.
    MatchResult<T> match(CharInput input, int start, ErrorContext context) {
        return skipAndMatch(null, input, start, context);
    }

    abstract MatchResult<T> skipAndMatch(@Nullable Parser<?> skip, CharInput input, int start, ErrorContext context);

    static int skipIfAny(@Nullable Parser<?> skip, CharInput input, int start) {
        if (skip == null) {
            return start;
        }
        return switch (skip.match(input, start, new ErrorContext(input))) {
            case MatchResult.Success<?> success -> success.tail();
            case MatchResult.Failure<?> failure -> start;
        };
    }

    sealed interface MatchResult<V> permits MatchResult.Success, MatchResult.Failure {
        /// Represents a successful parse result with a value and the [head, tail) range of the match.
        record Success<V>(int head, int tail, @Nullable V value) implements MatchResult<V> {}

        /// Represents a failed parse result.
        @SuppressWarnings("ArrayRecordComponent") // Array for varargs format args is intentional
        record Failure<V>(int at, String message, Object[] args) implements MatchResult<V> {
            @SuppressWarnings("unchecked")
            <X> Failure<X> safeCast() {
                return (Failure<X>) this;
            }

            ParseException toException(CharInput input) {
                return new ParseException(
                        at, String.format("at %s: %s", input.sourcePosition(at), String.format(message, args)));
            }
        }
    }

    private static final class ErrorContext {
        private final CharInput input;

        private MatchResult.@Nullable Failure<?> farthestFailure = null;

        ErrorContext(CharInput input) {
            this.input = input;
        }

        <V> MatchResult.Failure<V> expecting(String name, int at) {
            return failAt(at, "expecting <%s>, encountered %s.", name, new Snippet(input, at));
        }

        <V> MatchResult.Failure<V> failAt(int at, String message, Object... args) {
            var failure = new MatchResult.Failure<V>(at, message, args);
            if (farthestFailure == null || failure.at() >= farthestFailure.at()) {
                farthestFailure = failure;
            }
            return failure;
        }

        ParseException report(MatchResult.Failure<?> failure) {
            return (farthestFailure == null || failure.at() >= farthestFailure.at())
                    ? failure.toException(input)
                    : farthestFailure.toException(input);
        }
    }

    private static <A, T> Supplier<T> emptyValueSupplier(Collector<?, A, ? extends T> collector) {
        var supplier = collector.supplier();
        var finisher = collector.finisher();
        return () -> finisher.apply(supplier.get());
    }

    private static <T> T applyOperators(Iterable<? extends UnaryOperator<T>> ops, T operand) {
        for (var op : ops) {
            operand = op.apply(operand);
        }
        return operand;
    }

    record Snippet(CharInput input, int at) {
        Snippet(String input, int at) {
            this(CharInput.from(input), at);
        }

        @Override
        public String toString() {
            if (input.isEof(at)) {
                return "<EOF>";
            }
            var snippet = input.snippet(at, 50);
            // Trim to first whitespace or max 50 chars
            var end = 0;
            while (end < snippet.length() && !Character.isWhitespace(snippet.charAt(end))) {
                end++;
            }
            if (end == 0 && snippet.length() > 0) {
                end = Math.min(3, snippet.length());
            }
            snippet = snippet.substring(0, end);
            return "[" + (input.isInRange(at + snippet.length()) ? snippet + "..." : snippet) + "]";
        }
    }

    private static void checkArgument(boolean condition, String message, Object... args) {
        if (!condition) {
            throw new IllegalArgumentException(String.format(message, args));
        }
    }

    private static void checkState(boolean condition, String message, Object... args) {
        if (!condition) {
            throw new IllegalStateException(String.format(message, args));
        }
    }

    private static int checkPositionIndex(int index, int size, String name) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException(
                    String.format("%s (%s) must be in range of [0, %s]", name, index, size));
        }
        return index;
    }
}
