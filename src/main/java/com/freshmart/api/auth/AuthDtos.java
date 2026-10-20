package com.freshmart.api.auth;

import jakarta.validation.constraints.*;
import java.time.Instant;

public final class AuthDtos {
    private AuthDtos() { }

    public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 100) String fullName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Pattern(regexp = "[0-9]{10}") String mobile,
        @NotBlank @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$") String password,
        @NotBlank @Size(max = 72) String confirmPassword) {
        @AssertTrue(message = "Passwords must match")
        public boolean isPasswordConfirmed() { return password != null && password.equals(confirmPassword); }
        @AssertTrue(message = "Full name must contain at least three non-space characters")
        public boolean isFullNameValid() { return fullName != null && fullName.trim().length() >= 3; }
    }

    public record LoginRequest(@NotBlank @Size(max = 254) String identifier, @NotBlank @Size(max = 72) String password) { }
    public record UserView(Long id, String fullName, String email, String mobile, String role, Instant createdAt) {
        static UserView from(com.freshmart.api.user.User u) { return new UserView(u.getId(), u.getFullName(), u.getEmail(), u.getMobile(), u.getRole(), u.getCreatedAt()); }
    }
    public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds, UserView user) { }
    public record MessageResponse(String message) { }
}
