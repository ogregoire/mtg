package be.imgn.mtg.engine.ability.internal.parser.effect;

import java.util.Optional;

import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.selector.PlayerReference;

/// An effect that causes a player to gain life.
///
/// @param player who gains life (empty for implicit "you")
/// @param amount how much life to gain
public record GainLifeEffect(Optional<PlayerReference> player, Amount amount) implements Effect {}
