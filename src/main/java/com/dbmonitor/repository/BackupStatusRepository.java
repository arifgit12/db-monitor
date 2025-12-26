package com.dbmonitor.repository;

import com.dbmonitor.model.BackupStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BackupStatusRepository extends JpaRepository<BackupStatus, Long> {
    Optional<BackupStatus> findTopByConnectionIdOrderByLastBackupTimeDesc(Long connectionId);
    List<BackupStatus> findByConnectionIdOrderByLastBackupTimeDesc(Long connectionId);
    List<BackupStatus> findByBackupStatus(String status);
    List<BackupStatus> findByLastBackupTimeBefore(LocalDateTime before);
}
