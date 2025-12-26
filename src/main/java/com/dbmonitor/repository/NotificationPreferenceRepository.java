package com.dbmonitor.repository;

import com.dbmonitor.model.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    Optional<NotificationPreference> findByUserId(Long userId);
    List<NotificationPreference> findByEmailEnabledTrue();
    List<NotificationPreference> findBySmsEnabledTrue();
}
