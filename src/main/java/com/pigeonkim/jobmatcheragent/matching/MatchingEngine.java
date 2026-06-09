package com.pigeonkim.jobmatcheragent.matching;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pigeonkim.jobmatcheragent.claude.ClaudeClient;
import com.pigeonkim.jobmatcheragent.domain.*;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.pigeonkim.jobmatcheragent.domain.FeedbackLog;
import com.pigeonkim.jobmatcheragent.domain.FeedbackLogRepository;
import com.pigeonkim.jobmatcheragent.domain.FeedbackType;

@Service
public class MatchingEngine {

    private final ClaudeClient claudeClient;
    private final MatchResultRepository matchResultRepository;
    private final FeedbackLogRepository feedbackLogRepository;

    public MatchingEngine(ClaudeClient claudeClient, MatchResultRepository matchResultRepository,
                          FeedbackLogRepository feedbackLogRepository) {
        this.claudeClient = claudeClient;
        this.matchResultRepository = matchResultRepository;
        this.feedbackLogRepository = feedbackLogRepository;
    }

    public MatchResult analyze(UserProfile userProfile, JobPosting jobPosting) throws Exception {
        String prompt = buildPrompt(userProfile, jobPosting);
        String response = claudeClient.sendMessage(prompt);

        // Claude가 ```json ... ``` 으로 감쌀 때 제거
        String cleanResponse = response
                .replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> parsed = objectMapper.readValue(cleanResponse,
                new TypeReference<Map<String, Object>>() {
                });

        MatchResult result = new MatchResult();
        result.setUserProfileId(userProfile.getId());
        result.setJobPostingId(jobPosting.getId());
        result.setMatchedKeywords((String) parsed.get("matchedKeywords"));
        result.setRequirementAnalysis((String) parsed.get("requirementAnalysis"));
        result.setScore((Integer) parsed.get("score"));
        result.setSummary((String) parsed.get("summary"));

        return matchResultRepository.save(result);
    }

    private String buildPrompt(UserProfile userProfile, JobPosting jobPosting) {
        // 피드백 이력 조회
        List<FeedbackLog> interested = feedbackLogRepository.findByFeedbackType(FeedbackType.INTERESTED);
        List<FeedbackLog> notInterested = feedbackLogRepository.findByFeedbackType(FeedbackType.NOT_INTERESTED);

        String interestedSummary = interested.stream()
                .map(f -> f.getMatchResult().getJobPostingId().toString())
                .collect(Collectors.joining(", "));

        String notInterestedSummary = notInterested.stream()
                .map(f -> f.getMatchResult().getJobPostingId().toString())
                .collect(Collectors.joining(", "));

        return String.format("""
                    아래 개발자 프로필과 채용 공고를 분석하고 반드시 JSON 형식으로만 답하세요.
                    다른 설명 없이 JSON만 출력하세요.
                    
                    [개발자 프로필]
                    경력 및 소개: %s
                    선호 카테고리: %s
                    기피 키워드: %s
                    
                    [사용자 피드백 이력]
                    관심있음 공고 ID: %s
                    관심없음 공고 ID: %s
                    (위 피드백 이력을 참고해서 사용자 성향을 반영한 점수를 매겨주세요)
                    
                    [채용 공고]
                    제목: %s
                    회사: %s
                    내용: %s
                    
                    아래 JSON 형식으로만 답하세요:
                    {
                      "matchedKeywords": "일치하는 키워드들 (쉼표로 구분)",
                      "requirementAnalysis": "자격요건 충족 여부 분석 (2-3문장)",
                      "score": 75,
                      "summary": "종합 요약 및 지원 여부 추천"
                    }
                    """,
                userProfile.getResumeContent(),
                userProfile.getPreferredCategories(),
                userProfile.getAvoidKeywords(),
                interestedSummary.isEmpty() ? "없음" : interestedSummary,
                notInterestedSummary.isEmpty() ? "없음" : notInterestedSummary,
                jobPosting.getTitle(),
                jobPosting.getCompany(),
                jobPosting.getDescription()
        );
    }

    public MatchResult analyzeIfNeeded(UserProfile userProfile, JobPosting jobPosting) throws Exception {

        Optional<MatchResult> existing = matchResultRepository
                .findFirstByJobPostingIdOrderByCreatedAtDesc(jobPosting.getId());

        // ① 기존 분석 없음 → 신규 공고
        if (existing.isEmpty()) {
            return analyzeWithReason(userProfile, jobPosting, "신규 공고");
        }

        MatchResult prev = existing.get();

        // ② 프로필이 마지막 분석 이후 수정됨
        if (userProfile.getUpdatedAt() != null &&
                userProfile.getUpdatedAt().isAfter(prev.getCreatedAt())) {
            return analyzeWithReason(userProfile, jobPosting, "프로필 변경");
        }

        // ③ 변경 없음 → 건너뜀
        return prev;
    }

    private MatchResult analyzeWithReason(UserProfile userProfile, JobPosting jobPosting, String reason) throws Exception {
        // 기존 결과 삭제 후 새로 분석
        matchResultRepository.findFirstByJobPostingIdOrderByCreatedAtDesc(jobPosting.getId())
                .ifPresent(matchResultRepository::delete);

        MatchResult result = analyze(userProfile, jobPosting);
        result.setAnalysisReason(reason);
        return matchResultRepository.save(result);
    }
}