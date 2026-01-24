package be.imgn.mtg.engine.characteristics;

/// Creature subtypes, called creature types ({@mtg.rule 205.3m}).
///
/// Creature types are subtypes that are correlated to the creature card type. A creature can
/// have multiple creature types (e.g., "Human Wizard"). This enum contains a subset of the most
/// commonly used creature types.
public enum CreatureType implements Subtype {
    /// The Advisor creature type.
    ADVISOR,
    /// The Aetherborn creature type.
    AETHERBORN,
    /// The Alien creature type.
    ALIEN,
    /// The Ally creature type.
    ALLY,
    /// The Angel creature type.
    ANGEL,
    /// The Antelope creature type.
    ANTELOPE,
    /// The Ape creature type.
    APE,
    /// The Archer creature type.
    ARCHER,
    /// The Archon creature type.
    ARCHON,
    /// The Army creature type.
    ARMY,
    /// The Artificer creature type.
    ARTIFICER,
    /// The Assassin creature type.
    ASSASSIN,
    /// The Assembly-Worker creature type.
    ASSEMBLY_WORKER,
    /// The Astartes creature type.
    ASTARTES,
    /// The Atog creature type.
    ATOG,
    /// The Aurochs creature type.
    AUROCHS,
    /// The Avatar creature type.
    AVATAR,
    /// The Azra creature type.
    AZRA,
    /// The Badger creature type.
    BADGER,
    /// The Balloon creature type.
    BALLOON,
    /// The Barbarian creature type.
    BARBARIAN,
    /// The Bard creature type.
    BARD,
    /// The Basilisk creature type.
    BASILISK,
    /// The Bat creature type.
    BAT,
    /// The Bear creature type.
    BEAR,
    /// The Beast creature type.
    BEAST,
    /// The Beeble creature type.
    BEEBLE,
    /// The Beholder creature type.
    BEHOLDER,
    /// The Berserker creature type.
    BERSERKER,
    /// The Bird creature type.
    BIRD,
    /// The Blinkmoth creature type.
    BLINKMOTH,
    /// The Boar creature type.
    BOAR,
    /// The Bringer creature type.
    BRINGER,
    /// The Brushwagg creature type.
    BRUSHWAGG,
    /// The Camarid creature type.
    CAMARID,
    /// The Camel creature type.
    CAMEL,
    /// The Capybara creature type.
    CAPYBARA,
    /// The Caribou creature type.
    CARIBOU,
    /// The Carrier creature type.
    CARRIER,
    /// The Cat creature type.
    CAT,
    /// The Centaur creature type.
    CENTAUR,
    /// The Child creature type.
    CHILD,
    /// The Chimera creature type.
    CHIMERA,
    /// The Citizen creature type.
    CITIZEN,
    /// The Cleric creature type.
    CLERIC,
    /// The Clown creature type.
    CLOWN,
    /// The Cockatrice creature type.
    COCKATRICE,
    /// The Construct creature type.
    CONSTRUCT,
    /// The Coward creature type.
    COWARD,
    /// The Crab creature type.
    CRAB,
    /// The Crocodile creature type.
    CROCODILE,
    /// The Custodes creature type.
    CUSTODES,
    /// The Cyberman creature type.
    CYBERMAN,
    /// The Cyclops creature type.
    CYCLOPS,
    /// The Dalek creature type.
    DALEK,
    /// The Dauthi creature type.
    DAUTHI,
    /// The Demigod creature type.
    DEMIGOD,
    /// The Demon creature type.
    DEMON,
    /// The Deserter creature type.
    DESERTER,
    /// The Detective creature type.
    DETECTIVE,
    /// The Devil creature type.
    DEVIL,
    /// The Dinosaur creature type.
    DINOSAUR,
    /// The Djinn creature type.
    DJINN,
    /// The Doctor creature type.
    DOCTOR,
    /// The Dog creature type.
    DOG,
    /// The Dragon creature type.
    DRAGON,
    /// The Drake creature type.
    DRAKE,
    /// The Dreadnought creature type.
    DREADNOUGHT,
    /// The Drone creature type.
    DRONE,
    /// The Druid creature type.
    DRUID,
    /// The Dryad creature type.
    DRYAD,
    /// The Dwarf creature type.
    DWARF,
    /// The Efreet creature type.
    EFREET,
    /// The Egg creature type.
    EGG,
    /// The Elder creature type.
    ELDER,
    /// The Eldrazi creature type.
    ELDRAZI,
    /// The Elemental creature type.
    ELEMENTAL,
    /// The Elephant creature type.
    ELEPHANT,
    /// The Elf creature type.
    ELF,
    /// The Elk creature type.
    ELK,
    /// The Employee creature type.
    EMPLOYEE,
    /// The Eye creature type.
    EYE,
    /// The Faerie creature type.
    FAERIE,
    /// The Ferret creature type.
    FERRET,
    /// The Fish creature type.
    FISH,
    /// The Flagbearer creature type.
    FLAGBEARER,
    /// The Fox creature type.
    FOX,
    /// The Frog creature type.
    FROG,
    /// The Fungus creature type.
    FUNGUS,
    /// The Gamer creature type.
    GAMER,
    /// The Gargoyle creature type.
    GARGOYLE,
    /// The Germ creature type.
    GERM,
    /// The Giant creature type.
    GIANT,
    /// The Gith creature type.
    GITH,
    /// The Gnoll creature type.
    GNOLL,
    /// The Gnome creature type.
    GNOME,
    /// The Goat creature type.
    GOAT,
    /// The Goblin creature type.
    GOBLIN,
    /// The God creature type.
    GOD,
    /// The Golem creature type.
    GOLEM,
    /// The Gorgon creature type.
    GORGON,
    /// The Graveborn creature type.
    GRAVEBORN,
    /// The Gremlin creature type.
    GREMLIN,
    /// The Griffin creature type.
    GRIFFIN,
    /// The Guest creature type.
    GUEST,
    /// The Hag creature type.
    HAG,
    /// The Halfling creature type.
    HALFLING,
    /// The Hamster creature type.
    HAMSTER,
    /// The Harpy creature type.
    HARPY,
    /// The Hellion creature type.
    HELLION,
    /// The Hippo creature type.
    HIPPO,
    /// The Hippogriff creature type.
    HIPPOGRIFF,
    /// The Homarid creature type.
    HOMARID,
    /// The Homunculus creature type.
    HOMUNCULUS,
    /// The Horror creature type.
    HORROR,
    /// The Horse creature type.
    HORSE,
    /// The Human creature type.
    HUMAN,
    /// The Hydra creature type.
    HYDRA,
    /// The Hyena creature type.
    HYENA,
    /// The Illusion creature type.
    ILLUSION,
    /// The Imp creature type.
    IMP,
    /// The Incarnation creature type.
    INCARNATION,
    /// The Inkling creature type.
    INKLING,
    /// The Inquisitor creature type.
    INQUISITOR,
    /// The Insect creature type.
    INSECT,
    /// The Jackal creature type.
    JACKAL,
    /// The Jellyfish creature type.
    JELLYFISH,
    /// The Juggernaut creature type.
    JUGGERNAUT,
    /// The Kavu creature type.
    KAVU,
    /// The Kirin creature type.
    KIRIN,
    /// The Kithkin creature type.
    KITHKIN,
    /// The Knight creature type.
    KNIGHT,
    /// The Kobold creature type.
    KOBOLD,
    /// The Kor creature type.
    KOR,
    /// The Kraken creature type.
    KRAKEN,
    /// The Lamia creature type.
    LAMIA,
    /// The Lammasu creature type.
    LAMMASU,
    /// The Leech creature type.
    LEECH,
    /// The Leviathan creature type.
    LEVIATHAN,
    /// The Lhurgoyf creature type.
    LHURGOYF,
    /// The Licid creature type.
    LICID,
    /// The Lizard creature type.
    LIZARD,
    /// The Manticore creature type.
    MANTICORE,
    /// The Masticore creature type.
    MASTICORE,
    /// The Mercenary creature type.
    MERCENARY,
    /// The Merfolk creature type.
    MERFOLK,
    /// The Metathran creature type.
    METATHRAN,
    /// The Minion creature type.
    MINION,
    /// The Minotaur creature type.
    MINOTAUR,
    /// The Mite creature type.
    MITE,
    /// The Mole creature type.
    MOLE,
    /// The Monger creature type.
    MONGER,
    /// The Mongoose creature type.
    MONGOOSE,
    /// The Monk creature type.
    MONK,
    /// The Monkey creature type.
    MONKEY,
    /// The Moonfolk creature type.
    MOONFOLK,
    /// The Mount creature type.
    MOUNT,
    /// The Mouse creature type.
    MOUSE,
    /// The Mutant creature type.
    MUTANT,
    /// The Myr creature type.
    MYR,
    /// The Mystic creature type.
    MYSTIC,
    /// The Naga creature type.
    NAGA,
    /// The Nautilus creature type.
    NAUTILUS,
    /// The Necron creature type.
    NECRON,
    /// The Nephilim creature type.
    NEPHILIM,
    /// The Nightmare creature type.
    NIGHTMARE,
    /// The Nightstalker creature type.
    NIGHTSTALKER,
    /// The Ninja creature type.
    NINJA,
    /// The Noble creature type.
    NOBLE,
    /// The Noggle creature type.
    NOGGLE,
    /// The Nomad creature type.
    NOMAD,
    /// The Nymph creature type.
    NYMPH,
    /// The Octopus creature type.
    OCTOPUS,
    /// The Ogre creature type.
    OGRE,
    /// The Ooze creature type.
    OOZE,
    /// The Orb creature type.
    ORB,
    /// The Orc creature type.
    ORC,
    /// The Orgg creature type.
    ORGG,
    /// The Otter creature type.
    OTTER,
    /// The Ouphe creature type.
    OUPHE,
    /// The Ox creature type.
    OX,
    /// The Oyster creature type.
    OYSTER,
    /// The Pangolin creature type.
    PANGOLIN,
    /// The Peasant creature type.
    PEASANT,
    /// The Pegasus creature type.
    PEGASUS,
    /// The Pentavite creature type.
    PENTAVITE,
    /// The Performer creature type.
    PERFORMER,
    /// The Pest creature type.
    PEST,
    /// The Phelddagrif creature type.
    PHELDDAGRIF,
    /// The Phoenix creature type.
    PHOENIX,
    /// The Phyrexian creature type.
    PHYREXIAN,
    /// The Pilot creature type.
    PILOT,
    /// The Pincher creature type.
    PINCHER,
    /// The Pirate creature type.
    PIRATE,
    /// The Plant creature type.
    PLANT,
    /// The Praetor creature type.
    PRAETOR,
    /// The Primarch creature type.
    PRIMARCH,
    /// The Prism creature type.
    PRISM,
    /// The Processor creature type.
    PROCESSOR,
    /// The Rabbit creature type.
    RABBIT,
    /// The Raccoon creature type.
    RACCOON,
    /// The Ranger creature type.
    RANGER,
    /// The Rat creature type.
    RAT,
    /// The Rebel creature type.
    REBEL,
    /// The Reflection creature type.
    REFLECTION,
    /// The Rhino creature type.
    RHINO,
    /// The Rigger creature type.
    RIGGER,
    /// The Robot creature type.
    ROBOT,
    /// The Rogue creature type.
    ROGUE,
    /// The Sable creature type.
    SABLE,
    /// The Salamander creature type.
    SALAMANDER,
    /// The Samurai creature type.
    SAMURAI,
    /// The Sand creature type.
    SAND,
    /// The Saproling creature type.
    SAPROLING,
    /// The Satyr creature type.
    SATYR,
    /// The Scarecrow creature type.
    SCARECROW,
    /// The Scientist creature type.
    SCIENTIST,
    /// The Scion creature type.
    SCION,
    /// The Scorpion creature type.
    SCORPION,
    /// The Scout creature type.
    SCOUT,
    /// The Sculpture creature type.
    SCULPTURE,
    /// The Serf creature type.
    SERF,
    /// The Serpent creature type.
    SERPENT,
    /// The Servo creature type.
    SERVO,
    /// The Shade creature type.
    SHADE,
    /// The Shaman creature type.
    SHAMAN,
    /// The Shapeshifter creature type.
    SHAPESHIFTER,
    /// The Shark creature type.
    SHARK,
    /// The Sheep creature type.
    SHEEP,
    /// The Siren creature type.
    SIREN,
    /// The Skeleton creature type.
    SKELETON,
    /// The Slith creature type.
    SLITH,
    /// The Sliver creature type.
    SLIVER,
    /// The Sloth creature type.
    SLOTH,
    /// The Slug creature type.
    SLUG,
    /// The Snake creature type.
    SNAKE,
    /// The Soldier creature type.
    SOLDIER,
    /// The Soltari creature type.
    SOLTARI,
    /// The Sorcerer creature type.
    SORCERER,
    /// The Spawn creature type.
    SPAWN,
    /// The Specter creature type.
    SPECTER,
    /// The Spellshaper creature type.
    SPELLSHAPER,
    /// The Sphinx creature type.
    SPHINX,
    /// The Spider creature type.
    SPIDER,
    /// The Spike creature type.
    SPIKE,
    /// The Spirit creature type.
    SPIRIT,
    /// The Splinter creature type.
    SPLINTER,
    /// The Sponge creature type.
    SPONGE,
    /// The Squid creature type.
    SQUID,
    /// The Squirrel creature type.
    SQUIRREL,
    /// The Starfish creature type.
    STARFISH,
    /// The Surrakar creature type.
    SURRAKAR,
    /// The Survivor creature type.
    SURVIVOR,
    /// The Tentacle creature type.
    TENTACLE,
    /// The Tetravite creature type.
    TETRAVITE,
    /// The Thalakos creature type.
    THALAKOS,
    /// The Thopter creature type.
    THOPTER,
    /// The Thrull creature type.
    THRULL,
    /// The Tiefling creature type.
    TIEFLING,
    /// The Time Lord creature type.
    TIME_LORD,
    /// The Treefolk creature type.
    TREEFOLK,
    /// The Trilobite creature type.
    TRILOBITE,
    /// The Triskelavite creature type.
    TRISKELAVITE,
    /// The Troll creature type.
    TROLL,
    /// The Turtle creature type.
    TURTLE,
    /// The Tyranid creature type.
    TYRANID,
    /// The Unicorn creature type.
    UNICORN,
    /// The Vampire creature type.
    VAMPIRE,
    /// The Vedalken creature type.
    VEDALKEN,
    /// The Viashino creature type.
    VIASHINO,
    /// The Volver creature type.
    VOLVER,
    /// The Wall creature type.
    WALL,
    /// The Walrus creature type.
    WALRUS,
    /// The Warlock creature type.
    WARLOCK,
    /// The Warrior creature type.
    WARRIOR,
    /// The Weird creature type.
    WEIRD,
    /// The Werewolf creature type.
    WEREWOLF,
    /// The Whale creature type.
    WHALE,
    /// The Wizard creature type.
    WIZARD,
    /// The Wolf creature type.
    WOLF,
    /// The Wolverine creature type.
    WOLVERINE,
    /// The Wombat creature type.
    WOMBAT,
    /// The Worm creature type.
    WORM,
    /// The Wraith creature type.
    WRAITH,
    /// The Wurm creature type.
    WURM,
    /// The Yeti creature type.
    YETI,
    /// The Zombie creature type.
    ZOMBIE,
    /// The Zubera creature type.
    ZUBERA
}
