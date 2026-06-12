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
public class OpenRouterProvider implements AiProvider {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.model:meta-llama/llama-3-8b-instruct}")
    private String defaultModel;

    @Value("${ai.timeout-seconds:90}")
    private int timeoutSeconds;

    public OpenRouterProvider(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProviderId() {
        return "openrouter";
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
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ),
            "temperature", options.temperature(),
            "max_tokens", options.maxTokens()
        );

        try {
            String rawResponse = webClient.post()
                .uri("https://openrouter.ai/api/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", "https://resume-forge.app")
                .header("X-Title", "Resume Forge")
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
            return new GenerationResult(false, null, null, "openrouter", model,
                    0, 0, 0, 0, System.currentTimeMillis() - start,
                    e.getMessage(), errorCode);
        } catch (Exception e) {
            return new GenerationResult(false, null, null, "openrouter", model,
                    0, 0, 0, 0, System.currentTimeMillis() - start,
                    e.getMessage(), "PROVIDER_ERROR");
        }
    }

    private GenerationResult parseResponse(String rawResponse, String model, long start) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String text = root.path("choices").get(0).path("message").path("content").asText();
            JsonNode usage = root.path("usage");
            int inputTokens = usage.path("prompt_tokens").asInt(0);
            int outputTokens = usage.path("completion_tokens").asInt(0);

            Map<String, Object> parsed = objectMapper.readValue(text, Map.class);
            double cost = 0.0; // OpenRouter varies by model, often free
            return new GenerationResult(true, text, parsed, "openrouter", model,
                    inputTokens, outputTokens, inputTokens + outputTokens,
                    cost, System.currentTimeMillis() - start, null, null);
        } catch (JsonProcessingException e) {
            return new GenerationResult(false, rawResponse, null, "openrouter", model,
                    0, 0, 0, 0, System.currentTimeMillis() - start,
                    "Failed to parse response: " + e.getMessage(), "INVALID_OUTPUT");
        }
    }
}
