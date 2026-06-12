package com.resumeforge.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Enumeration of log categories stored in the processing_logs table.
 * Values correspond to the PostgreSQL CHECK constraint:
 * processing_logs.category IN ('ai_request','data_sync','validation','export','import','system','security','billing')
 */
public enum LogCategory {

    AI_REQUEST("ai_request"),
    DATA_SYNC("data_sync"),
    VALIDATION("validation"),
    EXPORT("export"),
    IMPORT("import"),
    SYSTEM("system"),
    SECURITY("security"),
    BILLING("billing");

    private final String value;

    LogCategory(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Converter
    public static class LogCategoryConverter implements AttributeConverter<LogCategory, String> {
        @Override
        public String convertToDatabaseColumn(LogCategory attribute) {
            return attribute == null ? null : attribute.getValue();
        }

        @Override
        public LogCategory convertToEntityAttribute(String dbData) {
            return dbData == null ? null : java.util.Arrays.stream(LogCategory.values())
                .filter(e -> e.getValue().equals(dbData))
                .findFirst().orElse(null);
        }
    }
}
