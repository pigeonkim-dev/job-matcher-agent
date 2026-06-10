package com.pigeonkim.jobmatcheragent.api;

import com.pigeonkim.jobmatcheragent.domain.MatchResult;
import com.pigeonkim.jobmatcheragent.domain.JobPosting;

public class MatchResultDto {
    public Long id;
    public Integer score;
    public Integer techScore;
    public Integer experienceScore;
    public Integer preferenceScore;
    public String matchedKeywords;
    public String requirementAnalysis;
    public String riskFactors;
    public String coverLetterKeywords;
    public String summary;
    public String analysisReason;
    public String jobTitle;
    public String company;
    public String jobUrl;

    public static MatchResultDto from(MatchResult r, JobPosting p) {
        MatchResultDto dto = new MatchResultDto();
        dto.id = r.getId();
        dto.score = r.getScore();
        dto.techScore = r.getTechScore();
        dto.experienceScore = r.getExperienceScore();
        dto.preferenceScore = r.getPreferenceScore();
        dto.matchedKeywords = r.getMatchedKeywords();
        dto.requirementAnalysis = r.getRequirementAnalysis();
        dto.riskFactors = r.getRiskFactors();
        dto.coverLetterKeywords = r.getCoverLetterKeywords();
        dto.summary = r.getSummary();
        dto.analysisReason = r.getAnalysisReason();
        dto.jobTitle = p.getTitle();
        dto.company = p.getCompany();
        dto.jobUrl = p.getUrl();
        return dto;
    }
}