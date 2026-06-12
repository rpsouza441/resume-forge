package com.resumeforge.model.converter;

import com.resumeforge.model.enums.AiRunStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AiRunStatusConverter implements AttributeConverter<AiRunStatus, String> {
    @Override
    public String convertToDatabaseColumn(AiRunStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public AiRunStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : java.util.Arrays.stream(AiRunStatus.values())
            .filter(e -> e.getValue().equals(dbData))
            .findFirst().orElse(null);
    }
}