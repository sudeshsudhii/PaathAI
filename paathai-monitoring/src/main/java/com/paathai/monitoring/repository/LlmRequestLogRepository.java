package com.paathai.monitoring.repository;

import com.paathai.monitoring.model.LlmRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for LLM request logs.
 */
@Repository
public interface LlmRequestLogRepository extends JpaRepository<LlmRequestLog, Long> {
}
