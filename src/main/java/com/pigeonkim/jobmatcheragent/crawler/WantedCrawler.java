package com.pigeonkim.jobmatcheragent.crawler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pigeonkim.jobmatcheragent.domain.JobPosting;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class WantedCrawler implements JobSiteCrawler {

    private final WebClient webClient;
    private final JobCrawlerService jobCrawlerService;

    public WantedCrawler(JobCrawlerService jobCrawlerService) {
        this.jobCrawlerService = jobCrawlerService;
        this.webClient = WebClient.builder()
                .baseUrl("https://www.wanted.co.kr")
                .defaultHeader("User-Agent", "Mozilla/5.0")
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> searchJobUrls(String keyword) {
        List<String> urls = new ArrayList<>();

        Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/chaos/search/v1/position")
                        .queryParam("query", keyword)
                        .queryParam("country", "kr")
                        .queryParam("years", -1)
                        .queryParam("locations", "all")
                        .queryParam("limit", 20)
                        .queryParam("offset", 0)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        // response 자체가 null일 수 있음
        if (response == null) return urls;

        List<Map<String, Object>> data =
                (List<Map<String, Object>>) response.get("data");

        // data가 null일 수 있음
        if (data == null) return urls;

        for (Map<String, Object> job : data) {
            Object empType = job.get("employment_type");
            Object id = job.get("id");

            // id나 employment_type이 null일 수 있음
            if (id != null && "regular".equals(empType)) {
                urls.add("https://www.wanted.co.kr/wd/" + id);
            }
        }

        return urls;
    }

    @Override
    public JobPosting parseJobPosting(String url) {
        try {
            return jobCrawlerService.fetchAndSave(url);
        } catch (Exception e) {
            throw new RuntimeException("크롤링 실패: " + url, e);
        }
    }
}