package com.pigeonkim.jobmatcheragent.feedback;

import com.pigeonkim.jobmatcheragent.domain.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackLogRepository feedbackLogRepository;
    private final MatchResultRepository matchResultRepository;

    public FeedbackLog saveFeedback(Long matchResultId, FeedbackType feedbackType, String memo) {
        MatchResult matchResult = matchResultRepository.findById(matchResultId)
                .orElseThrow(() -> new EntityNotFoundException("MatchResult not found: " + matchResultId));

        FeedbackLog feedbackLog = new FeedbackLog(matchResult, feedbackType, memo);
        return feedbackLogRepository.save(feedbackLog);
    }

    public List<FeedbackLog> getFeedbackByType(FeedbackType feedbackType) {
        return feedbackLogRepository.findByFeedbackType(feedbackType);
    }
}