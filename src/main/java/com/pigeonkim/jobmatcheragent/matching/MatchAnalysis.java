package com.pigeonkim.jobmatcheragent.matching;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Claude structured output(JSON schema)로 강제되는 매칭 분석 결과.
 *
 * <p>이 record가 곧 Claude에 보내는 JSON 스키마다. anthropic-java SDK의
 * {@code outputConfig(MatchAnalysis.class)}가 스키마를 자동 도출하고 응답을
 * 이 타입으로 검증·파싱한다. 정규식 strip + Map 캐스팅(B2)이 사라진다.
 */
public record MatchAnalysis(
        @JsonPropertyDescription("일치하는 키워드들 (쉼표로 구분)")
        String matchedKeywords,

        @JsonPropertyDescription("자격요건 충족 여부 분석 (2-3문장)")
        String requirementAnalysis,

        @JsonPropertyDescription("기술 매칭 점수 (0-100)")
        int techScore,

        @JsonPropertyDescription("경력 매칭 점수 (0-100)")
        int experienceScore,

        @JsonPropertyDescription("선호도 점수 (0-100)")
        int preferenceScore,

        @JsonPropertyDescription("종합 점수 (0-100)")
        int score,

        @JsonPropertyDescription("우려사항 (1-2문장)")
        String riskFactors,

        @JsonPropertyDescription("자소서에 강조할 키워드들 (쉼표로 구분)")
        String coverLetterKeywords,

        @JsonPropertyDescription("종합 요약 및 지원 여부 추천")
        String summary
) {
}
