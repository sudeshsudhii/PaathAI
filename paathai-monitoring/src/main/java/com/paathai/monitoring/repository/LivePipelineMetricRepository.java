package com.paathai.monitoring.repository;

import com.paathai.monitoring.model.LivePipelineMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LivePipelineMetricRepository extends JpaRepository<LivePipelineMetric, Long> {

    List<LivePipelineMetric> findBySessionIdOrderByCreatedAtDesc(Long sessionId);

    List<LivePipelineMetric> findBySessionIdAndPipelineStage(Long sessionId, String pipelineStage);
}
