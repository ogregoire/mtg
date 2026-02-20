package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.parse.Parser.anyOf;
import static be.imgn.mtg.parse.Parser.or;
import static be.imgn.mtg.parse.Parser.word;

import java.util.Arrays;

import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.Supertype;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.selector.Qualifier;
import be.imgn.mtg.engine.selector.StatusType;
import be.imgn.mtg.engine.selector.Trait;
import be.imgn.mtg.parse.Parser;

/// Parser for qualifiers in oracle text.
public final class QualifierParser {

    private QualifierParser() {}

    /// Parses "target" qualifier.
    public static final Parser<Qualifier> TARGET = OracleParser.word("target").thenReturn(new Qualifier.Target());

    /// Parses negation qualifiers like "nonland", "nonblack", "nonlegendary", etc.
    public static final Parser<Qualifier> NEGATION = anyOf(
            OracleParser.word("nonland").thenReturn(new Qualifier.Not(new Trait.CardType(Type.LAND))),
            OracleParser.word("noncreature").thenReturn(new Qualifier.Not(new Trait.CardType(Type.CREATURE))),
            OracleParser.word("nonartifact").thenReturn(new Qualifier.Not(new Trait.CardType(Type.ARTIFACT))),
            OracleParser.word("nonenchantment").thenReturn(new Qualifier.Not(new Trait.CardType(Type.ENCHANTMENT))),
            OracleParser.word("nonwhite").thenReturn(new Qualifier.Not(new Trait.ObjectColor(Color.WHITE))),
            OracleParser.word("nonblue").thenReturn(new Qualifier.Not(new Trait.ObjectColor(Color.BLUE))),
            OracleParser.word("nonblack").thenReturn(new Qualifier.Not(new Trait.ObjectColor(Color.BLACK))),
            OracleParser.word("nonred").thenReturn(new Qualifier.Not(new Trait.ObjectColor(Color.RED))),
            OracleParser.word("nongreen").thenReturn(new Qualifier.Not(new Trait.ObjectColor(Color.GREEN))),
            OracleParser.word("nonlegendary")
                    .thenReturn(new Qualifier.Not(new Trait.ObjectSupertype(Supertype.LEGENDARY))),
            OracleParser.word("nonbasic").thenReturn(new Qualifier.Not(new Trait.ObjectSupertype(Supertype.BASIC))),
            OracleParser.word("nonsnow").thenReturn(new Qualifier.Not(new Trait.ObjectSupertype(Supertype.SNOW))),
            OracleParser.word("nontoken").thenReturn(new Qualifier.Not(new Trait.TokenSource())));

    /// Parses status qualifiers.
    public static final Parser<Qualifier> STATUS = anyOf(
            word("tapped").thenReturn(new Qualifier.Status(StatusType.TAPPED)),
            word("untapped").thenReturn(new Qualifier.Status(StatusType.UNTAPPED)),
            word("attacking").thenReturn(new Qualifier.Status(StatusType.ATTACKING)),
            word("blocking").thenReturn(new Qualifier.Status(StatusType.BLOCKING)),
            word("equipped").thenReturn(new Qualifier.Status(StatusType.EQUIPPED)),
            word("enchanted").thenReturn(new Qualifier.Status(StatusType.ENCHANTED)));

    /// Parses positive supertype qualifiers like "legendary", "basic", etc.
    public static final Parser<Qualifier> SUPERTYPE = Arrays.stream(Supertype.values())
            .map(st -> OracleParser.word(st).thenReturn((Qualifier) new Qualifier.Has(new Trait.ObjectSupertype(st))))
            .collect(or());

    /// Parses any qualifier.
    public static final Parser<Qualifier> QUALIFIER = anyOf(TARGET, NEGATION, STATUS, SUPERTYPE);
}
