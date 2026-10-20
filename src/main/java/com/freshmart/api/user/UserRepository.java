package com.freshmart.api.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByMobile(String mobile);
    Optional<User> findByEmailIgnoreCaseOrMobile(String email, String mobile);
    Optional<User> findByIdAndActiveTrue(Long id);
    long countByActiveTrue();
}
