// =================================================================
// Formal Grammar for Magic: The Gathering Oracle Text
//
// ANTLR4 combined grammar (lexer + parser).
// Case-insensitive lexing: tokens match regardless of case.
//
// Generated parser lives in: be.imgn.mtg.engine.oracle
// =================================================================

grammar OracleText;

options {
    caseInsensitive = true;
}

// ═══════════════════════════════════════════════════════════════════
// §1  DOCUMENT STRUCTURE
// Each paragraph is a separate ability (Rule 113.2c).
// ═══════════════════════════════════════════════════════════════════

oracleText
    : paragraph (NEWLINE paragraph)* EOF
    ;

paragraph
    : modalAbility
    | triggeredAbility
    | activatedAbility
    | chapterLine
    | castingModifier
    | REMINDER_TEXT
    | effectBlock
    | keywordLine
    ;

// ═══════════════════════════════════════════════════════════════════
// §2  KEYWORD ABILITIES (Rule 702)
// ═══════════════════════════════════════════════════════════════════

keywordLine
    : keywordEntry (COMMA keywordEntry)*
    ;

keywordEntry
    : ENCHANT word+ REMINDER_TEXT?
    | PROTECTION FROM word+ REMINDER_TEXT?
    | HEXPROOF FROM word+ REMINDER_TEXT?
    | word+ manaCostSeq REMINDER_TEXT?
    | word+ NUMBER REMINDER_TEXT?
    | word+ REMINDER_TEXT
    | word+
    ;

// ═══════════════════════════════════════════════════════════════════
// §3  ACTIVATED ABILITIES (Rule 602)
// Format: Cost : Effect. [Activation restriction.]
// ═══════════════════════════════════════════════════════════════════

activatedAbility
    : costExpression COLON effectBlock activationRestriction?
    ;

activationRestriction
    : ACTIVATE restrictionBody PERIOD
    ;

restrictionBody
    : ONLY AS A_TOKEN word PERIOD?
    | ONLY DURING YOUR word+ PERIOD?
    | ONLY ONCE EACH TURN PERIOD?
    | ONLY IF condition PERIOD?
    ;

// ═══════════════════════════════════════════════════════════════════
// §4  TRIGGERED ABILITIES (Rule 603)
// ═══════════════════════════════════════════════════════════════════

triggeredAbility
    : triggerClause COMMA interveningIf? effectBlock
    ;

triggerClause
    : WHEN triggerEvent
    | WHENEVER triggerEvent triggerRestriction?
    | AT THE BEGINNING OF phaseReference
    ;

interveningIf
    : IF condition COMMA
    ;

triggerRestriction
    : FOR THE FIRST word EACH TURN
    ;

triggerEvent
    : subject eventVerb
    | playerRef playerTriggerVerb
    ;

eventVerb
    : ENTERS (THE BATTLEFIELD)? (UNDER possessive CONTROL)?
    | DIES
    | LEAVES THE BATTLEFIELD
    | ATTACKS (ALONE)? subject?
    | BLOCKS subject?
    | BECOMES THE TARGET OF selector
    | DEALS COMBAT? DAMAGE (TO subject)?
    | IS PUT INTO zone FROM zone
    ;

playerTriggerVerb
    : (CAST | CASTS) selector
    | (GAIN | GAINS) LIFE
    | (DRAW | DRAWS) A_TOKEN CARD
    | (LOSE | LOSES) LIFE
    | (SACRIFICE | SACRIFICES) selector
    | (DISCARD | DISCARDS) selector
    ;

phaseReference
    : possessive? phaseOrStep (ON possessive TURN)?
    ;

phaseOrStep
    : UPKEEP
    | END STEP
    | BEGINNING OF COMBAT
    | COMBAT
    | word+ STEP
    | word+ PHASE
    | THE NEXT END STEP
    ;

// ═══════════════════════════════════════════════════════════════════
// §5  STATIC ABILITIES (Rule 604)
// ═══════════════════════════════════════════════════════════════════

staticAbility
    : selector (HAS | HAVE | GAINS | GAIN) keywordOrAbilityList duration? PERIOD
    | selector (GETS | GET) ptModifier
        (AND (GAINS | GAIN) keywordOrAbilityList)? duration? PERIOD
    | selector (IS | ARE) qualityList
        (IN ADDITION TO possessive OTHER word+)? PERIOD
    | (selfReference POSSESSIVE | possessive) characteristic
        (IS | ARE) EACH? EQUAL TO valueExpression PERIOD
    | subject CANT restrictedAction PERIOD
    | AS LONG AS condition COMMA subject
        (HAS | HAVE | GETS | GET | GAINS | GAIN)
        (keywordOrAbilityList | ptModifier) PERIOD
    | selector COST manaCostSeq (MORE_KW | LESS) TO CAST PERIOD
    | ENCHANTED cardType (HAS | HAVE | GETS | GET | GAINS | GAIN)
        (keywordOrAbilityList | ptModifier) duration? PERIOD
    | word+ CREATURE (HAS | HAVE | GETS | GET | GAINS | GAIN)
        (keywordOrAbilityList | ptModifier) duration? PERIOD
    | selfReference ENTERS (THE BATTLEFIELD)?
        (TAPPED | WITH amount counterType counterNoun ON IT) PERIOD
    | AS selfReference ENTERS (THE BATTLEFIELD)? COMMA effectBlock
    ;

restrictedAction
    : ATTACK (OR BLOCK)? ALONE?
    | BLOCK
    | BE THE TARGET OF selector
    | BE word+
    ;

// ═══════════════════════════════════════════════════════════════════
// §6  SPELL ABILITIES (Rule 113.3a)
// ═══════════════════════════════════════════════════════════════════

spellAbility
    : effectBlock
    ;

// ═══════════════════════════════════════════════════════════════════
// §7  MODAL ABILITIES (Rule 700.2)
// ═══════════════════════════════════════════════════════════════════

modalAbility
    : CHOOSE modalQuantity modalConstraint? EMDASH
      NEWLINE modeLine (NEWLINE modeLine)*
    ;

modalQuantity
    : ONE | TWO | THREE
    | ONE OR BOTH
    | ONE OR MORE_KW
    | ANY NUMBER
    | UP TO wordNumber
    | word
    ;

modalConstraint
    : PERIOD YOU MAY CHOOSE THE SAME word+ MORE_KW THAN ONCE PERIOD
    ;

modeLine
    : BULLET (modeCost EMDASH)? effectBlock
    ;

modeCost
    : manaCostSeq
    | PLUS manaCostSeq
    ;

// ═══════════════════════════════════════════════════════════════════
// §8  CHAPTER ABILITIES (Sagas)
// ═══════════════════════════════════════════════════════════════════

chapterLine
    : romanNumerals EMDASH effectBlock
    ;

romanNumerals
    : word (COMMA word)*
    ;

// ═══════════════════════════════════════════════════════════════════
// §9  CASTING MODIFIERS
// ═══════════════════════════════════════════════════════════════════

castingModifier
    : AS AN ADDITIONAL COST TO CAST THIS SPELL COMMA costAction PERIOD
    | YOU MAY costAction (RATHER THAN PAY possessive MANA COST)? PERIOD
    | YOU MAY CAST selfReference WITHOUT PAYING possessive MANA COST
        (IF condition)? PERIOD
    | (THIS SPELL | selfReference) COSTS manaCostSeq (LESS | MORE_KW) TO CAST
        (FOR EACH selector zoneClause?)? PERIOD
    ;

// ═══════════════════════════════════════════════════════════════════
// §10  EFFECT BLOCK
// Ordered sequence of effect sentences (Rule 608.2c).
// ═══════════════════════════════════════════════════════════════════

effectBlock
    : effectSentence+
    ;

effectSentence
    : simpleEffect PERIOD
    | conditionalSentence PERIOD
    | THEN simpleEffect PERIOD
    | forEachSentence PERIOD
    | reflexiveTrigger PERIOD
    | delayedTrigger PERIOD
    ;

conditionalSentence
    : IF condition COMMA simpleEffect (PERIOD OTHERWISE COMMA simpleEffect)?
    | playerRef MAY simpleEffect
        (PERIOD IF (pronoun | YOU) (DO | DONT) COMMA simpleEffect)?
    | simpleEffect UNLESS playerRef costAction
    ;

forEachSentence
    : FOR EACH selector zoneClause? COMMA simpleEffect
    ;

reflexiveTrigger
    : WHEN (YOU DO | subject eventVerb) COMMA simpleEffect
    ;

delayedTrigger
    : AT THE BEGINNING OF phaseReference COMMA simpleEffect
    ;

// ═══════════════════════════════════════════════════════════════════
// §11  SIMPLE EFFECTS
// ═══════════════════════════════════════════════════════════════════

simpleEffect
    : destructionEffect
    | exileEffect
    | sacrificeEffect
    | bounceEffect
    | damageEffect
    | lifeEffect
    | drawEffect
    | discardEffect
    | millEffect
    | scryEffect
    | searchEffect
    | shuffleEffect
    | revealEffect
    | tapUntapEffect
    | counterManipulationEffect
    | abilityGrantEffect
    | ptModificationEffect
    | controlChangeEffect
    | tokenCreationEffect
    | counterspellEffect
    | fightEffect
    | manaEffect
    | zoneMoveEffect
    | transformEffect
    | copyEffect
    | replacementEffect
    | preventionEffect
    | winLossEffect
    | setCharacteristicEffect
    | simpleEffect AND simpleEffect
    | simpleEffect COMMA THEN simpleEffect
    ;

// --- Removal ---

destructionEffect
    : DESTROY subject
    ;

exileEffect
    : EXILE subject zoneSource? (UNTIL subject eventVerb)?
    ;

sacrificeEffect
    : playerRef (SACRIFICE | SACRIFICES) selector
    | SACRIFICE selector
    ;

bounceEffect
    : RETURN subject TO zoneDestination
    ;

// --- Damage ---

damageEffect
    : subject DEALS amount NONCOMBAT? DAMAGE TO subject
    | DEAL amount DAMAGE TO subject
    | subject DEALS amount DAMAGE DIVIDED AS YOU CHOOSE AMONG subject
    | subject DEALS DAMAGE EQUAL TO valueExpression TO subject
    ;

// --- Life ---

lifeEffect
    : playerRef (GAINS | GAIN | LOSES | LOSE) amount LIFE
    | playerRef POSSESSIVE LIFE TOTAL BECOMES amount
    ;

// --- Card Manipulation ---

drawEffect
    : playerRef (DRAWS | DRAW) amount (CARD | CARDS)
    ;

discardEffect
    : playerRef (DISCARDS | DISCARD)
        (amount (CARD | CARDS) | selector | possessive HAND)
    ;

millEffect
    : playerRef (MILLS | MILL) amount (CARD | CARDS)
    | MILL amount
    ;

scryEffect
    : (SCRY | SURVEIL) amount
    ;

searchEffect
    : SEARCH possessive LIBRARY FOR selector
        (COMMA searchDestination)? (PERIOD? THEN? SHUFFLE)?
    ;

searchDestination
    : PUT pronoun zoneDestination
    | REVEAL pronoun COMMA PUT pronoun zoneDestination
    ;

shuffleEffect
    : SHUFFLE possessive? LIBRARY?
    ;

revealEffect
    : REVEAL subject
    | LOOK AT THE TOP amount (CARD | CARDS) OF possessive LIBRARY
    ;

// --- Tap/Untap ---

tapUntapEffect
    : TAP_WORD subject
    | UNTAP_WORD subject
    | subject (DOESNT | DONT) UNTAP_WORD DURING possessive NEXT UNTAP_WORD STEP
    ;

// --- Counters ---

counterManipulationEffect
    : PUT amount counterType counterNoun ON subject
    | REMOVE amount counterType counterNoun FROM subject
    | MOVE amount counterType counterNoun FROM subject TO subject
    | playerRef (GETS | GET) amount playerCounter counterNoun
    ;

counterNoun : COUNTER | COUNTERS ;

// --- Ability Modification ---

abilityGrantEffect
    : subject (GAINS | GAIN | HAS | HAVE) keywordOrAbilityList duration?
    | subject (LOSES | LOSE) (keywordOrAbilityList | ALL ABILITIES) duration?
    ;

ptModificationEffect
    : subject (GETS | GET) ptModifier
        (AND (GAINS | GAIN) keywordOrAbilityList)? duration?
    | subject HAS BASE POWER AND TOUGHNESS ptValue duration?
    ;

// --- Control ---

controlChangeEffect
    : playerRef (GAINS | GAIN) CONTROL OF subject duration?
    | EXCHANGE CONTROL OF subject AND subject
    ;

// --- Tokens ---

tokenCreationEffect
    : CREATE amount tokenDescription entersState?
    ;

tokenDescription
    : predefinedToken (TOKEN | TOKENS)?
    | customToken
    ;

customToken
    : (word COMMA)? ptValue colorExpr supertype* cardType+ subtype*
        (TOKEN | TOKENS) (WITH abilityList)? (NAMED word+)?
    ;

predefinedToken
    : TREASURE | FOOD | GOLD | CLUE | BLOOD | word
    ;

entersState
    : THATS (TAPPED | TAPPED AND ATTACKING | word+)
    ;

// --- Counterspell ---

counterspellEffect
    : COUNTER subject (UNLESS playerRef costAction)?
    ;

// --- Combat ---

fightEffect
    : subject (FIGHTS | FIGHT) subject
    | subject DEALS DAMAGE EQUAL TO possessive POWER TO subject
    ;

// --- Mana ---

manaEffect
    : ADD MANA_SYMBOL+
    | ADD amount MANA (OF ANY ONE? COLOR | OF word+ | IN ANY COMBINATION OF word+)
    ;

// --- Zone Movement ---

zoneMoveEffect
    : PUT subject zoneDestination
    ;

// --- Transform ---

transformEffect
    : TRANSFORM subject
    ;

// --- Copy ---

copyEffect
    : COPY subject
    | subject (BECOMES | BECOME) A_TOKEN COPY OF subject duration?
        (EXCEPT exceptClause)?
    ;

exceptClause
    : IT (HAS keywordOrAbilityList | IS qualityList
        | HAS BASE POWER AND TOUGHNESS ptValue)
        (AND exceptClause)?
    ;

// --- Replacement (Rule 614) ---

replacementEffect
    : IF subject WOULD eventDescription COMMA
        simpleEffect INSTEAD
    ;

// --- Prevention (Rule 615) ---

preventionEffect
    : PREVENT (ALL | THE NEXT amount) COMBAT? DAMAGE
        (THAT WOULD BE DEALT (TO subject)? (THIS TURN)?)?
    ;

// --- Win/Loss ---

winLossEffect
    : playerRef (WINS | LOSES) THE GAME
    ;

// --- Characteristic Setting ---

setCharacteristicEffect
    : subject (BECOMES | BECOME) qualityList
        (IN ADDITION TO possessive OTHER TYPES)? duration?
    | subject (BECOMES | BECOME) ptValue colorExpr cardType+
        (WITH abilityList)? duration?
    ;

// ═══════════════════════════════════════════════════════════════════
// §12  SUBJECT — what an effect operates on
// ═══════════════════════════════════════════════════════════════════

subject
    : selector
    | pronoun
    | demonstrative
    | ANY TARGET
    | playerRef
    | selfReference
    | possessiveSubject
    ;

pronoun
    : IT | THEM | THEY
    ;

demonstrative
    : (THAT | THOSE | THE) typeExpression
    ;

possessiveSubject
    : (ITS | THEIR) (CONTROLLER | OWNER)
    ;

selfReference
    : TILDE
    | THIS (cardType | gameObjectType)
    ;

// ═══════════════════════════════════════════════════════════════════
// §13  PLAYER REFERENCES
// ═══════════════════════════════════════════════════════════════════

playerRef
    : YOU
    | (A_TOKEN | EACH | ANY | TARGET) PLAYER
    | (A_TOKEN | AN | EACH | TARGET) OPPONENT
    | THAT PLAYER
    | DEFENDING PLAYER
    | THEY
    | possessive (CONTROLLER | OWNER)
    ;

// ═══════════════════════════════════════════════════════════════════
// §14  SELECTOR — composable object description
//
// Pattern: [quantifier] [qualifier]* type [with]* [zone] [controller]
// ═══════════════════════════════════════════════════════════════════

selector
    : quantifier? qualifier* typeExpression
        withClause* zoneClause? controllerClause?
    ;

// --- Quantifier ---

quantifier
    : A_TOKEN | AN
    | ALL | EACH | EVERY
    | ANOTHER | OTHER
    | wordNumber | NUMBER
    | UP TO (wordNumber | NUMBER)
    | ANY NUMBER OF
    | EXACTLY (wordNumber | NUMBER)
    | THE
    | X_VAR
    ;

// --- Qualifier ---

qualifier
    : TARGET
    | color
    | negatedColor
    | supertype
    | negatedSupertype
    | negatedCardType
    | status
    | combatStatus
    | OTHER
    | EQUIPPED | ENCHANTED
    | HISTORIC
    | TOKEN | NONTOKEN
    ;

color       : WHITE | BLUE | BLACK | RED | GREEN | COLORLESS | MULTICOLORED ;
negatedColor : NONWHITE | NONBLUE | NONBLACK | NONRED | NONGREEN ;
supertype   : LEGENDARY | BASIC | SNOW ;
negatedSupertype : NONLEGENDARY | NONBASIC ;
negatedCardType  : NONCREATURE | NONARTIFACT | NONENCHANTMENT | NONLAND
                 | NONPLANESWALKER | NONTOKEN ;
status      : TAPPED | UNTAPPED | FACE_DOWN | FACE_UP | TRANSFORMED ;
combatStatus : ATTACKING | BLOCKING | BLOCKED | UNBLOCKED ;

// --- Type Expression ---

typeExpression
    : singleType OR singleType
    | singleType COMMA singleType (COMMA singleType)* COMMA OR singleType
    | singleType singleType singleType?
    | singleType
    ;

singleType
    : gameObjectType
    | cardType
    | subtype
    | gameObjectType cardType
    ;

gameObjectType : PERMANENT | SPELL | CARD | TOKEN | SOURCE | ABILITY ;
cardType    : CREATURE | ARTIFACT | ENCHANTMENT | LAND | PLANESWALKER
            | BATTLE | INSTANT | SORCERY | KINDRED ;
subtype     : word ;

// --- With-Clause ---

withClause
    : (WITH | WITHOUT) withPredicate
    ;

withPredicate
    : MANA VALUE comparisonExpr
    | POWER comparisonExpr
    | TOUGHNESS comparisonExpr
    | word+
    | (A_TOKEN | AN) counterType COUNTER ON (IT | THEM)
    ;

comparisonExpr
    : amount (OR (LESS | GREATER | MORE_KW))?
    ;

// --- Zone Clause ---

zoneClause
    : ON THE BATTLEFIELD
    | ON THE STACK
    | IN zone
    | FROM zone
    ;

// --- Controller Clause ---

controllerClause
    : YOU CONTROL
    | YOU DONT CONTROL
    | YOU OWN
    | (A_TOKEN | AN) OPPONENT CONTROLS
    | EACH OPPONENT CONTROLS
    | TARGET PLAYER CONTROLS
    | THAT PLAYER CONTROLS
    | DEFENDING PLAYER CONTROLS
    ;

// ═══════════════════════════════════════════════════════════════════
// §15  COSTS (Rule 118)
// ═══════════════════════════════════════════════════════════════════

costExpression
    : costComponent (COMMA costComponent)*
    ;

costComponent
    : manaCostSeq
    | TAP_SYMBOL
    | UNTAP_SYMBOL
    | LOYALTY_COST
    | PAY amount LIFE
    | SACRIFICE selector
    | DISCARD (selector | A_TOKEN CARD | amount (CARD | CARDS) | YOUR HAND)
    | TAP_WORD selector
    | UNTAP_WORD selector
    | EXILE selector zoneSource?
    | REMOVE amount counterType counterNoun FROM subject
    | RETURN selector TO zoneDestination
    | PUT selector zoneDestination
    | REVEAL selector FROM possessive HAND
    | PAY manaCostSeq
    ;

costAction
    : DISCARD (selector | A_TOKEN CARD | amount (CARD | CARDS))
    | SACRIFICE selector
    | PAY (amount LIFE | manaCostSeq)
    | EXILE selector zoneSource?
    | REMOVE amount counterType counterNoun FROM subject
    | RETURN selector TO zoneDestination
    | TAP_WORD selector
    ;

// ═══════════════════════════════════════════════════════════════════
// §16  MANA
// ═══════════════════════════════════════════════════════════════════

manaCostSeq
    : MANA_SYMBOL+
    ;

// ═══════════════════════════════════════════════════════════════════
// §17  AMOUNTS & VALUES
// ═══════════════════════════════════════════════════════════════════

amount
    : NUMBER
    | wordNumber
    | X_VAR
    | THAT (MUCH | MANY)
    | formula
    ;

wordNumber
    : ONE | TWO | THREE | FOUR | FIVE | SIX | SEVEN | EIGHT | NINE | TEN
    | ELEVEN | TWELVE | THIRTEEN | FOURTEEN | FIFTEEN
    | TWENTY
    ;

formula
    : EQUAL TO valueExpression
    ;

valueExpression
    : THE NUMBER OF selector zoneClause?
    | THE NUMBER OF counterType COUNTERS ON subject
    | possessive characteristic
    | THE (GREATEST | HIGHEST | LOWEST | TOTAL)
        characteristic AMONG selector
    | THE AMOUNT OF (LIFE playerRef word+ THIS TURN
                   | DAMAGE DEALT TO subject THIS TURN)
    | valueExpression (PLUS | MINUS) (valueExpression | NUMBER)
    ;

characteristic
    : POWER | TOUGHNESS | MANA VALUE | LIFE TOTAL | word
    ;

// ═══════════════════════════════════════════════════════════════════
// §18  DURATIONS (Rule 611.2)
// ═══════════════════════════════════════════════════════════════════

duration
    : UNTIL END OF TURN
    | UNTIL YOUR NEXT TURN
    | UNTIL END OF COMBAT
    | UNTIL subject (LEAVES THE BATTLEFIELD | DIES)
    | FOR AS LONG AS condition
    | THIS TURN
    ;

// ═══════════════════════════════════════════════════════════════════
// §19  CONDITIONS
// ═══════════════════════════════════════════════════════════════════

condition
    : subject (IS | ARE) qualityList
    | subject (HAS | HAVE)
        (counterType counterNoun ON pronoun | word+)
    | playerRef (CONTROLS | CONTROL) NO? selector
    | playerRef (HAS | HAVE) amount OR (MORE_KW | FEWER)
        (CARDS IN possessive zoneName | LIFE | selector)
    | playerRef word+ (LIFE | DAMAGE | amount OR MORE_KW LIFE) THIS TURN
    | IT POSSESSIVE YOUR TURN
    | selector (DIED | word+) THIS TURN
    | THERE (IS | ARE) selector zoneClause
    | subject word+ THE BATTLEFIELD THIS TURN
    | condition AND condition
    | condition OR condition
    ;

// ═══════════════════════════════════════════════════════════════════
// §20  ZONES (Rule 400)
// ═══════════════════════════════════════════════════════════════════

zone
    : THE BATTLEFIELD
    | EXILE
    | possessive zoneName
    | THE zoneName
    ;

zoneName
    : GRAVEYARD | LIBRARY | HAND | EXILE | BATTLEFIELD
    | COMMAND word
    | STACK
    ;

zoneSource
    : FROM zone
    | FROM AMONG (CARDS zoneClause | THEM)
    ;

zoneDestination
    : ONTO THE BATTLEFIELD TAPPED? (UNDER possessive CONTROL)?
    | ON (TOP | BOTTOM) OF possessive LIBRARY
    | INTO (possessive zoneName | EXILE)
    | TO (ITS OWNER POSSESSIVE HAND | possessive HAND | zone)
    ;

// ═══════════════════════════════════════════════════════════════════
// §21  TOKEN & P/T COMPONENTS
// ═══════════════════════════════════════════════════════════════════

ptValue
    : NUMBER SLASH NUMBER
    | STAR SLASH STAR
    | X_VAR SLASH X_VAR
    ;

ptModifier
    : sign NUMBER SLASH sign NUMBER
    | sign X_VAR SLASH sign X_VAR
    ;

sign : PLUS | MINUS ;

colorExpr
    : color
    | color AND color
    | color COMMA color COMMA AND color
    | COLORLESS
    ;

abilityList
    : keywordOrAbility ((COMMA | AND) keywordOrAbility)*
    ;

keywordOrAbility
    : word+
    | DOUBLE_QUOTE word+ DOUBLE_QUOTE
    ;

keywordOrAbilityList
    : keywordOrAbility ((COMMA | AND) keywordOrAbility)*
    ;

qualityList
    : quality (AND quality)*
    ;

quality
    : color | supertype | cardType | status
    | INDESTRUCTIBLE | HEXPROOF
    | MONOCOLORED | MULTICOLORED | COLORLESS
    ;

// ═══════════════════════════════════════════════════════════════════
// §22  COUNTER TYPES
// ═══════════════════════════════════════════════════════════════════

counterType
    : PLUS NUMBER SLASH PLUS NUMBER
    | MINUS NUMBER SLASH MINUS NUMBER
    | word
    ;

playerCounter
    : word
    ;

// ═══════════════════════════════════════════════════════════════════
// §23  EVENT DESCRIPTIONS (for replacement effects)
// ═══════════════════════════════════════════════════════════════════

eventDescription
    : DIE | BE DESTROYED
    | DEAL COMBAT? DAMAGE (TO subject)?
    | BE DEALT DAMAGE
    | LOSE LIFE | GAIN LIFE
    | DRAW A_TOKEN CARD
    | ENTER THE BATTLEFIELD TAPPED?
    | LEAVE THE BATTLEFIELD
    | BE PUT INTO zone FROM zone
    | BE word+
    ;

// ═══════════════════════════════════════════════════════════════════
// §24  SHARED NON-TERMINALS
// ═══════════════════════════════════════════════════════════════════

possessive
    : YOUR | ITS | THEIR
    | EACH (PLAYER | OPPONENT) POSSESSIVE
    | (THAT | TARGET | DEFENDING | ACTIVE) (PLAYER | OPPONENT) POSSESSIVE
    | selfReference POSSESSIVE
    ;

enchantTarget
    : cardType | PLAYER | qualifier* typeExpression | PERMANENT
    ;

protectionQuality
    : color (AND FROM? color)*
    | ALL COLORS
    | EVERYTHING
    | cardType (AND FROM? cardType)*
    | word+
    ;

// ═══════════════════════════════════════════════════════════════════
// §25  WORD — matches any single word token
// Used in positions where any word is acceptable (creature types,
// keyword names, card names, etc.).
// ═══════════════════════════════════════════════════════════════════

word
    : WORD
    // Trigger / conditional
    | WHEN | WHENEVER | IF | UNLESS | THEN | OTHERWISE | INSTEAD
    // Structural
    | AS | FOR | UNTIL | AT | BEGINNING | DURING | ONLY | ONCE
    // Modal
    | CHOOSE
    // Articles / quantifiers
    | A_TOKEN | AN | ALL | EACH | EVERY | ANOTHER | OTHER | THE
    | UP | ANY | EXACTLY | NO
    // Connectors
    | AND | OR | BOTH
    // Pronouns
    | YOU | YOUR | IT | ITS | THEM | THEIR | THEY
    | THAT | THOSE | THIS | HIS | HER
    // Player
    | PLAYER | OPPONENT | OPPONENTS | CONTROLLER | OWNER | DEFENDING | ACTIVE
    // Verbs
    | DESTROY | EXILE | SACRIFICE | SACRIFICES | RETURN | CREATE
    | COUNTER | COUNTERS | FIGHT | FIGHTS | ADD | PUT | REMOVE | MOVE
    | EXCHANGE | COPY | TRANSFORM | PREVENT | SEARCH | SHUFFLE | REVEAL
    | DEAL | DEALS | GAIN | GAINS | LOSE | LOSES
    | DRAW | DRAWS | DISCARD | DISCARDS | TAP_WORD | UNTAP_WORD
    | MILL | MILLS | SCRY | SURVEIL | PAY
    | HAS | HAVE | GETS | GET | IS | ARE | BECOMES | BECOME
    | MAY | WOULD | CAST | CASTS | DIES | ENTERS | LEAVES
    | ATTACKS | BLOCKS | CONTROL | CONTROLS | COST | COSTS | ACTIVATE
    | WINS | LOSES | DO | DONT | DOESNT | CANT | BE | BEEN
    // Types
    | CREATURE | ARTIFACT | ENCHANTMENT | LAND | PLANESWALKER
    | BATTLE | INSTANT | SORCERY | PERMANENT | SPELL | CARD | CARDS
    | TOKEN | TOKENS | SOURCE | ABILITY | ABILITIES | KINDRED
    // Supertypes
    | LEGENDARY | BASIC | SNOW
    // Negated types
    | NONCREATURE | NONARTIFACT | NONENCHANTMENT | NONLAND
    | NONPLANESWALKER | NONTOKEN | NONLEGENDARY | NONBASIC
    // Colors
    | WHITE | BLUE | BLACK | RED | GREEN | COLORLESS | MULTICOLORED | MONOCOLORED
    // Negated colors
    | NONWHITE | NONBLUE | NONBLACK | NONRED | NONGREEN
    // Statuses
    | TAPPED | UNTAPPED | TRANSFORMED | FACE_DOWN | FACE_UP
    | ATTACKING | BLOCKING | BLOCKED | UNBLOCKED
    // Keywords (common)
    | ENCHANT | PROTECTION | HEXPROOF | INDESTRUCTIBLE | FLYING
    | DOUBLE | FIRST | STRIKE
    // Game terms
    | LIFE | DAMAGE | COMBAT | NONCOMBAT | POWER | TOUGHNESS | MANA | VALUE
    | COLOR | TURN | STEP | PHASE | UPKEEP | TARGET | NAMED
    | BATTLEFIELD | GRAVEYARD | LIBRARY | HAND | EXILE | STACK | COMMAND
    // Comparison / misc
    | EQUAL | MORE_KW | LESS | GREATER | FEWER | TOTAL | BASE
    | NEXT | ADDITIONAL | RATHER | LONG | AMOUNT | MUCH | MANY
    | NUMBER_WORD | SAME | DIFFERENT | ALSO | STILL
    | DIED | DEALT | DIVIDED | NONCOMBAT
    // Prepositions
    | WITH | WITHOUT | FROM | TO | INTO | ONTO | ON | IN | OF | UNDER | AMONG
    // Zone terms
    | TOP | BOTTOM
    // Token names
    | TREASURE | FOOD | GOLD | CLUE | BLOOD
    // Misc
    | HISTORIC | EQUIPPED | THATS | EXCEPT | ALONE
    | RATHER | PAYING | OWN | WHERE | ABLE
    // Word numbers
    | ONE | TWO | THREE | FOUR | FIVE | SIX | SEVEN | EIGHT | NINE | TEN
    | ELEVEN | TWELVE | THIRTEEN | FOURTEEN | FIFTEEN | TWENTY
    // Misc
    | X_VAR | LOOK | ENTER | LEAVE | DIE
    ;


// ═══════════════════════════════════════════════════════════════════
//
// LEXER RULES
//
// With caseInsensitive=true, all character matching is
// case-insensitive. Token precedence: longest match first,
// then definition order. Keywords must precede WORD.
//
// ═══════════════════════════════════════════════════════════════════

// --- Special tokens ---

TAP_SYMBOL    : '{t}' ;
UNTAP_SYMBOL  : '{q}' ;
MANA_SYMBOL   : '{' ~[{}]+ '}' ;
LOYALTY_COST  : '[' [+\-\u2212]? [0-9x]+ ']' ;
REMINDER_TEXT : '(' ~[)\n\r]* ')' ;

// --- Punctuation ---

PERIOD        : '.' ;
COMMA         : ',' ;
COLON         : ':' ;
EMDASH        : '\u2014' | '--' ;
BULLET        : '\u2022' ;
SLASH         : '/' ;
PLUS          : '+' ;
MINUS         : '-' | '\u2212' ;
STAR          : '*' ;
DOUBLE_QUOTE  : '"' ;
POSSESSIVE    : '\'s' ;
TILDE         : '~' ;

// --- Trigger / Conditional ---

WHEN          : 'when' ;
WHENEVER      : 'whenever' ;
IF            : 'if' ;
UNLESS        : 'unless' ;
THEN          : 'then' ;
OTHERWISE     : 'otherwise' ;
INSTEAD       : 'instead' ;

// --- Structural ---

AS            : 'as' ;
FOR           : 'for' ;
UNTIL         : 'until' ;
AT            : 'at' ;
BEGINNING     : 'beginning' ;
DURING        : 'during' ;
ONLY          : 'only' ;
ONCE          : 'once' ;

// --- Modal ---

CHOOSE        : 'choose' ;

// --- Articles / Quantifiers ---

ANOTHER       : 'another' ;
EXACTLY       : 'exactly' ;
EVERY         : 'every' ;
OTHER         : 'other' ;
EACH          : 'each' ;
ALL           : 'all' ;
ANY           : 'any' ;
THE           : 'the' ;
AN            : 'an' ;
UP            : 'up' ;
NO            : 'no' ;
A_TOKEN       : 'a' ;

// --- Connectors ---

AND           : 'and' ;
OR            : 'or' ;
BOTH          : 'both' ;

// --- Pronouns ---

THOSE         : 'those' ;
THEIR         : 'their' ;
THESE         : 'these' ;
THEM          : 'them' ;
THEY          : 'they' ;
THIS          : 'this' ;
THAT          : 'that' ;
YOUR          : 'your' ;
YOU           : 'you' ;
HIS           : 'his' ;
HER           : 'her' ;
ITS           : 'its' ;
IT            : 'it' ;

// --- Player ---

OPPONENTS     : 'opponents' ;
OPPONENT      : 'opponent' ;
CONTROLLER    : 'controller' ;
DEFENDING     : 'defending' ;
ACTIVE        : 'active' ;
PLAYER        : 'player' ;
OWNER         : 'owner' ;

// --- Verbs (effect) ---

SACRIFICES    : 'sacrifices' ;
SACRIFICE     : 'sacrifice' ;
TRANSFORM     : 'transform' ;
DESTROYED     : 'destroyed' ;
EXCHANGE      : 'exchange' ;
DISCARDS      : 'discards' ;
DISCARD       : 'discard' ;
PREVENT       : 'prevent' ;
SHUFFLE       : 'shuffle' ;
DESTROY       : 'destroy' ;
COUNTER       : 'counter' ;
COUNTERS      : 'counters' ;
DIVIDED       : 'divided' ;
REVEAL        : 'reveal' ;
SEARCH        : 'search' ;
RETURN        : 'return' ;
SURVEIL       : 'surveil' ;
FIGHTS        : 'fights' ;
CREATE        : 'create' ;
REMOVE        : 'remove' ;
EXILE         : 'exile' ;
FIGHT         : 'fight' ;
MILLS         : 'mills' ;
SCRY          : 'scry' ;
COPY          : 'copy' ;
MILL          : 'mill' ;
MOVE          : 'move' ;
LOOK          : 'look' ;
ADD           : 'add' ;
PUT           : 'put' ;

// --- Verbs (state) ---

BECOMES       : 'becomes' ;
BECOME        : 'become' ;
DOESNT        : 'doesn\'t' ;
DONT          : 'don\'t' ;
CANT          : 'can\'t' ;
CONTROLS      : 'controls' ;
CONTROL       : 'control' ;
ENTERS        : 'enters' ;
LEAVES        : 'leaves' ;
ATTACKS       : 'attacks' ;
BLOCKS        : 'blocks' ;
DRAWS         : 'draws' ;
GAINS         : 'gains' ;
LOSES         : 'loses' ;
COSTS         : 'costs' ;
CASTS         : 'casts' ;
DEALS         : 'deals' ;
WOULD         : 'would' ;
GETS          : 'gets' ;
WINS          : 'wins' ;
BEEN          : 'been' ;
CAST          : 'cast' ;
DEAL          : 'deal' ;
DIES          : 'dies' ;
DIED          : 'died' ;
DRAW          : 'draw' ;
GAIN          : 'gain' ;
LOSE          : 'lose' ;
HAVE          : 'have' ;
COST          : 'cost' ;
HAS           : 'has' ;
GET           : 'get' ;
MAY           : 'may' ;
ARE           : 'are' ;
PAY           : 'pay' ;
IS            : 'is' ;
DO            : 'do' ;
BE            : 'be' ;

// --- Verb aliases for tap/untap (distinct from TAP_SYMBOL) ---

ACTIVATE      : 'activate' ;
UNTAP_WORD    : 'untap' ;
TAP_WORD      : 'tap' ;

// --- Types ---

PLANESWALKER  : 'planeswalker' ;
ENCHANTMENT   : 'enchantment' ;
CREATURE      : 'creature' ;
ARTIFACT      : 'artifact' ;
ABILITIES     : 'abilities' ;
PERMANENT     : 'permanent' ;
SORCERY       : 'sorcery' ;
ABILITY       : 'ability' ;
INSTANT       : 'instant' ;
KINDRED       : 'kindred' ;
BATTLE        : 'battle' ;
SOURCE        : 'source' ;
TOKENS        : 'tokens' ;
SPELL         : 'spell' ;
TOKEN         : 'token' ;
CARDS         : 'cards' ;
LAND          : 'land' ;
CARD          : 'card' ;

// --- Supertypes ---

LEGENDARY     : 'legendary' ;
BASIC         : 'basic' ;
SNOW          : 'snow' ;

// --- Negated types ---

NONPLANESWALKER : 'nonplaneswalker' ;
NONENCHANTMENT  : 'nonenchantment' ;
NONCREATURE     : 'noncreature' ;
NONARTIFACT     : 'nonartifact' ;
NONLEGENDARY    : 'nonlegendary' ;
NONTOKEN        : 'nontoken' ;
NONBASIC        : 'nonbasic' ;
NONLAND         : 'nonland' ;

// --- Colors ---

MULTICOLORED  : 'multicolored' ;
MONOCOLORED   : 'monocolored' ;
COLORLESS     : 'colorless' ;
WHITE         : 'white' ;
BLACK         : 'black' ;
GREEN         : 'green' ;
BLUE          : 'blue' ;
RED           : 'red' ;

// --- Negated colors ---

NONWHITE      : 'nonwhite' ;
NONBLACK      : 'nonblack' ;
NONGREEN      : 'nongreen' ;
NONBLUE       : 'nonblue' ;
NONRED        : 'nonred' ;

// --- Statuses ---

TRANSFORMED   : 'transformed' ;
UNBLOCKED     : 'unblocked' ;
ATTACKING     : 'attacking' ;
BLOCKING      : 'blocking' ;
EQUIPPED      : 'equipped' ;
ENCHANTED     : 'enchanted' ;
UNTAPPED      : 'untapped' ;
BLOCKED       : 'blocked' ;
FACE_DOWN     : 'face-down' ;
FACE_UP       : 'face-up' ;
TAPPED        : 'tapped' ;

// --- Common keyword ability names ---

INDESTRUCTIBLE : 'indestructible' ;
PROTECTION     : 'protection' ;
HEXPROOF       : 'hexproof' ;
HISTORIC       : 'historic' ;
ENCHANT        : 'enchant' ;
FLYING         : 'flying' ;
DOUBLE         : 'double' ;
STRIKE         : 'strike' ;
FIRST          : 'first' ;

// --- Game terms ---

COMBINATION   : 'combination' ;
NONCOMBAT     : 'noncombat' ;
TOUGHNESS     : 'toughness' ;
ADDITIONAL    : 'additional' ;
BATTLEFIELD   : 'battlefield' ;
DIFFERENT     : 'different' ;
GRAVEYARD     : 'graveyard' ;
LIBRARY       : 'library' ;
COMMAND       : 'command' ;
UPKEEP        : 'upkeep' ;
COMBAT        : 'combat' ;
DAMAGE        : 'damage' ;
PAYING        : 'paying' ;
RATHER        : 'rather' ;
AMOUNT        : 'amount' ;
EXCEPT        : 'except' ;
NAMED         : 'named' ;
POWER         : 'power' ;
PHASE         : 'phase' ;
DEALT         : 'dealt' ;
MANA          : 'mana' ;
HAND          : 'hand' ;
LIFE          : 'life' ;
STEP          : 'step' ;
TURN          : 'turn' ;
SAME          : 'same' ;
ALSO          : 'also' ;
ABLE          : 'able' ;
THAN          : 'than' ;
GAME          : 'game' ;
THATS         : 'that\'s' ;
STACK         : 'stack' ;
TOTAL         : 'total' ;
VALUE         : 'value' ;
COLOR         : 'color' ;
STILL         : 'still' ;
BASE          : 'base' ;
NEXT          : 'next' ;
LONG          : 'long' ;
LESS          : 'less' ;
MORE_KW          : 'more' ;
MUCH          : 'much' ;
MANY          : 'many' ;
EQUAL         : 'equal' ;
ALONE         : 'alone' ;
ENTER         : 'enter' ;
LEAVE         : 'leave' ;
DIE           : 'die' ;
OWN           : 'own' ;

// --- Comparison ---

GREATEST      : 'greatest' ;
HIGHEST       : 'highest' ;
GREATER       : 'greater' ;
LOWEST        : 'lowest' ;
FEWER         : 'fewer' ;

// --- Prepositions ---

WITHOUT       : 'without' ;
AMONG         : 'among' ;
UNDER         : 'under' ;
ONTO          : 'onto' ;
INTO          : 'into' ;
WITH          : 'with' ;
FROM          : 'from' ;
OF            : 'of' ;
ON            : 'on' ;
IN            : 'in' ;
TO            : 'to' ;

// --- Zone terms ---

BOTTOM        : 'bottom' ;
TOP           : 'top' ;
TARGET        : 'target' ;

// --- Predefined token names ---

TREASURE      : 'treasure' ;
BLOOD         : 'blood' ;
FOOD          : 'food' ;
GOLD          : 'gold' ;
CLUE          : 'clue' ;

// --- Word numbers ---

THIRTEEN      : 'thirteen' ;
FOURTEEN      : 'fourteen' ;
FIFTEEN       : 'fifteen' ;
ELEVEN        : 'eleven' ;
TWELVE        : 'twelve' ;
TWENTY        : 'twenty' ;
THREE         : 'three' ;
SEVEN         : 'seven' ;
EIGHT         : 'eight' ;
NINE          : 'nine' ;
FOUR          : 'four' ;
FIVE          : 'five' ;
TEN           : 'ten' ;
SIX           : 'six' ;
ONE           : 'one' ;
TWO           : 'two' ;

// --- Misc ---

NUMBER_WORD   : 'number' ;
WHERE         : 'where' ;
X_VAR         : 'x' ;
COLORS        : 'colors' ;
EVERYTHING    : 'everything' ;
TYPES         : 'types' ;
ADDITION      : 'addition' ;

// --- Number ---

NUMBER        : [0-9]+ ;

// --- Catch-all word ---

WORD          : [a-z]+ ;

// --- Whitespace ---

NEWLINE       : ('\r'? '\n')+ ;
WS            : [ \t]+ -> skip ;
