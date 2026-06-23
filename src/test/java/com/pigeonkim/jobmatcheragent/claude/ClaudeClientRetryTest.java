package com.pigeonkim.jobmatcheragent.claude;

import com.anthropic.client.AnthropicClient;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * ClaudeClient 재시도 동작 테스트 (Phase 8).
 *
 * <p>SDK 호출 자체는 모킹하기 까다로워, Retry가 호출을 감싸 재시도하는지를
 * sendStructured를 오버라이드한 테스트용 서브클래스로 검증한다.
 */
class ClaudeClientRetryTest {

    // 대기 없는 빠른 retry (테스트 속도)
    private final Retry fastRetry = Retry.of("test", RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(1))
            .retryExceptions(Exception.class)
            .build());

    /** callClaude를 흉내내는 테스트용 ClaudeClient — 실제 SDK 호출 대신 카운터 사용 */
    static class TestableClaudeClient extends ClaudeClient {
        final AtomicInteger calls = new AtomicInteger();
        final int failTimes;

        TestableClaudeClient(Retry retry, int failTimes) {
            super("test-key", "claude-sonnet-4-6", retry);
            this.failTimes = failTimes;
        }

        @Override
        protected <T> T doCall(String prompt, Class<T> type) {
            int n = calls.incrementAndGet();
            if (n <= failTimes) {
                throw new RuntimeException("일시적 오류 " + n);
            }
            return type.cast("성공");
        }
    }

    @Test
    void retries_then_succeeds_on_third_attempt() {
        TestableClaudeClient client = new TestableClaudeClient(fastRetry, 2); // 2번 실패 후 성공

        String result = client.sendStructured("프롬프트", String.class);

        assertThat(result).isEqualTo("성공");
        assertThat(client.calls.get()).isEqualTo(3); // 최초 1 + 재시도 2
    }

    @Test
    void gives_up_after_max_attempts() {
        TestableClaudeClient client = new TestableClaudeClient(fastRetry, 5); // 계속 실패

        assertThatThrownBy(() -> client.sendStructured("프롬프트", String.class))
                .isInstanceOf(RuntimeException.class);
        assertThat(client.calls.get()).isEqualTo(3); // 최대 3회까지만 시도
    }
}
