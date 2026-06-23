package com.pigeonkim.jobmatcheragent.matching;

/**
 * 피드백 이력에서 추출한 관심/비관심 키워드 묶음.
 *
 * <p>분석 루프 전에 1회만 조회해 모든 공고 분석에 재사용한다.
 * (이전에는 공고마다 2회씩 DB를 조회 — 2N 쿼리 — 했다. eng-review P1)
 */
public record FeedbackKeywords(String interested, String notInterested) {
}
