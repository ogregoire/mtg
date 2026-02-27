package be.imgn.mtg.engine.ability.internal.parser;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.digits;
import static com.google.common.labs.parse.Parser.or;
import static com.google.common.labs.parse.Parser.quotedBy;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.labs.parse.Parser;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.ability.internal.parser.effect.CreateTokenEffect;
import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.ability.internal.parser.selector.PowerToughness;
import be.imgn.mtg.engine.ability.internal.parser.selector.PredefinedTokenType;
import be.imgn.mtg.engine.characteristics.ArtifactType;
import be.imgn.mtg.engine.characteristics.BasicLandType;
import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.Colors;
import be.imgn.mtg.engine.characteristics.CreatureType;
import be.imgn.mtg.engine.characteristics.EnchantmentType;
import be.imgn.mtg.engine.characteristics.Subtype;
import be.imgn.mtg.engine.characteristics.Subtypes;
import be.imgn.mtg.engine.characteristics.Supertype;
import be.imgn.mtg.engine.characteristics.Supertypes;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Types;

/// Parser for token creation effects in oracle text.
public final class TokenParser {

    private TokenParser() {}

    /// Parses token amount ("a" = 1, numeric).
    private static final Parser<Amount> TOKEN_AMOUNT = anyOf(
            word("a").thenReturn(new Amount.Exact(1)), word("an").thenReturn(new Amount.Exact(1)), AmountParser.AMOUNT);

    /// Parses a single P/T value (numeric or X).
    private static final Parser<Amount> PT_VALUE =
            anyOf(AmountParser.X_VALUE, digits().map(Integer::parseInt).map(Amount.Exact::new));

    /// Parses P/T like "1/1", "2/2", "X/X", etc.
    private static final Parser<PowerToughness> POWER_TOUGHNESS =
            sequence(PT_VALUE, string("/").then(PT_VALUE), PowerToughness::new);

    /// Parses a single color word.
    private static final Parser<Color> SINGLE_COLOR = anyOf(
            word("white").thenReturn(Color.WHITE),
            word("blue").thenReturn(Color.BLUE),
            word("black").thenReturn(Color.BLACK),
            word("red").thenReturn(Color.RED),
            word("green").thenReturn(Color.GREEN));

    /// Parses three colors: "black, red, and green"
    private static final Parser<Colors> THREE_COLORS = sequence(
            SINGLE_COLOR.followedBy(string(",")), SINGLE_COLOR.followedBy(string(", and")), SINGLE_COLOR, Colors::of);

    /// Parses two colors: "green and white"
    private static final Parser<Colors> TWO_COLORS =
            sequence(SINGLE_COLOR, word("and").then(SINGLE_COLOR), Colors::of);

    /// Parses color words (including "colorless") and returns Colors.
    /// Supports single color, two colors ("X and Y"), and three colors ("X, Y, and Z").
    private static final Parser<Colors> TOKEN_COLORS =
            anyOf(word("colorless").thenReturn(Colors.empty()), THREE_COLORS, TWO_COLORS, SINGLE_COLOR.map(Colors::of));

    /// All five colors (WUBRG).
    private static final Colors ALL_COLORS = Colors.of(Color.WHITE, Color.BLUE, Color.BLACK, Color.RED, Color.GREEN);

    /// Parses "that's all colors" suffix.
    private static final Parser<Colors> THATS_ALL_COLORS =
            string("that's all colors").thenReturn(ALL_COLORS);

    /// Parses a single subtype (creature type, enchantment type, land type, artifact type).
    @SuppressWarnings("unchecked")
    private static final Parser<Subtype> SINGLE_SUBTYPE = Stream.of(
                    Arrays.stream(CreatureType.values()),
                    Arrays.stream(EnchantmentType.values()),
                    Arrays.stream(BasicLandType.values()),
                    Arrays.stream(ArtifactType.values()))
            .flatMap(s -> s)
            .map(st -> OracleParser.word(st).thenReturn((Subtype) st))
            .collect(or());

    /// Parses one or more subtypes (e.g., "Soldier", "Cat Dragon", "Aura Curse", "Forest Dryad").
    private static final Parser<Subtypes> SUBTYPES =
            SINGLE_SUBTYPE.atLeastOnce().map(list -> Subtypes.of(list.toArray(Subtype[]::new)));

    /// Parses a supertype (legendary, basic, snow, world).
    private static final Parser<Supertype> SINGLE_SUPERTYPE = Arrays.stream(Supertype.values())
            .map(st -> OracleParser.word(st).thenReturn(st))
            .collect(or());

    /// Parses a token name followed by comma (e.g., "Boo,").
    private static final Parser<String> TOKEN_NAME = word().followedBy(string(","));

    /// Helper record for first part of creature token: name, amount, supertypes.
    private record TokenIdentity(@Nullable String name, Amount amount, Supertypes supertypes) {}

    /// Variant: Named with supertype - "Name, a legendary"
    private static final Parser<TokenIdentity> NAMED_LEGENDARY =
            sequence(TOKEN_NAME, TOKEN_AMOUNT, SINGLE_SUPERTYPE.map(Supertypes::of), TokenIdentity::new);

    /// Variant: Named without supertype - "Name, a"
    private static final Parser<TokenIdentity> NAMED_PLAIN =
            sequence(TOKEN_NAME, TOKEN_AMOUNT, (name, amount) -> new TokenIdentity(name, amount, Supertypes.empty()));

    /// Variant: Unnamed with supertype - "a legendary"
    private static final Parser<TokenIdentity> UNNAMED_LEGENDARY = sequence(
            TOKEN_AMOUNT,
            SINGLE_SUPERTYPE.map(Supertypes::of),
            (amount, supertypes) -> new TokenIdentity(null, amount, supertypes));

    /// Variant: Unnamed without supertype - "a"
    private static final Parser<TokenIdentity> UNNAMED_PLAIN =
            TOKEN_AMOUNT.map(amount -> new TokenIdentity(null, amount, Supertypes.empty()));

    /// Parses \[name ","] amount \[supertype] - combines all variants.
    private static final Parser<TokenIdentity> TOKEN_IDENTITY =
            anyOf(NAMED_LEGENDARY, NAMED_PLAIN, UNNAMED_LEGENDARY, UNNAMED_PLAIN);

    /// Parses keyword abilities for tokens.
    private static final Parser<String> KEYWORD_ABILITY = anyOf(
            word("flying").thenReturn("flying"),
            word("haste").thenReturn("haste"),
            word("vigilance").thenReturn("vigilance"),
            word("lifelink").thenReturn("lifelink"),
            word("trample").thenReturn("trample"),
            word("menace").thenReturn("menace"),
            word("deathtouch").thenReturn("deathtouch"),
            word("reach").thenReturn("reach"),
            word("defender").thenReturn("defender"),
            word("indestructible").thenReturn("indestructible"),
            word("hexproof").thenReturn("hexproof"),
            string("first strike").thenReturn("first strike"),
            string("double strike").thenReturn("double strike"));

    /// Parses a quoted ability text like "When this creature dies, draw a card."
    private static final Parser<String> QUOTED_ABILITY = quotedBy('"', '"');

    /// Parses any token ability: keyword or quoted text.
    private static final Parser<String> TOKEN_ABILITY = anyOf(KEYWORD_ABILITY, QUOTED_ABILITY);

    /// Parses a comma-separated ability in a list (ability followed by comma).
    private static final Parser<String> COMMA_ABILITY = TOKEN_ABILITY.followedBy(string(","));

    /// Parses comma-separated list ending with "and ability": "a, b, and c" or "a, b, c, d, and e".
    private static final Parser<List<String>> COMMA_AND_ABILITIES =
            sequence(COMMA_ABILITY.atLeastOnce(), word("and").then(TOKEN_ABILITY), (commaAbilities, lastAbility) -> {
                var result = new ArrayList<>(commaAbilities);
                result.add(lastAbility);
                return List.copyOf(result);
            });

    /// Parses two abilities: "ability and ability".
    private static final Parser<List<String>> TWO_ABILITIES =
            sequence(TOKEN_ABILITY, word("and").then(TOKEN_ABILITY), List::of);

    /// Parses a single ability.
    private static final Parser<List<String>> SINGLE_ABILITY = TOKEN_ABILITY.map(List::of);

    /// Parses multiple abilities: "ability", "ability and ability", or "a, b, c, and d".
    private static final Parser<List<String>> ABILITIES_LIST =
            anyOf(COMMA_AND_ABILITIES, TWO_ABILITIES, SINGLE_ABILITY);

    /// Parses "with flying" or "with flying, vigilance, trample, lifelink, and haste".
    private static final Parser<List<String>> WITH_ABILITIES = word("with").then(ABILITIES_LIST);

    /// Parses predefined token types from rule 111.10.
    /// Pattern: "Type token(s)" where Type is the predefined token name.
    private static final Parser<PredefinedTokenType> PREDEFINED_TOKEN_TYPE = anyOf(
            // Artifact tokens (111.10a-c, 111.10f-h, 111.10s-u)
            word("Treasure").thenReturn(PredefinedTokenType.TREASURE),
            word("Food").thenReturn(PredefinedTokenType.FOOD),
            word("Gold").thenReturn(PredefinedTokenType.GOLD),
            word("Clue").thenReturn(PredefinedTokenType.CLUE),
            word("Blood").thenReturn(PredefinedTokenType.BLOOD),
            word("Powerstone").thenReturn(PredefinedTokenType.POWERSTONE),
            word("Map").thenReturn(PredefinedTokenType.MAP),
            word("Junk").thenReturn(PredefinedTokenType.JUNK),
            word("Lander").thenReturn(PredefinedTokenType.LANDER),
            // Enchantment token (111.10e)
            word("Shard").thenReturn(PredefinedTokenType.SHARD),
            // Creature token (111.10d)
            word("Walker").thenReturn(PredefinedTokenType.WALKER),
            // Special token (111.10i)
            word("Incubator").thenReturn(PredefinedTokenType.INCUBATOR));

    /// Parses Role token types (111.10j-r).
    /// Pattern: "Name Role token(s)" where Name is the role name.
    private static final Parser<PredefinedTokenType> ROLE_TOKEN_TYPE = anyOf(
            word("Cursed").thenReturn(PredefinedTokenType.CURSED_ROLE),
            word("Monster").thenReturn(PredefinedTokenType.MONSTER_ROLE),
            word("Royal").thenReturn(PredefinedTokenType.ROYAL_ROLE),
            word("Sorcerer").thenReturn(PredefinedTokenType.SORCERER_ROLE),
            word("Virtuous").thenReturn(PredefinedTokenType.VIRTUOUS_ROLE),
            word("Wicked").thenReturn(PredefinedTokenType.WICKED_ROLE),
            string("Young Hero").thenReturn(PredefinedTokenType.YOUNG_HERO_ROLE));

    /// Parses "Create a Treasure token." or "Create two Food tokens."
    ///
    /// Pattern: "Create" amount type "token(s)" ["."]
    private static final Parser<CreateTokenEffect.Predefined> CREATE_SIMPLE_PREDEFINED_TOKEN = word("Create")
            .then(sequence(
                    TOKEN_AMOUNT,
                    PREDEFINED_TOKEN_TYPE.followedBy(anyOf(string("tokens"), string("token"))),
                    CreateTokenEffect.Predefined::new))
            .optionallyFollowedBy(".");

    /// Parses "Create a Cursed Role token." or "Create two Monster Role tokens."
    ///
    /// Pattern: "Create" amount name "Role token(s)" ["."]
    private static final Parser<CreateTokenEffect.Predefined> CREATE_ROLE_TOKEN = word("Create")
            .then(sequence(
                    TOKEN_AMOUNT,
                    ROLE_TOKEN_TYPE.followedBy(string("Role")).followedBy(anyOf(string("tokens"), string("token"))),
                    CreateTokenEffect.Predefined::new))
            .optionallyFollowedBy(".");

    /// Parses any predefined token creation effect.
    private static final Parser<CreateTokenEffect> CREATE_PREDEFINED_TOKEN =
            anyOf(CREATE_ROLE_TOKEN, CREATE_SIMPLE_PREDEFINED_TOKEN).map(effect -> effect);

    /// Parses a single card type.
    private static final Parser<Type> SINGLE_TYPE = Arrays.stream(Type.values())
            .map(type -> OracleParser.word(type).thenReturn(type))
            .collect(or());

    /// Parses "token" or "tokens" (longer match first).
    private static final Parser<String> TOKEN_SUFFIX = anyOf(string("tokens"), string("token"));

    /// Parses one or more types followed by "token(s)".
    /// Examples: "creature token", "artifact creature tokens", "enchantment token"
    private static final Parser<Types> TOKEN_TYPE_SUFFIX = SINGLE_TYPE
            .atLeastOnce()
            .map(list -> Types.of(list.toArray(Type[]::new)))
            .followedBy(TOKEN_SUFFIX);

    /// Helper record to hold subtypes and card types together.
    private record SubtypesAndCardTypes(Subtypes subtypes, Types cardTypes) {}

    /// Parses subtypes followed by type suffix.
    private static final Parser<SubtypesAndCardTypes> SUBTYPES_AND_TYPE_SUFFIX =
            sequence(SUBTYPES, TOKEN_TYPE_SUFFIX, SubtypesAndCardTypes::new);

    /// Helper record for colors and type info.
    private record ColorsAndTypeInfo(Colors colors, SubtypesAndCardTypes typeInfo) {}

    /// Parses colors followed by subtypes and type suffix.
    private static final Parser<ColorsAndTypeInfo> COLORS_AND_TYPE_INFO =
            sequence(TOKEN_COLORS, SUBTYPES_AND_TYPE_SUFFIX, ColorsAndTypeInfo::new);

    /// Parses subtypes and type suffix without colors (colors come later or are absent).
    private static final Parser<ColorsAndTypeInfo> NO_COLORS_TYPE_INFO =
            SUBTYPES_AND_TYPE_SUFFIX.map(typeInfo -> new ColorsAndTypeInfo(Colors.empty(), typeInfo));

    /// Parses optional colors followed by subtypes and type suffix.
    /// Colors are optional - if not present, defaults to empty (colorless).
    private static final Parser<ColorsAndTypeInfo> OPTIONAL_COLORS_AND_TYPE_INFO =
            anyOf(COLORS_AND_TYPE_INFO, NO_COLORS_TYPE_INFO);

    /// Helper record for token characteristics: optional P/T, colors, subtypes, types.
    private record TokenCharacteristics(@Nullable PowerToughness pt, Colors colors, SubtypesAndCardTypes typeInfo) {}

    /// Parses token characteristics with P/T (for creatures).
    private static final Parser<TokenCharacteristics> CHARACTERISTICS_WITH_PT = sequence(
            POWER_TOUGHNESS,
            OPTIONAL_COLORS_AND_TYPE_INFO,
            (pt, cti) -> new TokenCharacteristics(pt, cti.colors(), cti.typeInfo()));

    /// Parses token characteristics without P/T (for non-creatures).
    private static final Parser<TokenCharacteristics> CHARACTERISTICS_WITHOUT_PT =
            OPTIONAL_COLORS_AND_TYPE_INFO.map(cti -> new TokenCharacteristics(null, cti.colors(), cti.typeInfo()));

    /// Parses token characteristics - tries with P/T first, falls back to without.
    private static final Parser<TokenCharacteristics> TOKEN_CHARACTERISTICS =
            anyOf(CHARACTERISTICS_WITH_PT, CHARACTERISTICS_WITHOUT_PT);

    /// Parses any token (creature or non-creature).
    /// Examples: "Create a 1/1 white Soldier creature token."
    ///           "Create Boo, a legendary 1/1 red Hamster creature token with trample and haste."
    ///           "Create a black Aura Curse enchantment token."
    ///           "Create Cragflame, a legendary colorless Equipment artifact token."
    ///           "Create Mechtitan, a legendary 10/10 Construct artifact creature token with flying, vigilance,
    // trample, lifelink, and haste that's all colors."
    private static final Parser<CreateTokenEffect.Token> CREATE_TOKEN_BASE = word("Create")
            .then(sequence(
                    TOKEN_IDENTITY,
                    TOKEN_CHARACTERISTICS,
                    (identity, chars) -> new CreateTokenEffect.Token(
                            identity.name(),
                            identity.amount(),
                            identity.supertypes(),
                            chars.pt(),
                            chars.colors(),
                            chars.typeInfo().cardTypes(),
                            chars.typeInfo().subtypes(),
                            List.of())))
            .optionallyFollowedBy(
                    WITH_ABILITIES,
                    (effect, abilities) -> new CreateTokenEffect.Token(
                            effect.name(),
                            effect.amount(),
                            effect.supertypes(),
                            effect.powerToughness(),
                            effect.colors(),
                            effect.types(),
                            effect.subtypes(),
                            abilities))
            .optionallyFollowedBy(
                    THATS_ALL_COLORS,
                    (effect, colors) -> new CreateTokenEffect.Token(
                            effect.name(),
                            effect.amount(),
                            effect.supertypes(),
                            effect.powerToughness(),
                            colors,
                            effect.types(),
                            effect.subtypes(),
                            effect.abilities()))
            .optionallyFollowedBy(".");

    private static final Parser<CreateTokenEffect> CREATE_TOKEN =
            CREATE_TOKEN_BASE.<CreateTokenEffect>map(effect -> effect);

    /// Parses a single word that is not "token" or "tokens" (case-insensitive).
    private static final Parser<String> NON_TOKEN_WORD =
            word().suchThat(w -> !w.equalsIgnoreCase("token") && !w.equalsIgnoreCase("tokens"), "non-token word");

    /// Parses a card name (one or more words, excluding "token/tokens").
    /// Card names can be multiple words like "Marit Lage" or "Minsc and Boo".
    private static final Parser<String> CARD_NAME = NON_TOKEN_WORD.atLeastOnce().map(words -> String.join(" ", words));

    /// Parses token creation by card name reference (rule 111.11).
    /// Pattern: "Create" amount cardName "token(s)" ["."]
    /// Examples: "Create a Tarmogoyf token.", "Create two Marit Lage tokens."
    private static final Parser<CreateTokenEffect.ByCardName> CREATE_BY_CARD_NAME_TOKEN = word("Create")
            .then(sequence(
                    TOKEN_AMOUNT,
                    CARD_NAME.followedBy(anyOf(string("tokens"), string("token"))),
                    CreateTokenEffect.ByCardName::new))
            .optionallyFollowedBy(".");

    private static final Parser<CreateTokenEffect> CREATE_CARD_NAME_TOKEN =
            CREATE_BY_CARD_NAME_TOKEN.<CreateTokenEffect>map(effect -> effect);

    /// Parses any token creation effect.
    /// Order matters: predefined tokens first, then explicit tokens, then card name tokens as fallback.
    public static final Parser<CreateTokenEffect> CREATE_TOKEN_EFFECT =
            anyOf(CREATE_PREDEFINED_TOKEN, CREATE_TOKEN, CREATE_CARD_NAME_TOKEN);
}
