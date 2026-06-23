package com.pigeonkim.jobmatcheragent.api;

import com.pigeonkim.jobmatcheragent.crawler.WantedCrawler;
import com.pigeonkim.jobmatcheragent.domain.*;
import com.pigeonkim.jobmatcheragent.matching.AnalysisOutcome;
import com.pigeonkim.jobmatcheragent.matching.FeedbackKeywords;
import com.pigeonkim.jobmatcheragent.matching.MatchAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class JobMatchingController {

    private static final Logger log = LoggerFactory.getLogger(JobMatchingController.class);

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
    public String analyze() {
        UserProfile profile = userProfileRepository.findById(1L).orElseThrow();
        List<JobPosting> postings = jobPostingRepository.findAll();

        // 피드백 키워드는 루프 전 1회만 조회 (eng-review P1: 공고마다 조회하면 2N 쿼리)
        FeedbackKeywords feedback = matchAnalysisService.loadFeedbackKeywords();

        int analyzed = 0, skipped = 0, failed = 0;
        for (JobPosting posting : postings) {
            // 공고 1건 분석이 실패해도 배치 전체가 멈추지 않도록 격리한다 (Phase 8)
            try {
                AnalysisOutcome outcome = matchAnalysisService.analyzeIfNeeded(profile, posting, feedback);
                if (outcome.reanalyzed()) analyzed++;
                else skipped++;
            } catch (Exception e) {
                failed++;
                log.warn("공고 분석 실패 (id={}): {}", posting.getId(), e.getMessage());
            }
        }

        return "분석: " + analyzed + "건 / 건너뜀: " + skipped + "건 / 실패: " + failed + "건";
    }

    // 크롤링 실행
    @PostMapping("/crawl")
    public String crawl() {
        List<String> keywords = List.of("Spring Boot", "Java 백엔드", "서버 개발자 Java");
        int count = 0, failed = 0;
        for (String keyword : keywords) {
            // 한 키워드 검색이 실패해도 다른 키워드 크롤링은 계속한다 (eng-review #5)
            List<String> urls;
            try {
                urls = wantedCrawler.searchJobUrls(keyword);
            } catch (Exception e) {
                failed++;
                log.warn("공고 검색 실패 (keyword={}): {}", keyword, e.getMessage());
                continue;
            }

            for (String url : urls) {
                // 공고 1건 수집 실패가 전체 크롤링을 멈추지 않도록 격리한다 (Phase 8)
                try {
                    wantedCrawler.parseJobPosting(url);
                    count++;
                } catch (Exception e) {
                    failed++;
                    log.warn("공고 수집 실패 (url={}): {}", url, e.getMessage());
                }
            }
        }
        return count + "건 수집 완료 / 실패: " + failed + "건";
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