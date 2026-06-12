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
import java.util.Map;
import java.util.Optional;

@Component
public class GeminiAiProvider implements AiProvider {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.model:gemini-2.0-flash}")
    private String defaultModel;

    @Value("${ai.timeout-seconds:90}")
    private int timeoutSeconds;

    public GeminiAiProvider(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProviderId() {
        return "gemini";
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

        String combinedPrompt = systemPrompt + "\n\n" + userPrompt;

        Map<String, Object> requestBody = Map.of(
            "contents", new Object[]{
                Map.of("parts", new Object[]{
                    Map.of("text", combinedPrompt)
                })
            },
            "generationConfig", Map.of(
                "temperature", options.temperature(),
                "maxOutputTokens", options.maxTokens(),
                "responseMimeType", "application/json"
            )
        );

        try {
            String rawResponse = webClient.post()
                .uri("https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
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
            return new GenerationResult(false, null, null, "gemini", model,
                    0, 0, 0, 0, System.currentTimeMillis() - start,
                    e.getMessage(), errorCode);
        } catch (Exception e) {
            return new GenerationResult(false, null, null, "gemini", model,
                    0, 0, 0, 0, System.currentTimeMillis() - start,
                    e.getMessage(), "PROVIDER_ERROR");
        }
    }

    private GenerationResult parseResponse(String rawResponse, String model, long start) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                String text = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
                JsonNode usageMetadata = root.path("usageMetadata");
                int inputTokens = usageMetadata.path("promptTokenCount").asInt(0);
                int outputTokens = usageMetadata.path("candidatesTokenCount").asInt(0);

                Map<String, Object> parsed = objectMapper.readValue(text, Map.class);
                double cost = (inputTokens / 1_000_000.0 * 0.10) + (outputTokens / 1_000_000.0 * 0.40);
                return new GenerationResult(true, text, parsed, "gemini", model,
                        inputTokens, outputTokens, inputTokens + outputTokens,
                        cost, System.currentTimeMillis() - start, null, null);
            } else {
                return new GenerationResult(false, rawResponse, null, "gemini", model,
                        0, 0, 0, 0, System.currentTimeMillis() - start,
                        "No candidates in response", "INVALID_OUTPUT");
            }
        } catch (JsonProcessingException e) {
            return new GenerationResult(false, rawResponse, null, "gemini", model,
                    0, 0, 0, 0, System.currentTimeMillis() - start,
                    "Failed to parse response: " + e.getMessage(), "INVALID_OUTPUT");
        }
    }
}
