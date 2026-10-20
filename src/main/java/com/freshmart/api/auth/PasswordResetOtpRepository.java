package com.freshmart.api.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, String> {
    Optional<PasswordResetOtp> findTopByUser_IdOrderByCreatedAtDesc(Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PasswordResetOtp o where o.id = :id")
    Optional<PasswordResetOtp> findByIdForUpdate(@Param("id") String id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PasswordResetOtp o where o.user.id = :userId and o.consumedAt is null order by o.createdAt desc")
    java.util.List<PasswordResetOtp> findActiveForUpdate(@Param("userId") Long userId);
}
