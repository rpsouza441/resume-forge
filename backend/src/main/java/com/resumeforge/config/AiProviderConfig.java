package com.resumeforge.config;

import com.resumeforge.ai.provider.AiProvider;
import com.resumeforge.ai.provider.impl.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AiProviderConfig {

    @Value("${ai.provider:gemini}")
    String activeProvider;

    @Value("${ai.fallback-provider:}")
    String fallbackProvider;

    @Bean
    @Primary
    public AiProvider activeAiProvider(
            GeminiAiProvider geminiProvider,
            OpenAiProvider openaiProvider,
            ClaudeAiProvider claudeProvider,
            OpenRouterProvider openrouterProvider) {
        return switch (activeProvider.toLowerCase()) {
            case "openai" -> openaiProvider;
            case "anthropic", "claude" -> claudeProvider;
            case "openrouter" -> openrouterProvider;
            default -> geminiProvider;
        };
    }

    @Bean
    public AiProvider fallbackAiProvider(
            GeminiAiProvider geminiProvider,
            OpenAiProvider openaiProvider,
            ClaudeAiProvider claudeProvider,
            OpenRouterProvider openrouterProvider) {
        if (fallbackProvider == null || fallbackProvider.isBlank()) {
            return null;
        }
        return switch (fallbackProvider.toLowerCase()) {
            case "openai" -> openaiProvider;
            case "anthropic", "claude" -> claudeProvider;
            case "openrouter" -> openrouterProvider;
            default -> geminiProvider;
        };
    }
}
