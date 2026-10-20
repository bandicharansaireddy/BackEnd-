package com.freshmart.api.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

public interface PasswordResetRateLimitRepository extends JpaRepository<PasswordResetRateLimit, String> {
    @Modifying
    @Query(value = "INSERT INTO password_reset_rate_limits (ip_hash, window_started_at, request_count) VALUES (:ipHash, UTC_TIMESTAMP(), 1) " +
        "ON DUPLICATE KEY UPDATE request_count = IF(window_started_at < :cutoff, 1, request_count + 1), " +
        "window_started_at = IF(window_started_at < :cutoff, UTC_TIMESTAMP(), window_started_at)", nativeQuery = true)
    void increment(@Param("ipHash") String ipHash, @Param("cutoff") Instant cutoff);
}
