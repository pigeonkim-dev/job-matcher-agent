package com.pigeonkim.jobmatcheragent;

import com.pigeonkim.jobmatcheragent.claude.ClaudeClient;
import com.pigeonkim.jobmatcheragent.domain.*;
import com.pigeonkim.jobmatcheragent.matching.MatchAnalysis;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 스모크 게이트(결정적) — analyze → results 플로우.
 *
 * <p>Claude만 스텁(structured output), 나머지는 실제 빈 + H2 DB.
 * 트랜잭션 경계(B1)·SDK 매핑(B2)·영속화·결과 조회가 한 번에 동작하는지 확인.
 * live 외부 API를 치지 않으므로 머지 게이트로 안전. (eng-review: 외부 API mock화)
 */
@SpringBootTest
@AutoConfigureMockMvc
class AnalyzeFlowSmokeIT {

    @MockitoBean
    ClaudeClient claudeClient;

    @Autowired MockMvc mvc;
    @Autowired UserProfileRepository profiles;
    @Autowired JobPostingRepository postings;
    @Autowired MatchResultRepository results;

    @Test
    void analyze_persists_match_and_results_endpoint_returns_it() throws Exception {
        // Claude 응답 스텁
        when(claudeClient.sendStructured(any(), eq(MatchAnalysis.class)))
                .thenReturn(new MatchAnalysis(
                        "Java, Spring", "충족", 80, 70, 90, 82,
                        "우려 없음", "Spring Boot", "지원 추천"));

        // 프로필(IDENTITY → 첫 행 id=1, 컨트롤러가 findById(1L) 사용)
        UserProfile profile = new UserProfile();
        profile.setResumeContent("12년차 백엔드");
        profile.setPreferredCategories("백엔드");
        profile.setAvoidKeywords("프론트");
        profiles.save(profile);

        JobPosting posting = new JobPosting();
        posting.setTitle("백엔드 개발자");
        posting.setCompany("Acme");
        posting.setDescription("Spring Boot 경험");
        posting.setUrl("https://example.com/wd/1");
        postings.save(posting);

        // 분석 실행 — 신규 공고 1건 분석
        mvc.perform(post("/api/analyze"))
                .andExpect(status().isOk());

        // 영속화 검증: reason 포함 단일 행
        assertThat(results.findAll()).hasSize(1);
        MatchResult saved = results.findAll().get(0);
        assertThat(saved.getScore()).isEqualTo(82);
        assertThat(saved.getAnalysisReason()).isEqualTo("신규 공고");
        assertThat(saved.getJobPostingId()).isEqualTo(posting.getId());

        // 결과 조회 엔드포인트
        mvc.perform(get("/api/results"))
                .andExpect(status().isOk());

        // 재실행 시 변경 없음 → 추가 분석/저장 없음(여전히 1행)
        mvc.perform(post("/api/analyze")).andExpect(status().isOk());
        assertThat(results.findAll()).hasSize(1);
    }
}
