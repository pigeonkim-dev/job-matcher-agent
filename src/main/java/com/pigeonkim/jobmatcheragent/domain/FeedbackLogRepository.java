package com.pigeonkim.jobmatcheragent.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedbackLogRepository extends JpaRepository<FeedbackLog, Long> {
    @Query("SELECT f FROM FeedbackLog f JOIN FETCH f.matchResult WHERE f.feedbackType = :feedbackType")
    List<FeedbackLog> findByFeedbackTypeWithMatchResult(@Param("feedbackType") FeedbackType feedbackType);

    // 공고(matchResult)당 피드백 1개 — upsert 및 현재 선택 조회용
    Optional<FeedbackLog> findByMatchResultId(Long matchResultId);
}