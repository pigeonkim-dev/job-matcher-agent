package com.pigeonkim.jobmatcheragent.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {
    // 나중에 필요한 메서드 추가 예정
    Optional<MatchResult> findFirstByJobPostingIdOrderByCreatedAtDesc(Long jobPostingId);

}