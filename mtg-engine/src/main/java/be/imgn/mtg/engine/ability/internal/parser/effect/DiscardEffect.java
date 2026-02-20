package be.imgn.mtg.engine.ability.internal.parser.effect;

import java.util.Optional;

import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.selector.PlayerReference;

/// An effect that causes a player to discard cards.
///
/// @param player who discards (empty for implicit "you")
/// @param amount how many cards to discard
public record DiscardEffect(Optional<PlayerReference> player, Amount amount) implements Effect {}
