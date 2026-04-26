package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.SUBTYPE;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.SUPERTYPE;
import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.string;

import java.util.ArrayList;
import java.util.List;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle.domain.Selector;
import be.imgn.mtg.engine.oracle.domain.TypeMatcher;

/// Type-axis qualifier parsers and the post-collection merge step
/// that folds adjacent type qualifiers into one. A single merge
/// fold suffices for every type axis (card type, subtype, supertype)
/// because they all share the unified [TypeMatcher] tree.
final class TypeQualifierParsers {
    private TypeQualifierParsers() {}

    /// "Legendary" / "Snow" / "Basic" / "World" — positive supertype.
    static final Parser<Selector.Qualifier> SUPERTYPE_Q =
            SUPERTYPE.<Selector.Qualifier>map(s -> new Selector.Qualifier.Types(new TypeMatcher.IsSupertype(s)));

    /// "Nonlegendary" / "Nonbasic" / "Nonsnow" — negated supertype
    /// qualifier.
    static final Parser<Selector.Qualifier> NEGATED_SUPERTYPE_Q = anyOf(
            phrase("Nonlegendary")
                    .<Selector.Qualifier>thenReturn(new Selector.Qualifier.Types(TypeMatcher.NONLEGENDARY)),
            phrase("Nonbasic").<Selector.Qualifier>thenReturn(new Selector.Qualifier.Types(TypeMatcher.NONBASIC)),
            phrase("Nonsnow").<Selector.Qualifier>thenReturn(new Selector.Qualifier.Types(TypeMatcher.NONSNOW)));

    /// "Noncreature" / "Nonartifact" / … — negated card-type qualifier.
    static final Parser<Selector.Qualifier> NEGATED_CARD_TYPE_Q = anyOf(
            phrase("Noncreature").<Selector.Qualifier>thenReturn(new Selector.Qualifier.Types(TypeMatcher.NONCREATURE)),
            phrase("Nonartifact").<Selector.Qualifier>thenReturn(new Selector.Qualifier.Types(TypeMatcher.NONARTIFACT)),
            phrase("Nonenchantment")
                    .<Selector.Qualifier>thenReturn(new Selector.Qualifier.Types(TypeMatcher.NONENCHANTMENT)),
            phrase("Nonland").<Selector.Qualifier>thenReturn(new Selector.Qualifier.Types(TypeMatcher.NONLAND)),
            phrase("Nonplaneswalker")
                    .<Selector.Qualifier>thenReturn(new Selector.Qualifier.Types(TypeMatcher.NONPLANESWALKER)));

    /// "non-Subtype" / "Non-Subtype" — negated subtype qualifier
    /// (e.g., "non-Human creature", "non-Vampire creature").
    static final Parser<Selector.Qualifier> NEGATED_SUBTYPE_Q = anyOf(string("non-"), string("Non-"))
            .then(SUBTYPE)
            .map(st -> (Selector.Qualifier)
                    new Selector.Qualifier.Types(new TypeMatcher.Not(new TypeMatcher.IsSubtype(st))));

    /// Folds every [Selector.Qualifier.Types] in `qs` into a single
    /// `Types(All[...])` qualifier, preserving the position of the
    /// first occurrence and dropping subsequent ones. Single-occurrence
    /// lists are returned unchanged. Nested `All` matchers are
    /// flattened so a multi-pass fold (e.g. QUALIFIER_LIST first
    /// folding three negations into `All[...]`, then `flatFromAlt`
    /// adding the trailing `IsCardType` from the type group) ends up
    /// with a single flat `All` rather than `All[All[...], ...]`.
    static List<Selector.Qualifier> mergeTypeQualifiers(List<Selector.Qualifier> qs) {
        var matchers = new ArrayList<TypeMatcher>();
        for (var q : qs) {
            if (q instanceof Selector.Qualifier.Types t) {
                if (t.matcher() instanceof TypeMatcher.All inner) {
                    matchers.addAll(inner.matchers());
                } else {
                    matchers.add(t.matcher());
                }
            }
        }
        if (matchers.size() <= 1) return qs;
        var merged = new Selector.Qualifier.Types(new TypeMatcher.All(List.copyOf(matchers)));
        var out = new ArrayList<Selector.Qualifier>(qs.size());
        var inserted = false;
        for (var q : qs) {
            if (q instanceof Selector.Qualifier.Types) {
                if (!inserted) {
                    out.add(merged);
                    inserted = true;
                }
            } else {
                out.add(q);
            }
        }
        return out;
    }
}
