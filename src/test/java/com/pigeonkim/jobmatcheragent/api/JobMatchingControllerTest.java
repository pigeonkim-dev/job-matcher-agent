package com.pigeonkim.jobmatcheragent.api;

import com.pigeonkim.jobmatcheragent.crawler.WantedCrawler;
import com.pigeonkim.jobmatcheragent.domain.*;
import com.pigeonkim.jobmatcheragent.matching.AnalysisOutcome;
import com.pigeonkim.jobmatcheragent.matching.FeedbackKeywords;
import com.pigeonkim.jobmatcheragent.matching.MatchAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 컨트롤러 웹 슬라이스 테스트 — /analyze가 reanalyzed 플래그로 분석/건너뜀을 정확히 센다.
 * (eng-review: 기존 getAnalysisReason() != null 카운팅 오분류 교정 검증)
 */
@WebMvcTest(JobMatchingController.class)
class JobMatchingControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean MatchAnalysisService matchAnalysisService;
    @MockitoBean UserProfileRepository userProfileRepository;
    @MockitoBean JobPostingRepository jobPostingRepository;
    @MockitoBean MatchResultRepository matchResultRepository;
    @MockitoBean WantedCrawler wantedCrawler;

    @Test
    void analyze_counts_reanalyzed_vs_skipped_correctly() throws Exception {
        when(userProfileRepository.findById(1L)).thenReturn(java.util.Optional.of(new UserProfile()));
        JobPosting p1 = new JobPosting(); p1.setId(1L);
        JobPosting p2 = new JobPosting(); p2.setId(2L);
        JobPosting p3 = new JobPosting(); p3.setId(3L);
        when(jobPostingRepository.findAll()).thenReturn(List.of(p1, p2, p3));
        when(matchAnalysisService.loadFeedbackKeywords())
                .thenReturn(new FeedbackKeywords("없음", "없음"));

        // p1 신규(분석), p2 변경(분석), p3 skip — skip은 reason이 있어도 reanalyzed=false
        MatchResult analyzed = new MatchResult(); analyzed.setAnalysisReason("신규 공고");
        MatchResult skipped = new MatchResult(); skipped.setAnalysisReason("신규 공고");
        when(matchAnalysisService.analyzeIfNeeded(any(), eq(p1), any()))
                .thenReturn(new AnalysisOutcome(analyzed, true));
        when(matchAnalysisService.analyzeIfNeeded(any(), eq(p2), any()))
                .thenReturn(new AnalysisOutcome(analyzed, true));
        when(matchAnalysisService.analyzeIfNeeded(any(), eq(p3), any()))
                .thenReturn(new AnalysisOutcome(skipped, false));

        mvc.perform(post("/api/analyze"))
                .andExpect(status().isOk())
                .andExpect(content().string("분석: 2건 / 건너뜀: 1건 / 실패: 0건"));
    }

    @Test
    void analyze_isolates_failures_and_continues() throws Exception {
        when(userProfileRepository.findById(1L)).thenReturn(java.util.Optional.of(new UserProfile()));
        JobPosting p1 = new JobPosting(); p1.setId(1L);
        JobPosting p2 = new JobPosting(); p2.setId(2L);
        when(jobPostingRepository.findAll()).thenReturn(List.of(p1, p2));
        when(matchAnalysisService.loadFeedbackKeywords())
                .thenReturn(new FeedbackKeywords("없음", "없음"));

        MatchResult ok = new MatchResult(); ok.setAnalysisReason("신규 공고");
        // p1은 분석 실패(예외), p2는 성공 — 한 건 실패해도 배치는 계속돼야 함
        when(matchAnalysisService.analyzeIfNeeded(any(), eq(p1), any()))
                .thenThrow(new RuntimeException("Claude 호출 실패"));
        when(matchAnalysisService.analyzeIfNeeded(any(), eq(p2), any()))
                .thenReturn(new AnalysisOutcome(ok, true));

        mvc.perform(post("/api/analyze"))
                .andExpect(status().isOk())
                .andExpect(content().string("분석: 1건 / 건너뜀: 0건 / 실패: 1건"));
    }

    @Test
    void crawl_isolates_failures_and_continues() throws Exception {
        // 첫 키워드만 url 2개 반환, 나머지는 빈 목록
        when(wantedCrawler.searchJobUrls("Spring Boot")).thenReturn(List.of("ok", "bad"));
        when(wantedCrawler.searchJobUrls("Java 백엔드")).thenReturn(List.of());
        when(wantedCrawler.searchJobUrls("서버 개발자 Java")).thenReturn(List.of());
        when(wantedCrawler.parseJobPosting("ok")).thenReturn(new JobPosting());
        when(wantedCrawler.parseJobPosting("bad")).thenThrow(new RuntimeException("크롤링 실패"));

        mvc.perform(post("/api/crawl"))
                .andExpect(status().isOk())
                .andExpect(content().string("1건 수집 완료 / 실패: 1건"));
    }

    @Test
    void crawl_isolates_search_failure_per_keyword() throws Exception {
        // 첫 키워드 검색이 실패해도 나머지 키워드는 계속 진행돼야 함 (eng-review #5)
        when(wantedCrawler.searchJobUrls("Spring Boot"))
                .thenThrow(new RuntimeException("검색 API 장애"));
        when(wantedCrawler.searchJobUrls("Java 백엔드")).thenReturn(List.of("ok"));
        when(wantedCrawler.searchJobUrls("서버 개발자 Java")).thenReturn(List.of());
        when(wantedCrawler.parseJobPosting("ok")).thenReturn(new JobPosting());

        // 검색 1건 실패(failed=1) + 수집 1건 성공 → 500 안 나고 정상 응답
        mvc.perform(post("/api/crawl"))
                .andExpect(status().isOk())
                .andExpect(content().string("1건 수집 완료 / 실패: 1건"));
    }
}
