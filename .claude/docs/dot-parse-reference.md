# Google Mug Dot-Parse Library Reference

**Package**: `com.google.common.labs.parse`
**Artifact**: `com.google.mug:dot-parse:9.9.9`
**Source**: https://github.com/google/mug/tree/master/dot-parse

Low-ceremony Java parser combinator library. Replaces regex for everyday parsing tasks.

## Class: `Parser<T>`

```java
public abstract class Parser<T>
```

### Nested Types

| Type | Description |
|------|-------------|
| `Parser.Lexical` | Fluent API for parsing while skipping patterns around lexical tokens |
| `Parser.OrEmpty` | Facilitates fluent chain for matching the current parser optionally |
| `Parser.ParseException` | Thrown if parsing failed |
| `Parser.Rule<T>` | Forward-declared grammar rule for recursive grammars |

---

## Static Factory Methods

### Character & String Matching

```java
// Single character literal
Parser<Character> one(char c)

// Single character matching a character set
Parser<Character> one(CharacterSet characterSet)

// Single character matching a predicate
Parser<Character> one(CharPredicate matcher, String name)

// One or more consecutive matching characters → String
Parser<String> consecutive(CharPredicate matcher, String name)
Parser<String> consecutive(CharacterSet characterSet)

// Exactly n characters
Parser<String> chars(int n)

// Word characters: [a-zA-Z0-9_]+
Parser<String> word()

// Specific word with word-boundary checking
Parser<String> word(String word)

// Digit characters: [0-9]+
Parser<String> digits()

// Exact literal string
Parser<String> string(String string)

// Case-insensitive literal string
Parser<?> caseInsensitive(String string)

// Case-insensitive word with word boundaries
Parser<?> caseInsensitiveWord(String word)

// First occurrence of target string (skips content before it)
Parser<String> first(String target)

// Zero or more consecutive matching characters
Parser<String>.OrEmpty zeroOrMore(CharPredicate charsToMatch, String name)
Parser<String>.OrEmpty zeroOrMore(CharacterSet characterSet)
```

### Quoted Strings

```java
// Content between delimiters (no escape handling)
Parser<String> quotedBy(char before, char after)
Parser<String> quotedBy(String before, String after)

// Content between delimiters with backslash escape support
Parser<String> quotedByWithEscapes(char before, char after,
    Parser<? extends CharSequence> escaped)
Parser<String> quotedByWithEscapes(String before, char after,
    Parser<? extends CharSequence> escaped)
```

### Unicode

```java
// Parse 4-digit hex BMP code unit (e.g., after \u)
Parser<Integer> bmpCodeUnit()
```

### Sequence Combinators

```java
// Two parsers → combined result
<A, B, C> Parser<C> sequence(Parser<A> left, Parser<B> right,
    BiFunction<? super A, ? super B, ? extends C> combiner)

// Two parsers, right optional
<A, B, C> Parser<C> sequence(Parser<A> left, Parser<B>.OrEmpty right,
    BiFunction<? super A, ? super B, ? extends C> combiner)

// Both optional
<A, B, C> Parser<C>.OrEmpty sequence(Parser<A>.OrEmpty left, Parser<B>.OrEmpty right,
    BiFunction<? super A, ? super B, ? extends C> combiner)

// Three parsers
<A, B, C, T> Parser<T> sequence(Parser<A> a, Parser<B> b, Parser<C> c,
    TriFunction<? super A, ? super B, ? super C, ? extends T> combiner)

// Four parsers
<A, B, C, D, T> Parser<T> sequence(Parser<A> a, Parser<B> b, Parser<C> c, Parser<D> d,
    Function4<? super A, ? super B, ? super C, ? super D, ? extends T> combiner)
```

### Alternation

```java
// Match any of the given parsers (tried in order)
@SafeVarargs
<T> Parser<T> anyOf(Parser<? extends T>... parsers)

// Match any of the given enum values (case-insensitive word matching)
@SafeVarargs
<T extends Enum<?>> Parser<T> anyOf(T... values)

// Collector that builds a parser matching any collected parser
<T> Collector<Parser<? extends T>, ?, Parser<T>> or()
```

### Delimited Repetition (Static)

```java
// Zero or more key-value pairs with delimiter
<A, B, R> Parser<R>.OrEmpty zeroOrMoreDelimited(Parser<A> first, Parser<B> second,
    String delimiter, BiCollector<? super A, ? super B, R> collector)

// With optional second element
<A, B, R> Parser<R>.OrEmpty zeroOrMoreDelimited(Parser<A> first, Parser<B>.OrEmpty second,
    String delimiter, BiCollector<? super A, ? super B, R> collector)
```

### Recursive Grammars

```java
// Define a self-referencing parser
<T> Parser<T> define(Function<? super Parser<T>, ? extends Parser<? extends T>> definition)
```

### Whitespace Suppression

```java
// Suppress character skipping for a sub-parser
<T> Parser<T> literally(Parser<T> parser)
<T> Parser<T>.OrEmpty literally(Parser<T>.OrEmpty rule)
```

---

## Instance Methods

### Sequential Composition

```java
// Match this, then capture next
<R> Parser<R> then(Parser<R> next)
<R> Parser<R> then(Parser<R>.OrEmpty next)

// Return constant value after matching this
<R> Parser<R> thenReturn(R result)
```

### Lookahead (Positive)

```java
// Match this, require suffix but don't capture it
Parser<T> followedBy(String suffix)
Parser<T> followedBy(Parser<?> suffix)
<X> Parser<T> followedBy(Parser<X>.OrEmpty suffix)
Parser<T> followedByOrEof(Parser<?> suffix)

// Optional suffix (match it if present, ignore if not)
Parser<T> optionallyFollowedBy(String suffix)
Parser<T> optionallyFollowedBy(String suffix, Function<? super T, ? extends T> op)
<S> Parser<T> optionallyFollowedBy(Parser<S> suffix,
    BiFunction<? super T, ? super S, ? extends T> op)
```

### Lookahead (Negative)

```java
// Succeed only if NOT followed by suffix
Parser<T> notFollowedBy(String suffix)
Parser<T> notFollowedBy(Parser<?> suffix, String name)
Parser<T> notImmediatelyFollowedBy(CharPredicate predicate, String name)
```

### Alternation

```java
// Try this parser, else try alternative
Parser<T> or(Parser<? extends T> that)
Parser<T>.OrEmpty or(Parser<? extends T>.OrEmpty that)
```

### Optional

```java
// Zero or one occurrence
Parser<Optional<T>>.OrEmpty optional()

// Default value if not matched
Parser<T>.OrEmpty orElse(T defaultValue)
```

### Repetition

```java
// One or more
Parser<List<T>> atLeastOnce()
Parser<T> atLeastOnce(BinaryOperator<T> reducer)
<R> Parser<R> atLeastOnce(Collector<? super T, ?, ? extends R> collector)

// One or more with delimiter
Parser<List<T>> atLeastOnceDelimitedBy(String delimiter)
Parser<T> atLeastOnceDelimitedBy(String delimiter, BinaryOperator<T> reducer)
<R> Parser<R> atLeastOnceDelimitedBy(String delimiter,
    Collector<? super T, ?, ? extends R> collector)
<R> Parser<R> atLeastOnceDelimitedBy(Parser<?> delimiter,
    Collector<? super T, ?, ? extends R> collector)

// Zero or more
Parser<List<T>>.OrEmpty zeroOrMore()
<R> Parser<R>.OrEmpty zeroOrMore(Collector<? super T, ?, ? extends R> collector)

// Zero or more with delimiter
Parser<List<T>>.OrEmpty zeroOrMoreDelimitedBy(String delimiter)
<R> Parser<R>.OrEmpty zeroOrMoreDelimitedBy(String delimiter,
    Collector<? super T, ?, ? extends R> collector)
<R> Parser<R>.OrEmpty zeroOrMoreDelimitedBy(Parser<?> delimiter,
    Collector<? super T, ?, ? extends R> collector)
```

### Prefix/Postfix Operators

```java
// Apply prefix operators zero or more times
Parser<T> withPrefixes(Parser<? extends UnaryOperator<T>> operator)

// Apply postfix operators zero or more times
Parser<T> withPostfixes(Parser<? extends UnaryOperator<T>> operator)
<S> Parser<T> withPostfixes(Parser<S> operator,
    BiFunction<? super T, ? super S, ? extends T> postfixFunction)
Parser<T> withPostfixes(String operator, UnaryOperator<T> postfixFunction)
```

### Enclosure

```java
// Match between string delimiters
Parser<T> between(String prefix, String suffix)

// Match between parser delimiters
Parser<T> between(Parser<?> prefix, Parser<?> suffix)
Parser<T> between(Parser<?>.OrEmpty prefix, Parser<?>.OrEmpty suffix)

// Between delimiters without whitespace skipping
Parser<T> immediatelyBetween(String prefix, String suffix)
```

### Transformation

```java
// Transform the parsed result
<R> Parser<R> map(Function<? super T, ? extends R> f)

// Chain to another parser based on result
<R> Parser<R> flatMap(Function<? super T, Parser<R>> f)

// Apply predicate filter
Parser<T> suchThat(Predicate<? super T> condition, String name)

// Return the matched source text instead of parsed value
Parser<String> source()
```

### Parsing Operations

```java
// Parse complete input
T parse(String input)
T parse(String input, int fromIndex)

// Parse while skipping characters/patterns
T parseSkipping(Parser<?> skip, String input)
T parseSkipping(CharPredicate charsToSkip, String input)

// Lazy stream parsing
Stream<T> parseToStream(String input)
Stream<T> parseToStream(String input, int fromIndex)
Stream<T> parseToStream(Reader input)

// Test if parser matches
boolean matches(String input)

// Test if parser matches a prefix of the input
boolean isPrefixOf(String input)

// Iteratively match until failure
Stream<T> probe(String input)
Stream<T> probe(String input, int fromIndex)
Stream<T> probe(Reader input)
```

### Whitespace/Skipping Configuration

```java
// Configure skipping for subsequent parsing
Parser<T>.Lexical skipping(Parser<?> skip)
Parser<T>.Lexical skipping(CharPredicate charsToSkip)
```

---

## Class: `Parser.OrEmpty`

Methods available on optional parser results:

```java
public final class OrEmpty {
    // Enclosure
    Parser<T> between(String prefix, String suffix)
    Parser<T> between(Parser<?> prefix, Parser<?> suffix)
    Parser<T>.OrEmpty between(Parser<?>.OrEmpty prefix, Parser<?>.OrEmpty suffix)
    Parser<T> immediatelyBetween(String prefix, String suffix)

    // Delimited repetition
    <R> Parser<R>.OrEmpty delimitedBy(String delimiter, Collector<? super T, ?, R> collector)
    Parser<List<T>>.OrEmpty delimitedBy(String delimiter)

    // Sequential composition
    <S> Parser<S>.OrEmpty then(Parser<S>.OrEmpty suffix)

    // Lookahead
    Parser<T> followedBy(String suffix)
    <S> Parser<T>.OrEmpty followedBy(Parser<S>.OrEmpty suffix)
    Parser<T>.OrEmpty optionallyFollowedBy(String suffix)

    // Promote to required
    Parser<T> notEmpty()

    // Parsing operations
    T parse(String input)
    T parseSkipping(Parser<?> skip, String input)
    T parseSkipping(CharPredicate charsToSkip, String input)
    boolean matches(String input)
}
```

---

## Class: `Parser.Lexical`

Parsing with whitespace/token skipping:

```java
public final class Lexical {
    T parse(String input)
    T parse(String input, int fromIndex)
    boolean matches(String input)
    Stream<T> parseToStream(String input)
    Stream<T> parseToStream(String input, int fromIndex)
    Stream<T> parseToStream(Reader input)
    Stream<T> probe(String input)
    Stream<T> probe(String input, int fromIndex)
    Stream<T> probe(Reader input)
}
```

---

## Class: `Parser.Rule<T>`

Forward-declared grammar rule for recursive grammars:

```java
public static final class Rule<T> extends Parser<T> {
    <S extends T> Parser<S> definedAs(Parser<S> parser)
}
```

---

## Class: `Parser.ParseException`

```java
public static class ParseException extends IllegalArgumentException {
    int getSourceIndex()
}
```

---

## Class: `OperatorTable<T>`

Builds expression parsers with operator precedence.

```java
new OperatorTable<T>()
    .prefix(String op, UnaryOperator<T> operator, int precedence)
    .prefix(Parser<? extends UnaryOperator<T>> operator, int precedence)
    .postfix(String op, UnaryOperator<T> operator, int precedence)
    .postfix(Parser<? extends UnaryOperator<T>> operator, int precedence)
    .postfix(Parser<S> operator, BiFunction<? super T, ? super S, ? extends T> fn, int precedence)
    .leftAssociative(String op, BinaryOperator<T> operator, int precedence)
    .leftAssociative(Parser<? extends BinaryOperator<T>> operator, int precedence)
    .rightAssociative(String op, BinaryOperator<T> operator, int precedence)
    .rightAssociative(Parser<? extends BinaryOperator<T>> operator, int precedence)
    .nonAssociative(String op, BinaryOperator<T> operator, int precedence)
    .nonAssociative(Parser<? extends BinaryOperator<T>> operator, int precedence)
    .build(Parser<? extends T> operand)
```

---

## Class: `CharacterSet`

Regex-style character set specifications.

```java
// Parse regex-style character set to CharPredicate
CharacterSet CharacterSet.charsIn(String characterSet)

// Test character membership
boolean contains(char ch)

// Negate the set
CharacterSet not()

// Precompute ASCII lookup table for performance
CharacterSet precomputeForAscii()
```

Use with `consecutive()` or `one()`:
```java
var hexDigits = Parser.consecutive(CharacterSet.charsIn("[0-9A-Fa-f]"));
var letter = Parser.one(CharacterSet.charsIn("[a-zA-Z]"));
```

---

## Regex → Parser Equivalents

| Regex | Parser | Notes |
|-------|--------|-------|
| `a` | `one('a')` | Single character |
| `(foo)+` | `string("foo").atLeastOnce()` | One or more |
| `[a-zA-Z0-9_]+` | `word()` | Word characters |
| `[0-9]{5}` | `digits().suchThat(s -> s.length() == 5, "zip")` | Constrained digits |
| `(foo\|bar)` | `anyOf(string("foo"), string("bar"))` | Alternatives |
| `'[^']*'` | `quotedBy('\'', '\'')` | Quoted string |
| `\d+(\.\d+)?` | `digits().optionallyFollowedBy(string(".").then(digits()))` | Optional suffix |
| `[\w+(,\w+)*]?` | `word().zeroOrMoreDelimitedBy(",").between("[", "]")` | Delimited list |
| `if\b` | `word("if")` | Whole word |
| `\d+(?!\.)` | `digits().notFollowedBy(".")` | Negative lookahead |
| `foo?` | `string("foo").optional()` | Zero or one |
| `\s+` | `consecutive(Character::isWhitespace)` | Whitespace |
| `[ \t\r\n]*` | `zeroOrMore(Character::isWhitespace)` | Optional whitespace |

---

## Common Patterns

### Whitespace-tolerant parsing

```java
var result = parser.parseSkipping(Character::isWhitespace, input);
```

### Recursive grammar (e.g., nested expressions)

```java
Parser<Expr> expr = Parser.define(self ->
    new OperatorTable<Expr>()
        .leftAssociative("+", Expr.Add::new, 10)
        .build(atom.or(self.between("(", ")")))
);
```

### Block comments (nestable)

```java
Parser<String> content = Parser.anyOf(
    consecutive(isNot('*')),
    string("*").notFollowedBy("/")
);
Parser<String> blockComment = Parser.define(
    nested -> content.or(nested)
        .zeroOrMore(joining())
        .between("/*", "*/")
);
```

### Key-value pairs

```java
Parser<Map<String, String>> kvPairs = Parser.zeroOrMoreDelimited(
    Parser.word().followedBy(":"),
    Parser.word(),
    ",",
    Collectors::toUnmodifiableMap
).between("{", "}");
```

### Quoted strings with escape handling

```java
Parser<String> escaped = Parser.chars(1).map(c -> switch (c) {
    case "t" -> "\t";
    case "n" -> "\n";
    case "r" -> "\r";
    default -> c;
});
Parser<String> quoted = Parser.quotedByWithEscapes('"', '"', escaped);
```

### Enum matching

```java
enum Color { RED, GREEN, BLUE }
Parser<Color> color = Parser.anyOf(Color.RED, Color.GREEN, Color.BLUE);
```

---

## Key Design Properties

- **Guaranteed consumption**: All parsers must consume at least one character, preventing infinite loops
- **No left recursion**: Grammar structure prevents common parser combinator pitfalls
- **Flexible composition**: Sequential, alternation, and repetition combinators
- **Stream integration**: Lazy parsing via `parseToStream()` and `probe()`
- **~1000 LOC**: Tiny footprint, roughly one-fifth the size of jparsec
