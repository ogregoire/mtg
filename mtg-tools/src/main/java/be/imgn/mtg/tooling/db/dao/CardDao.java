package be.imgn.mtg.tooling.db.dao;

import java.util.Map;
import java.util.UUID;

import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jspecify.annotations.Nullable;

/// Data access object for cards.
public interface CardDao {

    @SqlUpdate("""
            INSERT INTO card (
                oracle_id, name, layout, mana_value, color_identity, color_indicator,
                colors, defense, hand_modifier, keywords, life_modifier, loyalty,
                mana_cost, oracle_text, power, toughness, type_line,
                face_1_name, face_1_mana_value, face_1_color_indicator, face_1_colors,
                face_1_defense, face_1_loyalty, face_1_mana_cost, face_1_oracle_text,
                face_1_power, face_1_toughness, face_1_type_line,
                face_2_name, face_2_mana_value, face_2_color_indicator, face_2_colors,
                face_2_defense, face_2_loyalty, face_2_mana_cost, face_2_oracle_text,
                face_2_power, face_2_toughness, face_2_type_line,
                data
            ) VALUES (
                :oracleId, :name, :layout, :manaValue, :colorIdentity, :colorIndicator,
                :colors, :defense, :handModifier, :keywords, :lifeModifier, :loyalty,
                :manaCost, :oracleText, :power, :toughness, :typeLine,
                :face1Name, :face1ManaValue, :face1ColorIndicator, :face1Colors,
                :face1Defense, :face1Loyalty, :face1ManaCost, :face1OracleText,
                :face1Power, :face1Toughness, :face1TypeLine,
                :face2Name, :face2ManaValue, :face2ColorIndicator, :face2Colors,
                :face2Defense, :face2Loyalty, :face2ManaCost, :face2OracleText,
                :face2Power, :face2Toughness, :face2TypeLine,
                :data
            )
            """)
    @GetGeneratedKeys
    long insertAndGetId(
            @Bind("oracleId") UUID oracleId,
            @Bind("name") String name,
            @Bind("layout") String layout,
            @Bind("manaValue") double manaValue,
            @Bind("colorIdentity") @Nullable String colorIdentity,
            @Bind("colorIndicator") @Nullable String colorIndicator,
            @Bind("colors") @Nullable String colors,
            @Bind("defense") @Nullable String defense,
            @Bind("handModifier") @Nullable String handModifier,
            @Bind("keywords") @Nullable String keywords,
            @Bind("lifeModifier") @Nullable String lifeModifier,
            @Bind("loyalty") @Nullable String loyalty,
            @Bind("manaCost") @Nullable String manaCost,
            @Bind("oracleText") @Nullable String oracleText,
            @Bind("power") @Nullable String power,
            @Bind("toughness") @Nullable String toughness,
            @Bind("typeLine") @Nullable String typeLine,
            @Bind("face1Name") @Nullable String face1Name,
            @Bind("face1ManaValue") @Nullable Double face1ManaValue,
            @Bind("face1ColorIndicator") @Nullable String face1ColorIndicator,
            @Bind("face1Colors") @Nullable String face1Colors,
            @Bind("face1Defense") @Nullable String face1Defense,
            @Bind("face1Loyalty") @Nullable String face1Loyalty,
            @Bind("face1ManaCost") @Nullable String face1ManaCost,
            @Bind("face1OracleText") @Nullable String face1OracleText,
            @Bind("face1Power") @Nullable String face1Power,
            @Bind("face1Toughness") @Nullable String face1Toughness,
            @Bind("face1TypeLine") @Nullable String face1TypeLine,
            @Bind("face2Name") @Nullable String face2Name,
            @Bind("face2ManaValue") @Nullable Double face2ManaValue,
            @Bind("face2ColorIndicator") @Nullable String face2ColorIndicator,
            @Bind("face2Colors") @Nullable String face2Colors,
            @Bind("face2Defense") @Nullable String face2Defense,
            @Bind("face2Loyalty") @Nullable String face2Loyalty,
            @Bind("face2ManaCost") @Nullable String face2ManaCost,
            @Bind("face2OracleText") @Nullable String face2OracleText,
            @Bind("face2Power") @Nullable String face2Power,
            @Bind("face2Toughness") @Nullable String face2Toughness,
            @Bind("face2TypeLine") @Nullable String face2TypeLine,
            @Bind("data") @Nullable String data);

    @SqlUpdate("TRUNCATE TABLE card")
    void deleteAll();

    @SqlQuery("SELECT oracle_id, card_id FROM card")
    @KeyColumn("oracle_id")
    @ValueColumn("card_id")
    Map<UUID, Long> getAllOracleIdToCardId();
}
