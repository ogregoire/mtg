package be.imgn.mtg.engine.game;

import static be.imgn.mtg.engine.assertions.MTGAssertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.ActivatedAbility;
import be.imgn.mtg.engine.ability.ActivationResult;
import be.imgn.mtg.engine.ability.internal.parser.ReferenceParser;
import be.imgn.mtg.engine.action.ExecutionResult;
import be.imgn.mtg.engine.characteristics.BasicLandType;
import be.imgn.mtg.engine.cost.internal.TapCost;
import be.imgn.mtg.engine.mana.AddManaEffect;
import be.imgn.mtg.engine.mana.Mana;
import be.imgn.mtg.engine.mana.ManaType;
import be.imgn.mtg.engine.spell.SpellContext;
import be.imgn.mtg.engine.spell.TargetChoice;
import be.imgn.mtg.engine.spell.TargetChoices;
import be.imgn.mtg.engine.turn.Phase;

@DisplayName("Magic 2010 card interactions")
class M10Test {

    private static final Path DB_PATH =
            Path.of(System.getProperty("user.home"), "Library", "Application Support", "mtg-engine", "cards.mv.db");

    private TestGame game;

    @BeforeAll
    static void requireDatabase() {
        assumeThat(Files.exists(DB_PATH))
                .as("Card database must exist at %s", DB_PATH)
                .isTrue();
    }

    @BeforeEach
    void setUp() {
        game = TestGame.create();
    }

    @Test
    @DisplayName("Acidic Slime — 2/2 creature with deathtouch ETB")
    void acidicSlime() {
        assertThat(game.createPermanent("Acidic Slime"))
                .hasName("Acidic Slime")
                .isCreature()
                .hasPower(2)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Acolyte of Xathrid — 0/1 Human Cleric")
    void acolyteOfXathrid() {
        assertThat(game.createPermanent("Acolyte of Xathrid"))
                .isCreature()
                .hasPower(0)
                .hasToughness(1);
    }

    @Test
    @DisplayName("Act of Treason — sorcery")
    void actOfTreason() {
        assertThat(game.addToHand("Act of Treason")).isSorcery();
    }

    @Test
    @DisplayName("Air Elemental — 4/4 flying creature")
    void airElemental() {
        assertThat(game.createPermanent("Air Elemental"))
                .isCreature()
                .hasPower(4)
                .hasToughness(4);
    }

    @Test
    @DisplayName("Ajani Goldmane — legendary planeswalker")
    void ajaniGoldmane() {
        assertThat(game.createPermanent("Ajani Goldmane")).isPlaneswalker().isLegendary();
    }

    @Test
    @DisplayName("Alluring Siren — 1/1 Siren creature")
    void alluringSiren() {
        assertThat(game.createPermanent("Alluring Siren"))
                .isCreature()
                .hasPower(1)
                .hasToughness(1);
    }

    @Test
    @DisplayName("Angel's Feather — artifact")
    void angelsFeather() {
        assertThat(game.createPermanent("Angel's Feather")).isArtifact();
    }

    @Test
    @DisplayName("Angel's Mercy — instant")
    void angelsMercy() {
        assertThat(game.addToHand("Angel's Mercy")).isInstant();
    }

    @Test
    @DisplayName("Ant Queen — 5/5 Insect creature")
    void antQueen() {
        assertThat(game.createPermanent("Ant Queen")).isCreature().hasPower(5).hasToughness(5);
    }

    @Test
    @DisplayName("Armored Ascension — aura enchantment")
    void armoredAscension() {
        assertThat(game.addToHand("Armored Ascension")).isEnchantment();
    }

    @Test
    @DisplayName("Assassinate — sorcery")
    void assassinate() {
        assertThat(game.addToHand("Assassinate")).isSorcery();
    }

    @Test
    @DisplayName("Awakener Druid — 1/1 Human Druid")
    void awakenerDruid() {
        assertThat(game.createPermanent("Awakener Druid"))
                .isCreature()
                .hasPower(1)
                .hasToughness(1);
    }

    @Test
    @DisplayName("Ball Lightning — 6/1 Elemental")
    void ballLightning() {
        assertThat(game.createPermanent("Ball Lightning"))
                .isCreature()
                .hasPower(6)
                .hasToughness(1);
    }

    @Test
    @DisplayName("Baneslayer Angel — 5/5 Angel")
    void baneslayerAngel() {
        assertThat(game.createPermanent("Baneslayer Angel"))
                .isCreature()
                .hasPower(5)
                .hasToughness(5);
    }

    @Test
    @DisplayName("Berserkers of Blood Ridge — 4/4 Human Berserker")
    void berserkersOfBloodRidge() {
        assertThat(game.createPermanent("Berserkers of Blood Ridge"))
                .isCreature()
                .hasPower(4)
                .hasToughness(4);
    }

    @Test
    @DisplayName("Birds of Paradise — 0/1 flyer that taps for any color")
    void birdsOfParadise() {
        var birds = game.createPermanent("Birds of Paradise");
        assertThat(birds).isCreature().hasPower(0).hasToughness(1);

        // Verify mana ability: {T}: Add one mana of any color.
        var manaAbility = birds.abilities().stream()
                .filter(ActivatedAbility.class::isInstance)
                .map(ActivatedAbility.class::cast)
                .filter(ActivatedAbility::isManaAbility)
                .findFirst()
                .orElseThrow();
        Assertions.assertThat(manaAbility.cost()).isInstanceOf(TapCost.class);
        Assertions.assertThat(manaAbility.effect()).isInstanceOf(AddManaEffect.Combination.class);
        var combo = (AddManaEffect.Combination) manaAbility.effect();
        Assertions.assertThat(combo.allowedTypes()).containsExactlyInAnyOrder(ManaType.Colored.values());

        // Activate: tap Birds to add green mana
        var result = game.activateManaAbility(birds, ManaType.GREEN);
        Assertions.assertThat(result).isInstanceOf(ActivationResult.ManaAbilitySuccess.class);
        Assertions.assertThat(birds.isTapped()).isTrue();
        Assertions.assertThat(game.player1().manaPool().count(ManaType.GREEN)).isEqualTo(1);
    }

    @Test
    @DisplayName("Black Knight — 2/2 Human Knight")
    void blackKnight() {
        assertThat(game.createPermanent("Black Knight"))
                .isCreature()
                .hasPower(2)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Blinding Mage — 1/2 Human Wizard")
    void blindingMage() {
        assertThat(game.createPermanent("Blinding Mage"))
                .isCreature()
                .hasPower(1)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Bog Wraith — 3/3 Wraith")
    void bogWraith() {
        assertThat(game.createPermanent("Bog Wraith")).isCreature().hasPower(3).hasToughness(3);
    }

    @Test
    @DisplayName("Bogardan Hellkite — 5/5 Dragon")
    void bogardanHellkite() {
        assertThat(game.createPermanent("Bogardan Hellkite"))
                .isCreature()
                .hasPower(5)
                .hasToughness(5);
    }

    @Test
    @DisplayName("Borderland Ranger — 2/2 Human Scout Ranger")
    void borderlandRanger() {
        assertThat(game.createPermanent("Borderland Ranger"))
                .isCreature()
                .hasPower(2)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Bountiful Harvest — sorcery")
    void bountifulHarvest() {
        assertThat(game.addToHand("Bountiful Harvest")).isSorcery();
    }

    @Test
    @DisplayName("Bramble Creeper — 0/3 Elemental")
    void brambleCreeper() {
        assertThat(game.createPermanent("Bramble Creeper"))
                .isCreature()
                .hasPower(0)
                .hasToughness(3);
    }

    @Test
    @DisplayName("Burning Inquiry — sorcery")
    void burningInquiry() {
        assertThat(game.addToHand("Burning Inquiry")).isSorcery();
    }

    @Test
    @DisplayName("Burst of Speed — sorcery")
    void burstOfSpeed() {
        assertThat(game.addToHand("Burst of Speed")).isSorcery();
    }

    @Test
    @DisplayName("Cancel — instant counterspell")
    void cancel() {
        assertThat(game.addToHand("Cancel")).isInstant();
    }

    @Test
    @DisplayName("Canyon Minotaur — 3/3 vanilla creature")
    void canyonMinotaur() {
        assertThat(game.createPermanent("Canyon Minotaur"))
                .isCreature()
                .hasPower(3)
                .hasToughness(3);
    }

    @Test
    @DisplayName("Capricious Efreet — 6/4 Efreet")
    void capriciousEfreet() {
        assertThat(game.createPermanent("Capricious Efreet"))
                .isCreature()
                .hasPower(6)
                .hasToughness(4);
    }

    @Test
    @DisplayName("Captain of the Watch — 3/3 Human Soldier")
    void captainOfTheWatch() {
        assertThat(game.createPermanent("Captain of the Watch"))
                .isCreature()
                .hasPower(3)
                .hasToughness(3);
    }

    @Test
    @DisplayName("Celestial Purge — instant")
    void celestialPurge() {
        assertThat(game.addToHand("Celestial Purge")).isInstant();
    }

    @Test
    @DisplayName("Cemetery Reaper — 2/2 Zombie")
    void cemeteryReaper() {
        assertThat(game.createPermanent("Cemetery Reaper"))
                .isCreature()
                .hasPower(2)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Centaur Courser — 3/3 vanilla creature")
    void centaurCourser() {
        assertThat(game.createPermanent("Centaur Courser"))
                .isCreature()
                .hasPower(3)
                .hasToughness(3);
    }

    @Test
    @DisplayName("Chandra Nalaar — legendary planeswalker")
    void chandraNalaar() {
        assertThat(game.createPermanent("Chandra Nalaar")).isPlaneswalker().isLegendary();
    }

    @Test
    @DisplayName("Child of Night — 2/1 Vampire with lifelink")
    void childOfNight() {
        assertThat(game.createPermanent("Child of Night"))
                .isCreature()
                .hasPower(2)
                .hasToughness(1);
    }

    @Test
    @DisplayName("Clone — 0/0 Shapeshifter")
    void cloneCard() {
        assertThat(game.createPermanent("Clone")).isCreature().hasPower(0).hasToughness(0);
    }

    @Test
    @DisplayName("Coat of Arms — artifact")
    void coatOfArms() {
        assertThat(game.createPermanent("Coat of Arms")).isArtifact();
    }

    @Test
    @DisplayName("Consume Spirit — sorcery")
    void consumeSpirit() {
        assertThat(game.addToHand("Consume Spirit")).isSorcery();
    }

    @Test
    @DisplayName("Convincing Mirage — aura enchantment")
    void convincingMirage() {
        assertThat(game.addToHand("Convincing Mirage")).isEnchantment();
    }

    @Test
    @DisplayName("Coral Merfolk — 2/1 vanilla creature")
    void coralMerfolk() {
        assertThat(game.createPermanent("Coral Merfolk"))
                .isCreature()
                .hasPower(2)
                .hasToughness(1);
    }

    @Test
    @DisplayName("Craw Wurm — 6/4 vanilla creature")
    void crawWurm() {
        assertThat(game.createPermanent("Craw Wurm")).isCreature().hasPower(6).hasToughness(4);
    }

    @Test
    @DisplayName("Cudgel Troll — 4/3 Troll with regenerate")
    void cudgelTroll() {
        assertThat(game.createPermanent("Cudgel Troll"))
                .isCreature()
                .hasPower(4)
                .hasToughness(3);
    }

    @Test
    @DisplayName("Darksteel Colossus — 11/11 artifact creature")
    void darksteelColossus() {
        assertThat(game.createPermanent("Darksteel Colossus"))
                .isArtifact()
                .isCreature()
                .hasPower(11)
                .hasToughness(11);
    }

    @Test
    @DisplayName("Deadly Recluse — 1/2 Spider")
    void deadlyRecluse() {
        assertThat(game.createPermanent("Deadly Recluse"))
                .isCreature()
                .hasPower(1)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Deathmark — sorcery")
    void deathmark() {
        assertThat(game.addToHand("Deathmark")).isSorcery();
    }

    @Test
    @DisplayName("Demon's Horn — artifact")
    void demonsHorn() {
        assertThat(game.createPermanent("Demon's Horn")).isArtifact();
    }

    @Test
    @DisplayName("Diabolic Tutor — sorcery")
    void diabolicTutor() {
        assertThat(game.addToHand("Diabolic Tutor")).isSorcery();
    }

    @Test
    @DisplayName("Disentomb — sorcery")
    void disentomb() {
        assertThat(game.addToHand("Disentomb")).isSorcery();
    }

    @Test
    @DisplayName("Disorient — instant")
    void disorient() {
        assertThat(game.addToHand("Disorient")).isInstant();
    }

    @Test
    @DisplayName("Divination — sorcery")
    void divination() {
        assertThat(game.addToHand("Divination")).isSorcery();
    }

    @Test
    @DisplayName("Divine Verdict — instant")
    void divineVerdict() {
        assertThat(game.addToHand("Divine Verdict")).isInstant();
    }

    @Test
    @DisplayName("Djinn of Wishes — 4/4 Djinn")
    void djinnOfWishes() {
        assertThat(game.createPermanent("Djinn of Wishes"))
                .isCreature()
                .hasPower(4)
                .hasToughness(4);
    }

    @Test
    @DisplayName("Doom Blade — instant")
    void doomBlade() {
        assertThat(game.addToHand("Doom Blade")).isInstant();
    }

    @Test
    @DisplayName("Dragon Whelp — 2/3 Dragon")
    void dragonWhelp() {
        assertThat(game.createPermanent("Dragon Whelp"))
                .isCreature()
                .hasPower(2)
                .hasToughness(3);
    }

    @Test
    @DisplayName("Dragon's Claw — artifact")
    void dragonsClaw() {
        assertThat(game.createPermanent("Dragon's Claw")).isArtifact();
    }

    @Test
    @DisplayName("Dragonskull Summit — land")
    void dragonskullSummit() {
        assertThat(game.createPermanent("Dragonskull Summit")).isLand();
    }

    @Test
    @DisplayName("Dread Warlock — 2/2 Human Wizard Warlock")
    void dreadWarlock() {
        assertThat(game.createPermanent("Dread Warlock"))
                .isCreature()
                .hasPower(2)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Drowned Catacomb — land")
    void drownedCatacomb() {
        assertThat(game.createPermanent("Drowned Catacomb")).isLand();
    }

    @Test
    @DisplayName("Drudge Skeletons — 1/1 Skeleton")
    void drudgeSkeletons() {
        assertThat(game.createPermanent("Drudge Skeletons"))
                .isCreature()
                .hasPower(1)
                .hasToughness(1);
    }

    @Test
    @DisplayName("Duress — sorcery")
    void duress() {
        assertThat(game.addToHand("Duress")).isSorcery();
    }

    @Test
    @DisplayName("Earthquake — sorcery")
    void earthquake() {
        assertThat(game.addToHand("Earthquake")).isSorcery();
    }

    @Test
    @DisplayName("Elite Vanguard — 2/1 vanilla creature")
    void eliteVanguard() {
        assertThat(game.createPermanent("Elite Vanguard"))
                .isCreature()
                .hasPower(2)
                .hasToughness(1);
    }

    @Test
    @DisplayName("Elvish Archdruid — 2/2 Elf Druid")
    void elvishArchdruid() {
        assertThat(game.createPermanent("Elvish Archdruid"))
                .isCreature()
                .hasPower(2)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Elvish Piper — 1/1 Elf Shaman")
    void elvishPiper() {
        assertThat(game.createPermanent("Elvish Piper"))
                .isCreature()
                .hasPower(1)
                .hasToughness(1);
    }

    @Test
    @DisplayName("Elvish Visionary — 1/1 Elf Shaman")
    void elvishVisionary() {
        assertThat(game.createPermanent("Elvish Visionary"))
                .isCreature()
                .hasPower(1)
                .hasToughness(1);
    }

    @Test
    @DisplayName("Emerald Oryx — 2/3 Antelope")
    void emeraldOryx() {
        assertThat(game.createPermanent("Emerald Oryx"))
                .isCreature()
                .hasPower(2)
                .hasToughness(3);
    }

    @Test
    @DisplayName("Enormous Baloth — 7/7 vanilla Beast")
    void enormousBaloth() {
        assertThat(game.createPermanent("Enormous Baloth"))
                .isCreature()
                .hasPower(7)
                .hasToughness(7);
    }

    @Test
    @DisplayName("Entangling Vines — aura enchantment")
    void entanglingVines() {
        assertThat(game.addToHand("Entangling Vines")).isEnchantment();
    }

    @Test
    @DisplayName("Essence Scatter — instant")
    void essenceScatter() {
        assertThat(game.addToHand("Essence Scatter")).isInstant();
    }

    @Test
    @DisplayName("Excommunicate — sorcery")
    void excommunicate() {
        assertThat(game.addToHand("Excommunicate")).isSorcery();
    }

    @Test
    @DisplayName("Fabricate — sorcery")
    void fabricate() {
        assertThat(game.addToHand("Fabricate")).isSorcery();
    }

    @Test
    @DisplayName("Fiery Hellhound — 2/2 Elemental Dog")
    void fieryHellhound() {
        assertThat(game.createPermanent("Fiery Hellhound"))
                .isCreature()
                .hasPower(2)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Fireball — sorcery")
    void fireball() {
        assertThat(game.addToHand("Fireball")).isSorcery();
    }

    @Test
    @DisplayName("Firebreathing — aura enchantment")
    void firebreathing() {
        assertThat(game.addToHand("Firebreathing")).isEnchantment();
    }

    @Test
    @DisplayName("Flashfreeze — instant")
    void flashfreeze() {
        assertThat(game.addToHand("Flashfreeze")).isInstant();
    }

    @Test
    @DisplayName("Fog — instant")
    void fog() {
        assertThat(game.addToHand("Fog")).isInstant();
    }

    @Test
    @DisplayName("Forest — basic land")
    void forest() {
        assertThat(game.createPermanent("Forest")).isLand().isBasic().hasSubtype(BasicLandType.FOREST);
    }

    @Test
    @DisplayName("Gargoyle Castle — land")
    void gargoyleCastle() {
        assertThat(game.createPermanent("Gargoyle Castle")).isLand();
    }

    @Test
    @DisplayName("Garruk Wildspeaker — legendary planeswalker")
    void garrukWildspeaker() {
        assertThat(game.createPermanent("Garruk Wildspeaker")).isPlaneswalker().isLegendary();
    }

    @Test
    @DisplayName("Giant Growth — instant")
    void giantGrowth() {
        assertThat(game.addToHand("Giant Growth")).isInstant();
    }

    @Test
    @DisplayName("Giant Spider — 2/4 Spider with reach")
    void giantSpider() {
        assertThat(game.createPermanent("Giant Spider"))
                .isCreature()
                .hasPower(2)
                .hasToughness(4);
    }

    @Test
    @DisplayName("Glacial Fortress — land")
    void glacialFortress() {
        assertThat(game.createPermanent("Glacial Fortress")).isLand();
    }

    @Test
    @DisplayName("Glorious Charge — instant")
    void gloriousCharge() {
        assertThat(game.addToHand("Glorious Charge")).isInstant();
    }

    @Test
    @DisplayName("Goblin Artillery — 1/3 Goblin Warrior")
    void goblinArtillery() {
        assertThat(game.createPermanent("Goblin Artillery"))
                .isCreature()
                .hasPower(1)
                .hasToughness(3);
    }

    @Test
    @DisplayName("Goblin Chieftain — 2/2 Goblin lord")
    void goblinChieftain() {
        assertThat(game.createPermanent("Goblin Chieftain"))
                .isCreature()
                .hasPower(2)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Goblin Piker — 2/1 vanilla Goblin Warrior")
    void goblinPiker() {
        assertThat(game.createPermanent("Goblin Piker"))
                .isCreature()
                .hasPower(2)
                .hasToughness(1);
    }

    @Test
    @DisplayName("Gorgon Flail — equipment artifact")
    void gorgonFlail() {
        assertThat(game.createPermanent("Gorgon Flail")).isArtifact();
    }

    @Test
    @DisplayName("Gravedigger — 2/2 Zombie")
    void gravedigger() {
        assertThat(game.createPermanent("Gravedigger")).isCreature().hasPower(2).hasToughness(2);
    }

    @Test
    @DisplayName("Great Sable Stag — 3/3 Elk")
    void greatSableStag() {
        assertThat(game.createPermanent("Great Sable Stag"))
                .isCreature()
                .hasPower(3)
                .hasToughness(3);
    }

    @Test
    @DisplayName("Griffin Sentinel — 1/3 Griffin")
    void griffinSentinel() {
        assertThat(game.createPermanent("Griffin Sentinel"))
                .isCreature()
                .hasPower(1)
                .hasToughness(3);
    }

    @Test
    @DisplayName("Guardian Seraph — 3/4 Angel")
    void guardianSeraph() {
        assertThat(game.createPermanent("Guardian Seraph"))
                .isCreature()
                .hasPower(3)
                .hasToughness(4);
    }

    @Test
    @DisplayName("Harm's Way — instant")
    void harmsWay() {
        assertThat(game.addToHand("Harm's Way")).isInstant();
    }

    @Test
    @DisplayName("Haunting Echoes — sorcery")
    void hauntingEchoes() {
        assertThat(game.addToHand("Haunting Echoes")).isSorcery();
    }

    @Test
    @DisplayName("Hive Mind — enchantment")
    void hiveMind() {
        assertThat(game.createPermanent("Hive Mind")).isEnchantment();
    }

    @Test
    @DisplayName("Holy Strength — aura enchantment")
    void holyStrength() {
        assertThat(game.addToHand("Holy Strength")).isEnchantment();
    }

    @Test
    @DisplayName("Honor of the Pure — enchantment")
    void honorOfThePure() {
        assertThat(game.createPermanent("Honor of the Pure")).isEnchantment();
    }

    @Test
    @DisplayName("Horned Turtle — 1/4 vanilla creature")
    void hornedTurtle() {
        assertThat(game.createPermanent("Horned Turtle"))
                .isCreature()
                .hasPower(1)
                .hasToughness(4);
    }

    @Test
    @DisplayName("Howl of the Night Pack — sorcery")
    void howlOfTheNightPack() {
        assertThat(game.addToHand("Howl of the Night Pack")).isSorcery();
    }

    @Test
    @DisplayName("Howling Banshee — 3/3 Spirit")
    void howlingBanshee() {
        assertThat(game.createPermanent("Howling Banshee"))
                .isCreature()
                .hasPower(3)
                .hasToughness(3);
    }

    @Test
    @DisplayName("Howling Mine — artifact")
    void howlingMine() {
        assertThat(game.createPermanent("Howling Mine")).isArtifact();
    }

    @Test
    @DisplayName("Hypnotic Specter — 2/2 Specter")
    void hypnoticSpecter() {
        assertThat(game.createPermanent("Hypnotic Specter"))
                .isCreature()
                .hasPower(2)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Ice Cage — aura enchantment")
    void iceCage() {
        assertThat(game.addToHand("Ice Cage")).isEnchantment();
    }

    @Test
    @DisplayName("Ignite Disorder — instant")
    void igniteDisorder() {
        assertThat(game.addToHand("Ignite Disorder")).isInstant();
    }

    @Test
    @DisplayName("Illusionary Servant — 3/4 Illusion")
    void illusionaryServant() {
        assertThat(game.createPermanent("Illusionary Servant"))
                .isCreature()
                .hasPower(3)
                .hasToughness(4);
    }

    @Test
    @DisplayName("Indestructibility — aura enchantment")
    void indestructibility() {
        assertThat(game.addToHand("Indestructibility")).isEnchantment();
    }

    @Test
    @DisplayName("Inferno Elemental — 4/4 Elemental")
    void infernoElemental() {
        assertThat(game.createPermanent("Inferno Elemental"))
                .isCreature()
                .hasPower(4)
                .hasToughness(4);
    }

    @Test
    @DisplayName("Island — basic land")
    void island() {
        assertThat(game.createPermanent("Island")).isLand().isBasic().hasSubtype(BasicLandType.ISLAND);
    }

    @Test
    @DisplayName("Jace Beleren — legendary planeswalker")
    void jaceBeleren() {
        assertThat(game.createPermanent("Jace Beleren")).isPlaneswalker().isLegendary();
    }

    @Test
    @DisplayName("Jackal Familiar — 2/2 Jackal")
    void jackalFamiliar() {
        assertThat(game.createPermanent("Jackal Familiar"))
                .isCreature()
                .hasPower(2)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Jump — instant")
    void jump() {
        assertThat(game.addToHand("Jump")).isInstant();
    }

    @Test
    @DisplayName("Kalonian Behemoth — 9/9 Beast with shroud")
    void kalonianBehemoth() {
        assertThat(game.createPermanent("Kalonian Behemoth"))
                .isCreature()
                .hasPower(9)
                .hasToughness(9);
    }

    @Test
    @DisplayName("Kelinore Bat — 2/1 Bat")
    void kelinoreBat() {
        assertThat(game.createPermanent("Kelinore Bat"))
                .isCreature()
                .hasPower(2)
                .hasToughness(1);
    }

    @Test
    @DisplayName("Kindled Fury — instant")
    void kindledFury() {
        assertThat(game.addToHand("Kindled Fury")).isInstant();
    }

    @Test
    @DisplayName("Kraken's Eye — artifact")
    void krakensEye() {
        assertThat(game.createPermanent("Kraken's Eye")).isArtifact();
    }

    @Test
    @DisplayName("Lava Axe — sorcery")
    void lavaAxe() {
        assertThat(game.addToHand("Lava Axe")).isSorcery();
    }

    @Test
    @DisplayName("Levitation — enchantment")
    void levitation() {
        assertThat(game.createPermanent("Levitation")).isEnchantment();
    }

    @Test
    @DisplayName("Lifelink — aura enchantment")
    void lifelinkCard() {
        assertThat(game.addToHand("Lifelink")).isEnchantment();
    }

    @Test
    @DisplayName("Lightning Bolt — deals 3 damage to target player")
    void lightningBolt() {
        // Put Lightning Bolt in player1's hand
        var bolt = game.addToHand("Lightning Bolt");
        assertThat(bolt).isInstant();

        var mountain = game.createPermanent("Mountain");

        // Advance to main phase so we can cast
        game.advanceTo(Phase.MAIN);

        // Add red mana to player1's pool
        game.player1().manaPool().add(Mana.of(ManaType.RED, mountain));

        // Build targeting context: "any target" → player2
        var subject = ReferenceParser.ANY_TARGET_SUBJECT;
        var context = new SpellContext(new TargetChoices(List.of(new TargetChoice(subject, game.player2()))));

        // Cast Lightning Bolt targeting player2
        var result = game.castSpell(bolt, game.player1(), context);
        Assertions.assertThat(result).isInstanceOf(ExecutionResult.Success.class);

        // Mana should be spent
        Assertions.assertThat(game.player1().manaPool().isEmpty()).isTrue();

        // Bolt should be on the stack, not in hand
        Assertions.assertThat(game.stack().isEmpty()).isFalse();

        // Resolve the stack — bolt deals 3 damage to player2
        game.stack().resolve();

        // Player2 should have lost 3 life (20 → 17)
        Assertions.assertThat(game.player2().lifeTotal()).isEqualTo(17);
    }

    @Test
    @DisplayName("Lightning Elemental — 4/1 Elemental with haste")
    void lightningElemental() {
        assertThat(game.createPermanent("Lightning Elemental"))
                .isCreature()
                .hasPower(4)
                .hasToughness(1);
    }

    @Test
    @DisplayName("Lightwielder Paladin — 4/4 Human Knight")
    void lightwielderPaladin() {
        assertThat(game.createPermanent("Lightwielder Paladin"))
                .isCreature()
                .hasPower(4)
                .hasToughness(4);
    }

    @Test
    @DisplayName("Liliana Vess — legendary planeswalker")
    void lilianaVess() {
        assertThat(game.createPermanent("Liliana Vess")).isPlaneswalker().isLegendary();
    }

    @Test
    @DisplayName("Llanowar Elves — 1/1 Elf Druid")
    void llanowarElves() {
        assertThat(game.createPermanent("Llanowar Elves"))
                .isCreature()
                .hasPower(1)
                .hasToughness(1);
    }

    @Test
    @DisplayName("Looming Shade — 1/1 Shade")
    void loomingShade() {
        assertThat(game.createPermanent("Looming Shade"))
                .isCreature()
                .hasPower(1)
                .hasToughness(1);
    }

    @Test
    @DisplayName("Lurking Predators — enchantment")
    void lurkingPredators() {
        assertThat(game.createPermanent("Lurking Predators")).isEnchantment();
    }

    @Test
    @DisplayName("Magebane Armor — equipment artifact")
    void magebaneArmor() {
        assertThat(game.createPermanent("Magebane Armor")).isArtifact();
    }

    @Test
    @DisplayName("Magma Phoenix — 3/3 Phoenix")
    void magmaPhoenix() {
        assertThat(game.createPermanent("Magma Phoenix"))
                .isCreature()
                .hasPower(3)
                .hasToughness(3);
    }

    @Test
    @DisplayName("Manabarbs — enchantment")
    void manabarbs() {
        assertThat(game.createPermanent("Manabarbs")).isEnchantment();
    }

    @Test
    @DisplayName("Master of the Wild Hunt — 3/3 Human Shaman")
    void masterOfTheWildHunt() {
        assertThat(game.createPermanent("Master of the Wild Hunt"))
                .isCreature()
                .hasPower(3)
                .hasToughness(3);
    }

    @Test
    @DisplayName("Megrim — enchantment")
    void megrim() {
        assertThat(game.createPermanent("Megrim")).isEnchantment();
    }

    @Test
    @DisplayName("Merfolk Looter — 1/1 Merfolk Rogue")
    void merfolkLooter() {
        assertThat(game.createPermanent("Merfolk Looter"))
                .isCreature()
                .hasPower(1)
                .hasToughness(1);
    }

    @Test
    @DisplayName("Merfolk Sovereign — 2/2 Merfolk Noble")
    void merfolkSovereign() {
        assertThat(game.createPermanent("Merfolk Sovereign"))
                .isCreature()
                .hasPower(2)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Mesa Enchantress — 0/2 Human Druid")
    void mesaEnchantress() {
        assertThat(game.createPermanent("Mesa Enchantress"))
                .isCreature()
                .hasPower(0)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Might of Oaks — instant")
    void mightOfOaks() {
        assertThat(game.addToHand("Might of Oaks")).isInstant();
    }

    @Test
    @DisplayName("Mind Control — aura enchantment")
    void mindControl() {
        assertThat(game.addToHand("Mind Control")).isEnchantment();
    }

    @Test
    @DisplayName("Mind Rot — sorcery")
    void mindRot() {
        assertThat(game.addToHand("Mind Rot")).isSorcery();
    }

    @Test
    @DisplayName("Mind Shatter — sorcery")
    void mindShatter() {
        assertThat(game.addToHand("Mind Shatter")).isSorcery();
    }

    @Test
    @DisplayName("Mind Spring — sorcery")
    void mindSpring() {
        assertThat(game.addToHand("Mind Spring")).isSorcery();
    }

    @Test
    @DisplayName("Mirror of Fate — artifact")
    void mirrorOfFate() {
        assertThat(game.createPermanent("Mirror of Fate")).isArtifact();
    }

    @Test
    @DisplayName("Mist Leopard — 3/2 Cat with shroud")
    void mistLeopard() {
        assertThat(game.createPermanent("Mist Leopard"))
                .isCreature()
                .hasPower(3)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Mold Adder — 1/1 Fungus Snake")
    void moldAdder() {
        assertThat(game.createPermanent("Mold Adder")).isCreature().hasPower(1).hasToughness(1);
    }

    @Test
    @DisplayName("Mountain — basic land")
    void mountain() {
        assertThat(game.createPermanent("Mountain")).isLand().isBasic().hasSubtype(BasicLandType.MOUNTAIN);
    }

    @Test
    @DisplayName("Naturalize — instant")
    void naturalize() {
        assertThat(game.addToHand("Naturalize")).isInstant();
    }

    @Test
    @DisplayName("Nature's Spiral — sorcery")
    void naturesSpiral() {
        assertThat(game.addToHand("Nature's Spiral")).isSorcery();
    }

    @Test
    @DisplayName("Negate — instant")
    void negate() {
        assertThat(game.addToHand("Negate")).isInstant();
    }

    @Test
    @DisplayName("Nightmare — creature with variable P/T")
    void nightmare() {
        assertThat(game.createPermanent("Nightmare")).isCreature();
    }

    @Test
    @DisplayName("Oakenform — aura enchantment")
    void oakenform() {
        assertThat(game.addToHand("Oakenform")).isEnchantment();
    }

    @Test
    @DisplayName("Open the Vaults — sorcery")
    void openTheVaults() {
        assertThat(game.addToHand("Open the Vaults")).isSorcery();
    }

    @Test
    @DisplayName("Ornithopter — 0/2 artifact creature")
    void ornithopter() {
        var permanent = game.createPermanent("Ornithopter");
        assertThat(permanent).isArtifact().isCreature().hasPower(0).hasToughness(2);
    }

    @Test
    @DisplayName("Overrun — sorcery")
    void overrun() {
        assertThat(game.addToHand("Overrun")).isSorcery();
    }

    @Test
    @DisplayName("Pacifism — aura enchantment")
    void pacifism() {
        assertThat(game.addToHand("Pacifism")).isEnchantment();
    }

    @Test
    @DisplayName("Palace Guard — 1/4 Human Soldier")
    void palaceGuard() {
        assertThat(game.createPermanent("Palace Guard"))
                .isCreature()
                .hasPower(1)
                .hasToughness(4);
    }

    @Test
    @DisplayName("Panic Attack — sorcery")
    void panicAttack() {
        assertThat(game.addToHand("Panic Attack")).isSorcery();
    }

    @Test
    @DisplayName("Phantom Warrior — 2/2 unblockable Illusion Warrior")
    void phantomWarrior() {
        assertThat(game.createPermanent("Phantom Warrior"))
                .isCreature()
                .hasPower(2)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Pithing Needle — artifact")
    void pithingNeedle() {
        assertThat(game.createPermanent("Pithing Needle")).isArtifact();
    }

    @Test
    @DisplayName("Plains — basic land")
    void plains() {
        assertThat(game.createPermanent("Plains")).isLand().isBasic().hasSubtype(BasicLandType.PLAINS);
    }

    @Test
    @DisplayName("Planar Cleansing — sorcery")
    void planarCleansing() {
        assertThat(game.addToHand("Planar Cleansing")).isSorcery();
    }

    @Test
    @DisplayName("Platinum Angel — 4/4 artifact creature")
    void platinumAngel() {
        assertThat(game.createPermanent("Platinum Angel"))
                .isArtifact()
                .isCreature()
                .hasPower(4)
                .hasToughness(4);
    }

    @Test
    @DisplayName("Polymorph — sorcery")
    void polymorph() {
        assertThat(game.addToHand("Polymorph")).isSorcery();
    }

    @Test
    @DisplayName("Ponder — sorcery")
    void ponder() {
        assertThat(game.addToHand("Ponder")).isSorcery();
    }

    @Test
    @DisplayName("Prized Unicorn — 2/2 Unicorn")
    void prizedUnicorn() {
        assertThat(game.createPermanent("Prized Unicorn"))
                .isCreature()
                .hasPower(2)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Prodigal Pyromancer — 1/1 Human Wizard")
    void prodigalPyromancer() {
        assertThat(game.createPermanent("Prodigal Pyromancer"))
                .isCreature()
                .hasPower(1)
                .hasToughness(1);
    }

    @Test
    @DisplayName("Protean Hydra — 0/0 Hydra with X counters")
    void proteanHydra() {
        assertThat(game.createPermanent("Protean Hydra")).isCreature();
    }

    @Test
    @DisplayName("Pyroclasm — sorcery")
    void pyroclasm() {
        assertThat(game.addToHand("Pyroclasm")).isSorcery();
    }

    @Test
    @DisplayName("Raging Goblin — 1/1 Goblin with haste")
    void ragingGoblin() {
        assertThat(game.createPermanent("Raging Goblin"))
                .isCreature()
                .hasPower(1)
                .hasToughness(1);
    }

    @Test
    @DisplayName("Rampant Growth — sorcery")
    void rampantGrowth() {
        assertThat(game.addToHand("Rampant Growth")).isSorcery();
    }

    @Test
    @DisplayName("Razorfoot Griffin — 2/2 Griffin")
    void razorfootGriffin() {
        assertThat(game.createPermanent("Razorfoot Griffin"))
                .isCreature()
                .hasPower(2)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Regenerate — instant")
    void regenerate() {
        assertThat(game.addToHand("Regenerate")).isInstant();
    }

    @Test
    @DisplayName("Relentless Rats — 2/2 Rat")
    void relentlessRats() {
        assertThat(game.createPermanent("Relentless Rats"))
                .isCreature()
                .hasPower(2)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Rhox Pikemaster — 3/3 Rhino Soldier")
    void rhoxPikemaster() {
        assertThat(game.createPermanent("Rhox Pikemaster"))
                .isCreature()
                .hasPower(3)
                .hasToughness(3);
    }

    @Test
    @DisplayName("Righteousness — instant")
    void righteousness() {
        assertThat(game.addToHand("Righteousness")).isInstant();
    }

    @Test
    @DisplayName("Rise from the Grave — sorcery")
    void riseFromTheGrave() {
        assertThat(game.addToHand("Rise from the Grave")).isSorcery();
    }

    @Test
    @DisplayName("Rod of Ruin — artifact")
    void rodOfRuin() {
        assertThat(game.createPermanent("Rod of Ruin")).isArtifact();
    }

    @Test
    @DisplayName("Rootbound Crag — land")
    void rootboundCrag() {
        assertThat(game.createPermanent("Rootbound Crag")).isLand();
    }

    @Test
    @DisplayName("Royal Assassin — 1/1 Human Assassin")
    void royalAssassin() {
        assertThat(game.createPermanent("Royal Assassin"))
                .isCreature()
                .hasPower(1)
                .hasToughness(1);
    }

    @Test
    @DisplayName("Runeclaw Bear — 2/2 vanilla Bear")
    void runeclawBear() {
        assertThat(game.createPermanent("Runeclaw Bear"))
                .isCreature()
                .hasPower(2)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Safe Passage — instant")
    void safePassage() {
        assertThat(game.addToHand("Safe Passage")).isInstant();
    }

    @Test
    @DisplayName("Sage Owl — 1/1 Bird")
    void sageOwl() {
        assertThat(game.createPermanent("Sage Owl")).isCreature().hasPower(1).hasToughness(1);
    }

    @Test
    @DisplayName("Sanguine Bond — enchantment")
    void sanguineBond() {
        assertThat(game.createPermanent("Sanguine Bond")).isEnchantment();
    }

    @Test
    @DisplayName("Seismic Strike — instant")
    void seismicStrike() {
        assertThat(game.addToHand("Seismic Strike")).isInstant();
    }

    @Test
    @DisplayName("Serpent of the Endless Sea — creature with variable P/T")
    void serpentOfTheEndlessSea() {
        assertThat(game.createPermanent("Serpent of the Endless Sea")).isCreature();
    }

    @Test
    @DisplayName("Serra Angel — 4/4 Angel")
    void serraAngel() {
        assertThat(game.createPermanent("Serra Angel")).isCreature().hasPower(4).hasToughness(4);
    }

    @Test
    @DisplayName("Shatter — instant")
    void shatter() {
        assertThat(game.addToHand("Shatter")).isInstant();
    }

    @Test
    @DisplayName("Shivan Dragon — 5/5 Dragon")
    void shivanDragon() {
        assertThat(game.createPermanent("Shivan Dragon"))
                .isCreature()
                .hasPower(5)
                .hasToughness(5);
    }

    @Test
    @DisplayName("Siege Mastodon — 3/5 vanilla Elephant")
    void siegeMastodon() {
        assertThat(game.createPermanent("Siege Mastodon"))
                .isCreature()
                .hasPower(3)
                .hasToughness(5);
    }

    @Test
    @DisplayName("Siege-Gang Commander — 2/2 Goblin")
    void siegeGangCommander() {
        assertThat(game.createPermanent("Siege-Gang Commander"))
                .isCreature()
                .hasPower(2)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Sign in Blood — sorcery")
    void signInBlood() {
        assertThat(game.addToHand("Sign in Blood")).isSorcery();
    }

    @Test
    @DisplayName("Silence — instant")
    void silence() {
        assertThat(game.addToHand("Silence")).isInstant();
    }

    @Test
    @DisplayName("Silvercoat Lion — 2/2 vanilla Cat")
    void silvercoatLion() {
        assertThat(game.createPermanent("Silvercoat Lion"))
                .isCreature()
                .hasPower(2)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Sleep — sorcery")
    void sleepCard() {
        assertThat(game.addToHand("Sleep")).isSorcery();
    }

    @Test
    @DisplayName("Snapping Drake — 3/2 Drake")
    void snappingDrake() {
        assertThat(game.createPermanent("Snapping Drake"))
                .isCreature()
                .hasPower(3)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Solemn Offering — sorcery")
    void solemnOffering() {
        assertThat(game.addToHand("Solemn Offering")).isSorcery();
    }

    @Test
    @DisplayName("Soul Bleed — aura enchantment")
    void soulBleed() {
        assertThat(game.addToHand("Soul Bleed")).isEnchantment();
    }

    @Test
    @DisplayName("Soul Warden — 1/1 Human Cleric")
    void soulWarden() {
        assertThat(game.createPermanent("Soul Warden")).isCreature().hasPower(1).hasToughness(1);
    }

    @Test
    @DisplayName("Sparkmage Apprentice — 1/1 Human Wizard")
    void sparkmageApprentice() {
        assertThat(game.createPermanent("Sparkmage Apprentice"))
                .isCreature()
                .hasPower(1)
                .hasToughness(1);
    }

    @Test
    @DisplayName("Spellbook — artifact")
    void spellbook() {
        assertThat(game.createPermanent("Spellbook")).isArtifact();
    }

    @Test
    @DisplayName("Sphinx Ambassador — 5/5 Sphinx")
    void sphinxAmbassador() {
        assertThat(game.createPermanent("Sphinx Ambassador"))
                .isCreature()
                .hasPower(5)
                .hasToughness(5);
    }

    @Test
    @DisplayName("Stampeding Rhino — 4/4 Rhino with trample")
    void stampedingRhino() {
        assertThat(game.createPermanent("Stampeding Rhino"))
                .isCreature()
                .hasPower(4)
                .hasToughness(4);
    }

    @Test
    @DisplayName("Stone Giant — 3/4 Giant")
    void stoneGiant() {
        assertThat(game.createPermanent("Stone Giant")).isCreature().hasPower(3).hasToughness(4);
    }

    @Test
    @DisplayName("Stormfront Pegasus — 2/1 Pegasus")
    void stormfrontPegasus() {
        assertThat(game.createPermanent("Stormfront Pegasus"))
                .isCreature()
                .hasPower(2)
                .hasToughness(1);
    }

    @Test
    @DisplayName("Sunpetal Grove — land")
    void sunpetalGrove() {
        assertThat(game.createPermanent("Sunpetal Grove")).isLand();
    }

    @Test
    @DisplayName("Swamp — basic land")
    void swamp() {
        assertThat(game.createPermanent("Swamp")).isLand().isBasic().hasSubtype(BasicLandType.SWAMP);
    }

    @Test
    @DisplayName("Telepathy — enchantment")
    void telepathy() {
        assertThat(game.createPermanent("Telepathy")).isEnchantment();
    }

    @Test
    @DisplayName("Tempest of Light — instant")
    void tempestOfLight() {
        assertThat(game.addToHand("Tempest of Light")).isInstant();
    }

    @Test
    @DisplayName("Tendrils of Corruption — instant")
    void tendrilsOfCorruption() {
        assertThat(game.addToHand("Tendrils of Corruption")).isInstant();
    }

    @Test
    @DisplayName("Terramorphic Expanse — land")
    void terramorphicExpanse() {
        assertThat(game.createPermanent("Terramorphic Expanse")).isLand();
    }

    @Test
    @DisplayName("Time Warp — sorcery")
    void timeWarp() {
        assertThat(game.addToHand("Time Warp")).isSorcery();
    }

    @Test
    @DisplayName("Tome Scour — sorcery")
    void tomeScour() {
        assertThat(game.addToHand("Tome Scour")).isSorcery();
    }

    @Test
    @DisplayName("Traumatize — sorcery")
    void traumatize() {
        assertThat(game.addToHand("Traumatize")).isSorcery();
    }

    @Test
    @DisplayName("Trumpet Blast — instant")
    void trumpetBlast() {
        assertThat(game.addToHand("Trumpet Blast")).isInstant();
    }

    @Test
    @DisplayName("Twincast — instant")
    void twincast() {
        assertThat(game.addToHand("Twincast")).isInstant();
    }

    @Test
    @DisplayName("Undead Slayer — 2/2 Human Cleric")
    void undeadSlayer() {
        assertThat(game.createPermanent("Undead Slayer"))
                .isCreature()
                .hasPower(2)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Underworld Dreams — enchantment")
    void underworldDreams() {
        assertThat(game.createPermanent("Underworld Dreams")).isEnchantment();
    }

    @Test
    @DisplayName("Unholy Strength — aura enchantment")
    void unholyStrength() {
        assertThat(game.addToHand("Unholy Strength")).isEnchantment();
    }

    @Test
    @DisplayName("Unsummon — instant")
    void unsummon() {
        assertThat(game.addToHand("Unsummon")).isInstant();
    }

    @Test
    @DisplayName("Vampire Aristocrat — 2/2 Vampire Rogue Noble")
    void vampireAristocrat() {
        assertThat(game.createPermanent("Vampire Aristocrat"))
                .isCreature()
                .hasPower(2)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Vampire Nocturnus — 3/3 Vampire")
    void vampireNocturnus() {
        assertThat(game.createPermanent("Vampire Nocturnus"))
                .isCreature()
                .hasPower(3)
                .hasToughness(3);
    }

    @Test
    @DisplayName("Veteran Armorsmith — 2/3 Human Soldier")
    void veteranArmorsmith() {
        assertThat(game.createPermanent("Veteran Armorsmith"))
                .isCreature()
                .hasPower(2)
                .hasToughness(3);
    }

    @Test
    @DisplayName("Veteran Swordsmith — 3/2 Human Soldier")
    void veteranSwordsmith() {
        assertThat(game.createPermanent("Veteran Swordsmith"))
                .isCreature()
                .hasPower(3)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Viashino Spearhunter — 2/1 Lizard Warrior")
    void viashinoSpearhunter() {
        assertThat(game.createPermanent("Viashino Spearhunter"))
                .isCreature()
                .hasPower(2)
                .hasToughness(1);
    }

    @Test
    @DisplayName("Wall of Bone — 1/4 Skeleton Wall")
    void wallOfBone() {
        assertThat(game.createPermanent("Wall of Bone"))
                .isCreature()
                .hasPower(1)
                .hasToughness(4);
    }

    @Test
    @DisplayName("Wall of Faith — 0/5 Wall with defender")
    void wallOfFaith() {
        assertThat(game.createPermanent("Wall of Faith"))
                .isCreature()
                .hasPower(0)
                .hasToughness(5);
    }

    @Test
    @DisplayName("Wall of Fire — 0/5 Wall with defender")
    void wallOfFire() {
        assertThat(game.createPermanent("Wall of Fire"))
                .isCreature()
                .hasPower(0)
                .hasToughness(5);
    }

    @Test
    @DisplayName("Wall of Frost — 0/7 Wall with defender")
    void wallOfFrost() {
        assertThat(game.createPermanent("Wall of Frost"))
                .isCreature()
                .hasPower(0)
                .hasToughness(7);
    }

    @Test
    @DisplayName("Warp World — sorcery")
    void warpWorld() {
        assertThat(game.addToHand("Warp World")).isSorcery();
    }

    @Test
    @DisplayName("Warpath Ghoul — 3/2 vanilla Zombie")
    void warpathGhoul() {
        assertThat(game.createPermanent("Warpath Ghoul"))
                .isCreature()
                .hasPower(3)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Weakness — aura enchantment")
    void weakness() {
        assertThat(game.addToHand("Weakness")).isEnchantment();
    }

    @Test
    @DisplayName("Whispersilk Cloak — equipment artifact")
    void whispersilkCloak() {
        assertThat(game.createPermanent("Whispersilk Cloak")).isArtifact();
    }

    @Test
    @DisplayName("White Knight — 2/2 Human Knight")
    void whiteKnight() {
        assertThat(game.createPermanent("White Knight"))
                .isCreature()
                .hasPower(2)
                .hasToughness(2);
    }

    @Test
    @DisplayName("Wind Drake — 2/2 Drake")
    void windDrake() {
        assertThat(game.createPermanent("Wind Drake")).isCreature().hasPower(2).hasToughness(2);
    }

    @Test
    @DisplayName("Windstorm — instant")
    void windstorm() {
        assertThat(game.addToHand("Windstorm")).isInstant();
    }

    @Test
    @DisplayName("Wurm's Tooth — artifact")
    void wurmsTooth() {
        assertThat(game.createPermanent("Wurm's Tooth")).isArtifact();
    }

    @Test
    @DisplayName("Xathrid Demon — 7/7 Demon")
    void xathridDemon() {
        assertThat(game.createPermanent("Xathrid Demon"))
                .isCreature()
                .hasPower(7)
                .hasToughness(7);
    }

    @Test
    @DisplayName("Yawning Fissure — sorcery")
    void yawningFissure() {
        assertThat(game.addToHand("Yawning Fissure")).isSorcery();
    }

    @Test
    @DisplayName("Zephyr Sprite — 1/1 Faerie")
    void zephyrSprite() {
        assertThat(game.createPermanent("Zephyr Sprite"))
                .isCreature()
                .hasPower(1)
                .hasToughness(1);
    }
}
