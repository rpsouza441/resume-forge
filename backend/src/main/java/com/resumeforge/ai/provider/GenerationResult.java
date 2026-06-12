package com.resumeforge.ai.provider;

import java.util.Map;

public record GenerationResult(
    boolean success,
    String rawResponse,
    Map<String, Object> parsedResponse,
    String provider,
    String model,
    int inputTokens,
    int outputTokens,
    int totalTokens,
    double costEstimate,
    long durationMs,
    String errorMessage,
    String errorCode
) {
    public static GenerationResult failure(String provider, String errorMessage, String errorCode) {
        return new GenerationResult(false, null, null, provider, null, 0, 0, 0, 0, 0, errorMessage, errorCode);
    }

    public boolean isSuccess() {
        return success && errorMessage == null;
    }
}
