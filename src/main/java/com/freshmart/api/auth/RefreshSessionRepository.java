package com.freshmart.api.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, String> {
    Optional<RefreshSession> findByTokenHash(String tokenHash);
}
