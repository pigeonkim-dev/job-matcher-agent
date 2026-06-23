package com.pigeonkim.jobmatcheragent.api;

import com.pigeonkim.jobmatcheragent.crawler.WantedCrawler;
import com.pigeonkim.jobmatcheragent.domain.*;
import com.pigeonkim.jobmatcheragent.matching.AnalysisOutcome;
import com.pigeonkim.jobmatcheragent.matching.FeedbackKeywords;
import com.pigeonkim.jobmatcheragent.matching.MatchAnalysisService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class JobMatchingController {

    private final UserProfileRepository userProfileRepository;
    private final JobPostingRepository jobPostingRepository;
    private final MatchResultRepository matchResultRepository;
    private final WantedCrawler wantedCrawler;
    private final MatchAnalysisService matchAnalysisService;

    public JobMatchingController(
            UserProfileRepository userProfileRepository,
            JobPostingRepository jobPostingRepository,
            MatchResultRepository matchResultRepository,
            WantedCrawler wantedCrawler,
            MatchAnalysisService matchAnalysisService) {
        this.userProfileRepository = userProfileRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.matchResultRepository = matchResultRepository;
        this.wantedCrawler = wantedCrawler;
        this.matchAnalysisService = matchAnalysisService;
    }

    // 프로필 조회
    @GetMapping("/profile")
    public UserProfile getProfile() {
        return userProfileRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("프로필 없음"));
    }

    // 공고 목록
    @GetMapping("/postings")
    public List<JobPosting> getPostings() {
        return jobPostingRepository.findAll();
    }

    @GetMapping("/results")
    public List<MatchResultDto> getResults() {
        return matchResultRepository.findAll().stream()
                .map(r -> {
                    JobPosting p = jobPostingRepository.findById(r.getJobPostingId())
                            .orElse(new JobPosting());
                    return MatchResultDto.from(r, p);
                })
                .sorted((a, b) -> (b.score != null ? b.score : 0) - (a.score != null ? a.score : 0))
                .toList();
    }

    // 분석 실행 — 스마트 재분석
    @PostMapping("/analyze")
    public String analyze() throws Exception {
        UserProfile profile = userProfileRepository.findById(1L).orElseThrow();
        List<JobPosting> postings = jobPostingRepository.findAll();

        // 피드백 키워드는 루프 전 1회만 조회 (eng-review P1: 공고마다 조회하면 2N 쿼리)
        FeedbackKeywords feedback = matchAnalysisService.loadFeedbackKeywords();

        int analyzed = 0, skipped = 0;
        for (JobPosting posting : postings) {
            AnalysisOutcome outcome = matchAnalysisService.analyzeIfNeeded(profile, posting, feedback);
            if (outcome.reanalyzed()) analyzed++;
            else skipped++;
        }

        return "분석: " + analyzed + "건 / 건너뜀: " + skipped + "건";
    }

    // 크롤링 실행
    @PostMapping("/crawl")
    public String crawl() {
        List<String> keywords = List.of("Spring Boot", "Java 백엔드", "서버 개발자 Java");
        int count = 0;
        for (String keyword : keywords) {
            for (String url : wantedCrawler.searchJobUrls(keyword)) {
                wantedCrawler.parseJobPosting(url);
                count++;
            }
        }
        return count + "건 수집 완료";
    }

    // 프로필 수정
    @PutMapping("/profile")
    public UserProfile updateProfile(@RequestBody UserProfile updated) {
        UserProfile profile = userProfileRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("프로필 없음"));
        profile.setResumeContent(updated.getResumeContent());
        profile.setPreferredCategories(updated.getPreferredCategories());
        profile.setAvoidKeywords(updated.getAvoidKeywords());
        profile.setPayFloor(updated.getPayFloor());
        profile.setPayTarget(updated.getPayTarget());
        profile.setJobTitle(updated.getJobTitle());
        return userProfileRepository.save(profile);
    }
}