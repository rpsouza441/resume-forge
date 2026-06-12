package com.resumeforge.model.converter;

import com.resumeforge.model.enums.ReportType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ReportTypeConverter implements AttributeConverter<ReportType, String> {
    @Override
    public String convertToDatabaseColumn(ReportType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public ReportType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : java.util.Arrays.stream(ReportType.values())
            .filter(e -> e.getValue().equals(dbData))
            .findFirst().orElse(null);
    }
}