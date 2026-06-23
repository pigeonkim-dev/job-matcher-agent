package com.pigeonkim.jobmatcheragent.crawler;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * WantedCrawler.searchJobUrls 결정적 테스트 — live API 대신 WireMock 스텁.
 * (eng-review: 외부 API를 스텁해 flaky 없이 정규직 필터링·null 가드 검증)
 */
class WantedCrawlerWireMockTest {

    WireMockServer wireMock;

    @BeforeEach
    void start() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
    }

    @AfterEach
    void stop() {
        wireMock.stop();
    }

    private WantedCrawler crawlerPointingAt(String baseUrl) {
        // searchJobUrls는 jobCrawlerService를 쓰지 않으므로 mock으로 충분
        return new WantedCrawler(Mockito.mock(JobCrawlerService.class), baseUrl);
    }

    @Test
    void searchJobUrls_returns_only_regular_employment() {
        wireMock.stubFor(get(urlPathEqualTo("/api/chaos/search/v1/position"))
                .willReturn(okJson("""
                        {"data":[
                          {"id":123,"employment_type":"regular"},
                          {"id":456,"employment_type":"contract"},
                          {"id":789,"employment_type":"regular"}
                        ]}
                        """)));

        List<String> urls = crawlerPointingAt(wireMock.baseUrl()).searchJobUrls("Spring");

        assertThat(urls).containsExactlyInAnyOrder(
                "https://www.wanted.co.kr/wd/123",
                "https://www.wanted.co.kr/wd/789");
    }

    @Test
    void searchJobUrls_returns_empty_when_data_null() {
        wireMock.stubFor(get(urlPathEqualTo("/api/chaos/search/v1/position"))
                .willReturn(okJson("{}"))); // data 없음

        List<String> urls = crawlerPointingAt(wireMock.baseUrl()).searchJobUrls("Spring");

        assertThat(urls).isEmpty();
    }

    @Test
    void searchJobUrls_skips_entries_missing_id_or_type() {
        wireMock.stubFor(get(urlPathEqualTo("/api/chaos/search/v1/position"))
                .willReturn(okJson("""
                        {"data":[
                          {"employment_type":"regular"},
                          {"id":555},
                          {"id":777,"employment_type":"regular"}
                        ]}
                        """)));

        List<String> urls = crawlerPointingAt(wireMock.baseUrl()).searchJobUrls("Spring");

        assertThat(urls).containsExactly("https://www.wanted.co.kr/wd/777");
    }
}
