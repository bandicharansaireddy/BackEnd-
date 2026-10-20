package com.freshmart.api.auth;

import com.freshmart.api.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "refresh_sessions")
public class RefreshSession {
    @Id @Column(length = 36) private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected RefreshSession() { }
    public RefreshSession(String id, User user, String tokenHash, Instant expiresAt) {
        this.id = id; this.user = user; this.tokenHash = tokenHash; this.expiresAt = expiresAt;
    }
    @PrePersist void onCreate() { if (createdAt == null) createdAt = Instant.now(); }
    public String getId() { return id; }
    public User getUser() { return user; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public boolean isValidAt(Instant now) { return revokedAt == null && expiresAt.isAfter(now); }
    public void revoke(Instant now) { if (revokedAt == null) revokedAt = now; }
}
