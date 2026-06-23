package com.pigeonkim.jobmatcheragent.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
// 공고(matchResult)당 피드백은 1개만 — 같은 공고에 또 누르면 교체(upsert) (eng-review #2)
@Table(name = "feedback_log",
        uniqueConstraints = @UniqueConstraint(columnNames = "match_result_id"))
@Getter
@NoArgsConstructor
public class FeedbackLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_result_id")
    private MatchResult matchResult;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackType feedbackType;

    @Column(columnDefinition = "TEXT")
    private String memo; // 선택적 메모

    @CreationTimestamp
    private LocalDateTime createdAt;

    public FeedbackLog(MatchResult matchResult, FeedbackType feedbackType, String memo) {
        this.matchResult = matchResult;
        this.feedbackType = feedbackType;
        this.memo = memo;
    }

    /** 기존 피드백의 선택을 바꾼다 (upsert 시 사용). */
    public void changeType(FeedbackType feedbackType, String memo) {
        this.feedbackType = feedbackType;
        this.memo = memo;
    }
}