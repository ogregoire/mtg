package be.imgn.mtg.engine.ability.internal.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.ActivationLimit;
import be.imgn.mtg.engine.ability.ActivationTiming;
import be.imgn.mtg.engine.ability.OncePerTurn;
import be.imgn.mtg.engine.cost.internal.CompoundCost;
import be.imgn.mtg.engine.cost.internal.LoyaltyCost;
import be.imgn.mtg.engine.cost.internal.SacrificeCost;
import be.imgn.mtg.engine.cost.internal.TapCost;
import be.imgn.mtg.engine.mana.ManaCost;
import be.imgn.mtg.engine.zone.ZoneType;

@DisplayName("ActivatedAbilityParser")
class ActivatedAbilityParserTest {

    @Nested
    @DisplayName("Mana abilities")
    class ManaAbilities {

        @Test
        @DisplayName("{T}: Add {G}. → tap mana ability")
        void tapManaAbility() {
            var ability = ActivatedAbilityParser.parse("{T}: Add {G}.");

            assertThat(ability.cost()).isInstanceOf(TapCost.class);
            assertThat(ability.timing()).isEqualTo(ActivationTiming.MANA_ABILITY);
            assertThat(ability.limit()).isEqualTo(ActivationLimit.UNLIMITED);
            assertThat(ability.isManaAbility()).isTrue();
            assertThat(ability.isLoyaltyAbility()).isFalse();
            assertThat(ability.activatesFrom()).containsExactly(ZoneType.BATTLEFIELD);
        }
    }

    @Nested
    @DisplayName("Loyalty abilities")
    class LoyaltyAbilities {

        @Test
        @DisplayName("[-3]: Destroy target creature. → loyalty ability")
        void negativeLoyalty() {
            var ability = ActivatedAbilityParser.parse("[-3]: Destroy target creature.");

            assertThat(ability.cost()).isInstanceOf(LoyaltyCost.class);
            assertThat(((LoyaltyCost) ability.cost()).amount()).isEqualTo(-3);
            assertThat(ability.timing()).isEqualTo(ActivationTiming.SORCERY);
            assertThat(ability.limit()).isInstanceOf(OncePerTurn.class);
            assertThat(ability.isLoyaltyAbility()).isTrue();
            assertThat(ability.isManaAbility()).isFalse();
        }

        @Test
        @DisplayName("[+1]: Draw a card. → loyalty +1 ability")
        void positiveLoyalty() {
            var ability = ActivatedAbilityParser.parse("[+1]: Draw a card.");

            assertThat(ability.cost()).isInstanceOf(LoyaltyCost.class);
            assertThat(((LoyaltyCost) ability.cost()).amount()).isEqualTo(1);
            assertThat(ability.timing()).isEqualTo(ActivationTiming.SORCERY);
            assertThat(ability.limit()).isInstanceOf(OncePerTurn.class);
            assertThat(ability.isLoyaltyAbility()).isTrue();
        }
    }

    @Nested
    @DisplayName("Regular activated abilities")
    class RegularAbilities {

        @Test
        @DisplayName("{1}{R}: Deal 2 damage to any target. → instant timing")
        void manaCostActivated() {
            var ability = ActivatedAbilityParser.parse("{1}{R}: Deal 2 damage to any target.");

            assertThat(ability.cost()).isInstanceOf(ManaCost.class);
            assertThat(((ManaCost) ability.cost()).manaValue()).isEqualTo(2);
            assertThat(ability.timing()).isEqualTo(ActivationTiming.INSTANT);
            assertThat(ability.limit()).isEqualTo(ActivationLimit.UNLIMITED);
            assertThat(ability.isManaAbility()).isFalse();
            assertThat(ability.isLoyaltyAbility()).isFalse();
        }

        @Test
        @DisplayName("{2}{B}, Sacrifice a creature: Destroy target creature. → compound cost")
        void compoundCostActivated() {
            var ability = ActivatedAbilityParser.parse("{2}{B}, Sacrifice a creature: Destroy target creature.");

            assertThat(ability.cost()).isInstanceOf(CompoundCost.class);
            var compound = (CompoundCost) ability.cost();
            assertThat(compound.costs()).hasSize(2);
            assertThat(compound.costs().get(0)).isInstanceOf(ManaCost.class);
            assertThat(compound.costs().get(1)).isInstanceOf(SacrificeCost.class);
            assertThat(ability.timing()).isEqualTo(ActivationTiming.INSTANT);
            assertThat(ability.limit()).isEqualTo(ActivationLimit.UNLIMITED);
        }
    }

    @Nested
    @DisplayName("Ability properties")
    class AbilityProperties {

        @Test
        @DisplayName("parsed ability has a non-null id")
        void hasId() {
            var ability = ActivatedAbilityParser.parse("{T}: Add {G}.");

            assertThat(ability.id()).isNotNull();
            assertThat(ability.id().value()).isNotNull();
        }

        @Test
        @DisplayName("parsed ability has oracle text")
        void hasOracleText() {
            var ability = ActivatedAbilityParser.parse("{T}: Add {G}.");

            assertThat(ability.oracleText()).isNotNull();
            assertThat(ability.oracleText()).isNotEmpty();
        }

        @Test
        @DisplayName("different parsed abilities have different IDs")
        void uniqueIds() {
            var ability1 = ActivatedAbilityParser.parse("{T}: Add {G}.");
            var ability2 = ActivatedAbilityParser.parse("{T}: Add {G}.");

            assertThat(ability1.id()).isNotEqualTo(ability2.id());
        }
    }
}
