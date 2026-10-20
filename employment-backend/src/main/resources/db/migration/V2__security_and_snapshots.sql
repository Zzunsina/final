CREATE TABLE auth_sessions (
    session_id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    access_hash VARCHAR(64) NOT NULL,
    refresh_hash VARCHAR(64) NOT NULL,
    access_expires_at TIMESTAMP NOT NULL,
    refresh_expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE application_snapshots (
    application_id BIGINT PRIMARY KEY,
    snapshot_json TEXT NOT NULL,
    schema_version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (application_id) REFERENCES applications(application_id)
);

CREATE INDEX idx_application_seeker ON applications(job_seeker_id, applied_at);
CREATE INDEX idx_application_job ON applications(job_id, status);
CREATE INDEX idx_employment_status ON employment_links(status, requested_at);
CREATE INDEX idx_inquiry_status ON inquiries(status, created_at);
