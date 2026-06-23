package com.pigeonkim.jobmatcheragent.matching;

import com.pigeonkim.jobmatcheragent.claude.ClaudeClient;
import com.pigeonkim.jobmatcheragent.domain.JobPosting;
import com.pigeonkim.jobmatcheragent.domain.MatchResult;
import com.pigeonkim.jobmatcheragent.domain.UserProfile;
import org.springframework.stereotype.Service;

/**
 * Claude API 매칭 분석 — Claude 호출 + 결과 매핑만 담당하는 순수 컴포넌트.
 *
 * <p>영속화/트랜잭션/피드백 조회/스마트 재분석은 {@link MatchAnalysisService}가 책임진다.
 * 여기서 반환하는 {@link MatchResult}는 <b>저장되지 않은</b> 상태다.
 *
 * <p>응답은 SDK structured outputs로 {@link MatchAnalysis} 타입으로 강제 파싱된다
 * (정규식 strip + Map 캐스팅 제거 — Phase 7b B2).
 */
@Service
public class MatchingEngine {

    private final ClaudeClient claudeClient;

    public MatchingEngine(ClaudeClient claudeClient) {
        this.claudeClient = claudeClient;
    }

    /** 프로필·공고·피드백 키워드로 매칭을 분석한다. 결과는 저장하지 않고 반환한다. */
    public MatchResult analyze(UserProfile userProfile, JobPosting jobPosting,
                               FeedbackKeywords feedback) {
        String prompt = buildPrompt(userProfile, jobPosting, feedback);
        MatchAnalysis analysis = claudeClient.sendStructured(prompt, MatchAnalysis.class);

        MatchResult result = new MatchResult();
        result.setUserProfileId(userProfile.getId());
        result.setJobPostingId(jobPosting.getId());
        result.setMatchedKeywords(analysis.matchedKeywords());
        result.setRequirementAnalysis(analysis.requirementAnalysis());
        result.setSummary(analysis.summary());
        result.setScore(analysis.score());
        result.setTechScore(analysis.techScore());
        result.setExperienceScore(analysis.experienceScore());
        result.setPreferenceScore(analysis.preferenceScore());
        result.setRiskFactors(analysis.riskFactors());
        result.setCoverLetterKeywords(analysis.coverLetterKeywords());
        return result;
    }

    private String buildPrompt(UserProfile userProfile, JobPosting jobPosting,
                               FeedbackKeywords feedback) {
        return String.format("""
                아래 개발자 프로필과 채용 공고를 분석하세요.

                [개발자 프로필]
                경력 및 소개: %s
                관심 키워드: %s
                기피 키워드: %s

                [사용자 피드백 이력]
                관심있음 공고의 공통 키워드: %s
                관심없음 공고의 공통 키워드: %s
                (위 키워드 패턴을 참고해서 사용자 성향을 반영한 점수를 매겨주세요)

                [채용 공고]
                제목: %s
                회사: %s
                내용: %s
                """,
                userProfile.getResumeContent(),
                userProfile.getSearchKeywords(),
                userProfile.getAvoidKeywords(),
                feedback.interested(),
                feedback.notInterested(),
                jobPosting.getTitle(),
                jobPosting.getCompany(),
                jobPosting.getDescription()
        );
    }
}
