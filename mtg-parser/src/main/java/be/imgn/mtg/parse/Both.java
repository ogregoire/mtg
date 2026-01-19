package be.imgn.mtg.parse;

/// A simple tuple of two values. Used internally by the parser for combining results.
///
/// @param first the first value
/// @param second the second value
/// @param <A> the type of the first value
/// @param <B> the type of the second value
public record Both<A, B>(A first, B second) {

    /// Creates a new Both with the given values.
    ///
    /// @param first the first value
    /// @param second the second value
    /// @param <A> the type of the first value
    /// @param <B> the type of the second value
    /// @return a new Both instance
    public static <A, B> Both<A, B> of(A first, B second) {
        return new Both<>(first, second);
    }
}
