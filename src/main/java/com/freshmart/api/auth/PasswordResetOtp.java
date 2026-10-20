package com.freshmart.api.auth;

import com.freshmart.api.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "password_reset_otps")
public class PasswordResetOtp {
    @Id @Column(length = 36) private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(name = "otp_hash", nullable = false, length = 100) private String otpHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(nullable = false) private int attempts;
    @Column(name = "verified_at") private Instant verifiedAt;
    @Column(name = "consumed_at") private Instant consumedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected PasswordResetOtp() { }
    public PasswordResetOtp(String id, User user, String otpHash, Instant expiresAt) {
        this.id = id; this.user = user; this.otpHash = otpHash; this.expiresAt = expiresAt;
    }
    @PrePersist void onCreate() { if (createdAt == null) createdAt = Instant.now(); }
    public User getUser() { return user; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean canVerifyAt(Instant now) { return consumedAt == null && verifiedAt == null && attempts < 5 && expiresAt.isAfter(now); }
    public boolean matches(String otp, org.springframework.security.crypto.password.PasswordEncoder encoder) {
        return encoder.matches(otp, otpHash);
    }
    public void recordFailure() { if (attempts < 5) attempts++; }
    public int getAttempts() { return attempts; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public Instant getConsumedAt() { return consumedAt; }
    public String getId() { return id; }
    public void markVerified(Instant now) { verifiedAt = now; }
    public void consume(Instant now) { consumedAt = now; }
}
