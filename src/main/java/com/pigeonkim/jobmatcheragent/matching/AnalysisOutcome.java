package com.pigeonkim.jobmatcheragent.matching;

import com.pigeonkim.jobmatcheragent.domain.MatchResult;

/**
 * 스마트 재분석 결과.
 *
 * @param result     매칭 결과 (재분석 시 신규, skip 시 기존)
 * @param reanalyzed true면 이번에 새로 분석함, false면 변경 없어 건너뜀
 *
 * <p>{@code result.getAnalysisReason()} 으로 분석/건너뜀을 구분하던 기존 로직은
 * skip 시 기존 행의 reason이 남아 있어 오분류됐다. 명시적 플래그로 교정. (eng-review)
 */
public record AnalysisOutcome(MatchResult result, boolean reanalyzed) {
}
