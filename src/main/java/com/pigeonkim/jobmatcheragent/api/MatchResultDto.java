package com.pigeonkim.jobmatcheragent.api;

import com.pigeonkim.jobmatcheragent.domain.MatchResult;
import com.pigeonkim.jobmatcheragent.domain.JobPosting;

public class MatchResultDto {
    public Long id;
    public Integer score;
    public String matchedKeywords;
    public String requirementAnalysis;
    public String summary;
    public String analysisReason;
    public String jobTitle;
    public String company;
    public String jobUrl;

    public static MatchResultDto from(MatchResult r, JobPosting p) {
        MatchResultDto dto = new MatchResultDto();
        dto.id = r.getId();
        dto.score = r.getScore();
        dto.matchedKeywords = r.getMatchedKeywords();
        dto.requirementAnalysis = r.getRequirementAnalysis();
        dto.summary = r.getSummary();
        dto.analysisReason = r.getAnalysisReason();
        dto.jobTitle = p.getTitle();
        dto.company = p.getCompany();
        dto.jobUrl = p.getUrl();
        return dto;
    }
}