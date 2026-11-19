CREATE TABLE pat_expiry_notifications (
    pat_id TEXT NOT NULL REFERENCES personal_access_tokens(id) ON DELETE CASCADE,
    expires_at TEXT NOT NULL,
    days_before INTEGER NOT NULL CHECK (days_before IN (30, 14, 7, 1)),
    sent_at TEXT NOT NULL,
    PRIMARY KEY (pat_id, expires_at, days_before)
);
