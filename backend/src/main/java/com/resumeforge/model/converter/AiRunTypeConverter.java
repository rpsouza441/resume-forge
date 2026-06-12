package com.resumeforge.model.converter;

import com.resumeforge.model.enums.AiRunType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AiRunTypeConverter implements AttributeConverter<AiRunType, String> {
    @Override
    public String convertToDatabaseColumn(AiRunType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public AiRunType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : java.util.Arrays.stream(AiRunType.values())
            .filter(e -> e.getValue().equals(dbData))
            .findFirst().orElse(null);
    }
}