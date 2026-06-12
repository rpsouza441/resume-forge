package com.resumeforge.model.enums;

/**
 * Enumeration of AI run types stored in the ai_runs table.
 * Values correspond to the PostgreSQL CHECK constraint:
 * ai_runs.run_type IN ('resume_generation','resume_regeneration')
 */
public enum AiRunType {

    RESUME_GENERATION("resume_generation"),
    RESUME_REGENERATION("resume_regeneration");

    private final String value;

    AiRunType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
