package com.pigeonkim.jobmatcheragent.claude;

import com.pigeonkim.jobmatcheragent.crawler.JobCrawlerService;
import com.pigeonkim.jobmatcheragent.domain.*;
import com.pigeonkim.jobmatcheragent.matching.MatchingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ClaudeTestController {

    private final ClaudeClient claudeClient;

    @Autowired
    private JobCrawlerService jobCrawlerService;

    @Autowired
    private MatchingEngine matchingEngine;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @GetMapping("/claude/test")
    public String test(@RequestParam(defaultValue = "안녕! 한 줄로 인사해줘.") String prompt) {
        return claudeClient.sendMessage(prompt);
    }

    @GetMapping("/crawler/test")
    public String crawlerTest() throws Exception {
        JobPosting saved = jobCrawlerService.fetchAndSave(
                "https://www.wanted.co.kr/wd/365123"
        );
        return "저장 완료. ID: " + saved.getId();
    }

    @GetMapping("/crawler/save")
    public String crawlerSave() throws Exception {
        JobPosting saved = jobCrawlerService.fetchAndSave(
                "https://www.wanted.co.kr/wd/365123"
        );
        return "저장 완료. title: " + saved.getTitle()
                + " / company: " + saved.getCompany();
    }

    @GetMapping("/matching/test")
    public String matchingTest() throws Exception {
        UserProfile userProfile = userProfileRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("UserProfile 없음"));

        JobPosting jobPosting = jobPostingRepository.findById(2L)
                .orElseThrow(() -> new RuntimeException("JobPosting 없음"));

        MatchResult result = matchingEngine.analyze(userProfile, jobPosting);

        return "score: " + result.getScore() +
                "\nkeywords: " + result.getMatchedKeywords() +
                "\nsummary: " + result.getSummary();
    }
}