package be.imgn.mtg.tooling.db.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/// Data access object for formats.
public interface FormatDao {

    @SqlUpdate("INSERT INTO format (format_name) VALUES (:name)")
    @GetGeneratedKeys
    long insert(@Bind("name") String name);

    @SqlUpdate("TRUNCATE TABLE format")
    void deleteAll();
}
