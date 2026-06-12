package com.resumeforge.ai.provider;

import lombok.Builder;

@Builder
public record GenerationOptions(
    double temperature,     // default 0.3
    int maxTokens,          // default 8192
    long timeoutMs,         // default 90000
    String modelOverride,   // nullable
    boolean retryEnabled    // default true
) {
    public static GenerationOptions defaults() {
        return new GenerationOptions(0.3, 8192, 90000, null, true);
    }
}
