package be.imgn.mtg.tooling.db.dao;

import java.time.LocalDate;
import java.util.UUID;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/// Data access object for rulings.
public interface RulingDao {

    @SqlUpdate("""
            INSERT INTO ruling (card_id, source, published_at, comment)
            SELECT c.card_id, :source, :publishedAt, :comment
            FROM card c WHERE c.oracle_id = :oracleId
            """)
    void insert(
            @Bind("oracleId") UUID oracleId,
            @Bind("source") String source,
            @Bind("publishedAt") LocalDate publishedAt,
            @Bind("comment") String comment);

    @SqlUpdate("TRUNCATE TABLE ruling")
    void deleteAll();
}
