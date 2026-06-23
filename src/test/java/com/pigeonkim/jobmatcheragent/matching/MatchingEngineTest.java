package com.pigeonkim.jobmatcheragent.matching;

import com.pigeonkim.jobmatcheragent.claude.ClaudeClient;
import com.pigeonkim.jobmatcheragent.domain.JobPosting;
import com.pigeonkim.jobmatcheragent.domain.MatchResult;
import com.pigeonkim.jobmatcheragent.domain.UserProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * MatchingEngine 단위 테스트 — Claude는 모킹.
 * structured output(MatchAnalysis)을 MatchResult로 정확히 매핑하는지 검증.
 */
@ExtendWith(MockitoExtension.class)
class MatchingEngineTest {

    @Mock
    ClaudeClient claudeClient;

    @Test
    void analyze_maps_all_fields_from_structured_output() {
        MatchAnalysis stub = new MatchAnalysis(
                "Java, Spring", "자격요건 대부분 충족", 80, 70, 90, 82,
                "C# 경력 비중 우려", "Spring Boot, MSA", "지원 추천");
        when(claudeClient.sendStructured(any(), eq(MatchAnalysis.class))).thenReturn(stub);

        MatchingEngine engine = new MatchingEngine(claudeClient);
        UserProfile profile = new UserProfile();
        profile.setId(1L);
        JobPosting posting = new JobPosting();
        posting.setId(42L);
        posting.setTitle("백엔드 개발자");
        posting.setCompany("Acme");
        posting.setDescription("Spring Boot 경험");

        MatchResult result = engine.analyze(profile, posting,
                new FeedbackKeywords("Java", "PHP"));

        assertThat(result.getUserProfileId()).isEqualTo(1L);
        assertThat(result.getJobPostingId()).isEqualTo(42L);
        assertThat(result.getMatchedKeywords()).isEqualTo("Java, Spring");
        assertThat(result.getScore()).isEqualTo(82);
        assertThat(result.getTechScore()).isEqualTo(80);
        assertThat(result.getExperienceScore()).isEqualTo(70);
        assertThat(result.getPreferenceScore()).isEqualTo(90);
        assertThat(result.getRiskFactors()).isEqualTo("C# 경력 비중 우려");
        assertThat(result.getCoverLetterKeywords()).isEqualTo("Spring Boot, MSA");
        assertThat(result.getSummary()).isEqualTo("지원 추천");
        // analyze는 저장하지 않는다 — analysisReason은 서비스가 채운다
        assertThat(result.getAnalysisReason()).isNull();
    }

    @Test
    void analyze_passes_feedback_keywords_into_prompt() {
        when(claudeClient.sendStructured(any(), eq(MatchAnalysis.class)))
                .thenReturn(new MatchAnalysis("", "", 0, 0, 0, 0, "", "", ""));

        MatchingEngine engine = new MatchingEngine(claudeClient);
        UserProfile profile = new UserProfile();
        JobPosting posting = new JobPosting();
        posting.setTitle("t");
        posting.setCompany("c");
        posting.setDescription("d");

        engine.analyze(profile, posting, new FeedbackKeywords("관심키워드X", "비관심키워드Y"));

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(claudeClient)
                .sendStructured(prompt.capture(), eq(MatchAnalysis.class));
        assertThat(prompt.getValue()).contains("관심키워드X").contains("비관심키워드Y");
    }
}
