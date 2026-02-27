package be.imgn.mtg.engine.ability.internal.parser;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.selector.ControllerClause;
import be.imgn.mtg.engine.selector.PlayerReference;

/// Parser for controller clauses in oracle text.
public final class ControllerParser {

    private ControllerParser() {}

    /// Parses player references.
    public static final Parser<PlayerReference> PLAYER_REFERENCE = anyOf(
            word("you").thenReturn(PlayerReference.YOU),
            sequence(word("that"), word("player"), (a, b) -> PlayerReference.THAT_PLAYER),
            sequence(word("target"), word("player"), (a, b) -> PlayerReference.TARGET_PLAYER),
            sequence(word("each"), word("player"), (a, b) -> PlayerReference.EACH_PLAYER),
            sequence(word("each"), word("opponent"), (a, b) -> PlayerReference.EACH_OPPONENT),
            sequence(word("an"), word("opponent"), (a, b) -> PlayerReference.OPPONENT),
            word("opponent").thenReturn(PlayerReference.OPPONENT));

    /// Parses "control" or "controls".
    private static final Parser<String> CONTROL_VERB = anyOf(word("controls"), word("control"));

    /// Parses "X controls" or "you control" clause.
    public static final Parser<ControllerClause> CONTROLS =
            PLAYER_REFERENCE.followedBy(CONTROL_VERB).map(ControllerClause::new);

    /// Parses optional controller clause.
    public static final Parser<ControllerClause>.OrEmpty CONTROLLER_CLAUSE = CONTROLS.orElse(null);
}
