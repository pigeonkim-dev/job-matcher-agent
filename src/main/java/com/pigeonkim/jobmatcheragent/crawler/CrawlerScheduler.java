package com.pigeonkim.jobmatcheragent.crawler;

import com.pigeonkim.jobmatcheragent.digest.DailyDigestService;
import com.pigeonkim.jobmatcheragent.domain.UserProfile;
import com.pigeonkim.jobmatcheragent.domain.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CrawlerScheduler {

    private static final Logger log = LoggerFactory.getLogger(CrawlerScheduler.class);

    private final WantedCrawler wantedCrawler;
    private final DailyDigestService dailyDigestService;
    private final UserProfileRepository userProfileRepository;

    public CrawlerScheduler(WantedCrawler wantedCrawler, DailyDigestService dailyDigestService,
                            UserProfileRepository userProfileRepository) {
        this.wantedCrawler = wantedCrawler;
        this.dailyDigestService = dailyDigestService;
        this.userProfileRepository = userProfileRepository;
    }

    // 매일 9시, 12시, 15시, 18시, 21시 실행
    @Scheduled(cron = "0 0 9,12,15,18,21 * * *")
    public void crawlJobs() {
        log.info("채용공고 크롤링 배치 시작");

        // 검색어는 프로필의 검색 키워드에서 가져온다 (하드코딩 제거)
        UserProfile profile = userProfileRepository.findById(1L).orElse(null);
        List<String> keywords = profile != null ? profile.getSearchKeywordList() : List.of();
        if (keywords.isEmpty()) {
            log.warn("검색 키워드가 없어 크롤링을 건너뜁니다. 프로필을 확인하세요.");
            return;
        }

        int total = 0;
        for (String keyword : keywords) {
            List<String> urls = wantedCrawler.searchJobUrls(keyword);
            for (String url : urls) {
                wantedCrawler.parseJobPosting(url);
                total++;
            }
        }

        log.info("채용공고 크롤링 배치 완료: {}건", total);
    }

    // 매일 오전 8시 Slack 리포트 전송
    @Scheduled(cron = "0 0 8 * * *")
    public void sendDigest() {
        log.info("Slack 일일 리포트 전송 시작");
        dailyDigestService.send();
    }
}