package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.effect.Effect;
import be.imgn.mtg.engine.oracle2.domain.effect.LookAtEffect;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectPropertySelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.domain.selector.ZoneSelector;
import be.imgn.mtg.engine.oracle2.parser.selector.ZoneParser;

/// Parser for [LookAtEffect] ({@mtg.rule 701.10}). Subject-led:
/// "(who) look(s) at (zone)." The looked-at zone is a bare
/// possessive-zone reference ("target player's hand") that doesn't
/// reach [ZoneParser#ZONE_SELECTOR] because it has no leading
/// object-type noun, so the zone parser is inlined here.
public final class LookAtEffectParser {
    private LookAtEffectParser() {}

    /// "(owner) hand" — bare hand reference, wrapped in
    /// [ZoneSelector.Hand] with [ObjectPropertySelector.Anything#ANYTHING]
    /// as the implicit contents predicate (any card).
    private static final Parser<ZoneSelector.Hand> HAND = ZoneParser.POSSESSIVE_OWNER
            .followedBy(phrase("hand"))
            .map(owner -> new ZoneSelector.Hand(
                    owner, new ObjectTypeSelector.Card(ObjectPropertySelector.Anything.ANYTHING)));

    /// "look(s) at (zone)" — subject-led wrapper. Only the hand
    /// variant lands today; library / graveyard / battlefield zones
    /// pick up new arms as cards demand them.
    public static final Parser<Function<Selector, Effect>> LOOKS_AT_FN =
            EffectParser.subjectVerb(phrase("look(s) at").then(anyOf(HAND)), LookAtEffect::new);
}
