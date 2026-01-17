package be.imgn.mtg.tooling.db.dao;

import java.util.Map;

import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jspecify.annotations.Nullable;

/// Data access object for card sets.
public interface SetDao {

    @SqlUpdate("""
            INSERT INTO card_set (code, name, type, block, block_code, data)
            VALUES (:code, :name, :type, :block, :blockCode, :data)
            """)
    void insert(
            @Bind("code") String code,
            @Bind("name") String name,
            @Bind("type") String type,
            @Bind("block") @Nullable String block,
            @Bind("blockCode") @Nullable String blockCode,
            @Bind("data") String data);

    @SqlUpdate("TRUNCATE TABLE card_set")
    void deleteAll();

    @SqlQuery("SELECT code, set_id FROM card_set")
    @KeyColumn("code")
    @ValueColumn("set_id")
    Map<String, Long> getAllCodeToSetId();

    @SqlUpdate("UPDATE card_set SET parent_set_id = :parentSetId WHERE code = :code")
    void updateParentSetId(@Bind("code") String code, @Bind("parentSetId") long parentSetId);
}
