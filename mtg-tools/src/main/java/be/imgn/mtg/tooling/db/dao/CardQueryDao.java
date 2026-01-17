package be.imgn.mtg.tooling.db.dao;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Jdbi;
import org.jspecify.annotations.Nullable;

import be.imgn.mtg.tooling.card.model.CardLegalityResult;
import be.imgn.mtg.tooling.card.model.CardPrintResult;
import be.imgn.mtg.tooling.card.model.CardResult;
import be.imgn.mtg.tooling.card.model.CardRulingResult;

/// Data access object for card queries (read operations).
public final class CardQueryDao {

    private final Jdbi jdbi;

    public CardQueryDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    /// Finds a card by exact name match (case-insensitive).
    /// Also checks face_1_name and face_2_name for double-faced cards.
    /// If multiple cards match, returns the first one (prioritizing exact name match).
    ///
    /// @param name the card name to search for
    /// @return the card if found
    public Optional<CardResult> findByExactName(String name) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                        SELECT card_id, name, layout, mana_value, color_identity, colors, mana_cost,
                               oracle_text, power, toughness, type_line, loyalty, defense,
                               face_1_name, face_1_mana_cost, face_1_oracle_text, face_1_type_line,
                               face_1_power, face_1_toughness, face_1_loyalty,
                               face_2_name, face_2_mana_cost, face_2_oracle_text, face_2_type_line,
                               face_2_power, face_2_toughness, face_2_loyalty
                        FROM card
                        WHERE LOWER(name) = LOWER(:name)
                           OR LOWER(face_1_name) = LOWER(:name)
                           OR LOWER(face_2_name) = LOWER(:name)
                        ORDER BY
                            CASE WHEN LOWER(name) = LOWER(:name) THEN 0 ELSE 1 END,
                            CASE WHEN oracle_text IS NOT NULL AND oracle_text != '' THEN 0 ELSE 1 END,
                            CASE WHEN face_1_oracle_text IS NOT NULL AND face_1_oracle_text != '' THEN 0 ELSE 1 END
                        LIMIT 1
                        """)
                .bind("name", name)
                .map((rs, ctx) -> new CardResult(
                        rs.getLong("card_id"),
                        rs.getString("name"),
                        rs.getString("layout"),
                        rs.getDouble("mana_value"),
                        rs.getString("color_identity"),
                        rs.getString("colors"),
                        rs.getString("mana_cost"),
                        rs.getString("oracle_text"),
                        rs.getString("power"),
                        rs.getString("toughness"),
                        rs.getString("type_line"),
                        rs.getString("loyalty"),
                        rs.getString("defense"),
                        rs.getString("face_1_name"),
                        rs.getString("face_1_mana_cost"),
                        rs.getString("face_1_oracle_text"),
                        rs.getString("face_1_type_line"),
                        rs.getString("face_1_power"),
                        rs.getString("face_1_toughness"),
                        rs.getString("face_1_loyalty"),
                        rs.getString("face_2_name"),
                        rs.getString("face_2_mana_cost"),
                        rs.getString("face_2_oracle_text"),
                        rs.getString("face_2_type_line"),
                        rs.getString("face_2_power"),
                        rs.getString("face_2_toughness"),
                        rs.getString("face_2_loyalty")))
                .findOne());
    }

    /// Searches for cards matching the given criteria.
    /// Also checks face_1_name and face_2_name for double-faced cards.
    ///
    /// @param namePattern name pattern (SQL LIKE pattern with % wildcards)
    /// @param typeCondition SQL condition for type line (nullable)
    /// @param oraclePattern oracle text pattern (SQL LIKE pattern, nullable)
    /// @param colorCondition SQL condition for colors (e.g., "colors LIKE '%R%'")
    /// @param colorIdentityCondition SQL condition for color identity (nullable)
    /// @param dfcCondition SQL condition for DFC filtering (nullable)
    /// @param limit maximum number of results
    /// @param offset offset for pagination
    /// @return list of matching cards
    public List<CardResult> searchCards(
            String namePattern,
            @Nullable String typeCondition,
            @Nullable String oraclePattern,
            @Nullable String colorCondition,
            @Nullable String colorIdentityCondition,
            @Nullable String dfcCondition,
            int limit,
            int offset) {
        var sql = new StringBuilder("""
                SELECT card_id, name, layout, mana_value, color_identity, colors, mana_cost,
                       oracle_text, power, toughness, type_line, loyalty, defense,
                       face_1_name, face_1_mana_cost, face_1_oracle_text, face_1_type_line,
                       face_1_power, face_1_toughness, face_1_loyalty,
                       face_2_name, face_2_mana_cost, face_2_oracle_text, face_2_type_line,
                       face_2_power, face_2_toughness, face_2_loyalty
                FROM card
                WHERE (LOWER(name) LIKE LOWER(:namePattern)
                    OR LOWER(face_1_name) LIKE LOWER(:namePattern)
                    OR LOWER(face_2_name) LIKE LOWER(:namePattern))
                """);

        if (typeCondition != null && !typeCondition.isBlank()) {
            sql.append(" AND (").append(typeCondition).append(")");
        }
        if (oraclePattern != null) {
            sql.append(" AND LOWER(oracle_text) LIKE LOWER(:oraclePattern)");
        }
        if (colorCondition != null && !colorCondition.isBlank()) {
            sql.append(" AND (").append(colorCondition).append(")");
        }
        if (colorIdentityCondition != null && !colorIdentityCondition.isBlank()) {
            sql.append(" AND (").append(colorIdentityCondition).append(")");
        }
        if (dfcCondition != null && !dfcCondition.isBlank()) {
            sql.append(" AND (").append(dfcCondition).append(")");
        }

        sql.append(" ORDER BY name LIMIT :limit OFFSET :offset");

        var finalOraclePattern = oraclePattern;

        return jdbi.withHandle(handle -> {
            var query = handle.createQuery(sql.toString())
                    .bind("namePattern", namePattern)
                    .bind("limit", limit)
                    .bind("offset", offset);

            if (finalOraclePattern != null) {
                query.bind("oraclePattern", finalOraclePattern);
            }

            return query.map((rs, ctx) -> new CardResult(
                            rs.getLong("card_id"),
                            rs.getString("name"),
                            rs.getString("layout"),
                            rs.getDouble("mana_value"),
                            rs.getString("color_identity"),
                            rs.getString("colors"),
                            rs.getString("mana_cost"),
                            rs.getString("oracle_text"),
                            rs.getString("power"),
                            rs.getString("toughness"),
                            rs.getString("type_line"),
                            rs.getString("loyalty"),
                            rs.getString("defense"),
                            rs.getString("face_1_name"),
                            rs.getString("face_1_mana_cost"),
                            rs.getString("face_1_oracle_text"),
                            rs.getString("face_1_type_line"),
                            rs.getString("face_1_power"),
                            rs.getString("face_1_toughness"),
                            rs.getString("face_1_loyalty"),
                            rs.getString("face_2_name"),
                            rs.getString("face_2_mana_cost"),
                            rs.getString("face_2_oracle_text"),
                            rs.getString("face_2_type_line"),
                            rs.getString("face_2_power"),
                            rs.getString("face_2_toughness"),
                            rs.getString("face_2_loyalty")))
                    .list();
        });
    }

    /// Counts cards matching the given criteria.
    /// Also checks face_1_name and face_2_name for double-faced cards.
    ///
    /// @param namePattern name pattern (SQL LIKE pattern with % wildcards)
    /// @param typeCondition SQL condition for type line (nullable)
    /// @param oraclePattern oracle text pattern (SQL LIKE pattern, nullable)
    /// @param colorCondition SQL condition for colors
    /// @param colorIdentityCondition SQL condition for color identity (nullable)
    /// @param dfcCondition SQL condition for DFC filtering (nullable)
    /// @return count of matching cards
    public int countCards(
            String namePattern,
            @Nullable String typeCondition,
            @Nullable String oraclePattern,
            @Nullable String colorCondition,
            @Nullable String colorIdentityCondition,
            @Nullable String dfcCondition) {
        var sql = new StringBuilder("""
                SELECT COUNT(*) FROM card WHERE (LOWER(name) LIKE LOWER(:namePattern)
                    OR LOWER(face_1_name) LIKE LOWER(:namePattern)
                    OR LOWER(face_2_name) LIKE LOWER(:namePattern))""");

        if (typeCondition != null && !typeCondition.isBlank()) {
            sql.append(" AND (").append(typeCondition).append(")");
        }
        if (oraclePattern != null) {
            sql.append(" AND LOWER(oracle_text) LIKE LOWER(:oraclePattern)");
        }
        if (colorCondition != null && !colorCondition.isBlank()) {
            sql.append(" AND (").append(colorCondition).append(")");
        }
        if (colorIdentityCondition != null && !colorIdentityCondition.isBlank()) {
            sql.append(" AND (").append(colorIdentityCondition).append(")");
        }
        if (dfcCondition != null && !dfcCondition.isBlank()) {
            sql.append(" AND (").append(dfcCondition).append(")");
        }

        var finalOraclePattern = oraclePattern;

        return jdbi.withHandle(handle -> {
            var query = handle.createQuery(sql.toString()).bind("namePattern", namePattern);

            if (finalOraclePattern != null) {
                query.bind("oraclePattern", finalOraclePattern);
            }

            return query.mapTo(Integer.class).one();
        });
    }

    /// Searches for cards legal in a specific format.
    /// Also checks face_1_name and face_2_name for double-faced cards.
    ///
    /// @param namePattern name pattern (SQL LIKE pattern with % wildcards)
    /// @param formatName format name (e.g., "modern", "legacy")
    /// @param limit maximum number of results
    /// @param offset offset for pagination
    /// @return list of matching cards
    public List<CardResult> searchByFormat(String namePattern, String formatName, int limit, int offset) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                        SELECT DISTINCT c.card_id, c.name, c.layout, c.mana_value, c.color_identity, c.colors,
                               c.mana_cost, c.oracle_text, c.power, c.toughness, c.type_line, c.loyalty, c.defense,
                               c.face_1_name, c.face_1_mana_cost, c.face_1_oracle_text, c.face_1_type_line,
                               c.face_1_power, c.face_1_toughness, c.face_1_loyalty,
                               c.face_2_name, c.face_2_mana_cost, c.face_2_oracle_text, c.face_2_type_line,
                               c.face_2_power, c.face_2_toughness, c.face_2_loyalty
                        FROM card c
                        JOIN legality l ON c.card_id = l.card_id
                        JOIN format f ON l.format_id = f.format_id
                        WHERE (LOWER(c.name) LIKE LOWER(:namePattern)
                            OR LOWER(c.face_1_name) LIKE LOWER(:namePattern)
                            OR LOWER(c.face_2_name) LIKE LOWER(:namePattern))
                          AND LOWER(f.format_name) = LOWER(:formatName)
                          AND l.legality IN ('legal', 'restricted')
                        ORDER BY c.name
                        LIMIT :limit OFFSET :offset
                        """)
                .bind("namePattern", namePattern)
                .bind("formatName", formatName)
                .bind("limit", limit)
                .bind("offset", offset)
                .map((rs, ctx) -> new CardResult(
                        rs.getLong("card_id"),
                        rs.getString("name"),
                        rs.getString("layout"),
                        rs.getDouble("mana_value"),
                        rs.getString("color_identity"),
                        rs.getString("colors"),
                        rs.getString("mana_cost"),
                        rs.getString("oracle_text"),
                        rs.getString("power"),
                        rs.getString("toughness"),
                        rs.getString("type_line"),
                        rs.getString("loyalty"),
                        rs.getString("defense"),
                        rs.getString("face_1_name"),
                        rs.getString("face_1_mana_cost"),
                        rs.getString("face_1_oracle_text"),
                        rs.getString("face_1_type_line"),
                        rs.getString("face_1_power"),
                        rs.getString("face_1_toughness"),
                        rs.getString("face_1_loyalty"),
                        rs.getString("face_2_name"),
                        rs.getString("face_2_mana_cost"),
                        rs.getString("face_2_oracle_text"),
                        rs.getString("face_2_type_line"),
                        rs.getString("face_2_power"),
                        rs.getString("face_2_toughness"),
                        rs.getString("face_2_loyalty")))
                .list());
    }

    /// Counts cards legal in a specific format.
    /// Also checks face_1_name and face_2_name for double-faced cards.
    ///
    /// @param namePattern name pattern (SQL LIKE pattern with % wildcards)
    /// @param formatName format name (e.g., "modern", "legacy")
    /// @return count of matching cards
    public int countByFormat(String namePattern, String formatName) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                        SELECT COUNT(DISTINCT c.card_id)
                        FROM card c
                        JOIN legality l ON c.card_id = l.card_id
                        JOIN format f ON l.format_id = f.format_id
                        WHERE (LOWER(c.name) LIKE LOWER(:namePattern)
                            OR LOWER(c.face_1_name) LIKE LOWER(:namePattern)
                            OR LOWER(c.face_2_name) LIKE LOWER(:namePattern))
                          AND LOWER(f.format_name) = LOWER(:formatName)
                          AND l.legality IN ('legal', 'restricted')
                        """)
                .bind("namePattern", namePattern)
                .bind("formatName", formatName)
                .mapTo(Integer.class)
                .one());
    }

    /// Searches for cards printed in a specific set.
    /// Also checks face_1_name and face_2_name for double-faced cards.
    ///
    /// @param namePattern name pattern (SQL LIKE pattern with % wildcards)
    /// @param setPattern set name pattern (SQL LIKE pattern)
    /// @param limit maximum number of results
    /// @param offset offset for pagination
    /// @return list of matching cards
    public List<CardResult> searchBySet(String namePattern, String setPattern, int limit, int offset) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                        SELECT DISTINCT c.card_id, c.name, c.layout, c.mana_value, c.color_identity, c.colors,
                               c.mana_cost, c.oracle_text, c.power, c.toughness, c.type_line, c.loyalty, c.defense,
                               c.face_1_name, c.face_1_mana_cost, c.face_1_oracle_text, c.face_1_type_line,
                               c.face_1_power, c.face_1_toughness, c.face_1_loyalty,
                               c.face_2_name, c.face_2_mana_cost, c.face_2_oracle_text, c.face_2_type_line,
                               c.face_2_power, c.face_2_toughness, c.face_2_loyalty
                        FROM card c
                        JOIN print p ON c.card_id = p.card_id
                        JOIN card_set s ON p.set_id = s.set_id
                        WHERE (LOWER(c.name) LIKE LOWER(:namePattern)
                            OR LOWER(c.face_1_name) LIKE LOWER(:namePattern)
                            OR LOWER(c.face_2_name) LIKE LOWER(:namePattern))
                          AND LOWER(s.name) LIKE LOWER(:setPattern)
                        ORDER BY c.name
                        LIMIT :limit OFFSET :offset
                        """)
                .bind("namePattern", namePattern)
                .bind("setPattern", setPattern)
                .bind("limit", limit)
                .bind("offset", offset)
                .map((rs, ctx) -> new CardResult(
                        rs.getLong("card_id"),
                        rs.getString("name"),
                        rs.getString("layout"),
                        rs.getDouble("mana_value"),
                        rs.getString("color_identity"),
                        rs.getString("colors"),
                        rs.getString("mana_cost"),
                        rs.getString("oracle_text"),
                        rs.getString("power"),
                        rs.getString("toughness"),
                        rs.getString("type_line"),
                        rs.getString("loyalty"),
                        rs.getString("defense"),
                        rs.getString("face_1_name"),
                        rs.getString("face_1_mana_cost"),
                        rs.getString("face_1_oracle_text"),
                        rs.getString("face_1_type_line"),
                        rs.getString("face_1_power"),
                        rs.getString("face_1_toughness"),
                        rs.getString("face_1_loyalty"),
                        rs.getString("face_2_name"),
                        rs.getString("face_2_mana_cost"),
                        rs.getString("face_2_oracle_text"),
                        rs.getString("face_2_type_line"),
                        rs.getString("face_2_power"),
                        rs.getString("face_2_toughness"),
                        rs.getString("face_2_loyalty")))
                .list());
    }

    /// Counts cards printed in a specific set.
    /// Also checks face_1_name and face_2_name for double-faced cards.
    ///
    /// @param namePattern name pattern (SQL LIKE pattern with % wildcards)
    /// @param setPattern set name pattern (SQL LIKE pattern)
    /// @return count of matching cards
    public int countBySet(String namePattern, String setPattern) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                        SELECT COUNT(DISTINCT c.card_id)
                        FROM card c
                        JOIN print p ON c.card_id = p.card_id
                        JOIN card_set s ON p.set_id = s.set_id
                        WHERE (LOWER(c.name) LIKE LOWER(:namePattern)
                            OR LOWER(c.face_1_name) LIKE LOWER(:namePattern)
                            OR LOWER(c.face_2_name) LIKE LOWER(:namePattern))
                          AND LOWER(s.name) LIKE LOWER(:setPattern)
                        """)
                .bind("namePattern", namePattern)
                .bind("setPattern", setPattern)
                .mapTo(Integer.class)
                .one());
    }

    /// Gets all format legalities for a card.
    ///
    /// @param cardId the card ID
    /// @return list of legalities
    public List<CardLegalityResult> getLegalities(long cardId) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                        SELECT f.format_name, l.legality
                        FROM legality l
                        JOIN format f ON l.format_id = f.format_id
                        WHERE l.card_id = :cardId
                        ORDER BY f.format_name
                        """)
                .bind("cardId", cardId)
                .map((rs, ctx) -> new CardLegalityResult(rs.getString("format_name"), rs.getString("legality")))
                .list());
    }

    /// Counts distinct sets a card has been printed in.
    ///
    /// @param cardId the card ID
    /// @return number of sets
    public int countSets(long cardId) {
        return jdbi.withHandle(
                handle -> handle.createQuery("SELECT COUNT(DISTINCT set_id) FROM print WHERE card_id = :cardId")
                        .bind("cardId", cardId)
                        .mapTo(Integer.class)
                        .one());
    }

    /// Gets the prints for a card (most recent first, max 20).
    ///
    /// @param cardId the card ID
    /// @return list of prints
    public List<CardPrintResult> getPrints(long cardId) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                        SELECT s.name, s.code, p.collector_number
                        FROM print p
                        JOIN card_set s ON p.set_id = s.set_id
                        WHERE p.card_id = :cardId
                        ORDER BY p.print_id DESC
                        LIMIT 20
                        """)
                .bind("cardId", cardId)
                .map((rs, ctx) -> new CardPrintResult(
                        rs.getString("name"), rs.getString("code"), rs.getString("collector_number")))
                .list());
    }

    /// Gets the most recent print for a card (for Scryfall link).
    ///
    /// @param cardId the card ID
    /// @return the most recent print
    public Optional<CardPrintResult> getMostRecentPrint(long cardId) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                        SELECT s.name, s.code, p.collector_number
                        FROM print p
                        JOIN card_set s ON p.set_id = s.set_id
                        WHERE p.card_id = :cardId
                        ORDER BY p.print_id DESC
                        LIMIT 1
                        """)
                .bind("cardId", cardId)
                .map((rs, ctx) -> new CardPrintResult(
                        rs.getString("name"), rs.getString("code"), rs.getString("collector_number")))
                .findOne());
    }

    /// Gets all rulings for a card.
    ///
    /// @param cardId the card ID
    /// @return list of rulings
    public List<CardRulingResult> getRulings(long cardId) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                        SELECT published_at, source, comment
                        FROM ruling
                        WHERE card_id = :cardId
                        ORDER BY published_at DESC
                        """)
                .bind("cardId", cardId)
                .map((rs, ctx) -> new CardRulingResult(
                        rs.getDate("published_at").toLocalDate(), rs.getString("source"), rs.getString("comment")))
                .list());
    }
}
