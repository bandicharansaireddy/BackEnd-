package com.freshmart.api.auth;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "password_reset_rate_limits")
public class PasswordResetRateLimit {
    @Id @Column(name = "ip_hash", length = 64) private String ipHash;
    @Column(name = "window_started_at", nullable = false) private Instant windowStartedAt;
    @Column(name = "request_count", nullable = false) private int requestCount;
    protected PasswordResetRateLimit() { }
    public int getRequestCount() { return requestCount; }
}
