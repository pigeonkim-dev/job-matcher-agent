package com.pigeonkim.jobmatcheragent.claude;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class ClaudeClient {

    // 외부 API라 timeout 필수 — 한 번의 hang이 분석 배치 전체를 멈추지 않도록. (eng-review B4)
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(60);

    private final WebClient webClient;
    private final String model;

    public ClaudeClient(
            @Value("${anthropic.api-key}") String apiKey,
            @Value("${anthropic.base-url}") String baseUrl,
            @Value("${anthropic.model}") String model) {
        this.model = model;

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) CONNECT_TIMEOUT.toMillis())
                .responseTimeout(RESPONSE_TIMEOUT)
                .doOnConnected(conn -> conn.addHandlerLast(
                        new ReadTimeoutHandler((int) RESPONSE_TIMEOUT.getSeconds())));

        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("content-type", "application/json")
                .build();
    }

    @SuppressWarnings("unchecked")
    public String sendMessage(String userMessage) {
        Map<String, Object> request = Map.of(
                "model", model,
                "max_tokens", 1024,
                "messages", List.of(
                        Map.of("role", "user", "content", userMessage)
                )
        );

        Map<String, Object> response = webClient.post()
                .uri("/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            throw new RuntimeException("Claude API 응답이 null입니다.");
        }

        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");

        if (content == null || content.isEmpty()) {
            throw new RuntimeException("Claude API 응답 content가 비어있습니다.");
        }

        return (String) content.get(0).get("text");
    }
}