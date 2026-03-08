package be.imgn.mtg.engine.effect;

import java.util.Optional;

import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.selector.PlayerReference;

/// An effect that causes a player to lose life.
///
/// @param player who loses life (empty for implicit "you")
/// @param amount how much life to lose
public record LoseLifeEffect(Optional<PlayerReference> player, Amount amount) implements Effect {}
