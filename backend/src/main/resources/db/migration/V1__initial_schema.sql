-- Flyway Migration V1: Initial Schema for ResumeForge
-- PostgreSQL 15+
-- Spec reference: SPEC-03

------------------------------------------------------------
-- Phase 1: Base tables (no FK dependencies first)
------------------------------------------------------------

CREATE TABLE users (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    last_login_at   TIMESTAMPTZ,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE
);

CREATE TABLE resume_profiles (
    id UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID            NOT NULL,
    title               VARCHAR(255)    NOT NULL,
    content_text        TEXT            NOT NULL,
    content_markdown    TEXT            NOT NULL,
    content_jsonb       JSONB           NOT NULL DEFAULT '{}',
    is_default          BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ
);

CREATE TABLE job_applications (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID            NOT NULL,
    resume_profile_id   UUID,
    job_title           VARCHAR(255)    NOT NULL,
    company_name        VARCHAR(255)    NOT NULL,
    job_url             TEXT,
    job_description     TEXT            NOT NULL,
    job_source          VARCHAR(50),
    job_location        VARCHAR(255),
    job_type            VARCHAR(50),
    seniority           VARCHAR(50),
    status              VARCHAR(30)     NOT NULL DEFAULT 'saved',
    status_changed_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    applied_at          TIMESTAMPTZ,
    notes               TEXT,
    contact_name        VARCHAR(255),
    contact_email       VARCHAR(255),
    generated_count     INTEGER         NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ
);

CREATE TABLE generated_resumes (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_profile_id       UUID            NOT NULL,
    job_application_id      UUID,
    version_number          INTEGER         NOT NULL DEFAULT 1,
    parent_version_id       UUID,
    is_current              BOOLEAN         NOT NULL DEFAULT TRUE,
    content_text            TEXT            NOT NULL,
    content_markdown        TEXT            NOT NULL,
    content_jsonb           JSONB           NOT NULL DEFAULT '{}',
    ai_model                VARCHAR(100)    NOT NULL,
    ai_provider             VARCHAR(50)     NOT NULL,
    prompt_version          VARCHAR(20)     NOT NULL DEFAULT 'v1',
    generation_reason       TEXT,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'completed',
    word_count              INTEGER,
    char_count              INTEGER,
    match_score             NUMERIC(5,2),
    readability_score       NUMERIC(5,2),
    docx_generated_at       TIMESTAMPTZ,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at              TIMESTAMPTZ
);

CREATE TABLE analysis_reports (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    generated_resume_id         UUID            NOT NULL,
    report_type                 VARCHAR(50)     NOT NULL,
    report_version              VARCHAR(20)     NOT NULL DEFAULT 'v1',
    overall_score               NUMERIC(5,2),
    ats_compatibility_score     NUMERIC(5,2),
    dimension_scores            JSONB           NOT NULL DEFAULT '{}',
    findings JSONB           NOT NULL DEFAULT '[]',
    recommendations JSONB           NOT NULL DEFAULT '[]',
    analyzed_fields             JSONB           NOT NULL DEFAULT '[]',
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE ai_runs (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID            NOT NULL,
    generated_resume_id     UUID,
    run_type                VARCHAR(50)     NOT NULL,
    prompt_version          VARCHAR(20)     NOT NULL,
    ai_model                VARCHAR(100)    NOT NULL,
    ai_provider             VARCHAR(50)     NOT NULL,
    input_tokens            INTEGER,
    output_tokens           INTEGER,
    total_tokens            INTEGER         GENERATED ALWAYS AS (COALESCE(input_tokens, 0) + COALESCE(output_tokens, 0)) STORED,
    latency_ms              INTEGER,
    estimated_cost_usd      NUMERIC(10,6),
    prompt_text             TEXT            NOT NULL,
    raw_response            TEXT,
    parsed_response         JSONB,
    context                 JSONB           NOT NULL DEFAULT '{}',
    status                  VARCHAR(30)     NOT NULL DEFAULT 'started',
    success BOOLEAN,
    error_code              VARCHAR(50),
    error_message           TEXT,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    completed_at             TIMESTAMPTZ
);

CREATE TABLE processing_logs (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID,
    ai_run_id           UUID,
    generated_resume_id UUID,
    log_level           VARCHAR(20)     NOT NULL,
    category            VARCHAR(50)     NOT NULL,
    message             TEXT            NOT NULL,
    context_data        JSONB NOT NULL DEFAULT '{}',
    source_service      VARCHAR(100),
    source_ip           VARCHAR(45),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

------------------------------------------------------------
-- Phase 2: Foreign Key constraints
------------------------------------------------------------

ALTER TABLE resume_profiles ADD CONSTRAINT fk_resume_profiles_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE job_applications ADD CONSTRAINT fk_job_applications_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE job_applications ADD CONSTRAINT fk_job_applications_resume_profile
    FOREIGN KEY (resume_profile_id) REFERENCES resume_profiles(id) ON DELETE SET NULL;

ALTER TABLE generated_resumes ADD CONSTRAINT fk_generated_resumes_profile
    FOREIGN KEY (resume_profile_id) REFERENCES resume_profiles(id) ON DELETE CASCADE;
ALTER TABLE generated_resumes ADD CONSTRAINT fk_generated_resumes_application
    FOREIGN KEY (job_application_id) REFERENCES job_applications(id) ON DELETE CASCADE;
ALTER TABLE generated_resumes ADD CONSTRAINT fk_generated_resumes_parent
    FOREIGN KEY (parent_version_id) REFERENCES generated_resumes(id) ON DELETE SET NULL;

ALTER TABLE analysis_reports ADD CONSTRAINT fk_analysis_reports_generated_resume
    FOREIGN KEY (generated_resume_id) REFERENCES generated_resumes(id) ON DELETE CASCADE;

ALTER TABLE ai_runs ADD CONSTRAINT fk_ai_runs_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE ai_runs ADD CONSTRAINT fk_ai_runs_generated_resume
    FOREIGN KEY (generated_resume_id) REFERENCES generated_resumes(id) ON DELETE SET NULL;

ALTER TABLE processing_logs ADD CONSTRAINT fk_processing_logs_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE processing_logs ADD CONSTRAINT fk_processing_logs_ai_run
    FOREIGN KEY (ai_run_id) REFERENCES ai_runs(id) ON DELETE SET NULL;
ALTER TABLE processing_logs ADD CONSTRAINT fk_processing_logs_generated_resume
    FOREIGN KEY (generated_resume_id) REFERENCES generated_resumes(id) ON DELETE SET NULL;

------------------------------------------------------------
-- Phase 3: Check constraints
------------------------------------------------------------

ALTER TABLE job_applications ADD CONSTRAINT chk_job_applications_job_type
    CHECK (job_type IN ('fulltime', 'parttime', 'contract', 'internship', 'remote'));
ALTER TABLE job_applications ADD CONSTRAINT chk_job_applications_status
    CHECK (status IN ('saved', 'applied', 'interviewing', 'offer', 'rejected', 'withdrawn', 'archived'));

ALTER TABLE generated_resumes ADD CONSTRAINT chk_generated_resumes_status
    CHECK (status IN ('completed'));
ALTER TABLE generated_resumes ADD CONSTRAINT chk_generated_resumes_version_number_positive
    CHECK (version_number >= 1);

ALTER TABLE analysis_reports ADD CONSTRAINT chk_analysis_reports_report_type
    CHECK (report_type IN ('ats_score', 'keyword_analysis', 'format_analysis', 'content_quality', 'combined'));

ALTER TABLE ai_runs ADD CONSTRAINT chk_ai_runs_run_type
    CHECK (run_type IN ('resume_generation', 'resume_regeneration'));
ALTER TABLE ai_runs ADD CONSTRAINT chk_ai_runs_status
    CHECK (status IN ('started', 'succeeded', 'failed'));

ALTER TABLE processing_logs ADD CONSTRAINT chk_processing_logs_log_level
    CHECK (log_level IN ('DEBUG', 'INFO', 'WARN', 'ERROR', 'CRITICAL'));
ALTER TABLE processing_logs ADD CONSTRAINT chk_processing_logs_category
    CHECK (category IN ('ai_request', 'data_sync', 'validation', 'export', 'import', 'system', 'security', 'billing'));

------------------------------------------------------------
-- Phase 4: Unique constraints
------------------------------------------------------------

ALTER TABLE users ADD CONSTRAINT users_email_unique UNIQUE (email);

------------------------------------------------------------
-- Phase 5: Indexes (31 total per SPEC-03 section 10)
------------------------------------------------------------

-- users
CREATE INDEX idx_users_email ON users (email);

-- resume_profiles
CREATE INDEX idx_resume_profiles_user_id ON resume_profiles (user_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX idx_resume_profiles_user_id_title_unique
    ON resume_profiles (user_id, title) WHERE deleted_at IS NULL;

-- job_applications
CREATE INDEX idx_job_applications_user_id ON job_applications (user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_job_applications_resume_profile_id ON job_applications (resume_profile_id) WHERE resume_profile_id IS NOT NULL;
CREATE INDEX idx_job_applications_status ON job_applications (status) WHERE deleted_at IS NULL;
CREATE INDEX idx_job_applications_created_at ON job_applications (created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_job_applications_job_title_gin ON job_applications USING gin (to_tsvector('english', job_title || ' ' || company_name));

-- generated_resumes
CREATE INDEX idx_generated_resumes_profile_id ON generated_resumes (resume_profile_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_generated_resumes_application_id ON generated_resumes (job_application_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_generated_resumes_is_current ON generated_resumes (is_current) WHERE is_current = TRUE AND deleted_at IS NULL;
CREATE INDEX idx_generated_resumes_created_at ON generated_resumes (created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_generated_resumes_content_text_search ON generated_resumes USING gin (to_tsvector('english', content_text));
CREATE INDEX idx_generated_resumes_content_jsonb ON generated_resumes USING gin (content_jsonb);
CREATE INDEX idx_generated_resumes_ai_model ON generated_resumes (ai_model, ai_provider);
CREATE UNIQUE INDEX idx_generated_resumes_unique_current_version
    ON generated_resumes (resume_profile_id, job_application_id)
    WHERE is_current = TRUE AND deleted_at IS NULL;

-- analysis_reports
CREATE INDEX idx_analysis_reports_generated_resume_id ON analysis_reports (generated_resume_id);
CREATE INDEX idx_analysis_reports_report_type ON analysis_reports (report_type);
CREATE INDEX idx_analysis_reports_overall_score ON analysis_reports (overall_score DESC) WHERE overall_score IS NOT NULL;
CREATE INDEX idx_analysis_reports_created_at ON analysis_reports (created_at DESC);
CREATE INDEX idx_analysis_reports_dimension_scores ON analysis_reports USING gin (dimension_scores);
CREATE INDEX idx_analysis_reports_findings ON analysis_reports USING gin (findings);

-- ai_runs
CREATE INDEX idx_ai_runs_user_id ON ai_runs (user_id);
CREATE INDEX idx_ai_runs_generated_resume_id ON ai_runs (generated_resume_id) WHERE generated_resume_id IS NOT NULL;
CREATE INDEX idx_ai_runs_run_type ON ai_runs (run_type);
CREATE INDEX idx_ai_runs_ai_model ON ai_runs (ai_model, ai_provider);
CREATE INDEX idx_ai_runs_created_at ON ai_runs (created_at DESC);
CREATE INDEX idx_ai_runs_user_monthly ON ai_runs (user_id, created_at DESC) WHERE success = TRUE;
CREATE INDEX idx_ai_runs_context ON ai_runs USING gin (context);
CREATE INDEX idx_ai_runs_success ON ai_runs (success) WHERE success = FALSE;

-- processing_logs
CREATE INDEX idx_processing_logs_user_id ON processing_logs (user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_processing_logs_ai_run_id ON processing_logs (ai_run_id) WHERE ai_run_id IS NOT NULL;
CREATE INDEX idx_processing_logs_generated_resume_id ON processing_logs (generated_resume_id) WHERE generated_resume_id IS NOT NULL;
CREATE INDEX idx_processing_logs_log_level ON processing_logs (log_level);
CREATE INDEX idx_processing_logs_category ON processing_logs (category);
CREATE INDEX idx_processing_logs_created_at ON processing_logs (created_at DESC);
CREATE INDEX idx_processing_logs_context_data ON processing_logs USING gin (context_data);

------------------------------------------------------------
-- Phase 6: Trigger functions
------------------------------------------------------------

-- Auto-update updated_at column
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Enforce exactly one is_default resume profile per user
CREATE OR REPLACE FUNCTION enforce_one_default_resume_profile()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_default = TRUE THEN
        UPDATE resume_profiles
        SET is_default = FALSE
        WHERE user_id = NEW.user_id
          AND id != NEW.id
          AND is_default = TRUE
          AND deleted_at IS NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Enforce exactly one is_current resume version per (resume_profile_id, job_application_id) pair
CREATE OR REPLACE FUNCTION enforce_one_current_resume_version()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_current = TRUE THEN
        UPDATE generated_resumes
        SET is_current = FALSE
        WHERE resume_profile_id = NEW.resume_profile_id
          AND job_application_id = NEW.job_application_id
          AND id != NEW.id
          AND is_current = TRUE
          AND deleted_at IS NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

------------------------------------------------------------
-- Phase 7: Triggers
------------------------------------------------------------

-- users: auto-update updated_at
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- resume_profiles: auto-update updated_at + enforce one default
CREATE TRIGGER trg_resume_profiles_updated_at
    BEFORE UPDATE ON resume_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_resume_profiles_one_default
    BEFORE INSERT OR UPDATE ON resume_profiles
    FOR EACH ROW
    WHEN (NEW.deleted_at IS NULL)
    EXECUTE FUNCTION enforce_one_default_resume_profile();

-- job_applications: auto-update updated_at
CREATE TRIGGER trg_job_applications_updated_at
    BEFORE UPDATE ON job_applications
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- generated_resumes: auto-update updated_at + enforce one current version
CREATE TRIGGER trg_generated_resumes_updated_at
    BEFORE UPDATE ON generated_resumes
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_generated_resumes_one_current
    BEFORE INSERT OR UPDATE ON generated_resumes
    FOR EACH ROW
    WHEN (NEW.deleted_at IS NULL)
    EXECUTE FUNCTION enforce_one_current_resume_version();
