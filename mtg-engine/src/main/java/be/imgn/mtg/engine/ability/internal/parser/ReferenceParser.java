package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.parse.Parser.anyOf;
import static be.imgn.mtg.parse.Parser.string;
import static be.imgn.mtg.parse.Parser.word;

import java.util.List;

import be.imgn.mtg.engine.ability.internal.parser.reference.PronounType;
import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.selector.CompositeSelector;
import be.imgn.mtg.engine.selector.ObjectSelector;
import be.imgn.mtg.engine.selector.PlayerCriterion;
import be.imgn.mtg.engine.selector.PlayerSelector;
import be.imgn.mtg.engine.selector.Quantifier;
import be.imgn.mtg.engine.selector.TypeMatcher;
import be.imgn.mtg.parse.Parser;

/// Parser for reference types in oracle text.
public final class ReferenceParser {

    private ReferenceParser() {}

    /// Parses pronoun references.
    public static final Parser<Subject> PRONOUN = anyOf(
            word("it").thenReturn(new Subject.Pronoun(PronounType.IT)),
            word("them").thenReturn(new Subject.Pronoun(PronounType.THEM)));

    /// Parses "that creature", "that permanent", etc.
    public static final Parser<Subject> THAT_OBJECT =
            word("that").then(ObjectTypeParser.TYPE_MATCHER.optional()).map(optType -> new Subject.ThatObject(optType));

    /// Parses "any target" as a special case (any legal target for damage).
    /// This must be parsed before general selectors to prevent "target" being consumed as a qualifier.
    public static final Parser<Subject> ANY_TARGET = string("any target")
            .thenReturn(new Subject.Select(new CompositeSelector(
                    new Quantifier.One(),
                    new ObjectSelector(new Quantifier.One(), List.of(), new TypeMatcher.Target(), List.of(), null),
                    new PlayerSelector(new Quantifier.One(), PlayerCriterion.ANY))));

    /// Parses a selector-based subject.
    public static final Parser<Subject> SELECT = ObjectSelectorParser.OBJECT_SELECTOR.map(Subject.Select::new);

    /// Parses any subject reference.
    public static final Parser<Subject> SUBJECT = anyOf(PRONOUN, THAT_OBJECT, ANY_TARGET, SELECT);
}
