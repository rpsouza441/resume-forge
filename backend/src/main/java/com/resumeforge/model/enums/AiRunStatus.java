package com.resumeforge.model.enums;

/**
 * Enumeration of AI run statuses stored in the ai_runs table.
 * Values correspond to the PostgreSQL CHECK constraint:
 * ai_runs.status IN ('started','succeeded','failed')
 */
public enum AiRunStatus {

    STARTED("started"),
    SUCCEEDED("succeeded"),
    FAILED("failed");

    private final String value;

    AiRunStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
