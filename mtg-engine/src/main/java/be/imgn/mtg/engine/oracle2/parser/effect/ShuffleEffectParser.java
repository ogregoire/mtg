package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.PlayerRelation;
import be.imgn.mtg.engine.oracle2.domain.effect.Effect;
import be.imgn.mtg.engine.oracle2.domain.effect.ShuffleEffect;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerRelationSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;

/// Parser for [ShuffleEffect] ({@mtg.rule 701.20}). Subject-led:
/// "X shuffles Y's library." or bare "Shuffle Y's library." (where
/// [EffectParser] supplies the implicit "you" subject). Only the
/// "your library" form lands today — "target opponent's library" /
/// "their library" pick up new arms when the cards that need them
/// appear.
public final class ShuffleEffectParser {
    private ShuffleEffectParser() {}

    /// "your library" — the owner is the resolving controller. Bare
    /// possessive-zone phrase that doesn't reach
    /// [be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser#SELECTOR]
    /// (no leading object-type noun), so it's localized here.
    private static final Parser<PlayerSelector> YOUR_LIBRARY =
            phrase("your library").thenReturn(new PlayerRelationSelector(PlayerRelation.YOU));

    /// "shuffle(s) (owner's) library" — subject-led wrapper.
    public static final Parser<Function<Selector, Effect>> SHUFFLES_FN =
            EffectParser.subjectVerb(phrase("shuffle(s)").then(YOUR_LIBRARY), ShuffleEffect::new);
}
