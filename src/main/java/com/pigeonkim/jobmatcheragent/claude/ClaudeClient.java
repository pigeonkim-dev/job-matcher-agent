package com.pigeonkim.jobmatcheragent.claude;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import io.github.resilience4j.retry.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 공식 Anthropic Java SDK 래퍼.
 *
 * <p>structured outputs(`outputConfig(type)`)로 응답을 지정한 record 타입으로
 * 강제·검증한다. 이전의 수제 WebClient + 정규식 strip + Map 캐스팅(B2)을 제거했다.
 *
 * <p>일시적 오류(네트워크·5xx)에 대비해 Resilience4j Retry로 호출을 감싼다. (Phase 8)
 */
@Service
public class ClaudeClient {

    private static final long MAX_TOKENS = 1024L;

    private final AnthropicClient client;
    private final String model;
    private final Retry retry;

    public ClaudeClient(
            @Value("${anthropic.api-key}") String apiKey,
            @Value("${anthropic.model}") String model,
            Retry externalApiRetry) {
        this.model = model;
        this.retry = externalApiRetry;
        // env(fromEnv) 대신 설정값으로 명시 생성 — env 미설정 시 런타임 폭발 방지 (eng-review)
        this.client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    /**
     * 프롬프트를 보내고 응답을 {@code type} 스키마로 강제해 받는다. 실패 시 재시도.
     *
     * @param type structured output 스키마가 될 record 클래스
     * @return 검증·파싱된 타입 인스턴스
     */
    public <T> T sendStructured(String prompt, Class<T> type) {
        return retry.executeSupplier(() -> doCall(prompt, type));
    }

    // 테스트에서 SDK 호출을 대체할 수 있도록 protected (Phase 8 재시도 테스트)
    protected <T> T doCall(String prompt, Class<T> type) {
        StructuredMessageCreateParams<T> params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .outputConfig(type)
                .addUserMessage(prompt)
                .build();

        return client.messages().create(params).content().stream()
                .flatMap(block -> block.text().stream())
                .map(typed -> typed.text())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Claude 응답에 구조화된 출력이 없습니다."));
    }
}
