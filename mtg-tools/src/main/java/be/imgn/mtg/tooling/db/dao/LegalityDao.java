package be.imgn.mtg.tooling.db.dao;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/// Data access object for card legalities.
public interface LegalityDao {

    @SqlBatch("""
            INSERT INTO legality (card_id, format_id, legality)
            VALUES (:cardId, :formatId, :legality)
            """)
    void insertBatch(
            @Bind("cardId") List<Long> cardIds,
            @Bind("formatId") List<Long> formatIds,
            @Bind("legality") List<String> legalities);

    @SqlUpdate("TRUNCATE TABLE legality")
    void deleteAll();

    // Index management for bulk loading. We only drop/recreate indexes
    // whose columns are NOT backed by the (card_id, format_id) primary
    // key — H2 refuses to drop constraint-backed indexes.
    @SqlUpdate("DROP INDEX IF EXISTS idx_legality_legality")
    void dropLegalityIndex();

    @SqlUpdate("DROP INDEX IF EXISTS idx_legality_format_legality_card")
    void dropFormatLegalityCardIndex();

    @SqlUpdate("CREATE INDEX IF NOT EXISTS idx_legality_legality ON legality(legality)")
    void createLegalityIndex();

    @SqlUpdate("CREATE INDEX IF NOT EXISTS idx_legality_format_legality_card ON legality(format_id, legality, card_id)")
    void createFormatLegalityCardIndex();
}
