package be.imgn.mtg.engine.ability.internal.parser.effect;

import java.util.Optional;

import be.imgn.mtg.engine.ability.internal.parser.reference.PlayerReference;
import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;

/// An effect that causes a player to draw cards.
///
/// @param player who draws (empty for implicit "you")
/// @param amount how many cards to draw
public record DrawEffect(Optional<PlayerReference> player, Amount amount) implements Effect {}
