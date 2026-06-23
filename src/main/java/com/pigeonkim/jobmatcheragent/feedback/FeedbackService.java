package com.pigeonkim.jobmatcheragent.feedback;

import com.pigeonkim.jobmatcheragent.domain.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackLogRepository feedbackLogRepository;
    private final MatchResultRepository matchResultRepository;

    /**
     * 피드백 저장 — 공고당 1개. 이미 있으면 선택을 교체(upsert)한다. (eng-review #2)
     * find→수정→save를 한 트랜잭션으로 묶어 동시 클릭 시에도 일관되게 처리.
     */
    @Transactional
    public FeedbackLog saveFeedback(Long matchResultId, FeedbackType feedbackType, String memo) {
        MatchResult matchResult = matchResultRepository.findById(matchResultId)
                .orElseThrow(() -> new EntityNotFoundException("MatchResult not found: " + matchResultId));

        // 이미 이 공고에 피드백이 있으면 선택만 바꾸고, 없으면 새로 만든다
        FeedbackLog feedbackLog = feedbackLogRepository.findByMatchResultId(matchResultId)
                .orElseGet(() -> new FeedbackLog(matchResult, feedbackType, memo));
        feedbackLog.changeType(feedbackType, memo);

        return feedbackLogRepository.save(feedbackLog);
    }

    public List<FeedbackLog> getFeedbackByType(FeedbackType feedbackType) {
        // 기존 버그: 파라미터를 무시하고 항상 NOT_INTERESTED를 조회했음 → 파라미터를 쓰도록 수정
        return feedbackLogRepository.findByFeedbackTypeWithMatchResult(feedbackType);
    }
}