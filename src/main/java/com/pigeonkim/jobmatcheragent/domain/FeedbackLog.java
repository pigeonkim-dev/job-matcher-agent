package com.pigeonkim.jobmatcheragent.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback_log")
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
}