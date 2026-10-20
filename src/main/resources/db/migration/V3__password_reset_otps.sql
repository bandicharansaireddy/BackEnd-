CREATE TABLE password_reset_otps (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  otp_hash VARCHAR(100) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  verified_at TIMESTAMP NULL,
  consumed_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_password_reset_otps_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT chk_password_reset_otps_attempts CHECK (attempts >= 0 AND attempts <= 5),
  INDEX idx_password_reset_otps_user (user_id, created_at)
);

CREATE TABLE password_reset_rate_limits (
  ip_hash VARCHAR(64) NOT NULL PRIMARY KEY,
  window_started_at TIMESTAMP NOT NULL,
  request_count INT NOT NULL DEFAULT 0,
  CONSTRAINT chk_password_reset_rate_count CHECK (request_count >= 0)
);
