package com.pigeonkim.jobmatcheragent.config;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 외부 API(Claude, Wanted) 호출 재시도 설정.
 *
 * <p>네트워크 일시 오류·5xx 같은 순간적 실패에 대비해 최대 3회까지 재시도한다.
 * Spring Boot 4용 resilience4j 스타터가 아직 없어 core 모듈을 직접 빈으로 등록해 쓴다.
 */
@Configuration
public class ResilienceConfig {

    @Bean
    public Retry externalApiRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)                       // 최초 1회 + 재시도 2회
                .waitDuration(Duration.ofSeconds(2))  // 재시도 사이 2초 대기
                .retryExceptions(Exception.class)     // 외부 API 호출 예외 전반에 재시도
                .build();
        return Retry.of("externalApi", config);
    }
}
