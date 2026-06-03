package com.pigeonkim.jobmatcheragent.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "match_result", uniqueConstraints = {@UniqueConstraint(columnNames = "job_posting_id")})
@Getter
@Setter
@NoArgsConstructor
public class MatchResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userProfileId;

    private  Long jobPostingId;

    @Column(columnDefinition = "TEXT")
    private  String matchedKeywords;

    @Column(columnDefinition = "TEXT")
    private  String requirementAnalysis;

    private  Integer score;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String analysisReason; // "신규 공고" / "프로필 변경"
}
