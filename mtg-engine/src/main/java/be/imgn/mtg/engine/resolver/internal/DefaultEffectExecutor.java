package be.imgn.mtg.engine.resolver.internal;

import java.util.Optional;

import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.combat.DamageEvent;
import be.imgn.mtg.engine.combat.DamageTarget;
import be.imgn.mtg.engine.effect.CompoundEffect;
import be.imgn.mtg.engine.effect.DealDamageEffect;
import be.imgn.mtg.engine.effect.DestroyEffect;
import be.imgn.mtg.engine.effect.DiscardEffect;
import be.imgn.mtg.engine.effect.DrawEffect;
import be.imgn.mtg.engine.effect.Effect;
import be.imgn.mtg.engine.effect.GainLifeEffect;
import be.imgn.mtg.engine.game.Choice;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.resolver.EffectExecutor;
import be.imgn.mtg.engine.resolver.ResolutionContext;
import be.imgn.mtg.engine.selector.PlayerReference;
import be.imgn.mtg.engine.selector.Selectable;
import be.imgn.mtg.engine.zone.DiesEvent;
import be.imgn.mtg.engine.zone.DiscardEvent;
import be.imgn.mtg.engine.zone.DrawEvent;

/// Default implementation that switches on the sealed [Effect] type.
final class DefaultEffectExecutor implements EffectExecutor {

    @Override
    public void execute(Effect effect, ResolutionContext context) {
        switch (effect) {
            case DealDamageEffect e -> executeDealDamage(e, context);
            case DrawEffect e -> executeDraw(e, context);
            case DestroyEffect e -> executeDestroy(e, context);
            case GainLifeEffect e -> executeGainLife(e, context);
            case DiscardEffect e -> executeDiscard(e, context);
            case CompoundEffect e -> e.effects().forEach(sub -> execute(sub, context));
            default ->
                throw new UnsupportedOperationException(
                        "Effect not yet implemented: " + effect.getClass().getSimpleName());
        }
    }

    private void executeDealDamage(DealDamageEffect effect, ResolutionContext ctx) {
        var target = resolveSubject(effect.target(), ctx);
        var amount = resolveAmount(effect.amount());
        var damageTarget =
                switch (target) {
                    case Player p -> new DamageTarget.PlayerTarget(p);
                    case Permanent p -> {
                        if (p.types().isCreature()) yield new DamageTarget.CreatureTarget(p);
                        else if (p.types().isPlaneswalker()) yield new DamageTarget.PlaneswalkerTarget(p);
                        else yield new DamageTarget.BattleTarget(p);
                    }
                    default -> throw new IllegalStateException("Cannot deal damage to: " + target);
                };
        ctx.eventProcessor().process(new DamageEvent(ctx.spell(), damageTarget, amount, false, false));
    }

    private void executeDraw(DrawEffect effect, ResolutionContext ctx) {
        var player = resolvePlayer(effect.player(), ctx);
        var amount = resolveAmount(effect.amount());
        var library = ctx.gameState().library(player);
        for (int i = 0; i < amount; i++) {
            library.peekTop().ifPresent(card -> ctx.eventProcessor().process(new DrawEvent(card, player)));
        }
    }

    private void executeDestroy(DestroyEffect effect, ResolutionContext ctx) {
        var target = resolveSubject(effect.subject(), ctx);
        if (target instanceof Permanent permanent) {
            ctx.eventProcessor().process(new DiesEvent(permanent, new DiesEvent.DeathCause.Destroyed()));
        }
    }

    private void executeGainLife(GainLifeEffect effect, ResolutionContext ctx) {
        var player = resolvePlayer(effect.player(), ctx);
        var amount = resolveAmount(effect.amount());
        player.gainLife(amount);
    }

    private void executeDiscard(DiscardEffect effect, ResolutionContext ctx) {
        var player = resolvePlayer(effect.player(), ctx);
        var amount = resolveAmount(effect.amount());
        var hand = ctx.gameState().hand(player);
        var cards = hand.stream().toList();
        if (cards.isEmpty()) return;
        var toDiscard = Math.min(amount, cards.size());
        var chosen = player.choose(Choice.nOf(cards, toDiscard, "Choose cards to discard"));
        for (var card : chosen) {
            ctx.eventProcessor().process(new DiscardEvent(card, player));
        }
    }

    private Player resolvePlayer(Optional<PlayerReference> ref, ResolutionContext ctx) {
        if (ref.isEmpty() || ref.get() == PlayerReference.YOU) {
            return ctx.controller();
        }
        throw new UnsupportedOperationException("Player reference not yet implemented: " + ref.get());
    }

    private int resolveAmount(Amount amount) {
        return switch (amount) {
            case Amount.Exact exact -> exact.value();
            case Amount.Variable _ -> throw new UnsupportedOperationException("Variable amounts not yet implemented");
            case Amount.Reference _ -> throw new UnsupportedOperationException("Reference amounts not yet implemented");
        };
    }

    private Selectable resolveSubject(Subject subject, ResolutionContext ctx) {
        return switch (subject) {
            case Subject.Select _ ->
                ctx.spellContext()
                        .targets()
                        .findTarget(subject)
                        .orElseThrow(() -> new IllegalStateException("No target found for subject"));
            case Subject.Pronoun _ -> throw new UnsupportedOperationException("Pronoun resolution not yet implemented");
            case Subject.ThatObject _ ->
                throw new UnsupportedOperationException("ThatObject resolution not yet implemented");
        };
    }
}
