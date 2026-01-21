package be.imgn.mtg.engine.ability.internal;

import java.util.ArrayList;
import java.util.List;

import be.imgn.mtg.engine.ability.AbilityContext;
import be.imgn.mtg.engine.ability.AbilityManager;
import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.ability.ActivationResult;
import be.imgn.mtg.engine.object.GameObject;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.state.GameState;
import be.imgn.mtg.engine.zone.Stack;

/// Default implementation of [AbilityManager].
///
/// Coordinates ability activation and registration by delegating to specialized handlers:
/// - [ManaAbilityHandler] for mana abilities (Rule 605)
/// - [LoyaltyAbilityHandler] for loyalty abilities (Rule 606)
/// - [ActivatedAbilityHandler] for other activated abilities (Rule 602)
/// - [StaticAbilityScanner] for registering static/triggered abilities
public final class DefaultAbilityManager implements AbilityManager {

    private final ActivatedAbilityHandler activatedHandler;
    private final ManaAbilityHandler manaHandler;
    private final LoyaltyAbilityHandler loyaltyHandler;
    private final StaticAbilityScanner scanner;
    private final Stack stack;

    DefaultAbilityManager(
            ActivatedAbilityHandler activatedHandler,
            ManaAbilityHandler manaHandler,
            LoyaltyAbilityHandler loyaltyHandler,
            StaticAbilityScanner scanner,
            Stack stack) {
        this.activatedHandler = activatedHandler;
        this.manaHandler = manaHandler;
        this.loyaltyHandler = loyaltyHandler;
        this.scanner = scanner;
        this.stack = stack;
    }

    @Override
    public boolean canActivate(ActivatedAbility ability, GameObject source, GameState state) {
        // Route to appropriate handler based on ability type
        if (ability.isManaAbility()) {
            return manaHandler.canActivate(ability, source, state);
        }
        if (ability.isLoyaltyAbility()) {
            return loyaltyHandler.canActivate(ability, source, state);
        }
        return activatedHandler.canActivate(ability, source, state);
    }

    @Override
    public ActivationResult activate(ActivatedAbility ability, GameObject source, AbilityContext context) {
        // Route to appropriate handler based on ability type
        if (ability.isManaAbility()) {
            return manaHandler.activate(ability, source, context);
        }
        if (ability.isLoyaltyAbility()) {
            return loyaltyHandler.activate(ability, source, context, stack);
        }
        return activatedHandler.activate(ability, source, context, stack);
    }

    @Override
    public List<ActivatedAbility> getActivatableAbilities(GameObject source, GameState state) {
        var result = new ArrayList<ActivatedAbility>();

        for (var ability : source.abilities()) {
            if (ability instanceof ActivatedAbility activated && canActivate(activated, source, state)) {
                result.add(activated);
            }
        }

        return result;
    }

    @Override
    public void registerAbilities(Permanent permanent, GameState state) {
        scanner.registerAbilities(permanent, state);
    }

    @Override
    public void unregisterAbilities(Permanent permanent, GameState state) {
        scanner.unregisterAbilities(permanent, state);
    }
}
