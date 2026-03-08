package be.imgn.mtg.engine.zone.internal;

import static be.imgn.mtg.engine.util.MoreGatherers.instanceOf;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import be.imgn.mtg.engine.ability.SpellAbility;
import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.event.GameEventProcessor;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.object.AbilityOnStack;
import be.imgn.mtg.engine.object.Card;
import be.imgn.mtg.engine.object.CardCopy;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.object.Spell;
import be.imgn.mtg.engine.object.StackObject;
import be.imgn.mtg.engine.resolver.EffectExecutor;
import be.imgn.mtg.engine.resolver.ResolutionContext;
import be.imgn.mtg.engine.spell.TargetChoices;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.state.internal.ObjectStore;
import be.imgn.mtg.engine.zone.PutIntoGraveyardEvent;
import be.imgn.mtg.engine.zone.Stack;
import be.imgn.mtg.engine.zone.ZoneType;

/// Default implementation of [Stack].
///
/// Backed by the central [ObjectStore]. Maintains a secondary deque for LIFO ordering.
public final class DefaultStack implements Stack {

    private final ObjectStore store;
    private final GameEventProcessor eventProcessor;
    private final GameState gameState;
    private final EffectExecutor effectExecutor;

    /// LIFO ordering (first = top).
    private final Deque<StackObject> deque = new ArrayDeque<>();

    /// Creates a new empty stack backed by the given store.
    ///
    /// @param store the central object store
    /// @param eventProcessor the event processor for zone change events
    /// @param gameState the game state for accessing other zones
    /// @param effectExecutor the executor for spell effects
    public DefaultStack(
            ObjectStore store, GameEventProcessor eventProcessor, GameState gameState, EffectExecutor effectExecutor) {
        this.store = store;
        this.eventProcessor = eventProcessor;
        this.gameState = gameState;
        this.effectExecutor = effectExecutor;
    }

    @Override
    public void push(StackObject object) {
        deque.addFirst(object);
        store.add((GameObject) object, this);
    }

    @Override
    public Optional<StackObject> peek() {
        return deque.isEmpty() ? Optional.empty() : Optional.of(deque.peekFirst());
    }

    @Override
    public Optional<StackObject> pop() {
        if (deque.isEmpty()) {
            return Optional.empty();
        }
        var object = deque.removeFirst();
        store.remove((GameObject) object);
        return Optional.of(object);
    }

    @Override
    public void resolve() {
        var top = pop();
        if (top.isEmpty()) {
            return;
        }

        switch (top.get()) {
            case Spell spell -> resolveSpell(spell);
            case AbilityOnStack _ -> {} // Abilities cease to exist after resolving
        }
    }

    private void resolveSpell(Spell spell) {
        switch (spell.source()) {
            case Card card -> {
                if (spell.types().isPermanentType()) {
                    gameState.battlefield().enter(card, spell.controller(), ZoneType.STACK);
                } else {
                    executeSpellEffects(spell);
                    eventProcessor.process(new PutIntoGraveyardEvent(spell, ZoneType.STACK));
                }
            }
            case CardCopy _ -> throw new UnsupportedOperationException("CardCopy resolution not yet implemented");
        }
    }

    private void executeSpellEffects(Spell spell) {
        var spellAbility = spell.abilities().stream()
                .gather(instanceOf(SpellAbility.class))
                .findFirst();

        if (spellAbility.isEmpty() || spellAbility.get().effects().isEmpty()) {
            return;
        }

        // Check target legality (Rule 608.2b)
        var targets = spell.context().targets();
        if (!targets.choices().isEmpty() && !hasLegalTarget(targets, spell.controller())) {
            return; // Fizzle — all targets illegal
        }

        var context = new ResolutionContext(gameState, spell, spell.controller(), spell.context(), eventProcessor);
        for (var effect : spellAbility.get().effects()) {
            effectExecutor.execute(effect, context);
        }
    }

    private boolean hasLegalTarget(TargetChoices targets, Player controller) {
        return targets.choices().stream().anyMatch(choice -> {
            if (choice.subject() instanceof Subject.Select select) {
                return select.selector().matches(choice.target(), controller);
            }
            return true;
        });
    }

    @Override
    public boolean remove(StackObject object) {
        if (deque.remove(object)) {
            store.remove((GameObject) object);
            return true;
        }
        return false;
    }

    @Override
    public List<StackObject> all() {
        return List.copyOf(deque);
    }

    @Override
    public int size() {
        return deque.size();
    }

    @Override
    public boolean isEmpty() {
        return deque.isEmpty();
    }

    @Override
    public boolean contains(StackObject object) {
        return deque.contains(object);
    }

    @Override
    public boolean containsObject(GameObject object) {
        return deque.contains(object);
    }

    @Override
    public Stream<StackObject> stream() {
        return deque.stream();
    }
}
