package com.resumeforge.ai.provider.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeforge.ai.provider.AiProvider;
import com.resumeforge.ai.provider.GenerationOptions;
import com.resumeforge.ai.provider.GenerationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ClaudeAiProvider implements AiProvider {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.model:claude-3-5-sonnet-20241022}")
    private String defaultModel;

    @Value("${ai.timeout-seconds:90}")
    private int timeoutSeconds;

    public ClaudeAiProvider(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProviderId() {
        return "anthropic";
    }

    @Override
    public String getDefaultModel() {
        return defaultModel;
    }

    @Override
    public GenerationResult generate(String systemPrompt, String userPrompt, GenerationOptions options) {
        long start = System.currentTimeMillis();
        String model = Optional.ofNullable(options.modelOverride()).orElse(defaultModel);
        int timeoutMs = options.timeoutMs() > 0 ? (int) options.timeoutMs() : timeoutSeconds * 1000;

        Map<String, Object> requestBody = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "user", "content", List.of(
                    Map.of("type", "text", "text", systemPrompt + "\n\n" + userPrompt)
                ))
            ),
            "max_tokens", options.maxTokens(),
            "temperature", options.temperature()
        );

        try {
            String rawResponse = webClient.post()
                .uri("https://api.anthropic.com/v1/messages")
                .header("x-api-key", apiKey)
                .header("Content-Type", "application/json")
                .header("anthropic-version", "2023-06-01")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode().value() == 429) {
                        return Mono.error(new RuntimeException("RATE_LIMITED"));
                    }
                    return Mono.error(e);
                })
                .block(Duration.ofMillis(timeoutMs));

            return parseResponse(rawResponse, model, start);
        } catch (RuntimeException e) {
            String errorCode = "PROVIDER_ERROR";
            if ("RATE_LIMITED".equals(e.getMessage())) {
                errorCode = "RATE_LIMITED";
            }
            return new GenerationResult(false, null, null, "anthropic", model,
                    0, 0, 0, 0, System.currentTimeMillis() - start,
                    e.getMessage(), errorCode);
        } catch (Exception e) {
            return new GenerationResult(false, null, null, "anthropic", model,
                    0, 0, 0, 0, System.currentTimeMillis() - start,
                    e.getMessage(), "PROVIDER_ERROR");
        }
    }

    private GenerationResult parseResponse(String rawResponse, String model, long start) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String text = root.path("content").get(0).path("text").asText();
            JsonNode usage = root.path("usage");
            int inputTokens = usage.path("input_tokens").asInt(0);
            int outputTokens = usage.path("output_tokens").asInt(0);

            Map<String, Object> parsed = objectMapper.readValue(text, Map.class);
            double cost = (inputTokens / 1_000_000.0 * 3.0) + (outputTokens / 1_000_000.0 * 15.0);
            return new GenerationResult(true, text, parsed, "anthropic", model,
                    inputTokens, outputTokens, inputTokens + outputTokens,
                    cost, System.currentTimeMillis() - start, null, null);
        } catch (JsonProcessingException e) {
            return new GenerationResult(false, rawResponse, null, "anthropic", model,
                    0, 0, 0, 0, System.currentTimeMillis() - start,
                    "Failed to parse response: " + e.getMessage(), "INVALID_OUTPUT");
        }
    }
}
