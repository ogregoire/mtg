package be.imgn.mtg.engine.oracle2.parser.selector;

import static be.imgn.mtg.engine.oracle2.parser.AmountMatcherParser.AMOUNT_MATCHER;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static be.imgn.mtg.engine.oracle2.parser.selector.ObjectCounterSelectorParser.COUNTER_TYPE;
import static be.imgn.mtg.engine.oracle2.parser.selector.ObjectCounterSelectorParser.OPT_TYPED_COUNTER;
import static be.imgn.mtg.engine.oracle2.parser.selector.PlayerSelectorParser.BARE_PLAYER;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.AmountMatcher;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerCounterSelector;

/// Parser for [PlayerCounterSelector]. Postfix on a player —
/// "[player] with [matcher] [type] counter(s)".
///
/// Reuses the `COUNTER_TYPE` parser from [ObjectCounterSelectorParser]
/// (package-private). Uses [PlayerSelectorParser#BARE_PLAYER] for
/// the leading player to avoid infinite recursion through
/// [Refs#PLAYER_SELECTOR].
public final class PlayerCounterSelectorParser {
    private PlayerCounterSelectorParser() {}

    /// "[player] with a/an [type] counter" — presence form
    /// (`AtLeast(1)`).
    private static final Parser<PlayerCounterSelector.HasCounters> PRESENCE = sequence(
            BARE_PLAYER.followedBy(phrase("with [a|an]")),
            COUNTER_TYPE.followedBy(phrase("counter")),
            (player, type) -> new PlayerCounterSelector.HasCounters(
                    player, type, new AmountMatcher.AtLeast(new Amount.Exact(1))));

    /// "[player] with [matcher] [type]? counter(s)" — general form.
    /// Type missing → [CounterType.Any#ANY] (handled by
    /// [ObjectCounterSelectorParser#OPT_TYPED_COUNTER]).
    private static final Parser<PlayerCounterSelector.HasCounters> GENERAL = sequence(
            BARE_PLAYER.followedBy(phrase("with")),
            AMOUNT_MATCHER,
            OPT_TYPED_COUNTER,
            (player, matcher, type) -> new PlayerCounterSelector.HasCounters(player, type, matcher));

    /// Top-level [PlayerCounterSelector]. PRESENCE first (longer
    /// specific "with a/an" prefix), then GENERAL.
    public static final Parser<PlayerCounterSelector> PLAYER_COUNTER_SELECTOR = anyOf(PRESENCE, GENERAL);
}
