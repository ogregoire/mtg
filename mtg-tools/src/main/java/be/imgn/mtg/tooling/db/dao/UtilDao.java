package be.imgn.mtg.tooling.db.dao;

import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/// Utility DAO for database operations.
public interface UtilDao {

    @SqlUpdate("SET REFERENTIAL_INTEGRITY FALSE")
    void disableReferentialIntegrity();

    @SqlUpdate("SET REFERENTIAL_INTEGRITY TRUE")
    void enableReferentialIntegrity();
}
