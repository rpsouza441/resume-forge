package com.resumeforge.model.enums;

/**
 * Enumeration of job application statuses stored in the job_applications table.
 * Values correspond to the PostgreSQL CHECK constraint:
 * job_applications.status IN ('saved','applied','interviewing','offer','rejected','withdrawn','archived')
 */
public enum JobStatus {

    SAVED("saved"),
    APPLIED("applied"),
    INTERVIEWING("interviewing"),
    OFFER("offer"),
    REJECTED("rejected"),
    WITHDRAWN("withdrawn"),
    ARCHIVED("archived");

    private final String value;

    JobStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
