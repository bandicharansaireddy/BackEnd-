package com.freshmart.api.auth;

import com.freshmart.api.auth.AuthDtos.*;
import com.freshmart.api.user.User;
import com.freshmart.api.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import io.jsonwebtoken.Claims;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String REFRESH_COOKIE = "freshmart_refresh";
    private static final String DUMMY_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
    private final UserRepository users;
    private final RefreshSessionRepository sessions;
    private final PasswordResetOtpRepository resetOtps;
    private final PasswordResetRateLimitRepository rateLimits;
    private final OtpDelivery otpDelivery;
    private final PasswordEncoder passwords;
    private final JwtTokenService tokens;
    private final String allowedOrigin;
    private final boolean secureCookie;
    private final Duration refreshLifetime;
    private final String rateLimitKey;
    private final SecureRandom random = new SecureRandom();

    public AuthController(UserRepository users, RefreshSessionRepository sessions, PasswordResetOtpRepository resetOtps,
                          PasswordResetRateLimitRepository rateLimits,
                          OtpDelivery otpDelivery, PasswordEncoder passwords,
                          JwtTokenService tokens, @Value("${app.cors.allowed-origin}") String allowedOrigin,
                          @Value("${app.jwt.refresh-secure:false}") boolean secureCookie,
                          @Value("${app.jwt.refresh-days:30}") long refreshDays,
                          @Value("${app.jwt.secret}") String rateLimitKey) {
        this.users = users; this.sessions = sessions; this.resetOtps = resetOtps; this.rateLimits = rateLimits; this.otpDelivery = otpDelivery;
        this.passwords = passwords; this.tokens = tokens;
        this.allowedOrigin = allowedOrigin; this.secureCookie = secureCookie;
        this.rateLimitKey = rateLimitKey;
        if (refreshDays < 1 || refreshDays > 90) throw new IllegalStateException("Refresh lifetime must be 1 to 90 days");
        this.refreshLifetime = Duration.ofDays(refreshDays);
    }

    @PostMapping("/register") @Transactional
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email) || users.existsByMobile(request.mobile()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with those details already exists");
        User user = users.save(new User(request.fullName().trim(), email, request.mobile(), passwords.encode(request.password())));
        return authenticated(user);
    }

    @PostMapping("/login") @Transactional
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String identifier = request.identifier().trim();
        User user = users.findByEmailIgnoreCaseOrMobile(identifier, identifier).orElse(null);
        String hash = user == null ? DUMMY_HASH : user.getPasswordHash();
        boolean matches = passwords.matches(request.password(), hash);
        if (user == null || !matches || !user.isActive())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email/mobile or password");
        return authenticated(user);
    }

    @PostMapping("/refresh") @Transactional
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        requireBrowserOrigin(request);
        String raw = cookieValue(request, REFRESH_COOKIE);
        if (raw == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session expired");
        RefreshSession current = sessions.findByTokenHashForUpdate(hash(raw)).orElse(null);
        Instant now = Instant.now();
        if (current == null || !current.isValidAt(now) || !current.getUser().isActive()) {
            ResponseCookie clear = refreshCookie("", Duration.ZERO);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header(HttpHeaders.SET_COOKIE, clear.toString()).build();
        }
        current.revoke(now);
        sessions.save(current);
        return authenticated(current.getUser());
    }

    @PostMapping("/logout") @Transactional
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        requireBrowserOrigin(request);
        String raw = cookieValue(request, REFRESH_COOKIE);
        if (raw != null) sessions.findByTokenHashForUpdate(hash(raw)).ifPresent(session -> session.revoke(Instant.now()));
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString()).build();
    }

    @GetMapping("/me") @Transactional(readOnly = true)
    public UserView me(org.springframework.security.core.Authentication authentication) {
        Long userId;
        try { userId = Long.valueOf(authentication.getName()); }
        catch (RuntimeException e) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"); }
        return UserView.from(users.findByIdAndActiveTrue(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required")));
    }

    @PostMapping("/forgot-password") @Transactional
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest servletRequest) {
        String ipHash = hashIp(servletRequest.getRemoteAddr());
        rateLimits.increment(ipHash, Instant.now().minusSeconds(900));
        if (rateLimits.findById(ipHash).map(PasswordResetRateLimit::getRequestCount).orElse(0) > 10)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Try again later");
        String email = request.email().trim().toLowerCase();
        users.findByEmailIgnoreCaseOrMobile(email, email).filter(User::isActive).ifPresent(user -> {
            Instant now = Instant.now();
            if (resetOtps.findTopByUser_IdOrderByCreatedAtDesc(user.getId())
                .filter(previous -> previous.getCreatedAt() != null && previous.getCreatedAt().isAfter(now.minusSeconds(60))).isPresent()) return;
            resetOtps.findActiveForUpdate(user.getId()).forEach(otp -> otp.consume(now));
            int code = random.nextInt(1_000_000);
            String otp = String.format(java.util.Locale.ROOT, "%06d", code);
            resetOtps.save(new PasswordResetOtp(UUID.randomUUID().toString(), user, passwords.encode(otp), now.plusSeconds(300)));
            otpDelivery.deliver(email, otp);
        });
        return new MessageResponse("If the account exists, a verification code has been sent.");
    }

    @PostMapping("/verify-otp") @Transactional
    public ResetTokenResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = users.findByEmailIgnoreCaseOrMobile(email, email).orElse(null);
        PasswordResetOtp otp = user == null ? null : resetOtps.findActiveForUpdate(user.getId()).stream().findFirst().orElse(null);
        Instant now = Instant.now();
        if (otp == null || !otp.canVerifyAt(now)) {
            passwords.matches(request.otp(), DUMMY_HASH);
            throw invalidOtp();
        }
        if (!otp.matches(request.otp(), passwords)) {
            otp.recordFailure(); resetOtps.save(otp);
            throw invalidOtp();
        }
        otp.markVerified(now); resetOtps.save(otp);
        return new ResetTokenResponse(tokens.issueReset(user.getId(), otp.getId()), 300);
    }

    @PostMapping("/reset-password") @Transactional
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        Claims claims;
        String otpId;
        Long userId;
        try {
            claims = tokens.parse(request.resetToken());
            if (!"password_reset".equals(claims.get("purpose", String.class))) throw invalidOtp();
            otpId = claims.get("otpId", String.class);
            userId = Long.valueOf(claims.getSubject());
        } catch (RuntimeException e) { throw invalidOtp(); }
        PasswordResetOtp otp = resetOtps.findByIdForUpdate(otpId).orElseThrow(AuthController::invalidOtp);
        Instant now = Instant.now();
        if (!otp.getUser().getId().equals(userId) || otp.getVerifiedAt() == null || otp.getConsumedAt() != null || !otp.getExpiresAt().isAfter(now)) throw invalidOtp();
        User user = users.findByIdAndActiveTrue(userId).orElseThrow(AuthController::invalidOtp);
        otp.consume(now); resetOtps.save(otp);
        user.setPasswordHash(passwords.encode(request.newPassword()));
        revokeAllSessions(userId, now);
        return new MessageResponse("Password updated. Please sign in again.");
    }

    @PutMapping("/change-password") @Transactional
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                org.springframework.security.core.Authentication authentication) {
        Long userId;
        try { userId = Long.valueOf(authentication.getName()); }
        catch (RuntimeException e) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"); }
        User user = users.findByIdAndActiveTrue(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
        if (!passwords.matches(request.currentPassword(), user.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        user.setPasswordHash(passwords.encode(request.newPassword()));
        revokeAllSessions(userId, Instant.now());
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString()).build();
    }

    private ResponseEntity<AuthResponse> authenticated(User user) {
        byte[] bytes = new byte[48]; random.nextBytes(bytes);
        String rawRefresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.save(new RefreshSession(UUID.randomUUID().toString(), user, hash(rawRefresh), Instant.now().plus(refreshLifetime)));
        AuthResponse body = new AuthResponse(tokens.issue(user.getId(), user.getRole()), "Bearer", tokens.accessLifetimeSeconds(), UserView.from(user));
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie(rawRefresh, refreshLifetime).toString()).body(body);
    }

    private void revokeAllSessions(Long userId, Instant now) {
        sessions.findAllByUser_IdAndRevokedAtIsNull(userId).forEach(session -> session.revoke(now));
    }
    private static ResponseStatusException invalidOtp() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired verification code");
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value).httpOnly(true).secure(secureCookie)
            .sameSite("Strict").path("/api/auth").maxAge(maxAge).build();
    }
    private void requireBrowserOrigin(HttpServletRequest request) {
        if (!allowedOrigin.equals(request.getHeader("Origin")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Request origin is not allowed");
    }
    private static String cookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (var cookie : request.getCookies()) if (name.equals(cookie.getName())) return cookie.getValue();
        return null;
    }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException("Required hash algorithm unavailable", e); }
    }
    private String hashIp(String ip) {
        try {
            byte[] key = Base64.getDecoder().decode(rateLimitKey);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(ip.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException("Unable to apply password-reset rate limit", e); }
    }
}
