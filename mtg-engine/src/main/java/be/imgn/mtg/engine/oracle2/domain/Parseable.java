package be.imgn.mtg.engine.oracle.domain2;

/// Marker interface for enums whose values are recognized by the oracle
/// parser through a fixed [#text()] phrase form. The phrase form follows
/// the project's `phrase()` DSL — `Word(s)` for regular plural inflection,
/// `[Foo|Bar]` for required alternation, etc.
public interface Parseable {

    /// Phrase form recognized by the oracle parser.
    String text();
}
