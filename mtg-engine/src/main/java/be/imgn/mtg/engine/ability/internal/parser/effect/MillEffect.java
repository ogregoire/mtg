package be.imgn.mtg.engine.ability.internal.parser.effect;

import java.util.Optional;

import be.imgn.mtg.engine.ability.internal.parser.reference.PlayerReference;
import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;

/// A mill effect that puts cards from library into graveyard.
///
/// @param player who mills (empty for implicit "you")
/// @param amount how many cards to mill
public record MillEffect(Optional<PlayerReference> player, Amount amount) implements Effect {}
