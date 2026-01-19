package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.engine.ability.internal.parser.assertions.EffectAssertions.assertThat;
import static be.imgn.mtg.engine.characteristics.ArtifactType.EQUIPMENT;
import static be.imgn.mtg.engine.characteristics.BasicLandType.FOREST;
import static be.imgn.mtg.engine.characteristics.Color.BLACK;
import static be.imgn.mtg.engine.characteristics.Color.BLUE;
import static be.imgn.mtg.engine.characteristics.Color.GREEN;
import static be.imgn.mtg.engine.characteristics.Color.RED;
import static be.imgn.mtg.engine.characteristics.Color.WHITE;
import static be.imgn.mtg.engine.characteristics.CreatureType.BIRD;
import static be.imgn.mtg.engine.characteristics.CreatureType.CAT;
import static be.imgn.mtg.engine.characteristics.CreatureType.CITIZEN;
import static be.imgn.mtg.engine.characteristics.CreatureType.CONSTRUCT;
import static be.imgn.mtg.engine.characteristics.CreatureType.DRAGON;
import static be.imgn.mtg.engine.characteristics.CreatureType.DRYAD;
import static be.imgn.mtg.engine.characteristics.CreatureType.HAMSTER;
import static be.imgn.mtg.engine.characteristics.CreatureType.OOZE;
import static be.imgn.mtg.engine.characteristics.CreatureType.SAPROLING;
import static be.imgn.mtg.engine.characteristics.CreatureType.SOLDIER;
import static be.imgn.mtg.engine.characteristics.CreatureType.ZOMBIE;
import static be.imgn.mtg.engine.characteristics.EnchantmentType.AURA;
import static be.imgn.mtg.engine.characteristics.EnchantmentType.CURSE;
import static be.imgn.mtg.engine.characteristics.Type.ARTIFACT;
import static be.imgn.mtg.engine.characteristics.Type.CREATURE;
import static be.imgn.mtg.engine.characteristics.Type.ENCHANTMENT;
import static be.imgn.mtg.engine.characteristics.Type.LAND;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.internal.parser.effect.CreateTokenEffect;
import be.imgn.mtg.parse.CharPredicate;

@DisplayName("TokenParser")
class TokenParserTest {

    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    private CreateTokenEffect parse(String text) {
        return TokenParser.CREATE_TOKEN_EFFECT.parseSkipping(WHITESPACE, text);
    }

    @Nested
    @DisplayName("Creature tokens")
    class CreatureTokens {

        @Test
        @DisplayName("Create a 1/1 white Soldier creature token.")
        void createSoldierToken() {
            assertThat(parse("Create a 1/1 white Soldier creature token."))
                    .isToken()
                    .hasAmount(1)
                    .hasPowerAndToughness(1, 1)
                    .hasColors(WHITE)
                    .hasTypes(CREATURE)
                    .hasSubtypes(SOLDIER)
                    .hasNoAbilities();
        }

        @Test
        @DisplayName("Create two 1/1 green Saproling creature tokens.")
        void createTwoSaprolingTokens() {
            assertThat(parse("Create two 1/1 green Saproling creature tokens."))
                    .isToken()
                    .hasAmount(2)
                    .hasColors(GREEN)
                    .hasSubtypes(SAPROLING);
        }

        @Test
        @DisplayName("Create a 2/2 black Zombie creature token.")
        void createZombieToken() {
            assertThat(parse("Create a 2/2 black Zombie creature token."))
                    .isToken()
                    .hasAmount(1)
                    .hasPowerAndToughness(2, 2)
                    .hasColors(BLACK)
                    .hasSubtypes(ZOMBIE);
        }

        @Test
        @DisplayName("Create a 1/1 white Soldier creature token with vigilance.")
        void createSoldierWithVigilance() {
            assertThat(parse("Create a 1/1 white Soldier creature token with vigilance."))
                    .isToken()
                    .hasAbilities("vigilance");
        }

        @Test
        @DisplayName("Create a 1/1 red Elemental creature token with haste.")
        void createElementalWithHaste() {
            assertThat(parse("Create a 1/1 red Elemental creature token with haste."))
                    .isToken()
                    .hasColors(RED)
                    .hasAbilities("haste");
        }

        @Test
        @DisplayName("Create a 2/2 white Knight creature token with first strike.")
        void createKnightWithFirstStrike() {
            assertThat(parse("Create a 2/2 white Knight creature token with first strike."))
                    .isToken()
                    .hasPowerAndToughness(2, 2)
                    .hasColors(WHITE)
                    .hasAbilities("first strike");
        }

        @Test
        @DisplayName("Create a 4/4 red Dragon creature token with flying and double strike.")
        void createDragonWithFlyingAndDoubleStrike() {
            assertThat(parse("Create a 4/4 red Dragon creature token with flying and double strike."))
                    .isToken()
                    .hasPowerAndToughness(4, 4)
                    .hasColors(RED)
                    .hasSubtypes(DRAGON)
                    .hasAbilities("flying", "double strike");
        }

        @Test
        @DisplayName("Create a 3/3 white Knight creature token with first strike, vigilance, and lifelink.")
        void createKnightWithThreeAbilities() {
            assertThat(parse("Create a 3/3 white Knight creature token with first strike, vigilance, and lifelink."))
                    .isToken()
                    .hasPowerAndToughness(3, 3)
                    .hasColors(WHITE)
                    .hasAbilities("first strike", "vigilance", "lifelink");
        }

        @Test
        @DisplayName(
                "Create Smaug, a legendary 6/6 red Dragon creature token with flying, haste, and \"When Smaug dies, create fourteen Treasure tokens.\"")
        void createSmaugWithQuotedAbility() {
            assertThat(
                            parse(
                                    "Create Smaug, a legendary 6/6 red Dragon creature token with flying, haste, and \"When Smaug dies, create fourteen Treasure tokens.\""))
                    .isToken()
                    .hasName("Smaug")
                    .isLegendary()
                    .hasPowerAndToughness(6, 6)
                    .hasColors(RED)
                    .hasSubtypes(DRAGON)
                    .hasAbilities("flying", "haste", "When Smaug dies, create fourteen Treasure tokens.");
        }

        @Test
        @DisplayName("Create a 6/12 colorless Construct artifact creature token with trample.")
        void createColorlessConstructArtifactToken() {
            assertThat(parse("Create a 6/12 colorless Construct artifact creature token with trample."))
                    .isToken()
                    .hasAmount(1)
                    .hasPowerAndToughness(6, 12)
                    .isColorless()
                    .hasTypes(ARTIFACT, CREATURE)
                    .hasSubtypes(CONSTRUCT)
                    .hasAbilities("trample");
        }

        @Test
        @DisplayName("Create a 1/1 green and white Citizen creature token.")
        void createTwoColorCitizenToken() {
            assertThat(parse("Create a 1/1 green and white Citizen creature token."))
                    .isToken()
                    .hasAmount(1)
                    .hasPowerAndToughness(1, 1)
                    .hasColors(GREEN, WHITE)
                    .hasTypes(CREATURE)
                    .hasSubtypes(CITIZEN);
        }

        @Test
        @DisplayName("Create a 3/3 black, red, and green Cat Dragon creature token with flying.")
        void createThreeColorCatDragonToken() {
            assertThat(parse("Create a 3/3 black, red, and green Cat Dragon creature token with flying."))
                    .isToken()
                    .hasAmount(1)
                    .hasPowerAndToughness(3, 3)
                    .hasColors(BLACK, RED, GREEN)
                    .hasTypes(CREATURE)
                    .hasSubtypes(CAT, DRAGON)
                    .hasAbilities("flying");
        }

        @Test
        @DisplayName("Create a 2/2 blue Bird enchantment creature token with flying.")
        void createEnchantmentCreatureToken() {
            assertThat(parse("Create a 2/2 blue Bird enchantment creature token with flying."))
                    .isToken()
                    .hasAmount(1)
                    .hasPowerAndToughness(2, 2)
                    .hasColors(BLUE)
                    .hasTypes(ENCHANTMENT, CREATURE)
                    .hasSubtypes(BIRD)
                    .hasAbilities("flying");
        }

        @Test
        @DisplayName("Create X 1/1 green Forest Dryad land creature tokens.")
        void createLandCreatureTokens() {
            assertThat(parse("Create X 1/1 green Forest Dryad land creature tokens."))
                    .isToken()
                    .hasPowerAndToughness(1, 1)
                    .hasColors(GREEN)
                    .hasTypes(LAND, CREATURE)
                    .hasSubtypes(FOREST, DRYAD);
        }

        @Test
        @DisplayName("Create Boo, a legendary 1/1 red Hamster creature token with trample and haste.")
        void createLegendaryNamedToken() {
            assertThat(parse("Create Boo, a legendary 1/1 red Hamster creature token with trample and haste."))
                    .isToken()
                    .hasName("Boo")
                    .hasAmount(1)
                    .isLegendary()
                    .hasPowerAndToughness(1, 1)
                    .hasColors(RED)
                    .hasTypes(CREATURE)
                    .hasSubtypes(HAMSTER)
                    .hasAbilities("trample", "haste");
        }

        @Test
        @DisplayName("Create X X/X green Ooze creature tokens.")
        void createVariablePTTokens() {
            assertThat(parse("Create X X/X green Ooze creature tokens."))
                    .isToken()
                    .hasVariableAmount()
                    .hasVariablePowerAndToughness()
                    .hasColors(GREEN)
                    .hasTypes(CREATURE)
                    .hasSubtypes(OOZE);
        }

        @Test
        @DisplayName(
                "Create Mechtitan, a legendary 10/10 Construct artifact creature token with flying, vigilance, trample, lifelink, and haste that's all colors.")
        void createMechtitanToken() {
            assertThat(
                            parse(
                                    "Create Mechtitan, a legendary 10/10 Construct artifact creature token with flying, vigilance, trample, lifelink, and haste that's all colors."))
                    .isToken()
                    .hasName("Mechtitan")
                    .hasAmount(1)
                    .isLegendary()
                    .hasPowerAndToughness(10, 10)
                    .isAllColors()
                    .hasTypes(ARTIFACT, CREATURE)
                    .hasSubtypes(CONSTRUCT)
                    .hasAbilities("flying", "vigilance", "trample", "lifelink", "haste");
        }
    }

    @Nested
    @DisplayName("Non-creature tokens")
    class NoncreatureTokens {

        @Test
        @DisplayName("Create a black Aura Curse enchantment token.")
        void createAuraCurseEnchantmentToken() {
            assertThat(parse("Create a black Aura Curse enchantment token."))
                    .isToken()
                    .isNotCreature()
                    .hasAmount(1)
                    .hasColors(BLACK)
                    .hasTypes(ENCHANTMENT)
                    .hasSubtypes(AURA, CURSE);
        }

        @Test
        @DisplayName("Create Cragflame, a legendary colorless Equipment artifact token.")
        void createNamedLegendaryArtifactToken() {
            assertThat(parse("Create Cragflame, a legendary colorless Equipment artifact token."))
                    .isToken()
                    .isNotCreature()
                    .hasName("Cragflame")
                    .hasAmount(1)
                    .isLegendary()
                    .isColorless()
                    .hasTypes(ARTIFACT)
                    .hasSubtypes(EQUIPMENT);
        }
    }

    @Nested
    @DisplayName("Predefined tokens (rule 111.10)")
    class PredefinedTokens {

        @Nested
        @DisplayName("Artifact tokens")
        class ArtifactTokens {

            @Test
            @DisplayName("Create a Treasure token.")
            void createTreasureToken() {
                assertThat(parse("Create a Treasure token."))
                        .isPredefinedToken()
                        .hasAmount(1)
                        .isTreasure();
            }

            @Test
            @DisplayName("Create two Food tokens.")
            void createTwoFoodTokens() {
                assertThat(parse("Create two Food tokens."))
                        .isPredefinedToken()
                        .hasAmount(2)
                        .isFood();
            }

            @Test
            @DisplayName("Create a Gold token.")
            void createGoldToken() {
                assertThat(parse("Create a Gold token.")).isPredefinedToken().isGold();
            }

            @Test
            @DisplayName("Create a Clue token.")
            void createClueToken() {
                assertThat(parse("Create a Clue token.")).isPredefinedToken().isClue();
            }

            @Test
            @DisplayName("Create three Blood tokens.")
            void createThreeBloodTokens() {
                assertThat(parse("Create three Blood tokens."))
                        .isPredefinedToken()
                        .hasAmount(3)
                        .isBlood();
            }

            @Test
            @DisplayName("Create a Powerstone token.")
            void createPowerstoneToken() {
                assertThat(parse("Create a Powerstone token."))
                        .isPredefinedToken()
                        .isPowerstone();
            }

            @Test
            @DisplayName("Create a Map token.")
            void createMapToken() {
                assertThat(parse("Create a Map token.")).isPredefinedToken().isMap();
            }

            @Test
            @DisplayName("Create a Junk token.")
            void createJunkToken() {
                assertThat(parse("Create a Junk token.")).isPredefinedToken().isJunk();
            }

            @Test
            @DisplayName("Create a Lander token.")
            void createLanderToken() {
                assertThat(parse("Create a Lander token.")).isPredefinedToken().isLander();
            }
        }

        @Nested
        @DisplayName("Enchantment tokens")
        class EnchantmentTokens {

            @Test
            @DisplayName("Create a Shard token.")
            void createShardToken() {
                assertThat(parse("Create a Shard token.")).isPredefinedToken().isShard();
            }
        }

        @Nested
        @DisplayName("Role tokens")
        class RoleTokens {

            @Test
            @DisplayName("Create a Cursed Role token.")
            void createCursedRoleToken() {
                assertThat(parse("Create a Cursed Role token."))
                        .isPredefinedToken()
                        .isCursedRole();
            }

            @Test
            @DisplayName("Create a Monster Role token.")
            void createMonsterRoleToken() {
                assertThat(parse("Create a Monster Role token."))
                        .isPredefinedToken()
                        .isMonsterRole();
            }

            @Test
            @DisplayName("Create a Royal Role token.")
            void createRoyalRoleToken() {
                assertThat(parse("Create a Royal Role token."))
                        .isPredefinedToken()
                        .isRoyalRole();
            }

            @Test
            @DisplayName("Create a Sorcerer Role token.")
            void createSorcererRoleToken() {
                assertThat(parse("Create a Sorcerer Role token."))
                        .isPredefinedToken()
                        .isSorcererRole();
            }

            @Test
            @DisplayName("Create a Virtuous Role token.")
            void createVirtuousRoleToken() {
                assertThat(parse("Create a Virtuous Role token."))
                        .isPredefinedToken()
                        .isVirtuousRole();
            }

            @Test
            @DisplayName("Create a Wicked Role token.")
            void createWickedRoleToken() {
                assertThat(parse("Create a Wicked Role token."))
                        .isPredefinedToken()
                        .isWickedRole();
            }

            @Test
            @DisplayName("Create a Young Hero Role token.")
            void createYoungHeroRoleToken() {
                assertThat(parse("Create a Young Hero Role token."))
                        .isPredefinedToken()
                        .isYoungHeroRole();
            }

            @Test
            @DisplayName("Create two Monster Role tokens.")
            void createTwoMonsterRoleTokens() {
                assertThat(parse("Create two Monster Role tokens."))
                        .isPredefinedToken()
                        .hasAmount(2)
                        .isMonsterRole();
            }
        }

        @Nested
        @DisplayName("Creature tokens")
        class CreatureTokensNested {

            @Test
            @DisplayName("Create a Walker token.")
            void createWalkerToken() {
                assertThat(parse("Create a Walker token.")).isPredefinedToken().isWalker();
            }
        }

        @Nested
        @DisplayName("Special tokens")
        class SpecialTokens {

            @Test
            @DisplayName("Create an Incubator token.")
            void createIncubatorToken() {
                assertThat(parse("Create an Incubator token."))
                        .isPredefinedToken()
                        .isIncubator();
            }
        }
    }

    @Nested
    @DisplayName("Card-named tokens (rule 111.11)")
    class CardNamedTokens {

        @Test
        @DisplayName("Create a Tarmogoyf token.")
        void createTarmogoyfToken() {
            assertThat(parse("Create a Tarmogoyf token."))
                    .isByCardName()
                    .hasAmount(1)
                    .hasCardName("Tarmogoyf");
        }

        @Test
        @DisplayName("Create two Marit Lage tokens.")
        void createTwoMaritLageTokens() {
            assertThat(parse("Create two Marit Lage tokens."))
                    .isByCardName()
                    .hasAmount(2)
                    .hasCardName("Marit Lage");
        }

        @Test
        @DisplayName("Create a Minsc and Boo token.")
        void createMinscAndBooToken() {
            assertThat(parse("Create a Minsc and Boo token."))
                    .isByCardName()
                    .hasAmount(1)
                    .hasCardName("Minsc and Boo");
        }

        @Test
        @DisplayName("Create X Llanowar Elves tokens.")
        void createVariableAmountTokens() {
            assertThat(parse("Create X Llanowar Elves tokens."))
                    .isByCardName()
                    .hasVariableAmount()
                    .hasCardName("Llanowar Elves");
        }
    }
}
