package com.pigeonkim.jobmatcheragent.config;

import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.net.ConnectException;
import java.net.URI;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 재시도 범위 검증 (eng-review 1A) — 일시적 오류만 재시도하고 비일시적은 즉시 포기.
 */
class ResilienceConfigTest {

    private final RetryConfig config = new ResilienceConfig().externalApiRetry().getRetryConfig();

    @Test
    void retries_transient_connection_errors() {
        Predicate<Throwable> shouldRetry = config.getExceptionPredicate();
        WebClientRequestException transientError = new WebClientRequestException(
                new ConnectException("connection refused"),
                HttpMethod.GET, URI.create("http://example.com"), new HttpHeaders());

        assertThat(shouldRetry.test(transientError)).isTrue();
    }

    @Test
    void retries_response_timeout() {
        // 서버가 응답 안 하고 멈추는 케이스(응답 타임아웃)도 재시도 대상 (eng-review #1)
        Predicate<Throwable> shouldRetry = config.getExceptionPredicate();

        assertThat(shouldRetry.test(io.netty.handler.timeout.ReadTimeoutException.INSTANCE)).isTrue();
        assertThat(shouldRetry.test(new java.util.concurrent.TimeoutException())).isTrue();
    }

    @Test
    void does_not_retry_non_transient_errors() {
        Predicate<Throwable> shouldRetry = config.getExceptionPredicate();

        // 인증 실패·잘못된 요청·비즈니스 오류는 재시도해도 무의미 → 재시도 안 함
        assertThat(shouldRetry.test(new IllegalStateException("스키마 불일치"))).isFalse();
        assertThat(shouldRetry.test(new IllegalArgumentException("400"))).isFalse();
    }

    @Test
    void max_attempts_is_three() {
        assertThat(config.getMaxAttempts()).isEqualTo(3);
    }
}
