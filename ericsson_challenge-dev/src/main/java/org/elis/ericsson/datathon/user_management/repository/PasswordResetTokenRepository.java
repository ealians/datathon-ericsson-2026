package org.elis.ericsson.datathon.user_management.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.elis.ericsson.datathon.user_management.model.entity.PasswordResetToken;
import java.time.Instant;
import java.util.Optional;

/**
 * Repository to manage all the operations related to the password reset token.
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);

    @Transactional
    void deleteAllByExpiryDateIsLessThan(Instant expiryDate);

    @Modifying
    @Transactional
    void deleteByExpiryDateIsLessThan(Instant expiryDate);
}