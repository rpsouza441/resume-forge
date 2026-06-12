package com.resumeforge.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Enumeration of log levels stored in the processing_logs table.
 * Values correspond to the PostgreSQL CHECK constraint:
 * processing_logs.log_level IN ('DEBUG','INFO','WARN','ERROR','CRITICAL')
 */
public enum LogLevel {

    DEBUG("DEBUG"),
    INFO("INFO"),
    WARN("WARN"),
    ERROR("ERROR"),
    CRITICAL("CRITICAL");

    private final String value;

    LogLevel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Converter
    public static class LogLevelConverter implements AttributeConverter<LogLevel, String> {
        @Override
        public String convertToDatabaseColumn(LogLevel attribute) {
            return attribute == null ? null : attribute.name();
        }

        @Override
        public LogLevel convertToEntityAttribute(String dbData) {
            return dbData == null ? null : LogLevel.valueOf(dbData);
        }
    }
}
