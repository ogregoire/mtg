package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.parse.Parser.anyOf;
import static be.imgn.mtg.parse.Parser.word;

import be.imgn.mtg.engine.ability.internal.parser.selector.NegationType;
import be.imgn.mtg.engine.ability.internal.parser.selector.Qualifier;
import be.imgn.mtg.engine.ability.internal.parser.selector.StatusType;
import be.imgn.mtg.engine.characteristics.Supertype;
import be.imgn.mtg.parse.Parser;

/// Parser for qualifiers in oracle text.
public final class QualifierParser {

    private QualifierParser() {}

    /// Parses "target" qualifier.
    public static final Parser<Qualifier> TARGET = word("target").thenReturn(new Qualifier.Target());

    /// Parses negation types like "nonland", "nonblack", etc.
    public static final Parser<Qualifier> NEGATION = anyOf(
            word("nonland").thenReturn(new Qualifier.Negation(NegationType.LAND)),
            word("nonwhite").thenReturn(new Qualifier.Negation(NegationType.WHITE)),
            word("nonblue").thenReturn(new Qualifier.Negation(NegationType.BLUE)),
            word("nonblack").thenReturn(new Qualifier.Negation(NegationType.BLACK)),
            word("nonred").thenReturn(new Qualifier.Negation(NegationType.RED)),
            word("nongreen").thenReturn(new Qualifier.Negation(NegationType.GREEN)),
            word("noncreature").thenReturn(new Qualifier.Negation(NegationType.CREATURE)),
            word("nonartifact").thenReturn(new Qualifier.Negation(NegationType.ARTIFACT)),
            word("nonenchantment").thenReturn(new Qualifier.Negation(NegationType.ENCHANTMENT)),
            word("nontoken").thenReturn(new Qualifier.Negation(NegationType.TOKEN)));

    /// Parses status qualifiers.
    public static final Parser<Qualifier> STATUS = anyOf(
            word("tapped").thenReturn(new Qualifier.Status(StatusType.TAPPED)),
            word("untapped").thenReturn(new Qualifier.Status(StatusType.UNTAPPED)),
            word("attacking").thenReturn(new Qualifier.Status(StatusType.ATTACKING)),
            word("blocking").thenReturn(new Qualifier.Status(StatusType.BLOCKING)),
            word("equipped").thenReturn(new Qualifier.Status(StatusType.EQUIPPED)),
            word("enchanted").thenReturn(new Qualifier.Status(StatusType.ENCHANTED)));

    /// Parses supertype qualifiers.
    public static final Parser<Qualifier> SUPERTYPE = anyOf(
            word("basic").thenReturn(new Qualifier.SupertypeQualifier(Supertype.BASIC)),
            word("legendary").thenReturn(new Qualifier.SupertypeQualifier(Supertype.LEGENDARY)),
            word("snow").thenReturn(new Qualifier.SupertypeQualifier(Supertype.SNOW)),
            word("world").thenReturn(new Qualifier.SupertypeQualifier(Supertype.WORLD)));

    /// Parses any qualifier.
    public static final Parser<Qualifier> QUALIFIER = anyOf(TARGET, NEGATION, STATUS, SUPERTYPE);
}
