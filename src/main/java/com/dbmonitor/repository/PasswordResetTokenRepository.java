package com.dbmonitor.repository;

import com.dbmonitor.model.PasswordResetToken;
import com.dbmonitor.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Find a password reset token by token string
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Find a password reset token by user
     */
    Optional<PasswordResetToken> findByUser(User user);

    /**
     * Delete all expired tokens
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken p WHERE p.expiryDate < :now")
    void deleteExpiredTokens(LocalDateTime now);

    /**
     * Delete all tokens for a specific user
     */
    @Modifying
    @Transactional
    void deleteByUser(User user);

    /**
     * Delete all used tokens
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken p WHERE p.used = true")
    void deleteUsedTokens();
}
