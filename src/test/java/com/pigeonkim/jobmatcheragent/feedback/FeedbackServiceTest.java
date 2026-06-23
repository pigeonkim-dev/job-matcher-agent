package com.pigeonkim.jobmatcheragent.feedback;

import com.pigeonkim.jobmatcheragent.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 피드백 저장 upsert 테스트 (eng-review #2) — 공고당 1개, 다시 누르면 교체.
 * getFeedbackByType 파라미터 버그 수정도 검증.
 */
@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock FeedbackLogRepository feedbackLogRepository;
    @Mock MatchResultRepository matchResultRepository;

    FeedbackService service() {
        return new FeedbackService(feedbackLogRepository, matchResultRepository);
    }

    @Test
    void firstFeedback_creates_new() {
        MatchResult mr = new MatchResult();
        when(matchResultRepository.findById(1L)).thenReturn(Optional.of(mr));
        when(feedbackLogRepository.findByMatchResultId(1L)).thenReturn(Optional.empty());
        when(feedbackLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FeedbackLog saved = service().saveFeedback(1L, FeedbackType.INTERESTED, null);

        assertThat(saved.getFeedbackType()).isEqualTo(FeedbackType.INTERESTED);
        verify(feedbackLogRepository).save(any());
    }

    @Test
    void secondFeedback_replaces_existing_not_duplicate() {
        MatchResult mr = new MatchResult();
        FeedbackLog existing = new FeedbackLog(mr, FeedbackType.INTERESTED, null);
        when(matchResultRepository.findById(1L)).thenReturn(Optional.of(mr));
        when(feedbackLogRepository.findByMatchResultId(1L)).thenReturn(Optional.of(existing));
        when(feedbackLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 같은 공고에 다른 선택 → 기존 행을 교체(같은 인스턴스), 새로 만들지 않음
        FeedbackLog saved = service().saveFeedback(1L, FeedbackType.APPLIED, "지원 완료");

        assertThat(saved).isSameAs(existing);
        assertThat(saved.getFeedbackType()).isEqualTo(FeedbackType.APPLIED);
        assertThat(saved.getMemo()).isEqualTo("지원 완료");
    }

    @Test
    void getFeedbackByType_uses_the_requested_type() {
        // 기존 버그: 항상 NOT_INTERESTED만 조회 → 파라미터대로 조회하는지 확인
        service().getFeedbackByType(FeedbackType.APPLIED);
        verify(feedbackLogRepository).findByFeedbackTypeWithMatchResult(FeedbackType.APPLIED);
        verify(feedbackLogRepository, never())
                .findByFeedbackTypeWithMatchResult(FeedbackType.NOT_INTERESTED);
    }
}
