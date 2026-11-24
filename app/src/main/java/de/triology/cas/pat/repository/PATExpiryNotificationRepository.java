package de.triology.cas.pat.repository;

import de.triology.cas.pat.model.PATMetadata;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Stores successful reminder stages in the dedicated PAT database. */
public class PATExpiryNotificationRepository {
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transaction;

    public PATExpiryNotificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transaction = new TransactionTemplate(new DataSourceTransactionManager(jdbcTemplate.getDataSource()));
    }

    /** Loads enabled, unexpired PATs; the worker applies calendar-day boundaries in its configured zone. */
    public List<PATMetadata> findCandidates(Instant now) {
        return jdbcTemplate.query("""
                SELECT id, user_id, display_name, created_at, expires_at, scope, description, owner, expiry_notification
                FROM personal_access_tokens
                WHERE expiry_notification = TRUE AND expires_at IS NOT NULL
                  AND julianday(expires_at) > julianday(?) AND owner IS NOT NULL AND trim(owner) <> ''
                """, JdbcPATRepository::mapMetadata, now.toString());
    }

    /**
     * Serializes each stage in the database and commits it only after SMTP succeeds.
     * An SMTP failure rolls back the marker so a later run can retry.
     */
    public boolean sendOnce(PATMetadata pat, int daysBefore, Instant now, Runnable send) {
        return Boolean.TRUE.equals(transaction.execute(status -> {
            int inserted = jdbcTemplate.update("""
                    INSERT INTO pat_expiry_notifications (pat_id, expires_at, days_before, sent_at)
                    VALUES (?, ?, ?, ?) ON CONFLICT (pat_id, expires_at, days_before) DO NOTHING
                    """, pat.id().toString(), pat.expiresAt().toString(), daysBefore, now.toString());
            if (inserted == 0) {
                return false;
            }
            send.run();
            return true;
        }));
    }
}
