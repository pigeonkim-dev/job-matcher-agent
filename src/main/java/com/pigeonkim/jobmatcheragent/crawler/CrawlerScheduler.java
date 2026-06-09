package com.pigeonkim.jobmatcheragent.crawler;

import com.pigeonkim.jobmatcheragent.digest.DailyDigestService;
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

    public CrawlerScheduler(WantedCrawler wantedCrawler, DailyDigestService dailyDigestService) {
        this.wantedCrawler = wantedCrawler;
        this.dailyDigestService = dailyDigestService;
    }

    private static final List<String> KEYWORDS = List.of(
            "Spring Boot",
            "Java 백엔드",
            "서버 개발자 Java"
    );

    // 매일 9시, 12시, 15시, 18시, 21시 실행
    @Scheduled(cron = "0 0 9,12,15,18,21 * * *")
    public void crawlJobs() {
        log.info("채용공고 크롤링 배치 시작");

        int total = 0;
        for (String keyword : KEYWORDS) {
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