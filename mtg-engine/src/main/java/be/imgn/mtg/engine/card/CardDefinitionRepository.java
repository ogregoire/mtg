package be.imgn.mtg.engine.card;

import java.util.Optional;
import java.util.UUID;

/// Repository for looking up card definitions.
///
/// Implementation is deferred to a future module.
public interface CardDefinitionRepository {

    /// Finds a card definition by its name.
    ///
    /// @param name the card name to search for
    /// @return the card definition, or empty if not found
    Optional<CardDefinition> findByName(String name);

    /// Finds a card definition by its Oracle ID.
    ///
    /// @param oracleId the Oracle ID to search for
    /// @return the card definition, or empty if not found
    Optional<CardDefinition> findByOracleId(UUID oracleId);
}
