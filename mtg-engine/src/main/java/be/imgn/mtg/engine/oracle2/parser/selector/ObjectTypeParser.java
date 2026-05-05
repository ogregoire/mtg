package be.imgn.mtg.engine.oracle2.parser.selector;

import static be.imgn.mtg.engine.oracle2.domain.selector.ObjectPropertySelector.Anything.ANYTHING;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static be.imgn.mtg.engine.oracle2.parser.selector.PropertyParser.PROPERTY;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.or;
import static com.google.common.labs.parse.Parser.sequence;

import java.util.List;
import java.util.stream.Stream;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.ObjectType;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectPropertySelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectTypeSelector;

/// Parser for [ObjectTypeSelector] — the 7 game-object classes that
/// wrap an [ObjectPropertySelector] predicate.
///
/// Grammar shape: `[properties] [noun-keyword]? [properties]?`
///
/// - With an explicit noun keyword ("permanent", "token", "spell",
///   "ability", "copy", "card", "emblem"), the matching empty arm is
///   produced and properties are attached via
///   [ObjectTypeSelector#withWhere].
/// - Without a noun keyword, default to [ObjectTypeSelector.Permanent]
///   — common in oracle text where "creature" alone means "creature
///   permanent on the battlefield".
/// - Properties on either side of the noun are merged into a single
///   [ObjectPropertySelector.AllOf] (or kept as a single property if
///   only one slot is used).
public final class ObjectTypeParser {
    private ObjectTypeParser() {}

    /// The 7 bare noun keywords — one arm per [ObjectType] value,
    /// mapping the matched phrase to the empty (where = ANYTHING)
    /// selector arm. Properties are attached downstream via
    /// [#addProperty].
    private static final Parser<ObjectTypeSelector> BARE_TYPE = Stream.of(ObjectType.values())
            .map(t -> phrase(t.text()).thenReturn(empty(t)))
            .collect(or());

    /// `[properties]? noun [properties]?` — both property slots are
    /// optional and AND-merged into the noun's `where` via
    /// [#addProperty]. The leading-optional shape uses
    /// [Parser#sequence(Parser.OrEmpty, Parser, BiFunction)] (mug 10.0)
    /// with [ObjectPropertySelector.Anything#ANYTHING] as the
    /// no-property default — `addProperty` already treats ANYTHING as
    /// the AND identity.
    private static final Parser<ObjectTypeSelector> NOUN_WITH_PROPERTIES = sequence(
                    PROPERTY.orElse(ANYTHING), BARE_TYPE, (pre, noun) -> addProperty(noun, pre))
            .optionallyFollowedBy(PROPERTY, ObjectTypeParser::addProperty);

    /// Top-level [ObjectTypeSelector]. Noun-led form first (consumes
    /// the most), then property-only fallback (no noun → implicit
    /// [ObjectTypeSelector.Permanent]).
    public static final Parser<ObjectTypeSelector> OBJECT_TYPE =
            anyOf(NOUN_WITH_PROPERTIES, PROPERTY.map(ObjectTypeSelector.Permanent::new));

    /// Returns the empty (where = ANYTHING) selector arm for `t`.
    private static ObjectTypeSelector empty(ObjectType t) {
        return switch (t) {
            case PERMANENT -> new ObjectTypeSelector.Permanent();
            case TOKEN -> new ObjectTypeSelector.Token();
            case SPELL -> new ObjectTypeSelector.Spell();
            case ABILITY -> new ObjectTypeSelector.Ability();
            case COPY -> new ObjectTypeSelector.Copy();
            case CARD -> new ObjectTypeSelector.Card();
            case EMBLEM -> new ObjectTypeSelector.Emblem();
        };
    }

    /// AND-attach `extra` to `typed`'s `where` slot. ANYTHING is the
    /// identity for AND, so adding to or onto an empty slot collapses
    /// to the non-empty side.
    private static ObjectTypeSelector addProperty(ObjectTypeSelector typed, ObjectPropertySelector extra) {
        var existing = typed.where();
        if (existing == ANYTHING) return typed.withWhere(extra);
        if (extra == ANYTHING) return typed;
        return typed.withWhere(new ObjectPropertySelector.AllOf(List.of(existing, extra)));
    }
}
