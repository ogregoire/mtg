package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.SUBTYPE;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.SUPERTYPE;
import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.string;

import java.util.ArrayList;
import java.util.List;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle.domain.CardTypeMatcher;
import be.imgn.mtg.engine.oracle.domain.Selector;
import be.imgn.mtg.engine.oracle.domain.SubtypeMatcher;
import be.imgn.mtg.engine.oracle.domain.SupertypeMatcher;

/// Type-qualifier parsers and the post-collection merge steps that fold
/// adjacent same-axis qualifiers into one. Mirrors
/// [ColorQualifierParsers]; one file holds the three sibling axes
/// (supertype, card type, subtype) so the merge-fold helper is shared
/// implicitly through repeated structure.
final class TypeQualifierParsers {
    private TypeQualifierParsers() {}

    /// "Legendary" / "Snow" / "Basic" / "World" — positive supertype.
    /// Returns a [Selector.Qualifier.Supertypes] with an `Is` atom.
    static final Parser<Selector.Qualifier> SUPERTYPE_Q =
            SUPERTYPE.map(s -> (Selector.Qualifier) new Selector.Qualifier.Supertypes(new SupertypeMatcher.Is(s)));

    /// "Nonlegendary" / "Nonbasic" / "Nonsnow" — negated supertype
    /// qualifier.
    static final Parser<Selector.Qualifier> NEGATED_SUPERTYPE_Q = anyOf(
            phrase("Nonlegendary")
                    .<Selector.Qualifier>thenReturn(new Selector.Qualifier.Supertypes(SupertypeMatcher.NONLEGENDARY)),
            phrase("Nonbasic")
                    .<Selector.Qualifier>thenReturn(new Selector.Qualifier.Supertypes(SupertypeMatcher.NONBASIC)),
            phrase("Nonsnow")
                    .<Selector.Qualifier>thenReturn(new Selector.Qualifier.Supertypes(SupertypeMatcher.NONSNOW)));

    /// "Noncreature" / "Nonartifact" / … — negated card-type qualifier.
    static final Parser<Selector.Qualifier> NEGATED_CARD_TYPE_Q = anyOf(
            phrase("Noncreature")
                    .<Selector.Qualifier>thenReturn(new Selector.Qualifier.CardTypes(CardTypeMatcher.NONCREATURE)),
            phrase("Nonartifact")
                    .<Selector.Qualifier>thenReturn(new Selector.Qualifier.CardTypes(CardTypeMatcher.NONARTIFACT)),
            phrase("Nonenchantment")
                    .<Selector.Qualifier>thenReturn(new Selector.Qualifier.CardTypes(CardTypeMatcher.NONENCHANTMENT)),
            phrase("Nonland").<Selector.Qualifier>thenReturn(new Selector.Qualifier.CardTypes(CardTypeMatcher.NONLAND)),
            phrase("Nonplaneswalker")
                    .<Selector.Qualifier>thenReturn(new Selector.Qualifier.CardTypes(CardTypeMatcher.NONPLANESWALKER)));

    /// "non-Subtype" / "Non-Subtype" — negated subtype qualifier
    /// (e.g., "non-Human creature", "non-Vampire creature").
    static final Parser<Selector.Qualifier> NEGATED_SUBTYPE_Q = anyOf(string("non-"), string("Non-"))
            .then(SUBTYPE)
            .map(st -> (Selector.Qualifier) new Selector.Qualifier.Subtypes(new SubtypeMatcher.Not(st)));

    /// Folds every [Selector.Qualifier.Supertypes] in `qs` into a
    /// single `Supertypes(All[...])` qualifier. See
    /// [#mergeColorQualifiers] in [ColorQualifierParsers] for the
    /// shared shape.
    static List<Selector.Qualifier> mergeSupertypeQualifiers(List<Selector.Qualifier> qs) {
        var matchers = new ArrayList<SupertypeMatcher>();
        for (var q : qs) {
            if (q instanceof Selector.Qualifier.Supertypes s) matchers.add(s.matcher());
        }
        if (matchers.size() <= 1) return qs;
        var merged = new Selector.Qualifier.Supertypes(new SupertypeMatcher.All(List.copyOf(matchers)));
        return replaceFirstOfDropRest(qs, Selector.Qualifier.Supertypes.class, merged);
    }

    /// Folds every [Selector.Qualifier.CardTypes] in `qs` into a single
    /// `CardTypes(All[...])` qualifier.
    static List<Selector.Qualifier> mergeCardTypeQualifiers(List<Selector.Qualifier> qs) {
        var matchers = new ArrayList<CardTypeMatcher>();
        for (var q : qs) {
            if (q instanceof Selector.Qualifier.CardTypes c) matchers.add(c.matcher());
        }
        if (matchers.size() <= 1) return qs;
        var merged = new Selector.Qualifier.CardTypes(new CardTypeMatcher.All(List.copyOf(matchers)));
        return replaceFirstOfDropRest(qs, Selector.Qualifier.CardTypes.class, merged);
    }

    /// Folds every [Selector.Qualifier.Subtypes] in `qs` into a single
    /// `Subtypes(All[...])` qualifier (Victim of Night triple-
    /// negation case).
    static List<Selector.Qualifier> mergeSubtypeQualifiers(List<Selector.Qualifier> qs) {
        var matchers = new ArrayList<SubtypeMatcher>();
        for (var q : qs) {
            if (q instanceof Selector.Qualifier.Subtypes s) matchers.add(s.matcher());
        }
        if (matchers.size() <= 1) return qs;
        var merged = new Selector.Qualifier.Subtypes(new SubtypeMatcher.All(List.copyOf(matchers)));
        return replaceFirstOfDropRest(qs, Selector.Qualifier.Subtypes.class, merged);
    }

    /// Helper — replaces the first occurrence of any qualifier of class
    /// `cls` with `merged`, drops every subsequent occurrence, and
    /// preserves the relative order of all other qualifiers.
    private static List<Selector.Qualifier> replaceFirstOfDropRest(
            List<Selector.Qualifier> qs, Class<? extends Selector.Qualifier> cls, Selector.Qualifier merged) {
        var out = new ArrayList<Selector.Qualifier>(qs.size());
        var inserted = false;
        for (var q : qs) {
            if (cls.isInstance(q)) {
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
