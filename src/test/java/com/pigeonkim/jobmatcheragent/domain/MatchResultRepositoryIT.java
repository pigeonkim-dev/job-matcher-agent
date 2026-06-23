package com.pigeonkim.jobmatcheragent.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MatchResultRepository 통합 테스트 — 실제 PostgreSQL(Testcontainers).
 *
 * <p>Spring Boot 4 → Testcontainers 2.0: @ServiceConnection + testcontainers-postgresql.
 * Docker 미설치 환경에서는 @BeforeAll의 assume으로 전체 스킵(머지 게이트 비파괴).
 *
 * <p>검증: findFirstByJobPostingIdOrderByCreatedAtDesc 정렬, unique(job_posting_id) 제약.
 */
@Testcontainers(disabledWithoutDocker = true) // Docker 없으면 전체 스킵 (머지 게이트 비파괴)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MatchResultRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    MatchResultRepository repository;

    private MatchResult result(long jobPostingId, int score) {
        MatchResult r = new MatchResult();
        r.setUserProfileId(1L);
        r.setJobPostingId(jobPostingId);
        r.setScore(score);
        r.setAnalysisReason("신규 공고");
        return r;
    }

    @Test
    void findFirstByJobPostingId_returns_latest() {
        repository.save(result(100L, 60));
        MatchResult saved = repository.save(result(101L, 80));

        var found = repository.findFirstByJobPostingIdOrderByCreatedAtDesc(101L);

        assertThat(found).isPresent();
        assertThat(found.get().getScore()).isEqualTo(80);
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void unique_constraint_on_job_posting_id_is_enforced() {
        repository.saveAndFlush(result(200L, 50));
        // 같은 job_posting_id로 두 번째 insert → unique 제약 위반
        assertThatThrownBy(() -> repository.saveAndFlush(result(200L, 90)))
                .isInstanceOf(Exception.class);
    }
}
