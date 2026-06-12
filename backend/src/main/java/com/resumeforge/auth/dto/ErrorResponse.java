package com.resumeforge.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ErrorResponse {

    private String error;
    private String message;
    private List<Map<String, String>> details;
    private String timestamp;

    public static ErrorResponse of(String error, String message) {
        return ErrorResponse.builder()
                .error(error)
                .message(message)
                .timestamp(OffsetDateTime.now().toString())
                .build();
    }

    public static ErrorResponse withDetails(String error, String message, List<Map<String, String>> details) {
        return ErrorResponse.builder()
                .error(error)
                .message(message)
                .details(details)
                .timestamp(OffsetDateTime.now().toString())
                .build();
    }
}
