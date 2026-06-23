package com.pigeonkim.jobmatcheragent.crawler;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.pigeonkim.jobmatcheragent.domain.JobPosting;
import com.pigeonkim.jobmatcheragent.domain.JobPostingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * JobCrawlerService.fetchAndSave 결정적 테스트 — Jsoup이 WireMock HTML을 파싱.
 * companyLink 유/무, 기존 URL 캐시 분기를 커버. (eng-review: 외부 페이지 fetch를 mock화)
 */
class JobCrawlerServiceWireMockTest {

    WireMockServer wireMock;
    JobPostingRepository repository;
    JobCrawlerService service;

    @BeforeEach
    void setup() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
        repository = Mockito.mock(JobPostingRepository.class);
        service = new JobCrawlerService(repository);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void parses_company_and_title_from_anchor() throws Exception {
        wireMock.stubFor(get(urlPathMatching("/wd/.*")).willReturn(okHtml("""
                <html><head><title>무시되는제목</title></head><body>
                  <a data-company-name="Acme" data-position-name="백엔드 개발자">회사</a>
                </body></html>
                """)));
        when(repository.findByUrl(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        JobPosting saved = service.fetchAndSave(wireMock.url("/wd/123"));

        assertThat(saved.getCompany()).isEqualTo("Acme");
        assertThat(saved.getTitle()).isEqualTo("백엔드 개발자");
        verify(repository).save(any());
    }

    @Test
    void falls_back_when_anchor_missing() throws Exception {
        wireMock.stubFor(get(urlPathMatching("/wd/.*")).willReturn(okHtml(
                "<html><head><title>폴백제목</title></head><body>본문</body></html>")));
        when(repository.findByUrl(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        JobPosting saved = service.fetchAndSave(wireMock.url("/wd/456"));

        assertThat(saved.getCompany()).isEqualTo("알 수 없음");
        assertThat(saved.getTitle()).isEqualTo("폴백제목");
    }

    @Test
    void returns_existing_without_fetch_when_url_cached() throws Exception {
        JobPosting existing = new JobPosting();
        existing.setCompany("기존회사");
        when(repository.findByUrl("https://cached/wd/1")).thenReturn(Optional.of(existing));

        JobPosting result = service.fetchAndSave("https://cached/wd/1");

        assertThat(result).isSameAs(existing);
        verify(repository, never()).save(any()); // 캐시 히트 → 저장 안 함
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder okHtml(String body) {
        return aResponse().withStatus(200)
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBody(body);
    }
}
