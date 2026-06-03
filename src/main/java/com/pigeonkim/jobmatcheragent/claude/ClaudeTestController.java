package com.pigeonkim.jobmatcheragent.claude;

import com.pigeonkim.jobmatcheragent.crawler.JobCrawlerService;
import com.pigeonkim.jobmatcheragent.crawler.WantedCrawler;
import com.pigeonkim.jobmatcheragent.domain.*;
import com.pigeonkim.jobmatcheragent.matching.MatchingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

        // 가장 최근 저장된 공고로 테스트
        JobPosting jobPosting = jobPostingRepository.findAll().stream()
                .filter(j -> j.getDescription() != null)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("JobPosting 없음"));

        MatchResult result = matchingEngine.analyze(userProfile, jobPosting);

        return "=== 매칭 결과 ===\n" +
                "공고: " + jobPosting.getTitle() + " / " + jobPosting.getCompany() + "\n" +
                "점수: " + result.getScore() + "\n" +
                "키워드: " + result.getMatchedKeywords() + "\n" +
                "분석: " + result.getRequirementAnalysis() + "\n" +
                "요약: " + result.getSummary();
    }
    @Autowired
    private WantedCrawler wantedCrawler;

    @GetMapping("/wanted/test")
    public String wantedTest() {
        List<String> keywords = List.of(
                "Spring Boot",
                "Java 백엔드",
                "서버 개발자 Java"
        );

        int saved = 0;
        for (String keyword : keywords) {
            List<String> urls = wantedCrawler.searchJobUrls(keyword);
            for (String url : urls) {
                wantedCrawler.parseJobPosting(url);
                saved++;
            }
        }

        return "수집 완료: " + saved + "건";
    }
}