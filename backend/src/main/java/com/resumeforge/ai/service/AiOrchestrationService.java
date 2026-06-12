package com.resumeforge.ai.service;

import com.resumeforge.ai.provider.AiProvider;
import com.resumeforge.ai.provider.GenerationOptions;
import com.resumeforge.ai.provider.GenerationResult;
import com.resumeforge.exception.AiGenerationException;
import com.resumeforge.logging.service.LoggingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AiOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(AiOrchestrationService.class);

    private final AiProvider primaryProvider;
    private final AiProvider fallbackProvider;
    private final LoggingService loggingService;

    public AiOrchestrationService(
            @Qualifier("activeAiProvider") AiProvider primaryProvider,
            @Qualifier("fallbackAiProvider") @Nullable AiProvider fallbackProvider,
            LoggingService loggingService) {
        this.primaryProvider = primaryProvider;
        this.fallbackProvider = fallbackProvider;
        this.loggingService = loggingService;
    }

    public GenerationResult generate(String systemPrompt, String userPrompt, GenerationOptions options) {
        GenerationResult result = attempt(primaryProvider, systemPrompt, userPrompt, options);
        if (result.isSuccess()) return result;

        if (fallbackProvider != null && options.retryEnabled()) {
            log.warn("Primary provider {} failed, attempting fallback...", primaryProvider.getProviderId());
            loggingService.logInfo(com.resumeforge.model.enums.LogCategory.AI_REQUEST,
                "Primary AI provider failed, attempting fallback",
                Map.of("primaryProvider", primaryProvider.getProviderId(), "error", result.errorMessage()));
            result = attempt(fallbackProvider, systemPrompt, userPrompt, options);
        }

        if (!result.isSuccess()) {
            throw new AiGenerationException("All AI providers failed: " + result.errorMessage());
        }
        return result;
    }

    private GenerationResult attempt(AiProvider provider, String systemPrompt, String userPrompt, GenerationOptions options) {
        try {
            return provider.generate(systemPrompt, userPrompt, options);
        } catch (Exception e) {
            return GenerationResult.failure(provider.getProviderId(), e.getMessage(), "EXCEPTION");
        }
    }
}
