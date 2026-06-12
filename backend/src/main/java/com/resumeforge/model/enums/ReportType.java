package com.resumeforge.model.enums;

/**
 * Enumeration of analysis report types stored in the analysis_reports table.
 * Values correspond to the PostgreSQL CHECK constraint:
 * analysis_reports.report_type IN ('ats_score','keyword_analysis','format_analysis','content_quality','combined')
 */
public enum ReportType {

    ATS_SCORE("ats_score"),
    KEYWORD_ANALYSIS("keyword_analysis"),
    FORMAT_ANALYSIS("format_analysis"),
    CONTENT_QUALITY("content_quality"),
    COMBINED("combined");

    private final String value;

    ReportType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
