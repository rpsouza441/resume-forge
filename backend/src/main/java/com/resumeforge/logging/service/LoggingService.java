package com.resumeforge.logging.service;

import com.resumeforge.auth.entity.User;
import com.resumeforge.generation.entity.AiRun;
import com.resumeforge.generation.entity.GeneratedResume;
import com.resumeforge.logging.entity.ProcessingLog;
import com.resumeforge.logging.repository.ProcessingLogRepository;
import com.resumeforge.model.enums.LogCategory;
import com.resumeforge.model.enums.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class LoggingService {

    private static final Logger log = LoggerFactory.getLogger(LoggingService.class);
    private final ProcessingLogRepository repository;

    public LoggingService(ProcessingLogRepository repository) {
        this.repository = repository;
    }

    public void log(LogLevel level, LogCategory category, String message,
                   Map<String, Object> context, UUID userId, UUID aiRunId, UUID generatedResumeId) {
        ProcessingLog logEntry = ProcessingLog.builder()
            .logLevel(level)
            .category(category)
            .message(message)
            .contextData(context != null ? context : Map.of())
            .build();

        if (userId != null) {
            User user = new User();
            user.setId(userId);
            logEntry.setUser(user);
        }
        if (aiRunId != null) {
            AiRun aiRun = new AiRun();
            aiRun.setId(aiRunId);
            logEntry.setAiRun(aiRun);
        }
        if (generatedResumeId != null) {
            GeneratedResume gr = new GeneratedResume();
            gr.setId(generatedResumeId);
            logEntry.setGeneratedResume(gr);
        }

        logEntry.setSourceService("spring-boot-api");
        repository.save(logEntry);
    }

    public void logInfo(LogCategory category, String message, Map<String, Object> context) {
        log(LogLevel.INFO, category, message, context, null, null, null);
    }

    public void logInfo(LogCategory category, String message, UUID userId) {
        log(LogLevel.INFO, category, message, null, userId, null, null);
    }

    public void logError(LogCategory category, String message, Throwable t, UUID userId) {
        log(LogLevel.ERROR, category, message,
            Map.of("exception", t != null ? t.getClass().getSimpleName() : "unknown",
                   "message", t != null ? t.getMessage() : message),
            userId, null, null);
    }
}