ALTER TABLE personal_access_tokens ADD COLUMN description TEXT;
ALTER TABLE personal_access_tokens ADD COLUMN owner TEXT;
ALTER TABLE personal_access_tokens ADD COLUMN expiry_notification BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE personal_access_tokens SET owner = user_id;
