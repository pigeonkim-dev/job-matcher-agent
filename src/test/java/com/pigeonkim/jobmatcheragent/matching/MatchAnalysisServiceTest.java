package com.pigeonkim.jobmatcheragent.matching;

import com.pigeonkim.jobmatcheragent.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * MatchAnalysisService 단위 테스트 — 스마트 재분석 3분기 + 단일 save + reanalyzed 플래그.
 *
 * <p>회귀 IRON RULE 대상: analyzeIfNeeded의 신규/프로필변경/skip 분기와
 * "reason 포함 단일 save", "skip은 reanalyzed=false"는 eng-review가 교정한
 * 핵심 동작이므로 회귀로 봉인한다.
 */
@ExtendWith(MockitoExtension.class)
class MatchAnalysisServiceTest {

    @Mock MatchingEngine matchingEngine;
    @Mock MatchResultRepository matchResultRepository;
    @Mock FeedbackLogRepository feedbackLogRepository;

    MatchAnalysisService service() {
        return new MatchAnalysisService(matchingEngine, matchResultRepository, feedbackLogRepository);
    }

    private MatchResult freshResult() {
        MatchResult r = new MatchResult();
        // save가 인자를 그대로 돌려주도록
        return r;
    }

    @Test
    void newPosting_reanalyzes_and_saves_once_with_reason() {
        when(matchResultRepository.findFirstByJobPostingIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(Optional.empty());
        when(matchingEngine.analyze(any(), any(), any())).thenReturn(freshResult());
        when(matchResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserProfile profile = new UserProfile();
        JobPosting posting = new JobPosting();
        posting.setId(1L);

        AnalysisOutcome outcome = service().analyzeIfNeeded(profile, posting,
                new FeedbackKeywords("없음", "없음"));

        assertThat(outcome.reanalyzed()).isTrue();
        assertThat(outcome.result().getAnalysisReason()).isEqualTo("신규 공고");
        // 저장은 정확히 1회, reason이 채워진 상태로
        ArgumentCaptor<MatchResult> saved = ArgumentCaptor.forClass(MatchResult.class);
        verify(matchResultRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getAnalysisReason()).isEqualTo("신규 공고");
        verify(matchResultRepository, never()).delete(any());
    }

    @Test
    void profileChanged_deletes_old_then_reanalyzes() {
        MatchResult prev = new MatchResult();
        prev.setAnalysisReason("신규 공고");
        // 기존 분석은 과거, 프로필은 그 이후 수정
        LocalDateTime past = LocalDateTime.now().minusDays(1);
        // createdAt은 @CreationTimestamp라 직접 못 넣음 → 리플렉션 대신 prev mock 동작으로 우회
        MatchResult prevSpy = spy(prev);
        doReturn(past).when(prevSpy).getCreatedAt();

        when(matchResultRepository.findFirstByJobPostingIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(Optional.of(prevSpy));
        when(matchingEngine.analyze(any(), any(), any())).thenReturn(freshResult());
        when(matchResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserProfile profile = new UserProfile();
        profile.setUpdatedAt(LocalDateTime.now()); // past 이후
        JobPosting posting = new JobPosting();
        posting.setId(1L);

        AnalysisOutcome outcome = service().analyzeIfNeeded(profile, posting,
                new FeedbackKeywords("없음", "없음"));

        assertThat(outcome.reanalyzed()).isTrue();
        assertThat(outcome.result().getAnalysisReason()).isEqualTo("프로필 변경");
        verify(matchResultRepository).delete(prevSpy);
        verify(matchResultRepository, times(1)).save(any());
    }

    @Test
    void noChange_skips_without_analyze_or_save() {
        MatchResult prev = new MatchResult();
        prev.setAnalysisReason("신규 공고");
        MatchResult prevSpy = spy(prev);
        doReturn(LocalDateTime.now()).when(prevSpy).getCreatedAt();

        when(matchResultRepository.findFirstByJobPostingIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(Optional.of(prevSpy));

        UserProfile profile = new UserProfile();
        profile.setUpdatedAt(LocalDateTime.now().minusDays(2)); // createdAt보다 과거 → 변경 없음
        JobPosting posting = new JobPosting();
        posting.setId(1L);

        AnalysisOutcome outcome = service().analyzeIfNeeded(profile, posting,
                new FeedbackKeywords("없음", "없음"));

        assertThat(outcome.reanalyzed()).isFalse(); // skip이 분석으로 오분류되지 않음
        assertThat(outcome.result()).isSameAs(prevSpy);
        verify(matchingEngine, never()).analyze(any(), any(), any());
        verify(matchResultRepository, never()).save(any());
        verify(matchResultRepository, never()).delete(any());
    }

    @Test
    void loadFeedbackKeywords_joins_keywords_and_falls_back_to_none() {
        MatchResult mr = new MatchResult();
        mr.setMatchedKeywords("Java, Spring");
        FeedbackLog log = new FeedbackLog(mr, FeedbackType.INTERESTED, null);

        when(feedbackLogRepository.findByFeedbackTypeWithMatchResult(FeedbackType.INTERESTED))
                .thenReturn(List.of(log));
        when(feedbackLogRepository.findByFeedbackTypeWithMatchResult(FeedbackType.NOT_INTERESTED))
                .thenReturn(List.of());

        FeedbackKeywords fk = service().loadFeedbackKeywords();

        assertThat(fk.interested()).isEqualTo("Java, Spring");
        assertThat(fk.notInterested()).isEqualTo("없음");
        // 각 타입당 정확히 1회 — 루프 밖 1회 조회 보장 (eng-review P1)
        verify(feedbackLogRepository, times(1))
                .findByFeedbackTypeWithMatchResult(FeedbackType.INTERESTED);
        verify(feedbackLogRepository, times(1))
                .findByFeedbackTypeWithMatchResult(FeedbackType.NOT_INTERESTED);
    }
}
