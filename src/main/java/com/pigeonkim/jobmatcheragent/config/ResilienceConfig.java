package com.pigeonkim.jobmatcheragent.config;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.netty.handler.timeout.ReadTimeoutException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * 외부 API 호출 재시도 설정 (WebClient 기반 호출용, 현재 Wanted 크롤러).
 *
 * <p>연결 실패·타임아웃 같은 <b>일시적</b> 오류에만 재시도한다. 4xx/비즈니스 오류는
 * 재시도해도 똑같이 실패하므로 제외한다. (eng-review: 모든 예외 재시도는 시간 낭비)
 *
 * <p>Claude 호출은 anthropic-java SDK가 429·5xx를 자체 재시도하므로 여기서 감싸지 않는다.
 * Spring Boot 4용 resilience4j 스타터가 아직 없어 core 모듈을 직접 빈으로 등록해 쓴다.
 */
@Configuration
public class ResilienceConfig {

    @Bean
    public Retry externalApiRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)                       // 최초 1회 + 재시도 2회
                .waitDuration(Duration.ofSeconds(2))  // 재시도 사이 2초 대기
                // 일시적 오류만 재시도 (4xx 등 비일시적은 재시도해도 무의미)
                //  - WebClientRequestException: 연결 실패 등 요청 전송 단계 오류
                //  - ReadTimeout/Timeout: 서버가 응답 안 하고 멈추는 응답 타임아웃 (eng-review #1)
                .retryExceptions(
                        WebClientRequestException.class,
                        ReadTimeoutException.class,
                        TimeoutException.class)
                .build();
        return Retry.of("externalApi", config);
    }
}
