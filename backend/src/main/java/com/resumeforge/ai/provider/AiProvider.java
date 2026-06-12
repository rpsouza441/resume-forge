package com.resumeforge.ai.provider;

public interface AiProvider {
    String getProviderId();
    String getDefaultModel();
    GenerationResult generate(String systemPrompt, String userPrompt, GenerationOptions options);
}
