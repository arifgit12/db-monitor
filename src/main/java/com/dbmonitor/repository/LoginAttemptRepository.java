package com.dbmonitor.repository;

import com.dbmonitor.model.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
    List<LoginAttempt> findByUsernameOrderByAttemptTimeDesc(String username);
    List<LoginAttempt> findByIpAddressOrderByAttemptTimeDesc(String ipAddress);
    List<LoginAttempt> findByAttemptTimeBetweenOrderByAttemptTimeDesc(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT la FROM LoginAttempt la WHERE la.username = :username AND la.attemptTime >= :since AND la.successful = false")
    List<LoginAttempt> findRecentFailedAttempts(String username, LocalDateTime since);
    
    @Query("SELECT la FROM LoginAttempt la WHERE la.ipAddress = :ipAddress AND la.attemptTime >= :since AND la.successful = false")
    List<LoginAttempt> findRecentFailedAttemptsByIp(String ipAddress, LocalDateTime since);
    
    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.username = :username AND la.attemptTime >= :since AND la.successful = false")
    long countRecentFailedAttempts(String username, LocalDateTime since);
}
