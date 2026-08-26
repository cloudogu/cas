package de.triology.cas.pat.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.triology.cas.pat.model.PATMetadata;
import de.triology.cas.pat.model.StoredPAT;

/**
 * Database-agnostic persistence contract for personal access tokens.
 * Implementations must preserve owner isolation for lookup and deletion operations.
 */
public interface PATRepository {
    /**
     * Inserts a PAT record containing only its irreversible fingerprint and metadata.
     *
     * @param pat record to persist
     */
    void insert(StoredPAT pat);

    /**
     * Finds all PAT records owned by one user, newest first.
     *
     * @param userId owner identifier
     * @return matching records, possibly empty
     */
    List<PATMetadata> findAllByUserId(String userId);

    /**
     * Finds one PAT only when both owner and record identifier match.
     *
     * @param userId owner identifier
     * @param id token record identifier
     * @return matching record or an empty optional
     */
    Optional<PATMetadata> findByUserIdAndId(String userId, UUID id);

    /**
     * Physically deletes one PAT only when it belongs to the requested owner.
     *
     * @param userId owner identifier
     * @param id token record identifier
     * @return {@code true} when exactly one record was deleted
     */
    boolean deleteByUserIdAndId(String userId, UUID id);
}
