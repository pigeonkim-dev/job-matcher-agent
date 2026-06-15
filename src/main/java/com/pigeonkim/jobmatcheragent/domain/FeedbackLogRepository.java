package com.pigeonkim.jobmatcheragent.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedbackLogRepository extends JpaRepository<FeedbackLog, Long> {
    @Query("SELECT f FROM FeedbackLog f JOIN FETCH f.matchResult WHERE f.feedbackType = :feedbackType")
    List<FeedbackLog> findByFeedbackTypeWithMatchResult(@Param("feedbackType") FeedbackType feedbackType);
}