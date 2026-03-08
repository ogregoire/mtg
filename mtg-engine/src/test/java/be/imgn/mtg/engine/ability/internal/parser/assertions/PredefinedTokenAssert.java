package be.imgn.mtg.engine.ability.internal.parser.assertions;

import org.assertj.core.api.AbstractAssert;

import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.ability.internal.parser.selector.PredefinedTokenType;
import be.imgn.mtg.engine.effect.CreateTokenEffect;

/// Assertion class for CreateTokenEffect.Predefined.
public class PredefinedTokenAssert extends AbstractAssert<PredefinedTokenAssert, CreateTokenEffect.Predefined> {

    protected PredefinedTokenAssert(CreateTokenEffect.Predefined actual) {
        super(actual, PredefinedTokenAssert.class);
    }

    public static PredefinedTokenAssert assertThat(CreateTokenEffect.Predefined actual) {
        return new PredefinedTokenAssert(actual);
    }

    /// Verifies that the token amount equals the expected value.
    public PredefinedTokenAssert hasAmount(int expected) {
        isNotNull();
        var expectedAmount = new Amount.Exact(expected);
        if (!actual.amount().equals(expectedAmount)) {
            failWithMessage("Expected amount to be <%s> but was <%s>", expectedAmount, actual.amount());
        }
        return this;
    }

    /// Verifies that the token type equals the expected value.
    public PredefinedTokenAssert hasType(PredefinedTokenType expected) {
        isNotNull();
        if (actual.type() != expected) {
            failWithMessage("Expected type to be <%s> but was <%s>", expected, actual.type());
        }
        return this;
    }

    // Artifact tokens

    /// Verifies that this is a Treasure token.
    public PredefinedTokenAssert isTreasure() {
        return hasType(PredefinedTokenType.TREASURE);
    }

    /// Verifies that this is a Food token.
    public PredefinedTokenAssert isFood() {
        return hasType(PredefinedTokenType.FOOD);
    }

    /// Verifies that this is a Gold token.
    public PredefinedTokenAssert isGold() {
        return hasType(PredefinedTokenType.GOLD);
    }

    /// Verifies that this is a Clue token.
    public PredefinedTokenAssert isClue() {
        return hasType(PredefinedTokenType.CLUE);
    }

    /// Verifies that this is a Blood token.
    public PredefinedTokenAssert isBlood() {
        return hasType(PredefinedTokenType.BLOOD);
    }

    /// Verifies that this is a Powerstone token.
    public PredefinedTokenAssert isPowerstone() {
        return hasType(PredefinedTokenType.POWERSTONE);
    }

    /// Verifies that this is a Map token.
    public PredefinedTokenAssert isMap() {
        return hasType(PredefinedTokenType.MAP);
    }

    /// Verifies that this is a Junk token.
    public PredefinedTokenAssert isJunk() {
        return hasType(PredefinedTokenType.JUNK);
    }

    /// Verifies that this is a Lander token.
    public PredefinedTokenAssert isLander() {
        return hasType(PredefinedTokenType.LANDER);
    }

    // Enchantment token

    /// Verifies that this is a Shard token.
    public PredefinedTokenAssert isShard() {
        return hasType(PredefinedTokenType.SHARD);
    }

    // Role tokens

    /// Verifies that this is a Cursed Role token.
    public PredefinedTokenAssert isCursedRole() {
        return hasType(PredefinedTokenType.CURSED_ROLE);
    }

    /// Verifies that this is a Monster Role token.
    public PredefinedTokenAssert isMonsterRole() {
        return hasType(PredefinedTokenType.MONSTER_ROLE);
    }

    /// Verifies that this is a Royal Role token.
    public PredefinedTokenAssert isRoyalRole() {
        return hasType(PredefinedTokenType.ROYAL_ROLE);
    }

    /// Verifies that this is a Sorcerer Role token.
    public PredefinedTokenAssert isSorcererRole() {
        return hasType(PredefinedTokenType.SORCERER_ROLE);
    }

    /// Verifies that this is a Virtuous Role token.
    public PredefinedTokenAssert isVirtuousRole() {
        return hasType(PredefinedTokenType.VIRTUOUS_ROLE);
    }

    /// Verifies that this is a Wicked Role token.
    public PredefinedTokenAssert isWickedRole() {
        return hasType(PredefinedTokenType.WICKED_ROLE);
    }

    /// Verifies that this is a Young Hero Role token.
    public PredefinedTokenAssert isYoungHeroRole() {
        return hasType(PredefinedTokenType.YOUNG_HERO_ROLE);
    }

    // Creature token

    /// Verifies that this is a Walker token.
    public PredefinedTokenAssert isWalker() {
        return hasType(PredefinedTokenType.WALKER);
    }

    // Special token

    /// Verifies that this is an Incubator token.
    public PredefinedTokenAssert isIncubator() {
        return hasType(PredefinedTokenType.INCUBATOR);
    }
}
