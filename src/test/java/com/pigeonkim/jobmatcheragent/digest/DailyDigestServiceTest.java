package com.pigeonkim.jobmatcheragent.digest;

import com.pigeonkim.jobmatcheragent.domain.JobPosting;
import com.pigeonkim.jobmatcheragent.domain.MatchResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * selectTop 정렬 회귀 테스트 — score null이 섞여도 NPE 없이 내림차순 상위 N.
 * (eng-review B3: 실버그 아닌 방어적 강화. nullsLast 명시가 회귀로 봉인되는지 확인)
 */
class DailyDigestServiceTest {

    private MatchResult withScore(Integer score) {
        MatchResult r = new MatchResult();
        r.setScore(score);
        return r;
    }

    @Test
    void selectTop_excludes_null_and_sorts_desc_without_npe() {
        List<MatchResult> input = new ArrayList<>(Arrays.asList(
                withScore(50), withScore(null), withScore(90), withScore(null), withScore(70)));

        List<MatchResult> top = DailyDigestService.selectTop(input, 5);

        assertThat(top).extracting(MatchResult::getScore)
                .containsExactly(90, 70, 50); // null 제외 + 내림차순
    }

    @Test
    void selectTop_handles_empty_and_all_null() {
        assertThatCode(() -> {
            assertThat(DailyDigestService.selectTop(List.of(), 5)).isEmpty();
            assertThat(DailyDigestService.selectTop(
                    Arrays.asList(withScore(null), withScore(null)), 5)).isEmpty();
        }).doesNotThrowAnyException();
    }

    @Test
    void selectTop_respects_limit() {
        List<MatchResult> input = Arrays.asList(
                withScore(10), withScore(20), withScore(30), withScore(40), withScore(50), withScore(60));
        assertThat(DailyDigestService.selectTop(input, 5))
                .extracting(MatchResult::getScore)
                .containsExactly(60, 50, 40, 30, 20);
    }

    @Test
    void buildMessage_empty_shows_placeholder() {
        String msg = DailyDigestService.buildMessage(List.of(), id -> new JobPosting());
        assertThat(msg).contains("분석된 공고가 없습니다.");
    }

    @Test
    void buildMessage_renders_each_posting_with_lookup() {
        MatchResult r = withScore(88);
        r.setJobPostingId(7L);
        r.setMatchedKeywords("Java, Spring");
        r.setSummary("지원 추천");

        JobPosting p = new JobPosting();
        p.setCompany("Acme");
        p.setTitle("백엔드 개발자");
        p.setUrl("https://example.com/wd/7");
        Function<Long, JobPosting> lookup = id -> id == 7L ? p : new JobPosting();

        String msg = DailyDigestService.buildMessage(List.of(r), lookup);

        assertThat(msg)
                .contains("오늘의 TOP 1")
                .contains("Acme").contains("백엔드 개발자")
                .contains("88점").contains("Java, Spring")
                .contains("지원 추천").contains("https://example.com/wd/7");
    }

    @Test
    void buildMessage_falls_back_to_empty_posting_when_lookup_misses() {
        MatchResult r = withScore(50);
        r.setJobPostingId(999L);
        // lookup이 빈 JobPosting 반환 → company/title null이어도 NPE 없이 렌더
        assertThatCode(() -> DailyDigestService.buildMessage(List.of(r), id -> new JobPosting()))
                .doesNotThrowAnyException();
    }
}
