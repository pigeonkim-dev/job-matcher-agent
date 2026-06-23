package com.pigeonkim.jobmatcheragent.matching;

import com.pigeonkim.jobmatcheragent.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 매칭 분석의 영속 단위 + 스마트 재분석 오케스트레이션.
 *
 * <pre>
 *  Controller ─▶ analyzeIfNeeded(@Transactional, 프록시 진입)
 *                  ├─ 신규/프로필변경 → reanalyze: delete(기존) + engine.analyze + save(reason 포함, 1회)
 *                  └─ 무변경         → 기존 결과 반환(skip)
 * </pre>
 *
 * <p>왜 별도 빈인가: 트랜잭션 경계를 프록시로 진입시키기 위해서다.
 * 이전 {@code MatchingEngine.analyzeWithReason}는 private + self-invocation이라
 * {@code @Transactional}이 적용되지 않았다(AOP 프록시 우회). delete→insert 사이
 * 실패 시 정합성이 깨질 수 있었다. (eng-review B1)
 *
 * <p>{@link MatchingEngine}은 Claude 호출 + 결과 매핑만 담당(순수)하고,
 * 영속화/트랜잭션/피드백 조회는 이 서비스가 책임진다.
 */
@Service
public class MatchAnalysisService {

    private final MatchingEngine matchingEngine;
    private final MatchResultRepository matchResultRepository;
    private final FeedbackLogRepository feedbackLogRepository;

    public MatchAnalysisService(MatchingEngine matchingEngine,
                                MatchResultRepository matchResultRepository,
                                FeedbackLogRepository feedbackLogRepository) {
        this.matchingEngine = matchingEngine;
        this.matchResultRepository = matchResultRepository;
        this.feedbackLogRepository = feedbackLogRepository;
    }

    /**
     * 피드백 키워드를 1회 조회한다. 분석 루프 시작 전에 호출해 결과를 재사용할 것.
     * (공고마다 호출하면 2N 쿼리 — eng-review P1)
     */
    public FeedbackKeywords loadFeedbackKeywords() {
        String interested = keywordsOf(FeedbackType.INTERESTED);
        String notInterested = keywordsOf(FeedbackType.NOT_INTERESTED);
        return new FeedbackKeywords(
                interested.isEmpty() ? "없음" : interested,
                notInterested.isEmpty() ? "없음" : notInterested);
    }

    private String keywordsOf(FeedbackType type) {
        List<FeedbackLog> logs = feedbackLogRepository.findByFeedbackTypeWithMatchResult(type);
        return logs.stream()
                .map(f -> f.getMatchResult().getMatchedKeywords())
                .filter(k -> k != null && !k.isBlank())
                .collect(Collectors.joining(", "));
    }

    /**
     * 신규 공고이거나 프로필이 마지막 분석 이후 변경됐을 때만 재분석한다.
     * delete(기존) + analyze + save를 한 트랜잭션으로 묶는다.
     */
    @Transactional
    public AnalysisOutcome analyzeIfNeeded(UserProfile profile, JobPosting posting,
                                           FeedbackKeywords feedback) {
        Optional<MatchResult> existing = matchResultRepository
                .findFirstByJobPostingIdOrderByCreatedAtDesc(posting.getId());

        String reason;
        if (existing.isEmpty()) {
            reason = "신규 공고";
        } else if (profile.getUpdatedAt() != null
                && profile.getUpdatedAt().isAfter(existing.get().getCreatedAt())) {
            reason = "프로필 변경";
        } else {
            return new AnalysisOutcome(existing.get(), false); // 변경 없음 → skip
        }

        // 기존 결과 삭제 후 새로 분석 (unique 제약: job_posting_id)
        existing.ifPresent(matchResultRepository::delete);

        MatchResult result = matchingEngine.analyze(profile, posting, feedback);
        result.setAnalysisReason(reason);          // 단일 save 전에 reason 세팅 (eng-review: 이중 save 제거)
        return new AnalysisOutcome(matchResultRepository.save(result), true);
    }
}
