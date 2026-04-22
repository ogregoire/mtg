package be.imgn.mtg.tooling.db.dao;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/// Data access object for card prints.
public interface PrintDao {

    @SqlBatch("""
            INSERT INTO print (card_id, set_id, collector_number, rarity, data)
            VALUES (:cardId, :setId, :collectorNumber, :rarity, :data)
            """)
    void insertBatch(
            @Bind("cardId") List<Long> cardIds,
            @Bind("setId") List<Long> setIds,
            @Bind("collectorNumber") List<String> collectorNumbers,
            @Bind("rarity") List<String> rarities,
            @Bind("data") List<String> data);

    @SqlUpdate("TRUNCATE TABLE print")
    void deleteAll();

    // Index management for bulk loading. We only drop/recreate indexes
    // whose columns are NOT backed by a foreign key or primary key —
    // H2 refuses to drop constraint-backed indexes (see schema.sql).
    @SqlUpdate("DROP INDEX IF EXISTS idx_print_rarity")
    void dropRarityIndex();

    @SqlUpdate("ALTER TABLE print DROP CONSTRAINT IF EXISTS constraint_print_unique")
    void dropUniqueConstraint();

    @SqlUpdate("CREATE INDEX IF NOT EXISTS idx_print_rarity ON print(rarity)")
    void createRarityIndex();

    @SqlUpdate(
            "ALTER TABLE print ADD CONSTRAINT IF NOT EXISTS constraint_print_unique UNIQUE (set_id, collector_number)")
    void createUniqueConstraint();
}
