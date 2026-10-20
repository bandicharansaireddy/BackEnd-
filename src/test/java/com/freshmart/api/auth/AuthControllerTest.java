package com.freshmart.api.auth;

import com.freshmart.api.config.SecurityConfig;
import com.freshmart.api.user.User;
import com.freshmart.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuthController.class, properties = "app.jwt.secret=VGhpc0lzQVN0dW55VGVzdEtleUZvckZyZXNobWFydCE=")
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class AuthControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private UserRepository users;
    @MockitoBean private RefreshSessionRepository sessions;
    @MockitoBean private PasswordResetOtpRepository resetOtps;
    @MockitoBean private PasswordResetRateLimitRepository rateLimits;
    @MockitoBean private OtpDelivery otpDelivery;
    @MockitoBean private JwtTokenService tokens;
    @MockitoBean private JwtAuthenticationFilter jwtFilter;

    @Test
    void rejectsWeakPasswordBeforeLookingUpAccount() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content("""
                    {"fullName":"Fresh Buyer","email":"buyer@example.com","mobile":"9876543210","password":"weakpass","confirmPassword":"weakpass"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").exists());
        verifyNoInteractions(users, sessions);
    }

    @Test
    void rejectsDuplicateRegistrationWithoutSavingPassword() throws Exception {
        when(users.existsByEmailIgnoreCase("buyer@example.com")).thenReturn(true);
        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content("""
                    {"fullName":"Fresh Buyer","email":"Buyer@Example.com","mobile":"9876543210","password":"StrongPass9!","confirmPassword":"StrongPass9!"}
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("An account with those details already exists"));
        verify(users, never()).save(any());
    }

    @Test
    void registrationCreatesHashedRefreshSessionAndHttpOnlyCookie() throws Exception {
        when(users.save(any(User.class))).thenAnswer(call -> {
            User user = call.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 42L);
            ReflectionTestUtils.setField(user, "createdAt", java.time.Instant.now());
            return user;
        });
        when(tokens.issue(42L, "CUSTOMER")).thenReturn("short-lived-access-token");

        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content("""
                    {"fullName":"Fresh Buyer","email":"Buyer@Example.com","mobile":"9876543210","password":"StrongPass9!","confirmPassword":"StrongPass9!"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("short-lived-access-token"))
            .andExpect(jsonPath("$.user.email").value("buyer@example.com"))
            .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")))
            .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("SameSite=Strict")));

        verify(users).existsByEmailIgnoreCase("buyer@example.com");
        verify(users).existsByMobile("9876543210");
        verify(sessions).save(any(RefreshSession.class));
    }

    @Test
    void unknownLoginReturnsGenericError() throws Exception {
        when(users.findByEmailIgnoreCaseOrMobile("missing@example.com", "missing@example.com"))
            .thenReturn(java.util.Optional.empty());
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("""
                    {"identifier":"missing@example.com","password":"WrongPass9!"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid email/mobile or password"));
    }

    @Test
    void loginAcceptsEmailAndIssuesShortLivedAccessTokenAndCookie() throws Exception {
        User user = new User("Fresh Buyer", "buyer@example.com", "9876543210",
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("StrongPass9!"));
        ReflectionTestUtils.setField(user, "id", 42L);
        ReflectionTestUtils.setField(user, "createdAt", Instant.now());
        when(users.findByEmailIgnoreCaseOrMobile("buyer@example.com", "buyer@example.com"))
            .thenReturn(Optional.of(user));
        when(tokens.issue(42L, "CUSTOMER")).thenReturn("short-lived-access-token");

        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("""
                    {"identifier":"buyer@example.com","password":"StrongPass9!"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("short-lived-access-token"))
            .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")));
    }

    @Test
    void refreshRequiresTheConfiguredBrowserOrigin() throws Exception {
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("freshmart_refresh", "opaque-token"))
                .header("Origin", "https://untrusted.example"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(sessions);
    }

    @Test
    void refreshRevokesOldSessionAndIssuesRotatedCookie() throws Exception {
        User user = new User("Fresh Buyer", "buyer@example.com", "9876543210", "hash");
        ReflectionTestUtils.setField(user, "id", 42L);
        ReflectionTestUtils.setField(user, "createdAt", Instant.now());
        RefreshSession prior = new RefreshSession("prior-session", user, "prior-hash", Instant.now().plusSeconds(600));
        when(sessions.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(prior));
        when(tokens.issue(42L, "CUSTOMER")).thenReturn("next-access-token");

        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("freshmart_refresh", "opaque-token"))
                .header("Origin", "http://localhost:5173"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("next-access-token"))
            .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")));

        org.mockito.ArgumentCaptor<RefreshSession> saved = org.mockito.ArgumentCaptor.forClass(RefreshSession.class);
        verify(sessions, times(2)).save(saved.capture());
        org.junit.jupiter.api.Assertions.assertNotNull(saved.getAllValues().get(0).getRevokedAt());
        org.junit.jupiter.api.Assertions.assertTrue(saved.getAllValues().get(1).isValidAt(Instant.now()));
    }

    @Test
    void forgotPasswordUsesTheSameResponseForUnknownAccounts() throws Exception {
        when(users.findByEmailIgnoreCaseOrMobile("absent@example.com", "absent@example.com"))
            .thenReturn(Optional.empty());
        mvc.perform(post("/api/auth/forgot-password").contentType("application/json")
                .content("{\"email\":\"absent@example.com\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("If the account exists, a verification code has been sent."));
        verifyNoInteractions(resetOtps, otpDelivery);
    }

    @Test
    void wrongOtpIncrementsAttemptCounter() throws Exception {
        User user = new User("Fresh Buyer", "buyer@example.com", "9876543210", "hash");
        ReflectionTestUtils.setField(user, "id", 42L);
        PasswordResetOtp otp = new PasswordResetOtp("otp-id", user,
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("123456"), Instant.now().plusSeconds(300));
        when(users.findByEmailIgnoreCaseOrMobile("buyer@example.com", "buyer@example.com")).thenReturn(Optional.of(user));
        when(resetOtps.findActiveForUpdate(42L)).thenReturn(java.util.List.of(otp));

        for (int attempt = 0; attempt < 6; attempt++) {
            mvc.perform(post("/api/auth/verify-otp").contentType("application/json")
                    .content("{\"email\":\"buyer@example.com\",\"otp\":\"111111\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired verification code"));
        }
        org.junit.jupiter.api.Assertions.assertEquals(5, otp.getAttempts());
        verify(resetOtps, times(5)).save(otp);
    }

    @Test
    void expiredOtpIsRejected() throws Exception {
        User user = new User("Fresh Buyer", "buyer@example.com", "9876543210", "hash");
        ReflectionTestUtils.setField(user, "id", 42L);
        PasswordResetOtp otp = new PasswordResetOtp("otp-id", user,
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("123456"), Instant.now().minusSeconds(1));
        when(users.findByEmailIgnoreCaseOrMobile("buyer@example.com", "buyer@example.com")).thenReturn(Optional.of(user));
        when(resetOtps.findActiveForUpdate(42L)).thenReturn(java.util.List.of(otp));

        mvc.perform(post("/api/auth/verify-otp").contentType("application/json")
                .content("{\"email\":\"buyer@example.com\",\"otp\":\"123456\"}"))
            .andExpect(status().isBadRequest());
        verify(resetOtps, never()).save(any(PasswordResetOtp.class));
    }
}
