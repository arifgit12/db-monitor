package com.dbmonitor.repository;

import com.dbmonitor.model.IndexRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndexRecommendationRepository extends JpaRepository<IndexRecommendation, Long> {
    List<IndexRecommendation> findByConnectionId(Long connectionId);
    List<IndexRecommendation> findByStatus(String status);
    List<IndexRecommendation> findByConnectionIdAndStatusOrderByImpactScoreDesc(Long connectionId, String status);
    List<IndexRecommendation> findByTableName(String tableName);
}
