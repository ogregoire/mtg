package be.imgn.mtg.engine.characteristics;

/// Creature subtypes, called creature types ({@mtg.rule 205.3m}).
///
/// Creature types are subtypes that are correlated to the creature card type. A creature can
/// have multiple creature types (e.g., "Human Wizard"). This enum contains a subset of the most
/// commonly used creature types.
public enum CreatureType implements Subtype {
    /// The Advisor creature type.
    ADVISOR("Advisor"),
    /// The Aetherborn creature type.
    AETHERBORN("Aetherborn"),
    /// The Alien creature type.
    ALIEN("Alien"),
    /// The Ally creature type.
    ALLY("Ally"),
    /// The Angel creature type.
    ANGEL("Angel"),
    /// The Antelope creature type.
    ANTELOPE("Antelope"),
    /// The Ape creature type.
    APE("Ape"),
    /// The Archer creature type.
    ARCHER("Archer"),
    /// The Archon creature type.
    ARCHON("Archon"),
    /// The Army creature type.
    ARMY("Army"),
    /// The Artificer creature type.
    ARTIFICER("Artificer"),
    /// The Assassin creature type.
    ASSASSIN("Assassin"),
    /// The Assembly-Worker creature type.
    ASSEMBLY_WORKER("Assembly-Worker"),
    /// The Astartes creature type.
    ASTARTES("Astartes"),
    /// The Atog creature type.
    ATOG("Atog"),
    /// The Aurochs creature type.
    AUROCHS("Aurochs"),
    /// The Avatar creature type.
    AVATAR("Avatar"),
    /// The Azra creature type.
    AZRA("Azra"),
    /// The Badger creature type.
    BADGER("Badger"),
    /// The Balloon creature type.
    BALLOON("Balloon"),
    /// The Barbarian creature type.
    BARBARIAN("Barbarian"),
    /// The Bard creature type.
    BARD("Bard"),
    /// The Basilisk creature type.
    BASILISK("Basilisk"),
    /// The Bat creature type.
    BAT("Bat"),
    /// The Bear creature type.
    BEAR("Bear"),
    /// The Beast creature type.
    BEAST("Beast"),
    /// The Beeble creature type.
    BEEBLE("Beeble"),
    /// The Beholder creature type.
    BEHOLDER("Beholder"),
    /// The Berserker creature type.
    BERSERKER("Berserker"),
    /// The Bird creature type.
    BIRD("Bird"),
    /// The Blinkmoth creature type.
    BLINKMOTH("Blinkmoth"),
    /// The Boar creature type.
    BOAR("Boar"),
    /// The Bringer creature type.
    BRINGER("Bringer"),
    /// The Brushwagg creature type.
    BRUSHWAGG("Brushwagg"),
    /// The Camarid creature type.
    CAMARID("Camarid"),
    /// The Camel creature type.
    CAMEL("Camel"),
    /// The Capybara creature type.
    CAPYBARA("Capybara"),
    /// The Caribou creature type.
    CARIBOU("Caribou"),
    /// The Carrier creature type.
    CARRIER("Carrier"),
    /// The Cat creature type.
    CAT("Cat"),
    /// The Centaur creature type.
    CENTAUR("Centaur"),
    /// The Child creature type.
    CHILD("Child"),
    /// The Chimera creature type.
    CHIMERA("Chimera"),
    /// The Citizen creature type.
    CITIZEN("Citizen"),
    /// The Cleric creature type.
    CLERIC("Cleric"),
    /// The Clown creature type.
    CLOWN("Clown"),
    /// The Cockatrice creature type.
    COCKATRICE("Cockatrice"),
    /// The Construct creature type.
    CONSTRUCT("Construct"),
    /// The Coward creature type.
    COWARD("Coward"),
    /// The Crab creature type.
    CRAB("Crab"),
    /// The Crocodile creature type.
    CROCODILE("Crocodile"),
    /// The Custodes creature type.
    CUSTODES("Custodes"),
    /// The Cyberman creature type.
    CYBERMAN("Cyberman"),
    /// The Cyclops creature type.
    CYCLOPS("Cyclops"),
    /// The Dalek creature type.
    DALEK("Dalek"),
    /// The Dauthi creature type.
    DAUTHI("Dauthi"),
    /// The Demigod creature type.
    DEMIGOD("Demigod"),
    /// The Demon creature type.
    DEMON("Demon"),
    /// The Deserter creature type.
    DESERTER("Deserter"),
    /// The Detective creature type.
    DETECTIVE("Detective"),
    /// The Devil creature type.
    DEVIL("Devil"),
    /// The Dinosaur creature type.
    DINOSAUR("Dinosaur"),
    /// The Djinn creature type.
    DJINN("Djinn"),
    /// The Doctor creature type.
    DOCTOR("Doctor"),
    /// The Dog creature type.
    DOG("Dog"),
    /// The Dragon creature type.
    DRAGON("Dragon"),
    /// The Drake creature type.
    DRAKE("Drake"),
    /// The Dreadnought creature type.
    DREADNOUGHT("Dreadnought"),
    /// The Drone creature type.
    DRONE("Drone"),
    /// The Druid creature type.
    DRUID("Druid"),
    /// The Dryad creature type.
    DRYAD("Dryad"),
    /// The Dwarf creature type.
    DWARF("Dwarf"),
    /// The Efreet creature type.
    EFREET("Efreet"),
    /// The Egg creature type.
    EGG("Egg"),
    /// The Elder creature type.
    ELDER("Elder"),
    /// The Eldrazi creature type.
    ELDRAZI("Eldrazi"),
    /// The Elemental creature type.
    ELEMENTAL("Elemental"),
    /// The Elephant creature type.
    ELEPHANT("Elephant"),
    /// The Elf creature type.
    ELF("Elf"),
    /// The Elk creature type.
    ELK("Elk"),
    /// The Employee creature type.
    EMPLOYEE("Employee"),
    /// The Eye creature type.
    EYE("Eye"),
    /// The Faerie creature type.
    FAERIE("Faerie"),
    /// The Ferret creature type.
    FERRET("Ferret"),
    /// The Fish creature type.
    FISH("Fish"),
    /// The Flagbearer creature type.
    FLAGBEARER("Flagbearer"),
    /// The Fox creature type.
    FOX("Fox"),
    /// The Frog creature type.
    FROG("Frog"),
    /// The Fungus creature type.
    FUNGUS("Fungus"),
    /// The Gamer creature type.
    GAMER("Gamer"),
    /// The Gargoyle creature type.
    GARGOYLE("Gargoyle"),
    /// The Germ creature type.
    GERM("Germ"),
    /// The Giant creature type.
    GIANT("Giant"),
    /// The Gith creature type.
    GITH("Gith"),
    /// The Gnoll creature type.
    GNOLL("Gnoll"),
    /// The Gnome creature type.
    GNOME("Gnome"),
    /// The Goat creature type.
    GOAT("Goat"),
    /// The Goblin creature type.
    GOBLIN("Goblin"),
    /// The God creature type.
    GOD("God"),
    /// The Golem creature type.
    GOLEM("Golem"),
    /// The Gorgon creature type.
    GORGON("Gorgon"),
    /// The Graveborn creature type.
    GRAVEBORN("Graveborn"),
    /// The Gremlin creature type.
    GREMLIN("Gremlin"),
    /// The Griffin creature type.
    GRIFFIN("Griffin"),
    /// The Guest creature type.
    GUEST("Guest"),
    /// The Hag creature type.
    HAG("Hag"),
    /// The Halfling creature type.
    HALFLING("Halfling"),
    /// The Hamster creature type.
    HAMSTER("Hamster"),
    /// The Harpy creature type.
    HARPY("Harpy"),
    /// The Hellion creature type.
    HELLION("Hellion"),
    /// The Hippo creature type.
    HIPPO("Hippo"),
    /// The Hippogriff creature type.
    HIPPOGRIFF("Hippogriff"),
    /// The Homarid creature type.
    HOMARID("Homarid"),
    /// The Homunculus creature type.
    HOMUNCULUS("Homunculus"),
    /// The Horror creature type.
    HORROR("Horror"),
    /// The Horse creature type.
    HORSE("Horse"),
    /// The Human creature type.
    HUMAN("Human"),
    /// The Hydra creature type.
    HYDRA("Hydra"),
    /// The Hyena creature type.
    HYENA("Hyena"),
    /// The Illusion creature type.
    ILLUSION("Illusion"),
    /// The Imp creature type.
    IMP("Imp"),
    /// The Incarnation creature type.
    INCARNATION("Incarnation"),
    /// The Inkling creature type.
    INKLING("Inkling"),
    /// The Inquisitor creature type.
    INQUISITOR("Inquisitor"),
    /// The Insect creature type.
    INSECT("Insect"),
    /// The Jackal creature type.
    JACKAL("Jackal"),
    /// The Jellyfish creature type.
    JELLYFISH("Jellyfish"),
    /// The Juggernaut creature type.
    JUGGERNAUT("Juggernaut"),
    /// The Kavu creature type.
    KAVU("Kavu"),
    /// The Kirin creature type.
    KIRIN("Kirin"),
    /// The Kithkin creature type.
    KITHKIN("Kithkin"),
    /// The Knight creature type.
    KNIGHT("Knight"),
    /// The Kobold creature type.
    KOBOLD("Kobold"),
    /// The Kor creature type.
    KOR("Kor"),
    /// The Kraken creature type.
    KRAKEN("Kraken"),
    /// The Lamia creature type.
    LAMIA("Lamia"),
    /// The Lammasu creature type.
    LAMMASU("Lammasu"),
    /// The Leech creature type.
    LEECH("Leech"),
    /// The Leviathan creature type.
    LEVIATHAN("Leviathan"),
    /// The Lhurgoyf creature type.
    LHURGOYF("Lhurgoyf"),
    /// The Licid creature type.
    LICID("Licid"),
    /// The Lizard creature type.
    LIZARD("Lizard"),
    /// The Manticore creature type.
    MANTICORE("Manticore"),
    /// The Masticore creature type.
    MASTICORE("Masticore"),
    /// The Mercenary creature type.
    MERCENARY("Mercenary"),
    /// The Merfolk creature type.
    MERFOLK("Merfolk"),
    /// The Metathran creature type.
    METATHRAN("Metathran"),
    /// The Minion creature type.
    MINION("Minion"),
    /// The Minotaur creature type.
    MINOTAUR("Minotaur"),
    /// The Mite creature type.
    MITE("Mite"),
    /// The Mole creature type.
    MOLE("Mole"),
    /// The Monger creature type.
    MONGER("Monger"),
    /// The Mongoose creature type.
    MONGOOSE("Mongoose"),
    /// The Monk creature type.
    MONK("Monk"),
    /// The Monkey creature type.
    MONKEY("Monkey"),
    /// The Moonfolk creature type.
    MOONFOLK("Moonfolk"),
    /// The Mount creature type.
    MOUNT("Mount"),
    /// The Mouse creature type.
    MOUSE("Mouse"),
    /// The Mutant creature type.
    MUTANT("Mutant"),
    /// The Myr creature type.
    MYR("Myr"),
    /// The Mystic creature type.
    MYSTIC("Mystic"),
    /// The Naga creature type.
    NAGA("Naga"),
    /// The Nautilus creature type.
    NAUTILUS("Nautilus"),
    /// The Necron creature type.
    NECRON("Necron"),
    /// The Nephilim creature type.
    NEPHILIM("Nephilim"),
    /// The Nightmare creature type.
    NIGHTMARE("Nightmare"),
    /// The Nightstalker creature type.
    NIGHTSTALKER("Nightstalker"),
    /// The Ninja creature type.
    NINJA("Ninja"),
    /// The Noble creature type.
    NOBLE("Noble"),
    /// The Noggle creature type.
    NOGGLE("Noggle"),
    /// The Nomad creature type.
    NOMAD("Nomad"),
    /// The Nymph creature type.
    NYMPH("Nymph"),
    /// The Octopus creature type.
    OCTOPUS("Octopus"),
    /// The Ogre creature type.
    OGRE("Ogre"),
    /// The Ooze creature type.
    OOZE("Ooze"),
    /// The Orb creature type.
    ORB("Orb"),
    /// The Orc creature type.
    ORC("Orc"),
    /// The Orgg creature type.
    ORGG("Orgg"),
    /// The Otter creature type.
    OTTER("Otter"),
    /// The Ouphe creature type.
    OUPHE("Ouphe"),
    /// The Ox creature type.
    OX("Ox"),
    /// The Oyster creature type.
    OYSTER("Oyster"),
    /// The Pangolin creature type.
    PANGOLIN("Pangolin"),
    /// The Peasant creature type.
    PEASANT("Peasant"),
    /// The Pegasus creature type.
    PEGASUS("Pegasus"),
    /// The Pentavite creature type.
    PENTAVITE("Pentavite"),
    /// The Performer creature type.
    PERFORMER("Performer"),
    /// The Pest creature type.
    PEST("Pest"),
    /// The Phelddagrif creature type.
    PHELDDAGRIF("Phelddagrif"),
    /// The Phoenix creature type.
    PHOENIX("Phoenix"),
    /// The Phyrexian creature type.
    PHYREXIAN("Phyrexian"),
    /// The Pilot creature type.
    PILOT("Pilot"),
    /// The Pincher creature type.
    PINCHER("Pincher"),
    /// The Pirate creature type.
    PIRATE("Pirate"),
    /// The Plant creature type.
    PLANT("Plant"),
    /// The Praetor creature type.
    PRAETOR("Praetor"),
    /// The Primarch creature type.
    PRIMARCH("Primarch"),
    /// The Prism creature type.
    PRISM("Prism"),
    /// The Processor creature type.
    PROCESSOR("Processor"),
    /// The Rabbit creature type.
    RABBIT("Rabbit"),
    /// The Raccoon creature type.
    RACCOON("Raccoon"),
    /// The Ranger creature type.
    RANGER("Ranger"),
    /// The Rat creature type.
    RAT("Rat"),
    /// The Rebel creature type.
    REBEL("Rebel"),
    /// The Reflection creature type.
    REFLECTION("Reflection"),
    /// The Rhino creature type.
    RHINO("Rhino"),
    /// The Rigger creature type.
    RIGGER("Rigger"),
    /// The Robot creature type.
    ROBOT("Robot"),
    /// The Rogue creature type.
    ROGUE("Rogue"),
    /// The Sable creature type.
    SABLE("Sable"),
    /// The Salamander creature type.
    SALAMANDER("Salamander"),
    /// The Samurai creature type.
    SAMURAI("Samurai"),
    /// The Sand creature type.
    SAND("Sand"),
    /// The Saproling creature type.
    SAPROLING("Saproling"),
    /// The Satyr creature type.
    SATYR("Satyr"),
    /// The Scarecrow creature type.
    SCARECROW("Scarecrow"),
    /// The Scientist creature type.
    SCIENTIST("Scientist"),
    /// The Scion creature type.
    SCION("Scion"),
    /// The Scorpion creature type.
    SCORPION("Scorpion"),
    /// The Scout creature type.
    SCOUT("Scout"),
    /// The Sculpture creature type.
    SCULPTURE("Sculpture"),
    /// The Serf creature type.
    SERF("Serf"),
    /// The Serpent creature type.
    SERPENT("Serpent"),
    /// The Servo creature type.
    SERVO("Servo"),
    /// The Shade creature type.
    SHADE("Shade"),
    /// The Shaman creature type.
    SHAMAN("Shaman"),
    /// The Shapeshifter creature type.
    SHAPESHIFTER("Shapeshifter"),
    /// The Shark creature type.
    SHARK("Shark"),
    /// The Sheep creature type.
    SHEEP("Sheep"),
    /// The Siren creature type.
    SIREN("Siren"),
    /// The Skeleton creature type.
    SKELETON("Skeleton"),
    /// The Slith creature type.
    SLITH("Slith"),
    /// The Sliver creature type.
    SLIVER("Sliver"),
    /// The Sloth creature type.
    SLOTH("Sloth"),
    /// The Slug creature type.
    SLUG("Slug"),
    /// The Snake creature type.
    SNAKE("Snake"),
    /// The Soldier creature type.
    SOLDIER("Soldier"),
    /// The Soltari creature type.
    SOLTARI("Soltari"),
    /// The Sorcerer creature type.
    SORCERER("Sorcerer"),
    /// The Spawn creature type.
    SPAWN("Spawn"),
    /// The Specter creature type.
    SPECTER("Specter"),
    /// The Spellshaper creature type.
    SPELLSHAPER("Spellshaper"),
    /// The Sphinx creature type.
    SPHINX("Sphinx"),
    /// The Spider creature type.
    SPIDER("Spider"),
    /// The Spike creature type.
    SPIKE("Spike"),
    /// The Spirit creature type.
    SPIRIT("Spirit"),
    /// The Splinter creature type.
    SPLINTER("Splinter"),
    /// The Sponge creature type.
    SPONGE("Sponge"),
    /// The Squid creature type.
    SQUID("Squid"),
    /// The Squirrel creature type.
    SQUIRREL("Squirrel"),
    /// The Starfish creature type.
    STARFISH("Starfish"),
    /// The Surrakar creature type.
    SURRAKAR("Surrakar"),
    /// The Survivor creature type.
    SURVIVOR("Survivor"),
    /// The Tentacle creature type.
    TENTACLE("Tentacle"),
    /// The Tetravite creature type.
    TETRAVITE("Tetravite"),
    /// The Thalakos creature type.
    THALAKOS("Thalakos"),
    /// The Thopter creature type.
    THOPTER("Thopter"),
    /// The Thrull creature type.
    THRULL("Thrull"),
    /// The Tiefling creature type.
    TIEFLING("Tiefling"),
    /// The Time Lord creature type.
    TIME_LORD("Time Lord"),
    /// The Treefolk creature type.
    TREEFOLK("Treefolk"),
    /// The Trilobite creature type.
    TRILOBITE("Trilobite"),
    /// The Triskelavite creature type.
    TRISKELAVITE("Triskelavite"),
    /// The Troll creature type.
    TROLL("Troll"),
    /// The Turtle creature type.
    TURTLE("Turtle"),
    /// The Tyranid creature type.
    TYRANID("Tyranid"),
    /// The Unicorn creature type.
    UNICORN("Unicorn"),
    /// The Vampire creature type.
    VAMPIRE("Vampire"),
    /// The Vedalken creature type.
    VEDALKEN("Vedalken"),
    /// The Viashino creature type.
    VIASHINO("Viashino"),
    /// The Volver creature type.
    VOLVER("Volver"),
    /// The Wall creature type.
    WALL("Wall"),
    /// The Walrus creature type.
    WALRUS("Walrus"),
    /// The Warlock creature type.
    WARLOCK("Warlock"),
    /// The Warrior creature type.
    WARRIOR("Warrior"),
    /// The Weird creature type.
    WEIRD("Weird"),
    /// The Werewolf creature type.
    WEREWOLF("Werewolf"),
    /// The Whale creature type.
    WHALE("Whale"),
    /// The Wizard creature type.
    WIZARD("Wizard"),
    /// The Wolf creature type.
    WOLF("Wolf"),
    /// The Wolverine creature type.
    WOLVERINE("Wolverine"),
    /// The Wombat creature type.
    WOMBAT("Wombat"),
    /// The Worm creature type.
    WORM("Worm"),
    /// The Wraith creature type.
    WRAITH("Wraith"),
    /// The Wurm creature type.
    WURM("Wurm"),
    /// The Yeti creature type.
    YETI("Yeti"),
    /// The Zombie creature type.
    ZOMBIE("Zombie"),
    /// The Zubera creature type.
    ZUBERA("Zubera");

    private final String text;

    CreatureType(String text) {
        this.text = text;
    }

    @Override
    public String text() {
        return text;
    }
}
