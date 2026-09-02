package de.triology.cas.pat.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.triology.cas.pat.model.PATMetadata;
import de.triology.cas.pat.model.StoredPAT;
import de.triology.cas.pat.service.PATStorageUnavailableException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Spring JDBC implementation of {@link PATRepository} for the dedicated PAT data source.
 * Database-vendor-specific failures are kept behind the repository boundary.
 */
public class JdbcPATRepository implements PATRepository {
    private static final String METADATA_COLUMNS =
            "id, user_id, display_name, created_at, expires_at, scope";

    private final JdbcTemplate jdbcTemplate;

    /**
     * Creates a repository backed by the configured PAT JDBC template.
     *
     * @param jdbcTemplate template connected to the PAT database
     */
    public JdbcPATRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** {@inheritDoc} */
    @Override
    public void insert(StoredPAT pat) {
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO personal_access_tokens
                            (id, user_id, display_name, token_fingerprint, created_at, expires_at, scope)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """);
                statement.setString(1, pat.id().toString());
                statement.setString(2, pat.userId());
                statement.setString(3, pat.displayName());
                statement.setBytes(4, pat.tokenFingerprint().bytes());
                statement.setString(5, pat.createdAt().toString());
                if (pat.expiresAt() == null) {
                    statement.setNull(6, Types.VARCHAR);
                } else {
                    statement.setString(6, pat.expiresAt().toString());
                }
                statement.setString(7, pat.scope());
                return statement;
            });
        } catch (DataAccessException e) {
            throw translate(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<PATMetadata> findAllByUserId(String userId) {
        try {
            return jdbcTemplate.query(
                    "SELECT " + METADATA_COLUMNS + " FROM personal_access_tokens WHERE user_id = ? ORDER BY created_at DESC",
                    this::mapMetadata,
                    userId);
        } catch (DataAccessException e) {
            throw translate(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Optional<PATMetadata> findByUserIdAndId(String userId, UUID id) {
        try {
            List<PATMetadata> result = jdbcTemplate.query(
                    "SELECT " + METADATA_COLUMNS + " FROM personal_access_tokens WHERE user_id = ? AND id = ?",
                    this::mapMetadata,
                    userId,
                    id.toString());
            return result.stream().findFirst();
        } catch (DataAccessException e) {
            throw translate(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteByUserIdAndId(String userId, UUID id) {
        try {
            return jdbcTemplate.update(
                    "DELETE FROM personal_access_tokens WHERE user_id = ? AND id = ?", userId, id.toString()) == 1;
        } catch (DataAccessException e) {
            throw translate(e);
        }
    }

    /**
     * Maps one JDBC result row to public PAT metadata.
     *
     * @param resultSet current query result
     * @param rowNumber zero-based row number supplied by Spring JDBC
     * @return mapped PAT metadata
     * @throws SQLException when a column cannot be read
     */
    private PATMetadata mapMetadata(ResultSet resultSet, int rowNumber) throws SQLException {
        String expiresAt = resultSet.getString("expires_at");
        try {
            return new PATMetadata(
                    UUID.fromString(resultSet.getString("id")),
                    resultSet.getString("user_id"),
                    resultSet.getString("display_name"),
                    Instant.parse(resultSet.getString("created_at")),
                    expiresAt == null ? null : Instant.parse(expiresAt),
                    resultSet.getString("scope"));
        } catch (RuntimeException e) {
            throw new DataIntegrityViolationException("PAT storage contains invalid metadata", e);
        }
    }

    /**
     * Classifies a Spring persistence failure at the repository boundary.
     *
     * @param cause original data-access failure
     * @return storage-unavailable wrapper for resource failures, otherwise the original exception
     */
    private RuntimeException translate(DataAccessException cause) {
        if (cause instanceof TransientDataAccessException
                || cause instanceof DataAccessResourceFailureException) {
            return new PATStorageUnavailableException(cause);
        }
        return cause;
    }
}
