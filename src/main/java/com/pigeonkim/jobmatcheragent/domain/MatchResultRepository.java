package com.pigeonkim.jobmatcheragent.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {
    // 나중에 필요한 메서드 추가 예정
}