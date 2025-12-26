package com.dbmonitor.repository;

import com.dbmonitor.model.ReplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReplicationStatusRepository extends JpaRepository<ReplicationStatus, Long> {
    Optional<ReplicationStatus> findTopByConnectionIdOrderByLastCheckedDesc(Long connectionId);
    List<ReplicationStatus> findByConnectionId(Long connectionId);
    List<ReplicationStatus> findByReplicationState(String state);
    List<ReplicationStatus> findByLagSecondsGreaterThan(Long lagSeconds);
}
